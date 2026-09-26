package com.sas.lostandfound;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.slider.Slider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Professional Full-Screen Image Editor.
 * Features Segmented Control (Preview | Edit), per-photo independent edit states,
 * interactive 8-handle Crop Overlay with aspect ratios, straighten, rotate/flip,
 * Rule-of-Thirds grid, live preview, per-image crop state, and Undo/Redo history.
 */
public class ImageEditorActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvTitle;
    private MaterialButton btnDone;

    private TextView btnModePreview, btnModeEdit;
    private ImageView ivEditorPreview;
    private View layoutAdjustmentControls;
    private TextView tvSliderLabel;
    private Slider sliderAdjustment;
    private View layoutBottomTools;
    private RecyclerView rvEditorThumbnails;

    private LinearLayout toolCrop, toolRotate, toolFlip, toolAdjust, toolFilters, toolText, toolEmoji, toolDraw, toolReset;

    // Crop UI Components
    private CropOverlayView cropOverlayView;
    private View layoutCropControls;
    private ImageButton btnCropCancel, btnCropUndo, btnCropRedo;
    private MaterialButton btnCropApply;
    private Chip chipRatioFree, chipRatioOriginal, chipRatio11, chipRatio32, chipRatio43, chipRatio54, chipRatio169, chipRatio916, chipRatioCircle;
    private ImageButton btnCropRotateLeft, btnCropRotateRight, btnCropFlipH, btnCropFlipV;
    private TextView tvStraightenLabel;
    private Slider sliderStraighten;

    private final List<ImageEditState> editStates = new ArrayList<>();
    private int activeIndex = 0;
    private boolean isPreviewMode = false;
    private boolean isCropMode = false;
    private int currentAdjustmentMode = 0; // 0=Brightness, 1=Contrast, 2=Saturation, 3=Warmth

    private EditorThumbnailAdapter thumbnailAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeManager.applyTheme(this);
        setContentView(R.layout.activity_image_editor);

        initViews();
        initCropViews();
        loadImagesFromIntent();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTitle = findViewById(R.id.tvTitle);
        btnDone = findViewById(R.id.btnDone);

        btnModePreview = findViewById(R.id.btnModePreview);
        btnModeEdit = findViewById(R.id.btnModeEdit);
        ivEditorPreview = findViewById(R.id.ivEditorPreview);

        layoutAdjustmentControls = findViewById(R.id.layoutAdjustmentControls);
        tvSliderLabel = findViewById(R.id.tvSliderLabel);
        sliderAdjustment = findViewById(R.id.sliderAdjustment);
        layoutBottomTools = findViewById(R.id.layoutBottomTools);

        rvEditorThumbnails = findViewById(R.id.rvEditorThumbnails);
        rvEditorThumbnails.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        thumbnailAdapter = new EditorThumbnailAdapter();
        rvEditorThumbnails.setAdapter(thumbnailAdapter);

        toolCrop = findViewById(R.id.toolCrop);
        toolRotate = findViewById(R.id.toolRotate);
        toolFlip = findViewById(R.id.toolFlip);
        toolAdjust = findViewById(R.id.toolAdjust);
        toolFilters = findViewById(R.id.toolFilters);
        toolText = findViewById(R.id.toolText);
        toolEmoji = findViewById(R.id.toolEmoji);
        toolDraw = findViewById(R.id.toolDraw);
        toolReset = findViewById(R.id.toolReset);
    }

    private void initCropViews() {
        cropOverlayView = findViewById(R.id.cropOverlayView);
        layoutCropControls = findViewById(R.id.layoutCropControls);

        btnCropCancel = findViewById(R.id.btnCropCancel);
        btnCropUndo = findViewById(R.id.btnCropUndo);
        btnCropRedo = findViewById(R.id.btnCropRedo);
        btnCropApply = findViewById(R.id.btnCropApply);

        chipRatioFree = findViewById(R.id.chipRatioFree);
        chipRatioOriginal = findViewById(R.id.chipRatioOriginal);
        chipRatio11 = findViewById(R.id.chipRatio11);
        chipRatio32 = findViewById(R.id.chipRatio32);
        chipRatio43 = findViewById(R.id.chipRatio43);
        chipRatio54 = findViewById(R.id.chipRatio54);
        chipRatio169 = findViewById(R.id.chipRatio169);
        chipRatio916 = findViewById(R.id.chipRatio916);
        chipRatioCircle = findViewById(R.id.chipRatioCircle);

        btnCropRotateLeft = findViewById(R.id.btnCropRotateLeft);
        btnCropRotateRight = findViewById(R.id.btnCropRotateRight);
        btnCropFlipH = findViewById(R.id.btnCropFlipH);
        btnCropFlipV = findViewById(R.id.btnCropFlipV);

        tvStraightenLabel = findViewById(R.id.tvStraightenLabel);
        sliderStraighten = findViewById(R.id.sliderStraighten);
    }

    private void loadImagesFromIntent() {
        ArrayList<Uri> uris = getIntent().getParcelableArrayListExtra("image_uris");
        if (uris == null || uris.isEmpty()) {
            Toast.makeText(this, "No image to edit", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        for (Uri uri : uris) {
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                try (InputStream is1 = getContentResolver().openInputStream(uri)) {
                    if (is1 != null) {
                        BitmapFactory.decodeStream(is1, null, options);
                    }
                }
                options.inSampleSize = calculateInSampleSize(options, 2048, 2048);
                options.inJustDecodeBounds = false;

                try (InputStream inputStream = getContentResolver().openInputStream(uri)) {
                    if (inputStream != null) {
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                        if (bitmap != null) {
                            editStates.add(new ImageEditState(uri, bitmap));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (editStates.isEmpty()) {
            Toast.makeText(this, "Failed to load images", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        renderActiveState();
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
        btnBack.setOnClickListener(v -> {
            if (isCropMode) {
                exitCropMode(false);
            } else {
                finish();
            }
        });

        btnDone.setOnClickListener(v -> exportAllEditedBitmapsAndFinish());

        btnModePreview.setOnClickListener(v -> setPreviewMode(true));

        btnModeEdit.setOnClickListener(v -> setPreviewMode(false));

        toolRotate.setOnClickListener(v -> {
            if (editStates.isEmpty()) return;
            ImageEditState state = editStates.get(activeIndex);
            state.rotationDegrees = (state.rotationDegrees + 90f) % 360f;
            renderActiveState();
        });

        toolFlip.setOnClickListener(v -> {
            if (editStates.isEmpty()) return;
            ImageEditState state = editStates.get(activeIndex);
            state.isFlippedHorizontal = !state.isFlippedHorizontal;
            renderActiveState();
        });

        toolAdjust.setOnClickListener(v -> {
            if (layoutAdjustmentControls.getVisibility() == View.VISIBLE) {
                layoutAdjustmentControls.setVisibility(View.GONE);
            } else {
                layoutAdjustmentControls.setVisibility(View.VISIBLE);
                setupSliderForMode(0); // Default to Brightness
            }
        });

        toolCrop.setOnClickListener(v -> enterCropMode());

        toolFilters.setOnClickListener(v -> {
            if (editStates.isEmpty()) return;
            ImageEditState state = editStates.get(activeIndex);
            state.filterId = (state.filterId + 1) % 7;
            String[] filterNames = {"Natural", "Warm", "Cool", "Vintage", "Mono", "Fade", "Bright"};
            Toast.makeText(this, "Filter: " + filterNames[state.filterId], Toast.LENGTH_SHORT).show();
            renderActiveState();
        });

        toolText.setOnClickListener(v -> showAddTextDialog());

        toolEmoji.setOnClickListener(v -> showAddEmojiDialog());

        toolDraw.setOnClickListener(v -> Toast.makeText(this, "Draw mode activated", Toast.LENGTH_SHORT).show());

        toolReset.setOnClickListener(v -> showResetConfirmationDialog());

        // Crop Toolbar Action Listeners
        btnCropCancel.setOnClickListener(v -> exitCropMode(false));
        btnCropApply.setOnClickListener(v -> exitCropMode(true));

        btnCropUndo.setOnClickListener(v -> undoCropStep());
        btnCropRedo.setOnClickListener(v -> redoCropStep());

        setupCropRatioChips();

        btnCropRotateLeft.setOnClickListener(v -> {
            if (editStates.isEmpty()) return;
            pushCropSnapshot();
            ImageEditState state = editStates.get(activeIndex);
            state.rotationDegrees = (state.rotationDegrees - 90f) % 360f;
            renderActiveState();
            updateCropOverlayBounds();
        });

        btnCropRotateRight.setOnClickListener(v -> {
            if (editStates.isEmpty()) return;
            pushCropSnapshot();
            ImageEditState state = editStates.get(activeIndex);
            state.rotationDegrees = (state.rotationDegrees + 90f) % 360f;
            renderActiveState();
            updateCropOverlayBounds();
        });

        btnCropFlipH.setOnClickListener(v -> {
            if (editStates.isEmpty()) return;
            pushCropSnapshot();
            ImageEditState state = editStates.get(activeIndex);
            state.isFlippedHorizontal = !state.isFlippedHorizontal;
            renderActiveState();
        });

        btnCropFlipV.setOnClickListener(v -> {
            if (editStates.isEmpty()) return;
            pushCropSnapshot();
            ImageEditState state = editStates.get(activeIndex);
            state.isFlippedVertical = !state.isFlippedVertical;
            renderActiveState();
        });

        sliderStraighten.addOnChangeListener((slider, value, fromUser) -> {
            if (!fromUser || editStates.isEmpty()) return;
            ImageEditState state = editStates.get(activeIndex);
            state.straightenAngle = value;
            tvStraightenLabel.setText("Straighten: " + (int) value + "°");
            renderActiveState();
            updateCropOverlayBounds();
        });
    }

    private void setupCropRatioChips() {
        Chip[] chips = {chipRatioFree, chipRatioOriginal, chipRatio11, chipRatio32, chipRatio43, chipRatio54, chipRatio169, chipRatio916, chipRatioCircle};
        int[] modes = {
                CropOverlayView.RATIO_FREE,
                CropOverlayView.RATIO_ORIGINAL,
                CropOverlayView.RATIO_1_1,
                CropOverlayView.RATIO_3_2,
                CropOverlayView.RATIO_4_3,
                CropOverlayView.RATIO_5_4,
                CropOverlayView.RATIO_16_9,
                CropOverlayView.RATIO_9_16,
                CropOverlayView.RATIO_CIRCLE
        };

        for (int i = 0; i < chips.length; i++) {
            final int mode = modes[i];
            final Chip chip = chips[i];
            chip.setOnClickListener(v -> {
                if (editStates.isEmpty()) return;
                pushCropSnapshot();
                ImageEditState state = editStates.get(activeIndex);
                state.cropAspectRatioMode = mode;
                updateCropChipSelection(mode);

                Bitmap bmp = state.originalBitmap;
                float origW = bmp != null ? bmp.getWidth() : 1;
                float origH = bmp != null ? bmp.getHeight() : 1;
                cropOverlayView.setAspectRatio(mode, origW, origH);
            });
        }
    }

    private void updateCropChipSelection(int activeMode) {
        Chip[] chips = {chipRatioFree, chipRatioOriginal, chipRatio11, chipRatio32, chipRatio43, chipRatio54, chipRatio169, chipRatio916, chipRatioCircle};
        int[] modes = {
                CropOverlayView.RATIO_FREE,
                CropOverlayView.RATIO_ORIGINAL,
                CropOverlayView.RATIO_1_1,
                CropOverlayView.RATIO_3_2,
                CropOverlayView.RATIO_4_3,
                CropOverlayView.RATIO_5_4,
                CropOverlayView.RATIO_16_9,
                CropOverlayView.RATIO_9_16,
                CropOverlayView.RATIO_CIRCLE
        };

        for (int i = 0; i < chips.length; i++) {
            if (modes[i] == activeMode) {
                chips[i].setChipBackgroundColorResource(R.color.crop_chip_bg_active);
                chips[i].setTextColor(ContextCompat.getColor(ImageEditorActivity.this, R.color.crop_chip_text_active));
            } else {
                chips[i].setChipBackgroundColorResource(R.color.crop_chip_bg_inactive);
                chips[i].setTextColor(ContextCompat.getColor(ImageEditorActivity.this, R.color.crop_chip_text_inactive));
            }
        }
    }

    private void enterCropMode() {
        if (editStates.isEmpty()) return;

        isCropMode = true;
        ImageEditState state = editStates.get(activeIndex);

        layoutBottomTools.setVisibility(View.GONE);
        layoutAdjustmentControls.setVisibility(View.GONE);

        layoutCropControls.setVisibility(View.VISIBLE);
        cropOverlayView.setVisibility(View.VISIBLE);

        pushCropSnapshot();

        updateCropChipSelection(state.cropAspectRatioMode);
        Bitmap bmp = state.originalBitmap;
        float origW = bmp != null ? bmp.getWidth() : 1;
        float origH = bmp != null ? bmp.getHeight() : 1;
        cropOverlayView.setAspectRatio(state.cropAspectRatioMode, origW, origH);

        sliderStraighten.setValue(state.straightenAngle);
        tvStraightenLabel.setText("Straighten: " + (int) state.straightenAngle + "°");

        ivEditorPreview.post(() -> {
            updateCropOverlayBounds();

            if (state.cropNormalizedRect != null && !state.cropNormalizedRect.isEmpty()) {
                RectF bounds = cropOverlayView.getImageBounds();
                if (!bounds.isEmpty()) {
                    RectF viewRect = new RectF(
                            bounds.left + state.cropNormalizedRect.left * bounds.width(),
                            bounds.top + state.cropNormalizedRect.top * bounds.height(),
                            bounds.left + state.cropNormalizedRect.right * bounds.width(),
                            bounds.top + state.cropNormalizedRect.bottom * bounds.height()
                    );
                    cropOverlayView.setCropRect(viewRect);
                }
            } else {
                cropOverlayView.resetCropToImageBounds();
            }
        });
    }

    private void exitCropMode(boolean applyChanges) {
        if (!isCropMode) return;

        isCropMode = false;
        if (!editStates.isEmpty()) {
            ImageEditState state = editStates.get(activeIndex);
            if (applyChanges) {
                RectF cropViewRect = cropOverlayView.getCropRect();
                RectF imageBounds = cropOverlayView.getImageBounds();
                if (!cropViewRect.isEmpty() && !imageBounds.isEmpty()) {
                    float normL = (cropViewRect.left - imageBounds.left) / imageBounds.width();
                    float normT = (cropViewRect.top - imageBounds.top) / imageBounds.height();
                    float normR = (cropViewRect.right - imageBounds.left) / imageBounds.width();
                    float normB = (cropViewRect.bottom - imageBounds.top) / imageBounds.height();

                    normL = Math.max(0f, Math.min(1f, normL));
                    normT = Math.max(0f, Math.min(1f, normT));
                    normR = Math.max(normL + 0.05f, Math.min(1f, normR));
                    normB = Math.max(normT + 0.05f, Math.min(1f, normB));

                    state.cropNormalizedRect = new RectF(normL, normT, normR, normB);
                }
            } else {
                // Restore pre-crop snapshot if cancelled
                if (!state.cropUndoStack.isEmpty()) {
                    ImageEditState.CropSnapshot initial = state.cropUndoStack.get(0);
                    state.cropNormalizedRect = initial.cropNormalizedRect;
                    state.straightenAngle = initial.straightenAngle;
                    state.rotationDegrees = initial.rotationDegrees;
                    state.isFlippedHorizontal = initial.isFlippedHorizontal;
                    state.isFlippedVertical = initial.isFlippedVertical;
                    state.cropAspectRatioMode = initial.cropAspectRatioMode;
                }
            }
        }

        cropOverlayView.setVisibility(View.GONE);
        layoutCropControls.setVisibility(View.GONE);

        if (!isPreviewMode) {
            layoutBottomTools.setVisibility(View.VISIBLE);
        }

        renderActiveState();
    }

    private void updateCropOverlayBounds() {
        if (!isCropMode || ivEditorPreview == null) return;
        RectF bounds = getImageDisplayBounds();
        cropOverlayView.setImageBounds(bounds);
    }

    private RectF getImageDisplayBounds() {
        if (ivEditorPreview == null || ivEditorPreview.getDrawable() == null) {
            return new RectF(0, 0, ivEditorPreview.getWidth(), ivEditorPreview.getHeight());
        }
        int dWidth = ivEditorPreview.getDrawable().getIntrinsicWidth();
        int dHeight = ivEditorPreview.getDrawable().getIntrinsicHeight();
        int vWidth = ivEditorPreview.getWidth();
        int vHeight = ivEditorPreview.getHeight();

        if (dWidth <= 0 || dHeight <= 0 || vWidth <= 0 || vHeight <= 0) {
            return new RectF(0, 0, vWidth, vHeight);
        }

        float scale;
        float dx = 0, dy = 0;
        if (dWidth * vHeight > vWidth * dHeight) {
            scale = (float) vWidth / (float) dWidth;
            dy = (vHeight - dHeight * scale) * 0.5f;
        } else {
            scale = (float) vHeight / (float) dHeight;
            dx = (vWidth - dWidth * scale) * 0.5f;
        }

        float left = dx;
        float top = dy;
        float right = left + dWidth * scale;
        float bottom = top + dHeight * scale;

        return new RectF(left, top, right, bottom);
    }

    private void pushCropSnapshot() {
        if (editStates.isEmpty()) return;
        ImageEditState state = editStates.get(activeIndex);
        RectF currentCrop = cropOverlayView != null ? cropOverlayView.getCropRect() : state.cropNormalizedRect;
        state.cropUndoStack.add(new ImageEditState.CropSnapshot(
                currentCrop,
                state.straightenAngle,
                state.rotationDegrees,
                state.isFlippedHorizontal,
                state.isFlippedVertical,
                state.cropAspectRatioMode
        ));
        state.cropRedoStack.clear();
    }

    private void undoCropStep() {
        if (editStates.isEmpty()) return;
        ImageEditState state = editStates.get(activeIndex);
        if (state.cropUndoStack.size() <= 1) return;

        ImageEditState.CropSnapshot current = state.cropUndoStack.remove(state.cropUndoStack.size() - 1);
        state.cropRedoStack.add(current);

        ImageEditState.CropSnapshot prev = state.cropUndoStack.get(state.cropUndoStack.size() - 1);
        applyCropSnapshot(prev);
    }

    private void redoCropStep() {
        if (editStates.isEmpty()) return;
        ImageEditState state = editStates.get(activeIndex);
        if (state.cropRedoStack.isEmpty()) return;

        ImageEditState.CropSnapshot next = state.cropRedoStack.remove(state.cropRedoStack.size() - 1);
        state.cropUndoStack.add(next);
        applyCropSnapshot(next);
    }

    private void applyCropSnapshot(ImageEditState.CropSnapshot snapshot) {
        if (snapshot == null || editStates.isEmpty()) return;
        ImageEditState state = editStates.get(activeIndex);

        state.straightenAngle = snapshot.straightenAngle;
        state.rotationDegrees = snapshot.rotationDegrees;
        state.isFlippedHorizontal = snapshot.isFlippedHorizontal;
        state.isFlippedVertical = snapshot.isFlippedVertical;
        state.cropAspectRatioMode = snapshot.cropAspectRatioMode;

        updateCropChipSelection(state.cropAspectRatioMode);
        Bitmap bmp = state.originalBitmap;
        float origW = bmp != null ? bmp.getWidth() : 1;
        float origH = bmp != null ? bmp.getHeight() : 1;
        cropOverlayView.setAspectRatio(state.cropAspectRatioMode, origW, origH);

        sliderStraighten.setValue(state.straightenAngle);
        tvStraightenLabel.setText("Straighten: " + (int) state.straightenAngle + "°");

        renderActiveState();
        updateCropOverlayBounds();

        if (snapshot.cropNormalizedRect != null) {
            cropOverlayView.setCropRect(snapshot.cropNormalizedRect);
        }
    }

    private void setupSliderForMode(int mode) {
        currentAdjustmentMode = mode;
        sliderAdjustment.clearOnChangeListeners();
        ImageEditState state = editStates.get(activeIndex);

        switch (mode) {
            case 0: // Brightness (-100 to +100)
                tvSliderLabel.setText("Brightness");
                sliderAdjustment.setValueFrom(-100f);
                sliderAdjustment.setValueTo(100f);
                sliderAdjustment.setValue(state.brightness);
                break;
            case 1: // Contrast (0.5 to 2.0)
                tvSliderLabel.setText("Contrast");
                sliderAdjustment.setValueFrom(0.5f);
                sliderAdjustment.setValueTo(2.0f);
                sliderAdjustment.setValue(state.contrast);
                break;
            case 2: // Saturation (0.0 to 2.0)
                tvSliderLabel.setText("Saturation");
                sliderAdjustment.setValueFrom(0.0f);
                sliderAdjustment.setValueTo(2.0f);
                sliderAdjustment.setValue(state.saturation);
                break;
            case 3: // Warmth (-50 to +50)
                tvSliderLabel.setText("Warmth");
                sliderAdjustment.setValueFrom(-50f);
                sliderAdjustment.setValueTo(50f);
                sliderAdjustment.setValue(state.warmth);
                break;
        }

        sliderAdjustment.addOnChangeListener((slider, value, fromUser) -> {
            if (!fromUser || editStates.isEmpty()) return;
            ImageEditState st = editStates.get(activeIndex);
            if (mode == 0) st.brightness = value;
            else if (mode == 1) st.contrast = value;
            else if (mode == 2) st.saturation = value;
            else if (mode == 3) st.warmth = value;
            renderActiveState();
        });
    }

    private void setPreviewMode(boolean preview) {
        isPreviewMode = preview;
        int activeText = ContextCompat.getColor(this, R.color.editor_segmented_text_active);
        int inactiveText = ContextCompat.getColor(this, R.color.editor_segmented_text_inactive);

        if (preview) {
            btnModePreview.setTextColor(activeText);
            btnModePreview.setBackgroundResource(R.drawable.bg_segmented_active);
            btnModeEdit.setTextColor(inactiveText);
            btnModeEdit.setBackground(null);

            layoutAdjustmentControls.setVisibility(View.GONE);
            layoutBottomTools.setVisibility(View.GONE);
            if (isCropMode) exitCropMode(false);
        } else {
            btnModeEdit.setTextColor(activeText);
            btnModeEdit.setBackgroundResource(R.drawable.bg_segmented_active);
            btnModePreview.setTextColor(inactiveText);
            btnModePreview.setBackground(null);

            if (!isCropMode) {
                layoutBottomTools.setVisibility(View.VISIBLE);
            }
        }
    }

    private void renderActiveState() {
        if (editStates.isEmpty() || activeIndex < 0 || activeIndex >= editStates.size()) return;

        ImageEditState state = editStates.get(activeIndex);
        if (state.originalBitmap == null) return;

        Matrix matrix = new Matrix();
        if (state.isFlippedHorizontal) {
            matrix.postScale(-1f, 1f);
        }
        if (state.isFlippedVertical) {
            matrix.postScale(1f, -1f);
        }
        float totalRotation = state.rotationDegrees + state.straightenAngle;
        if (totalRotation != 0f) {
            matrix.postRotate(totalRotation);
        }

        Bitmap transformedBase = Bitmap.createBitmap(
                state.originalBitmap,
                0,
                0,
                state.originalBitmap.getWidth(),
                state.originalBitmap.getHeight(),
                matrix,
                true
        );

        Bitmap finalBitmap = transformedBase;

        // Apply crop if set and not in interactive crop mode
        if (!isCropMode && state.cropNormalizedRect != null && !state.cropNormalizedRect.isEmpty()) {
            int bw = transformedBase.getWidth();
            int bh = transformedBase.getHeight();

            int cx = Math.max(0, (int) (state.cropNormalizedRect.left * bw));
            int cy = Math.max(0, (int) (state.cropNormalizedRect.top * bh));
            int cw = Math.min(bw - cx, (int) (state.cropNormalizedRect.width() * bw));
            int ch = Math.min(bh - cy, (int) (state.cropNormalizedRect.height() * bh));

            if (cw > 10 && ch > 10) {
                Bitmap cropped = Bitmap.createBitmap(transformedBase, cx, cy, cw, ch);
                if (state.cropAspectRatioMode == CropOverlayView.RATIO_CIRCLE) {
                    Bitmap circular = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888);
                    Canvas c = new Canvas(circular);
                    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                    c.drawCircle(cw / 2f, ch / 2f, Math.min(cw, ch) / 2f, p);
                    p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                    c.drawBitmap(cropped, 0, 0, p);
                    finalBitmap = circular;
                } else {
                    finalBitmap = cropped;
                }
            }
        }

        ivEditorPreview.setImageBitmap(finalBitmap);

        // Combined ColorMatrix calculation
        ColorMatrix cm = calculateCombinedColorMatrix(state);
        ivEditorPreview.setColorFilter(new ColorMatrixColorFilter(cm));

        thumbnailAdapter.notifyDataSetChanged();
    }

    private ColorMatrix calculateCombinedColorMatrix(ImageEditState state) {
        ColorMatrix cm = new ColorMatrix();

        // 1. Saturation
        cm.setSaturation(state.saturation);

        // 2. Brightness (-100 to +100) & Contrast (0.5 to 2.0)
        float scale = state.contrast;
        float translate = state.brightness;
        ColorMatrix contrastMatrix = new ColorMatrix(new float[] {
                scale, 0,     0,     0, translate,
                0,     scale, 0,     0, translate,
                0,     0,     scale, 0, translate,
                0,     0,     0,     1, 0
        });
        cm.postConcat(contrastMatrix);

        // 3. Warmth (-50 to +50)
        if (state.warmth != 0f) {
            float rOffset = state.warmth;
            float bOffset = -state.warmth;
            ColorMatrix warmthMatrix = new ColorMatrix(new float[] {
                    1, 0, 0, 0, rOffset,
                    0, 1, 0, 0, 0,
                    0, 0, 1, 0, bOffset,
                    0, 0, 0, 1, 0
            });
            cm.postConcat(warmthMatrix);
        }

        // 4. Presets
        switch (state.filterId) {
            case 1: // Warm
                cm.postConcat(new ColorMatrix(new float[] { 1.1f, 0, 0, 0, 10, 0, 1.0f, 0, 0, 0, 0, 0, 0.9f, 0, -10, 0, 0, 0, 1, 0 }));
                break;
            case 2: // Cool
                cm.postConcat(new ColorMatrix(new float[] { 0.9f, 0, 0, 0, -10, 0, 1.0f, 0, 0, 0, 0, 0, 1.2f, 0, 15, 0, 0, 0, 1, 0 }));
                break;
            case 3: // Vintage
                cm.postConcat(new ColorMatrix(new float[] { 0.9f, 0.1f, 0.1f, 0, 10, 0.1f, 0.8f, 0.1f, 0, 5, 0.1f, 0.1f, 0.7f, 0, 0, 0, 0, 0, 1, 0 }));
                break;
            case 4: // Mono
                ColorMatrix mono = new ColorMatrix();
                mono.setSaturation(0);
                cm.postConcat(mono);
                break;
            case 5: // Fade
                cm.postConcat(new ColorMatrix(new float[] { 0.9f, 0, 0, 0, 20, 0, 0.9f, 0, 0, 20, 0, 0, 0.9f, 0, 20, 0, 0, 0, 1, 0 }));
                break;
            case 6: // Bright
                cm.postConcat(new ColorMatrix(new float[] { 1.2f, 0, 0, 0, 15, 0, 1.2f, 0, 0, 15, 0, 0, 1.2f, 0, 15, 0, 0, 0, 1, 0 }));
                break;
        }

        return cm;
    }

    private void showAddTextDialog() {
        if (editStates.isEmpty()) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Text Overlay");

        final EditText input = new EditText(this);
        input.setHint("Enter text...");
        input.setText(editStates.get(activeIndex).overlayText);
        builder.setView(input);

        builder.setPositiveButton("Add", (dialog, which) -> {
            editStates.get(activeIndex).overlayText = input.getText().toString();
            renderActiveState();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showAddEmojiDialog() {
        if (editStates.isEmpty()) return;
        String[] emojis = {"😊", "❤️", "🔥", "👍", "⭐", "🎉", "Lost & Found"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Sticker / Emoji");
        builder.setItems(emojis, (dialog, which) -> {
            editStates.get(activeIndex).overlayText = emojis[which];
            renderActiveState();
        });
        builder.show();
    }

    private void showResetConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Reset Edits")
                .setMessage("Are you sure you want to reset all edits for this photo?")
                .setPositiveButton("Reset", (dialog, which) -> {
                    if (!editStates.isEmpty()) {
                        editStates.get(activeIndex).reset();
                        renderActiveState();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void exportAllEditedBitmapsAndFinish() {
        if (editStates.isEmpty()) return;

        ArrayList<Uri> editedUris = new ArrayList<>();
        File cacheDir = getCacheDir();

        for (int i = 0; i < editStates.size(); i++) {
            ImageEditState state = editStates.get(i);
            try {
                Matrix matrix = new Matrix();
                if (state.isFlippedHorizontal) matrix.postScale(-1f, 1f);
                if (state.isFlippedVertical) matrix.postScale(1f, -1f);
                float totalRot = state.rotationDegrees + state.straightenAngle;
                if (totalRot != 0f) matrix.postRotate(totalRot);

                Bitmap transformedBase = Bitmap.createBitmap(
                        state.originalBitmap,
                        0,
                        0,
                        state.originalBitmap.getWidth(),
                        state.originalBitmap.getHeight(),
                        matrix,
                        true
                );

                Bitmap croppedBitmap = transformedBase;
                if (state.cropNormalizedRect != null && !state.cropNormalizedRect.isEmpty()) {
                    int bw = transformedBase.getWidth();
                    int bh = transformedBase.getHeight();

                    int cx = Math.max(0, (int) (state.cropNormalizedRect.left * bw));
                    int cy = Math.max(0, (int) (state.cropNormalizedRect.top * bh));
                    int cw = Math.min(bw - cx, (int) (state.cropNormalizedRect.width() * bw));
                    int ch = Math.min(bh - cy, (int) (state.cropNormalizedRect.height() * bh));

                    if (cw > 10 && ch > 10) {
                        Bitmap cropped = Bitmap.createBitmap(transformedBase, cx, cy, cw, ch);
                        if (state.cropAspectRatioMode == CropOverlayView.RATIO_CIRCLE) {
                            Bitmap circular = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888);
                            Canvas c = new Canvas(circular);
                            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                            c.drawCircle(cw / 2f, ch / 2f, Math.min(cw, ch) / 2f, p);
                            p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
                            c.drawBitmap(cropped, 0, 0, p);
                            croppedBitmap = circular;
                        } else {
                            croppedBitmap = cropped;
                        }
                    }
                }

                Bitmap finalBitmap = croppedBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Canvas canvas = new Canvas(finalBitmap);
                Paint paint = new Paint();

                ColorMatrix cm = calculateCombinedColorMatrix(state);
                paint.setColorFilter(new ColorMatrixColorFilter(cm));
                canvas.drawBitmap(croppedBitmap, 0, 0, paint);

                if (state.overlayText != null && !state.overlayText.isEmpty()) {
                    Paint textPaint = new Paint();
                    textPaint.setColor(state.textColor);
                    textPaint.setTextSize(48f);
                    textPaint.setAntiAlias(true);
                    textPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(state.overlayText, finalBitmap.getWidth() / 2f, finalBitmap.getHeight() / 2f, textPaint);
                }

                boolean isPng = state.cropAspectRatioMode == CropOverlayView.RATIO_CIRCLE;
                String ext = isPng ? ".png" : ".jpg";
                Bitmap.CompressFormat format = isPng ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;

                File tempFile = new File(cacheDir, "pro_edited_img_" + i + "_" + System.currentTimeMillis() + ext);
                try (FileOutputStream out = new FileOutputStream(tempFile)) {
                    finalBitmap.compress(format, 95, out);
                }
                Uri contentUri = androidx.core.content.FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempFile);
                editedUris.add(contentUri);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    private class EditorThumbnailAdapter extends RecyclerView.Adapter<EditorThumbnailAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_staging_thumbnail, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ImageEditState state = editStates.get(position);
            holder.ivThumbnail.setImageBitmap(state.originalBitmap);

            int activeBorder = ContextCompat.getColor(ImageEditorActivity.this, R.color.staging_card_border_active);
            int inactiveBorder = ContextCompat.getColor(ImageEditorActivity.this, R.color.staging_card_border_inactive);

            if (position == activeIndex) {
                holder.cardThumbnail.setStrokeColor(activeBorder);
                holder.cardThumbnail.setStrokeWidth((int) (4 * getResources().getDisplayMetrics().density));
            } else {
                holder.cardThumbnail.setStrokeColor(inactiveBorder);
                holder.cardThumbnail.setStrokeWidth((int) (1 * getResources().getDisplayMetrics().density));
            }

            holder.itemView.setOnClickListener(v -> {
                if (isCropMode) {
                    exitCropMode(true);
                }
                int prev = activeIndex;
                activeIndex = holder.getAdapterPosition();
                notifyItemChanged(prev);
                notifyItemChanged(activeIndex);
                renderActiveState();
            });
        }

        @Override
        public int getItemCount() {
            return editStates.size();
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
