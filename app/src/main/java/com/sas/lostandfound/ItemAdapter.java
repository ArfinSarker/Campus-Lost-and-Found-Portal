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
import com.bumptech.glide.load.engine.DiskCacheStrategy;
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
    private boolean isAdmin = false;
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

    public ItemAdapter(List<Item> items, OnItemClickListener listener, boolean isAdmin) {
        this.items = items;
        this.listener = listener;
        this.isAdmin = isAdmin;
    }

    public ItemAdapter(List<Item> items, int layoutId, OnItemClickListener listener, boolean isAdmin) {
        this.items = items;
        this.layoutId = layoutId;
        this.listener = listener;
        this.isAdmin = isAdmin;
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
        
        if (holder.tvLocation != null) {
            holder.tvLocation.setText(ReportLocationDisplay.formatFullLocation(
                    item.getLocation(), 
                    item.getManualLocation(), 
                    item.getAdditionalLocationDetails()));
        }
        
        if (holder.tvTimeAgo != null) holder.tvTimeAgo.setText(item.getDate());
        if (holder.tvDate != null) holder.tvDate.setText(item.getDate());
        
        if (holder.tvReportId != null) {
            String displayId = item.getDisplayId();
            if (displayId == null || displayId.isEmpty()) {
                displayId = item.getReportId();
            }
            holder.tvReportId.setText(ReportIdFormatter.format(displayId));

            // Status-based colors for the report ID badge
            String adminStatus = item.getAdminStatus();
            int color;
            if ("Claimed".equalsIgnoreCase(adminStatus) || "Returned".equalsIgnoreCase(adminStatus)) {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.orange); // Orange/Gold
            } else if ("lost".equalsIgnoreCase(item.getStatus())) {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_lost_bg); // Red
            } else {
                color = ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_found_bg); // Green
            }
            holder.tvReportId.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        }

        if (holder.tvType != null) {
            String adminStatus = item.getAdminStatus();
            if ("Claimed".equalsIgnoreCase(adminStatus) || "Returned".equalsIgnoreCase(adminStatus)) {
                holder.tvType.setText(R.string.status_resolved);
                holder.tvType.setBackgroundResource(R.drawable.bg_status_badge_resolved);
                holder.tvType.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            } else if ("lost".equalsIgnoreCase(item.getStatus())) {
                holder.tvType.setText(R.string.status_lost_label);
                holder.tvType.setBackgroundResource(R.drawable.bg_status_badge_lost);
                holder.tvType.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            } else {
                holder.tvType.setText(R.string.status_found_label);
                holder.tvType.setBackgroundResource(R.drawable.bg_status_badge_found);
                holder.tvType.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            }
        }

        setupImageOrSlider(holder, item, position);

        // Entire card click leads to details
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            } else {
                ItemNavigationUtils.navigateToDetail(v.getContext(), item, isAdmin);
            }
        });

        if (holder.btnViewDetails != null) {
            holder.btnViewDetails.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                } else {
                    ItemNavigationUtils.navigateToDetail(v.getContext(), item, isAdmin);
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
            
            // Use fitCenter (true) for multiple images to prevent zooming in cards
            ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(urls, true);
            // Image click in slider also leads to details
            sliderAdapter.setOnImageClickListener(pos -> {
                if (listener != null) {
                    listener.onItemClick(item);
                } else {
                    ItemNavigationUtils.navigateToDetail(holder.itemView.getContext(), item, isAdmin);
                }
            });
            
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
                holder.ivImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
                if (holder.tvEmoji != null) holder.tvEmoji.setVisibility(View.GONE);
                GlideApp.with(holder.itemView.getContext())
                        .load(SupabaseStorageHelper.ensurePublicUrl(item.getImageUrl()))
                        .placeholder(R.drawable.ic_package)
                        .thumbnail(0.1f)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .into(holder.ivImage);
                
                // Single image click also leads to details
                holder.ivImage.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemClick(item);
                    } else {
                        ItemNavigationUtils.navigateToDetail(v.getContext(), item, isAdmin);
                    }
                });
            } else {
                holder.ivImage.setVisibility(View.VISIBLE);
                holder.ivImage.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                holder.ivImage.setImageResource(R.drawable.ic_package);
                // Even if placeholder, click leads to details
                holder.ivImage.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onItemClick(item);
                    } else {
                        ItemNavigationUtils.navigateToDetail(v.getContext(), item, isAdmin);
                    }
                });
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

    public void updateItems(List<Item> newItems) {
        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return items.size();
            }

            @Override
            public int getNewListSize() {
                return newItems.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Item oldItem = items.get(oldItemPosition);
                Item newItem = newItems.get(newItemPosition);
                return oldItem.getId() != null && newItem.getId() != null && oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Item oldItem = items.get(oldItemPosition);
                Item newItem = newItems.get(newItemPosition);
                return java.util.Objects.equals(oldItem.getName(), newItem.getName()) &&
                       java.util.Objects.equals(oldItem.getLocation(), newItem.getLocation()) &&
                       java.util.Objects.equals(oldItem.getDate(), newItem.getDate()) &&
                       java.util.Objects.equals(oldItem.getAdminStatus(), newItem.getAdminStatus()) &&
                       java.util.Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                       java.util.Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl()) &&
                       java.util.Objects.equals(oldItem.getImageUrls(), newItem.getImageUrls());
            }
        });

        this.items.clear();
        this.items.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }
}
