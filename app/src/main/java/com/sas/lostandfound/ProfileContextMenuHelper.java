package com.sas.lostandfound;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileContextMenuHelper {

    public interface Callback {
        void onBlockStatusChanged(boolean isBlocked);
    }

    public static void show(Context context, View anchor, String currentUserId, String otherUserId, String otherUserName, boolean isBlocked, boolean isBlockedByOther, Callback callback) {
        PopupMenu popup = new PopupMenu(context, anchor);
        popup.getMenu().add(0, 1, 0, "View Profile");
        
        String blockOption = isBlocked ? "Unblock User" : "Block User";
        popup.getMenu().add(0, 2, 1, blockOption);
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                if (isBlockedByOther) {
                    new AlertDialog.Builder(context)
                            .setTitle("Profile Unavailable")
                            .setMessage("This profile is not available right now.")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    openOtherUserProfile(context, otherUserId, otherUserName);
                }
                return true;
            } else if (itemId == 2) {
                toggleUserBlock(context, currentUserId, otherUserId, isBlocked, callback);
                return true;
            }
            return false;
        });
        popup.show();
    }

    public static void toggleUserBlock(Context context, String currentUserId, String otherUserId, boolean isBlocked, Callback callback) {
        if (isBlocked) {
            confirmUnblockUser(context, currentUserId, otherUserId, callback);
        } else {
            confirmBlockUser(context, currentUserId, otherUserId, callback);
        }
    }

    private static void openOtherUserProfile(Context context, String otherUserId, String otherUserName) {
        if (otherUserId == null) {
            SnackbarManager.show(SnackbarManager.Type.ERROR, "User details not loaded yet.");
            return;
        }
        Intent intent = new Intent(context, UserProfileActivity.class);
        intent.putExtra("targetUserId", otherUserId);
        intent.putExtra("isAdminViewing", true);
        intent.putExtra("isViewOnly", true);
        intent.putExtra("intentFullName", otherUserName);
        context.startActivity(intent);
    }

    private static void confirmBlockUser(Context context, String currentUserId, String otherUserId, Callback callback) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Block this user?")
                .setMessage("You won't receive messages, calls, or notifications from this user until you unblock them. You can unblock them at any time.")
                .setPositiveButton("Block", (d, which) -> executeBlockUser(context, currentUserId, otherUserId, callback))
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(d -> {
            boolean isNightMode = (context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            int positiveColor = isNightMode ? android.graphics.Color.parseColor("#FCA5A5") : android.graphics.Color.parseColor("#DC2626");
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(positiveColor);
            int negativeColor = isNightMode ? android.graphics.Color.parseColor("#94A3B8") : android.graphics.Color.parseColor("#64748B");
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(negativeColor);
        });
        dialog.show();
    }

    private static void executeBlockUser(Context context, String currentUserId, String otherUserId, Callback callback) {
        if (currentUserId == null || otherUserId == null) return;
        Map<String, String> blockData = new HashMap<>();
        blockData.put("blocker_id", currentUserId);
        blockData.put("blocked_id", otherUserId);
        
        SupabaseDatabaseHelper.insert("blocked_users", blockData, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "User blocked successfully.");
                UnreadBadgeHelper.sendBadgeUpdateBroadcast(context);
                if (callback != null) {
                    callback.onBlockStatusChanged(true);
                }
            }
            
            @Override
            public void onFailure(String error) {
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to block user: " + error);
            }
        });
    }

    private static void confirmUnblockUser(Context context, String currentUserId, String otherUserId, Callback callback) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Unblock this user?")
                .setMessage("You'll be able to send and receive messages again.")
                .setPositiveButton("Unblock", (d, which) -> executeUnblockUser(context, currentUserId, otherUserId, callback))
                .setNegativeButton("Cancel", null)
                .create();
        dialog.setOnShowListener(d -> {
            boolean isNightMode = (context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            int positiveColor = isNightMode ? android.graphics.Color.parseColor("#34D399") : android.graphics.Color.parseColor("#10B981");
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(positiveColor);
            int negativeColor = isNightMode ? android.graphics.Color.parseColor("#94A3B8") : android.graphics.Color.parseColor("#64748B");
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(negativeColor);
        });
        dialog.show();
    }

    private static void executeUnblockUser(Context context, String currentUserId, String otherUserId, Callback callback) {
        if (currentUserId == null || otherUserId == null) return;
        String query = "blocker_id=eq." + currentUserId + "&blocked_id=eq." + otherUserId;
        SupabaseDatabaseHelper.delete("blocked_users", query, new SupabaseDatabaseHelper.DatabaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "User unblocked successfully.");
                UnreadBadgeHelper.sendBadgeUpdateBroadcast(context);
                if (callback != null) {
                    callback.onBlockStatusChanged(false);
                }
            }
            
            @Override
            public void onFailure(String error) {
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to unblock user: " + error);
            }
        });
    }

    public static void checkBlockStatusAndShowMenu(Context context, View anchor, String currentUserId, String otherUserId, String otherUserName, Callback callback) {
        if (currentUserId == null || otherUserId == null) return;
        
        String query = "blocker_id=in.(" + currentUserId + "," + otherUserId + ")&blocked_id=in.(" + currentUserId + "," + otherUserId + ")";
        SupabaseDatabaseHelper.select("blocked_users", query, new TypeToken<List<ChatActivity.BlockedRecord>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<ChatActivity.BlockedRecord>>() {
            @Override
            public void onSuccess(List<ChatActivity.BlockedRecord> result) {
                boolean isBlockedByMe = false;
                boolean isBlockedByOther = false;
                if (result != null) {
                    for (ChatActivity.BlockedRecord record : result) {
                        if (currentUserId.equals(record.getBlockerId()) && otherUserId.equals(record.getBlockedId())) {
                            isBlockedByMe = true;
                        }
                        if (otherUserId.equals(record.getBlockerId()) && currentUserId.equals(record.getBlockedId())) {
                            isBlockedByOther = true;
                        }
                    }
                }
                show(context, anchor, currentUserId, otherUserId, otherUserName, isBlockedByMe, isBlockedByOther, callback);
            }
            
            @Override
            public void onFailure(String error) {
                show(context, anchor, currentUserId, otherUserId, otherUserName, false, false, callback);
            }
        });
    }
}
