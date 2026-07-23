package com.sas.lostandfound;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ForwardActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private TextView tvHeaderTitle;
    private EditText etSearchChats;
    private RecyclerView rvForwardChats;
    private FloatingActionButton fabForward;

    private String currentUnivId;
    private String messageTextToForward;

    private List<Conversation> conversationList = new ArrayList<>();
    private List<Conversation> fullConversationList = new ArrayList<>();
    private List<String> selectedConversationIds = new ArrayList<>();
    private ForwardAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forward);

        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUnivId = prefs.getString("universityId", null);

        messageTextToForward = getIntent().getStringExtra("messageText");
        ChatActivity.MessageMeta meta = ChatActivity.MessageMeta.parseMeta(messageTextToForward);
        if (meta != null && meta.text != null) {
            messageTextToForward = meta.text;
        }

        initializeViews();
        setupRecyclerView();
        loadConversations();

        btnBack.setOnClickListener(v -> finish());
        fabForward.setOnClickListener(v -> performForwarding());

        etSearchChats.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterConversations(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        etSearchChats = findViewById(R.id.etSearchChats);
        rvForwardChats = findViewById(R.id.rvForwardChats);
        fabForward = findViewById(R.id.fabForward);

        // Styling AppBar background dynamically if needed
        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            int headerColor = ContextCompat.getColor(this, R.color.messenger_bottom_sheet_bg);
            boolean isNightMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            HeaderColorHelper.setup(this, appBarLayout, headerColor, headerColor, !isNightMode);
        }
    }

    private void setupRecyclerView() {
        rvForwardChats.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ForwardAdapter(conversationList, this);
        rvForwardChats.setAdapter(adapter);
    }

    private void loadConversations() {
        if (currentUnivId == null) return;

        Map<String, Object> params = new HashMap<>();
        params.put("p_user_id", currentUnivId);

        SupabaseDatabaseHelper.rpc("get_user_conversations", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    List<Conversation> list = new com.google.gson.Gson().fromJson(result, new TypeToken<List<Conversation>>(){}.getType());
                    fullConversationList.clear();
                    if (list != null) {
                        fullConversationList.addAll(list);
                    }
                    filterConversations(etSearchChats.getText().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(ForwardActivity.this, "Failed to load chats: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterConversations(String query) {
        conversationList.clear();
        if (TextUtils.isEmpty(query)) {
            conversationList.addAll(fullConversationList);
        } else {
            String lowerQuery = query.toLowerCase().trim();
            for (Conversation c : fullConversationList) {
                if (c.getOtherUserName() != null && c.getOtherUserName().toLowerCase().contains(lowerQuery)) {
                    conversationList.add(c);
                }
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void toggleSelection(String conversationId) {
        if (selectedConversationIds.contains(conversationId)) {
            selectedConversationIds.remove(conversationId);
        } else {
            selectedConversationIds.add(conversationId);
        }
        // Update Floating Action Button visibility/color state
        if (selectedConversationIds.isEmpty()) {
            fabForward.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.chat_disabled_send_bg)));
        } else {
            fabForward.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.messenger_accent)));
        }
    }

    private void performForwarding() {
        if (selectedConversationIds.isEmpty()) {
            Toast.makeText(this, "Please select at least one chat", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(messageTextToForward)) {
            Toast.makeText(this, "No content to forward", Toast.LENGTH_SHORT).show();
            return;
        }

        fabForward.setEnabled(false);
        final int totalToForward = selectedConversationIds.size();
        final int[] completedCount = {0};
        final int[] successCount = {0};

        for (String convoId : selectedConversationIds) {
            // Forward it as a fresh message
            Message msg = new Message(convoId, currentUnivId, messageTextToForward);
            SupabaseDatabaseHelper.insert("messages", msg, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    successCount[0]++;
                    checkCompletion();
                }

                @Override
                public void onFailure(String errorMessage) {
                    checkCompletion();
                }

                private void checkCompletion() {
                    completedCount[0]++;
                    if (completedCount[0] == totalToForward) {
                        Toast.makeText(ForwardActivity.this, "Successfully forwarded to " + successCount[0] + " chats", Toast.LENGTH_SHORT).show();
                        // Trigger badge update for unread updates
                        UnreadBadgeHelper.sendBadgeUpdateBroadcast(ForwardActivity.this);
                        finish();
                    }
                }
            });
        }
    }

    // Forwarding adapter definition
    private class ForwardAdapter extends RecyclerView.Adapter<ForwardAdapter.ViewHolder> {
        private final List<Conversation> list;
        private final Context context;

        public ForwardAdapter(List<Conversation> list, Context context) {
            this.list = list;
            this.context = context;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_forward_chat, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Conversation c = list.get(position);
            holder.tvChatName.setText(c.getOtherUserName());

            // Bind checkbox select status
            holder.cbSelectChat.setChecked(selectedConversationIds.contains(c.getConversationId()));

            // Format online / offline status
            String lastActiveIso = c.getOtherUserLastActive();
            String statusText = formatLastActiveStatus(lastActiveIso);
            if (!TextUtils.isEmpty(statusText)) {
                holder.tvChatStatus.setText(statusText);
                if ("Active now".equals(statusText)) {
                    holder.tvChatStatus.setTextColor(ContextCompat.getColor(context, R.color.chat_header_last_active));
                } else {
                    holder.tvChatStatus.setTextColor(ContextCompat.getColor(context, R.color.messenger_text_secondary));
                }
            } else {
                holder.tvChatStatus.setText("Offline");
                holder.tvChatStatus.setTextColor(ContextCompat.getColor(context, R.color.messenger_text_secondary));
            }

            // Load avatar
            String avatarUrl = c.getOtherUserImageUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                GlideApp.with(context)
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_user_placeholder_white)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .into(holder.ivChatAvatar);
            } else {
                holder.ivChatAvatar.setImageResource(R.drawable.ic_user_placeholder_white);
            }

            // Click listener
            holder.itemView.setOnClickListener(v -> {
                toggleSelection(c.getConversationId());
                notifyItemChanged(position);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivChatAvatar;
            TextView tvChatName;
            TextView tvChatStatus;
            CheckBox cbSelectChat;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivChatAvatar = itemView.findViewById(R.id.ivChatAvatar);
                tvChatName = itemView.findViewById(R.id.tvChatName);
                tvChatStatus = itemView.findViewById(R.id.tvChatStatus);
                cbSelectChat = itemView.findViewById(R.id.cbSelectChat);
            }
        }

        private String formatLastActiveStatus(String lastActiveIso) {
            if (lastActiveIso == null || lastActiveIso.isEmpty()) {
                return "";
            }
            try {
                Date lastActiveDate = ValidationUtils.parseIso8601(lastActiveIso);
                if (lastActiveDate == null) return "";

                long diffMs = System.currentTimeMillis() - lastActiveDate.getTime();
                if (diffMs < 0) diffMs = 0;

                long diffMinutes = diffMs / (60 * 1000);
                if (diffMinutes < 2) {
                    return "Active now";
                }

                if (diffMinutes < 60) {
                    return "Last active " + diffMinutes + "m ago";
                }

                java.util.Calendar nowCal = java.util.Calendar.getInstance();
                java.util.Calendar activeCal = java.util.Calendar.getInstance();
                activeCal.setTime(lastActiveDate);

                boolean isToday = nowCal.get(java.util.Calendar.YEAR) == activeCal.get(java.util.Calendar.YEAR) &&
                                  nowCal.get(java.util.Calendar.DAY_OF_YEAR) == activeCal.get(java.util.Calendar.DAY_OF_YEAR);

                java.util.Calendar yesterdayCal = java.util.Calendar.getInstance();
                yesterdayCal.add(java.util.Calendar.DAY_OF_YEAR, -1);
                boolean isYesterday = yesterdayCal.get(java.util.Calendar.YEAR) == activeCal.get(java.util.Calendar.YEAR) &&
                                      yesterdayCal.get(java.util.Calendar.DAY_OF_YEAR) == activeCal.get(java.util.Calendar.DAY_OF_YEAR);

                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                if (isToday) {
                    return "Active today at " + timeFormat.format(lastActiveDate);
                } else if (isYesterday) {
                    return "Active yesterday";
                } else {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    return "Active on " + dateFormat.format(lastActiveDate);
                }
            } catch (Exception e) {
                return "";
            }
        }
    }
}
