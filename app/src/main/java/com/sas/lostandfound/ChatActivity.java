package com.sas.lostandfound;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ChatActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView ivHeaderAvatar;
    private TextView tvHeaderName, tvHeaderStatus;
    private View llHeaderTitle, btnSettings;
    private RecyclerView rvMessages;
    private EditText etMessageInput;
    private MaterialCardView btnSendMessageCard;

    private View layoutInputArea, layoutRequestActions;
    private com.google.android.material.button.MaterialButton btnAcceptRequest, btnRejectRequest;
    private String requestStatus;
    private String requestSenderId;
    private String initialMessage;
    private String requestCreatedAt;

    private String conversationId, reportId, itemName;
    String otherUserId, otherUserName;
    String currentUnivId;
    boolean isOtherUserBlockedByMe = false;
    boolean amIBlockedByOtherUser = false;
    private int defaultHeaderStatusColor;

    private List<Message> messageList = new ArrayList<>();
    private MessagesAdapter adapter;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadMessages(false);
            if (!"accepted".equals(requestStatus)) {
                queryRequestStatus();
            }
            queryOtherUserProfile();
            SupabaseDatabaseHelper.updateUserActivityStatus();
            pollHandler.postDelayed(this, 3000); // Poll every 3 seconds
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUnivId = prefs.getString("universityId", null);

        conversationId = getIntent().getStringExtra("conversationId");
        otherUserId = getIntent().getStringExtra("otherUserId");
        otherUserName = getIntent().getStringExtra("otherUserName");
        reportId = getIntent().getStringExtra("reportId");
        itemName = getIntent().getStringExtra("itemName");

        initializeViews();
        setupToolbar();
        
        if (tvHeaderStatus != null) {
            defaultHeaderStatusColor = tvHeaderStatus.getCurrentTextColor();
        }

        setupRecyclerView();

        btnSendMessageCard.setOnClickListener(v -> sendMessage());
        btnSettings.setOnClickListener(v -> showSettingsMenu(v));
        llHeaderTitle.setOnClickListener(v -> showProfileContextMenu(v));

        requestStatus = getIntent().getStringExtra("requestStatus");
        if (requestStatus == null) requestStatus = "accepted";
        requestSenderId = getIntent().getStringExtra("requestSenderId");
        initialMessage = getIntent().getStringExtra("initialMessage");
        requestCreatedAt = getIntent().getStringExtra("requestCreatedAt");

        updateRequestLayoutUI();

        loadMessages(true);
        if ("accepted".equals(requestStatus)) {
            markMessagesAsRead();
        }
        
        loadMissingDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        pollHandler.post(pollRunnable);
        checkBlockStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        ivHeaderAvatar = findViewById(R.id.ivHeaderAvatar);
        tvHeaderName = findViewById(R.id.tvHeaderName);
        tvHeaderStatus = findViewById(R.id.tvHeaderStatus);
        llHeaderTitle = findViewById(R.id.llHeaderTitle);
        btnSettings = findViewById(R.id.btnSettings);
        rvMessages = findViewById(R.id.rvMessages);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSendMessageCard = findViewById(R.id.btnSendMessageCard);
        
        layoutInputArea = findViewById(R.id.layoutInputArea);
        layoutRequestActions = findViewById(R.id.layoutRequestActions);
        btnAcceptRequest = findViewById(R.id.btnAcceptRequest);
        btnRejectRequest = findViewById(R.id.btnRejectRequest);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvHeaderName.setText(otherUserName != null ? otherUserName : "Chat");
        
        // Prevent layout coloring utility from centering tvHeaderName and tvHeaderStatus
        tvHeaderName.setTag("centered");
        if (tvHeaderStatus != null) {
            tvHeaderStatus.setTag("centered");
        }

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout, toolbar);
        }
    }

    private void setupRecyclerView() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Always show last messages first
        rvMessages.setLayoutManager(layoutManager);
        adapter = new MessagesAdapter(messageList, currentUnivId, this);
        adapter.setRequestStatus(requestStatus);
        rvMessages.setAdapter(adapter);
    }

    private void loadMissingDetails() {
        if (conversationId == null) return;

        // 1. Fetch reportId if missing
        if (reportId == null) {
            String query = "id=eq." + conversationId + "&limit=1";
            SupabaseDatabaseHelper.select("conversations", query, new TypeToken<List<ConversationRecord>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<ConversationRecord>>() {
                @Override
                public void onSuccess(List<ConversationRecord> list) {
                    if (list != null && !list.isEmpty()) {
                        reportId = list.get(0).getReportId();
                        queryItemName();
                    }
                }
                @Override public void onFailure(String error) {}
            });
        } else if (itemName == null) {
            queryItemName();
        }

        // 2. Fetch otherUserId if missing
        if (otherUserId == null) {
            String query = "conversation_id=eq." + conversationId + "&university_id=neq." + currentUnivId + "&limit=1";
            SupabaseDatabaseHelper.select("conversation_participants", query, new TypeToken<List<Participant>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Participant>>() {
                @Override
                public void onSuccess(List<Participant> list) {
                    if (list != null && !list.isEmpty()) {
                        otherUserId = list.get(0).getUniversityId();
                        queryOtherUserProfile();
                    }
                }
                @Override public void onFailure(String error) {}
            });
        } else {
            queryOtherUserProfile();
        }
    }

    private void queryItemName() {
        if (reportId == null) return;
        String query = "id=eq." + reportId + "&limit=1";
        SupabaseDatabaseHelper.select("reports", query, new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> items) {
                if (items != null && !items.isEmpty()) {
                    itemName = items.get(0).getName();
                }
            }
            @Override public void onFailure(String error) {}
        });
    }

    private void queryOtherUserProfile() {
        if (otherUserId == null) return;

        String query = "university_id=eq." + otherUserId + "&limit=1";
        SupabaseDatabaseHelper.select("profiles", query, new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users != null && !users.isEmpty()) {
                    User user = users.get(0);
                    otherUserName = user.getFullName() != null ? user.getFullName() : user.getName();
                    tvHeaderName.setText(otherUserName);
                    
                    String imgUrl = user.getProfileImageUrl();
                    if (imgUrl != null && !imgUrl.isEmpty()) {
                        GlideApp.with(ChatActivity.this)
                                .load(imgUrl)
                                .placeholder(R.drawable.ic_user)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .circleCrop()
                                .into(ivHeaderAvatar);
                    }
                    if (adapter != null) {
                        adapter.setOtherUserProfileImageUrl(imgUrl);
                    }

                    if (tvHeaderStatus != null) {
                        if ("accepted".equals(requestStatus) && !isOtherUserBlockedByMe && !amIBlockedByOtherUser) {
                            String lastActiveIso = user.getLastActiveAt();
                            String statusText = formatLastActiveStatus(lastActiveIso);
                            if (!android.text.TextUtils.isEmpty(statusText)) {
                                tvHeaderStatus.setText(statusText);
                                if ("Active now".equals(statusText)) {
                                    tvHeaderStatus.setTextColor(androidx.core.content.ContextCompat.getColor(ChatActivity.this, R.color.statusFound));
                                } else {
                                    tvHeaderStatus.setTextColor(defaultHeaderStatusColor);
                                }
                                tvHeaderStatus.setVisibility(View.VISIBLE);
                            } else {
                                tvHeaderStatus.setVisibility(View.GONE);
                            }
                        } else {
                            tvHeaderStatus.setVisibility(View.GONE);
                        }
                    }
                }
                checkBlockStatus();
            }
            @Override
            public void onFailure(String error) {
                checkBlockStatus();
            }
        });
    }

    private String formatLastActiveStatus(String lastActiveIso) {
        if (lastActiveIso == null || lastActiveIso.isEmpty()) {
            return "";
        }
        try {
            Date lastActiveDate = ValidationUtils.parseIso8601(lastActiveIso);
            if (lastActiveDate == null) return "";
            
            long diffMs = System.currentTimeMillis() - lastActiveDate.getTime();
            if (diffMs < 0) diffMs = 0; // Prevent future time anomalies
            
            long diffMinutes = diffMs / (60 * 1000);
            
            // 2 minutes threshold for "Active now"
            if (diffMinutes < 2) {
                return "Active now";
            }
            
            if (diffMinutes < 60) {
                return "Last active " + diffMinutes + " minutes ago";
            }
            
            // Check if today or yesterday
            java.util.Calendar nowCal = java.util.Calendar.getInstance();
            java.util.Calendar activeCal = java.util.Calendar.getInstance();
            activeCal.setTime(lastActiveDate);
            
            boolean isToday = nowCal.get(java.util.Calendar.YEAR) == activeCal.get(java.util.Calendar.YEAR) &&
                              nowCal.get(java.util.Calendar.DAY_OF_YEAR) == activeCal.get(java.util.Calendar.DAY_OF_YEAR);
                              
            // check yesterday
            java.util.Calendar yesterdayCal = java.util.Calendar.getInstance();
            yesterdayCal.add(java.util.Calendar.DAY_OF_YEAR, -1);
            boolean isYesterday = yesterdayCal.get(java.util.Calendar.YEAR) == activeCal.get(java.util.Calendar.YEAR) &&
                                  yesterdayCal.get(java.util.Calendar.DAY_OF_YEAR) == activeCal.get(java.util.Calendar.DAY_OF_YEAR);
                                  
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            
            if (isToday) {
                return "Last active today at " + timeFormat.format(lastActiveDate);
            } else if (isYesterday) {
                return "Last active yesterday";
            } else {
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                return "Last active on " + dateFormat.format(lastActiveDate);
            }
        } catch (Exception e) {
            return "";
        }
    }

    private void checkBlockStatus() {
        if (currentUnivId == null || otherUserId == null) return;
        
        // Check if other user is blocked by me
        String queryMe = "blocker_id=eq." + currentUnivId + "&blocked_id=eq." + otherUserId;
        SupabaseDatabaseHelper.select("blocked_users", queryMe, new TypeToken<List<BlockedRecord>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<BlockedRecord>>() {
            @Override
            public void onSuccess(List<BlockedRecord> result) {
                isOtherUserBlockedByMe = (result != null && !result.isEmpty());
                
                // Now check if I am blocked by the other user
                String queryOther = "blocker_id=eq." + otherUserId + "&blocked_id=eq." + currentUnivId;
                SupabaseDatabaseHelper.select("blocked_users", queryOther, new TypeToken<List<BlockedRecord>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<BlockedRecord>>() {
                    @Override
                    public void onSuccess(List<BlockedRecord> result2) {
                        amIBlockedByOtherUser = (result2 != null && !result2.isEmpty());
                        updateChatMessagingUI();
                    }
                    @Override
                    public void onFailure(String error) {
                        updateChatMessagingUI();
                    }
                });
            }
            @Override
            public void onFailure(String error) {
                updateChatMessagingUI();
            }
        });
    }

    private void updateChatMessagingUI() {
        int horizontalPadding = (int) (16 * getResources().getDisplayMetrics().density);
        int verticalPadding = (int) (10 * getResources().getDisplayMetrics().density);

        if (isOtherUserBlockedByMe) {
            etMessageInput.setEnabled(false);
            etMessageInput.setSingleLine(true);
            etMessageInput.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            etMessageInput.setText("");
            etMessageInput.setHint("Unblock this user to send messages");
            btnSendMessageCard.setEnabled(false);
            btnSendMessageCard.setCardBackgroundColor(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(ChatActivity.this, R.color.chat_disabled_send_bg)));
            if (tvHeaderStatus != null) tvHeaderStatus.setVisibility(View.GONE);
        } else if (amIBlockedByOtherUser) {
            etMessageInput.setEnabled(false);
            etMessageInput.setSingleLine(true);
            etMessageInput.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            etMessageInput.setText("");
            etMessageInput.setHint("You cannot send messages to this user");
            btnSendMessageCard.setEnabled(false);
            btnSendMessageCard.setCardBackgroundColor(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(ChatActivity.this, R.color.chat_disabled_send_bg)));
            if (tvHeaderStatus != null) tvHeaderStatus.setVisibility(View.GONE);
        } else {
            etMessageInput.setEnabled(true);
            etMessageInput.setSingleLine(false);
            etMessageInput.setMaxLines(4);
            etMessageInput.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            etMessageInput.setHint(getString(R.string.hint_type_message));
            btnSendMessageCard.setEnabled(true);
            btnSendMessageCard.setCardBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.primaryColor)));
        }
    }

    private void toggleUserBlock() {
        if (currentUnivId == null || otherUserId == null) return;
        ProfileContextMenuHelper.toggleUserBlock(this, currentUnivId, otherUserId, isOtherUserBlockedByMe, isBlocked -> {
            isOtherUserBlockedByMe = isBlocked;
            updateChatMessagingUI();
            queryOtherUserProfile();
        });
    }

    private void showSettingsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "User Profile");
        popup.getMenu().add(0, 2, 1, "Report Details");
        
        String blockOption = isOtherUserBlockedByMe ? "Unblock User" : "Block User";
        popup.getMenu().add(0, 3, 2, blockOption);
        
        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == 1) {
                openOtherUserProfile();
                return true;
            } else if (itemId == 2) {
                queryAndOpenReportDetails();
                return true;
            } else if (itemId == 3) {
                toggleUserBlock();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showProfileContextMenu(View anchor) {
        View stableAnchor = anchor;
        View dummy = findViewById(R.id.popupAnchorDummy);
        if (dummy != null) {
            int[] loc = new int[2];
            anchor.getLocationInWindow(loc);
            dummy.setX(loc[0]);
            dummy.setY(loc[1]);
            stableAnchor = dummy;
        }
        ProfileContextMenuHelper.show(this, stableAnchor, currentUnivId, otherUserId, otherUserName, isOtherUserBlockedByMe, amIBlockedByOtherUser, isBlocked -> {
            isOtherUserBlockedByMe = isBlocked;
            updateChatMessagingUI();
            queryOtherUserProfile();
        });
    }

    private void openOtherUserProfile() {
        if (otherUserId == null) {
            SnackbarManager.show(SnackbarManager.Type.ERROR, "User details not loaded yet.");
            return;
        }
        if (amIBlockedByOtherUser) {
            new AlertDialog.Builder(this)
                    .setTitle("Profile Unavailable")
                    .setMessage("This profile is not available right now.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        Intent intent = new Intent(ChatActivity.this, UserProfileActivity.class);
        intent.putExtra("targetUserId", otherUserId);
        intent.putExtra("isAdminViewing", true);
        intent.putExtra("isViewOnly", true);
        intent.putExtra("intentFullName", otherUserName);
        startActivity(intent);
    }

    private void loadMessages(boolean forceScroll) {
        if (conversationId == null) return;

        String query = "conversation_id=eq." + conversationId + "&order=created_at.asc";
        SupabaseDatabaseHelper.select("messages", query, new TypeToken<List<Message>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Message>>() {
            @Override
            public void onSuccess(List<Message> messages) {
                if (messages == null) return;
                
                int oldSize = messageList.size();
                int newSize = messages.size();

                if (newSize > oldSize || areMessageListsDifferent(messageList, messages)) {
                    messageList.clear();
                    messageList.addAll(messages);
                    adapter.setMessages(messageList);
                    
                    if (forceScroll || oldSize == 0) {
                        rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                    } else {
                        rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
                    }
                    if ("accepted".equals(requestStatus)) {
                        markMessagesAsRead();
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {}
        });
    }

    private boolean areMessageListsDifferent(List<Message> list1, List<Message> list2) {
        if (list1 == null || list2 == null) return true;
        if (list1.size() != list2.size()) return true;
        for (int i = 0; i < list1.size(); i++) {
            Message m1 = list1.get(i);
            Message m2 = list2.get(i);
            if (m1 == null || m2 == null) return true;
            if (m1.isRead() != m2.isRead() || m1.isDelivered() != m2.isDelivered()) {
                return true;
            }
        }
        return false;
    }

    private void markMessagesAsRead() {
        if (conversationId == null || currentUnivId == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("is_read", true);
        data.put("is_delivered", true);

        String query = "conversation_id=eq." + conversationId + "&sender_id=neq." + currentUnivId + "&is_read=eq.false";
        SupabaseDatabaseHelper.update("messages", query, data, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override public void onSuccess(String result) {
                UnreadBadgeHelper.sendBadgeUpdateBroadcast(ChatActivity.this);
            }
            @Override public void onFailure(String errorMessage) {}
        });

        // Mark corresponding notifications of type 'chat_message' as read
        Map<String, Object> notifData = new HashMap<>();
        notifData.put("is_read", true);
        String notifQuery = "recipient_id=eq." + currentUnivId + "&type=eq.chat_message&additional_details=eq." + conversationId + "&is_read=eq.false";
        SupabaseDatabaseHelper.update("notifications", notifQuery, notifData, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override public void onSuccess(String result) {}
            @Override public void onFailure(String errorMessage) {}
        });
    }

    private void sendMessage() {
        String text = etMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;

        if (conversationId == null || currentUnivId == null) return;

        if (isOtherUserBlockedByMe) {
            SnackbarManager.show(SnackbarManager.Type.ERROR, "Unblock this user to send messages.");
            return;
        }
        if (amIBlockedByOtherUser) {
            SnackbarManager.show(SnackbarManager.Type.ERROR, "You cannot send messages to this user.");
            return;
        }

        etMessageInput.setText(""); // Clear input immediately
        Message msg = new Message(conversationId, currentUnivId, text);

        boolean isReceiver = currentUnivId != null && !currentUnivId.equals(requestSenderId);
        if (!"accepted".equals(requestStatus) && isReceiver) {
            requestStatus = "accepted";
            updateRequestLayoutUI();
        }

        SupabaseDatabaseHelper.insert("messages", msg, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                UnreadBadgeHelper.sendBadgeUpdateBroadcast(ChatActivity.this);
                loadMessages(false);
            }

            @Override
            public void onFailure(String errorMessage) {
                etMessageInput.setText(text);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to send: " + errorMessage);
            }
        });
    }

    private void acceptChatRequest() {
        if (conversationId == null) return;
        
        if (btnAcceptRequest != null) btnAcceptRequest.setEnabled(false);
        if (btnRejectRequest != null) btnRejectRequest.setEnabled(false);

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("p_request_id", java.util.UUID.fromString(conversationId));

        SupabaseDatabaseHelper.rpc("accept_chat_request", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    String activeConversationId = result.replace("\"", "").trim();
                    SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Request accepted!");

                    conversationId = activeConversationId;
                    requestStatus = "accepted";

                    updateRequestLayoutUI();
                    loadMessages(true);
                    markMessagesAsRead();

                    UnreadBadgeHelper.sendBadgeUpdateBroadcast(ChatActivity.this);
                } catch (Exception e) {
                    e.printStackTrace();
                    finish();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (btnAcceptRequest != null) btnAcceptRequest.setEnabled(true);
                if (btnRejectRequest != null) btnRejectRequest.setEnabled(true);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to accept request: " + errorMessage);
            }
        });
    }

    private void rejectChatRequest() {
        if (conversationId == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Decline Request")
                .setMessage("Are you sure you want to decline this chat request?")
                .setPositiveButton("Decline", (dialog, which) -> {
                    if (btnAcceptRequest != null) btnAcceptRequest.setEnabled(false);
                    if (btnRejectRequest != null) btnRejectRequest.setEnabled(false);

                    Map<String, Object> params = new java.util.HashMap<>();
                    params.put("p_request_id", java.util.UUID.fromString(conversationId));

                    SupabaseDatabaseHelper.rpc("reject_chat_request", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Request declined.");
                            requestStatus = "rejected";
                            updateRequestLayoutUI();

                            UnreadBadgeHelper.sendBadgeUpdateBroadcast(ChatActivity.this);
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            if (btnAcceptRequest != null) btnAcceptRequest.setEnabled(true);
                            if (btnRejectRequest != null) btnRejectRequest.setEnabled(true);
                            SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to decline request: " + errorMessage);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void queryRequestStatus() {
        if (reportId == null || otherUserId == null || currentUnivId == null) return;
        boolean isReceiver = !currentUnivId.equals(requestSenderId);
        String senderId = isReceiver ? otherUserId : currentUnivId;
        
        String query = "report_id=eq." + reportId + "&sender_id=eq." + senderId + "&limit=1";
        SupabaseDatabaseHelper.select("chat_requests", query, new TypeToken<List<ChatRequest>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<ChatRequest>>() {
            @Override
            public void onSuccess(List<ChatRequest> list) {
                if (list != null && !list.isEmpty()) {
                    String newStatus = list.get(0).getStatus();
                    if (newStatus != null && !newStatus.equals(requestStatus)) {
                        requestStatus = newStatus;
                        updateRequestLayoutUI();
                    }
                }
            }
            @Override public void onFailure(String error) {}
        });
    }

    private void updateRequestLayoutUI() {
        if (adapter != null) {
            adapter.setRequestStatus(requestStatus);
        }
        boolean isReceiver = currentUnivId != null && !currentUnivId.equals(requestSenderId);
        
        if (!"accepted".equals(requestStatus)) {
            if (isReceiver) {
                if ("pending".equals(requestStatus)) {
                    if (layoutInputArea != null) layoutInputArea.setVisibility(View.GONE);
                    if (layoutRequestActions != null) layoutRequestActions.setVisibility(View.VISIBLE);
                    if (btnAcceptRequest != null) btnAcceptRequest.setOnClickListener(v -> acceptChatRequest());
                    if (btnRejectRequest != null) btnRejectRequest.setOnClickListener(v -> rejectChatRequest());
                } else {
                    // rejected/declined
                    if (layoutRequestActions != null) layoutRequestActions.setVisibility(View.GONE);
                    if (layoutInputArea != null) layoutInputArea.setVisibility(View.VISIBLE);
                    updateChatMessagingUI();
                }
            } else {
                if (layoutInputArea != null) layoutInputArea.setVisibility(View.VISIBLE);
                if (layoutRequestActions != null) layoutRequestActions.setVisibility(View.GONE);
                updateChatMessagingUI();
            }
        } else {
            if (layoutInputArea != null) layoutInputArea.setVisibility(View.VISIBLE);
            if (layoutRequestActions != null) layoutRequestActions.setVisibility(View.GONE);
            updateChatMessagingUI();
        }
    }

    private void queryAndOpenReportDetails() {
        if (reportId == null) {
            SnackbarManager.show(SnackbarManager.Type.ERROR, "Report details not associated with this chat.");
            return;
        }

        SupabaseDatabaseHelper.select("reports", "id=eq." + reportId + "&limit=1", new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> reports) {
                if (reports != null && !reports.isEmpty()) {
                    Item item = reports.get(0);
                    Intent intent = new Intent(ChatActivity.this, ItemDetailActivity.class);
                    intent.putExtra("itemId", item.getId());
                    intent.putExtra("itemStatus", item.getType()); // "lost" or "found"
                    intent.putExtra("userId", item.getReporterId());
                    intent.putExtra("itemName", item.getName());
                    intent.putExtra("itemDescription", item.getDescription());
                    intent.putExtra("itemCategory", item.getCategory());
                    intent.putExtra("itemLocation", item.getLocation());
                    intent.putExtra("manualLocation", item.getManualLocation());
                    intent.putExtra("itemDate", item.getDate());
                    intent.putExtra("itemTime", item.getTime());
                    intent.putExtra("itemImageUrl", item.getImageUrl());
                    startActivity(intent);
                } else {
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "Report details could not be found.");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to fetch details: " + errorMessage);
            }
        });
    }

    // Message bubble Adapter
    private static class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_DATE_HEADER = 0;
        private static final int TYPE_MESSAGE_SENDER = 1;
        private static final int TYPE_MESSAGE_RECEIVER = 2;

        private final List<Message> list;
        private final List<Object> displayItems = new ArrayList<>();
        private final String currentUserId;
        private final Context context;
        private String otherUserProfileImageUrl;
        private String requestStatus;

        public MessagesAdapter(List<Message> list, String currentUserId, Context context) {
            this.list = list;
            this.currentUserId = currentUserId;
            this.context = context;
            rebuildDisplayItems();
        }

        public void setMessages(List<Message> newMessages) {
            if (newMessages != this.list) {
                this.list.clear();
                if (newMessages != null) {
                    this.list.addAll(newMessages);
                }
            }
            rebuildDisplayItems();
            notifyDataSetChanged();
        }

        public void setOtherUserProfileImageUrl(String url) {
            if (url == null && this.otherUserProfileImageUrl == null) return;
            if (url != null && url.equals(this.otherUserProfileImageUrl)) return;
            this.otherUserProfileImageUrl = url;
            notifyDataSetChanged();
        }

        public void setRequestStatus(String requestStatus) {
            if (requestStatus == null && this.requestStatus == null) return;
            if (requestStatus != null && requestStatus.equals(this.requestStatus)) return;
            this.requestStatus = requestStatus;
            notifyDataSetChanged();
        }

        private void rebuildDisplayItems() {
            displayItems.clear();
            if (list == null || list.isEmpty()) {
                return;
            }
            
            Date lastDate = null;
            for (Message msg : list) {
                Date msgDate = parseDate(msg.getCreatedAt());
                if (lastDate == null || !isSameDay(lastDate, msgDate)) {
                    displayItems.add(getDayLabel(msgDate));
                    lastDate = msgDate;
                }
                displayItems.add(msg);
            }
        }

        private static Date parseDate(String isoTime) {
            if (isoTime == null || isoTime.isEmpty()) {
                return new Date();
            }
            try {
                Date date = ValidationUtils.parseIso8601(isoTime);
                if (date != null) {
                    return date;
                }
            } catch (Exception ignored) {}
            return new Date();
        }

        private static boolean isSameDay(Date d1, Date d2) {
            if (d1 == null || d2 == null) return false;
            java.util.Calendar cal1 = java.util.Calendar.getInstance();
            cal1.setTime(d1);
            java.util.Calendar cal2 = java.util.Calendar.getInstance();
            cal2.setTime(d2);
            return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                   cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
        }

        private static String getDayLabel(Date date) {
            java.util.Calendar now = java.util.Calendar.getInstance();
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(date);
            
            if (now.get(java.util.Calendar.YEAR) == cal.get(java.util.Calendar.YEAR) &&
                now.get(java.util.Calendar.DAY_OF_YEAR) == cal.get(java.util.Calendar.DAY_OF_YEAR)) {
                return "Today";
            }
            
            java.util.Calendar yesterday = java.util.Calendar.getInstance();
            yesterday.add(java.util.Calendar.DAY_OF_YEAR, -1);
            if (yesterday.get(java.util.Calendar.YEAR) == cal.get(java.util.Calendar.YEAR) &&
                yesterday.get(java.util.Calendar.DAY_OF_YEAR) == cal.get(java.util.Calendar.DAY_OF_YEAR)) {
                return "Yesterday";
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());
            return sdf.format(date);
        }

        @Override
        public int getItemViewType(int position) {
            Object item = displayItems.get(position);
            if (item instanceof String) {
                return TYPE_DATE_HEADER;
            } else {
                Message msg = (Message) item;
                if (msg.getSenderId() != null && msg.getSenderId().equals(currentUserId)) {
                    return TYPE_MESSAGE_SENDER;
                } else {
                    return TYPE_MESSAGE_RECEIVER;
                }
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_DATE_HEADER) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_date_header, parent, false);
                return new DateHeaderViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
                return new MessageViewHolder(v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof DateHeaderViewHolder) {
                DateHeaderViewHolder dateHolder = (DateHeaderViewHolder) holder;
                String dateStr = (String) displayItems.get(position);
                dateHolder.tvDateHeader.setText(dateStr);
            } else if (holder instanceof MessageViewHolder) {
                MessageViewHolder msgHolder = (MessageViewHolder) holder;
                Message msg = (Message) displayItems.get(position);
                msgHolder.tvMessageText.setText(msg.getMessageText());
                msgHolder.tvMessageTime.setText(formatTime(msg.getCreatedAt()));

                int viewType = getItemViewType(position);
                float density = context.getResources().getDisplayMetrics().density;
                float roundedCorner = 18 * density;
                float sharpCorner = 4 * density;

                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) msgHolder.cardMessageBubble.getLayoutParams();

                if (viewType == TYPE_MESSAGE_SENDER) {
                    lp.addRule(RelativeLayout.ALIGN_PARENT_END);
                    lp.removeRule(RelativeLayout.ALIGN_PARENT_START);
                    lp.removeRule(RelativeLayout.END_OF);
                    lp.setMargins((int) (64 * density), 0, (int) (8 * density), 0);

                    msgHolder.ivUserAvatar.setVisibility(View.GONE);
                    msgHolder.cardMessageBubble.setCardBackgroundColor(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.msg_sender_bg)));
                    msgHolder.tvMessageText.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.msg_sender_text));
                    msgHolder.tvMessageTime.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.msg_sender_time));

                    msgHolder.cardMessageBubble.setShapeAppearanceModel(
                        msgHolder.cardMessageBubble.getShapeAppearanceModel().toBuilder()
                            .setTopLeftCornerSize(roundedCorner)
                            .setBottomLeftCornerSize(roundedCorner)
                            .setTopRightCornerSize(roundedCorner)
                            .setBottomRightCornerSize(sharpCorner)
                            .build()
                    );

                    if (msgHolder.ivMessageStatus != null) {
                        msgHolder.ivMessageStatus.setVisibility(View.VISIBLE);
                        boolean blocked = false;
                        if (context instanceof ChatActivity) {
                            blocked = ((ChatActivity) context).amIBlockedByOtherUser;
                        }
                        if (!blocked && "accepted".equals(requestStatus) && msg.isRead()) {
                            msgHolder.ivMessageStatus.setImageResource(R.drawable.ic_double_check);
                            msgHolder.ivMessageStatus.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.msg_sender_status_read)));
                        } else if (!blocked && "accepted".equals(requestStatus) && msg.isDelivered()) {
                            msgHolder.ivMessageStatus.setImageResource(R.drawable.ic_double_check);
                            msgHolder.ivMessageStatus.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.msg_sender_status_delivered)));
                        } else {
                            msgHolder.ivMessageStatus.setImageResource(R.drawable.ic_single_check);
                            msgHolder.ivMessageStatus.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.msg_sender_status_delivered)));
                        }
                    }
                } else {
                    // Receiver message
                    lp.addRule(RelativeLayout.END_OF, R.id.ivUserAvatar);
                    lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
                    lp.removeRule(RelativeLayout.ALIGN_PARENT_START);
                    lp.setMargins(0, 0, (int) (64 * density), 0);

                    msgHolder.cardMessageBubble.setCardBackgroundColor(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.msg_receiver_bg)));
                    msgHolder.tvMessageText.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.msg_receiver_text));
                    msgHolder.tvMessageTime.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.msg_receiver_time));

                    msgHolder.cardMessageBubble.setShapeAppearanceModel(
                        msgHolder.cardMessageBubble.getShapeAppearanceModel().toBuilder()
                            .setTopLeftCornerSize(roundedCorner)
                            .setBottomLeftCornerSize(sharpCorner)
                            .setTopRightCornerSize(roundedCorner)
                            .setBottomRightCornerSize(roundedCorner)
                            .build()
                    );

                    if (msgHolder.ivMessageStatus != null) {
                        msgHolder.ivMessageStatus.setVisibility(View.GONE);
                    }

                    msgHolder.ivUserAvatar.setVisibility(View.VISIBLE);
                    if (otherUserProfileImageUrl != null && !otherUserProfileImageUrl.isEmpty()) {
                        msgHolder.ivUserAvatar.setImageTintList(null);
                        msgHolder.ivUserAvatar.setPadding(0, 0, 0, 0);
                        GlideApp.with(context)
                                .load(otherUserProfileImageUrl)
                                .placeholder(R.drawable.ic_user)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .circleCrop()
                                .into(msgHolder.ivUserAvatar);
                    } else {
                        msgHolder.ivUserAvatar.setImageResource(R.drawable.ic_user);
                        msgHolder.ivUserAvatar.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.avatar_icon_tint)));
                        int padding = (int) (4 * density);
                        msgHolder.ivUserAvatar.setPadding(padding, padding, padding, padding);
                    }

                    msgHolder.ivUserAvatar.setOnClickListener(v -> {
                        if (context instanceof ChatActivity) {
                            ChatActivity chatAct = (ChatActivity) context;
                            View stableAnchor = msgHolder.ivUserAvatar;
                            View dummy = chatAct.findViewById(R.id.popupAnchorDummy);
                            if (dummy != null) {
                                int[] loc = new int[2];
                                msgHolder.ivUserAvatar.getLocationInWindow(loc);
                                dummy.setX(loc[0]);
                                dummy.setY(loc[1]);
                                stableAnchor = dummy;
                            }
                            ProfileContextMenuHelper.show(
                                    context,
                                    stableAnchor,
                                    chatAct.currentUnivId,
                                    chatAct.otherUserId,
                                    chatAct.otherUserName,
                                    chatAct.isOtherUserBlockedByMe,
                                    chatAct.amIBlockedByOtherUser,
                                    isBlocked -> {
                                        chatAct.isOtherUserBlockedByMe = isBlocked;
                                        chatAct.updateChatMessagingUI();
                                        chatAct.queryOtherUserProfile();
                                    }
                            );
                        }
                    });
                }
                msgHolder.cardMessageBubble.setLayoutParams(lp);
            }
        }

        @Override
        public int getItemCount() {
            return displayItems.size();
        }

        public static class MessageViewHolder extends RecyclerView.ViewHolder {
            ImageView ivUserAvatar;
            MaterialCardView cardMessageBubble;
            TextView tvMessageText, tvMessageTime;
            ImageView ivMessageStatus;

            public MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
                cardMessageBubble = itemView.findViewById(R.id.cardMessageBubble);
                tvMessageText = itemView.findViewById(R.id.tvMessageText);
                tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
                ivMessageStatus = itemView.findViewById(R.id.ivMessageStatus);
            }
        }

        public static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
            TextView tvDateHeader;

            public DateHeaderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDateHeader = itemView.findViewById(R.id.tvDateHeader);
            }
        }

        private static String formatTime(String isoTime) {
            if (isoTime == null || isoTime.isEmpty()) {
                SimpleDateFormat tf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                return tf.format(new Date());
            }
            try {
                Date date = ValidationUtils.parseIso8601(isoTime);
                if (date != null) {
                    SimpleDateFormat tf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                    tf.setTimeZone(TimeZone.getDefault());
                    return tf.format(date);
                }
            } catch (Exception ignored) {}
            return "";
        }
    }

    public static class BlockedRecord {
        private String id;
        private String blocker_id;
        private String blocked_id;
        private String created_at;
        
        public String getId() { return id; }
        public String getBlockerId() { return blocker_id; }
        public String getBlockedId() { return blocked_id; }
    }

    public static class Participant {
        private String conversation_id;
        private String university_id;
        
        public String getConversationId() { return conversation_id; }
        public String getUniversityId() { return university_id; }
    }

    public static class ConversationRecord {
        private String id;
        private String report_id;
        
        public String getId() { return id; }
        public String getReportId() { return report_id; }
    }
}
