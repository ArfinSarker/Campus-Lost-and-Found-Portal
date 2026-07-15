package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.AppBarLayout;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private LinearLayout llEmptyState;
    private ImageButton btnMarkAllRead;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String resolvedUserId;
    private boolean isFetching = false;
    private boolean isAdminMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        resolvedUserId = prefs.getString("universityId", null);
        isAdminMode = getIntent().getBooleanExtra("isAdminMode", false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvNotifications = findViewById(R.id.rvNotifications);
        llEmptyState = findViewById(R.id.llEmptyState);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        android.widget.TextView tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        if (tvToolbarTitle != null) {
            tvToolbarTitle.setText(isAdminMode ? "Admin Notifications" : "Notifications");
        }

        android.widget.TextView tvEmptyStateDesc = findViewById(R.id.tvEmptyStateDesc);
        if (tvEmptyStateDesc != null && isAdminMode) {
            tvEmptyStateDesc.setText("We'll notify you here when new user reports or admin requests arrive.");
        }
        
        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout, toolbar);
        }
        
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList, 
            notification -> {
                // Instantly update UI for responsiveness
                if (!notification.isRead()) {
                    notification.setRead(true);
                    int index = notificationList.indexOf(notification);
                    if (index != -1) {
                        adapter.notifyItemChanged(index);
                    }
                    // Update Mark All Read button visibility
                    checkUnreadStatus();
                    
                    // Mark as read in DB
                    Map<String, Object> update = new HashMap<>();
                    update.put("is_read", true);
                    SupabaseDatabaseHelper.update("notifications", "id=eq." + notification.getId(), update, new SupabaseDatabaseHelper.DatabaseCallback<>() {
                        @Override public void onSuccess(String result) {
                            Log.d("Notifications", "Marked as read in DB: " + notification.getId());
                        }
                        @Override public void onFailure(String e) {
                            Log.e("Notifications", "Failed to mark as read in DB: " + e);
                            // On failure, we revert the local state to match DB
                            notification.setRead(false);
                            int revertIndex = notificationList.indexOf(notification);
                            if (revertIndex != -1) {
                                adapter.notifyItemChanged(revertIndex);
                            }
                            checkUnreadStatus();
                            SnackbarManager.show(SnackbarManager.Type.ERROR, "Sync failed: status not saved");
                        }
                    });
                }
                
                if ("admin_report".equals(notification.getType())) {
                    Intent intent = new Intent(this, AdminReportDetailsActivity.class);
                    intent.putExtra("reportId", notification.getItemId());
                    startActivity(intent);
                } else if ("admin_report_new".equals(notification.getType())) {
                    Intent intent = new Intent(this, AdminReportReviewActivity.class);
                    intent.putExtra("reportId", notification.getItemId());
                    startActivity(intent);
                } else if ("admin_request".equals(notification.getType())) {
                    Intent intent = new Intent(this, AdminRequestsActivity.class);
                    startActivity(intent);
                } else if ("item_claimed".equals(notification.getType()) || "item_return".equals(notification.getType())) {
                    Intent intent = new Intent(this, ClaimDetailsActivity.class);
                    intent.putExtra("senderId", notification.getSenderId());
                    intent.putExtra("claimerId", notification.getClaimerId());
                    intent.putExtra("senderName", notification.getSenderName());
                    intent.putExtra("senderPhone", notification.getSenderPhone());
                    intent.putExtra("senderEmail", notification.getSenderEmail());
                    intent.putExtra("itemId", notification.getItemId());
                    intent.putExtra("itemName", notification.getItemName());
                    intent.putExtra("type", notification.getType());
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(this, ClaimDetailsActivity.class);
                    intent.putExtra("senderId", notification.getSenderId());
                    intent.putExtra("claimerId", notification.getClaimerId());
                    intent.putExtra("senderName", notification.getSenderName());
                    intent.putExtra("senderPhone", notification.getSenderPhone());
                    intent.putExtra("senderEmail", notification.getSenderEmail());
                    intent.putExtra("itemId", notification.getItemId());
                    intent.putExtra("itemName", notification.getItemName());
                    intent.putExtra("additionalDetails", notification.getAdditionalDetails());
                    intent.putExtra("type", notification.getType());
                    startActivity(intent);
                }
            },
            this::showDeleteConfirmation
        );

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());

        setupSwipeRefresh();
        
        if (resolvedUserId != null) {
            fetchNotifications(resolvedUserId);
        } else {
            resolveUserAndFetchNotifications();
        }
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(() -> {
                if (resolvedUserId != null) fetchNotifications(resolvedUserId);
                else resolveUserAndFetchNotifications();
            });
        }
    }

    private void showDeleteConfirmation(Notification notification) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notification")
                .setMessage("Are you sure you want to delete this notification?")
                .setPositiveButton("Delete", (dialog, which) -> SupabaseDatabaseHelper.delete("notifications", "id=eq." + notification.getId(), new SupabaseDatabaseHelper.DatabaseCallback<>() {
                    @Override
                    public void onSuccess(Void result) {
                        SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Notification deleted successfully");
                        if (resolvedUserId != null) fetchNotifications(resolvedUserId);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to delete notification");
                    }
                }))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void resolveUserAndFetchNotifications() {
        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        String authId = prefs.getString("authId", null);
        if (authId == null) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }

        SupabaseDatabaseHelper.select("profiles", "auth_id=eq." + authId + "&limit=1", new TypeToken<List<Map<String, String>>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Map<String, String>>>() {
            @Override
            public void onSuccess(List<Map<String, String>> result) {
                if (result != null && !result.isEmpty()) {
                    resolvedUserId = result.get(0).get("university_id");
                    if (resolvedUserId != null) {
                        fetchNotifications(resolvedUserId);
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void fetchNotifications(String userId) {
        if (isFetching) return;
        isFetching = true;

        // Query by recipient_id (University ID) as used in the DB
        String typeFilter;
        if (isAdminMode) {
            typeFilter = "type=in.(admin_report_new,admin_request)";
        } else {
            typeFilter = "type=in.(lost_item,found_item,item_claimed,item_return,admin_report)";
        }
        
        SupabaseDatabaseHelper.select("notifications", "recipient_id=eq." + userId + "&" + typeFilter, new TypeToken<List<Notification>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Notification>>() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                final List<String> senderIds = new ArrayList<>();
                if (notifications != null) {
                    for (Notification n : notifications) {
                        String sId = n.getSenderId();
                        if (sId != null && !sId.trim().isEmpty() && !senderIds.contains(sId)) {
                            senderIds.add(sId);
                        }
                    }
                }
                
                if (!senderIds.isEmpty()) {
                    StringBuilder filterBuilder = new StringBuilder("university_id=in.(");
                    for (int i = 0; i < senderIds.size(); i++) {
                        filterBuilder.append(senderIds.get(i));
                        if (i < senderIds.size() - 1) {
                            filterBuilder.append(",");
                        }
                    }
                    filterBuilder.append(")");
                    
                    SupabaseDatabaseHelper.select("profiles", filterBuilder.toString(), new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
                        @Override
                        public void onSuccess(List<User> users) {
                            java.util.Map<String, String> profileImageMap = new java.util.HashMap<>();
                            if (users != null) {
                                for (User u : users) {
                                    if (u.getUniversityId() != null) {
                                        profileImageMap.put(u.getUniversityId(), u.getProfileImageUrl());
                                    }
                                }
                            }
                            
                            if (notifications != null) {
                                for (Notification n : notifications) {
                                    String sId = n.getSenderId();
                                    String imgUrl = profileImageMap.get(sId);
                                    if (imgUrl != null && !imgUrl.trim().isEmpty() && !"null".equalsIgnoreCase(imgUrl.trim())) {
                                        n.setSenderImageUrl(imgUrl);
                                    }
                                }
                            }
                            displayLoadedNotifications(notifications);
                        }
                        
                        @Override
                        public void onFailure(String e) {
                            displayLoadedNotifications(notifications);
                        }
                    });
                } else {
                    displayLoadedNotifications(notifications);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                isFetching = false;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void displayLoadedNotifications(List<Notification> notifications) {
        List<Notification> temp = new ArrayList<>();
        if (notifications != null) {
            temp.addAll(notifications);
        }
        Collections.sort(temp, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
        
        adapter.updateNotifications(temp);
        
        llEmptyState.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
        rvNotifications.setVisibility(notificationList.isEmpty() ? View.GONE : View.VISIBLE);
        checkUnreadStatus();
        
        isFetching = false;
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
    }

    private void checkUnreadStatus() {
        boolean hasUnread = false;
        for (Notification n : notificationList) {
            if (!n.isRead()) {
                hasUnread = true;
                break;
            }
        }
        btnMarkAllRead.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
    }

    private void markAllAsRead() {
        if (resolvedUserId == null) return;

        // Keep local reference to unread notifications to revert if necessary
        List<Notification> unreadBefore = new ArrayList<>();
        for (Notification n : notificationList) {
            if (!n.isRead()) {
                unreadBefore.add(n);
                n.setRead(true);
            }
        }
        
        if (unreadBefore.isEmpty()) return;

        adapter.notifyItemRangeChanged(0, notificationList.size());
        btnMarkAllRead.setVisibility(View.GONE);
        SnackbarManager.show(SnackbarManager.Type.SUCCESS, "All marked as read");

        Map<String, Object> updates = new HashMap<>();
        updates.put("is_read", true);

        String typeFilter;
        if (isAdminMode) {
            typeFilter = "type=in.(admin_report_new,admin_request)";
        } else {
            typeFilter = "type=in.(lost_item,found_item,item_claimed,item_return,admin_report)";
        }

        // Perform bulk update in background - filter by recipient, types, and only those not yet read
        SupabaseDatabaseHelper.update("notifications", "recipient_id=eq." + resolvedUserId + "&is_read=eq.false&" + typeFilter, updates, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d("Notifications", "All marked as read in DB successfully");
                // Optional: Refresh to be sure
                fetchNotifications(resolvedUserId);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("Notifications", "Failed bulk update: " + errorMessage);
                // On failure, refresh the list to show true DB state
                fetchNotifications(resolvedUserId);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Sync failed: " + errorMessage);
            }
        });
    }
}
