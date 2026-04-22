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
     */
    public static void setupImageOrSlider(Context context, List<String> urls, String fallbackUrl, 
                                        ImageView ivIcon, ViewPager2 viewPagerSlider, 
                                        TabLayout tabLayoutIndicator) {
        if (urls != null && urls.size() > 1 && viewPagerSlider != null) {
            if (ivIcon != null) ivIcon.setVisibility(View.GONE);
            viewPagerSlider.setVisibility(View.VISIBLE);
            if (tabLayoutIndicator != null) tabLayoutIndicator.setVisibility(View.VISIBLE);

            ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(urls);
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
    }

    /**
     * Navigates to the ItemDetailActivity with default user privileges.
     */
    public static void navigateToDetail(Context context, Item item) {
        navigateToDetail(context, item, false);
    }

    /**
     * Opens the FullScreenImageActivity to view images in full screen.
     */
    public static void openFullScreenImage(Context context, List<String> imageUrls, int position) {
        if (context == null || imageUrls == null || imageUrls.isEmpty()) return;
        
        Intent intent = new Intent(context, FullScreenImageActivity.class);
        intent.putStringArrayListExtra("imageUrls", new ArrayList<>(imageUrls));
        intent.putExtra("position", position);
        context.startActivity(intent);
    }
}
