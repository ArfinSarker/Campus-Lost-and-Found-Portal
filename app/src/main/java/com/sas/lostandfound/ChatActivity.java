package com.sas.lostandfound;

import android.content.Context;
import android.content.Intent;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.Set;
import java.util.HashSet;
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
import android.widget.PopupWindow;

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

    public static class MessageMeta {
        public String text;
        public String replyToId;
        public String replyToText;
        public String replyToSenderName;
        public Map<String, String> reactions; // universityId -> emoji
        public List<String> deletedForUsers;
        public boolean isUnsent;

        public boolean isSystemMessage;
        public String systemType;
        public String blockerId;

        public static MessageMeta parseMeta(String rawText) {
            if (rawText == null || !rawText.trim().startsWith("{") || !rawText.trim().endsWith("}")) {
                return null;
            }
            try {
                if (rawText.contains("\"isUnsent\"") || rawText.contains("\"deletedForUsers\"") || rawText.contains("\"reactions\"") || rawText.contains("\"text\"") || rawText.contains("\"isSystemMessage\"")) {
                    return new com.google.gson.Gson().fromJson(rawText, MessageMeta.class);
                }
            } catch (Exception ignored) {}
            return null;
        }
    }

    // Reply system state
    private Message replyingToMessage = null;
    private View layoutReplyPreview;
    private TextView tvReplyName, tvReplyContent;
    private View btnCancelReply;

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

    private View layoutPinnedBanner;
    private TextView tvPinnedTitle, tvPinnedContent;
    private String currentPinnedMessageId = null;

    private View cardBlockNotice;
    private TextView tvBlockNoticeText;
    private com.google.android.material.button.MaterialButton btnBlockNoticeDelete, btnBlockNoticeUnblock;

    private View layoutNormalComposer, layoutBlockedComposer;
    private com.google.android.material.button.MaterialButton btnBlockedDeleteChat, btnBlockedUnblock;

    private String conversationId, reportId, itemName;
    String otherUserId, otherUserName, otherUserProfileImageUrl;
    String currentUnivId;
    boolean isOtherUserBlockedByMe = false;
    boolean amIBlockedByOtherUser = false;
    private int defaultHeaderStatusColor;

    private List<Message> messageList = new ArrayList<>();
    private MessagesAdapter adapter;
    private final Map<String, String> pendingMessageTexts = new java.util.concurrent.ConcurrentHashMap<>();

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadMessages(false);
            queryPinnedMessage();
            if (!"accepted".equals(requestStatus)) {
                queryRequestStatus();
            }
            queryOtherUserProfile();
            checkBlockStatus();
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
        if (currentUnivId != null) currentUnivId = currentUnivId.trim();

        conversationId = getIntent().getStringExtra("conversationId");
        if (conversationId != null) conversationId = conversationId.trim();
        otherUserId = getIntent().getStringExtra("otherUserId");
        if (otherUserId != null) otherUserId = otherUserId.trim();
        otherUserName = getIntent().getStringExtra("otherUserName");
        reportId = getIntent().getStringExtra("reportId");
        itemName = getIntent().getStringExtra("itemName");

        initializeViews();
        setupToolbar();

        layoutReplyPreview = findViewById(R.id.layoutReplyPreview);
        tvReplyName = findViewById(R.id.tvReplyName);
        tvReplyContent = findViewById(R.id.tvReplyContent);
        btnCancelReply = findViewById(R.id.btnCancelReply);
        if (btnCancelReply != null) {
            btnCancelReply.setOnClickListener(v -> cancelReplyMode());
        }
        
        if (tvHeaderStatus != null) {
            defaultHeaderStatusColor = tvHeaderStatus.getCurrentTextColor();
        }

        setupRecyclerView();

        isOtherUserBlockedByMe = getIntent().getBooleanExtra("isBlockedByMe", false);
        amIBlockedByOtherUser = getIntent().getBooleanExtra("isBlockedByOther", false);
        updateChatMessagingUI();

        btnSendMessageCard.setOnClickListener(v -> sendMessage());
        btnSettings.setOnClickListener(v -> showSettingsMenu(v));
        llHeaderTitle.setOnClickListener(v -> showProfileContextMenu(v));

        if (btnBlockNoticeUnblock != null) {
            btnBlockNoticeUnblock.setOnClickListener(v -> {
                toggleUserBlock();
            });
        }
        if (btnBlockNoticeDelete != null) {
            btnBlockNoticeDelete.setOnClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setTitle("Delete Chat?")
                        .setMessage("This will delete the conversation and clear all message history only from your account. The other user will still keep their conversation history.")
                        .setPositiveButton("Delete", (dialog, which) -> {
                            SharedPreferences chatPrefs = getSharedPreferences("ChatPrefs_" + (currentUnivId != null ? currentUnivId.trim() : "default"), Context.MODE_PRIVATE);
                            chatPrefs.edit().putLong("delete_timestamp_" + conversationId, System.currentTimeMillis()).apply();
                            java.util.Set<String> deleted = new java.util.HashSet<>(chatPrefs.getStringSet("locally_deleted_conversations", new java.util.HashSet<>()));
                            deleted.add(conversationId);
                            chatPrefs.edit().putStringSet("locally_deleted_conversations", deleted).apply();
                            
                            SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Chat deleted successfully.");
                            finish();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }

        if (btnBlockedUnblock != null) {
            btnBlockedUnblock.setOnClickListener(v -> {
                toggleUserBlock();
            });
        }
        if (btnBlockedDeleteChat != null) {
            btnBlockedDeleteChat.setOnClickListener(v -> {
                if (btnBlockNoticeDelete != null) {
                    btnBlockNoticeDelete.performClick();
                }
            });
        }

        requestStatus = getIntent().getStringExtra("requestStatus");
        if (requestStatus == null) requestStatus = "accepted";
        requestSenderId = getIntent().getStringExtra("requestSenderId");
        initialMessage = getIntent().getStringExtra("initialMessage");
        requestCreatedAt = getIntent().getStringExtra("requestCreatedAt");

        updateRequestLayoutUI();

        loadMessages(true);
        queryPinnedMessage();
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
        UnreadBadgeHelper.sendBadgeUpdateBroadcast(this);
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

        layoutPinnedBanner = findViewById(R.id.layoutPinnedBanner);
        tvPinnedTitle = findViewById(R.id.tvPinnedTitle);
        tvPinnedContent = findViewById(R.id.tvPinnedContent);

        cardBlockNotice = findViewById(R.id.cardBlockNotice);
        tvBlockNoticeText = findViewById(R.id.tvBlockNoticeText);
        btnBlockNoticeDelete = findViewById(R.id.btnBlockNoticeDelete);
        btnBlockNoticeUnblock = findViewById(R.id.btnBlockNoticeUnblock);

        layoutNormalComposer = findViewById(R.id.layoutNormalComposer);
        layoutBlockedComposer = findViewById(R.id.layoutBlockedComposer);
        btnBlockedDeleteChat = findViewById(R.id.btnBlockedDeleteChat);
        btnBlockedUnblock = findViewById(R.id.btnBlockedUnblock);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvHeaderName.setText(otherUserName != null ? otherUserName : "Chat");
        
        // Prevent layout coloring utility from centering tvHeaderName and tvHeaderStatus
        tvHeaderName.setTag("centered");
        if (tvHeaderStatus != null) {
            tvHeaderStatus.setTag("centered");
        }

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            int headerColor = androidx.core.content.ContextCompat.getColor(this, R.color.chat_header_bg);
            boolean isNightMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            HeaderColorHelper.setup(this, appBarLayout, headerColor, headerColor, !isNightMode);
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
                    otherUserProfileImageUrl = imgUrl;
                    float density = getResources().getDisplayMetrics().density;
                    if (imgUrl != null && !imgUrl.isEmpty()) {
                        ivHeaderAvatar.setImageTintList(null);
                        ivHeaderAvatar.setPadding(0, 0, 0, 0);
                        GlideApp.with(ChatActivity.this)
                                .load(imgUrl)
                                .placeholder(R.drawable.ic_user_placeholder_white)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .circleCrop()
                                .into(ivHeaderAvatar);
                    } else {
                        ivHeaderAvatar.setImageResource(R.drawable.ic_user_placeholder_white);
                        ivHeaderAvatar.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(ChatActivity.this, R.color.white)));
                        int padding = (int) (6 * density);
                        ivHeaderAvatar.setPadding(padding, padding, padding, padding);
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
                                    tvHeaderStatus.setTextColor(androidx.core.content.ContextCompat.getColor(ChatActivity.this, R.color.chat_header_last_active));
                                } else {
                                    tvHeaderStatus.setTextColor(androidx.core.content.ContextCompat.getColor(ChatActivity.this, R.color.chat_header_last_seen));
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
            if (layoutNormalComposer != null) layoutNormalComposer.setVisibility(View.GONE);
            if (layoutBlockedComposer != null) layoutBlockedComposer.setVisibility(View.VISIBLE);

            if (tvHeaderStatus != null) tvHeaderStatus.setVisibility(View.GONE);

            if (cardBlockNotice != null) {
                cardBlockNotice.setVisibility(View.GONE);
            }
        } else if (amIBlockedByOtherUser) {
            if (layoutNormalComposer != null) layoutNormalComposer.setVisibility(View.VISIBLE);
            if (layoutBlockedComposer != null) layoutBlockedComposer.setVisibility(View.GONE);

            etMessageInput.setFocusable(false);
            etMessageInput.setFocusableInTouchMode(false);
            etMessageInput.setClickable(true);
            etMessageInput.setOnClickListener(v -> {
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Messaging is currently unavailable.");
            });
            etMessageInput.setSingleLine(true);
            etMessageInput.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            etMessageInput.setText("");
            etMessageInput.setHint("You can't send messages to this user.");
            btnSendMessageCard.setVisibility(View.GONE);
            if (tvHeaderStatus != null) tvHeaderStatus.setVisibility(View.GONE);

            if (cardBlockNotice != null) {
                cardBlockNotice.setVisibility(View.GONE);
            }
        } else {
            if (layoutNormalComposer != null) layoutNormalComposer.setVisibility(View.VISIBLE);
            if (layoutBlockedComposer != null) layoutBlockedComposer.setVisibility(View.GONE);

            etMessageInput.setFocusable(true);
            etMessageInput.setFocusableInTouchMode(true);
            etMessageInput.setClickable(true);
            etMessageInput.setOnClickListener(null);
            etMessageInput.setSingleLine(false);
            etMessageInput.setMaxLines(4);
            etMessageInput.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            etMessageInput.setHint(getString(R.string.hint_type_message));
            btnSendMessageCard.setVisibility(View.VISIBLE);
            btnSendMessageCard.setEnabled(true);
            btnSendMessageCard.setCardBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.primaryColor)));

            if (cardBlockNotice != null && !"You unblocked this person.".equals(tvBlockNoticeText.getText().toString())) {
                cardBlockNotice.setVisibility(View.GONE);
            }
        }
    }

    private void toggleUserBlock() {
        if (currentUnivId == null || otherUserId == null) return;
        boolean wasBlocked = isOtherUserBlockedByMe;
        ProfileContextMenuHelper.toggleUserBlock(this, currentUnivId, otherUserId, isOtherUserBlockedByMe, isBlocked -> {
            isOtherUserBlockedByMe = isBlocked;
            updateChatMessagingUI();
            queryOtherUserProfile();
            if (wasBlocked && !isBlocked) {
                insertSystemMessage("unblock");
                showTemporaryUnblockNotice();
            } else if (!wasBlocked && isBlocked) {
                insertSystemMessage("block");
            }
        });
    }

    private void insertSystemMessage(String type) {
        if (conversationId == null || currentUnivId == null) return;
        
        MessageMeta meta = new MessageMeta();
        meta.isSystemMessage = true;
        meta.systemType = type;
        meta.blockerId = currentUnivId;
        
        String jsonText = new com.google.gson.Gson().toJson(meta);
        Message msg = new Message(conversationId, "system", jsonText);
        
        SupabaseDatabaseHelper.insert("messages", msg, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                loadMessages(false);
            }
            @Override
            public void onFailure(String error) {}
        });
    }

    private void showTemporaryUnblockNotice() {
        if (cardBlockNotice != null && tvBlockNoticeText != null) {
            cardBlockNotice.setVisibility(View.VISIBLE);
            tvBlockNoticeText.setText("You unblocked this person.");
            View btnDelete = findViewById(R.id.btnBlockNoticeDelete);
            View btnUnblock = findViewById(R.id.btnBlockNoticeUnblock);
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
            if (btnUnblock != null) btnUnblock.setVisibility(View.GONE);
            
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (tvBlockNoticeText != null && "You unblocked this person.".equals(tvBlockNoticeText.getText().toString())) {
                    if (cardBlockNotice != null) cardBlockNotice.setVisibility(View.GONE);
                    tvBlockNoticeText.setText("You blocked this person. Tap to unblock.");
                    if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
                    if (btnUnblock != null) btnUnblock.setVisibility(View.VISIBLE);
                }
            }, 2500);
        }
    }

    private void showSettingsMenu(View anchor) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.layout_chat_settings_menu, null);
        
        final android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );

        TextView textBlockUser = popupView.findViewById(R.id.text_block_user);
        if (textBlockUser != null) {
            textBlockUser.setText(isOtherUserBlockedByMe ? "Unblock User" : "Block User");
        }

        View menuUserProfile = popupView.findViewById(R.id.menu_user_profile);
        if (menuUserProfile != null) {
            menuUserProfile.setOnClickListener(v -> {
                popupWindow.dismiss();
                openOtherUserProfile();
            });
        }

        View menuReportDetails = popupView.findViewById(R.id.menu_report_details);
        if (menuReportDetails != null) {
            menuReportDetails.setOnClickListener(v -> {
                popupWindow.dismiss();
                queryAndOpenReportDetails();
            });
        }

        View menuBlockUser = popupView.findViewById(R.id.menu_block_user);
        if (menuBlockUser != null) {
            menuBlockUser.setOnClickListener(v -> {
                popupWindow.dismiss();
                toggleUserBlock();
            });
        }

        popupWindow.setElevation(8f);
        popupWindow.showAsDropDown(anchor, 0, 10);
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
            boolean wasBlocked = isOtherUserBlockedByMe;
            isOtherUserBlockedByMe = isBlocked;
            updateChatMessagingUI();
            queryOtherUserProfile();
            if (wasBlocked && !isBlocked) {
                insertSystemMessage("unblock");
                showTemporaryUnblockNotice();
            } else if (!wasBlocked && isBlocked) {
                insertSystemMessage("block");
            }
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

                SharedPreferences chatPrefs = getSharedPreferences("ChatPrefs_" + (currentUnivId != null ? currentUnivId.trim() : "default"), Context.MODE_PRIVATE);
                long deleteTime = chatPrefs.getLong("delete_timestamp_" + conversationId, 0);

                List<Message> filtered = new ArrayList<>();
                for (Message m : messages) {
                    if (pendingMessageTexts.containsKey(m.getId())) {
                        m.setMessageText(pendingMessageTexts.get(m.getId()));
                    }
                    Date d = MessagesAdapter.parseDate(m.getCreatedAt());
                    if (d != null && d.getTime() > deleteTime) {
                        filtered.add(m);
                    }
                }

                // Real-time updates for reaction details popup
                if (activeReactionDetailsDialog != null && activeReactionDetailsDialog.isShowing() && activeReactionDetailsMessageId != null) {
                    Message updatedMsg = null;
                    for (Message m : filtered) {
                        if (activeReactionDetailsMessageId.equals(m.getId())) {
                            updatedMsg = m;
                            break;
                        }
                    }
                    if (updatedMsg != null) {
                        Map<String, String> newReactions = new HashMap<>();
                        MessageMeta updatedMeta = MessageMeta.parseMeta(updatedMsg.getMessageText());
                        if (updatedMeta != null && updatedMeta.reactions != null) {
                            newReactions.putAll(updatedMeta.reactions);
                        }
                        if (!newReactions.equals(activeReactionDetailsMap)) {
                            refreshActiveReactionDetails(newReactions);
                        }
                    } else {
                        activeReactionDetailsDialog.dismiss();
                    }
                }
                
                int oldSize = messageList.size();
                int newSize = filtered.size();

                if (newSize > oldSize || areMessageListsDifferent(messageList, filtered)) {
                    messageList.clear();
                    messageList.addAll(filtered);
                    adapter.setMessages(messageList);
                    
                    if (forceScroll || oldSize == 0) {
                        rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                    } else if (newSize > oldSize) {
                        rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1);
                    }
                    markMessagesAsRead();
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
            String t1 = m1.getMessageText();
            String t2 = m2.getMessageText();
            if (t1 == null && t2 != null) return true;
            if (t1 != null && !t1.equals(t2)) return true;
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

        Map<String, Object> unreadData = new HashMap<>();
        unreadData.put("receiver_marked_unread", false);
        String unreadQuery = "conversation_id=eq." + conversationId + "&sender_id=neq." + currentUnivId + "&receiver_marked_unread=eq.true";
        SupabaseDatabaseHelper.update("messages", unreadQuery, unreadData, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
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
            SnackbarManager.show(SnackbarManager.Type.ERROR, "You have blocked this user. Unblock them to send and receive messages.");
            return;
        }
        if (amIBlockedByOtherUser) {
            SnackbarManager.show(SnackbarManager.Type.ERROR, "You can't send messages to this user.");
            return;
        }

        etMessageInput.setText(""); // Clear input immediately

        String finalText = text;
        if (replyingToMessage != null) {
            MessageMeta meta = new MessageMeta();
            meta.text = text;
            meta.replyToId = replyingToMessage.getId();

            String parentText = replyingToMessage.getMessageText();
            meta.replyToText = getMessagePreview(parentText);

            String parentSenderName = currentUnivId.equals(replyingToMessage.getSenderId()) ? "You" : otherUserName;
            meta.replyToSenderName = parentSenderName;
            meta.reactions = new HashMap<>();
            meta.deletedForUsers = new ArrayList<>();
            meta.isUnsent = false;

            finalText = new com.google.gson.Gson().toJson(meta);
            cancelReplyMode();
        }

        Message msg = new Message(conversationId, currentUnivId, finalText);

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

    public void scrollToMessage(String targetId) {
        scrollToAndHighlightMessage(targetId);
    }

    private void enterReplyMode(Message msg) {
        replyingToMessage = msg;
        String previewText = getMessagePreview(msg.getMessageText());

        if (layoutReplyPreview != null) {
            String senderName = currentUnivId.equals(msg.getSenderId()) ? "You" : otherUserName;
            tvReplyName.setText("Replying to " + senderName);
            tvReplyContent.setText(previewText);
            layoutReplyPreview.setVisibility(View.VISIBLE);
        }
    }

    private void cancelReplyMode() {
        replyingToMessage = null;
        if (layoutReplyPreview != null) {
            layoutReplyPreview.setVisibility(View.GONE);
        }
    }

    public static String getMessagePreview(String text) {
        if (text == null) return "";
        MessageMeta meta = MessageMeta.parseMeta(text);
        String plainText = text;
        if (meta != null) {
            if (meta.isUnsent) {
                return "This message was unsent";
            }
            plainText = meta.text;
        }

        if (plainText == null) return "";
        String lower = plainText.toLowerCase().trim();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            if (lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".gif") || lower.endsWith(".webp")) {
                return "📷 Photo";
            }
            if (lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".avi") || lower.endsWith(".3gp") || lower.endsWith(".webm")) {
                return "📹 Video";
            }
            if (lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".m4a") || lower.endsWith(".aac")) {
                return "🎵 Audio";
            }
            if (lower.endsWith(".pdf") || lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".ppt") || lower.endsWith(".pptx") || lower.endsWith(".txt") || lower.endsWith(".zip") || lower.endsWith(".rar")) {
                return "📄 Document";
            }
            return "🔗 Link";
        }
        return plainText;
    }

    private void queryPinnedMessage() {
        if (conversationId == null) return;
        
        SupabaseDatabaseHelper.select("conversations", "id=eq." + conversationId, new TypeToken<List<Conversation>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Conversation>>() {
            @Override
            public void onSuccess(List<Conversation> list) {
                if (list != null && !list.isEmpty()) {
                    Conversation c = list.get(0);
                    String pinnedId = c.getPinnedMessageId();
                    updatePinnedBannerUI(pinnedId);
                } else {
                    updatePinnedBannerUI(null);
                }
            }

            @Override
            public void onFailure(String error) {
            }
        });
    }

    private void updatePinnedBannerUI(String pinnedId) {
        if (pinnedId == null || pinnedId.isEmpty()) {
            currentPinnedMessageId = null;
            if (layoutPinnedBanner != null) {
                layoutPinnedBanner.setVisibility(View.GONE);
            }
            return;
        }

        currentPinnedMessageId = pinnedId;

        // Search in messageList for the message object
        Message pinnedMsg = null;
        for (Message m : messageList) {
            if (pinnedId.equals(m.getId())) {
                pinnedMsg = m;
                break;
            }
        }

        if (pinnedMsg != null) {
            renderPinnedBanner(pinnedMsg);
        } else {
            // Not in messageList yet, query database directly to ensure sync works on both sides
            SupabaseDatabaseHelper.select("messages", "id=eq." + pinnedId, new TypeToken<List<Message>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Message>>() {
                @Override
                public void onSuccess(List<Message> list) {
                    if (list != null && !list.isEmpty()) {
                        renderPinnedBanner(list.get(0));
                    } else {
                        unpinMessageSilently();
                    }
                }

                @Override
                public void onFailure(String error) {
                    if (layoutPinnedBanner != null) {
                        layoutPinnedBanner.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    private void renderPinnedBanner(Message pinnedMsg) {
        if (pinnedMsg == null) return;

        MessageMeta meta = MessageMeta.parseMeta(pinnedMsg.getMessageText());
        if (meta != null && meta.isUnsent) {
            unpinMessageSilently();
            return;
        }

        // Show banner
        if (layoutPinnedBanner != null) {
            layoutPinnedBanner.setVisibility(View.VISIBLE);
            layoutPinnedBanner.setOnClickListener(v -> scrollToAndHighlightMessage(pinnedMsg.getId()));
        }

        // Set title: "Pinned Message"
        String senderName = currentUnivId.equals(pinnedMsg.getSenderId()) ? "You" : otherUserName;
        if (tvPinnedTitle != null) {
            tvPinnedTitle.setText("Pinned Message");
        }

        // Set content preview: "Sender: Preview"
        String previewText = getMessagePreview(pinnedMsg.getMessageText());
        if (tvPinnedContent != null) {
            tvPinnedContent.setText(senderName + ": " + previewText);
        }

    }

    private void unpinMessageSilently() {
        if (conversationId == null) return;
        currentPinnedMessageId = null;
        if (layoutPinnedBanner != null) {
            layoutPinnedBanner.setVisibility(View.GONE);
        }
        Map<String, Object> data = new HashMap<>();
        data.put("pinned_message_id", null);
        SupabaseDatabaseHelper.update("conversations", "id=eq." + conversationId, data, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override public void onSuccess(String r) {}
            @Override public void onFailure(String e) {}
        });
    }

    private void playSubtleHighlightAnimation(MessagesAdapter.MessageViewHolder msgHolder) {
        final View bubble = msgHolder.cardMessageBubble;
        if (bubble == null) return;

        bubble.setScaleX(1.0f);
        bubble.setScaleY(1.0f);

        bubble.animate()
            .scaleX(1.08f)
            .scaleY(1.08f)
            .setDuration(150)
            .withEndAction(() -> {
                bubble.animate()
                    .scaleX(0.96f)
                    .scaleY(0.96f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        bubble.animate()
                            .scaleX(1.04f)
                            .scaleY(1.04f)
                            .setDuration(150)
                            .withEndAction(() -> {
                                bubble.animate()
                                    .scaleX(1.0f)
                                    .scaleY(1.0f)
                                    .setDuration(150)
                                    .start();
                            })
                            .start();
                    })
                    .start();
            })
            .start();
    }

    private void scrollToAndHighlightMessage(String targetId) {
        if (targetId == null || adapter == null) return;
        int position = -1;
        for (int i = 0; i < adapter.displayItems.size(); i++) {
            Object obj = adapter.displayItems.get(i);
            if (obj instanceof Message) {
                Message m = (Message) obj;
                if (targetId.equals(m.getId())) {
                    position = i;
                    break;
                }
            }
        }

        if (position != -1) {
            final int targetPos = position;
            
            // Check if already visible
            androidx.recyclerview.widget.RecyclerView.ViewHolder holder = rvMessages.findViewHolderForAdapterPosition(targetPos);
            if (holder instanceof MessagesAdapter.MessageViewHolder) {
                playSubtleHighlightAnimation((MessagesAdapter.MessageViewHolder) holder);
            } else {
                // Attach a listener to play the animation when smooth scrolling finishes
                rvMessages.addOnScrollListener(new androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrollStateChanged(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView recyclerView, int newState) {
                        if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE) {
                            recyclerView.removeOnScrollListener(this);
                            androidx.recyclerview.widget.RecyclerView.ViewHolder newHolder = recyclerView.findViewHolderForAdapterPosition(targetPos);
                            if (newHolder instanceof MessagesAdapter.MessageViewHolder) {
                                playSubtleHighlightAnimation((MessagesAdapter.MessageViewHolder) newHolder);
                            }
                        }
                    }
                });
                rvMessages.smoothScrollToPosition(targetPos);
            }
        }
    }

    private void pinMessage(Message msg) {
        if (conversationId == null) return;
        
        Map<String, Object> data = new HashMap<>();
        data.put("pinned_message_id", msg.getId());
        
        SupabaseDatabaseHelper.update("conversations", "id=eq." + conversationId, data, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Message pinned");
                queryPinnedMessage();
            }

            @Override
            public void onFailure(String errorMessage) {
                String friendlyError = errorMessage;
                if (errorMessage != null && errorMessage.contains("No records were updated")) {
                    friendlyError = "You don't have permission to pin messages, or the conversation was not found.";
                }
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to pin message: " + friendlyError);
                android.util.Log.e("ChatActivity", "Failed to pin message: " + errorMessage);
            }
        });
    }

    private void unpinMessage(Message msg) {
        if (conversationId == null) return;
        
        Map<String, Object> data = new HashMap<>();
        data.put("pinned_message_id", null);
        
        SupabaseDatabaseHelper.update("conversations", "id=eq." + conversationId, data, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Message unpinned");
                queryPinnedMessage();
            }

            @Override
            public void onFailure(String errorMessage) {
                String friendlyError = errorMessage;
                if (errorMessage != null && errorMessage.contains("No records were updated")) {
                    friendlyError = "You don't have permission to unpin messages, or the conversation was not found.";
                }
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to unpin message: " + friendlyError);
                android.util.Log.e("ChatActivity", "Failed to unpin message: " + errorMessage);
            }
        });
    }

    private void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("message", text);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Copied to clipboard!");
        }
    }

    private void startForwardActivity(Message msg) {
        Intent intent = new Intent(this, ForwardActivity.class);
        intent.putExtra("messageText", msg.getMessageText());
        startActivity(intent);
    }

    private void showDeleteOptionsDialog(Message msg) {
        boolean isSender = currentUnivId.equals(msg.getSenderId());

        Date createdDate = MessagesAdapter.parseDate(msg.getCreatedAt());
        long diffMinutes = (System.currentTimeMillis() - createdDate.getTime()) / (60 * 1000);
        boolean canUnsend = isSender && (diffMinutes < 10);

        BottomSheetDialog deleteDialog = new BottomSheetDialog(this);
        View deleteView = getLayoutInflater().inflate(R.layout.layout_delete_options_bottom_sheet, null);
        deleteDialog.setContentView(deleteView);

        View optionUnsend = deleteView.findViewById(R.id.option_unsend_everyone);
        View optionDeleteMe = deleteView.findViewById(R.id.option_delete_for_me);
        View optionCancel = deleteView.findViewById(R.id.option_cancel);

        if (canUnsend) {
            optionUnsend.setVisibility(View.VISIBLE);
        } else {
            optionUnsend.setVisibility(View.GONE);
        }

        optionUnsend.setOnClickListener(v -> {
            deleteDialog.dismiss();
            new AlertDialog.Builder(this)
                    .setTitle("Unsend message?")
                    .setMessage("This will remove the message for everyone in the chat.")
                    .setPositiveButton("Unsend", (d, w) -> performUnsend(msg))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        optionDeleteMe.setOnClickListener(v -> {
            deleteDialog.dismiss();
            new AlertDialog.Builder(this)
                    .setTitle("Delete message for you?")
                    .setMessage("This will only remove the message for you. Other chat participants will still be able to see it.")
                    .setPositiveButton("Delete", (d, w) -> performDeleteForYou(msg))
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        optionCancel.setOnClickListener(v -> deleteDialog.dismiss());

        deleteDialog.show();
    }

    private void performUnsend(Message msg) {
        if (msg.getId().equals(currentPinnedMessageId)) {
            unpinMessageSilently();
        }
        String rawText = msg.getMessageText();
        MessageMeta meta = MessageMeta.parseMeta(rawText);
        if (meta == null) {
            meta = new MessageMeta();
            meta.text = rawText;
            meta.reactions = new HashMap<>();
            meta.deletedForUsers = new ArrayList<>();
        }
        meta.isUnsent = true;

        String newJson = new com.google.gson.Gson().toJson(meta);
        msg.setMessageText(newJson);
        
        // Optimistic update caching
        pendingMessageTexts.put(msg.getId(), newJson);

        adapter.notifyDataSetChanged();

        Map<String, Object> data = new HashMap<>();
        data.put("message_text", newJson);
        SupabaseDatabaseHelper.update("messages", "id=eq." + msg.getId(), data, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                pendingMessageTexts.remove(msg.getId());
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Message unsent.");
            }

            @Override
            public void onFailure(String errorMessage) {
                pendingMessageTexts.remove(msg.getId());
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to unsend: " + errorMessage);
                loadMessages(false);
            }
        });
    }

    private void performDeleteForYou(Message msg) {
        String rawText = msg.getMessageText();
        MessageMeta meta = MessageMeta.parseMeta(rawText);
        if (meta == null) {
            meta = new MessageMeta();
            meta.text = rawText;
            meta.reactions = new HashMap<>();
            meta.deletedForUsers = new ArrayList<>();
            meta.isUnsent = false;
        }
        if (meta.deletedForUsers == null) {
            meta.deletedForUsers = new ArrayList<>();
        }
        if (!meta.deletedForUsers.contains(currentUnivId)) {
            meta.deletedForUsers.add(currentUnivId);
        }

        String newJson = new com.google.gson.Gson().toJson(meta);
        msg.setMessageText(newJson);
        
        // Optimistic update caching
        pendingMessageTexts.put(msg.getId(), newJson);

        messageList.remove(msg);
        adapter.setMessages(messageList);

        Map<String, Object> data = new HashMap<>();
        data.put("message_text", newJson);
        SupabaseDatabaseHelper.update("messages", "id=eq." + msg.getId(), data, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                pendingMessageTexts.remove(msg.getId());
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Message deleted.");
            }

            @Override
            public void onFailure(String errorMessage) {
                pendingMessageTexts.remove(msg.getId());
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to delete: " + errorMessage);
                loadMessages(false);
            }
        });
    }

    private void addOrUpdateReaction(Message msg, String emoji) {
        recordEmojiUsage(emoji);
        String rawText = msg.getMessageText();
        MessageMeta meta = MessageMeta.parseMeta(rawText);

        if (meta == null) {
            meta = new MessageMeta();
            meta.text = rawText;
            meta.reactions = new HashMap<>();
            meta.deletedForUsers = new ArrayList<>();
            meta.isUnsent = false;
        }
        if (meta.reactions == null) {
            meta.reactions = new HashMap<>();
        }

        String existing = meta.reactions.get(currentUnivId);
        if (emoji.equals(existing)) {
            meta.reactions.remove(currentUnivId);
        } else {
            meta.reactions.put(currentUnivId, emoji);
        }

        String newJson = new com.google.gson.Gson().toJson(meta);
        msg.setMessageText(newJson);
        
        // Optimistic update caching
        pendingMessageTexts.put(msg.getId(), newJson);

        adapter.notifyMessageChanged(msg);

        Map<String, Object> data = new HashMap<>();
        data.put("message_text", newJson);
        SupabaseDatabaseHelper.update("messages", "id=eq." + msg.getId(), data, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override 
            public void onSuccess(String result) {
                pendingMessageTexts.remove(msg.getId());
            }
            @Override 
            public void onFailure(String errorMessage) {
                pendingMessageTexts.remove(msg.getId());
                loadMessages(false);
            }
        });
    }

    public void showMessageActionsSheet(Message msg) {
        String rawText = msg.getMessageText();
        String displayText = rawText;
        boolean isUnsent = false;
        MessageMeta meta = MessageMeta.parseMeta(rawText);
        if (meta != null) {
            displayText = meta.text;
            isUnsent = meta.isUnsent;
        }

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_message_actions_bottom_sheet, null);
        dialog.setContentView(view);

        View layoutReactions = view.findViewById(R.id.layout_reactions_bar);
        View divider = view.findViewById(R.id.reactions_divider);
        View actionPin = view.findViewById(R.id.action_pin);
        ImageView ivActionPin = view.findViewById(R.id.iv_action_pin);
        TextView tvActionPin = view.findViewById(R.id.tv_action_pin);
        View actionReply = view.findViewById(R.id.action_reply);
        View actionCopy = view.findViewById(R.id.action_copy);
        View actionForward = view.findViewById(R.id.action_forward);
        View actionDelete = view.findViewById(R.id.action_delete);

        boolean isAlreadyPinned = msg.getId().equals(currentPinnedMessageId);
        boolean isBlocked = isOtherUserBlockedByMe || amIBlockedByOtherUser;
        if (isUnsent || isBlocked) {
            if (layoutReactions != null) layoutReactions.setVisibility(View.GONE);
            if (divider != null) divider.setVisibility(View.GONE);
            if (actionReply != null) actionReply.setVisibility(View.GONE);
            if (actionForward != null) actionForward.setVisibility(View.GONE);
            if (isUnsent && actionPin != null) {
                actionPin.setVisibility(View.GONE);
            }
            if (isBlocked && actionCopy != null && isUnsent) {
                actionCopy.setVisibility(View.GONE);
            }
        } else {
            if (layoutReactions != null) {
                layoutReactions.setVisibility(View.VISIBLE);
                
                // Highlight user's reaction
                String currentReaction = null;
                if (meta != null && meta.reactions != null) {
                    currentReaction = meta.reactions.get(currentUnivId);
                }

                List<String> quickReactions = getQuickReactionsForUser(currentUnivId);
                View[] emojis = new View[]{
                    view.findViewById(R.id.react_like),
                    view.findViewById(R.id.react_love),
                    view.findViewById(R.id.react_haha),
                    view.findViewById(R.id.react_wow),
                    view.findViewById(R.id.react_sad),
                    view.findViewById(R.id.react_angry)
                };

                for (int i = 0; i < emojis.length; i++) {
                    View v = emojis[i];
                    if (v instanceof TextView && i < quickReactions.size()) {
                        TextView tv = (TextView) v;
                        String emojiText = quickReactions.get(i);
                        tv.setText(emojiText);
                        
                        if (currentReaction != null && currentReaction.equals(emojiText)) {
                            tv.setBackground(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_circle_neutral_light));
                        } else {
                            tv.setBackgroundResource(0);
                        }
                        
                        tv.setOnClickListener(clickV -> {
                            tv.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction(() -> {
                                tv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).withEndAction(() -> {
                                    addOrUpdateReaction(msg, emojiText);
                                    dialog.dismiss();
                                }).start();
                            }).start();
                        });
                    }
                }

                View reactAddMore = view.findViewById(R.id.react_add_more);
                if (reactAddMore != null) {
                    reactAddMore.setOnClickListener(clickV -> {
                        reactAddMore.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction(() -> {
                            reactAddMore.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).withEndAction(() -> {
                                dialog.dismiss();
                                showEmojiPickerDialog(msg);
                            }).start();
                        }).start();
                    });
                }
            }
            if (divider != null) divider.setVisibility(View.VISIBLE);
            
            if (actionPin != null) {
                actionPin.setVisibility(View.VISIBLE);
                if (isAlreadyPinned) {
                    if (ivActionPin != null) ivActionPin.setImageResource(R.drawable.ic_unpin);
                    if (tvActionPin != null) tvActionPin.setText("Unpin");
                    actionPin.setOnClickListener(v -> {
                        unpinMessage(msg);
                        dialog.dismiss();
                    });
                } else {
                    if (ivActionPin != null) ivActionPin.setImageResource(R.drawable.ic_pin);
                    if (tvActionPin != null) tvActionPin.setText("Pin");
                    actionPin.setOnClickListener(v -> {
                        pinMessage(msg);
                        dialog.dismiss();
                    });
                }
            }
        }

        if (actionReply != null) {
            actionReply.setOnClickListener(v -> {
                enterReplyMode(msg);
                dialog.dismiss();
            });
        }

        if (actionCopy != null) {
            final String textToCopy = displayText;
            actionCopy.setOnClickListener(v -> {
                copyToClipboard(textToCopy);
                dialog.dismiss();
            });
        }

        if (actionForward != null) {
            actionForward.setOnClickListener(v -> {
                startForwardActivity(msg);
                dialog.dismiss();
            });
        }

        if (actionDelete != null) {
            actionDelete.setOnClickListener(v -> {
                showDeleteOptionsDialog(msg);
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private static final String PREF_QUICK_REACTIONS_PREFIX = "quick_reactions_";
    private static final String DEFAULT_QUICK_REACTIONS = "👍,❤️,😂,😮,😢,😡";
    private static final String PREF_RECENT_REACTIONS = "recent_reactions";
    private static final String PREF_FREQUENT_REACTIONS = "frequent_reactions_count";
    private static final String PREF_FAVORITE_REACTIONS = "favorite_reactions";

    private List<String> getQuickReactionsForUser(String univId) {
        String key = PREF_QUICK_REACTIONS_PREFIX + (univId != null ? univId.trim() : "default");
        android.content.SharedPreferences prefs = getSharedPreferences("EmojiPickerPrefs", android.content.Context.MODE_PRIVATE);
        String saved = prefs.getString(key, DEFAULT_QUICK_REACTIONS);
        String[] split = saved.split(",");
        List<String> result = new ArrayList<>();
        for (String s : split) {
            if (!s.trim().isEmpty()) {
                result.add(s.trim());
            }
        }
        while (result.size() < 6) {
            result.add("👍");
        }
        return result.subList(0, 6);
    }

    private void saveQuickReactionsForUser(String univId, List<String> reactions) {
        String key = PREF_QUICK_REACTIONS_PREFIX + (univId != null ? univId.trim() : "default");
        android.content.SharedPreferences prefs = getSharedPreferences("EmojiPickerPrefs", android.content.Context.MODE_PRIVATE);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reactions.size(); i++) {
            sb.append(reactions.get(i));
            if (i < reactions.size() - 1) {
                sb.append(",");
            }
        }
        prefs.edit().putString(key, sb.toString()).apply();
    }

    private List<String> getRecentReactions() {
        String keyPref = (currentUnivId != null ? currentUnivId.trim() : "default");
        android.content.SharedPreferences prefs = getSharedPreferences("EmojiPrefs_" + keyPref, android.content.Context.MODE_PRIVATE);
        String saved = prefs.getString(PREF_RECENT_REACTIONS, "");
        if (saved.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(java.util.Arrays.asList(saved.split(",")));
    }

    private List<String> getFrequentReactions() {
        String keyPref = (currentUnivId != null ? currentUnivId.trim() : "default");
        android.content.SharedPreferences prefs = getSharedPreferences("EmojiPrefs_" + keyPref, android.content.Context.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();
        List<String> result = new ArrayList<>();
        List<android.util.Pair<String, Integer>> counts = new ArrayList<>();
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getKey().startsWith(PREF_FREQUENT_REACTIONS + "_")) {
                String emoji = entry.getKey().substring((PREF_FREQUENT_REACTIONS + "_").length());
                Object val = entry.getValue();
                if (val instanceof Integer) {
                    counts.add(new android.util.Pair<>(emoji, (Integer) val));
                }
            }
        }
        java.util.Collections.sort(counts, (p1, p2) -> p2.second.compareTo(p1.second));
        for (android.util.Pair<String, Integer> pair : counts) {
            result.add(pair.first);
        }
        return result;
    }

    private List<String> getFavoriteReactions() {
        String keyPref = (currentUnivId != null ? currentUnivId.trim() : "default");
        android.content.SharedPreferences prefs = getSharedPreferences("EmojiPrefs_" + keyPref, android.content.Context.MODE_PRIVATE);
        String saved = prefs.getString(PREF_FAVORITE_REACTIONS, "");
        if (saved.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(java.util.Arrays.asList(saved.split(",")));
    }

    private void recordEmojiUsage(String emoji) {
        if (emoji == null || emoji.isEmpty()) return;
        String keyPref = (currentUnivId != null ? currentUnivId.trim() : "default");
        android.content.SharedPreferences prefs = getSharedPreferences("EmojiPrefs_" + keyPref, android.content.Context.MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();

        // 1. Record Recent
        List<String> recents = getRecentReactions();
        recents.remove(emoji);
        recents.add(0, emoji);
        if (recents.size() > 20) {
            recents = recents.subList(0, 20);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < recents.size(); i++) {
            sb.append(recents.get(i));
            if (i < recents.size() - 1) sb.append(",");
        }
        editor.putString(PREF_RECENT_REACTIONS, sb.toString());

        // 2. Record Frequency
        String freqKey = PREF_FREQUENT_REACTIONS + "_" + emoji;
        int count = prefs.getInt(freqKey, 0);
        editor.putInt(freqKey, count + 1);

        editor.apply();
    }

    private boolean toggleFavoriteEmoji(String emoji) {
        if (emoji == null || emoji.isEmpty()) return false;
        String keyPref = (currentUnivId != null ? currentUnivId.trim() : "default");
        android.content.SharedPreferences prefs = getSharedPreferences("EmojiPrefs_" + keyPref, android.content.Context.MODE_PRIVATE);
        List<String> favorites = getFavoriteReactions();
        boolean added = false;
        if (favorites.contains(emoji)) {
            favorites.remove(emoji);
        } else {
            favorites.add(emoji);
            added = true;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < favorites.size(); i++) {
            sb.append(favorites.get(i));
            if (i < favorites.size() - 1) sb.append(",");
        }
        prefs.edit().putString(PREF_FAVORITE_REACTIONS, sb.toString()).apply();
        return added;
    }

    private boolean isCustomizeMode = false;
    private int selectedSlotIndex = -1;
    private List<String> tempQuickReactions = new ArrayList<>();
    private TextView[] slotViews = new TextView[6];

    private void showEmojiPickerDialog(Message msg) {
        isCustomizeMode = false;
        selectedSlotIndex = -1;
        tempQuickReactions = new ArrayList<>(getQuickReactionsForUser(currentUnivId));

        com.google.android.material.bottomsheet.BottomSheetDialog pickerDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_emoji_picker_dialog, null);
        pickerDialog.setContentView(view);

        com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior = 
            com.google.android.material.bottomsheet.BottomSheetBehavior.from((View) view.getParent());
        behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
        behavior.setSkipCollapsed(true);

        EditText etSearch = view.findViewById(R.id.et_emoji_search);
        TextView btnCustomize = view.findViewById(R.id.btn_customize_reactions);
        androidx.recyclerview.widget.RecyclerView rvEmojis = view.findViewById(R.id.rv_emoji_categories);

        slotViews[0] = view.findViewById(R.id.slot_0);
        slotViews[1] = view.findViewById(R.id.slot_1);
        slotViews[2] = view.findViewById(R.id.slot_2);
        slotViews[3] = view.findViewById(R.id.slot_3);
        slotViews[4] = view.findViewById(R.id.slot_4);
        slotViews[5] = view.findViewById(R.id.slot_5);

        Runnable updateSlotsUI = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 6; i++) {
                    TextView tv = slotViews[i];
                    if (tv == null) continue;
                    tv.setText(tempQuickReactions.get(i));
                    
                    if (isCustomizeMode) {
                        if (i == selectedSlotIndex) {
                            tv.setBackgroundResource(R.drawable.bg_emoji_slot_selected);
                            tv.animate().scaleX(1.15f).scaleY(1.15f).setDuration(150).start();
                        } else {
                            tv.setBackgroundResource(R.drawable.bg_emoji_slot_customizing);
                            tv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                        }
                    } else {
                        tv.setBackgroundResource(R.drawable.bg_emoji_slot);
                        tv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                    }
                }
            }
        };

        updateSlotsUI.run();

        for (int i = 0; i < 6; i++) {
            final int slotIndex = i;
            TextView tv = slotViews[i];
            if (tv != null) {
                tv.setOnClickListener(v -> {
                    if (isCustomizeMode) {
                        if (selectedSlotIndex == slotIndex) return;
                        
                        String temp = tempQuickReactions.get(selectedSlotIndex);
                        tempQuickReactions.set(selectedSlotIndex, tempQuickReactions.get(slotIndex));
                        tempQuickReactions.set(slotIndex, temp);
                        
                        tv.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction(() -> {
                            tv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                        }).start();
                        
                        TextView tvPrev = slotViews[selectedSlotIndex];
                        if (tvPrev != null) {
                            tvPrev.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction(() -> {
                                tvPrev.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                            }).start();
                        }
                        
                        selectedSlotIndex = slotIndex;
                        updateSlotsUI.run();
                    } else {
                        String emojiText = tempQuickReactions.get(slotIndex);
                        tv.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction(() -> {
                            tv.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).withEndAction(() -> {
                                addOrUpdateReaction(msg, emojiText);
                                pickerDialog.dismiss();
                            }).start();
                        }).start();
                    }
                });
            }
        }

        btnCustomize.setOnClickListener(v -> {
            btnCustomize.animate().scaleX(1.1f).scaleY(1.1f).setDuration(80).withEndAction(() -> {
                btnCustomize.animate().scaleX(1.0f).scaleY(1.0f).setDuration(80).start();
            }).start();

            if (!isCustomizeMode) {
                isCustomizeMode = true;
                selectedSlotIndex = 0;
                btnCustomize.setText("DONE");
                btnCustomize.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.ep_accent));
                updateSlotsUI.run();
            } else {
                isCustomizeMode = false;
                selectedSlotIndex = -1;
                btnCustomize.setText("CUSTOMIZE");
                btnCustomize.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.ep_accent));
                saveQuickReactionsForUser(currentUnivId, tempQuickReactions);
                updateSlotsUI.run();
            }
        });

        EmojiPickerAdapter adapter = new EmojiPickerAdapter(this, emoji -> {
            if (isCustomizeMode) {
                int dupIndex = tempQuickReactions.indexOf(emoji);
                if (dupIndex != -1) {
                    String temp = tempQuickReactions.get(selectedSlotIndex);
                    tempQuickReactions.set(selectedSlotIndex, emoji);
                    tempQuickReactions.set(dupIndex, temp);
                } else {
                    tempQuickReactions.set(selectedSlotIndex, emoji);
                }
                
                TextView tvActive = slotViews[selectedSlotIndex];
                if (tvActive != null) {
                    tvActive.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction(() -> {
                        tvActive.animate().scaleX(1.15f).scaleY(1.15f).setDuration(100).start();
                    }).start();
                }

                selectedSlotIndex = (selectedSlotIndex + 1) % 6;
                updateSlotsUI.run();
            } else {
                addOrUpdateReaction(msg, emoji);
                pickerDialog.dismiss();
            }
        });

        androidx.recyclerview.widget.GridLayoutManager glm = new androidx.recyclerview.widget.GridLayoutManager(this, 7);
        glm.setSpanSizeLookup(new androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter.getItemViewType(position) == EmojiPickerAdapter.TYPE_HEADER) {
                    return 7;
                }
                return 1;
            }
        });

        rvEmojis.setLayoutManager(glm);
        rvEmojis.setAdapter(adapter);

        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String query = s.toString();
                rvEmojis.animate().alpha(0f).setDuration(80).withEndAction(() -> {
                    adapter.filter(query);
                    rvEmojis.animate().alpha(1f).setDuration(120).start();
                }).start();
            }
        });

        view.findViewById(R.id.tab_recent).setOnClickListener(v -> scrollToCategory(rvEmojis, adapter, glm, "🕘 Recently Used"));
        view.findViewById(R.id.tab_frequent).setOnClickListener(v -> scrollToCategory(rvEmojis, adapter, glm, "💯 Frequently Used"));
        view.findViewById(R.id.tab_favorites).setOnClickListener(v -> scrollToCategory(rvEmojis, adapter, glm, "⭐ Favorites"));
        view.findViewById(R.id.tab_smileys).setOnClickListener(v -> scrollToCategory(rvEmojis, adapter, glm, "😀 Smileys & Emotion"));
        view.findViewById(R.id.tab_people).setOnClickListener(v -> scrollToCategory(rvEmojis, adapter, glm, "👋 People & Body"));
        view.findViewById(R.id.tab_animals).setOnClickListener(v -> scrollToCategory(rvEmojis, adapter, glm, "🐶 Animals & Nature"));
        view.findViewById(R.id.tab_food).setOnClickListener(v -> scrollToCategory(rvEmojis, adapter, glm, "🍔 Food & Drink"));
        view.findViewById(R.id.tab_travel).setOnClickListener(v -> scrollToCategory(rvEmojis, adapter, glm, "🌍 Travel & Places"));
        view.findViewById(R.id.tab_activities).setOnClickListener(v -> scrollToCategory(rvEmojis, adapter, glm, "⚽ Activities"));
        view.findViewById(R.id.tab_objects).setOnClickListener(v -> scrollToCategory(rvEmojis, adapter, glm, "📦 Objects"));
        view.findViewById(R.id.tab_symbols).setOnClickListener(v -> scrollToCategory(rvEmojis, adapter, glm, "🔣 Symbols"));
        view.findViewById(R.id.tab_flags).setOnClickListener(v -> scrollToCategory(rvEmojis, adapter, glm, "🏳️ Flags"));
        view.findViewById(R.id.tab_component).setOnClickListener(v -> scrollToCategory(rvEmojis, adapter, glm, "👨‍👩‍👧‍👦 Component"));

        pickerDialog.show();
    }

    private void scrollToCategory(androidx.recyclerview.widget.RecyclerView rv, EmojiPickerAdapter adapter, androidx.recyclerview.widget.GridLayoutManager glm, String categoryName) {
        int pos = adapter.getPositionForCategory(categoryName);
        if (pos != -1) {
            glm.scrollToPositionWithOffset(pos, 0);
        }
    }

    private static class EmojiPickerAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder> {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_EMOJI = 1;

        public interface OnEmojiClickListener {
            void onEmojiClick(String emoji);
        }

        private final android.content.Context context;
        private final OnEmojiClickListener listener;
        
        private final List<Object> allItems = new ArrayList<>();
        private final List<Object> displayItems = new ArrayList<>();

        public EmojiPickerAdapter(android.content.Context context, OnEmojiClickListener listener) {
            this.context = context;
            this.listener = listener;
            rebuildAllItems();
        }

        public void rebuildAllItems() {
            allItems.clear();
            
            // 1. Add Recent
            List<String> recents = ((ChatActivity) context).getRecentReactions();
            if (!recents.isEmpty()) {
                allItems.add("🕘 Recently Used");
                for (String emoji : recents) {
                    allItems.add(new EmojiData.EmojiItem(emoji, "recent", "🕘 Recently Used"));
                }
            }

            // 2. Add Frequent
            List<String> frequents = ((ChatActivity) context).getFrequentReactions();
            if (!frequents.isEmpty()) {
                allItems.add("💯 Frequently Used");
                for (String emoji : frequents) {
                    allItems.add(new EmojiData.EmojiItem(emoji, "frequent", "💯 Frequently Used"));
                }
            }

            // 3. Add Favorites
            List<String> favorites = ((ChatActivity) context).getFavoriteReactions();
            if (!favorites.isEmpty()) {
                allItems.add("⭐ Favorites");
                for (String emoji : favorites) {
                    allItems.add(new EmojiData.EmojiItem(emoji, "favorite", "⭐ Favorites"));
                }
            }

            // 4. Add standard categories
            for (String category : EmojiData.CATEGORIES) {
                allItems.add(category);
                for (EmojiData.EmojiItem item : EmojiData.getAllEmojis()) {
                    if (item.category.equals(category)) {
                        allItems.add(item);
                    }
                }
            }
            displayItems.clear();
            displayItems.addAll(allItems);
            notifyDataSetChanged();
        }

        public void filter(String query) {
            displayItems.clear();
            if (query == null || query.trim().isEmpty()) {
                displayItems.addAll(allItems);
            } else {
                for (EmojiData.EmojiItem item : EmojiData.getAllEmojis()) {
                    if (item.matches(query)) {
                        displayItems.add(item);
                    }
                }
            }
            notifyDataSetChanged();
        }

        public int getPositionForCategory(String categoryName) {
            for (int i = 0; i < displayItems.size(); i++) {
                Object item = displayItems.get(i);
                if (item instanceof String && ((String) item).equals(categoryName)) {
                    return i;
                }
            }
            return -1;
        }

        @Override
        public int getItemViewType(int position) {
            if (displayItems.get(position) instanceof String) {
                return TYPE_HEADER;
            }
            return TYPE_EMOJI;
        }

        @androidx.annotation.NonNull
        @Override
        public androidx.recyclerview.widget.RecyclerView.ViewHolder onCreateViewHolder(@androidx.annotation.NonNull android.view.ViewGroup parent, int viewType) {
            android.view.LayoutInflater inflater = android.view.LayoutInflater.from(context);
            if (viewType == TYPE_HEADER) {
                android.view.View view = inflater.inflate(R.layout.item_emoji_category_header, parent, false);
                return new HeaderViewHolder(view);
            } else {
                android.view.View view = inflater.inflate(R.layout.item_emoji_cell, parent, false);
                return new EmojiViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@androidx.annotation.NonNull androidx.recyclerview.widget.RecyclerView.ViewHolder holder, int position) {
            Object item = displayItems.get(position);
            if (holder instanceof HeaderViewHolder) {
                ((HeaderViewHolder) holder).tvTitle.setText((String) item);
            } else if (holder instanceof EmojiViewHolder) {
                EmojiData.EmojiItem emojiItem = (EmojiData.EmojiItem) item;
                EmojiViewHolder evh = (EmojiViewHolder) holder;
                evh.tvEmoji.setText(emojiItem.emoji);
                evh.tvEmoji.setOnClickListener(v -> {
                    evh.tvEmoji.animate().scaleX(1.3f).scaleY(1.3f).setDuration(100).withEndAction(() -> {
                        evh.tvEmoji.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).withEndAction(() -> {
                            listener.onEmojiClick(emojiItem.emoji);
                        }).start();
                    }).start();
                });
                
                evh.tvEmoji.setOnLongClickListener(v -> {
                    boolean added = ((ChatActivity) context).toggleFavoriteEmoji(emojiItem.emoji);
                    android.widget.Toast.makeText(context, added ? "Added to Favorites" : "Removed from Favorites", android.widget.Toast.LENGTH_SHORT).show();
                    rebuildAllItems();
                    return true;
                });
            }
        }

        @Override
        public int getItemCount() {
            return displayItems.size();
        }

        private static class HeaderViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            final android.widget.TextView tvTitle;
            HeaderViewHolder(android.view.View view) {
                super(view);
                tvTitle = (android.widget.TextView) view;
            }
        }

        private static class EmojiViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            final android.widget.TextView tvEmoji;
            EmojiViewHolder(android.view.View view) {
                super(view);
                tvEmoji = view.findViewById(R.id.tv_emoji);
            }
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
                    intent.putExtra("itemAdminStatus", item.getAdminStatus());
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
        private static final int TYPE_SYSTEM_MESSAGE = 3;

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

        public void notifyMessageChanged(Message msg) {
            if (msg == null || msg.getId() == null) return;
            // Local sync of message data in the adapter's backing list
            for (int i = 0; i < list.size(); i++) {
                Message m = list.get(i);
                if (m != null && m.getId().equals(msg.getId())) {
                    m.setMessageText(msg.getMessageText());
                    break;
                }
            }
            rebuildDisplayItems();
            for (int i = 0; i < displayItems.size(); i++) {
                Object item = displayItems.get(i);
                if (item instanceof Message) {
                    Message m = (Message) item;
                    if (m.getId().equals(msg.getId())) {
                        notifyItemChanged(i);
                        break;
                    }
                }
            }
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
                if (isDeletedForCurrentUser(msg)) {
                    continue;
                }

                String text = msg.getMessageText();
                MessageMeta meta = MessageMeta.parseMeta(text);
                if (meta != null && meta.isSystemMessage) {
                    if (currentUserId != null && currentUserId.equals(meta.blockerId)) {
                        displayItems.add(msg);
                    }
                    continue;
                }

                Date msgDate = parseDate(msg.getCreatedAt());
                if (lastDate == null || !isSameDay(lastDate, msgDate)) {
                    displayItems.add(getDayLabel(msgDate));
                    lastDate = msgDate;
                }
                displayItems.add(msg);
            }
        }

        private boolean isDeletedForCurrentUser(Message msg) {
            String text = msg.getMessageText();
            MessageMeta meta = MessageMeta.parseMeta(text);
            if (meta != null && meta.deletedForUsers != null) {
                return meta.deletedForUsers.contains(currentUserId);
            }
            return false;
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
                String text = msg.getMessageText();
                MessageMeta meta = MessageMeta.parseMeta(text);
                if (meta != null && meta.isSystemMessage) {
                    return TYPE_SYSTEM_MESSAGE;
                }

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
            } else if (viewType == TYPE_SYSTEM_MESSAGE) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_system_message, parent, false);
                return new SystemMessageViewHolder(v);
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
            } else if (holder instanceof SystemMessageViewHolder) {
                SystemMessageViewHolder sysHolder = (SystemMessageViewHolder) holder;
                Message msg = (Message) displayItems.get(position);
                String rawText = msg.getMessageText();
                MessageMeta meta = MessageMeta.parseMeta(rawText);
                if (meta != null && meta.isSystemMessage) {
                    if ("block".equals(meta.systemType)) {
                        sysHolder.tvSystemMessage.setText("You blocked this person. Tap to unblock.");
                        sysHolder.itemView.setOnClickListener(v -> {
                            if (context instanceof ChatActivity) {
                                ((ChatActivity) context).toggleUserBlock();
                            }
                        });
                        sysHolder.tvSystemMessage.setOnClickListener(v -> {
                            if (context instanceof ChatActivity) {
                                ((ChatActivity) context).toggleUserBlock();
                            }
                        });
                    } else if ("unblock".equals(meta.systemType)) {
                        sysHolder.tvSystemMessage.setText("You unblocked this person.");
                        sysHolder.itemView.setOnClickListener(null);
                        sysHolder.tvSystemMessage.setOnClickListener(null);
                    }
                }
            } else if (holder instanceof MessageViewHolder) {
                MessageViewHolder msgHolder = (MessageViewHolder) holder;
                Message msg = (Message) displayItems.get(position);
                float density = context.getResources().getDisplayMetrics().density;
                
                String rawText = msg.getMessageText();
                String displayText = rawText;
                String replyToId = null;
                String replyToText = null;
                String replyToSenderName = null;
                Map<String, String> reactions = null;
                boolean isUnsent = false;

                MessageMeta meta = MessageMeta.parseMeta(rawText);
                if (meta != null) {
                    displayText = meta.text;
                    replyToId = meta.replyToId;
                    replyToText = meta.replyToText;
                    replyToSenderName = meta.replyToSenderName;
                    reactions = meta.reactions;
                    isUnsent = meta.isUnsent;
                }

                int viewType = getItemViewType(position);

                if (isUnsent) {
                    msgHolder.tvMessageText.setText(viewType == TYPE_MESSAGE_SENDER ? "You unsent a message" : "This message was unsent");
                    msgHolder.tvMessageText.setTypeface(null, android.graphics.Typeface.ITALIC);
                } else {
                    msgHolder.tvMessageText.setTypeface(null, android.graphics.Typeface.NORMAL);
                    if (displayText != null && displayText.contains("@")) {
                        android.text.SpannableStringBuilder ssb = new android.text.SpannableStringBuilder(displayText);
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@([a-zA-Z0-9_.-]+)");
                        java.util.regex.Matcher matcher = pattern.matcher(ssb);
                        boolean hasMentions = false;
                        float cornerRadius = 6 * density;
                        float paddingLeftRight = 6 * density;
                        float paddingTopBottom = 2 * density;
                        while (matcher.find()) {
                            int start = matcher.start();
                            int end = matcher.end();
                            hasMentions = true;
                            
                            int mentionBg = androidx.core.content.ContextCompat.getColor(context, R.color.mention_bg);
                            int mentionTextColor = androidx.core.content.ContextCompat.getColor(context, R.color.mention_text);
                            
                            ssb.setSpan(new RoundedBackgroundSpan(mentionBg, mentionTextColor, cornerRadius, paddingLeftRight, paddingTopBottom), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        if (hasMentions) {
                            msgHolder.tvMessageText.setText(ssb);
                        } else {
                            msgHolder.tvMessageText.setText(displayText);
                        }
                    } else {
                        msgHolder.tvMessageText.setText(displayText);
                    }
                }

                msgHolder.tvMessageTime.setText(formatTime(msg.getCreatedAt()));

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
                    msgHolder.tvMessageText.setTextColor(androidx.core.content.ContextCompat.getColor(context, isUnsent ? R.color.msg_sender_unsent_text : R.color.msg_sender_text));
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
                        if (msg.isReceiverMarkedUnread()) {
                            msgHolder.ivMessageStatus.setImageResource(R.drawable.ic_double_check);
                            msgHolder.ivMessageStatus.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.msg_sender_status_delivered)));
                        } else if (msg.isRead()) {
                            msgHolder.ivMessageStatus.setImageResource(R.drawable.ic_double_check);
                            msgHolder.ivMessageStatus.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.msg_sender_status_read)));
                        } else if (msg.isDelivered()) {
                            msgHolder.ivMessageStatus.setImageResource(R.drawable.ic_double_check);
                            msgHolder.ivMessageStatus.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.msg_sender_status_delivered)));
                        } else {
                            msgHolder.ivMessageStatus.setImageResource(R.drawable.ic_single_check);
                            msgHolder.ivMessageStatus.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.msg_sender_status_sent)));
                        }
                    }
                } else {
                    // Receiver message
                    lp.addRule(RelativeLayout.END_OF, R.id.ivUserAvatar);
                    lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
                    lp.removeRule(RelativeLayout.ALIGN_PARENT_START);
                    lp.setMargins(0, 0, (int) (64 * density), 0);

                    msgHolder.cardMessageBubble.setCardBackgroundColor(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.msg_receiver_bg)));
                    msgHolder.tvMessageText.setTextColor(androidx.core.content.ContextCompat.getColor(context, isUnsent ? R.color.msg_receiver_unsent_text : R.color.msg_receiver_text));
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
                                .placeholder(R.drawable.ic_user_placeholder_white)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .circleCrop()
                                .into(msgHolder.ivUserAvatar);
                    } else {
                        msgHolder.ivUserAvatar.setImageResource(R.drawable.ic_user_placeholder_white);
                        msgHolder.ivUserAvatar.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.white)));
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

                // Render Reply Preview inside Bubble (For both sender and receiver)
                if (replyToId != null && !isUnsent) {
                    msgHolder.layoutReplyBubble.setVisibility(View.VISIBLE);
                    msgHolder.tvReplyBubbleName.setText(replyToSenderName != null ? replyToSenderName : "Reply");
                    
                    // Set reply bubble background dynamically from shape drawables (Day/Night responsive)
                    if (viewType == TYPE_MESSAGE_SENDER) {
                        msgHolder.layoutReplyBubble.setBackgroundResource(R.drawable.bg_reply_bubble_sender);
                    } else {
                        msgHolder.layoutReplyBubble.setBackgroundResource(R.drawable.bg_reply_bubble_receiver);
                    }
                    
                    boolean isNightMode = (context.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                            == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                    if (!isNightMode) {
                        if (viewType == TYPE_MESSAGE_SENDER) {
                            msgHolder.tvReplyBubbleName.setTextColor(android.graphics.Color.WHITE);
                            msgHolder.tvReplyBubbleText.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.msg_sender_time));
                        } else {
                            msgHolder.tvReplyBubbleName.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.mention_text));
                            msgHolder.tvReplyBubbleText.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.msg_receiver_time));
                        }
                    } else {
                        msgHolder.tvReplyBubbleName.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.messenger_accent));
                        msgHolder.tvReplyBubbleText.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.messenger_text_secondary));
                    }

                    // Parse and format mentions inside replied message text preview
                    if (replyToText != null && replyToText.contains("@")) {
                        android.text.SpannableStringBuilder replySsb = new android.text.SpannableStringBuilder(replyToText);
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("@([a-zA-Z0-9_.-]+)");
                        java.util.regex.Matcher matcher = pattern.matcher(replySsb);
                        boolean hasMentions = false;
                        float cornerRadius = 6 * density;
                        float paddingLeftRight = 6 * density;
                        float paddingTopBottom = 2 * density;
                        while (matcher.find()) {
                            int start = matcher.start();
                            int end = matcher.end();
                            hasMentions = true;
                            
                            int mentionBg = androidx.core.content.ContextCompat.getColor(context, R.color.mention_bg);
                            int mentionTextColor = androidx.core.content.ContextCompat.getColor(context, R.color.mention_text);
                            
                            replySsb.setSpan(new RoundedBackgroundSpan(mentionBg, mentionTextColor, cornerRadius, paddingLeftRight, paddingTopBottom), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        if (hasMentions) {
                            msgHolder.tvReplyBubbleText.setText(replySsb);
                        } else {
                            msgHolder.tvReplyBubbleText.setText(replyToText);
                        }
                    } else {
                        msgHolder.tvReplyBubbleText.setText(replyToText);
                    }
                    
                    final String finalReplyToId = replyToId;
                    msgHolder.layoutReplyBubble.setOnClickListener(v -> {
                        if (context instanceof ChatActivity) {
                            ((ChatActivity) context).scrollToMessage(finalReplyToId);
                        }
                    });
                } else {
                    msgHolder.layoutReplyBubble.setVisibility(View.GONE);
                }

                // Render Reactions bubble (For both sender and receiver)
                if (reactions != null && !reactions.isEmpty() && !isUnsent) {
                    StringBuilder emojisStr = new StringBuilder();
                    Set<String> uniqueEmojis = new HashSet<>(reactions.values());
                    int idx = 0;
                    for (String emoji : uniqueEmojis) {
                        if (idx > 0) emojisStr.append(" ");
                        emojisStr.append(emoji);
                        idx++;
                    }
                    msgHolder.cardReactionsContainer.setVisibility(View.VISIBLE);
                    msgHolder.tvReactionsEmojis.setText(emojisStr.toString());
                    msgHolder.tvReactionsCount.setText(String.valueOf(reactions.size()));

                    RelativeLayout.LayoutParams reactionsLp = (RelativeLayout.LayoutParams) msgHolder.cardReactionsContainer.getLayoutParams();
                    if (viewType == TYPE_MESSAGE_SENDER) {
                        reactionsLp.addRule(RelativeLayout.ALIGN_END, R.id.cardMessageBubble);
                        reactionsLp.removeRule(RelativeLayout.ALIGN_START);
                        reactionsLp.setMargins(0, (int)(-6 * density), (int)(12 * density), 0);
                    } else {
                        reactionsLp.addRule(RelativeLayout.ALIGN_START, R.id.cardMessageBubble);
                        reactionsLp.removeRule(RelativeLayout.ALIGN_END);
                        reactionsLp.setMargins((int)(12 * density), (int)(-6 * density), 0, 0);
                    }
                    msgHolder.cardReactionsContainer.setLayoutParams(reactionsLp);
                    msgHolder.cardReactionsContainer.setOnClickListener(v -> {
                        if (context instanceof ChatActivity) {
                            ((ChatActivity) context).showReactionDetails(msg);
                        }
                    });
                } else {
                    msgHolder.cardReactionsContainer.setVisibility(View.GONE);
                }

                // Long press popup actions bottom sheet (For both sender and receiver)
                if (!isUnsent) {
                    msgHolder.cardMessageBubble.setOnLongClickListener(v -> {
                        if (context instanceof ChatActivity) {
                            ((ChatActivity) context).showMessageActionsSheet(msg);
                        }
                        return true;
                    });
                } else {
                    // Allow delete for you even on unsent messages
                    msgHolder.cardMessageBubble.setOnLongClickListener(v -> {
                        if (context instanceof ChatActivity) {
                            ((ChatActivity) context).showDeleteOptionsDialog(msg);
                        }
                        return true;
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

            View layoutReplyBubble;
            TextView tvReplyBubbleName, tvReplyBubbleText;
            View cardReactionsContainer;
            TextView tvReactionsEmojis, tvReactionsCount;

            public MessageViewHolder(@NonNull View itemView) {
                super(itemView);
                ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
                cardMessageBubble = itemView.findViewById(R.id.cardMessageBubble);
                tvMessageText = itemView.findViewById(R.id.tvMessageText);
                tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
                ivMessageStatus = itemView.findViewById(R.id.ivMessageStatus);

                layoutReplyBubble = itemView.findViewById(R.id.layoutReplyBubble);
                tvReplyBubbleName = itemView.findViewById(R.id.tvReplyBubbleName);
                tvReplyBubbleText = itemView.findViewById(R.id.tvReplyBubbleText);
                cardReactionsContainer = itemView.findViewById(R.id.cardReactionsContainer);
                tvReactionsEmojis = itemView.findViewById(R.id.tvReactionsEmojis);
                tvReactionsCount = itemView.findViewById(R.id.tvReactionsCount);
            }
        }

        public static class DateHeaderViewHolder extends RecyclerView.ViewHolder {
            TextView tvDateHeader;

            public DateHeaderViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDateHeader = itemView.findViewById(R.id.tvDateHeader);
            }
        }

        public static class SystemMessageViewHolder extends RecyclerView.ViewHolder {
            TextView tvSystemMessage;

            public SystemMessageViewHolder(@NonNull View itemView) {
                super(itemView);
                tvSystemMessage = itemView.findViewById(R.id.tvSystemMessage);
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

    private BottomSheetDialog activeReactionDetailsDialog = null;
    private String activeReactionDetailsMessageId = null;
    private List<User> activeReactionDetailsUsers = null;
    private Map<String, String> activeReactionDetailsMap = null;
    private String selectedReactionFilter = "All";

    private void showReactionDetails(Message msg) {
        Map<String, String> reactionMap = new HashMap<>();
        MessageMeta meta = MessageMeta.parseMeta(msg.getMessageText());
        if (meta != null && meta.reactions != null) {
            reactionMap.putAll(meta.reactions);
        }
        if (reactionMap.isEmpty()) return;
        
        java.util.List<String> uids = new ArrayList<>(reactionMap.keySet());
        StringBuilder inClause = new StringBuilder("university_id=in.(");
        for (int i = 0; i < uids.size(); i++) {
            if (i > 0) inClause.append(",");
            inClause.append(uids.get(i));
        }
        inClause.append(")");
        
        selectedReactionFilter = "All";
        activeReactionDetailsMessageId = msg.getId();
        activeReactionDetailsMap = reactionMap;
        
        SupabaseDatabaseHelper.select("profiles", inClause.toString(), new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                activeReactionDetailsUsers = buildReactionDetailsUsers(users, reactionMap);
                
                BottomSheetDialog dialog = new BottomSheetDialog(ChatActivity.this);
                View sheetView = getLayoutInflater().inflate(R.layout.layout_reaction_details_bottom_sheet, null);
                dialog.setContentView(sheetView);
                
                activeReactionDetailsDialog = dialog;
                
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setWindowAnimations(com.google.android.material.R.style.Animation_Design_BottomSheetDialog);
                }
                
                dialog.setOnDismissListener(d -> {
                    activeReactionDetailsDialog = null;
                    activeReactionDetailsMessageId = null;
                    activeReactionDetailsUsers = null;
                    activeReactionDetailsMap = null;
                });
                
                dialog.show();
                updateReactionDetailsBottomSheetUI();
            }
            
            @Override
            public void onFailure(String errorMessage) {
                // Fallback to locally built profiles if the database query fails
                activeReactionDetailsUsers = buildReactionDetailsUsers(new ArrayList<>(), reactionMap);
                
                BottomSheetDialog dialog = new BottomSheetDialog(ChatActivity.this);
                View sheetView = getLayoutInflater().inflate(R.layout.layout_reaction_details_bottom_sheet, null);
                dialog.setContentView(sheetView);
                
                activeReactionDetailsDialog = dialog;
                
                if (dialog.getWindow() != null) {
                    dialog.getWindow().setWindowAnimations(com.google.android.material.R.style.Animation_Design_BottomSheetDialog);
                }
                
                dialog.setOnDismissListener(d -> {
                    activeReactionDetailsDialog = null;
                    activeReactionDetailsMessageId = null;
                    activeReactionDetailsUsers = null;
                    activeReactionDetailsMap = null;
                });
                
                dialog.show();
                updateReactionDetailsBottomSheetUI();
            }
        });
    }

    private List<User> buildReactionDetailsUsers(List<User> queriedUsers, Map<String, String> reactionMap) {
        List<User> list = new ArrayList<>();
        if (queriedUsers != null) {
            list.addAll(queriedUsers);
        }
        
        for (String uid : reactionMap.keySet()) {
            if (uid == null) continue;
            boolean found = false;
            for (User u : list) {
                if (u.getUniversityId() != null && u.getUniversityId().trim().equals(uid.trim())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                String targetUid = uid.trim();
                String myId = currentUnivId != null ? currentUnivId.trim() : "";
                String peerId = otherUserId != null ? otherUserId.trim() : "";
                
                if (targetUid.equals(myId)) {
                    SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
                    String name = prefs.getString("cachedUserName", "You");
                    String avatar = prefs.getString("cachedProfileImageUrl", "");
                    
                    String safeName = name.replace("\"", "\\\"");
                    String safeAvatar = avatar != null ? avatar.replace("\"", "\\\"") : "";
                    
                    String json = "{\"university_id\":\"" + targetUid + "\",\"display_name\":\"" + safeName + "\",\"profile_image_url\":\"" + safeAvatar + "\"}";
                    try {
                        User user = new com.google.gson.Gson().fromJson(json, User.class);
                        list.add(user);
                    } catch (Exception ignored) {}
                } else if (targetUid.equals(peerId)) {
                    String name = otherUserName != null ? otherUserName : "User";
                    String avatar = otherUserProfileImageUrl != null ? otherUserProfileImageUrl : "";
                    
                    String safeName = name.replace("\"", "\\\"");
                    String safeAvatar = avatar.replace("\"", "\\\"");
                    
                    String json = "{\"university_id\":\"" + targetUid + "\",\"display_name\":\"" + safeName + "\",\"profile_image_url\":\"" + safeAvatar + "\"}";
                    try {
                        User user = new com.google.gson.Gson().fromJson(json, User.class);
                        list.add(user);
                    } catch (Exception ignored) {}
                } else {
                    String json = "{\"university_id\":\"" + targetUid + "\",\"display_name\":\"User " + (targetUid.length() > 4 ? targetUid.substring(targetUid.length() - 4) : targetUid) + "\",\"profile_image_url\":\"\"}";
                    try {
                        User user = new com.google.gson.Gson().fromJson(json, User.class);
                        list.add(user);
                    } catch (Exception ignored) {}
                }
            }
        }
        return list;
    }

    private void updateReactionDetailsBottomSheetUI() {
        if (activeReactionDetailsDialog == null) return;
        
        View view = activeReactionDetailsDialog.findViewById(R.id.layoutReactionTabs);
        if (view == null) return;
        
        android.widget.LinearLayout layoutTabs = (android.widget.LinearLayout) view;
        RecyclerView rvUsers = activeReactionDetailsDialog.findViewById(R.id.rvReactionUsers);
        View layoutEmpty = activeReactionDetailsDialog.findViewById(R.id.layoutReactionEmpty);
        
        // Build tab list dynamically
        java.util.Set<String> uniqueEmojis = new java.util.LinkedHashSet<>();
        if (activeReactionDetailsMap != null) {
            uniqueEmojis.addAll(activeReactionDetailsMap.values());
        }
        
        java.util.List<String> tabs = new ArrayList<>();
        tabs.add("All");
        tabs.addAll(uniqueEmojis);
        
        if (!tabs.contains(selectedReactionFilter)) {
            selectedReactionFilter = "All";
        }
        
        layoutTabs.removeAllViews();
        float density = getResources().getDisplayMetrics().density;
        
        for (String tabText : tabs) {
            TextView tvTab = new TextView(this);
            tvTab.setText(tabText);
            tvTab.setTextSize(12);
            tvTab.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTab.setGravity(android.view.Gravity.CENTER);
            tvTab.setPadding((int)(12 * density), (int)(6 * density), (int)(12 * density), (int)(6 * density));
            
            android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(0, 0, (int)(8 * density), 0);
            tvTab.setLayoutParams(lp);
            
            if (tabText.equals(selectedReactionFilter)) {
                tvTab.setBackgroundResource(R.drawable.bg_reaction_tab_selected);
                tvTab.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.reaction_details_tab_selected_text));
            } else {
                tvTab.setBackgroundResource(R.drawable.bg_reaction_tab_normal);
                tvTab.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.reaction_details_tab_text));
            }
            
            android.util.TypedValue outValue = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            tvTab.setForeground(androidx.core.content.ContextCompat.getDrawable(this, outValue.resourceId));
            tvTab.setClickable(true);
            tvTab.setFocusable(true);
            
            tvTab.setOnClickListener(v -> {
                selectedReactionFilter = tabText;
                updateReactionDetailsBottomSheetUI();
            });
            
            layoutTabs.addView(tvTab);
        }
        
        List<User> filteredUsers = new ArrayList<>();
        if (activeReactionDetailsUsers != null) {
            for (User u : activeReactionDetailsUsers) {
                String uId = u.getUniversityId() != null ? u.getUniversityId().trim() : "";
                String emoji = null;
                if (activeReactionDetailsMap != null) {
                    emoji = activeReactionDetailsMap.get(uId);
                    if (emoji == null) {
                        for (Map.Entry<String, String> entry : activeReactionDetailsMap.entrySet()) {
                            if (entry.getKey() != null && entry.getKey().trim().equals(uId)) {
                                emoji = entry.getValue();
                                break;
                            }
                        }
                    }
                }
                if ("All".equals(selectedReactionFilter) || (emoji != null && emoji.equals(selectedReactionFilter))) {
                    filteredUsers.add(u);
                }
            }
        }
        
        if (filteredUsers.isEmpty()) {
            if (rvUsers != null) rvUsers.setVisibility(View.GONE);
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
        } else {
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            if (rvUsers != null) {
                rvUsers.setVisibility(View.VISIBLE);
                ReactionUsersAdapter userAdapter = new ReactionUsersAdapter(filteredUsers, activeReactionDetailsMap);
                rvUsers.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
                rvUsers.setAdapter(userAdapter);
            }
        }
    }

    private void refreshActiveReactionDetails(Map<String, String> newReactions) {
        activeReactionDetailsMap = newReactions;
        if (newReactions.isEmpty()) {
            if (activeReactionDetailsDialog != null) {
                activeReactionDetailsDialog.dismiss();
            }
            return;
        }
        
        java.util.List<String> uids = new ArrayList<>(newReactions.keySet());
        StringBuilder inClause = new StringBuilder("university_id=in.(");
        for (int i = 0; i < uids.size(); i++) {
            if (i > 0) inClause.append(",");
            inClause.append(uids.get(i));
        }
        inClause.append(")");
        
        SupabaseDatabaseHelper.select("profiles", inClause.toString(), new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (activeReactionDetailsDialog != null && activeReactionDetailsDialog.isShowing()) {
                    activeReactionDetailsUsers = buildReactionDetailsUsers(users, newReactions);
                    updateReactionDetailsBottomSheetUI();
                }
            }
            @Override
            public void onFailure(String errorMessage) {
                if (activeReactionDetailsDialog != null && activeReactionDetailsDialog.isShowing()) {
                    activeReactionDetailsUsers = buildReactionDetailsUsers(new ArrayList<>(), newReactions);
                    updateReactionDetailsBottomSheetUI();
                }
            }
        });
    }

    private class ReactionUsersAdapter extends RecyclerView.Adapter<ReactionUsersAdapter.ViewHolder> {
        private final List<User> users;
        private final Map<String, String> reactionMap;

        public ReactionUsersAdapter(List<User> users, Map<String, String> reactionMap) {
            this.users = users;
            this.reactionMap = reactionMap;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_reaction_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = users.get(position);
            String uId = user.getUniversityId() != null ? user.getUniversityId().trim() : "";
            
            if (uId.equals(currentUnivId != null ? currentUnivId.trim() : "")) {
                holder.tvName.setText("You");
            } else {
                String name = user.getDisplayName();
                if (name == null || name.trim().isEmpty()) {
                    name = user.getFullName();
                }
                holder.tvName.setText(name);
            }
            
            String emoji = null;
            if (reactionMap != null) {
                emoji = reactionMap.get(uId);
                if (emoji == null) {
                    for (Map.Entry<String, String> entry : reactionMap.entrySet()) {
                        if (entry.getKey() != null && entry.getKey().trim().equals(uId)) {
                            emoji = entry.getValue();
                            break;
                        }
                    }
                }
            }
            holder.tvEmoji.setText(emoji != null ? emoji : "");
            
            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                holder.ivAvatar.setImageTintList(null);
                holder.ivAvatar.setPadding(0, 0, 0, 0);
                GlideApp.with(ChatActivity.this)
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.ic_user_placeholder_white)
                        .circleCrop()
                        .into(holder.ivAvatar);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.ic_user_placeholder_white);
                holder.ivAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(ChatActivity.this, R.color.avatar_icon_tint)));
                float density = ChatActivity.this.getResources().getDisplayMetrics().density;
                int padding = (int) (4 * density);
                holder.ivAvatar.setPadding(padding, padding, padding, padding);
            }
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvName, tvEmoji;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivReactionUserAvatar);
                tvName = itemView.findViewById(R.id.tvReactionUserName);
                tvEmoji = itemView.findViewById(R.id.tvReactionUserEmoji);
            }
        }
    }

    public static class RoundedBackgroundSpan extends android.text.style.ReplacementSpan {
        private final int backgroundColor;
        private final int textColor;
        private final float cornerRadius;
        private final float paddingLeftRight;
        private final float paddingTopBottom;

        public RoundedBackgroundSpan(int backgroundColor, int textColor, float cornerRadius, float paddingLeftRight, float paddingTopBottom) {
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            this.cornerRadius = cornerRadius;
            this.paddingLeftRight = paddingLeftRight;
            this.paddingTopBottom = paddingTopBottom;
        }

        @Override
        public int getSize(@androidx.annotation.NonNull android.graphics.Paint paint, CharSequence text, int start, int end, android.graphics.Paint.FontMetricsInt fm) {
            if (fm != null) {
                fm.ascent = (int) (paint.ascent() - paddingTopBottom);
                fm.descent = (int) (paint.descent() + paddingTopBottom);
                fm.top = fm.ascent;
                fm.bottom = fm.descent;
            }
            return (int) (paint.measureText(text, start, end) + paddingLeftRight * 2);
        }

        @Override
        public void draw(@androidx.annotation.NonNull android.graphics.Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @androidx.annotation.NonNull android.graphics.Paint paint) {
            float textWidth = paint.measureText(text, start, end);
            
            android.graphics.Paint bgPaint = new android.graphics.Paint();
            bgPaint.setAntiAlias(true);
            bgPaint.setColor(backgroundColor);
            
            float rectTop = y + paint.ascent() - paddingTopBottom;
            float rectBottom = y + paint.descent() + paddingTopBottom;
            float rectLeft = x;
            float rectRight = x + textWidth + paddingLeftRight * 2;
            
            android.graphics.RectF rect = new android.graphics.RectF(rectLeft, rectTop, rectRight, rectBottom);
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, bgPaint);
            
            paint.setColor(textColor);
            canvas.drawText(text, start, end, x + paddingLeftRight, y, paint);
        }
    }
}
