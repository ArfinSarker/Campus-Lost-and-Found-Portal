package com.sas.lostandfound;

import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.RectF;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates independent per-photo editing parameters.
 */
public class ImageEditState {
    public Uri sourceUri;
    public Bitmap originalBitmap;

    // Transform
    public float rotationDegrees = 0f;
    public boolean isFlippedHorizontal = false;
    public boolean isFlippedVertical = false;

    // Crop & Straighten State
    public RectF cropNormalizedRect = null; // null means full uncropped image (0,0,1,1)
    public float straightenAngle = 0f;      // -45 to +45
    public int cropAspectRatioMode = CropOverlayView.RATIO_FREE;

    public static class CropSnapshot {
        public RectF cropNormalizedRect;
        public float straightenAngle;
        public float rotationDegrees;
        public boolean isFlippedHorizontal;
        public boolean isFlippedVertical;
        public int cropAspectRatioMode;

        public CropSnapshot(RectF cropNormalizedRect, float straightenAngle, float rotationDegrees, boolean isFlippedHorizontal, boolean isFlippedVertical, int cropAspectRatioMode) {
            this.cropNormalizedRect = cropNormalizedRect != null ? new RectF(cropNormalizedRect) : null;
            this.straightenAngle = straightenAngle;
            this.rotationDegrees = rotationDegrees;
            this.isFlippedHorizontal = isFlippedHorizontal;
            this.isFlippedVertical = isFlippedVertical;
            this.cropAspectRatioMode = cropAspectRatioMode;
        }
    }
    public List<CropSnapshot> cropUndoStack = new ArrayList<>();
    public List<CropSnapshot> cropRedoStack = new ArrayList<>();

    // Adjustments
    public float brightness = 0f;    // -100 to +100, default 0
    public float contrast = 1.0f;    // 0.5 to 2.0, default 1.0
    public float saturation = 1.0f;  // 0.0 to 2.0, default 1.0
    public float warmth = 0f;        // -50 to +50, default 0

    // Filters
    public int filterId = 0;         // 0=Natural, 1=Warm, 2=Cool, 3=Vintage, 4=Mono, 5=Fade, 6=Bright

    // Overlay Text
    public String overlayText = "";
    public int textColor = 0xFFFFFFFF;
    public float textX = 0.5f; // Relative canvas position 0.0 - 1.0
    public float textY = 0.5f;

    // Drawing Canvas
    public static class DrawPathItem {
        public Path path;
        public int color;
        public float strokeWidth;
        public boolean isEraser;

        public DrawPathItem(Path path, int color, float strokeWidth, boolean isEraser) {
            this.path = path;
            this.color = color;
            this.strokeWidth = strokeWidth;
            this.isEraser = isEraser;
        }
    }
    public List<DrawPathItem> drawPaths = new ArrayList<>();

    public ImageEditState(Uri sourceUri, Bitmap originalBitmap) {
        this.sourceUri = sourceUri;
        this.originalBitmap = originalBitmap;
    }

    public void reset() {
        rotationDegrees = 0f;
        isFlippedHorizontal = false;
        isFlippedVertical = false;
        cropNormalizedRect = null;
        straightenAngle = 0f;
        cropAspectRatioMode = CropOverlayView.RATIO_FREE;
        cropUndoStack.clear();
        cropRedoStack.clear();
        brightness = 0f;
        contrast = 1.0f;
        saturation = 1.0f;
        warmth = 0f;
        filterId = 0;
        overlayText = "";
        drawPaths.clear();
    }
}
