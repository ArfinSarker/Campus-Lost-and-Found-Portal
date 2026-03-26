package com.sas.lostandfound;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
        
        String fullMsg = notification.getMessage();
        if (fullMsg != null && fullMsg.contains("Click to view details.")) {
            int start = fullMsg.indexOf("Click to view details.");
            int end = start + "Click to view details.".length();
            SpannableString spannableString = new SpannableString(fullMsg);
            spannableString.setSpan(new UnderlineSpan(), start, end, 0);
            spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(holder.itemView.getContext(), R.color.primaryColor)), start, end, 0);
            holder.tvMessage.setText(spannableString);
        } else {
            holder.tvMessage.setText(fullMsg);
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

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        View viewUnread, llRoot;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            viewUnread = itemView.findViewById(R.id.viewUnreadIndicator);
            llRoot = itemView.findViewById(R.id.llNotificationRoot);
            btnDelete = itemView.findViewById(R.id.btnDeleteNotification);
        }
    }
}
