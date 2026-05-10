package com.sas.lostandfound;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications;
    private OnNotificationClickListener listener;
    private OnNotificationDeleteListener deleteListener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public interface OnNotificationDeleteListener {
        void onNotificationDelete(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener, OnNotificationDeleteListener deleteListener) {
        this.notifications = notifications;
        this.listener = listener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        
        String type = notification.getType();
        if ("lost_claim".equals(type) || "found_claim".equals(type)) {
            holder.tvMessage.setText(styleClaimNotification(notification, holder.itemView.getContext()));
        } else {
            String fullMsg = notification.getMessage();
            if (fullMsg != null && fullMsg.contains("Click to view details")) {
                int start = fullMsg.indexOf("Click to view details");
                int end = start + "Click to view details".length();
                SpannableString spannableString = new SpannableString(fullMsg);
                spannableString.setSpan(new UnderlineSpan(), start, end, 0);
                spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(holder.itemView.getContext(), R.color.primaryColor)), start, end, 0);
                holder.tvMessage.setText(spannableString);
            } else {
                holder.tvMessage.setText(fullMsg);
            }
        }

        // Load sender image if available, otherwise show default bell
        if (notification.getSenderImageUrl() != null && !notification.getSenderImageUrl().isEmpty()) {
            GlideApp.with(holder.itemView.getContext())
                    .load(notification.getSenderImageUrl())
                    .placeholder(R.drawable.ic_user)
                    .circleCrop()
                    .into(holder.ivIcon);
            holder.ivIcon.setPadding(0, 0, 0, 0);
            holder.ivIcon.setBackground(null);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_bell);
            holder.ivIcon.setPadding(8, 8, 8, 8);
            holder.ivIcon.setBackgroundResource(R.drawable.bg_notification_badge);
            holder.ivIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.primaryLightColor)));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(notification.getTimestamp())));

        if (notification.isRead()) {
            holder.tvMessage.setTypeface(null, Typeface.NORMAL);
            holder.viewUnread.setVisibility(View.GONE);
            holder.llRoot.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.backgroundColor));
            holder.itemView.setAlpha(0.7f);
        } else {
            holder.tvMessage.setTypeface(null, Typeface.BOLD);
            holder.viewUnread.setVisibility(View.VISIBLE);
            holder.llRoot.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.light_red_bg));
            holder.itemView.setAlpha(1.0f);
        }

        holder.itemView.setOnClickListener(v -> listener.onNotificationClick(notification));
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onNotificationDelete(notification);
            }
        });
    }

    private SpannableString styleClaimNotification(Notification notification, android.content.Context context) {
        String claimerName = notification.getSenderName() != null ? notification.getSenderName() : "A user";
        String itemName = notification.getItemName() != null ? notification.getItemName() : "Unknown Item";
        String fullText;
        
        if ("found_claim".equals(notification.getType())) {
            // Found Report Claim: {Claimer Name} has claimed ownership of the item ‘{Item Name}’. Review their request and verify before handing over.
            fullText = claimerName + " has claimed ownership of the item ‘" + itemName + "’. Review their request and verify before handing over.";
        } else {
            // Lost Report Claim: {Claimer Name} has claimed they found your lost item ‘{Item Name}’. Check details and verify.
            fullText = claimerName + " has claimed they found your lost item ‘" + itemName + "’. Check details and verify.";
        }

        SpannableString spannableString = new SpannableString(fullText);
        int blueColor = ContextCompat.getColor(context, R.color.primaryColor);

        // Style Claimer Name
        int nameStart = fullText.indexOf(claimerName);
        if (nameStart != -1) {
            spannableString.setSpan(new ForegroundColorSpan(blueColor), nameStart, nameStart + claimerName.length(), 0);
        }

        // Style Item Name
        int itemStart = fullText.indexOf("‘" + itemName + "’");
        if (itemStart != -1) {
            // Highlight only the name inside quotes
            spannableString.setSpan(new ForegroundColorSpan(blueColor), itemStart + 1, itemStart + 1 + itemName.length(), 0);
        }

        return spannableString;
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        View viewUnread, llRoot;
        ImageButton btnDelete;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            viewUnread = itemView.findViewById(R.id.viewUnreadIndicator);
            llRoot = itemView.findViewById(R.id.llNotificationRoot);
            btnDelete = itemView.findViewById(R.id.btnDeleteNotification);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
        }
    }
}
