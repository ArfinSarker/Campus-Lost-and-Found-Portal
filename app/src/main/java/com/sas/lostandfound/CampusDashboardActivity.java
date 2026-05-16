package com.sas.lostandfound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CampusDashboardActivity extends AppCompatActivity {

    private RecyclerView rvRecentItems;
    private RecentItemsAdapter adapter;
    private List<Item> fullItemList;
    private List<Item> displayedItemList;
    private TextView tvWelcome, tvNotificationBadge, tvBrowseAll, tvLostCount, tvFoundCount, tvLostLabel, tvFoundLabel;
    private View btnReportLost, btnReportFound, btnReportProblem, btnMenu, btnNotifications, tvDeveloperInfo, btnViewMore, btnViewLess;
    private TabLayout tabLayout;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView ivNavHeaderProfile;
    private TextView tvNavHeaderName;
    private View cardLostReports, cardFoundReports;
    private SwipeRefreshLayout swipeRefreshLayout;
    private com.google.android.material.appbar.AppBarLayout appBarLayout;

    private int currentLimit = 5;
    private String currentUniversityId;
    private boolean isFetchingItems = false;

    private Handler badgeHandler = new Handler(Looper.getMainLooper());
    private Runnable badgeRunnable = new Runnable() {
        @Override
        public void run() {
            listenForNotifications();
            badgeHandler.postDelayed(this, 10000); // Refresh every 10 seconds
        }
    };

    private Intent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RoleVerifier.checkUserAccess(this);
        checkSessionAndRedirect();

        setContentView(R.layout.activity_campus_dashboard);

        initializeViews();
        setupRecyclerView();
        setupTabLayout();
        setupNavigationView();
        setupNavigationDrawerBehavior();
        setupSwipeRefresh();

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                if (ItemNavigationUtils.canNavigate()) {
                    startActivity(new Intent(this, NotificationsActivity.class));
                }
            });
        }

        if (btnReportLost != null) {
            btnReportLost.setOnClickListener(v -> {
                if (ItemNavigationUtils.canNavigate()) {
                    startActivity(new Intent(this, CampusReportLostActivity.class));
                }
            });
        }
        
        if (btnReportFound != null) {
            btnReportFound.setOnClickListener(v -> {
                if (ItemNavigationUtils.canNavigate()) {
                    startActivity(new Intent(this, CampusReportFoundActivity.class));
                }
            });
        }

        if (tvDeveloperInfo != null) {
            tvDeveloperInfo.setOnClickListener(v -> {
                if (ItemNavigationUtils.canNavigate()) {
                    Intent intent = new Intent(CampusDashboardActivity.this, DeveloperInfoActivity.class);
                    startActivity(intent);
                }
            });
        }

        if (btnReportProblem != null) {
            btnReportProblem.setOnClickListener(v -> {
                if (ItemNavigationUtils.canNavigate()) {
                    Intent intent = new Intent(CampusDashboardActivity.this, ReportToAdminActivity.class);
                    startActivity(intent);
                }
            });
        }

        if (tvBrowseAll != null) {
            tvBrowseAll.setOnClickListener(v -> {
                if (ItemNavigationUtils.canNavigate()) {
                    startActivity(new Intent(this, AllReportedItemsActivity.class));
                }
            });
        }

        if (btnViewMore != null) {
            btnViewMore.setOnClickListener(v -> {
                currentLimit += 5;
                updateDisplayedList();
            });
        }

        if (btnViewLess != null) {
            btnViewLess.setOnClickListener(v -> {
                if (currentLimit > 5) {
                    currentLimit -= 5;
                    updateDisplayedList();
                }
            });
        }

        applyNavigationStateFromIntent(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Admins should behave like normal users when on the main dashboard
        ModeManager.setMode(this, ModeManager.MODE_USER);
        
        // Refresh data whenever user returns to dashboard
        fetchUserData();
        fetchRecentItems();
        badgeHandler.post(badgeRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        badgeHandler.removeCallbacks(badgeRunnable);
    }

    private void checkSessionAndRedirect() {
        // Admins are now allowed to use the normal user dashboard.
        // They can access the Admin Dashboard via the navigation drawer.
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyNavigationStateFromIntent(intent);
    }

    private void applyNavigationStateFromIntent(Intent intent) {
        if (intent != null && drawerLayout != null) {
            if (intent.getBooleanExtra("openDrawer", false)) {
                drawerLayout.openDrawer(GravityCompat.START);
                int selectedId = intent.getIntExtra("selectedItemId", -1);
                if (selectedId != -1 && navigationView != null) {
                    navigationView.setCheckedItem(selectedId);
                }
            }
        }
    }

    private void initializeViews() {
        rvRecentItems = findViewById(R.id.rvRecentItems);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvDeveloperInfo = findViewById(R.id.tvDeveloperInfo);
        btnReportLost = findViewById(R.id.btnReportLost);
        btnReportFound = findViewById(R.id.btnReportFound);
        btnReportProblem = findViewById(R.id.btnReportProblem);
        btnMenu = findViewById(R.id.btnMenu);
        btnNotifications = findViewById(R.id.btnNotifications);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        tabLayout = findViewById(R.id.tabLayout);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        tvBrowseAll = findViewById(R.id.tvBrowseAll);
        btnViewMore = findViewById(R.id.btnViewMore);
        btnViewLess = findViewById(R.id.btnViewLess);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        appBarLayout = findViewById(R.id.appBarLayout);

        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout);
        }

        if (navigationView != null && navigationView.getHeaderCount() > 0) {
            View headerView = navigationView.getHeaderView(0);
            ivNavHeaderProfile = headerView.findViewById(R.id.nav_header_imageView);
            tvNavHeaderName = headerView.findViewById(R.id.nav_header_name);
        }

        View viewLost = findViewById(R.id.cardUserLostReports);
        if (viewLost != null) {
            tvLostCount = viewLost.findViewById(R.id.tvUserLostCount);
            tvLostLabel = viewLost.findViewById(R.id.tvUserLostLabel);
            cardLostReports = viewLost;
            cardLostReports.setOnClickListener(v -> {
                if (ItemNavigationUtils.canNavigate()) {
                    Intent intent = new Intent(this, CampusMyItemsActivity.class);
                    intent.putExtra("filterType", "find");
                    startActivity(intent);
                }
            });
        }

        View viewFound = findViewById(R.id.cardUserFoundReports);
        if (viewFound != null) {
            tvFoundCount = viewFound.findViewById(R.id.tvUserFoundCount);
            tvFoundLabel = viewFound.findViewById(R.id.tvUserFoundLabel);
            cardFoundReports = viewFound;
            cardFoundReports.setOnClickListener(v -> {
                if (ItemNavigationUtils.canNavigate()) {
                    Intent intent = new Intent(this, CampusMyItemsActivity.class);
                    intent.putExtra("filterType", "reported");
                    startActivity(intent);
                }
            });
        }
    }

    private void setupNavigationDrawerBehavior() {
        if (drawerLayout != null && navigationView != null) {
            drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerClosed(View drawerView) {
                    super.onDrawerClosed(drawerView);
                    resetNavigationSelection();
                    if (pendingIntent != null) {
                        final Intent intentToLaunch = pendingIntent;
                        pendingIntent = null;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            startActivity(intentToLaunch);
                            overridePendingTransition(R.anim.material_shared_axis_z_enter, R.anim.material_shared_axis_z_exit);
                        }, 200);
                    }
                }
            });
        }
    }

    private void resetNavigationSelection() {
        if (navigationView != null) {
            android.view.Menu menu = navigationView.getMenu();
            for (int i = 0; i < menu.size(); i++) {
                android.view.MenuItem item = menu.getItem(i);
                if (item.hasSubMenu()) {
                    android.view.Menu subMenu = item.getSubMenu();
                    for (int j = 0; j < subMenu.size(); j++) {
                        subMenu.getItem(j).setChecked(false);
                    }
                } else item.setChecked(false);
            }
        }
    }

    private void setupNavigationView() {
        if (navigationView != null) {
            SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
            boolean isAdmin = prefs.getBoolean("isAdminLoggedIn", false);
            Menu menu = navigationView.getMenu();
            MenuItem adminDashboardItem = menu.findItem(R.id.nav_admin_dashboard);
            if (adminDashboardItem != null) {
                adminDashboardItem.setVisible(isAdmin);
            }

            navigationView.setNavigationItemSelectedListener(item -> {
                if (!ItemNavigationUtils.canNavigate()) return false;
                int id = item.getItemId();
                if (id == R.id.nav_profile) {
                    pendingIntent = new Intent(this, UserProfileActivity.class);
                    pendingIntent.putExtra("fromDrawer", true);
                } else if (id == R.id.nav_reported_items) {
                    pendingIntent = new Intent(this, CampusMyItemsActivity.class);
                    pendingIntent.putExtra("filterType", "reported");
                    pendingIntent.putExtra("fromDrawer", true);
                } else if (id == R.id.nav_find_items) {
                    pendingIntent = new Intent(this, CampusMyItemsActivity.class);
                    pendingIntent.putExtra("filterType", "find");
                    pendingIntent.putExtra("fromDrawer", true);
                } else if (id == R.id.nav_resolved_items) {
                    pendingIntent = new Intent(this, CampusMyItemsActivity.class);
                    pendingIntent.putExtra("filterType", "resolved");
                    pendingIntent.putExtra("fromDrawer", true);
                } else if (id == R.id.nav_admin_reports) {
                    pendingIntent = new Intent(this, CampusMyItemsActivity.class);
                    pendingIntent.putExtra("filterType", "admin_reports");
                    pendingIntent.putExtra("fromDrawer", true);
                } else if (id == R.id.nav_admin_dashboard) {
                    pendingIntent = new Intent(this, AdminDashboardActivity.class);
                    pendingIntent.putExtra("fromDrawer", true);
                } else if (id == R.id.nav_logout) {
                    SupabaseAuthHelper.signOut();
                    getSharedPreferences("MyApp", MODE_PRIVATE).edit().clear().apply();
                    Intent intent = new Intent(this, UserLoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                }
                if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
        }
    }

    private void setupTabLayout() {
        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    String tabText = tab.getText() != null ? tab.getText().toString() : "";
                    if ("Search Items".equals(tabText) || "Search".equals(tabText)) {
                        startActivity(new Intent(CampusDashboardActivity.this, BrowseItemsActivity.class));
                    } else if ("Report".equals(tabText)) {
                        startActivity(new Intent(CampusDashboardActivity.this, ReportToAdminActivity.class));
                    }
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) {}
                @Override public void onTabReselected(TabLayout.Tab tab) { onTabSelected(tab); }
            });
        }
    }

    private void setupRecyclerView() {
        if (rvRecentItems != null) {
            fullItemList = new ArrayList<>();
            displayedItemList = new ArrayList<>();
            adapter = new RecentItemsAdapter(displayedItemList);
            rvRecentItems.setLayoutManager(new LinearLayoutManager(this));
            rvRecentItems.setNestedScrollingEnabled(false);
            rvRecentItems.setAdapter(adapter);
        }
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(() -> { fetchUserData(); fetchRecentItems(); });
        }
    }

    private void fetchUserData() {
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUniversityId = prefs.getString("universityId", null);
        String authId = prefs.getString("authId", null);

        if (authId != null && currentUniversityId != null) {
            fetchUserStats(authId);
        }

        if (currentUniversityId != null) {
            SupabaseDatabaseHelper.select("profiles", "university_id=eq." + currentUniversityId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
                @Override
                public void onSuccess(List<User> users) {
                    if (users != null && !users.isEmpty()) {
                        User user = users.get(0);
                        if (user != null) {
                            tvWelcome.setText("Welcome back, " + user.getName() + "!");
                            if (tvNavHeaderName != null) tvNavHeaderName.setText(user.getName());
                            loadNavHeaderProfileImageRectangular(user.getProfileImageUrl());
                            setupProfileImageFullScreenViewer(user.getProfileImageUrl());
                            fetchUserStats(user.getAuthId());
                            listenForNotifications();
                        }
                    }
                }
                @Override public void onFailure(String errorMessage) {}
            });
        }
    }

    private void loadNavHeaderProfileImageRectangular(String imageUrl) {
        if (ivNavHeaderProfile == null) return;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            GlideApp.with(this).load(imageUrl).placeholder(R.drawable.ic_user).diskCacheStrategy(DiskCacheStrategy.ALL).centerCrop().into(ivNavHeaderProfile);
        } else {
            ivNavHeaderProfile.setImageResource(R.drawable.ic_user);
            ivNavHeaderProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }

    private void setupProfileImageFullScreenViewer(String imageUrl) {
        if (ivNavHeaderProfile == null || imageUrl == null || imageUrl.isEmpty()) return;
        ivNavHeaderProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, FullScreenImageActivity.class);
            ArrayList<String> images = new ArrayList<>();
            images.add(imageUrl);
            intent.putStringArrayListExtra("imageUrls", images);
            intent.putExtra("position", 0);
            startActivity(intent);
        });
    }

    private void fetchUserStats(String authId) {
        if (authId == null || currentUniversityId == null) return;
        String q = "or=(user_id.eq." + authId + ",reporter_id.eq." + currentUniversityId + ")";
        SupabaseDatabaseHelper.select("lost_reports", q + "&select=count", new TypeToken<List<Map<String, Object>>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Map<String, Object>>>() {
            @Override public void onSuccess(List<Map<String, Object>> res) {
                if (res != null && !res.isEmpty() && res.get(0).get("count") != null) {
                    Object countObj = res.get(0).get("count");
                    long count = (countObj instanceof Number) ? ((Number) countObj).longValue() : 0;
                    tvLostCount.setText(String.valueOf(count));
                    tvLostLabel.setText(count == 1 ? "Lost Report" : "Lost Reports");
                }
            }
            @Override public void onFailure(String e) {}
        });
        SupabaseDatabaseHelper.select("found_reports", q + "&select=count", new TypeToken<List<Map<String, Object>>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Map<String, Object>>>() {
            @Override public void onSuccess(List<Map<String, Object>> res) {
                if (res != null && !res.isEmpty() && res.get(0).get("count") != null) {
                    Object countObj = res.get(0).get("count");
                    long count = (countObj instanceof Number) ? ((Number) countObj).longValue() : 0;
                    tvFoundCount.setText(String.valueOf(count));
                    tvFoundLabel.setText(count == 1 ? "Found Report" : "Found Reports");
                }
            }
            @Override public void onFailure(String e) {}
        });
    }

    private void listenForNotifications() {
        if (currentUniversityId == null) return;
        SupabaseDatabaseHelper.select("notifications", "recipient_id=eq." + currentUniversityId + "&is_read=eq.false&select=count", new TypeToken<List<Map<String, Object>>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Map<String, Object>>>() {
            @Override public void onSuccess(List<Map<String, Object>> res) {
                if (res != null && !res.isEmpty() && res.get(0).get("count") != null) {
                    long count = ((Number) res.get(0).get("count")).longValue();
                    tvNotificationBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                    tvNotificationBadge.setText(String.valueOf(count));
                }
            }
            @Override public void onFailure(String e) {}
        });
    }

    private void fetchRecentItems() {
        if (isFetchingItems) return;
        isFetchingItems = true;

        Log.d("Dashboard", "Fetching recent lost reports...");
        SupabaseDatabaseHelper.select("lost_reports", "deleted_by_user=eq.false&order=timestamp.desc&limit=10", new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override public void onSuccess(List<Item> lostItems) {
                Log.d("Dashboard", "Lost reports fetched: " + (lostItems != null ? lostItems.size() : 0));
                SupabaseDatabaseHelper.select("found_reports", "deleted_by_user=eq.false&order=timestamp.desc&limit=10", new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
                    @Override public void onSuccess(List<Item> foundItems) {
                        Log.d("Dashboard", "Found reports fetched: " + (foundItems != null ? foundItems.size() : 0));
                        List<Item> combined = new ArrayList<>();
                        if (lostItems != null) combined.addAll(lostItems);
                        if (foundItems != null) combined.addAll(foundItems);
                        
                        // Robust sorting by timestamp
                        combined.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                        
                        fullItemList.clear();
                        fullItemList.addAll(combined);
                        
                        updateDisplayedList();
                        isFetchingItems = false;
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    }
                    @Override public void onFailure(String e) {
                        Log.e("Dashboard", "Failed to fetch found reports: " + e);
                        isFetchingItems = false;
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
            @Override public void onFailure(String e) {
                Log.e("Dashboard", "Failed to fetch lost reports: " + e);
                isFetchingItems = false;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void updateDisplayedList() {
        displayedItemList.clear();
        int limit = Math.min(currentLimit, fullItemList.size());
        for (int i = 0; i < limit; i++) displayedItemList.add(fullItemList.get(i));
        adapter.notifyDataSetChanged();
        btnViewMore.setVisibility(fullItemList.size() > currentLimit ? View.VISIBLE : View.GONE);
        btnViewLess.setVisibility(currentLimit > 5 ? View.VISIBLE : View.GONE);
    }

    private class RecentItemsAdapter extends RecyclerView.Adapter<RecentItemsAdapter.ViewHolder> {
        private List<Item> items;
        private Map<Integer, Runnable> sliderRunnables = new HashMap<>();
        private Handler sliderHandler = new Handler(Looper.getMainLooper());
        public RecentItemsAdapter(List<Item> items) { this.items = items; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_campus_reported_recent, parent, false));
        }
        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Item item = items.get(position);
            holder.tvTitle.setText(item.getName());
            holder.tvLocation.setText(ReportLocationDisplay.formatFullLocation(item.getLocation(), item.getManualLocation(), item.getAdditionalLocationDetails()));
            holder.tvTime.setText(item.getDate());
            
            String displayId = item.getDisplayId();
            if (displayId == null || displayId.isEmpty()) {
                displayId = item.getReportId();
            }
            holder.tvReportId.setText(ReportIdFormatter.format(displayId));

            boolean res = "Claimed".equalsIgnoreCase(item.getAdminStatus()) || "Returned".equalsIgnoreCase(item.getAdminStatus());
            int color = ContextCompat.getColor(holder.itemView.getContext(), res ? R.color.badge_resolved_bg : ("lost".equals(item.getStatus()) ? R.color.badge_lost_bg : R.color.badge_found_bg));
            holder.statusIndicator.setBackgroundColor(color);
            holder.tvBadge.setText(res ? "RESOLVED" : item.getStatus().toUpperCase());
            if (holder.cardBadge != null) holder.cardBadge.setCardBackgroundColor(color);
            setupImageOrSlider(holder, item, position);
            holder.itemView.setOnClickListener(v -> ItemNavigationUtils.navigateToDetail(v.getContext(), item));
        }
        private void setupImageOrSlider(ViewHolder h, Item item, int pos) {
            List<String> urls = item.getImageUrls();
            if (urls != null && urls.size() > 1 && h.viewPagerSlider != null) {
                h.ivIcon.setVisibility(View.GONE); h.viewPagerSlider.setVisibility(View.VISIBLE);
                if (h.tabLayoutIndicator != null) h.tabLayoutIndicator.setVisibility(View.VISIBLE);
                ImageSliderAdapter sAdapter = new ImageSliderAdapter(urls, true);
                sAdapter.setOnImageClickListener(p -> ItemNavigationUtils.navigateToDetail(h.itemView.getContext(), item));
                h.viewPagerSlider.setAdapter(sAdapter);
                if (h.tabLayoutIndicator != null) new TabLayoutMediator(h.tabLayoutIndicator, h.viewPagerSlider, (t, p) -> {}).attach();
                stopSlider(pos);
                Runnable r = new Runnable() { @Override public void run() { if (h.viewPagerSlider != null) { int c = h.viewPagerSlider.getCurrentItem(); h.viewPagerSlider.setCurrentItem((c + 1) % urls.size(), true); sliderHandler.postDelayed(this, 3000); } } };
                sliderRunnables.put(pos, r); sliderHandler.postDelayed(r, 3000);
            } else {
                if (h.viewPagerSlider != null) h.viewPagerSlider.setVisibility(View.GONE);
                if (h.tabLayoutIndicator != null) h.tabLayoutIndicator.setVisibility(View.GONE);
                h.ivIcon.setVisibility(View.VISIBLE); stopSlider(pos);
                String url = (urls != null && !urls.isEmpty()) ? urls.get(0) : item.getImageUrl();
                if (url != null && !url.isEmpty()) {
                    h.ivIcon.setImageTintList(null); h.ivIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    GlideApp.with(h.itemView.getContext()).load(url).placeholder(R.drawable.ic_package).thumbnail(0.1f).diskCacheStrategy(DiskCacheStrategy.ALL).centerCrop().into(h.ivIcon);
                    h.ivIcon.setOnClickListener(v -> ItemNavigationUtils.navigateToDetail(v.getContext(), item));
                } else {
                    h.ivIcon.setImageResource(R.drawable.ic_package); h.ivIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    h.ivIcon.setImageTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(h.itemView.getContext(), R.color.textSecondary)));
                    h.ivIcon.setOnClickListener(v -> ItemNavigationUtils.navigateToDetail(v.getContext(), item));
                }
            }
        }
        private void stopSlider(int pos) { Runnable r = sliderRunnables.get(pos); if (r != null) { sliderHandler.removeCallbacks(r); sliderRunnables.remove(pos); } }
        @Override public void onViewRecycled(@NonNull ViewHolder h) { super.onViewRecycled(h); stopSlider(h.getBindingAdapterPosition()); }
        @Override public int getItemCount() { return items.size(); }
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvLocation, tvTime, tvBadge, tvReportId; ImageView ivIcon; View statusIndicator; MaterialCardView cardBadge; ViewPager2 viewPagerSlider; TabLayout tabLayoutIndicator;
            public ViewHolder(@NonNull View v) {
                super(v); tvTitle = v.findViewById(R.id.tvItemTitle); tvLocation = v.findViewById(R.id.tvItemLocation); tvTime = v.findViewById(R.id.tvItemTime); ivIcon = v.findViewById(R.id.ivItemIcon); statusIndicator = v.findViewById(R.id.viewStatusIndicator); tvBadge = v.findViewById(R.id.tvBadge); cardBadge = v.findViewById(R.id.cardBadge); tvReportId = v.findViewById(R.id.tvReportId); viewPagerSlider = v.findViewById(R.id.viewPagerSlider); tabLayoutIndicator = v.findViewById(R.id.tabLayoutIndicator);
            }
        }
    }
}
