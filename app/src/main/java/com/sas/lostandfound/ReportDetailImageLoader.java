package com.sas.lostandfound;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class specifically for loading images in Report Details / Item Details screen.
 * Safely resolves public storage URLs, loads valid uploaded images, and falls back
 * to the default placeholder image when the URL is missing, invalid, or fails to load.
 */
public class ReportDetailImageLoader {

    public interface OnImageClickListener {
        void onImageClick(View view);
    }

    /**
     * Cleans and resolves a list of valid public URLs from raw image URL sources.
     */
    public static List<String> resolveValidUrls(List<String> imageUrls, String fallbackUrl) {
        List<String> validUrls = new ArrayList<>();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String url : imageUrls) {
                String clean = sanitizeUrl(url);
                if (clean != null && !validUrls.contains(clean)) {
                    validUrls.add(clean);
                }
            }
        }
        if (validUrls.isEmpty() && fallbackUrl != null) {
            String clean = sanitizeUrl(fallbackUrl);
            if (clean != null) {
                validUrls.add(clean);
            }
        }
        return validUrls;
    }

    /**
     * Sanitizes a raw image URL string and ensures public availability via SupabaseStorageHelper.
     */
    public static String sanitizeUrl(String rawUrl) {
        if (rawUrl == null) return null;
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed) || "[]".equals(trimmed)) {
            return null;
        }
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
            if (trimmed.contains(",")) {
                String[] parts = trimmed.split(",");
                if (parts.length > 0) trimmed = parts[0].trim();
            }
        }
        trimmed = trimmed.replace("\"", "").replace("'", "").trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return null;
        }
        return SupabaseStorageHelper.ensurePublicUrl(trimmed);
    }

    /**
     * Loads an image into the target ImageView with placeholder and error fallback.
     * Clears image tint list so uploaded photos render immediately in full natural colors.
     */
    public static void loadImage(Context context, String rawUrl, ImageView imageView, @Nullable OnImageClickListener clickListener) {
        if (context == null || imageView == null) return;

        String publicUrl = sanitizeUrl(rawUrl);
        if (publicUrl != null && !publicUrl.isEmpty()) {
            imageView.setImageTintList(null);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            GlideApp.with(context)
                    .load(publicUrl)
                    .placeholder(R.drawable.ic_package)
                    .error(R.drawable.ic_package)
                    .thumbnail(0.1f)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            showPlaceholder(context, imageView);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            imageView.setImageTintList(null);
                            if (clickListener != null) {
                                imageView.setOnClickListener(clickListener::onImageClick);
                            }
                            return false;
                        }
                    })
                    .into(imageView);

            if (clickListener != null) {
                imageView.setOnClickListener(clickListener::onImageClick);
            }
        } else {
            showPlaceholder(context, imageView);
        }
    }

    /**
     * Displays default placeholder image with item details package tint.
     */
    public static void showPlaceholder(Context context, ImageView imageView) {
        if (context == null || imageView == null) return;
        GlideApp.with(context).clear(imageView);
        imageView.setImageResource(R.drawable.ic_package);
        int tintColor = androidx.core.content.ContextCompat.getColor(context, R.color.item_details_icon_package);
        imageView.setImageTintList(android.content.res.ColorStateList.valueOf(tintColor));
        imageView.setOnClickListener(null);
    }
}
