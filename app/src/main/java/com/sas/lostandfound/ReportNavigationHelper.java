package com.sas.lostandfound;

import com.google.gson.reflect.TypeToken;
import java.util.List;

/**
 * Centralized helper for resolving related report IDs across different tables.
 */
public class ReportNavigationHelper {

    public interface ResolutionCallback {
        /**
         * Called when a report is found.
         * @param report The found report object (Item or AdminReport).
         */
        void onResolved(Object report);
        
        /**
         * Called when an error occurs during resolution.
         */
        void onError(String message);
        
        /**
         * Called when no report is found with the given ID.
         */
        void onNotFound();
    }

    /**
     * Resolves a display ID (like #F1, L2, #R3) to its corresponding report object.
     */
    public static void resolve(String displayId, ResolutionCallback callback) {
        if (displayId == null || displayId.trim().isEmpty()) {
            callback.onError("Report ID is empty");
            return;
        }

        String rawId = ReportIdFormatter.getRawId(displayId);
        String prefix = ReportIdFormatter.getPrefix(displayId);
        
        // Normalize rawId to have uppercase prefix for consistent database matching (e.g. f1 -> F1)
        if (!rawId.isEmpty()) {
            rawId = prefix + rawId.substring(1);
        }
        
        final String finalRawId = rawId;

        if ("F".equals(prefix)) {
            fetchItem("found_reports", finalRawId, "found", callback);
        } else if ("L".equals(prefix)) {
            fetchItem("lost_reports", finalRawId, "lost", callback);
        } else if ("R".equals(prefix)) {
            fetchAdminReport(finalRawId, callback);
        } else {
            // Fallback: Try all tables sequentially
            fetchItem("found_reports", finalRawId, "found", new ResolutionCallback() {
                @Override public void onResolved(Object r) { callback.onResolved(r); }
                @Override public void onError(String m) { tryLost(); }
                @Override public void onNotFound() { tryLost(); }
                
                private void tryLost() {
                    fetchItem("lost_reports", finalRawId, "lost", new ResolutionCallback() {
                        @Override public void onResolved(Object r) { callback.onResolved(r); }
                        @Override public void onError(String m) { tryAdmin(); }
                        @Override public void onNotFound() { tryAdmin(); }
                        
                        private void tryAdmin() {
                            fetchAdminReport(finalRawId, callback);
                        }
                    });
                }
            });
        }
    }

    private static void fetchItem(String table, String displayId, String type, ResolutionCallback callback) {
        SupabaseDatabaseHelper.select(table, "display_id=eq." + displayId + "&limit=1", 
                new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> items) {
                if (items != null && !items.isEmpty()) {
                    Item item = items.get(0);
                    item.setType(type); // Ensure type is set for navigation
                    callback.onResolved(item);
                } else {
                    callback.onNotFound();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    private static void fetchAdminReport(String displayId, ResolutionCallback callback) {
        SupabaseDatabaseHelper.select("admin_reports", "display_id=eq." + displayId + "&limit=1", 
                new TypeToken<List<AdminReport>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<AdminReport>>() {
            @Override
            public void onSuccess(List<AdminReport> reports) {
                if (reports != null && !reports.isEmpty()) {
                    callback.onResolved(reports.get(0));
                } else {
                    callback.onNotFound();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }
}
