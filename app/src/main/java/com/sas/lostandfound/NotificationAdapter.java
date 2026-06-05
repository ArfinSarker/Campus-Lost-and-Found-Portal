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
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;

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
        } else if ("admin_request".equals(type)) {
            holder.tvMessage.setText(styleAdminRequestNotification(notification, holder.itemView.getContext()));
            holder.itemView.setOnClickListener(null);
        } else {
            String fullMsg = notification.getMessage();
            if (fullMsg != null && fullMsg.contains("Click to view details")) {
                int start = fullMsg.indexOf("Click to view details");
                int end = start + "Click to view details".length();
                SpannableString spannableString = new SpannableString(fullMsg);
                spannableString.setSpan(new UnderlineSpan(), start, end, 0);
                spannableString.setSpan(new ForegroundColorSpan(ContextCompat.getColor(holder.itemView.getContext(), R.color.scienceBlue)), start, end, 0);
                
                // Add click for simple "Click to view details"
                spannableString.setSpan(new ClickableSpan() {
                    @Override public void onClick(@NonNull View widget) { listener.onNotificationClick(notification); }
                    @Override public void updateDrawState(@NonNull android.text.TextPaint ds) { ds.setUnderlineText(true); ds.setColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.scienceBlue)); }
                }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                holder.tvMessage.setText(spannableString);
                holder.itemView.setOnClickListener(null);
            } else {
                holder.tvMessage.setText(fullMsg);
                holder.itemView.setOnClickListener(v -> listener.onNotificationClick(notification));
            }
        }

        android.content.Context context = holder.itemView.getContext();

        // Load sender image if available, otherwise show type-specific icon
        if (notification.getSenderImageUrl() != null && !notification.getSenderImageUrl().isEmpty()) {
            GlideApp.with(context)
                    .load(notification.getSenderImageUrl())
                    .placeholder(R.drawable.ic_user)
                    .into(holder.ivIcon);
            holder.ivIcon.setPadding(0, 0, 0, 0);
            holder.ivIcon.setBackgroundResource(R.drawable.bg_notification_icon_container);
            holder.ivIcon.setBackgroundTintList(null);
            holder.ivIcon.setImageTintList(null);
        } else {
            int iconRes = R.drawable.ic_notification;
            int tintColor = ContextCompat.getColor(context, R.color.primaryColor);

            if ("lost_item".equals(type)) {
                iconRes = R.drawable.ic_lost_tag;
                tintColor = ContextCompat.getColor(context, R.color.statusLost);
            } else if ("found_item".equals(type)) {
                iconRes = R.drawable.ic_found_tag;
                tintColor = ContextCompat.getColor(context, R.color.statusFound);
            } else if ("lost_claim".equals(type)) {
                iconRes = R.drawable.ic_alert_match;
                tintColor = ContextCompat.getColor(context, R.color.warningColor);
            } else if ("found_claim".equals(type)) {
                iconRes = R.drawable.ic_id_card;
                tintColor = ContextCompat.getColor(context, R.color.warningColor);
            } else if ("item_claimed".equals(type)) {
                iconRes = R.drawable.ic_user_check;
                tintColor = ContextCompat.getColor(context, R.color.statusFound);
            } else if ("lost_claimed_confirmed".equals(type)) {
                iconRes = R.drawable.ic_check_square;
                tintColor = ContextCompat.getColor(context, R.color.statusFound);
            } else if ("item_return".equals(type)) {
                iconRes = R.drawable.ic_package;
                tintColor = ContextCompat.getColor(context, R.color.statusFound);
            } else if ("item_returned_confirmed".equals(type)) {
                iconRes = R.drawable.ic_check_circle;
                tintColor = ContextCompat.getColor(context, R.color.statusFound);
            } else if ("admin_report".equals(type)) {
                iconRes = R.drawable.ic_shield;
                tintColor = ContextCompat.getColor(context, R.color.admin_accent);
            } else if ("admin_report_new".equals(type)) {
                iconRes = R.drawable.ic_report_management;
                tintColor = ContextCompat.getColor(context, R.color.admin_accent);
            } else if ("admin_request".equals(type)) {
                iconRes = R.drawable.ic_status;
                tintColor = ContextCompat.getColor(context, R.color.admin_accent);
            }

            int bgTint = ColorUtils.setAlphaComponent(tintColor, 21);

            holder.ivIcon.setImageResource(iconRes);
            int paddingPx = dpToPx(context, 10);
            holder.ivIcon.setPadding(paddingPx, paddingPx, paddingPx, paddingPx);
            holder.ivIcon.setBackgroundResource(R.drawable.bg_notification_icon_container);
            holder.ivIcon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgTint));
            holder.ivIcon.setImageTintList(android.content.res.ColorStateList.valueOf(tintColor));
        }

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        holder.tvTime.setText(sdf.format(new Date(notification.getTimestamp())));

        if (notification.isRead()) {
            holder.tvMessage.setTypeface(null, Typeface.NORMAL);
            holder.viewUnread.setVisibility(View.GONE);
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.surfaceColor));
            holder.cardView.setStrokeColor(ContextCompat.getColor(context, R.color.dividerColor));
            holder.cardView.setStrokeWidth(dpToPx(context, 1));
            holder.itemView.setAlpha(1.0f);
        } else {
            holder.tvMessage.setTypeface(null, Typeface.BOLD);
            holder.viewUnread.setVisibility(View.VISIBLE);
            holder.cardView.setCardBackgroundColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(context, R.color.primaryColor), 12));
            holder.cardView.setStrokeColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(context, R.color.primaryColor), 50));
            holder.cardView.setStrokeWidth(dpToPx(context, 1));
            holder.itemView.setAlpha(1.0f);
        }

        if (!"admin_report".equals(type) && !"admin_report_new".equals(type) && !"admin_request".equals(type)) {
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
        
        if ("item_claimed".equals(type) || "lost_claimed_confirmed".equals(type) || "lost_claim".equals(type)) {
            action = " has marked that they received ";
            itemColor = ContextCompat.getColor(context, R.color.statusLost);
        } else {
            action = " has marked that they returned ";
            itemColor = ContextCompat.getColor(context, R.color.statusFound);
        }
        
        String clickText = "Click to view details";
        String fullText = "\"" + name + "\"" + action + "\"" + itemName + "\" from you. " + clickText + ".";
        
        SpannableString ss = new SpannableString(fullText);
        int nameColor = ContextCompat.getColor(context, R.color.orange);
        int linkColor = ContextCompat.getColor(context, R.color.scienceBlue);
        
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
                    ds.setUnderlineText(false); ds.setColor(nameColor); ds.setFakeBoldText(true); 
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
        styleClickToViewDetails(ss, fullText, clickText, notification, linkColor);
        
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
        
        // High-contrast professional colors
        int adminColor = ContextCompat.getColor(context, R.color.admin_accent);
        int reportNameColor = ContextCompat.getColor(context, R.color.statusFound);
        int linkColor = ContextCompat.getColor(context, R.color.scienceBlue);

        // 1. Style "Admin"
        int adminStart = fullText.indexOf("Admin");
        if (adminStart != -1) {
            spannableString.setSpan(new ForegroundColorSpan(adminColor), adminStart, adminStart + 5, 0);
            spannableString.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), adminStart, adminStart + 5, 0);
        }

        // 2. Style Report Name (inside quotes)
        int nameStart = fullText.indexOf("\"" + reportName + "\"");
        if (nameStart != -1) {
            int nameEnd = nameStart + reportName.length() + 2;
            spannableString.setSpan(new ForegroundColorSpan(reportNameColor), nameStart, nameEnd, 0);
            spannableString.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), nameStart, nameEnd, 0);
            
            // Make report name clickable
            spannableString.setSpan(new ClickableSpan() {
                @Override public void onClick(@NonNull View widget) { if (listener != null) listener.onNotificationClick(notification); }
                @Override public void updateDrawState(@NonNull android.text.TextPaint ds) { ds.setUnderlineText(false); ds.setColor(reportNameColor); }
            }, nameStart, nameEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 3. Style "Click to view details"
        int clickStart = fullText.indexOf(clickText);
        if (clickStart != -1) {
            spannableString.setSpan(new ForegroundColorSpan(linkColor), clickStart, clickStart + clickText.length(), 0);
            spannableString.setSpan(new android.text.style.UnderlineSpan(), clickStart, clickStart + clickText.length(), 0);
            
            spannableString.setSpan(new ClickableSpan() {
                @Override public void onClick(@NonNull View widget) { if (listener != null) listener.onNotificationClick(notification); }
                @Override public void updateDrawState(@NonNull android.text.TextPaint ds) { ds.setUnderlineText(true); ds.setColor(linkColor); }
            }, clickStart, clickStart + clickText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannableString;
    }

    private SpannableString styleAdminRequestNotification(Notification notification, android.content.Context context) {
        String requesterName = notification.getSenderName() != null ? notification.getSenderName() : "A user";
        String clickText = "Click to view details";
        String fullText = notification.getMessage();

        if (fullText == null || fullText.isEmpty()) {
            fullText = "\"" + requesterName + "\" has requested admin access. Click to view details";
        } else if (!fullText.contains(clickText)) {
            fullText += " " + clickText;
        }

        SpannableString ss = new SpannableString(fullText);
        
        int nameColor = ContextCompat.getColor(context, R.color.orange);
        int linkColor = ContextCompat.getColor(context, R.color.scienceBlue);

        // 1. Style Requester Name (inside quotes)
        int nameStart = fullText.indexOf("\"" + requesterName + "\"");
        if (nameStart != -1) {
            int nameEnd = nameStart + requesterName.length() + 2;
            ss.setSpan(new ForegroundColorSpan(nameColor), nameStart, nameEnd, 0);
            ss.setSpan(new StyleSpan(Typeface.BOLD), nameStart, nameEnd, 0);
        }

        // 2. Style "Click to view details"
        styleClickToViewDetails(ss, fullText, clickText, notification, linkColor);

        return ss;
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
        int nameColor = ContextCompat.getColor(context, R.color.orange);
        int itemColor = "lost_claim".equals(type) ? ContextCompat.getColor(context, R.color.statusLost) : ContextCompat.getColor(context, R.color.statusFound);
        int linkColor = ContextCompat.getColor(context, R.color.scienceBlue);

        // 1. Style Claimer Name (Orange)
        int nameStart = fullText.indexOf("\"" + claimerName + "\"");
        if (nameStart != -1) {
            ss.setSpan(new ForegroundColorSpan(nameColor), nameStart, nameStart + claimerName.length() + 2, 0);
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
                    ds.setColor(linkColor);
                }
            }, clickStart, clickStart + clickText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return ss;
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    private int dpToPx(android.content.Context context, float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        View viewUnread, llRoot;
        ImageButton btnDelete;
        ImageView ivIcon;
        MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            viewUnread = itemView.findViewById(R.id.viewUnreadIndicator);
            llRoot = itemView.findViewById(R.id.llNotificationRoot);
            btnDelete = itemView.findViewById(R.id.btnDeleteNotification);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            cardView = itemView.findViewById(R.id.cvNotificationCard);
        }
    }

    public void updateNotifications(List<Notification> newNotifications) {
        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return notifications.size();
            }

            @Override
            public int getNewListSize() {
                return newNotifications.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Notification oldItem = notifications.get(oldItemPosition);
                Notification newItem = newNotifications.get(newItemPosition);
                return oldItem.getId() != null && newItem.getId() != null && oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Notification oldItem = notifications.get(oldItemPosition);
                Notification newItem = newNotifications.get(newItemPosition);
                return oldItem.isRead() == newItem.isRead() &&
                       java.util.Objects.equals(oldItem.getMessage(), newItem.getMessage()) &&
                       java.util.Objects.equals(oldItem.getType(), newItem.getType()) &&
                       oldItem.getTimestamp() == newItem.getTimestamp();
            }
        });

        this.notifications.clear();
        this.notifications.addAll(newNotifications);
        diffResult.dispatchUpdatesTo(this);
    }
}
