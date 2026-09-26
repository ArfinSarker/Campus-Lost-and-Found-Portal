package com.sas.lostandfound;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Image Staging Screen Activity.
 * Displays selected images (max 20) with side-by-side EDIT and SEND buttons.
 */
public class ImageStagingActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvTitle;
    private ImageView ivMainPreview;
    private RecyclerView rvThumbnails;
    private MaterialButton btnEdit, btnSend;

    private ThumbnailAdapter thumbnailAdapter;
    private final List<Uri> imageUris = new ArrayList<>();
    private final List<Bitmap> imageBitmaps = new ArrayList<>();
    private int activeIndex = 0;

    private String conversationId;
    private String currentUnivId;

    private ActivityResultLauncher<Intent> editorLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_image_staging);

        conversationId = getIntent().getStringExtra("conversationId");
        currentUnivId = getIntent().getStringExtra("currentUnivId");

        editorLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<Uri> editedUris = result.getData().getParcelableArrayListExtra("edited_image_uris");
                    if (editedUris != null && !editedUris.isEmpty()) {
                        imageUris.clear();
                        imageBitmaps.clear();
                        imageUris.addAll(editedUris);
                        if (activeIndex >= imageUris.size()) {
                            activeIndex = 0;
                        }
                        loadBitmapsFromUris();
                        thumbnailAdapter.notifyDataSetChanged();
                        updateMainPreview();
                    }
                }
            }
        );

        initViews();
        loadInitialUrisFromIntent();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        ivMainPreview = findViewById(R.id.ivMainPreview);

        rvThumbnails = findViewById(R.id.rvThumbnails);
        rvThumbnails.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        thumbnailAdapter = new ThumbnailAdapter();
        rvThumbnails.setAdapter(thumbnailAdapter);

        btnEdit = findViewById(R.id.btnEdit);
        btnSend = findViewById(R.id.btnSend);
    }

    private void loadInitialUrisFromIntent() {
        ArrayList<Uri> listExtra = getIntent().getParcelableArrayListExtra("image_uris");
        if (listExtra != null && !listExtra.isEmpty()) {
            imageUris.addAll(listExtra);
        } else {
            Uri singleUri = getIntent().getParcelableExtra("image_uri");
            if (singleUri != null) {
                imageUris.add(singleUri);
            }
        }

        if (imageUris.isEmpty()) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadBitmapsFromUris();
        updateMainPreview();
    }

    private void loadBitmapsFromUris() {
        imageBitmaps.clear();
        for (Uri uri : imageUris) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                try (InputStream is1 = getContentResolver().openInputStream(uri)) {
                    BitmapFactory.decodeStream(is1, null, options);
                }
                options.inSampleSize = calculateInSampleSize(options, 1080, 1080);
                options.inJustDecodeBounds = false;
                try (InputStream is2 = getContentResolver().openInputStream(uri)) {
                    Bitmap bitmap = BitmapFactory.decodeStream(is2, null, options);
                    imageBitmaps.add(bitmap);
                }
            } catch (Exception e) {
                e.printStackTrace();
                imageBitmaps.add(null);
            }
        }
        tvTitle.setText("Selected Images (" + imageUris.size() + ")");
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnEdit.setOnClickListener(v -> {
            if (imageUris.isEmpty()) return;
            Intent editorIntent = new Intent(this, ImageEditorActivity.class);
            editorIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            editorIntent.putParcelableArrayListExtra("image_uris", new ArrayList<>(imageUris));
            if (!imageUris.isEmpty()) {
                android.content.ClipData clipData = android.content.ClipData.newUri(getContentResolver(), "image", imageUris.get(0));
                for (int i = 1; i < imageUris.size(); i++) {
                    clipData.addItem(new android.content.ClipData.Item(imageUris.get(i)));
                }
                editorIntent.setClipData(clipData);
            }
            editorLauncher.launch(editorIntent);
        });

        btnSend.setOnClickListener(v -> uploadAndSendImages());
    }

    private void updateMainPreview() {
        if (imageBitmaps.isEmpty() || activeIndex < 0 || activeIndex >= imageBitmaps.size()) return;
        Bitmap current = imageBitmaps.get(activeIndex);
        if (current != null) {
            ivMainPreview.setImageBitmap(current);
        } else if (activeIndex < imageUris.size()) {
            ivMainPreview.setImageURI(imageUris.get(activeIndex));
        }
        thumbnailAdapter.notifyDataSetChanged();
    }

    private void uploadAndSendImages() {
        if (imageUris.isEmpty() || conversationId == null || currentUnivId == null) return;

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading 1 of " + imageUris.size() + "...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        List<String> uploadedUrls = new ArrayList<>();
        uploadNextImageBatch(0, uploadedUrls, progressDialog);
    }

    private void uploadNextImageBatch(int index, List<String> uploadedUrls, ProgressDialog progressDialog) {
        if (index >= imageUris.size()) {
            if (!isFinishing() && !isDestroyed()) {
                progressDialog.dismiss();
            }
            if (!uploadedUrls.isEmpty()) {
                sendMultiImageMessagePayload(uploadedUrls);
            }
            return;
        }

        if (!isFinishing() && !isDestroyed()) {
            progressDialog.setMessage("Uploading " + (index + 1) + " of " + imageUris.size() + "...");
        }

        Uri currentUri = imageUris.get(index);
        SupabaseStorageHelper.uploadImage(this, currentUri, new SupabaseStorageHelper.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                if (publicUrl != null && !publicUrl.isEmpty()) {
                    uploadedUrls.add(publicUrl);
                }
                uploadNextImageBatch(index + 1, uploadedUrls, progressDialog);
            }

            @Override
            public void onFailure(Exception e) {
                uploadNextImageBatch(index + 1, uploadedUrls, progressDialog);
            }
        });
    }

    private void sendMultiImageMessagePayload(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;

        String captionText = imageUrls.size() > 1 ? "[" + imageUrls.size() + " Photos]" : "[Photo]";
        Message imageMessage = new Message(conversationId, currentUnivId, captionText, imageUrls);
        SupabaseDatabaseHelper.insert("messages", imageMessage, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                UnreadBadgeHelper.sendBadgeUpdateBroadcast(ImageStagingActivity.this);
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(ImageStagingActivity.this, "Failed to send image message: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class ThumbnailAdapter extends RecyclerView.Adapter<ThumbnailAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staging_thumbnail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Bitmap bitmap = position < imageBitmaps.size() ? imageBitmaps.get(position) : null;
            if (bitmap != null) {
                holder.ivThumbnail.setImageBitmap(bitmap);
            } else if (position < imageUris.size()) {
                holder.ivThumbnail.setImageURI(imageUris.get(position));
            }

            int activeBorder = ContextCompat.getColor(ImageStagingActivity.this, R.color.staging_card_border_active);
            int inactiveBorder = ContextCompat.getColor(ImageStagingActivity.this, R.color.staging_card_border_inactive);

            if (position == activeIndex) {
                holder.cardThumbnail.setStrokeColor(activeBorder);
                holder.cardThumbnail.setStrokeWidth((int) (4 * getResources().getDisplayMetrics().density));
            } else {
                holder.cardThumbnail.setStrokeColor(inactiveBorder);
                holder.cardThumbnail.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
            }

            holder.itemView.setOnClickListener(v -> {
                int prev = activeIndex;
                activeIndex = holder.getAdapterPosition();
                notifyItemChanged(prev);
                notifyItemChanged(activeIndex);
                updateMainPreview();
            });
        }

        @Override
        public int getItemCount() {
            return imageUris.size();
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
