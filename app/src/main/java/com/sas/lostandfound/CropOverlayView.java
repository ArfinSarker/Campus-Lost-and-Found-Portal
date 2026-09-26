package com.sas.lostandfound;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Professional Interactive Crop Overlay View.
 * Renders outer translucent dimming, 8 resizable touch handles (4 corners + 4 full edges),
 * Rule-of-Thirds grid, circle guide cutout, aspect ratio constraints, and multi-touch gestures.
 * Supports 100% independent edge and corner dragging in Free Crop mode.
 */
public class CropOverlayView extends View {

    public static final int RATIO_FREE = 0;
    public static final int RATIO_ORIGINAL = 1;
    public static final int RATIO_1_1 = 2;
    public static final int RATIO_3_2 = 3;
    public static final int RATIO_4_3 = 4;
    public static final int RATIO_5_4 = 5;
    public static final int RATIO_16_9 = 6;
    public static final int RATIO_9_16 = 7;
    public static final int RATIO_CIRCLE = 8;

    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_TOP_LEFT = 1;
    private static final int TOUCH_TOP_RIGHT = 2;
    private static final int TOUCH_BOTTOM_LEFT = 3;
    private static final int TOUCH_BOTTOM_RIGHT = 4;
    private static final int TOUCH_TOP = 5;
    private static final int TOUCH_BOTTOM = 6;
    private static final int TOUCH_LEFT = 7;
    private static final int TOUCH_RIGHT = 8;
    private static final int TOUCH_MOVE = 9;

    private static final float MIN_CROP_SIZE_DP = 48f;
    private static final float HANDLE_TOUCH_RADIUS_DP = 28f;
    private static final float HANDLE_CORNER_LENGTH_DP = 22f;
    private static final float HANDLE_CORNER_THICKNESS_DP = 4f;

    private final RectF cropRect = new RectF();
    private final RectF imageBounds = new RectF();
    private int aspectRatioMode = RATIO_FREE;
    private float targetAspectRatio = -1f; // width / height

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint handlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private int activeTouchMode = TOUCH_NONE;
    private float lastTouchX, lastTouchY;
    private boolean isDraggingOrResizing = false;

    private ScaleGestureDetector scaleGestureDetector;
    private float minCropSizePx;
    private float handleTouchRadiusPx;
    private float handleCornerLengthPx;
    private float handleCornerThicknessPx;

    public interface OnCropChangeListener {
        void OnCropRectChanged(RectF cropRect);
        void onCropInteractionStart();
        void onCropInteractionEnd();
    }
    private OnCropChangeListener cropChangeListener;

    public CropOverlayView(Context context) {
        super(context);
        init(context);
    }

    public CropOverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CropOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        float density = getResources().getDisplayMetrics().density;
        minCropSizePx = MIN_CROP_SIZE_DP * density;
        handleTouchRadiusPx = HANDLE_TOUCH_RADIUS_DP * density;
        handleCornerLengthPx = HANDLE_CORNER_LENGTH_DP * density;
        handleCornerThicknessPx = HANDLE_CORNER_THICKNESS_DP * density;

        dimPaint.setColor(ContextCompat.getColor(context, R.color.crop_overlay_dim));
        dimPaint.setStyle(Paint.Style.FILL);

        borderPaint.setColor(ContextCompat.getColor(context, R.color.crop_frame_stroke));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(1.5f * density);

        handlePaint.setColor(ContextCompat.getColor(context, R.color.crop_handle_color));
        handlePaint.setStyle(Paint.Style.STROKE);
        handlePaint.setStrokeWidth(handleCornerThicknessPx);
        handlePaint.setStrokeCap(Paint.Cap.ROUND);

        gridPaint.setColor(ContextCompat.getColor(context, R.color.crop_grid_line));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f * density);

        circlePaint.setColor(ContextCompat.getColor(context, R.color.crop_frame_stroke));
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(2f * density);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                float factor = detector.getScaleFactor();
                scaleCropRect(factor);
                return true;
            }
        });
    }

    public void setOnCropChangeListener(OnCropChangeListener listener) {
        this.cropChangeListener = listener;
    }

    public void setImageBounds(RectF bounds) {
        if (bounds == null || bounds.isEmpty()) return;
        this.imageBounds.set(bounds);

        if (cropRect.isEmpty() || !imageBounds.contains(cropRect)) {
            resetCropToImageBounds();
        } else {
            constrainCropRectToImageBounds();
        }
        invalidate();
    }

    public RectF getCropRect() {
        return new RectF(cropRect);
    }

    public RectF getImageBounds() {
        return new RectF(imageBounds);
    }

    public int getAspectRatioMode() {
        return aspectRatioMode;
    }

    public void setCropRect(RectF rect) {
        if (rect != null && !rect.isEmpty()) {
            this.cropRect.set(rect);
            constrainCropRectToImageBounds();
            invalidate();
            if (cropChangeListener != null) cropChangeListener.OnCropRectChanged(cropRect);
        }
    }

    public void resetCropToImageBounds() {
        if (imageBounds.isEmpty()) return;
        cropRect.set(imageBounds);
        if (targetAspectRatio > 0) {
            applyTargetAspectRatio();
        }
        invalidate();
        if (cropChangeListener != null) cropChangeListener.OnCropRectChanged(cropRect);
    }

    public void setAspectRatio(int ratioMode, float originalWidth, float originalHeight) {
        this.aspectRatioMode = ratioMode;
        switch (ratioMode) {
            case RATIO_FREE:
                targetAspectRatio = -1f;
                break;
            case RATIO_ORIGINAL:
                targetAspectRatio = (originalWidth > 0 && originalHeight > 0) ? (originalWidth / originalHeight) : -1f;
                break;
            case RATIO_1_1:
            case RATIO_CIRCLE:
                targetAspectRatio = 1.0f;
                break;
            case RATIO_3_2:
                targetAspectRatio = 3.0f / 2.0f;
                break;
            case RATIO_4_3:
                targetAspectRatio = 4.0f / 3.0f;
                break;
            case RATIO_5_4:
                targetAspectRatio = 5.0f / 4.0f;
                break;
            case RATIO_16_9:
                targetAspectRatio = 16.0f / 9.0f;
                break;
            case RATIO_9_16:
                targetAspectRatio = 9.0f / 16.0f;
                break;
        }

        if (targetAspectRatio > 0) {
            applyTargetAspectRatio();
        }
        invalidate();
        if (cropChangeListener != null) cropChangeListener.OnCropRectChanged(cropRect);
    }

    private void applyTargetAspectRatio() {
        if (targetAspectRatio <= 0 || imageBounds.isEmpty()) return;

        float width = imageBounds.width();
        float height = imageBounds.height();
        float currentRatio = width / height;

        float cropW, cropH;
        if (currentRatio > targetAspectRatio) {
            cropH = height * 0.88f;
            cropW = cropH * targetAspectRatio;
        } else {
            cropW = width * 0.88f;
            cropH = cropW / targetAspectRatio;
        }

        float cx = imageBounds.centerX();
        float cy = imageBounds.centerY();

        cropRect.set(cx - cropW / 2f, cy - cropH / 2f, cx + cropW / 2f, cy + cropH / 2f);
        constrainCropRectToImageBounds();
    }

    private void constrainCropRectToImageBounds() {
        if (imageBounds.isEmpty()) return;

        if (cropRect.width() < minCropSizePx) {
            cropRect.right = cropRect.left + minCropSizePx;
        }
        if (cropRect.height() < minCropSizePx) {
            cropRect.bottom = cropRect.top + minCropSizePx;
        }

        if (cropRect.left < imageBounds.left) cropRect.left = imageBounds.left;
        if (cropRect.right > imageBounds.right) cropRect.right = imageBounds.right;
        if (cropRect.top < imageBounds.top) cropRect.top = imageBounds.top;
        if (cropRect.bottom > imageBounds.bottom) cropRect.bottom = imageBounds.bottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cropRect.isEmpty() || imageBounds.isEmpty()) return;

        // 1. Draw outer translucent dimming mask
        canvas.save();
        Path path = new Path();
        path.addRect(imageBounds, Path.Direction.CW);

        if (aspectRatioMode == RATIO_CIRCLE) {
            path.addOval(cropRect, Path.Direction.CCW);
        } else {
            path.addRect(cropRect, Path.Direction.CCW);
        }
        canvas.drawPath(path, dimPaint);
        canvas.restore();

        // 2. Draw Crop Frame Border or Circle Mask Border
        if (aspectRatioMode == RATIO_CIRCLE) {
            canvas.drawOval(cropRect, circlePaint);
        } else {
            canvas.drawRect(cropRect, borderPaint);
        }

        // 3. Draw Rule-of-Thirds Grid (visible while dragging/resizing or active)
        if (isDraggingOrResizing || aspectRatioMode != RATIO_FREE) {
            float stepW = cropRect.width() / 3f;
            float stepH = cropRect.height() / 3f;

            canvas.drawLine(cropRect.left + stepW, cropRect.top, cropRect.left + stepW, cropRect.bottom, gridPaint);
            canvas.drawLine(cropRect.left + stepW * 2f, cropRect.top, cropRect.left + stepW * 2f, cropRect.bottom, gridPaint);
            canvas.drawLine(cropRect.left, cropRect.top + stepH, cropRect.right, cropRect.top + stepH, gridPaint);
            canvas.drawLine(cropRect.left, cropRect.top + stepH * 2f, cropRect.right, cropRect.top + stepH * 2f, gridPaint);
        }

        // 4. Draw Corner and Edge Handles
        drawHandles(canvas);
    }

    private void drawHandles(Canvas canvas) {
        float l = cropRect.left;
        float t = cropRect.top;
        float r = cropRect.right;
        float b = cropRect.bottom;
        float len = Math.min(handleCornerLengthPx, Math.min(cropRect.width(), cropRect.height()) / 3.5f);

        // Corner Handles
        canvas.drawLine(l, t, l + len, t, handlePaint);
        canvas.drawLine(l, t, l, t + len, handlePaint);

        canvas.drawLine(r - len, t, r, t, handlePaint);
        canvas.drawLine(r, t, r, t + len, handlePaint);

        canvas.drawLine(l, b, l + len, b, handlePaint);
        canvas.drawLine(l, b - len, l, b, handlePaint);

        canvas.drawLine(r - len, b, r, b, handlePaint);
        canvas.drawLine(r, b - len, r, b, handlePaint);

        // Edge Midpoint Handles (Top, Bottom, Left, Right)
        if (aspectRatioMode != RATIO_CIRCLE) {
            float cx = cropRect.centerX();
            float cy = cropRect.centerY();
            canvas.drawLine(cx - len / 2f, t, cx + len / 2f, t, handlePaint);
            canvas.drawLine(cx - len / 2f, b, cx + len / 2f, b, handlePaint);
            canvas.drawLine(l, cy - len / 2f, l, cy + len / 2f, handlePaint);
            canvas.drawLine(r, cy - len / 2f, r, cy + len / 2f, handlePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);

        if (scaleGestureDetector.isInProgress()) {
            return true;
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                activeTouchMode = getTouchMode(x, y);
                lastTouchX = x;
                lastTouchY = y;

                if (activeTouchMode != TOUCH_NONE) {
                    isDraggingOrResizing = true;
                    invalidate();
                    if (cropChangeListener != null) cropChangeListener.onCropInteractionStart();
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (activeTouchMode != TOUCH_NONE) {
                    float dx = x - lastTouchX;
                    float dy = y - lastTouchY;
                    handleMoveOrResize(dx, dy);
                    lastTouchX = x;
                    lastTouchY = y;
                    invalidate();
                    if (cropChangeListener != null) cropChangeListener.OnCropRectChanged(cropRect);
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (activeTouchMode != TOUCH_NONE) {
                    activeTouchMode = TOUCH_NONE;
                    isDraggingOrResizing = false;
                    invalidate();
                    if (cropChangeListener != null) cropChangeListener.onCropInteractionEnd();
                    return true;
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    private int getTouchMode(float x, float y) {
        float rad = handleTouchRadiusPx;
        float l = cropRect.left;
        float t = cropRect.top;
        float r = cropRect.right;
        float b = cropRect.bottom;

        // 1. Check 4 Corner Handles first
        if (dist(x, y, l, t) < rad) return TOUCH_TOP_LEFT;
        if (dist(x, y, r, t) < rad) return TOUCH_TOP_RIGHT;
        if (dist(x, y, l, b) < rad) return TOUCH_BOTTOM_LEFT;
        if (dist(x, y, r, b) < rad) return TOUCH_BOTTOM_RIGHT;

        // 2. Check 4 Edges across full length (Top, Bottom, Left, Right)
        if (aspectRatioMode != RATIO_CIRCLE) {
            if (Math.abs(y - t) < rad && x >= l - rad && x <= r + rad) return TOUCH_TOP;
            if (Math.abs(y - b) < rad && x >= l - rad && x <= r + rad) return TOUCH_BOTTOM;
            if (Math.abs(x - l) < rad && y >= t - rad && y <= b + rad) return TOUCH_LEFT;
            if (Math.abs(x - r) < rad && y >= t - rad && y <= b + rad) return TOUCH_RIGHT;
        }

        // 3. Check Inside Drag Move
        if (cropRect.contains(x, y)) {
            return TOUCH_MOVE;
        }

        return TOUCH_NONE;
    }

    private float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void handleMoveOrResize(float dx, float dy) {
        if (activeTouchMode == TOUCH_MOVE) {
            cropRect.offset(dx, dy);
            if (cropRect.left < imageBounds.left) cropRect.offset(imageBounds.left - cropRect.left, 0);
            if (cropRect.right > imageBounds.right) cropRect.offset(imageBounds.right - cropRect.right, 0);
            if (cropRect.top < imageBounds.top) cropRect.offset(0, imageBounds.top - cropRect.top);
            if (cropRect.bottom > imageBounds.bottom) cropRect.offset(0, imageBounds.bottom - cropRect.bottom);
            return;
        }

        float l = cropRect.left;
        float t = cropRect.top;
        float r = cropRect.right;
        float b = cropRect.bottom;

        switch (activeTouchMode) {
            case TOUCH_TOP_LEFT:
                l += dx;
                t += dy;
                break;
            case TOUCH_TOP_RIGHT:
                r += dx;
                t += dy;
                break;
            case TOUCH_BOTTOM_LEFT:
                l += dx;
                b += dy;
                break;
            case TOUCH_BOTTOM_RIGHT:
                r += dx;
                b += dy;
                break;
            case TOUCH_TOP:
                t += dy;
                break;
            case TOUCH_BOTTOM:
                b += dy;
                break;
            case TOUCH_LEFT:
                l += dx;
                break;
            case TOUCH_RIGHT:
                r += dx;
                break;
        }

        // Clamp to min crop size and image boundaries without affecting non-dragged edges
        l = Math.max(imageBounds.left, Math.min(r - minCropSizePx, l));
        t = Math.max(imageBounds.top, Math.min(b - minCropSizePx, t));
        r = Math.min(imageBounds.right, Math.max(l + minCropSizePx, r));
        b = Math.min(imageBounds.bottom, Math.max(t + minCropSizePx, b));

        cropRect.set(l, t, r, b);

        if (targetAspectRatio > 0) {
            applyAspectRatioToRect();
            if (cropRect.left < imageBounds.left) cropRect.left = imageBounds.left;
            if (cropRect.right > imageBounds.right) cropRect.right = imageBounds.right;
            if (cropRect.top < imageBounds.top) cropRect.top = imageBounds.top;
            if (cropRect.bottom > imageBounds.bottom) cropRect.bottom = imageBounds.bottom;
        }
    }

    private void applyAspectRatioToRect() {
        float currentW = cropRect.width();
        float currentH = cropRect.height();

        if (currentH <= 0 || targetAspectRatio <= 0) return;

        float expectedW = currentH * targetAspectRatio;
        cropRect.right = cropRect.left + expectedW;
    }

    private void scaleCropRect(float factor) {
        if (factor <= 0 || cropRect.isEmpty()) return;
        float cx = cropRect.centerX();
        float cy = cropRect.centerY();
        float newW = cropRect.width() * factor;
        float newH = cropRect.height() * factor;

        if (newW < minCropSizePx || newH < minCropSizePx) return;

        cropRect.set(cx - newW / 2f, cy - newH / 2f, cx + newW / 2f, cy + newH / 2f);
        constrainCropRectToImageBounds();
        invalidate();
        if (cropChangeListener != null) cropChangeListener.OnCropRectChanged(cropRect);
    }
}
