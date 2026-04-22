package com.sas.lostandfound;

import android.app.Activity;
import android.graphics.Color;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;

import com.google.android.material.appbar.AppBarLayout;

/**
 * Utility class to implement smooth, scroll-based header color transitions
 * mimicking the Telegram X dynamic header behavior.
 */
public class HeaderColorHelper {

    /**
     * Sets up the dynamic header color behavior for an activity.
     *
     * @param activity     The activity whose status bar will be updated.
     * @param appBarLayout The AppBarLayout whose background will transition.
     * @param toolbar      The Toolbar whose background will transition (optional).
     */
    public static void setup(Activity activity, AppBarLayout appBarLayout, Toolbar toolbar) {
        if (activity == null || appBarLayout == null) return;

        // Force disable liftOnScroll to prevent color flickering/switching conflicts
        appBarLayout.setLiftOnScroll(false);

        // Requirement: Default header color is primary blue
        final int primaryColor = ContextCompat.getColor(activity, R.color.primaryColor);
        
        // Requirement: Gradually become darker when scrolling down
        // Blending with black (40%) to create a smooth dark variant
        final int darkColor = ColorUtils.blendARGB(primaryColor, Color.BLACK, 0.4f);

        appBarLayout.addOnOffsetChangedListener((layout, verticalOffset) -> {
            int scrollRange = layout.getTotalScrollRange();
            if (scrollRange <= 0) return;

            // Calculate stable scroll depth percentage (0.0 to 1.0)
            float percentage = (float) Math.abs(verticalOffset) / (float) scrollRange;
            // Clamp percentage to ensure stability during overscroll or jitter
            percentage = Math.max(0f, Math.min(1f, percentage));

            // Linear transition from primary blue to dark color based on scroll depth
            int currentColor = ColorUtils.blendARGB(primaryColor, darkColor, percentage);

            // Apply the current color directly to backgrounds
            layout.setBackgroundColor(currentColor);
            if (toolbar != null) {
                toolbar.setBackgroundColor(currentColor);
            }

            // Sync status bar color for a fluid and immersive experience
            activity.getWindow().setStatusBarColor(currentColor);

            // Adjust elevation dynamically up to 8dp to add depth during scroll
            float elevation = percentage * 8 * activity.getResources().getDisplayMetrics().density;
            layout.setElevation(elevation);
        });
    }

    /**
     * Overloaded setup method for when a Toolbar reference is not available or handled separately.
     */
    public static void setup(Activity activity, AppBarLayout appBarLayout) {
        setup(activity, appBarLayout, null);
    }
}
