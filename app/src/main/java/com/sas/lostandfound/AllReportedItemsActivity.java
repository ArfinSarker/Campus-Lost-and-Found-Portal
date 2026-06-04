package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllReportedItemsActivity extends AppCompatActivity {

    private RecyclerView rvAllItems;
    private AllItemsAdapter adapter;
    private List<Item> itemList;
    private List<Item> filteredList;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private com.google.android.material.appbar.AppBarLayout appBarLayout;
    private TextView tvHeaderTitle;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String filterStatus; // "lost", "found", "returned" or null
    private String targetUserId;
    private String userName;
    private boolean isAdmin = false;
    private boolean isFetching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reported_items);

        filterStatus = getIntent().getStringExtra("filterStatus");
        targetUserId = getIntent().getStringExtra("targetUserId");
        userName = getIntent().getStringExtra("userName");
        
        isAdmin = ModeManager.isAdminMode(this);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();

        // Ensure back press always exits the activity immediately
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAllItems();
    }

    private void initializeViews() {
        rvAllItems = findViewById(R.id.rvAllItems);
        progressBar = findViewById(R.id.progressBar);
        toolbar = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.appBarLayout);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout, toolbar);
        }
        
        String prefix = (userName != null && !userName.isEmpty()) ? userName + "'s " : "All ";
        
        if ("lost".equalsIgnoreCase(filterStatus)) {
            tvHeaderTitle.setText(prefix + "Lost Reports");
        } else if ("found".equalsIgnoreCase(filterStatus)) {
            tvHeaderTitle.setText(prefix + "Found Reports");
        } else if ("returned".equalsIgnoreCase(filterStatus) || "resolved".equalsIgnoreCase(filterStatus)) {
            tvHeaderTitle.setText(prefix + "Resolved Items");
        } else {
            tvHeaderTitle.setText("All Reported Items");
        }
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setTitle("");
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        itemList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new AllItemsAdapter(filteredList);
        rvAllItems.setLayoutManager(new LinearLayoutManager(this));
        rvAllItems.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(this::fetchAllItems);
        }
    }

    private void fetchAllItems() {
        if (isFetching) return;
        isFetching = true;

        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        List<Item> accumulatedItems = new ArrayList<>();

        // Fetch Lost Items
        String lostQuery = "deleted_by_user=eq.false";
        if (targetUserId != null) lostQuery += "&reporter_id=eq." + targetUserId;

        String foundQuery = "deleted_by_user=eq.false";
        if (targetUserId != null) foundQuery += "&reporter_id=eq." + targetUserId;

        final int[] completedCount = {0};
        final List<Item> lostList = new ArrayList<>();
        final List<Item> foundList = new ArrayList<>();

        SupabaseDatabaseHelper.DatabaseCallback<List<Item>> lostCallback = new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> items) {
                if (items != null) lostList.addAll(items);
                checkCompletion();
            }
            @Override public void onFailure(String e) { checkCompletion(); }

            private void checkCompletion() {
                completedCount[0]++;
                if (completedCount[0] == 2) finalizeList(lostList, foundList);
            }
        };

        SupabaseDatabaseHelper.DatabaseCallback<List<Item>> foundCallback = new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> items) {
                if (items != null) foundList.addAll(items);
                checkCompletion();
            }
            @Override public void onFailure(String e) { checkCompletion(); }

            private void checkCompletion() {
                completedCount[0]++;
                if (completedCount[0] == 2) finalizeList(lostList, foundList);
            }
        };

        SupabaseDatabaseHelper.select("lost_reports", lostQuery, new TypeToken<List<Item>>(){}.getType(), lostCallback);
        SupabaseDatabaseHelper.select("found_reports", foundQuery, new TypeToken<List<Item>>(){}.getType(), foundCallback);
    }

    private void finalizeList(List<Item> lostList, List<Item> foundList) {
        itemList.clear();
        itemList.addAll(lostList);
        
        for (Item item : foundList) {
            boolean exists = false;
            for (Item existing : lostList) {
                if (existing.getId().equals(item.getId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) itemList.add(item);
        }

        applyFilter();
        
        isFetching = false;
        progressBar.setVisibility(View.GONE);
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
    }

    private void applyFilter() {
        List<Item> newFilteredList = new ArrayList<>();
        for (Item item : itemList) {
            boolean matchesUser = (targetUserId == null || item.getUserId().equals(targetUserId));
            if (!matchesUser) continue;

            if (filterStatus == null) {
                newFilteredList.add(item);
            } else if ("returned".equalsIgnoreCase(filterStatus) || "resolved".equalsIgnoreCase(filterStatus)) {
                String status = item.getAdminStatus();
                if ("Returned".equalsIgnoreCase(status) || "Claimed".equalsIgnoreCase(status)) {
                    newFilteredList.add(item);
                }
            } else if (item.getStatus().equalsIgnoreCase(filterStatus)) {
                newFilteredList.add(item);
            }
        }
        newFilteredList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

        // Calculate diff between filteredList and newFilteredList
        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return filteredList.size();
            }

            @Override
            public int getNewListSize() {
                return newFilteredList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Item oldItem = filteredList.get(oldItemPosition);
                Item newItem = newFilteredList.get(newItemPosition);
                return oldItem.getId() != null && newItem.getId() != null && oldItem.getId().equals(newItem.getId());
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Item oldItem = filteredList.get(oldItemPosition);
                Item newItem = newFilteredList.get(newItemPosition);
                return java.util.Objects.equals(oldItem.getName(), newItem.getName()) &&
                       java.util.Objects.equals(oldItem.getLocation(), newItem.getLocation()) &&
                       java.util.Objects.equals(oldItem.getDate(), newItem.getDate()) &&
                       java.util.Objects.equals(oldItem.getAdminStatus(), newItem.getAdminStatus()) &&
                       java.util.Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                       java.util.Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl()) &&
                       java.util.Objects.equals(oldItem.getImageUrls(), newItem.getImageUrls());
            }
        });

        filteredList.clear();
        filteredList.addAll(newFilteredList);
        diffResult.dispatchUpdatesTo(adapter);
    }

    private class AllItemsAdapter extends RecyclerView.Adapter<AllItemsAdapter.ViewHolder> {
        private List<Item> items;
        private Map<Integer, Runnable> sliderRunnables = new HashMap<>();
        private Handler sliderHandler = new Handler(Looper.getMainLooper());

        public AllItemsAdapter(List<Item> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_campus_reported_recent, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Item item = items.get(position);
            holder.tvTitle.setText(item.getName());
            
            holder.tvLocation.setText(ReportLocationDisplay.formatFullLocation(
                    item.getLocation(), 
                    item.getManualLocation(), 
                    item.getAdditionalLocationDetails()));
                    
            holder.tvTime.setText(item.getDate());

            if (holder.tvReportId != null) {
                String displayId = item.getDisplayId();
                if (displayId == null || displayId.isEmpty()) {
                    displayId = item.getReportId();
                }

                String formattedId = ReportIdFormatter.format(displayId);
                if (!formattedId.isEmpty()) {
                    holder.tvReportId.setText(formattedId);
                    View parent = (View) holder.tvReportId.getParent();
                    if (parent != null) parent.setVisibility(View.VISIBLE);
                } else {
                    holder.tvReportId.setText("");
                    View parent = (View) holder.tvReportId.getParent();
                    if (parent != null) parent.setVisibility(View.GONE);
                }
            }

            if ("lost".equals(item.getStatus())) {
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.statusLost));
                holder.tvBadge.setText("LOST");
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_lost_bg));
            } else {
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.statusFound));
                holder.tvBadge.setText("FOUND");
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_found_bg));
            }
            holder.tvBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

            setupImageOrSlider(holder, item, position);

            // Entire card click leads to details - Use Activity's isAdmin flag
            holder.itemView.setOnClickListener(v -> ItemNavigationUtils.navigateToDetail(v.getContext(), item, isAdmin));
        }

        private void setupImageOrSlider(ViewHolder holder, Item item, int position) {
            List<String> urls = item.getImageUrls();
            if (urls != null && urls.size() > 1 && holder.viewPagerSlider != null) {
                holder.ivIcon.setVisibility(View.GONE);
                holder.viewPagerSlider.setVisibility(View.VISIBLE);
                holder.tabLayoutIndicator.setVisibility(View.VISIBLE);

                // Use fitCenter (true) for multiple images to prevent zooming in cards
                ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(urls, true);
                // Slider clicks lead to details - Use Activity's isAdmin flag
                sliderAdapter.setOnImageClickListener(pos -> ItemNavigationUtils.navigateToDetail(holder.itemView.getContext(), item, isAdmin));
                
                holder.viewPagerSlider.setAdapter(sliderAdapter);
                holder.viewPagerSlider.setUserInputEnabled(true);

                new TabLayoutMediator(holder.tabLayoutIndicator, holder.viewPagerSlider, (tab, pos) -> {}).attach();

                stopSlider(position);
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (holder.viewPagerSlider != null) {
                            int current = holder.viewPagerSlider.getCurrentItem();
                            int next = (current + 1) % urls.size();
                            holder.viewPagerSlider.setCurrentItem(next, true);
                            sliderHandler.postDelayed(this, 3000);
                        }
                    }
                };
                sliderRunnables.put(position, runnable);
                sliderHandler.postDelayed(runnable, 3000);
            } else {
                if (holder.viewPagerSlider != null) holder.viewPagerSlider.setVisibility(View.GONE);
                if (holder.tabLayoutIndicator != null) holder.tabLayoutIndicator.setVisibility(View.GONE);
                holder.ivIcon.setVisibility(View.VISIBLE);
                stopSlider(position);

                if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                    holder.ivIcon.setImageTintList(null);
                    holder.ivIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    GlideApp.with(holder.itemView.getContext())
                            .load(item.getImageUrl())
                            .placeholder(R.drawable.ic_package)
                            .thumbnail(0.1f)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(holder.ivIcon);
                            
                    // Image click leads to details - Use Activity's isAdmin flag
                    holder.ivIcon.setOnClickListener(v -> ItemNavigationUtils.navigateToDetail(v.getContext(), item, isAdmin));
                } else {
                    holder.ivIcon.setImageResource(R.drawable.ic_package);
                    holder.ivIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    holder.ivIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(holder.itemView.getContext(), R.color.textSecondary)));
                    // Navigate even for placeholders - Use Activity's isAdmin flag
                    holder.ivIcon.setOnClickListener(v -> ItemNavigationUtils.navigateToDetail(v.getContext(), item, isAdmin));
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

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvLocation, tvTime, tvBadge, tvReportId;
            ImageView ivIcon;
            View statusIndicator;
            MaterialCardView cardBadge;
            ViewPager2 viewPagerSlider;
            TabLayout tabLayoutIndicator;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvItemTitle);
                tvLocation = itemView.findViewById(R.id.tvItemLocation);
                tvTime = itemView.findViewById(R.id.tvItemTime);
                ivIcon = itemView.findViewById(R.id.ivItemIcon);
                statusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
                tvBadge = itemView.findViewById(R.id.tvBadge);
                cardBadge = itemView.findViewById(R.id.cardBadge);
                viewPagerSlider = itemView.findViewById(R.id.viewPagerSlider);
                tabLayoutIndicator = itemView.findViewById(R.id.tabLayoutIndicator);
                tvReportId = itemView.findViewById(R.id.tvReportId);
            }
        }
    }
}
