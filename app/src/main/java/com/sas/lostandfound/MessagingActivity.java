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

        View cardArchivedRow = findViewById(R.id.cardArchivedRow);
        if (cardArchivedRow != null) {
            cardArchivedRow.setOnClickListener(v -> {
                Intent intent = new Intent(this, ArchivedChatsActivity.class);
                startActivity(intent);
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
        String lowerQuery = (query != null) ? query.toLowerCase().trim() : "";
        for (Conversation c : fullConversationList) {
            if (isConversationLocallyDeleted(c.getConversationId())) {
                continue;
            }
            if (isConversationArchived(c.getConversationId())) {
                continue;
            }
            if (lowerQuery.isEmpty() || (c.getOtherUserName() != null && c.getOtherUserName().toLowerCase().contains(lowerQuery))) {
                conversationList.add(c);
            }
        }
        
        sortConversationsList(conversationList);
        
        if (conversationsAdapter != null) {
            conversationsAdapter.notifyDataSetChanged();
        }
        updateEmptyState();
        updateArchivedRowVisibility();
    }

    private void updateArchivedRowVisibility() {
        int archivedCount = 0;
        for (Conversation c : fullConversationList) {
            if (isConversationLocallyDeleted(c.getConversationId())) {
                continue;
            }
            if (isConversationArchived(c.getConversationId())) {
                archivedCount++;
            }
        }
        
        View cardArchivedRow = findViewById(R.id.cardArchivedRow);
        TextView tvArchiveRowText = findViewById(R.id.tvArchiveRowText);
        if (cardArchivedRow != null && tvArchiveRowText != null) {
            if (archivedCount > 0) {
                tvArchiveRowText.setText("Archived (" + archivedCount + ")");
                cardArchivedRow.setVisibility(View.VISIBLE);
            } else {
                cardArchivedRow.setVisibility(View.GONE);
            }
        }
    }

    private void sortConversationsList(java.util.List<Conversation> list) {
        java.util.List<String> pinnedList = getPinnedConversationsList();
        java.util.Collections.sort(list, (c1, c2) -> {
            boolean p1 = pinnedList.contains(c1.getConversationId());
            boolean p2 = pinnedList.contains(c2.getConversationId());
            if (p1 && !p2) return -1;
            if (!p1 && p2) return 1;
            if (p1 && p2) {
                return pinnedList.indexOf(c1.getConversationId()) - pinnedList.indexOf(c2.getConversationId());
            }
            String t1 = c1.getLastMessageTime();
            String t2 = c2.getLastMessageTime();
            if (t1 == null) return 1;
            if (t2 == null) return -1;
            return t2.compareTo(t1);
        });
    }

    public java.util.List<String> getPinnedConversationsList() {
        android.content.SharedPreferences prefs = getSharedPreferences("ChatPrefs_" + (currentUnivId != null ? currentUnivId.trim() : "default"), Context.MODE_PRIVATE);
        String json = prefs.getString("pinned_conversations_list", "[]");
        try {
            return new com.google.gson.Gson().fromJson(json, new com.google.gson.reflect.TypeToken<java.util.List<String>>(){}.getType());
        } catch (Exception e) {
            return new java.util.ArrayList<>();
        }
    }

    public void savePinnedConversationsList(java.util.List<String> list) {
        android.content.SharedPreferences prefs = getSharedPreferences("ChatPrefs_" + (currentUnivId != null ? currentUnivId.trim() : "default"), Context.MODE_PRIVATE);
        String json = new com.google.gson.Gson().toJson(list);
        prefs.edit().putString("pinned_conversations_list", json).apply();
    }

    public boolean isConversationPinned(String conversationId) {
        if (conversationId == null) return false;
        return getPinnedConversationsList().contains(conversationId);
    }

    public void togglePinConversation(String conversationId) {
        if (conversationId == null) return;
        java.util.List<String> pinnedList = getPinnedConversationsList();
        boolean wasPinned = pinnedList.contains(conversationId);
        if (wasPinned) {
            pinnedList.remove(conversationId);
            SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Chat unpinned");
        } else {
            pinnedList.add(conversationId);
            SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Chat pinned to top");
        }
        savePinnedConversationsList(pinnedList);
        filterConversations(etSearchChats != null ? etSearchChats.getText().toString() : "");
    }

    public void togglePinConversationAnimated(int adapterPosition) {
        if (adapterPosition < 0 || adapterPosition >= conversationList.size()) return;
        Conversation c = conversationList.get(adapterPosition);
        String conversationId = c.getConversationId();
        
        java.util.List<String> pinnedList = getPinnedConversationsList();
        boolean wasPinned = pinnedList.contains(conversationId);
        
        if (wasPinned) {
            pinnedList.remove(conversationId);
            SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Chat unpinned");
        } else {
            pinnedList.add(conversationId);
            SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Chat pinned to top");
        }
        savePinnedConversationsList(pinnedList);
        
        sortConversationsList(conversationList);
        
        int newPosition = conversationList.indexOf(c);
        if (conversationsAdapter != null) {
            if (adapterPosition != newPosition) {
                conversationsAdapter.notifyItemMoved(adapterPosition, newPosition);
            }
            conversationsAdapter.notifyItemChanged(newPosition);
        }
    }

    public boolean isConversationLocallyDeleted(String conversationId) {
        if (conversationId == null) return false;
        android.content.SharedPreferences prefs = getSharedPreferences("ChatPrefs_" + (currentUnivId != null ? currentUnivId.trim() : "default"), Context.MODE_PRIVATE);
        java.util.Set<String> deleted = prefs.getStringSet("locally_deleted_conversations", new java.util.HashSet<>());
        return deleted.contains(conversationId);
    }

    public boolean isConversationArchived(String conversationId) {
        if (conversationId == null) return false;
        android.content.SharedPreferences prefs = getSharedPreferences("ChatPrefs_" + (currentUnivId != null ? currentUnivId.trim() : "default"), Context.MODE_PRIVATE);
        java.util.Set<String> archived = prefs.getStringSet("archived_conversations", new java.util.HashSet<>());
        return archived.contains(conversationId);
    }

    public void toggleArchiveConversation(String conversationId) {
        if (conversationId == null) return;
        android.content.SharedPreferences prefs = getSharedPreferences("ChatPrefs_" + (currentUnivId != null ? currentUnivId.trim() : "default"), Context.MODE_PRIVATE);
        java.util.Set<String> archived = new java.util.HashSet<>(prefs.getStringSet("archived_conversations", new java.util.HashSet<>()));
        boolean wasArchived = archived.contains(conversationId);
        if (wasArchived) {
            archived.remove(conversationId);
            SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Conversation unarchived");
        } else {
            archived.add(conversationId);
            SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Conversation archived");
        }
        prefs.edit().putStringSet("archived_conversations", archived).apply();
        filterConversations(etSearchChats != null ? etSearchChats.getText().toString() : "");
    }

    public void toggleArchiveConversationAnimated(int adapterPosition) {
        if (adapterPosition < 0 || adapterPosition >= conversationList.size()) return;
        Conversation c = conversationList.get(adapterPosition);
        toggleArchiveConversation(c.getConversationId());
        if (conversationsAdapter != null) {
            conversationsAdapter.notifyItemRemoved(adapterPosition);
        }
    }

    public boolean isConversationMuted(String conversationId) {
        if (conversationId == null) return false;
        android.content.SharedPreferences prefs = getSharedPreferences("ChatPrefs_" + (currentUnivId != null ? currentUnivId.trim() : "default"), Context.MODE_PRIVATE);
        long muteUntil = prefs.getLong("mute_" + conversationId, 0);
        if (muteUntil == -1) return true; // until turned off
        return System.currentTimeMillis() < muteUntil;
    }

    public void muteConversation(String conversationId, long durationMs) {
        if (conversationId == null) return;
        android.content.SharedPreferences prefs = getSharedPreferences("ChatPrefs_" + (currentUnivId != null ? currentUnivId.trim() : "default"), Context.MODE_PRIVATE);
        long muteUntil = durationMs == -1 ? -1 : (System.currentTimeMillis() + durationMs);
        prefs.edit().putLong("mute_" + conversationId, muteUntil).apply();
        if (conversationsAdapter != null) conversationsAdapter.notifyDataSetChanged();
    }

    public void unmuteConversation(String conversationId) {
        if (conversationId == null) return;
        android.content.SharedPreferences prefs = getSharedPreferences("ChatPrefs_" + (currentUnivId != null ? currentUnivId.trim() : "default"), Context.MODE_PRIVATE);
        prefs.edit().remove("mute_" + conversationId).apply();
        if (conversationsAdapter != null) conversationsAdapter.notifyDataSetChanged();
    }

    public boolean isLocalUnread(String conversationId) {
        if (conversationId == null) return false;
        android.content.SharedPreferences prefs = getSharedPreferences("ChatPrefs_" + (currentUnivId != null ? currentUnivId.trim() : "default"), Context.MODE_PRIVATE);
        java.util.Set<String> unread = prefs.getStringSet("unread_conversations", new java.util.HashSet<>());
        return unread.contains(conversationId);
    }

    public void markChatAsUnreadLocally(String conversationId) {
        markChatAsUnreadLocally(conversationId, null);
    }

    public void markChatAsUnreadLocally(String conversationId, String otherUserId) {
        if (conversationId == null) return;
        android.content.SharedPreferences prefs = getSharedPreferences("ChatPrefs_" + (currentUnivId != null ? currentUnivId.trim() : "default"), Context.MODE_PRIVATE);
        java.util.Set<String> unread = new java.util.HashSet<>(prefs.getStringSet("unread_conversations", new java.util.HashSet<>()));
        unread.add(conversationId);
        prefs.edit().putStringSet("unread_conversations", unread).apply();
        if (conversationsAdapter != null) conversationsAdapter.notifyDataSetChanged();

        if (currentUnivId != null) {
            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<java.util.Map<String, Object>>>(){}.getType();
            String filter = "conversation_id=eq." + conversationId + "&sender_id=eq." + currentUnivId + "&select=created_at&order=created_at.desc&limit=1";
            SupabaseDatabaseHelper.select("messages", filter, listType, new SupabaseDatabaseHelper.DatabaseCallback<java.util.List<java.util.Map<String, Object>>>() {
                @Override
                public void onSuccess(java.util.List<java.util.Map<String, Object>> list) {
                    String lastSentTime = null;
                    if (list != null && !list.isEmpty() && list.get(0) != null && list.get(0).containsKey("created_at")) {
                        Object cat = list.get(0).get("created_at");
                        if (cat != null) lastSentTime = cat.toString();
                    }

                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("receiver_marked_unread", true);
                    String query;
                    if (otherUserId != null && !otherUserId.isEmpty()) {
                        if (lastSentTime != null && !lastSentTime.isEmpty()) {
                            query = "conversation_id=eq." + conversationId + "&sender_id=eq." + otherUserId + "&created_at=gt." + lastSentTime;
                        } else {
                            query = "conversation_id=eq." + conversationId + "&sender_id=eq." + otherUserId;
                        }
                    } else {
                        if (lastSentTime != null && !lastSentTime.isEmpty()) {
                            query = "conversation_id=eq." + conversationId + "&sender_id=neq." + currentUnivId + "&created_at=gt." + lastSentTime;
                        } else {
                            query = "conversation_id=eq." + conversationId + "&sender_id=neq." + currentUnivId;
                        }
                    }

                    SupabaseDatabaseHelper.update("messages", query, data, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                        @Override public void onSuccess(String result) {
                            UnreadBadgeHelper.sendBadgeUpdateBroadcast(MessagingActivity.this);
                        }
                        @Override public void onFailure(String errorMessage) {}
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("receiver_marked_unread", true);
                    String query = (otherUserId != null && !otherUserId.isEmpty()) ?
                            "conversation_id=eq." + conversationId + "&sender_id=eq." + otherUserId :
                            "conversation_id=eq." + conversationId + "&sender_id=neq." + currentUnivId;
                    SupabaseDatabaseHelper.update("messages", query, data, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                        @Override public void onSuccess(String result) {
                            UnreadBadgeHelper.sendBadgeUpdateBroadcast(MessagingActivity.this);
                        }
                        @Override public void onFailure(String errorMessage) {}
                    });
                }
            });
        }
    }

    public void markChatAsReadLocally(String conversationId) {
        markChatAsReadLocally(conversationId, null);
    }

    public void markChatAsReadLocally(String conversationId, String otherUserId) {
        if (conversationId == null) return;
        android.content.SharedPreferences prefs = getSharedPreferences("ChatPrefs_" + (currentUnivId != null ? currentUnivId.trim() : "default"), Context.MODE_PRIVATE);
        java.util.Set<String> unread = new java.util.HashSet<>(prefs.getStringSet("unread_conversations", new java.util.HashSet<>()));
        if (unread.contains(conversationId)) {
            unread.remove(conversationId);
            prefs.edit().putStringSet("unread_conversations", unread).apply();
            if (conversationsAdapter != null) conversationsAdapter.notifyDataSetChanged();
        }

        if (currentUnivId != null) {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("is_read", true);
            data.put("is_delivered", true);
            data.put("receiver_marked_unread", false);
            String query;
            if (otherUserId != null && !otherUserId.trim().isEmpty()) {
                query = "conversation_id=eq." + conversationId.trim() + "&sender_id=eq." + otherUserId.trim();
            } else {
                query = "conversation_id=eq." + conversationId.trim() + "&sender_id=neq." + currentUnivId.trim();
            }
            SupabaseDatabaseHelper.update("messages", query, data, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                @Override public void onSuccess(String result) {
                    UnreadBadgeHelper.sendBadgeUpdateBroadcast(MessagingActivity.this);
                }
                @Override public void onFailure(String errorMessage) {}
            });
        }
    }

    public void showConversationActionMenu(Conversation c, int adapterPosition) {
        if (c == null) return;
        com.google.android.material.bottomsheet.BottomSheetDialog sheetDialog = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.layout_conversation_actions_bottom_sheet, null);
        sheetDialog.setContentView(view);

        View parent = (View) view.getParent();
        if (parent != null) {
            parent.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        }

        TextView tvTitle = view.findViewById(R.id.tvSheetTitle);
        tvTitle.setText(c.getOtherUserName());

        // 1. Pin option
        TextView tvTextPin = view.findViewById(R.id.tvTextPin);
        ImageView ivIconPin = view.findViewById(R.id.ivIconPin);
        boolean isPinned = isConversationPinned(c.getConversationId());
        tvTextPin.setText(isPinned ? "Unpin Chat" : "Pin Chat");
        ivIconPin.setImageResource(isPinned ? R.drawable.ic_pin : R.drawable.ic_pin);

        view.findViewById(R.id.optionPin).setOnClickListener(v -> {
            sheetDialog.dismiss();
            togglePinConversationAnimated(adapterPosition);
        });

        // 2. Archive option
        TextView tvTextArchive = view.findViewById(R.id.tvTextArchive);
        boolean isArchived = isConversationArchived(c.getConversationId());
        tvTextArchive.setText(isArchived ? "Unarchive" : "Archive");
        view.findViewById(R.id.optionArchive).setOnClickListener(v -> {
            sheetDialog.dismiss();
            toggleArchiveConversationAnimated(adapterPosition);
        });

        // 3. Mute option
        TextView tvTextMute = view.findViewById(R.id.tvTextMute);
        boolean isMuted = isConversationMuted(c.getConversationId());
        tvTextMute.setText(isMuted ? "Unmute Notifications" : "Mute Notifications");
        view.findViewById(R.id.optionMute).setOnClickListener(v -> {
            sheetDialog.dismiss();
            if (isMuted) {
                unmuteConversation(c.getConversationId());
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Conversation unmuted");
            } else {
                showMuteOptionsDialog(c.getConversationId());
            }
        });

        // 4. Mark as Unread option
        TextView tvTextUnread = view.findViewById(R.id.tvTextUnread);
        boolean isCurrentlyUnread = (c.getUnreadCount() > 0 || isLocalUnread(c.getConversationId()));
        tvTextUnread.setText(isCurrentlyUnread ? "Mark as Read" : "Mark as Unread");
        view.findViewById(R.id.optionUnread).setOnClickListener(v -> {
            sheetDialog.dismiss();
            if (isCurrentlyUnread) {
                markChatAsReadLocally(c.getConversationId(), c.getOtherUserId());
            } else {
                markChatAsUnreadLocally(c.getConversationId(), c.getOtherUserId());
            }
        });

        // 5. Block option
        TextView tvTextBlock = view.findViewById(R.id.tvTextBlock);
        boolean isBlockedByMe = blockedUserIds.contains(c.getOtherUserId());
        tvTextBlock.setText(isBlockedByMe ? "Unblock User" : "Block User");
        view.findViewById(R.id.optionBlock).setOnClickListener(v -> {
            sheetDialog.dismiss();
            ProfileContextMenuHelper.toggleUserBlock(this, currentUnivId, c.getOtherUserId(), isBlockedByMe, isNowBlocked -> {
                fetchBlockListsAndLoadConversations(true);
            });
        });

        // 6. Delete option
        view.findViewById(R.id.optionDelete).setOnClickListener(v -> {
            sheetDialog.dismiss();
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Conversation?")
                .setMessage("Are you sure you want to permanently delete this conversation? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteConversationFromServer(c.getConversationId()))
                .setNegativeButton("Cancel", null)
                .show();
        });

        sheetDialog.show();
    }

    private void showMuteOptionsDialog(String conversationId) {
        String[] options = {
            "For 15 minutes",
            "For 1 hour",
            "For 8 hours",
            "For 24 hours",
            "Until I turn it back on",
            "Custom..."
        };
        long[] durations = {
            15 * 60 * 1000L,
            60 * 60 * 1000L,
            8 * 60 * 60 * 1000L,
            24 * 60 * 60 * 1000L,
            -1L,
            -2L // Custom trigger
        };

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Mute notifications for this chat")
            .setItems(options, (dialog, which) -> {
                long duration = durations[which];
                if (duration == -2L) {
                    showCustomMuteDialog(conversationId);
                } else {
                    muteConversation(conversationId, duration);
                    SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Chat muted " + options[which].toLowerCase());
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showCustomMuteDialog(String conversationId) {
        View dialogView = getLayoutInflater().inflate(R.layout.layout_custom_mute_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        EditText etDays = dialogView.findViewById(R.id.etMuteDays);
        EditText etHours = dialogView.findViewById(R.id.etMuteHours);
        EditText etMinutes = dialogView.findViewById(R.id.etMuteMinutes);
        EditText etSeconds = dialogView.findViewById(R.id.etMuteSeconds);

        dialogView.findViewById(R.id.btnCancelMute).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnConfirmMute).setOnClickListener(v -> {
            int days = 0, hours = 0, minutes = 0, seconds = 0;
            try {
                days = Integer.parseInt(etDays.getText().toString().trim());
                hours = Integer.parseInt(etHours.getText().toString().trim());
                minutes = Integer.parseInt(etMinutes.getText().toString().trim());
                seconds = Integer.parseInt(etSeconds.getText().toString().trim());
            } catch (Exception ignored) {}

            if (days < 0 || hours < 0 || minutes < 0 || seconds < 0) {
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Duration values cannot be negative.");
                return;
            }
            if (days == 0 && hours == 0 && minutes == 0 && seconds == 0) {
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Duration cannot be zero.");
                return;
            }

            long totalSeconds = ((days * 24L + hours) * 60L + minutes) * 60L + seconds;
            long durationMs = totalSeconds * 1000L;

            muteConversation(conversationId, durationMs);
            dialog.dismiss();

            StringBuilder sb = new StringBuilder("Muted for ");
            if (days > 0) sb.append(days).append("d ");
            if (hours > 0) sb.append(hours).append("h ");
            if (minutes > 0) sb.append(minutes).append("m ");
            if (seconds > 0) sb.append(seconds).append("s ");
            SnackbarManager.show(SnackbarManager.Type.SUCCESS, sb.toString().trim());
        });

        dialog.show();
    }

    private void deleteConversationFromServer(String conversationId) {
        if (conversationId == null) return;
        SupabaseDatabaseHelper.delete("messages", "conversation_id=eq." + conversationId, new SupabaseDatabaseHelper.DatabaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                deleteConversationRecord(conversationId);
            }

            @Override
            public void onFailure(String error) {
                deleteConversationRecord(conversationId);
            }
        });
    }

    private void deleteConversationRecord(String conversationId) {
        SupabaseDatabaseHelper.delete("conversations", "id=eq." + conversationId, new SupabaseDatabaseHelper.DatabaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                cleanUpLocalConversationState(conversationId);
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Conversation deleted successfully.");
                fetchBlockListsAndLoadConversations(true);
            }

            @Override
            public void onFailure(String error) {
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to delete conversation: " + error);
            }
        });
    }

    private void cleanUpLocalConversationState(String conversationId) {
        if (conversationId == null) return;
        try {
            java.util.List<String> pinnedList = getPinnedConversationsList();
            if (pinnedList.contains(conversationId)) {
                pinnedList.remove(conversationId);
                savePinnedConversationsList(pinnedList);
            }
            android.content.SharedPreferences prefs = getSharedPreferences("ChatPrefs_" + (currentUnivId != null ? currentUnivId.trim() : "default"), Context.MODE_PRIVATE);
            java.util.Set<String> archived = new java.util.HashSet<>(prefs.getStringSet("archived_conversations", new java.util.HashSet<>()));
            if (archived.contains(conversationId)) {
                archived.remove(conversationId);
                prefs.edit().putStringSet("archived_conversations", archived).apply();
            }
        } catch (Exception ignored) {}
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
            
            boolean isPinned = false;
            boolean isMuted = false;
            if (context instanceof MessagingActivity) {
                isPinned = ((MessagingActivity) context).isConversationPinned(c.getConversationId());
                isMuted = ((MessagingActivity) context).isConversationMuted(c.getConversationId());
            }

            StringBuilder nameBuilder = new StringBuilder(c.getOtherUserName() != null ? c.getOtherUserName() : "");
            holder.tvUserName.setText(nameBuilder.toString());

            if (holder.ivPinIndicator != null) {
                if (isPinned) {
                    holder.ivPinIndicator.setVisibility(View.VISIBLE);
                    int pinColor = androidx.core.content.ContextCompat.getColor(context, R.color.pin_indicator_tint);
                    holder.ivPinIndicator.setImageTintList(android.content.res.ColorStateList.valueOf(pinColor));
                } else {
                    holder.ivPinIndicator.setVisibility(View.GONE);
                }
            }

            if (holder.ivMuteIndicator != null) {
                if (isMuted) {
                    holder.ivMuteIndicator.setVisibility(View.VISIBLE);
                    int muteColor = androidx.core.content.ContextCompat.getColor(context, R.color.mute_indicator_tint);
                    holder.ivMuteIndicator.setImageTintList(android.content.res.ColorStateList.valueOf(muteColor));
                } else {
                    holder.ivMuteIndicator.setVisibility(View.GONE);
                }
            }
            
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
                if (blockedUserIds.contains(c.getOtherUserId())) {
                    statusText = "Blocked";
                } else if (currentUserId != null && !currentUserId.equals(c.getRequestSenderId()) && !usersWhoBlockedMe.contains(c.getOtherUserId())) {
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
                    } else if ("Blocked".equals(statusText)) {
                        holder.tvUserStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.ca_accent_block));
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
            String displayLastMsg = "No messages yet";
            String lastMsg = c.getLastMessage();
            if (lastMsg != null && !lastMsg.isEmpty()) {
                displayLastMsg = lastMsg;
                ChatActivity.MessageMeta meta = ChatActivity.MessageMeta.parseMeta(lastMsg);
                if (meta != null) {
                    if (meta.isUnsent) {
                        String senderId = c.getLastMessageSenderId();
                        if (senderId != null && senderId.equals(currentUserId)) {
                            displayLastMsg = "You unsent a message";
                        } else {
                            displayLastMsg = "This message was unsent";
                        }
                    } else if (meta.deletedForUsers != null && meta.deletedForUsers.contains(currentUserId)) {
                        displayLastMsg = "Message deleted";
                    } else {
                        displayLastMsg = meta.text;
                    }
                }
            }
            holder.tvLastMessage.setText(displayLastMsg);
            holder.tvTimestamp.setText(formatTime(c.getLastMessageTime()));

            // Highlight unread conversations
            boolean isLocalUnread = false;
            if (context instanceof MessagingActivity) {
                isLocalUnread = ((MessagingActivity) context).isLocalUnread(c.getConversationId());
            }
            if (c.getUnreadCount() > 0 || isLocalUnread) {
                int displayCount = c.getUnreadCount() > 0 ? c.getUnreadCount() : 1;
                holder.tvLastMessage.setTypeface(null, Typeface.BOLD);
                holder.tvLastMessage.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_text_primary));
                holder.tvTimestamp.setTypeface(null, Typeface.BOLD);
                holder.tvTimestamp.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_loading_indicator));
                
                holder.tvUnreadBadge.setText(String.valueOf(displayCount));
                holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            } else {
                holder.tvLastMessage.setTypeface(null, Typeface.NORMAL);
                holder.tvLastMessage.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_last_message));
                holder.tvTimestamp.setTypeface(null, Typeface.NORMAL);
                holder.tvTimestamp.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_time));
                
                holder.tvUnreadBadge.setVisibility(View.GONE);
            }

            // Show last message status tick (if sent by us), independently of unread badge
            if (holder.ivLastMessageStatus != null) {
                if (c.getLastMessageSenderId() != null && c.getLastMessageSenderId().equals(currentUserId)) {
                    holder.ivLastMessageStatus.setVisibility(View.VISIBLE);
                    boolean isReceiverMarkedUnread = c.getLastMessageReceiverMarkedUnread() != null && c.getLastMessageReceiverMarkedUnread();
                    boolean isRead = c.getLastMessageIsRead() != null && c.getLastMessageIsRead();
                    boolean isDelivered = c.getLastMessageIsDelivered() != null && c.getLastMessageIsDelivered();

                    if (isReceiverMarkedUnread) {
                        holder.ivLastMessageStatus.setImageResource(R.drawable.ic_double_check);
                        holder.ivLastMessageStatus.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_double_tick)));
                    } else if (isRead) {
                        holder.ivLastMessageStatus.setImageResource(R.drawable.ic_double_check);
                        holder.ivLastMessageStatus.setImageTintList(ColorStateList.valueOf(androidx.core.content.ContextCompat.getColor(context, R.color.chat_list_read_double_tick)));
                    } else if (isDelivered) {
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

            holder.itemView.setOnLongClickListener(v -> {
                if (context instanceof MessagingActivity) {
                    ((MessagingActivity) context).showConversationActionMenu(c, holder.getAdapterPosition());
                }
                return true;
            });

            holder.itemView.setOnClickListener(v -> {
                if (context instanceof MessagingActivity) {
                    ((MessagingActivity) context).markChatAsReadLocally(c.getConversationId());
                }
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

                boolean blockedByMe = false;
                boolean blockedByOther = false;
                if (context instanceof MessagingActivity) {
                    blockedByMe = ((MessagingActivity) context).blockedUserIds.contains(c.getOtherUserId());
                    blockedByOther = ((MessagingActivity) context).usersWhoBlockedMe.contains(c.getOtherUserId());
                }
                intent.putExtra("isBlockedByMe", blockedByMe);
                intent.putExtra("isBlockedByOther", blockedByOther);

                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            MaterialCardView cardUserAvatar;
            ImageView ivUserAvatar, ivLastMessageStatus, ivPinIndicator, ivMuteIndicator;
            TextView tvUserName, tvUserStatus, tvLastMessage, tvTimestamp, tvUnreadBadge;
            View viewActiveIndicator;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                cardUserAvatar = itemView.findViewById(R.id.cardUserAvatar);
                ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
                ivLastMessageStatus = itemView.findViewById(R.id.ivLastMessageStatus);
                ivPinIndicator = itemView.findViewById(R.id.ivPinIndicator);
                ivMuteIndicator = itemView.findViewById(R.id.ivMuteIndicator);
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
