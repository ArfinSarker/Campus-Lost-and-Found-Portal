package com.sas.lostandfound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CampusMyItemsActivity extends AppCompatActivity {

    private static final String TAG = "CampusMyItems";
    private RecyclerView rvMyItems;
    private MyItemsAdapter adapter;
    private List<Item> itemList;
    private String filterType;
    private TextView tvHeaderTitle;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean fromDrawer = false;
    private boolean isFetching = false;

    private String currentUniversityId;
    private String currentAuthId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_my_items);

        filterType = getIntent().getStringExtra("filterType");
        if (filterType == null) filterType = "reported";

        fromDrawer = getIntent().getBooleanExtra("fromDrawer", false);

        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUniversityId = prefs.getString("universityId", null);
        currentAuthId = prefs.getString("authId", null);

        rvMyItems = findViewById(R.id.rvMyItems);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        toolbar = findViewById(R.id.toolbar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        setupBackNavigation();
        setupTitle();
        setupSwipeRefresh();

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout, toolbar);
        }

        itemList = new ArrayList<>();
        adapter = new MyItemsAdapter(itemList);
        rvMyItems.setLayoutManager(new LinearLayoutManager(this));
        rvMyItems.setAdapter(adapter);
        rvMyItems.setHasFixedSize(true);

        fetchMyItems();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchMyItems();
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        // UI refinement after activity transition
    }

    /**
     * Dedicated function to handle back navigation logic.
     * Ensures that if the activity was launched from the navigation drawer,
     * the drawer is re-opened when returning to the dashboard.
     */
    private void setupBackNavigation() {
        View.OnClickListener backClickListener = v -> {
            Intent intent = new Intent(CampusMyItemsActivity.this, CampusDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (fromDrawer) {
                intent.putExtra("openDrawer", true);
                intent.putExtra("selectedItemId", getNavIdForFilter(filterType));
            }
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.material_shared_axis_z_pop_enter, R.anim.material_shared_axis_z_pop_exit);
        };

        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(backClickListener);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backClickListener.onClick(null);
            }
        });
    }

    /**
     * Dedicated function to map the current filter type to its corresponding navigation menu item ID.
     */
    private int getNavIdForFilter(String filter) {
        if ("reported".equals(filter)) return R.id.nav_reported_items;
        if ("find".equals(filter)) return R.id.nav_find_items;
        if ("resolved".equals(filter)) return R.id.nav_resolved_items;
        if ("admin_reports".equals(filter)) return R.id.nav_admin_reports;
        return -1;
    }

    private void setupTitle() {
        switch (filterType) {
            case "reported":
                tvHeaderTitle.setText("My Found Reports");
                break;
            case "find":
                tvHeaderTitle.setText("My Lost Reports");
                break;
            case "resolved":
                tvHeaderTitle.setText("My Resolved Items");
                break;
            case "admin_reports":
                tvHeaderTitle.setText("My Admin Reports");
                break;
            default:
                tvHeaderTitle.setText("My Reports");
                break;
        }
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(this::fetchMyItems);
        }
    }

    private void fetchMyItems() {
        if (currentUniversityId == null) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            SnackbarManager.show(SnackbarManager.Type.ERROR, "User session not found. Please login again.");
            return;
        }

        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);
        loadItems(currentUniversityId);
    }

    private void removeListeners() {
        // Listeners were removed in favor of Supabase REST calls
    }

    private void loadItems(String universityId) {
        if (isFetching) return;
        isFetching = true;

        List<Item> accumulatedItems = new ArrayList<>();

        if ("admin_reports".equals(filterType)) {
            fetchAdminReportsInternal(universityId, currentAuthId, accumulatedItems);
        } else if ("resolved".equals(filterType)) {
            fetchResolvedItems(universityId, currentAuthId, accumulatedItems);
        } else {
            String table = "reported".equals(filterType) ? "found_reports" : "lost_reports";
            String type = "reported".equals(filterType) ? "found" : "lost";

            // Use OR filter for better compatibility
            String query = "or=(reporter_id.eq." + universityId + (currentAuthId != null ? ",user_id.eq." + currentAuthId : "") + ")";

            SupabaseDatabaseHelper.select(table, query, new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
                @Override
                public void onSuccess(List<Item> items) {
                    if (items != null) {
                        for (Item item : items) {
                            item.setType(type);
                            if (shouldInclude(item, universityId, currentAuthId)) accumulatedItems.add(item);
                        }
                    }
                    finalizeAndDisplay(accumulatedItems);
                }

                @Override
                public void onFailure(String e) {
                    finalizeAndDisplay(accumulatedItems);
                }
            });
        }
    }

    private void fetchResolvedItems(String universityId, String authId, List<Item> accumulatedItems) {
        fetchLostResolved(universityId, authId, accumulatedItems);
    }

    private void fetchLostResolved(String universityId, String authId, List<Item> accumulatedItems) {
        String filter = "or=(reporter_id.eq." + universityId + ",claimed_by_id.eq." + universityId + (authId != null ? ",user_id.eq." + authId : "") + ")";
        SupabaseDatabaseHelper.select("lost_reports", filter, new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> items) {
                if (items != null) {
                    for (Item item : items) {
                        item.setType("lost");
                        if (shouldInclude(item, universityId, authId)) accumulatedItems.add(item);
                    }
                }
                fetchFoundResolved(universityId, authId, accumulatedItems);
            }
            @Override public void onFailure(String e) { fetchFoundResolved(universityId, authId, accumulatedItems); }
        });
    }

    private void fetchFoundResolved(String universityId, String authId, List<Item> accumulatedItems) {
        String filter = "or=(reporter_id.eq." + universityId + ",claimed_by_id.eq." + universityId + (authId != null ? ",user_id.eq." + authId : "") + ")";
        SupabaseDatabaseHelper.select("found_reports", filter, new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> items) {
                if (items != null) {
                    for (Item item : items) {
                        item.setType("found");
                        if (shouldInclude(item, universityId, authId)) accumulatedItems.add(item);
                    }
                }
                finalizeAndDisplay(accumulatedItems);
            }
            @Override public void onFailure(String e) { finalizeAndDisplay(accumulatedItems); }
        });
    }

    private void fetchAdminReportsInternal(String universityId, String authId, List<Item> accumulatedItems) {
        StringBuilder filterBuilder = new StringBuilder();
        filterBuilder.append("or=(");
        boolean hasFilter = false;
        if (universityId != null && !universityId.isEmpty()) {
            filterBuilder.append("reporter_id.eq.").append(universityId);
            hasFilter = true;
        }
        if (authId != null && !authId.isEmpty()) {
            if (hasFilter) filterBuilder.append(",");
            filterBuilder.append("reporter_auth_id.eq.").append(authId);
            hasFilter = true;
        }
        filterBuilder.append(")");

        String filter = hasFilter ? filterBuilder.toString() : "id=not.is.null";

        SupabaseDatabaseHelper.select("admin_reports", filter, new TypeToken<List<AdminReport>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<AdminReport>>() {
            @Override
            public void onSuccess(List<AdminReport> reports) {
                if (reports != null) {
                    for (AdminReport report : reports) {
                        Item item = convertToItem(report);
                        if (shouldInclude(item, universityId, authId)) accumulatedItems.add(item);
                    }
                }
                finalizeAndDisplay(accumulatedItems);
            }

            @Override
            public void onFailure(String errorMessage) {
                finalizeAndDisplay(accumulatedItems);
            }
        });
    }

    private void finalizeAndDisplay(List<Item> accumulatedItems) {
        accumulatedItems.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
        
        adapter.updateItems(new ArrayList<>(accumulatedItems));
        itemList.clear();
        itemList.addAll(accumulatedItems);
        
        isFetching = false;
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
    }

    private Item convertToItem(AdminReport report) {
        Item item = new Item();
        item.setId(report.getReportId());
        item.setDisplayId(report.getDisplayId());
        item.setName(report.getTitle());
        item.setDescription(report.getDescription());
        item.setCategory(report.getCategory());
        item.setUserId(report.getUniversityId()); // sets reporterId
        item.setAuthUserId(report.getReporterAuthId());
        item.setStatus("admin_report");
        item.setAdminStatus(report.getStatus());
        item.setTimestamp(report.getTimestamp());
        item.setImageUrl(report.getImageUrl());
        item.setImageUrls(report.getImageUrls());
        item.setUserName(report.getReporterName());
        item.setUserPhone(report.getPhone());
        item.setLocation("Reported to Admin");
        item.setDeletedByUser(report.isDeletedByUser());
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        item.setDate(sdf.format(new Date(report.getTimestamp())));

        return item;
    }

    private boolean shouldInclude(Item item, String userId, String authId) {
        if (item.isDeletedByUser()) return false;

        // User Reports ONLY appear in User Reports section
        if ("admin_report".equals(item.getStatus())) {
            return "admin_reports".equals(filterType);
        }

        boolean isResolved = "Claimed".equalsIgnoreCase(item.getAdminStatus()) || 
                            "Returned".equalsIgnoreCase(item.getAdminStatus());

        boolean isOwner = (userId != null && userId.equalsIgnoreCase(item.getUserId())) ||
                         (authId != null && authId.equalsIgnoreCase(item.getAuthUserId()));

        switch (filterType) {
            case "reported":
                return "found".equals(item.getStatus()) && isOwner;
            case "find":
                return "lost".equals(item.getStatus()) && isOwner;
            case "resolved":
                boolean isClaimer = (userId != null && userId.equalsIgnoreCase(item.getClaimedByUserId())) ||
                                   (authId != null && authId.equalsIgnoreCase(item.getClaimedByUserId()));
                return isResolved && (isOwner || isClaimer);
            default:
                return false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();
    }

    private class MyItemsAdapter extends RecyclerView.Adapter<MyItemsAdapter.ViewHolder> {
        private List<Item> items;
        private Map<Integer, Runnable> sliderRunnables = new HashMap<>();
        private Handler sliderHandler = new Handler(Looper.getMainLooper());

        public MyItemsAdapter(List<Item> items) {
            this.items = items;
        }

        public void updateItems(List<Item> newItems) {
            // Take a copy of current items as the old list for DiffUtil
            List<Item> oldItems = new ArrayList<>(this.items);
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ItemDiffCallback(oldItems, newItems));
            this.items.clear();
            this.items.addAll(newItems);
            diffResult.dispatchUpdatesTo(this);
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
                String dId = item.getDisplayId();
                if (dId == null || dId.isEmpty()) {
                    dId = item.getReportId();
                }

                String formattedId = ReportIdFormatter.format(dId);
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

            boolean isResolved = "Claimed".equalsIgnoreCase(item.getAdminStatus()) || 
                                "Returned".equalsIgnoreCase(item.getAdminStatus()) ||
                                "Reviewed".equalsIgnoreCase(item.getAdminStatus());

            if (isResolved && "resolved".equals(filterType)) {
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_resolved_bg));
                holder.tvBadge.setText(R.string.status_resolved);
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_resolved_bg));
            } else if ("admin_report".equals(item.getStatus())) {
                String status = item.getAdminStatus() != null ? item.getAdminStatus() : "Pending";
                int statusColor;
                if ("Pending".equalsIgnoreCase(status)) {
                    statusColor = 0xFFFF9800; // Orange
                } else if ("Reviewed".equalsIgnoreCase(status)) {
                    statusColor = 0xFF2AABEE; // Blue
                } else {
                    statusColor = 0xFF757575; // Gray fallback
                }
                
                holder.statusIndicator.setBackgroundColor(statusColor);
                holder.tvBadge.setText(status.toUpperCase());
                holder.cardBadge.setCardBackgroundColor(statusColor);
            } else if (isResolved) {
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_resolved_bg));
                holder.tvBadge.setText(R.string.status_resolved);
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_resolved_bg));
            } else if ("lost".equals(item.getStatus())) {
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_lost_bg));
                holder.tvBadge.setText(R.string.status_lost_label);
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_lost_bg));
            } else {
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_found_bg));
                holder.tvBadge.setText(R.string.status_found_label);
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_found_bg));
            }
            holder.tvBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

            setupImageOrSlider(holder, item, position);

            holder.itemView.setOnClickListener(v -> {
                if ("admin_report".equals(item.getStatus())) {
                    Intent intent = new Intent(v.getContext(), AdminReportDetailsActivity.class);
                    intent.putExtra("reportId", item.getId());
                    v.getContext().startActivity(intent);
                    return;
                }
                // Centralized navigation for report cards
                ItemNavigationUtils.navigateToDetail(v.getContext(), item);
            });
        }

        @SuppressLint("ClickableViewAccessibility")
        private void setupImageOrSlider(ViewHolder holder, Item item, int position) {
            List<String> urls = item.getImageUrls();
            if (urls != null && urls.size() > 1 && holder.viewPagerSlider != null) {
                holder.ivIcon.setVisibility(View.GONE);
                holder.viewPagerSlider.setVisibility(View.VISIBLE);
                holder.tabLayoutIndicator.setVisibility(View.VISIBLE);

                // Use fitCenter (true) for multiple images to prevent zooming in cards
                ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(urls, true);
                // Slider click leads to detail view, not full screen
                sliderAdapter.setOnImageClickListener(pos -> {
                    if ("admin_report".equals(item.getStatus())) {
                        Intent intent = new Intent(holder.itemView.getContext(), AdminReportDetailsActivity.class);
                        intent.putExtra("reportId", item.getId());
                        holder.itemView.getContext().startActivity(intent);
                    } else {
                        ItemNavigationUtils.navigateToDetail(holder.itemView.getContext(), item);
                    }
                });

                holder.viewPagerSlider.setAdapter(sliderAdapter);
                holder.viewPagerSlider.setUserInputEnabled(true);

                new TabLayoutMediator(holder.tabLayoutIndicator, holder.viewPagerSlider, (tab, pos) -> {}).attach();

                startSlider(holder, urls, position);
            } else {
                if (holder.viewPagerSlider != null) holder.viewPagerSlider.setVisibility(View.GONE);
                if (holder.tabLayoutIndicator != null) holder.tabLayoutIndicator.setVisibility(View.GONE);
                holder.ivIcon.setVisibility(View.VISIBLE);
                stopSlider(position);

                if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                    GlideApp.with(holder.itemView.getContext())
                            .load(item.getImageUrl())
                            .placeholder(R.drawable.ic_package)
                            .thumbnail(0.1f)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(holder.ivIcon);
                            
                    // Image click leads to detail view, not full screen
                    holder.ivIcon.setOnClickListener(v -> {
                        if ("admin_report".equals(item.getStatus())) {
                            Intent intent = new Intent(v.getContext(), AdminReportDetailsActivity.class);
                            intent.putExtra("reportId", item.getId());
                            v.getContext().startActivity(intent);
                        } else {
                            ItemNavigationUtils.navigateToDetail(v.getContext(), item);
                        }
                    });
                } else {
                    holder.ivIcon.setImageResource(R.drawable.ic_package);
                    // Still navigate to details on placeholder click
                    holder.ivIcon.setOnClickListener(v -> {
                        if ("admin_report".equals(item.getStatus())) {
                            Intent intent = new Intent(v.getContext(), AdminReportDetailsActivity.class);
                            intent.putExtra("reportId", item.getId());
                            v.getContext().startActivity(intent);
                        } else {
                            ItemNavigationUtils.navigateToDetail(v.getContext(), item);
                        }
                    });
                }
            }
        }

        private void startSlider(ViewHolder holder, List<String> urls, int position) {
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

                // Move listeners to ViewHolder to avoid redundant object creation in onBind
                if (viewPagerSlider != null) {
                    viewPagerSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                        @Override
                        public void onPageScrollStateChanged(int state) {
                            super.onPageScrollStateChanged(state);
                            if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                                // Use bindingAdapterPosition to stop the correct slider
                                int pos = getBindingAdapterPosition();
                                if (pos != RecyclerView.NO_POSITION) {
                                    stopSlider(pos);
                                }
                            }
                        }
                    });

                    // The first child of ViewPager2 is the RecyclerView that handles scrolling
                    viewPagerSlider.post(() -> {
                        if (viewPagerSlider.getChildCount() > 0) {
                            viewPagerSlider.getChildAt(0).setOnTouchListener((v, event) -> {
                                int pos = getBindingAdapterPosition();
                                if (pos == RecyclerView.NO_POSITION) return false;
                                
                                if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                                    stopSlider(pos);
                                }
                                return false;
                            });
                        }
                    });
                }
            }
        }
    }

    private static class ItemDiffCallback extends DiffUtil.Callback {
        private final List<Item> oldList;
        private final List<Item> newList;

        public ItemDiffCallback(List<Item> oldList, List<Item> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Item oldItem = oldList.get(oldItemPosition);
            Item newItem = newList.get(newItemPosition);
            
            // Handle potential nulls in admin status or name
            String oldStatus = oldItem.getAdminStatus() != null ? oldItem.getAdminStatus() : "";
            String newStatus = newItem.getAdminStatus() != null ? newItem.getAdminStatus() : "";
            String oldName = oldItem.getName() != null ? oldItem.getName() : "";
            String newName = newItem.getName() != null ? newItem.getName() : "";

            return oldItem.getTimestamp() == newItem.getTimestamp() &&
                   oldStatus.equals(newStatus) &&
                   oldName.equals(newName);
        }
    }
}
