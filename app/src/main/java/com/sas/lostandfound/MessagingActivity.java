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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
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
    private TabLayout tabLayout;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvConversations, rvRequests;
    private LinearLayout llEmptyState;
    private TextView tvEmptyTitle, tvEmptyDesc;

    private String currentUnivId;
    private boolean isChatsTabSelected = true;

    private List<Conversation> conversationList = new ArrayList<>();
    private List<ChatRequest> requestList = new ArrayList<>();
    private ConversationsAdapter conversationsAdapter;
    private ChatRequestsAdapter chatRequestsAdapter;

    private View cardSearchBar;
    private EditText etSearchChats;
    private List<Conversation> fullConversationList = new ArrayList<>();

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollRunnable = new Runnable() {
        @Override
        public void run() {
            if (isChatsTabSelected) {
                loadConversationsSilently();
            } else {
                loadChatRequestsSilently();
            }
            pollHandler.postDelayed(this, 5000); // Poll every 5 seconds
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messaging);

        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUnivId = prefs.getString("universityId", null);

        initializeViews();
        setupToolbar();
        setupTabLayout();
        setupRecyclerViews();
        setupSwipeRefresh();

        // Check if redirected from a specific request notification
        boolean selectRequests = getIntent().getBooleanExtra("selectRequestsTab", false);
        if (selectRequests && tabLayout != null && tabLayout.getTabAt(1) != null) {
            tabLayout.getTabAt(1).select();
        } else {
            loadConversations();
        }
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
        Map<String, Object> data = new HashMap<>();
        data.put("is_delivered", true);

        String query = "sender_id=neq." + currentUnivId + "&is_delivered=eq.false";
        SupabaseDatabaseHelper.update("messages", query, data, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override public void onSuccess(String result) {}
            @Override public void onFailure(String errorMessage) {}
        });
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tabLayout = findViewById(R.id.tabLayout);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvConversations = findViewById(R.id.rvConversations);
        rvRequests = findViewById(R.id.rvRequests);
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
            HeaderColorHelper.setup(this, appBarLayout, toolbar);
        }
    }

    private void setupTabLayout() {
        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    isChatsTabSelected = (tab.getPosition() == 0);
                    toggleTabViews();
                    refreshCurrentTab();
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        }
    }

    private void setupRecyclerViews() {
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        conversationsAdapter = new ConversationsAdapter(conversationList, this, currentUnivId);
        rvConversations.setAdapter(conversationsAdapter);

        rvRequests.setLayoutManager(new LinearLayoutManager(this));
        chatRequestsAdapter = new ChatRequestsAdapter(requestList, this);
        rvRequests.setAdapter(chatRequestsAdapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(this::refreshCurrentTab);
        }
    }

    private void toggleTabViews() {
        if (isChatsTabSelected) {
            rvConversations.setVisibility(View.VISIBLE);
            rvRequests.setVisibility(View.GONE);
            if (cardSearchBar != null) {
                cardSearchBar.setVisibility(View.VISIBLE);
            }
        } else {
            rvConversations.setVisibility(View.GONE);
            rvRequests.setVisibility(View.VISIBLE);
            if (cardSearchBar != null) {
                cardSearchBar.setVisibility(View.GONE);
            }
            if (etSearchChats != null) {
                etSearchChats.setText("");
            }
        }
        llEmptyState.setVisibility(View.GONE);
    }

    private void refreshCurrentTab() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        if (isChatsTabSelected) {
            loadConversations();
        } else {
            loadChatRequests();
        }
    }

    private void loadConversations() {
        if (currentUnivId == null) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }

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

    private void loadChatRequests() {
        if (currentUnivId == null) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("p_user_id", currentUnivId);

        SupabaseDatabaseHelper.rpc("get_user_chat_requests", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                try {
                    List<ChatRequest> list = new com.google.gson.Gson().fromJson(result, new TypeToken<List<ChatRequest>>(){}.getType());
                    requestList.clear();
                    if (list != null) {
                        requestList.addAll(list);
                    }
                    chatRequestsAdapter.notifyDataSetChanged();
                    updateEmptyState();
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

    private void loadConversationsSilently() {
        if (currentUnivId == null) return;

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

    private void loadChatRequestsSilently() {
        if (currentUnivId == null) return;

        Map<String, Object> params = new HashMap<>();
        params.put("p_user_id", currentUnivId);

        SupabaseDatabaseHelper.rpc("get_user_chat_requests", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    List<ChatRequest> list = new com.google.gson.Gson().fromJson(result, new TypeToken<List<ChatRequest>>(){}.getType());
                    if (list != null) {
                        requestList.clear();
                        requestList.addAll(list);
                        chatRequestsAdapter.notifyDataSetChanged();
                        updateEmptyState();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override public void onFailure(String errorMessage) {}
        });
    }

    private void updateEmptyState() {
        if (isChatsTabSelected) {
            if (conversationList.isEmpty()) {
                tvEmptyTitle.setText(R.string.no_conversations);
                tvEmptyDesc.setText("Ongoing chats will appear here after reporters accept requests.");
                llEmptyState.setVisibility(View.VISIBLE);
            } else {
                llEmptyState.setVisibility(View.GONE);
            }
        } else {
            if (requestList.isEmpty()) {
                tvEmptyTitle.setText(R.string.no_chat_requests);
                tvEmptyDesc.setText("Chat requests for items with 'In-app Chat' enabled will appear here.");
                llEmptyState.setVisibility(View.VISIBLE);
            } else {
                llEmptyState.setVisibility(View.GONE);
            }
        }
    }

    // Adapters Section
    private static class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ViewHolder> {
        private final List<Conversation> list;
        private final Context context;
        private final String currentUserId;

        public ConversationsAdapter(List<Conversation> list, Context context, String currentUserId) {
            this.list = list;
            this.context = context;
            this.currentUserId = currentUserId;
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
            String lastActiveIso = c.getOtherUserLastActive();
            boolean isOnline = false;
            String statusText = "";
            
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

            // Bind active status TextView
            if (holder.tvUserStatus != null) {
                if (!statusText.isEmpty()) {
                    holder.tvUserStatus.setText(statusText);
                    holder.tvUserStatus.setVisibility(View.VISIBLE);
                    if (isOnline) {
                        holder.tvUserStatus.setTextColor(Color.parseColor("#34C759")); // Messenger Green
                        holder.tvUserStatus.setTypeface(null, Typeface.BOLD);
                    } else {
                        holder.tvUserStatus.setTextColor(context.getResources().getColor(R.color.textSecondary));
                        holder.tvUserStatus.setTypeface(null, Typeface.NORMAL);
                    }
                } else {
                    holder.tvUserStatus.setVisibility(View.GONE);
                }
            }

            // Bind active status green dot indicator
            if (holder.viewActiveIndicator != null) {
                holder.viewActiveIndicator.setVisibility(isOnline ? View.VISIBLE : View.GONE);
            }

            // Bind last message & timestamp
            holder.tvLastMessage.setText(c.getLastMessage() != null ? c.getLastMessage() : "No messages yet");
            holder.tvTimestamp.setText(formatTime(c.getLastMessageTime()));

            // Highlight unread conversations
            if (c.getUnreadCount() > 0) {
                holder.tvLastMessage.setTypeface(null, Typeface.BOLD);
                holder.tvLastMessage.setTextColor(context.getResources().getColor(R.color.textPrimary));
                holder.tvTimestamp.setTypeface(null, Typeface.BOLD);
                holder.tvTimestamp.setTextColor(Color.parseColor("#0084FF")); // Messenger blue color
                
                holder.tvUnreadBadge.setText(String.valueOf(c.getUnreadCount()));
                holder.tvUnreadBadge.setVisibility(View.VISIBLE);
                if (holder.ivLastMessageStatus != null) {
                    holder.ivLastMessageStatus.setVisibility(View.GONE);
                }
            } else {
                holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
                holder.tvLastMessage.setTextColor(context.getResources().getColor(R.color.textSecondary));
                holder.tvTimestamp.setTypeface(null, Typeface.NORMAL);
                holder.tvTimestamp.setTextColor(context.getResources().getColor(R.color.textSecondary));
                
                holder.tvUnreadBadge.setVisibility(View.GONE);
                
                // Show last message status tick (if sent by us)
                if (holder.ivLastMessageStatus != null) {
                    if (c.getLastMessageSenderId() != null && c.getLastMessageSenderId().equals(currentUserId)) {
                        holder.ivLastMessageStatus.setVisibility(View.VISIBLE);
                        if (c.getLastMessageIsRead() != null && c.getLastMessageIsRead()) {
                            holder.ivLastMessageStatus.setImageResource(R.drawable.ic_double_check);
                            holder.ivLastMessageStatus.setImageTintList(ColorStateList.valueOf(Color.parseColor("#0084FF"))); // Messenger Blue
                        } else if (c.getLastMessageIsDelivered() != null && c.getLastMessageIsDelivered()) {
                            holder.ivLastMessageStatus.setImageResource(R.drawable.ic_double_check);
                            holder.ivLastMessageStatus.setImageTintList(ColorStateList.valueOf(Color.parseColor("#8E9AA6"))); // Medium Gray
                        } else {
                            holder.ivLastMessageStatus.setImageResource(R.drawable.ic_single_check);
                            holder.ivLastMessageStatus.setImageTintList(ColorStateList.valueOf(Color.parseColor("#8E9AA6"))); // Medium Gray
                        }
                    } else {
                        holder.ivLastMessageStatus.setVisibility(View.GONE);
                    }
                }
            }

            if (c.getOtherUserImageUrl() != null && !c.getOtherUserImageUrl().isEmpty()) {
                GlideApp.with(context)
                        .load(c.getOtherUserImageUrl())
                        .placeholder(R.drawable.ic_user)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .into(holder.ivUserAvatar);
            } else {
                holder.ivUserAvatar.setImageResource(R.drawable.ic_user);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("conversationId", c.getConversationId());
                intent.putExtra("otherUserId", c.getOtherUserId());
                intent.putExtra("otherUserName", c.getOtherUserName());
                intent.putExtra("reportId", c.getReportId());
                intent.putExtra("itemName", c.getItemName());
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivUserAvatar, ivLastMessageStatus;
            TextView tvUserName, tvUserStatus, tvLastMessage, tvTimestamp, tvUnreadBadge;
            View viewActiveIndicator;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
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

    private static class ChatRequestsAdapter extends RecyclerView.Adapter<ChatRequestsAdapter.ViewHolder> {
        private final List<ChatRequest> list;
        private final MessagingActivity activity;

        public ChatRequestsAdapter(List<ChatRequest> list, MessagingActivity activity) {
            this.list = list;
            this.activity = activity;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_request, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ChatRequest cr = list.get(position);
            boolean isIncoming = cr.getReceiverId().equals(activity.currentUnivId);

            if (isIncoming) {
                holder.tvRequesterName.setText(cr.getSenderName());
                if (cr.getSenderImageUrl() != null && !cr.getSenderImageUrl().isEmpty()) {
                    GlideApp.with(activity)
                            .load(cr.getSenderImageUrl())
                            .placeholder(R.drawable.ic_user)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .circleCrop()
                            .into(holder.ivSenderAvatar);
                } else {
                    holder.ivSenderAvatar.setImageResource(R.drawable.ic_user);
                }
            } else {
                holder.tvRequesterName.setText(cr.getReceiverName() + " (Reporter)");
                holder.ivSenderAvatar.setImageResource(R.drawable.ic_user);
            }

            holder.tvRequestItemBadge.setText(cr.getItemName());
            holder.tvInitialMessage.setText(cr.getInitialMessage());
            holder.tvRequestTime.setText(formatDate(cr.getCreatedAt()));

            // Setup linked report details navigation on click of badge
            holder.tvRequestItemBadge.setOnClickListener(v -> {
                Intent intent = new Intent(activity, ItemDetailActivity.class);
                intent.putExtra("itemId", cr.getReportId());
                intent.putExtra("itemStatus", cr.getItemType());
                intent.putExtra("userId", isIncoming ? cr.getSenderId() : cr.getReceiverId());
                activity.startActivity(intent);
            });

            // Action / Status Visibility
            if ("pending".equalsIgnoreCase(cr.getStatus())) {
                if (isIncoming) {
                    holder.llActionsContainer.setVisibility(View.VISIBLE);
                    holder.tvRequestStatusBadge.setVisibility(View.GONE);

                    holder.btnAcceptRequest.setOnClickListener(v -> activity.acceptRequest(cr));
                    holder.btnRejectRequest.setOnClickListener(v -> activity.rejectRequest(cr));
                } else {
                    holder.llActionsContainer.setVisibility(View.GONE);
                    holder.tvRequestStatusBadge.setText("Pending Reporter Approval");
                    holder.tvRequestStatusBadge.setVisibility(View.VISIBLE);
                }
            } else {
                holder.llActionsContainer.setVisibility(View.GONE);
                if ("accepted".equalsIgnoreCase(cr.getStatus())) {
                    holder.tvRequestStatusBadge.setText("Accepted");
                    holder.tvRequestStatusBadge.setTextColor(activity.getResources().getColor(R.color.statusFound));
                } else {
                    holder.tvRequestStatusBadge.setText("Declined");
                    holder.tvRequestStatusBadge.setTextColor(activity.getResources().getColor(R.color.statusLost));
                }
                holder.tvRequestStatusBadge.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivSenderAvatar;
            TextView tvRequesterName, tvRequestItemBadge, tvInitialMessage, tvRequestTime, tvRequestStatusBadge;
            LinearLayout llActionsContainer;
            MaterialButton btnAcceptRequest, btnRejectRequest;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivSenderAvatar = itemView.findViewById(R.id.ivSenderAvatar);
                tvRequesterName = itemView.findViewById(R.id.tvRequesterName);
                tvRequestItemBadge = itemView.findViewById(R.id.tvRequestItemBadge);
                tvInitialMessage = itemView.findViewById(R.id.tvInitialMessage);
                tvRequestTime = itemView.findViewById(R.id.tvRequestTime);
                tvRequestStatusBadge = itemView.findViewById(R.id.tvRequestStatusBadge);
                llActionsContainer = itemView.findViewById(R.id.llActionsContainer);
                btnAcceptRequest = itemView.findViewById(R.id.btnAcceptRequest);
                btnRejectRequest = itemView.findViewById(R.id.btnRejectRequest);
            }
        }
    }

    private void acceptRequest(ChatRequest cr) {
        if (cr == null) return;
        Map<String, Object> params = new HashMap<>();
        params.put("p_request_id", UUID.fromString(cr.getRequestId()));

        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        SupabaseDatabaseHelper.rpc("accept_chat_request", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                try {
                    // Result will be the conversation ID (UUID)
                    String conversationId = result.replace("\"", "").trim();
                    SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Request accepted!");
                    
                    // Open the ChatActivity directly
                    Intent intent = new Intent(MessagingActivity.this, ChatActivity.class);
                    intent.putExtra("conversationId", conversationId);
                    intent.putExtra("otherUserId", cr.getSenderId());
                    intent.putExtra("otherUserName", cr.getSenderName());
                    intent.putExtra("reportId", cr.getReportId());
                    intent.putExtra("itemName", cr.getItemName());
                    startActivity(intent);

                    // Refresh requests list
                    loadChatRequests();
                } catch (Exception e) {
                    e.printStackTrace();
                    refreshCurrentTab();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed: " + errorMessage);
            }
        });
    }

    private void rejectRequest(ChatRequest cr) {
        if (cr == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Decline Request")
                .setMessage("Are you sure you want to decline this chat request?")
                .setPositiveButton("Decline", (dialog, which) -> {
                    Map<String, Object> params = new HashMap<>();
                    params.put("p_request_id", UUID.fromString(cr.getRequestId()));

                    if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

                    SupabaseDatabaseHelper.rpc("reject_chat_request", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                        @Override
                        public void onSuccess(String result) {
                            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                            SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Request declined.");
                            loadChatRequests();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                            SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed: " + errorMessage);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
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
