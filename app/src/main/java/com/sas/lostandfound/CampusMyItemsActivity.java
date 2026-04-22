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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

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

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private ValueEventListener lostItemsListener, foundItemsListener, adminReportsListener;
    private Query lostItemsQuery, foundItemsQuery, adminReportsQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_my_items);

        filterType = getIntent().getStringExtra("filterType");
        if (filterType == null) filterType = "reported";

        fromDrawer = getIntent().getBooleanExtra("fromDrawer", false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).getReference();

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

        fetchMyItems();
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
        if (mAuth.getCurrentUser() == null) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }
        String authUid = mAuth.getCurrentUser().getUid();

        mDatabase.child("UIDToUniversityID").child(authUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final String resolvedUserId = snapshot.exists() ? snapshot.getValue(String.class) : authUid;
                loadItems(resolvedUserId, authUid);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadItems(authUid, authUid);
            }
        });
    }

    private void removeListeners() {
        if (lostItemsQuery != null && lostItemsListener != null) {
            lostItemsQuery.removeEventListener(lostItemsListener);
        }
        if (foundItemsQuery != null && foundItemsListener != null) {
            foundItemsQuery.removeEventListener(foundItemsListener);
        }
        if (adminReportsQuery != null && adminReportsListener != null) {
            adminReportsQuery.removeEventListener(adminReportsListener);
        }
    }

    private void loadItems(String universityId, String authUid) {
        removeListeners();
        itemList.clear(); 
        adapter.notifyDataSetChanged();
        
        ValueEventListener itemListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Item item = data.getValue(Item.class);
                    if (item != null) {
                        if (shouldInclude(item, universityId)) {
                            updateOrAddItem(item);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        };

        lostItemsListener = itemListener;
        foundItemsListener = itemListener;

        if (!"admin_reports".equals(filterType)) {
            lostItemsQuery = mDatabase.child("LostItems").orderByChild("userId").equalTo(universityId);
            foundItemsQuery = mDatabase.child("FoundItems").orderByChild("userId").equalTo(universityId);
            
            lostItemsQuery.addValueEventListener(lostItemsListener);
            foundItemsQuery.addValueEventListener(foundItemsListener);
            
            if ("resolved".equals(filterType)) {
                mDatabase.child("LostItems").orderByChild("claimedByUserId").equalTo(universityId).addValueEventListener(itemListener);
                mDatabase.child("FoundItems").orderByChild("claimedByUserId").equalTo(universityId).addValueEventListener(itemListener);
            }
        }

        adminReportsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    AdminReport report = data.getValue(AdminReport.class);
                    if (report != null) {
                        if (!report.isDeletedByUser()) {
                            Item item = convertToItem(report);
                            if (shouldInclude(item, universityId)) {
                                updateOrAddItem(item);
                            }
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching admin reports: " + error.getMessage());
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        };
        
        adminReportsQuery = mDatabase.child("AdminReports").orderByChild("reporterAuthId").equalTo(authUid);
        adminReportsQuery.addValueEventListener(adminReportsListener);
    }

    private Item convertToItem(AdminReport report) {
        Item item = new Item();
        item.setId(report.getReportId());
        item.setDisplayId(report.getDisplayId());
        item.setName(report.getTitle());
        item.setDescription(report.getDescription());
        item.setCategory(report.getCategory());
        item.setUserId(report.getReporterAuthId());
        item.setStatus("admin_report");
        item.setAdminStatus(report.getStatus());
        item.setTimestamp(report.getTimestamp());
        item.setImageUrl(report.getImageUrl());
        item.setImageUrls(report.getImageUrls());
        item.setUserName(report.getReporterName());
        item.setUserPhone(report.getPhone());
        item.setLocation("Reported to Admin");
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        item.setDate(sdf.format(new Date(report.getTimestamp())));

        return item;
    }

    private boolean shouldInclude(Item item, String userId) {
        boolean isResolved = "Claimed".equalsIgnoreCase(item.getAdminStatus()) || "Returned".equalsIgnoreCase(item.getAdminStatus());

        if ("admin_report".equals(item.getStatus())) {
            return "admin_reports".equals(filterType);
        }

        switch (filterType) {
            case "reported":
                return "found".equals(item.getStatus()) && userId.equals(item.getUserId());
            case "find":
                return "lost".equals(item.getStatus()) && userId.equals(item.getUserId());
            case "resolved":
                return isResolved && (userId.equals(item.getUserId()) || userId.equals(item.getClaimedByUserId()));
            default:
                return false;
        }
    }

    private synchronized void updateOrAddItem(Item item) {
        for (int i = 0; i < itemList.size(); i++) {
            if (itemList.get(i).getId().equals(item.getId())) {
                itemList.set(i, item);
                return;
            }
        }
        itemList.add(item);
        itemList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
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

                if (displayId != null && !displayId.isEmpty()) {
                    if (!displayId.startsWith("#")) {
                        displayId = "#" + displayId;
                    }
                    holder.tvReportId.setText(displayId);
                    View parent = (View) holder.tvReportId.getParent();
                    if (parent != null) parent.setVisibility(View.VISIBLE);
                } else {
                    holder.tvReportId.setText("");
                    View parent = (View) holder.tvReportId.getParent();
                    if (parent != null) parent.setVisibility(View.GONE);
                }
            }

            boolean isResolved = "Claimed".equalsIgnoreCase(item.getAdminStatus()) || "Returned".equalsIgnoreCase(item.getAdminStatus());

            if ("admin_report".equals(item.getStatus())) {
                String status = item.getAdminStatus();
                int statusColor;
                if ("Pending".equalsIgnoreCase(status)) statusColor = 0xFF757575; // Gray
                else if ("Reviewed".equalsIgnoreCase(status)) statusColor = 0xFF1976D2; // Blue
                else statusColor = 0xFF2E7D32; // Green
                
                holder.statusIndicator.setBackgroundColor(statusColor);
                holder.tvBadge.setText(status.toUpperCase());
                holder.cardBadge.setCardBackgroundColor(statusColor);
            } else if (isResolved) {
                holder.statusIndicator.setBackgroundColor(0xFF2E7D32); // Green
                holder.tvBadge.setText("RESOLVED");
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_found_bg));
            } else if ("lost".equals(item.getStatus())) {
                holder.statusIndicator.setBackgroundColor(0xFFA31621); // Red
                holder.tvBadge.setText("LOST");
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_lost_bg));
            } else {
                holder.statusIndicator.setBackgroundColor(0xFF2E7D32); // Green
                holder.tvBadge.setText("FOUND");
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

                ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(urls);
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

                holder.viewPagerSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                    @Override
                    public void onPageScrollStateChanged(int state) {
                        super.onPageScrollStateChanged(state);
                        if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                            stopSlider(position);
                        }
                    }
                });

                holder.viewPagerSlider.getChildAt(0).setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                        stopSlider(position);
                    } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                        startSlider(holder, urls, position);
                    }
                    return false;
                });

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
            }
        }
    }
}
