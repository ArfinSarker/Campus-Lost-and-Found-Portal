package com.sas.lostandfound;

import android.content.Context;
import android.content.Intent;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to handle unified navigation for Lost/Found items.
 * Ensures consistent behavior across the application.
 */
public class ItemNavigationUtils {

    /**
     * Navigates to the ItemDetailActivity with all necessary item data.
     * This is the intended destination for any tap on a report card.
     */
    public static void navigateToDetail(Context context, Item item) {
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
        // Pass the displayId/reportId if available
        intent.putExtra("itemReportId", item.getDisplayId());
        
        context.startActivity(intent);
    }

    /**
     * Opens the FullScreenImageActivity to view images in full screen.
     * This should typically only be called from the ItemDetailActivity.
     */
    public static void openFullScreenImage(Context context, List<String> imageUrls, int position) {
        if (context == null || imageUrls == null || imageUrls.isEmpty()) return;
        
        Intent intent = new Intent(context, FullScreenImageActivity.class);
        intent.putStringArrayListExtra("imageUrls", new ArrayList<>(imageUrls));
        intent.putExtra("position", position);
        context.startActivity(intent);
    }
}
