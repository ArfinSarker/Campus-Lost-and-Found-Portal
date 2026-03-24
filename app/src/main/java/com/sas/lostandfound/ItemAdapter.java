package com.sas.lostandfound;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

/**
 * Adapter for displaying lost and found items in a RecyclerView.
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<Item> items;
    private OnItemClickListener listener;
    private int layoutId = R.layout.item_list_row;

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    public ItemAdapter(List<Item> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public ItemAdapter(List<Item> items, int layoutId, OnItemClickListener listener) {
        this.items = items;
        this.layoutId = layoutId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(layoutId, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item item = items.get(position);
        
        if (holder.tvName != null) holder.tvName.setText(item.getName());
        if (holder.tvTitle != null) holder.tvTitle.setText(item.getName());
        
        if (holder.tvLocation != null) holder.tvLocation.setText(item.getLocation());
        
        if (holder.tvTimeAgo != null) holder.tvTimeAgo.setText(item.getDate());
        if (holder.tvDate != null) holder.tvDate.setText(item.getDate());
        
        if (holder.tvReportId != null) {
            holder.tvReportId.setText(item.getDisplayId() != null ? item.getDisplayId() : "");
        }

        if (holder.tvType != null) {
            String adminStatus = item.getAdminStatus();
            if ("Claimed".equalsIgnoreCase(adminStatus) || "Returned".equalsIgnoreCase(adminStatus)) {
                holder.tvType.setText(R.string.status_claimed_label);
                holder.tvType.setBackgroundResource(R.drawable.bg_status_badge_found);
                // Set text color to white for better readability in Browse view
                holder.tvType.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            } else if ("lost".equalsIgnoreCase(item.getStatus())) {
                holder.tvType.setText(R.string.status_lost_label);
                holder.tvType.setBackgroundResource(R.drawable.bg_status_badge_lost);
                // Set text color to white for better readability in Browse view
                holder.tvType.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            } else {
                holder.tvType.setText(R.string.chip_found);
                holder.tvType.setBackgroundResource(R.drawable.bg_status_badge_found);
                // Set text color to white for better readability in Browse view
                holder.tvType.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            }
        }

        if (holder.ivImage != null) {
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                holder.ivImage.setVisibility(View.VISIBLE);
                if (holder.tvEmoji != null) holder.tvEmoji.setVisibility(View.GONE);
                Glide.with(holder.itemView.getContext())
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.ic_package)
                        .centerCrop()
                        .into(holder.ivImage);
            } else {
                holder.ivImage.setImageResource(R.drawable.ic_package);
                if (holder.tvEmoji != null) {
                    holder.ivImage.setVisibility(View.GONE);
                    holder.tvEmoji.setVisibility(View.VISIBLE);
                    holder.tvEmoji.setText("📦");
                }
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
        
        if (holder.btnViewDetails != null) {
            holder.btnViewDetails.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvName, tvLocation, tvTimeAgo;
        TextView tvTitle, tvType, tvDate, btnViewDetails, tvReportId;
        ImageView ivImage;

        ViewHolder(View itemView) {
            super(itemView);
            // item_list_row
            tvEmoji = itemView.findViewById(R.id.tvIcon);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvLocation = itemView.findViewById(R.id.tvItemLocation);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
            
            // item_browse_card
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvType = itemView.findViewById(R.id.tvItemType);
            tvDate = itemView.findViewById(R.id.tvItemDate);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            tvReportId = itemView.findViewById(R.id.tvReportId);

            // Both use ivItemImage
            ivImage = itemView.findViewById(R.id.ivItemImage);
        }
    }
}
