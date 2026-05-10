package com.sas.lostandfound;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to handle unified navigation and UI components for Lost/Found items.
 * Ensures consistent behavior across the application.
 */
public class ItemNavigationUtils {

    /**
     * Common method to setup image slider or a single image for an item.
     * Defaults to fitCenter for multi-image sliders to ensure full image visibility.
     */
    public static void setupImageOrSlider(Context context, List<String> urls, String fallbackUrl, 
                                        ImageView ivIcon, ViewPager2 viewPagerSlider, 
                                        TabLayout tabLayoutIndicator) {
        setupImageOrSlider(context, urls, fallbackUrl, ivIcon, viewPagerSlider, tabLayoutIndicator, true);
    }

    /**
     * Common method to setup image slider or a single image for an item with configurable scale type.
     */
    public static void setupImageOrSlider(Context context, List<String> urls, String fallbackUrl, 
                                        ImageView ivIcon, ViewPager2 viewPagerSlider, 
                                        TabLayout tabLayoutIndicator, boolean useFitCenter) {
        if (urls != null && urls.size() > 1 && viewPagerSlider != null) {
            if (ivIcon != null) ivIcon.setVisibility(View.GONE);
            viewPagerSlider.setVisibility(View.VISIBLE);
            if (tabLayoutIndicator != null) tabLayoutIndicator.setVisibility(View.VISIBLE);

            ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(urls, useFitCenter);
            sliderAdapter.setOnImageClickListener(pos -> openFullScreenImage(context, urls, pos));
            viewPagerSlider.setAdapter(sliderAdapter);
            if (tabLayoutIndicator != null) {
                new TabLayoutMediator(tabLayoutIndicator, viewPagerSlider, (tab, pos) -> {}).attach();
            }
        } else {
            if (viewPagerSlider != null) viewPagerSlider.setVisibility(View.GONE);
            if (tabLayoutIndicator != null) tabLayoutIndicator.setVisibility(View.GONE);
            if (ivIcon != null) {
                ivIcon.setVisibility(View.VISIBLE);
                String imageUrl = (urls != null && !urls.isEmpty()) ? urls.get(0) : fallbackUrl;
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    GlideApp.with(context)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_package)
                            .thumbnail(0.1f)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(ivIcon);
                    ivIcon.setOnClickListener(v -> {
                        List<String> singleUrl = new ArrayList<>();
                        singleUrl.add(imageUrl);
                        openFullScreenImage(context, singleUrl, 0);
                    });
                } else {
                    ivIcon.setImageResource(R.drawable.ic_package);
                    ivIcon.setOnClickListener(null);
                }
            }
        }
    }

    /**
     * Navigates to the ItemDetailActivity with all necessary item data.
     * This version includes an isAdmin flag for role-based UI rendering.
     */
    public static void navigateToDetail(Context context, Item item, boolean isAdmin) {
        if (!canNavigate()) return;
        if (context == null || item == null) return;
        
        Intent intent = new Intent(context, ItemDetailActivity.class);
        intent.putExtra("itemId", item.getId());
        intent.putExtra("itemName", item.getName());
        intent.putExtra("itemDescription", item.getDescription());
        intent.putExtra("itemLocation", item.getLocation());
        intent.putExtra("manualLocation", item.getManualLocation());
        intent.putExtra("additionalLocationDetails", item.getAdditionalLocationDetails());
        intent.putExtra("itemDate", item.getDate());
        intent.putExtra("itemTime", item.getTime());
        intent.putExtra("itemStatus", item.getStatus());
        intent.putExtra("itemCategory", item.getCategory());
        intent.putExtra("itemImageUrl", item.getImageUrl());
        intent.putExtra("userName", item.getUserName());
        intent.putExtra("userDepartment", item.getUserDepartment());
        intent.putExtra("userPhone", item.getUserPhone());
        intent.putExtra("userId", item.getUserId());
        intent.putExtra("itemReportId", item.getDisplayId());
        intent.putExtra("isAdmin", isAdmin);

        context.startActivity(intent);
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).overridePendingTransition(R.anim.material_shared_axis_z_enter, R.anim.material_shared_axis_z_exit);
        }
    }

    /**
     * Navigates to the ItemDetailActivity with default user privileges.
     */
    public static void navigateToDetail(Context context, Item item) {
        navigateToDetail(context, item, false);
    }

    private static long lastClickTime = 0;
    private static final long CLICK_THRESHOLD = 500; // ms

    /**
     * Opens the FullScreenImageActivity to view images in full screen.
     * Includes a global debounce to prevent double-launching the activity.
     */
    public static void openFullScreenImage(Context context, List<String> imageUrls, int position) {
        if (!canNavigate()) return;
        if (context == null || imageUrls == null || imageUrls.isEmpty()) return;

        Intent intent = new Intent(context, FullScreenImageActivity.class);
        intent.putStringArrayListExtra("imageUrls", new ArrayList<>(imageUrls));
        intent.putExtra("position", position);
        context.startActivity(intent);
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).overridePendingTransition(R.anim.material_shared_axis_z_enter, R.anim.material_shared_axis_z_exit);
        }
    }

    /**
     * Global navigation debounce check.
     * Returns true if navigation is allowed, false otherwise.
     */
    public static boolean canNavigate() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < CLICK_THRESHOLD) {
            return false;
        }
        lastClickTime = currentTime;
        return true;
    }
}
