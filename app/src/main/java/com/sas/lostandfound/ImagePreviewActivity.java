package com.sas.lostandfound;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-Image Preview & Editing Activity.
 * Supports thumbnail carousel navigation, adding/removing photos, individual per-photo rotations, flips, and color filters.
 */
public class ImagePreviewActivity extends AppCompatActivity {

    public static class ImageItem {
        public Uri sourceUri;
        public Bitmap originalBitmap;
        public float rotationDegrees = 0f;
        public boolean isFlippedHorizontal = false;
        public int filterMode = 0; // 0=Normal, 1=B&W, 2=Sepia, 3=Cool/Contrast

        public ImageItem(Uri sourceUri, Bitmap originalBitmap) {
            this.sourceUri = sourceUri;
            this.originalBitmap = originalBitmap;
        }
    }

    private ImageButton btnClose, btnAddMorePhotos, btnDeletePhoto;
    private MaterialButton btnSend;
    private ImageView ivPreview;
    private RecyclerView rvThumbnails;
    private ThumbnailAdapter thumbnailAdapter;

    private LinearLayout btnToolRotate, btnToolFlip, btnToolFilter, btnToolReset;
    private View layoutFilterOptions;
    private MaterialButton btnFilterNormal, btnFilterBw, btnFilterSepia, btnFilterVintage;

    private final List<ImageItem> imageItems = new ArrayList<>();
    private int activeIndex = 0;

    private ActivityResultLauncher<Intent> addMoreLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_image_preview);

        addMoreLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    List<Uri> newUris = new ArrayList<>();
                    if (data.getClipData() != null) {
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            newUris.add(data.getClipData().getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null) {
                        newUris.add(data.getData());
                    }
                    if (!newUris.isEmpty()) {
                        loadAdditionalUris(newUris);
                    }
                }
            }
        );

        initViews();
        loadInitialUrisFromIntent();
        setupListeners();
    }

    private void initViews() {
        btnClose = findViewById(R.id.btnClose);
        btnAddMorePhotos = findViewById(R.id.btnAddMorePhotos);
        btnDeletePhoto = findViewById(R.id.btnDeletePhoto);
        btnSend = findViewById(R.id.btnSend);
        ivPreview = findViewById(R.id.ivPreview);

        rvThumbnails = findViewById(R.id.rvThumbnails);
        rvThumbnails.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        thumbnailAdapter = new ThumbnailAdapter();
        rvThumbnails.setAdapter(thumbnailAdapter);

        btnToolRotate = findViewById(R.id.btnToolRotate);
        btnToolFlip = findViewById(R.id.btnToolFlip);
        btnToolFilter = findViewById(R.id.btnToolFilter);
        btnToolReset = findViewById(R.id.btnToolReset);
        layoutFilterOptions = findViewById(R.id.layoutFilterOptions);
        btnFilterNormal = findViewById(R.id.btnFilterNormal);
        btnFilterBw = findViewById(R.id.btnFilterBw);
        btnFilterSepia = findViewById(R.id.btnFilterSepia);
        btnFilterVintage = findViewById(R.id.btnFilterVintage);
    }

    private void loadInitialUrisFromIntent() {
        Intent intent = getIntent();
        List<Uri> uris = new ArrayList<>();

        if (intent != null) {
            ArrayList<Uri> listExtra = intent.getParcelableArrayListExtra("image_uris");
            if (listExtra != null && !listExtra.isEmpty()) {
                uris.addAll(listExtra);
            } else {
                Uri singleUri = intent.getParcelableExtra("image_uri");
                if (singleUri == null && intent.getData() != null) {
                    singleUri = intent.getData();
                }
                if (singleUri != null) {
                    uris.add(singleUri);
                }
            }
        }

        if (uris.isEmpty()) {
            Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadAdditionalUris(uris);
    }

    private void loadAdditionalUris(List<Uri> uris) {
        for (Uri uri : uris) {
            try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap != null) {
                    imageItems.add(new ImageItem(uri, bitmap));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (imageItems.isEmpty()) {
            Toast.makeText(this, "Failed to decode images", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (activeIndex >= imageItems.size()) {
            activeIndex = imageItems.size() - 1;
        }

        thumbnailAdapter.notifyDataSetChanged();
        renderActiveImage();
    }

    private void setupListeners() {
        btnClose.setOnClickListener(v -> finish());

        btnSend.setOnClickListener(v -> exportAndSendAllImages());

        btnAddMorePhotos.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/jpeg", "image/png", "image/webp"});
            addMoreLauncher.launch(Intent.createChooser(intent, "Select More Images"));
        });

        btnDeletePhoto.setOnClickListener(v -> {
            if (imageItems.isEmpty()) return;
            imageItems.remove(activeIndex);
            if (imageItems.isEmpty()) {
                finish();
                return;
            }
            if (activeIndex >= imageItems.size()) {
                activeIndex = imageItems.size() - 1;
            }
            thumbnailAdapter.notifyDataSetChanged();
            renderActiveImage();
        });

        btnToolRotate.setOnClickListener(v -> {
            if (imageItems.isEmpty()) return;
            ImageItem current = imageItems.get(activeIndex);
            current.rotationDegrees = (current.rotationDegrees + 90f) % 360f;
            renderActiveImage();
        });

        btnToolFlip.setOnClickListener(v -> {
            if (imageItems.isEmpty()) return;
            ImageItem current = imageItems.get(activeIndex);
            current.isFlippedHorizontal = !current.isFlippedHorizontal;
            renderActiveImage();
        });

        btnToolFilter.setOnClickListener(v -> {
            if (layoutFilterOptions.getVisibility() == View.VISIBLE) {
                layoutFilterOptions.setVisibility(View.GONE);
            } else {
                layoutFilterOptions.setVisibility(View.VISIBLE);
            }
        });

        btnToolReset.setOnClickListener(v -> {
            if (imageItems.isEmpty()) return;
            ImageItem current = imageItems.get(activeIndex);
            current.rotationDegrees = 0f;
            current.isFlippedHorizontal = false;
            current.filterMode = 0;
            layoutFilterOptions.setVisibility(View.GONE);
            renderActiveImage();
        });

        btnFilterNormal.setOnClickListener(v -> {
            if (imageItems.isEmpty()) return;
            imageItems.get(activeIndex).filterMode = 0;
            renderActiveImage();
        });

        btnFilterBw.setOnClickListener(v -> {
            if (imageItems.isEmpty()) return;
            imageItems.get(activeIndex).filterMode = 1;
            renderActiveImage();
        });

        btnFilterSepia.setOnClickListener(v -> {
            if (imageItems.isEmpty()) return;
            imageItems.get(activeIndex).filterMode = 2;
            renderActiveImage();
        });

        btnFilterVintage.setOnClickListener(v -> {
            if (imageItems.isEmpty()) return;
            imageItems.get(activeIndex).filterMode = 3;
            renderActiveImage();
        });
    }

    private void renderActiveImage() {
        if (imageItems.isEmpty() || activeIndex < 0 || activeIndex >= imageItems.size()) return;

        ImageItem current = imageItems.get(activeIndex);
        if (current.originalBitmap == null) return;

        Matrix matrix = new Matrix();
        if (current.isFlippedHorizontal) {
            matrix.postScale(-1f, 1f);
        }
        if (current.rotationDegrees != 0f) {
            matrix.postRotate(current.rotationDegrees);
        }

        Bitmap transformed = Bitmap.createBitmap(
                current.originalBitmap,
                0,
                0,
                current.originalBitmap.getWidth(),
                current.originalBitmap.getHeight(),
                matrix,
                true
        );

        ivPreview.setImageBitmap(transformed);

        // Apply Filter ColorMatrix
        ColorMatrix colorMatrix = new ColorMatrix();
        switch (current.filterMode) {
            case 1: // B&W
                colorMatrix.setSaturation(0);
                break;
            case 2: // Sepia
                colorMatrix.set(new float[] {
                        0.393f, 0.769f, 0.189f, 0, 0,
                        0.349f, 0.686f, 0.168f, 0, 0,
                        0.272f, 0.534f, 0.131f, 0, 0,
                        0,      0,      0,      1, 0
                });
                break;
            case 3: // Cool / Contrast
                colorMatrix.set(new float[] {
                        1.2f, 0,    0,    0, -10f,
                        0,    1.2f, 0,    0, -10f,
                        0,    0,    1.4f, 0, 10f,
                        0,    0,    0,    1, 0
                });
                break;
            default:
                break;
        }

        if (current.filterMode == 0) {
            ivPreview.setColorFilter(null);
        } else {
            ivPreview.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        }

        thumbnailAdapter.notifyDataSetChanged();
    }

    private void exportAndSendAllImages() {
        if (imageItems.isEmpty()) {
            Toast.makeText(this, "No image to send", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<Uri> editedUris = new ArrayList<>();
        File cacheDir = getCacheDir();

        for (int i = 0; i < imageItems.size(); i++) {
            ImageItem item = imageItems.get(i);
            try {
                Matrix matrix = new Matrix();
                if (item.isFlippedHorizontal) {
                    matrix.postScale(-1f, 1f);
                }
                if (item.rotationDegrees != 0f) {
                    matrix.postRotate(item.rotationDegrees);
                }

                Bitmap transformed = Bitmap.createBitmap(
                        item.originalBitmap,
                        0,
                        0,
                        item.originalBitmap.getWidth(),
                        item.originalBitmap.getHeight(),
                        matrix,
                        true
                );

                Bitmap finalBitmap = transformed;
                if (item.filterMode != 0) {
                    finalBitmap = transformed.copy(Bitmap.Config.ARGB_8888, true);
                    android.graphics.Canvas canvas = new android.graphics.Canvas(finalBitmap);
                    android.graphics.Paint paint = new android.graphics.Paint();
                    ColorMatrix cm = new ColorMatrix();
                    if (item.filterMode == 1) {
                        cm.setSaturation(0);
                    } else if (item.filterMode == 2) {
                        cm.set(new float[] {
                                0.393f, 0.769f, 0.189f, 0, 0,
                                0.349f, 0.686f, 0.168f, 0, 0,
                                0.272f, 0.534f, 0.131f, 0, 0,
                                0,      0,      0,      1, 0
                        });
                    } else if (item.filterMode == 3) {
                        cm.set(new float[] {
                                1.2f, 0,    0,    0, -10f,
                                0,    1.2f, 0,    0, -10f,
                                0,    0,    1.4f, 0, 10f,
                                0,    0,    0,    1, 0
                        });
                    }
                    paint.setColorFilter(new ColorMatrixColorFilter(cm));
                    canvas.drawBitmap(transformed, 0, 0, paint);
                }

                File tempFile = new File(cacheDir, "edited_chat_img_" + i + "_" + System.currentTimeMillis() + ".jpg");
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                }
                Uri contentUri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempFile);
                editedUris.add(contentUri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (editedUris.isEmpty()) {
            Toast.makeText(this, "Failed to export images", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent resultIntent = new Intent();
        resultIntent.putParcelableArrayListExtra("edited_image_uris", editedUris);
        resultIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (!editedUris.isEmpty()) {
            android.content.ClipData clipData = android.content.ClipData.newUri(getContentResolver(), "image", editedUris.get(0));
            for (int j = 1; j < editedUris.size(); j++) {
                clipData.addItem(new android.content.ClipData.Item(editedUris.get(j)));
            }
            resultIntent.setClipData(clipData);
        }
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private class ThumbnailAdapter extends RecyclerView.Adapter<ThumbnailAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_preview_thumbnail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ImageItem item = imageItems.get(position);
            holder.ivThumbnail.setImageBitmap(item.originalBitmap);

            int activeColor = ContextCompat.getColor(ImagePreviewActivity.this, R.color.image_preview_tool_active);
            int inactiveColor = ContextCompat.getColor(ImagePreviewActivity.this, R.color.image_preview_tool_inactive);

            if (position == activeIndex) {
                holder.cardThumbnail.setStrokeColor(activeColor);
                holder.cardThumbnail.setStrokeWidth((int) (3 * getResources().getDisplayMetrics().density));
            } else {
                holder.cardThumbnail.setStrokeColor(inactiveColor);
                holder.cardThumbnail.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
            }

            holder.itemView.setOnClickListener(v -> {
                int previousIndex = activeIndex;
                activeIndex = holder.getAdapterPosition();
                notifyItemChanged(previousIndex);
                notifyItemChanged(activeIndex);
                renderActiveImage();
            });
        }

        @Override
        public int getItemCount() {
            return imageItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardThumbnail;
            ImageView ivThumbnail;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardThumbnail = itemView.findViewById(R.id.cardThumbnail);
                ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            }
        }
    }
}
