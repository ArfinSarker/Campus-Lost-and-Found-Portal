package com.sas.lostandfound;

import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
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
        holder.tvMessage.setText(notification.getMessage());
        holder.tvTime.setText(getTimeAgo(notification.getTimestamp()));

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
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static String getTimeAgo(long time) {
        long now = System.currentTimeMillis();
        final long diff = now - time;
        if (diff < 60 * 1000) {
            return "just now";
        } else if (diff < 2 * 60 * 1000) {
            return "1 minute ago";
        } else if (diff < 50 * 60 * 1000) {
            return diff / (60 * 1000) + " minutes ago";
        } else if (diff < 90 * 60 * 1000) {
            return "an hour ago";
        } else if (diff < 24 * 60 * 60 * 1000) {
            return diff / (60 * 60 * 1000) + " hours ago";
        } else if (diff < 48 * 60 * 60 * 1000) {
            return "yesterday";
        } else {
            return diff / (24 * 60 * 60 * 1000) + " days ago";
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        View viewUnread, llRoot;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            viewUnread = itemView.findViewById(R.id.viewUnreadIndicator);
            llRoot = itemView.findViewById(R.id.llNotificationRoot);
        }
    }
}
