package com.sas.lostandfound;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Typeface;
import android.graphics.Color;
import java.util.Calendar;
import android.widget.EditText;
import android.content.res.ColorStateList;

public class MessagingActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvConversations;
    private LinearLayout llEmptyState;
    private TextView tvEmptyTitle, tvEmptyDesc;

    private String currentUnivId;

    private List<Conversation> conversationList = new ArrayList<>();
    private ConversationsAdapter conversationsAdapter;

    private View cardSearchBar;
    private EditText etSearchChats;
    private List<Conversation> fullConversationList = new ArrayList<>();
    private final List<String> blockedUserIds = new ArrayList<>();
    private final List<String> usersWhoBlockedMe = new ArrayList<>();

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            loadConversationsSilently();
            pollHandler.postDelayed(this, 5000); // Poll every 5 seconds
        }
    };

    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private Runnable debounceRunnable;

    private final android.content.BroadcastReceiver badgeUpdateReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            reloadConversationsDebounced();
        }
    };

    private void reloadConversationsDebounced() {
        debounceHandler.removeCallbacks(debounceRunnable);
        debounceRunnable = () -> loadConversationsSilently();
        debounceHandler.postDelayed(debounceRunnable, 300); // 300ms debounce
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUnivId = prefs.getString("universityId", null);

        initializeViews();
        setupToolbar();
        setupRecyclerViews();
        setupSwipeRefresh();

        loadConversations();
    }

    @Override
    protected void onStart() {
        super.onStart();
        androidx.core.content.ContextCompat.registerReceiver(
                this,
                badgeUpdateReceiver,
                new android.content.IntentFilter("com.sas.lostandfound.UPDATE_BADGES"),
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(badgeUpdateReceiver);
        } catch (Exception ignored) {}
        debounceHandler.removeCallbacks(debounceRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCurrentTab();
        markChatNotificationsAsRead();
        markAllReceivedMessagesAsDelivered();
        pollHandler.post(pollRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        pollHandler.removeCallbacks(pollRunnable);
    }

    private void markChatNotificationsAsRead() {
        if (currentUnivId == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("is_read", true);

        String query = "recipient_id=eq." + currentUnivId + "&type=in.(chat_request,chat_accepted)&is_read=eq.false";
        SupabaseDatabaseHelper.update("notifications", query, data, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override public void onSuccess(String result) {}
            @Override public void onFailure(String errorMessage) {}
        });
    }

    private void markAllReceivedMessagesAsDelivered() {
        if (currentUnivId == null) return;
        Map<String, Object> params = new HashMap<>();
        params.put("p_user_id", currentUnivId);
        SupabaseDatabaseHelper.rpc("mark_messages_as_delivered", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override public void onSuccess(String result) {}
            @Override public void onFailure(String errorMessage) {}
        });
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvConversations = findViewById(R.id.rvConversations);
        llEmptyState = findViewById(R.id.llEmptyState);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptyDesc = findViewById(R.id.tvEmptyDesc);

        cardSearchBar = findViewById(R.id.cardSearchBar);
        etSearchChats = findViewById(R.id.etSearchChats);
        if (etSearchChats != null) {
            etSearchChats.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    filterConversations(s.toString());
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            int headerColor = androidx.core.content.ContextCompat.getColor(this, R.color.chat_list_header_bar_bg);
            boolean isNightMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            HeaderColorHelper.setup(this, appBarLayout, headerColor, headerColor, !isNightMode);
        }
    }

    private void setupRecyclerViews() {
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        conversationsAdapter = new ConversationsAdapter(conversationList, this, currentUnivId);
        rvConversations.setAdapter(conversationsAdapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.chat_list_loading_indicator));
            swipeRefreshLayout.setOnRefreshListener(this::refreshCurrentTab);
        }
    }

    private void refreshCurrentTab() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        loadConversations();
    }

    private void loadConversations() {
        fetchBlockListsAndLoadConversations(false);
    }

    private void loadConversationsSilently() {
        fetchBlockListsAndLoadConversations(true);
    }

    private void fetchBlockListsAndLoadConversations(boolean silent) {
        if (currentUnivId == null) {
            if (!silent && swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }

        String query = "or=(blocker_id.eq." + currentUnivId + ",blocked_id.eq." + currentUnivId + ")";
        SupabaseDatabaseHelper.select("blocked_users", query, new TypeToken<List<ChatActivity.BlockedRecord>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<ChatActivity.BlockedRecord>>() {
            @Override
            public void onSuccess(List<ChatActivity.BlockedRecord> result) {
                blockedUserIds.clear();
                usersWhoBlockedMe.clear();
                if (result != null) {
                    for (ChatActivity.BlockedRecord record : result) {
                        if (currentUnivId.equals(record.getBlockerId())) {
                            blockedUserIds.add(record.getBlockedId());
                        } else if (currentUnivId.equals(record.getBlockedId())) {
                            usersWhoBlockedMe.add(record.getBlockerId());
                        }
                    }
                }
                if (conversationsAdapter != null) {
                    conversationsAdapter.setBlockLists(blockedUserIds, usersWhoBlockedMe);
                }
                if (silent) {
                    loadConversationsSilentlyFromServer();
                } else {
                    loadConversationsFromServer();
                }
            }

            @Override
            public void onFailure(String error) {
                blockedUserIds.clear();
                usersWhoBlockedMe.clear();
                if (conversationsAdapter != null) {
                    conversationsAdapter.setBlockLists(blockedUserIds, usersWhoBlockedMe);
                }
                if (silent) {
                    loadConversationsSilentlyFromServer();
                } else {
                    loadConversationsFromServer();
                }
            }
        });
    }

    private void loadConversationsFromServer() {
        Map<String, Object> params = new HashMap<>();
        params.put("p_user_id", currentUnivId);

        SupabaseDatabaseHelper.rpc("get_user_conversations", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                try {
                    List<Conversation> list = new com.google.gson.Gson().fromJson(result, new TypeToken<List<Conversation>>(){}.getType());
                    fullConversationList.clear();
                    if (list != null) {
                        fullConversationList.addAll(list);
                    }
                    filterConversations(etSearchChats != null ? etSearchChats.getText().toString() : "");
                } catch (Exception e) {
                    e.printStackTrace();
                    updateEmptyState();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                updateEmptyState();
            }
        });
    }

    private void loadConversationsSilentlyFromServer() {
        Map<String, Object> params = new HashMap<>();
        params.put("p_user_id", currentUnivId);

        SupabaseDatabaseHelper.rpc("get_user_conversations", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    List<Conversation> list = new com.google.gson.Gson().fromJson(result, new TypeToken<List<Conversation>>(){}.getType());
                    if (list != null) {
                        fullConversationList.clear();
                        fullConversationList.addAll(list);
                        filterConversations(etSearchChats != null ? etSearchChats.getText().toString() : "");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override public void onFailure(String errorMessage) {}
        });
    }

    private void filterConversations(String query) {
        conversationList.clear();
        if (query == null || query.trim().isEmpty()) {
            conversationList.addAll(fullConversationList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Conversation c : fullConversationList) {
                if (c.getOtherUserName() != null && c.getOtherUserName().toLowerCase().contains(lowerQuery)) {
                    conversationList.add(c);
                }
            }
        }
        if (conversationsAdapter != null) {
            conversationsAdapter.notifyDataSetChanged();
        }
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (conversationList.isEmpty()) {
            tvEmptyTitle.setText(R.string.no_conversations);
            tvEmptyDesc.setText("Ongoing chats and message requests will appear here.");
            llEmptyState.setVisibility(View.VISIBLE);
        } else {
            llEmptyState.setVisibility(View.GONE);
        }
    }

    // Adapters Section
    private static class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ViewHolder> {
        private final List<Conversation> list;
        private final Context context;
        private final String currentUserId;
        private final List<String> blockedUserIds = new ArrayList<>();
        private final List<String> usersWhoBlockedMe = new ArrayList<>();

        public ConversationsAdapter(List<Conversation> list, Context context, String currentUserId) {
            this.list = list;
            this.context = context;
            this.currentUserId = currentUserId;
        }

        public void setBlockLists(List<String> blockedUserIds, List<String> usersWhoBlockedMe) {
            this.blockedUserIds.clear();
            if (blockedUserIds != null) this.blockedUserIds.addAll(blockedUserIds);
            
            this.usersWhoBlockedMe.clear();
            if (usersWhoBlockedMe != null) this.usersWhoBlockedMe.addAll(usersWhoBlockedMe);
            
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Conversation c = list.get(position);
            holder.tvUserName.setText(c.getOtherUserName());
            
            // Online & Last Active Status calculation
            boolean isBlockedRelationship = blockedUserIds.contains(c.getOtherUserId()) || usersWhoBlockedMe.contains(c.getOtherUserId());
            boolean showActiveStatus = "accepted".equals(c.getRequestStatus()) && !isBlockedRelationship;
            String lastActiveIso = c.getOtherUserLastActive();
            boolean isOnline = false;
            String statusText = "";
            
            if (showActiveStatus) {
                if (lastActiveIso != null && !lastActiveIso.isEmpty()) {
                    try {
                        Date lastActiveDate = ValidationUtils.parseIso8601(lastActiveIso);
                        if (lastActiveDate != null) {
                            long diffMs = System.currentTimeMillis() - lastActiveDate.getTime();
                            if (diffMs < 0) diffMs = 0;
                            long diffMinutes = diffMs / (60 * 1000);
                            
                            if (diffMinutes < 2) {
                                isOnline = true;
                                statusText = "Active Now";
                            } else if (diffMinutes < 60) {
                                statusText = "Last active " + diffMinutes + " minutes ago";
                            } else {
                                Calendar nowCal = Calendar.getInstance();
                                Calendar activeCal = Calendar.getInstance();
                                activeCal.setTime(lastActiveDate);
                                
                                boolean isToday = nowCal.get(Calendar.YEAR) == activeCal.get(Calendar.YEAR) &&
                                                  nowCal.get(Calendar.DAY_OF_YEAR) == activeCal.get(Calendar.DAY_OF_YEAR);
                                                  
                                Calendar yesterdayCal = Calendar.getInstance();
                                yesterdayCal.add(Calendar.DAY_OF_YEAR, -1);
                                boolean isYesterday = yesterdayCal.get(Calendar.YEAR) == activeCal.get(Calendar.YEAR) &&
                                                      yesterdayCal.get(Calendar.DAY_OF_YEAR) == activeCal.get(Calendar.DAY_OF_YEAR);
                                                      
                                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                                
                                if (isToday) {
                                    statusText = "Last active today at " + timeFormat.format(lastActiveDate);
                                } else if (isYesterday) {
                                    statusText = "Last active yesterday";
                                } else {
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                                    statusText = "Last active on " + dateFormat.format(lastActiveDate);
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            } else {
                // Not accepted. If incoming pending/rejected, receiver sees "New Message Request"
                if (!isBlockedRelationship && currentUserId != null && !currentUserId.equals(c.getRequestSenderId())) {
                    statusText = "New Message Request";
                }
            }

            // Bind active status TextView
            if (holder.tvUserStatus != null) {
                if (!statusText.isEmpty()) {
                    holder.tvUserStatus.setText(statusText);
                    holder.tvUserStatus.setVisibility(View.VISIBLE);
                    if (isOnline) {
                        holder.tvUserStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_active_status_text));
                        holder.tvUserStatus.setTypeface(null, Typeface.BOLD);
                    } else if ("New Message Request".equals(statusText)) {
                        holder.tvUserStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_loading_indicator));
                        holder.tvUserStatus.setTypeface(null, Typeface.BOLD);
                    } else {
                        holder.tvUserStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_text_secondary));
                        holder.tvUserStatus.setTypeface(null, Typeface.NORMAL);
                    }
                } else {
                    holder.tvUserStatus.setVisibility(View.GONE);
                }
            }

            // Bind active status green dot indicator
            if (holder.viewActiveIndicator != null) {
                holder.viewActiveIndicator.setVisibility((isOnline && showActiveStatus) ? View.VISIBLE : View.GONE);
            }

            // Bind last message & timestamp
            holder.tvLastMessage.setText(c.getLastMessage() != null ? c.getLastMessage() : "No messages yet");
            holder.tvTimestamp.setText(formatTime(c.getLastMessageTime()));

            // Highlight unread conversations
            if (c.getUnreadCount() > 0) {
                holder.tvLastMessage.setTypeface(null, Typeface.BOLD);
                holder.tvLastMessage.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_text_primary));
                holder.tvTimestamp.setTypeface(null, Typeface.BOLD);
                holder.tvTimestamp.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_loading_indicator));
                
                holder.tvUnreadBadge.setText(String.valueOf(c.getUnreadCount()));
                holder.tvUnreadBadge.setVisibility(View.VISIBLE);
                if (holder.ivLastMessageStatus != null) {
                    holder.ivLastMessageStatus.setVisibility(View.GONE);
                }
            } else {
                holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
                holder.tvLastMessage.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_last_message));
                holder.tvTimestamp.setTypeface(null, Typeface.NORMAL);
                holder.tvTimestamp.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_time));
                
                holder.tvUnreadBadge.setVisibility(View.GONE);
                
                // Show last message status tick (if sent by us)
                if (holder.ivLastMessageStatus != null) {
                    if (c.getLastMessageSenderId() != null && c.getLastMessageSenderId().equals(currentUserId)) {
                        holder.ivLastMessageStatus.setVisibility(View.VISIBLE);
                        if (!usersWhoBlockedMe.contains(c.getOtherUserId()) && c.getLastMessageIsRead() != null && c.getLastMessageIsRead()) {
                            holder.ivLastMessageStatus.setImageResource(R.drawable.ic_double_check);
                            holder.ivLastMessageStatus.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_read_double_tick)));
                        } else if (!usersWhoBlockedMe.contains(c.getOtherUserId()) && c.getLastMessageIsDelivered() != null && c.getLastMessageIsDelivered()) {
                            holder.ivLastMessageStatus.setImageResource(R.drawable.ic_double_check);
                            holder.ivLastMessageStatus.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_double_tick)));
                        } else {
                            holder.ivLastMessageStatus.setImageResource(R.drawable.ic_single_check);
                            holder.ivLastMessageStatus.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_single_tick)));
                        }
                    } else {
                        holder.ivLastMessageStatus.setVisibility(View.GONE);
                    }
                }
            }

            float density = context.getResources().getDisplayMetrics().density;
            if (c.getOtherUserImageUrl() != null && !c.getOtherUserImageUrl().isEmpty()) {
                holder.ivUserAvatar.setImageTintList(null);
                holder.ivUserAvatar.setPadding(0, 0, 0, 0);
                GlideApp.with(context)
                        .load(c.getOtherUserImageUrl())
                        .placeholder(R.drawable.ic_user_placeholder_white)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .into(holder.ivUserAvatar);
            } else {
                holder.ivUserAvatar.setImageResource(R.drawable.ic_user_placeholder_white);
                holder.ivUserAvatar.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.white)));
                int padding = (int) (8 * density);
                holder.ivUserAvatar.setPadding(padding, padding, padding, padding);
            }

            holder.cardUserAvatar.setOnClickListener(v -> {
                holder.itemView.performClick();
            });

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("conversationId", c.getConversationId());
                intent.putExtra("otherUserId", c.getOtherUserId());
                intent.putExtra("otherUserName", c.getOtherUserName());
                intent.putExtra("reportId", c.getReportId());
                intent.putExtra("itemName", c.getItemName());
                intent.putExtra("requestStatus", c.getRequestStatus());
                intent.putExtra("requestSenderId", c.getRequestSenderId());
                intent.putExtra("initialMessage", c.getLastMessage());
                intent.putExtra("requestCreatedAt", c.getLastMessageTime());
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardUserAvatar;
            ImageView ivUserAvatar, ivLastMessageStatus;
            TextView tvUserName, tvUserStatus, tvLastMessage, tvTimestamp, tvUnreadBadge;
            View viewActiveIndicator;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardUserAvatar = itemView.findViewById(R.id.cardUserAvatar);
                ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
                ivLastMessageStatus = itemView.findViewById(R.id.ivLastMessageStatus);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvUserStatus = itemView.findViewById(R.id.tvUserStatus);
                tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
                tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
                tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
                viewActiveIndicator = itemView.findViewById(R.id.viewActiveIndicator);
            }
        }
    }



    private static String formatTime(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        try {
            java.util.Date date = ValidationUtils.parseIso8601(isoTime);
            if (date != null) {
                SimpleDateFormat tf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                tf.setTimeZone(TimeZone.getDefault());
                return tf.format(date);
            }
        } catch (Exception ignored) {}
        return "";
    }

    private static String formatDate(String isoTime) {
        if (isoTime == null || isoTime.isEmpty()) return "";
        try {
            java.util.Date date = ValidationUtils.parseIso8601(isoTime);
            if (date != null) {
                SimpleDateFormat tf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                tf.setTimeZone(TimeZone.getDefault());
                return tf.format(date);
            }
        } catch (Exception ignored) {}
        return "";
    }
}
