package com.sas.lostandfound;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
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
        holder.tvMessage.setMovementMethod(LinkMovementMethod.getInstance());
        
        if ("item_claimed".equals(type) || "item_return".equals(type)) {
            holder.tvMessage.setText(styleEnhancedNotification(notification, holder.itemView.getContext()));
            holder.itemView.setOnClickListener(null);
        } else if ("lost_claimed_confirmed".equals(type) || "item_returned_confirmed".equals(type)) {
            // Keep support for old types during transition
            holder.tvMessage.setText(styleEnhancedNotification(notification, holder.itemView.getContext()));
            holder.itemView.setOnClickListener(null);
        } else if ("lost_claim".equals(type) || "found_claim".equals(type)) {
            holder.tvMessage.setText(styleClaimNotification(notification, holder.itemView.getContext()));
            holder.itemView.setOnClickListener(null);
        } else if ("admin_report".equals(type) || "admin_report_new".equals(type)) {
            holder.tvMessage.setText(styleAdminReportNotification(notification, holder.itemView.getContext()));
            holder.itemView.setOnClickListener(null);
        } else {
            String fullMsg = notification.getMessage();
            if (fullMsg != null && fullMsg.contains("Click to view details")) {
                int start = fullMsg.indexOf("Click to view details");
                int end = start + "Click to view details".length();
                SpannableString spannableString = new SpannableString(fullMsg);
                spannableString.setSpan(new UnderlineSpan(), start, end, 0);
                spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(holder.itemView.getContext(), R.color.primaryColor)), start, end, 0);
                
                // Add click for simple "Click to view details"
                spannableString.setSpan(new ClickableSpan() {
                    @Override public void onClick(@NonNull View widget) { listener.onNotificationClick(notification); }
                    @Override public void updateDrawState(@NonNull android.text.TextPaint ds) { ds.setUnderlineText(true); ds.setColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.primaryColor)); }
                }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                holder.tvMessage.setText(spannableString);
                holder.itemView.setOnClickListener(null);
            } else {
                holder.tvMessage.setText(fullMsg);
                holder.itemView.setOnClickListener(v -> listener.onNotificationClick(notification));
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

        if (!"admin_report".equals(type)) {
            holder.itemView.setOnClickListener(v -> listener.onNotificationClick(notification));
        }
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onNotificationDelete(notification);
            }
        });
    }

    private SpannableString styleEnhancedNotification(Notification notification, android.content.Context context) {
        String name = notification.getSenderName() != null ? notification.getSenderName() : "A user";
        String itemName = notification.getItemName() != null ? notification.getItemName() : "Unknown Item";
        String type = notification.getType();
        String action;
        int itemColor;
        
        if ("item_claimed".equals(type) || "lost_claimed_confirmed".equals(type) || "found_claim".equals(type)) {
            action = " has marked that they received ";
            itemColor = ContextCompat.getColor(context, R.color.lightRed); // Red for claimed
        } else {
            action = " has marked that they returned ";
            itemColor = ContextCompat.getColor(context, R.color.statusFound); // Green for returned
        }
        
        String clickText = "Click to view details";
        String fullText = "\"" + name + "\"" + action + "\"" + itemName + "\" from you. " + clickText + ".";
        
        SpannableString ss = new SpannableString(fullText);
        int orange = ContextCompat.getColor(context, R.color.orange);
        int blue = ContextCompat.getColor(context, R.color.primaryColor);
        
        // 1. Clickable Name (Orange)
        int nameStart = fullText.indexOf("\"" + name + "\"");
        if (nameStart != -1) {
            ss.setSpan(new ClickableSpan() {
                @Override public void onClick(@NonNull View widget) { 
                    Intent intent = new Intent(context, UserProfileActivity.class);
                    intent.putExtra("targetUserId", notification.getSenderId());
                    intent.putExtra("isViewOnly", true);
                    context.startActivity(intent);
                }
                @Override public void updateDrawState(@NonNull android.text.TextPaint ds) { 
                    ds.setUnderlineText(false); ds.setColor(orange); ds.setFakeBoldText(true); 
                }
            }, nameStart, nameStart + name.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        // 2. Clickable Item (Red/Green)
        int itemStart = fullText.indexOf("\"" + itemName + "\"");
        if (itemStart != -1) {
            ss.setSpan(new ClickableSpan() {
                @Override public void onClick(@NonNull View widget) { 
                    Intent intent = new Intent(context, ItemDetailActivity.class);
                    intent.putExtra("itemId", notification.getItemId());
                    // Deduce status for detail page navigation
                    String status = "lost_claim".equals(type) || "lost_claimed_confirmed".equals(type) || "item_claimed".equals(type) ? "lost" : "found";
                    intent.putExtra("itemStatus", status);
                    intent.putExtra("userId", notification.getRecipientId());
                    context.startActivity(intent);
                }
                @Override public void updateDrawState(@NonNull android.text.TextPaint ds) { 
                    ds.setUnderlineText(false); ds.setColor(itemColor); ds.setFakeBoldText(true); 
                }
            }, itemStart, itemStart + itemName.length() + 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        // 3. Click to view details (Blue Link)
        styleClickToViewDetails(ss, fullText, clickText, notification, blue);
        
        return ss;
    }

    private void styleClickToViewDetails(SpannableString ss, String fullText, String clickText, Notification notification, int blue) {
        int clickStart = fullText.indexOf(clickText);
        if (clickStart != -1) {
            ss.setSpan(new ClickableSpan() {
                @Override public void onClick(@NonNull View widget) { listener.onNotificationClick(notification); }
                @Override public void updateDrawState(@NonNull android.text.TextPaint ds) { ds.setUnderlineText(true); ds.setColor(blue); }
            }, clickStart, clickStart + clickText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private SpannableString styleAdminReportNotification(Notification notification, android.content.Context context) {
        String reportName = notification.getItemName() != null ? notification.getItemName() : "Report";
        String clickText = "Click to view details";
        String fullText = notification.getMessage();

        if (fullText == null || fullText.isEmpty()) {
            // Fallback for old/legacy notifications
            fullText = "Admin has reviewed your \"" + reportName + "\". Click to view details";
        } else if (!fullText.contains(clickText)) {
            // Append click text if missing
            fullText += " " + clickText;
        }

        SpannableString spannableString = new SpannableString(fullText);
        
        // Colors from requirements
        int orangeColor = Color.parseColor("#FF9800");   // Admin
        int neonGreenColor = Color.parseColor("#39FF14"); // Report Name
        int blueColor = Color.parseColor("#2AABEE");     // Click to view

        // 1. Style "Admin"
        int adminStart = fullText.indexOf("Admin");
        if (adminStart != -1) {
            spannableString.setSpan(new ForegroundColorSpan(orangeColor), adminStart, adminStart + 5, 0);
            spannableString.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), adminStart, adminStart + 5, 0);
        }

        // 2. Style Report Name (inside quotes)
        int nameStart = fullText.indexOf("\"" + reportName + "\"");
        if (nameStart != -1) {
            int nameEnd = nameStart + reportName.length() + 2;
            spannableString.setSpan(new ForegroundColorSpan(neonGreenColor), nameStart, nameEnd, 0);
            spannableString.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), nameStart, nameEnd, 0);
            
            // Make report name clickable
            spannableString.setSpan(new ClickableSpan() {
                @Override public void onClick(@NonNull View widget) { if (listener != null) listener.onNotificationClick(notification); }
                @Override public void updateDrawState(@NonNull android.text.TextPaint ds) { ds.setUnderlineText(false); ds.setColor(neonGreenColor); }
            }, nameStart, nameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 3. Style "Click to view details"
        int clickStart = fullText.indexOf(clickText);
        if (clickStart != -1) {
            spannableString.setSpan(new ForegroundColorSpan(blueColor), clickStart, clickStart + clickText.length(), 0);
            spannableString.setSpan(new android.text.style.UnderlineSpan(), clickStart, clickStart + clickText.length(), 0);
            
            spannableString.setSpan(new ClickableSpan() {
                @Override public void onClick(@NonNull View widget) { if (listener != null) listener.onNotificationClick(notification); }
                @Override public void updateDrawState(@NonNull android.text.TextPaint ds) { ds.setUnderlineText(true); ds.setColor(blueColor); }
            }, clickStart, clickStart + clickText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannableString;
    }

    private SpannableString styleClaimNotification(Notification notification, android.content.Context context) {
        String claimerName = notification.getSenderName() != null ? notification.getSenderName() : "A user";
        String itemName = notification.getItemName() != null ? notification.getItemName() : "Unknown Item";
        String type = notification.getType();
        String fullText = notification.getMessage();

        if (fullText == null || fullText.isEmpty()) {
            if ("lost_claim".equals(type)) {
                fullText = "\"" + claimerName + "\" has claimed that they found your \"" + itemName + "\". Click to view details";
            } else {
                fullText = "\"" + claimerName + "\" has claimed that the item \"" + itemName + "\" belongs to them. Click to view details";
            }
        }

        SpannableString ss = new SpannableString(fullText);
        int orange = ContextCompat.getColor(context, R.color.orange);
        int itemColor = "lost_claim".equals(type) ? ContextCompat.getColor(context, R.color.lightRed) : ContextCompat.getColor(context, R.color.statusFound);
        int blue = ContextCompat.getColor(context, R.color.primaryColor);

        // 1. Style Claimer Name (Orange)
        int nameStart = fullText.indexOf("\"" + claimerName + "\"");
        if (nameStart != -1) {
            ss.setSpan(new ForegroundColorSpan(orange), nameStart, nameStart + claimerName.length() + 2, 0);
            ss.setSpan(new StyleSpan(Typeface.BOLD), nameStart, nameStart + claimerName.length() + 2, 0);
        }

        // 2. Style Item Name (Red for Lost, Green for Found)
        int itemStart = fullText.indexOf("\"" + itemName + "\"");
        if (itemStart != -1) {
            ss.setSpan(new ForegroundColorSpan(itemColor), itemStart, itemStart + itemName.length() + 2, 0);
            ss.setSpan(new StyleSpan(Typeface.BOLD), itemStart, itemStart + itemName.length() + 2, 0);
        }

        // 3. Click to view details (Blue Link)
        String clickText = "Click to view details";
        int clickStart = fullText.indexOf(clickText);
        if (clickStart != -1) {
            ss.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    if (listener != null) listener.onNotificationClick(notification);
                }
                @Override
                public void updateDrawState(@NonNull android.text.TextPaint ds) {
                    ds.setUnderlineText(true);
                    ds.setColor(blue);
                }
            }, clickStart, clickStart + clickText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return ss;
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
