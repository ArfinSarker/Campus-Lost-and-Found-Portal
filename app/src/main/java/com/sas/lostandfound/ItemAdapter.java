package com.sas.lostandfound;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
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
        
        holder.tvName.setText(item.getName());
        holder.tvLocation.setText(item.getLocation());
        holder.tvTimeAgo.setText(item.getDate());

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            holder.ivImage.setVisibility(View.VISIBLE);
            holder.tvEmoji.setVisibility(View.GONE);
            Glide.with(holder.itemView.getContext())
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_package)
                    .centerCrop()
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setVisibility(View.GONE);
            holder.tvEmoji.setVisibility(View.VISIBLE);
            holder.tvEmoji.setText("📦"); // Default icon
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvName, tvLocation, tvTimeAgo;
        ImageView ivImage;

        ViewHolder(View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tvIcon);
            ivImage = itemView.findViewById(R.id.ivItemImage);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvLocation = itemView.findViewById(R.id.tvItemLocation);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
        }
    }
}
