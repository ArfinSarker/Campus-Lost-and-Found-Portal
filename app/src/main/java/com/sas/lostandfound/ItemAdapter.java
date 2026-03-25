package com.sas.lostandfound;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;
import com.bumptech.glide.Glide;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying lost and found items in a RecyclerView.
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    private List<Item> items;
    private OnItemClickListener listener;
    private int layoutId = R.layout.item_list_row;
    private Map<Integer, Runnable> sliderRunnables = new HashMap<>();
    private Handler sliderHandler = new Handler(Looper.getMainLooper());

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
                holder.tvType.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            } else if ("lost".equalsIgnoreCase(item.getStatus())) {
                holder.tvType.setText(R.string.status_lost_label);
                holder.tvType.setBackgroundResource(R.drawable.bg_status_badge_lost);
                holder.tvType.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            } else {
                holder.tvType.setText(R.string.chip_found);
                holder.tvType.setBackgroundResource(R.drawable.bg_status_badge_found);
                holder.tvType.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            }
        }

        setupImageOrSlider(holder, item, position);

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

    private void setupImageOrSlider(ViewHolder holder, Item item, int position) {
        if (holder.ivImage == null) return;

        List<String> urls = item.getImageUrls();
        if (urls != null && urls.size() > 1 && holder.viewPagerSlider != null) {
            holder.ivImage.setVisibility(View.GONE);
            if (holder.tvEmoji != null) holder.tvEmoji.setVisibility(View.GONE);
            holder.viewPagerSlider.setVisibility(View.VISIBLE);
            if (holder.tabLayoutIndicator != null) holder.tabLayoutIndicator.setVisibility(View.VISIBLE);
            
            ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(urls);
            holder.viewPagerSlider.setAdapter(sliderAdapter);
            holder.viewPagerSlider.setUserInputEnabled(true); 

            if (holder.tabLayoutIndicator != null) {
                new TabLayoutMediator(holder.tabLayoutIndicator, holder.viewPagerSlider, (tab, pos) -> {}).attach();
            }

            stopSlider(position);
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (holder.viewPagerSlider != null) {
                        int currentItem = holder.viewPagerSlider.getCurrentItem();
                        int nextItem = (currentItem + 1) % urls.size();
                        holder.viewPagerSlider.setCurrentItem(nextItem, true);
                        sliderHandler.postDelayed(this, 3000);
                    }
                }
            };
            sliderRunnables.put(position, runnable);
            sliderHandler.postDelayed(runnable, 3000);

        } else {
            if (holder.viewPagerSlider != null) holder.viewPagerSlider.setVisibility(View.GONE);
            if (holder.tabLayoutIndicator != null) holder.tabLayoutIndicator.setVisibility(View.GONE);
            stopSlider(position);

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                holder.ivImage.setVisibility(View.VISIBLE);
                if (holder.tvEmoji != null) holder.tvEmoji.setVisibility(View.GONE);
                Glide.with(holder.itemView.getContext())
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.ic_package)
                        .centerCrop()
                        .into(holder.ivImage);
            } else {
                holder.ivImage.setVisibility(View.VISIBLE);
                holder.ivImage.setImageResource(R.drawable.ic_package);
                if (holder.tvEmoji != null) {
                    holder.ivImage.setVisibility(View.GONE);
                    holder.tvEmoji.setVisibility(View.VISIBLE);
                    holder.tvEmoji.setText("📦");
                }
            }
        }
    }

    private void stopSlider(int position) {
        Runnable runnable = sliderRunnables.get(position);
        if (runnable != null) {
            sliderHandler.removeCallbacks(runnable);
            sliderRunnables.remove(position);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        stopSlider(holder.getBindingAdapterPosition());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvName, tvLocation, tvTimeAgo;
        TextView tvTitle, tvType, tvDate, btnViewDetails, tvReportId;
        ImageView ivImage;
        ViewPager2 viewPagerSlider;
        TabLayout tabLayoutIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tvIcon);
            tvName = itemView.findViewById(R.id.tvItemName);
            tvLocation = itemView.findViewById(R.id.tvItemLocation);
            tvTimeAgo = itemView.findViewById(R.id.tvTimeAgo);
            tvTitle = itemView.findViewById(R.id.tvItemTitle);
            tvType = itemView.findViewById(R.id.tvItemType);
            tvDate = itemView.findViewById(R.id.tvItemDate);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            tvReportId = itemView.findViewById(R.id.tvReportId);
            ivImage = itemView.findViewById(R.id.ivItemImage);
            viewPagerSlider = itemView.findViewById(R.id.viewPagerSlider);
            tabLayoutIndicator = itemView.findViewById(R.id.tabLayoutIndicator);
        }
    }
}
