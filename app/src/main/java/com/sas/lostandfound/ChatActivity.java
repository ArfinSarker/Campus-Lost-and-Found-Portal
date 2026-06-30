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

    private String conversationId, otherUserId, otherUserName, reportId, itemName;
    private String currentUnivId;
    private boolean isOtherUserBlockedByMe = false;
    private boolean amIBlockedByOtherUser = false;

    private List<Message> messageList = new ArrayList<>();
    private MessagesAdapter adapter;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadMessages(false);
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
        setupRecyclerView();

        btnSendMessageCard.setOnClickListener(v -> sendMessage());
        btnSettings.setOnClickListener(v -> showSettingsMenu(v));
        llHeaderTitle.setOnClickListener(v -> openOtherUserProfile());

        loadMessages(true);
        markMessagesAsRead();
        
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

                    if (tvHeaderStatus != null) {
                        String lastActiveIso = user.getLastActiveAt();
                        String statusText = formatLastActiveStatus(lastActiveIso);
                        if (!android.text.TextUtils.isEmpty(statusText)) {
                            tvHeaderStatus.setText(statusText);
                            tvHeaderStatus.setVisibility(View.VISIBLE);
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
            
            // 2 minutes threshold for "Active Now"
            if (diffMinutes < 2) {
                return "Active Now";
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
        if (isOtherUserBlockedByMe) {
            etMessageInput.setEnabled(false);
            etMessageInput.setText("");
            etMessageInput.setHint("Unblock this user to send messages");
            btnSendMessageCard.setEnabled(false);
            btnSendMessageCard.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#94A3B8")));
        } else if (amIBlockedByOtherUser) {
            etMessageInput.setEnabled(false);
            etMessageInput.setText("");
            etMessageInput.setHint("You cannot send messages to this user");
            btnSendMessageCard.setEnabled(false);
            btnSendMessageCard.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#94A3B8")));
        } else {
            etMessageInput.setEnabled(true);
            etMessageInput.setHint(getString(R.string.hint_type_message));
            btnSendMessageCard.setEnabled(true);
            btnSendMessageCard.setCardBackgroundColor(ColorStateList.valueOf(getResources().getColor(R.color.primaryColor)));
        }
    }

    private void toggleUserBlock() {
        if (currentUnivId == null || otherUserId == null) return;
        
        if (isOtherUserBlockedByMe) {
            // Unblock
            String query = "blocker_id=eq." + currentUnivId + "&blocked_id=eq." + otherUserId;
            SupabaseDatabaseHelper.delete("blocked_users", query, new SupabaseDatabaseHelper.DatabaseCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    isOtherUserBlockedByMe = false;
                    updateChatMessagingUI();
                    SnackbarManager.show(SnackbarManager.Type.SUCCESS, "User unblocked successfully.");
                }
                
                @Override
                public void onFailure(String error) {
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to unblock user: " + error);
                }
            });
        } else {
            // Block
            Map<String, String> blockData = new HashMap<>();
            blockData.put("blocker_id", currentUnivId);
            blockData.put("blocked_id", otherUserId);
            
            SupabaseDatabaseHelper.insert("blocked_users", blockData, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    isOtherUserBlockedByMe = true;
                    updateChatMessagingUI();
                    SnackbarManager.show(SnackbarManager.Type.SUCCESS, "User blocked successfully.");
                }
                
                @Override
                public void onFailure(String error) {
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to block user: " + error);
                }
            });
        }
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

    private void openOtherUserProfile() {
        if (otherUserId == null) {
            SnackbarManager.show(SnackbarManager.Type.ERROR, "User details not loaded yet.");
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
                    adapter.notifyDataSetChanged();
                    
                    if (forceScroll || oldSize == 0) {
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    } else {
                        rvMessages.smoothScrollToPosition(messageList.size() - 1);
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
            @Override public void onSuccess(String result) {}
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

        etMessageInput.setText(""); // Clear input immediately
        Message msg = new Message(conversationId, currentUnivId, text);

        SupabaseDatabaseHelper.insert("messages", msg, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                loadMessages(false);
            }

            @Override
            public void onFailure(String errorMessage) {
                etMessageInput.setText(text);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to send: " + errorMessage);
            }
        });
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
    private static class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {
        private final List<Message> list;
        private final String currentUserId;
        private final Context context;

        public MessagesAdapter(List<Message> list, String currentUserId, Context context) {
            this.list = list;
            this.currentUserId = currentUserId;
            this.context = context;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Message msg = list.get(position);
            holder.tvMessageText.setText(msg.getMessageText());
            holder.tvMessageTime.setText(formatTime(msg.getCreatedAt()));

            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) holder.cardMessageBubble.getLayoutParams();
            if (msg.getSenderId() != null && msg.getSenderId().equals(currentUserId)) {
                lp.addRule(RelativeLayout.ALIGN_PARENT_END);
                lp.removeRule(RelativeLayout.ALIGN_PARENT_START);
                holder.cardMessageBubble.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#0084FF")));
                holder.tvMessageText.setTextColor(Color.WHITE);
                holder.tvMessageTime.setTextColor(Color.parseColor("#D8EEFF"));
                
                if (holder.ivMessageStatus != null) {
                    holder.ivMessageStatus.setVisibility(View.VISIBLE);
                    if (msg.isRead()) {
                        holder.ivMessageStatus.setImageResource(R.drawable.ic_double_check);
                        holder.ivMessageStatus.setImageTintList(ColorStateList.valueOf(Color.parseColor("#00FFFF"))); // Electric Cyan
                    } else if (msg.isDelivered()) {
                        holder.ivMessageStatus.setImageResource(R.drawable.ic_double_check);
                        holder.ivMessageStatus.setImageTintList(ColorStateList.valueOf(Color.parseColor("#D8EEFF"))); // Light Blue Gray
                    } else {
                        holder.ivMessageStatus.setImageResource(R.drawable.ic_single_check);
                        holder.ivMessageStatus.setImageTintList(ColorStateList.valueOf(Color.parseColor("#D8EEFF"))); // Light Blue Gray
                    }
                }
            } else {
                lp.addRule(RelativeLayout.ALIGN_PARENT_START);
                lp.removeRule(RelativeLayout.ALIGN_PARENT_END);
                holder.cardMessageBubble.setCardBackgroundColor(ColorStateList.valueOf(Color.parseColor("#F0F2F5")));
                holder.tvMessageText.setTextColor(Color.parseColor("#0F172A"));
                holder.tvMessageTime.setTextColor(Color.parseColor("#65676B"));
                
                if (holder.ivMessageStatus != null) {
                    holder.ivMessageStatus.setVisibility(View.GONE);
                }
            }
            holder.cardMessageBubble.setLayoutParams(lp);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardMessageBubble;
            TextView tvMessageText, tvMessageTime;
            ImageView ivMessageStatus;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardMessageBubble = itemView.findViewById(R.id.cardMessageBubble);
                tvMessageText = itemView.findViewById(R.id.tvMessageText);
                tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
                ivMessageStatus = itemView.findViewById(R.id.ivMessageStatus);
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
