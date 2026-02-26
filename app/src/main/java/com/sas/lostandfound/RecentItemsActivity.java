package com.sas.lostandfound;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class RecentItemsActivity extends RecyclerView.Adapter<RecentItemsActivity.ViewHolder> {

    private List<ItemActivity> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(ItemActivity item);
    }

    public RecentItemsActivity(List<ItemActivity> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate a single item row layout – ensure this file exists (e.g., R.layout.item_recent)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_recent_items, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemActivity item = items.get(position);
        // Use existing getters: getImageUrl() for the emoji, getTitle() for the name
        holder.tvIcon.setText(item.getImageUrl());      // was getIcon()
        holder.tvName.setText(item.getTitle());         // was getName()
        holder.tvLocation.setText(item.getLocation());
        holder.tvTimeAgo.setText(item.getTimeAgo());

        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvName, tvLocation, tvTimeAgo;

        ViewHolder(View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tvIcon);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvLocation = itemView.findViewById(R.id.tvItemLocation);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
        }
    }
}