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
import android.widget.FrameLayout;
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

    private static final String TAG = "CampusDashboard";
    private RecyclerView rvRecentItems;
    private RecentItemsAdapter adapter;
    private List<Item> fullItemList;
    private List<Item> displayedItemList;
    private TextView tvWelcome, tvNotificationBadge, tvBrowseAll, tvLostCount, tvFoundCount, tvLostLabel, tvFoundLabel;
    private FrameLayout layoutWelcomeBg;
    private ImageView ivWelcomeWatermark;

    private String lastWelcomeKey = null;
    private android.os.Handler welcomeLoopHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable welcomeLoopRunnable = null;
    private android.os.Handler typingHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable typingRunnable = null;
    private View btnReportLost, btnReportFound, btnReportProblem, btnMenu, btnNotifications, tvDeveloperInfo,
            btnViewMore, btnViewLess;
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
    private int currentSelectedDrawerItemId = -1;

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
                if (drawerLayout != null)
                    drawerLayout.openDrawer(GravityCompat.START);
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

        // Reset TabLayout selection to Home tab (index 0)
        if (tabLayout != null && tabLayout.getTabAt(0) != null) {
            tabLayout.getTabAt(0).select();
        }
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
                    currentSelectedDrawerItemId = selectedId;
                    navigationView.setCheckedItem(selectedId);
                    customizeNavigationViewIcons();
                }
            }
        }
    }

    private void initializeViews() {
        rvRecentItems = findViewById(R.id.rvRecentItems);
        tvWelcome = findViewById(R.id.tvWelcome);
        layoutWelcomeBg = findViewById(R.id.layoutWelcomeBg);
        ivWelcomeWatermark = findViewById(R.id.ivWelcomeWatermark);
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
                    if (pendingIntent == null) {
                        currentSelectedDrawerItemId = -1;
                    }
                    resetNavigationSelection();
                    if (pendingIntent != null) {
                        final Intent intentToLaunch = pendingIntent;
                        pendingIntent = null;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            startActivity(intentToLaunch);
                            overridePendingTransition(R.anim.material_shared_axis_z_enter,
                                    R.anim.material_shared_axis_z_exit);
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
                        android.view.MenuItem subItem = subMenu.getItem(j);
                        subItem.setChecked(subItem.getItemId() == currentSelectedDrawerItemId);
                    }
                } else {
                    item.setChecked(item.getItemId() == currentSelectedDrawerItemId);
                }
            }
            customizeNavigationViewIcons();
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
                if (!ItemNavigationUtils.canNavigate())
                    return false;
                int id = item.getItemId();
                currentSelectedDrawerItemId = id;
                customizeNavigationViewIcons();
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
                    SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Logged out successfully");
                    Intent intent = new Intent(this, UserLoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return true;
                }
                if (drawerLayout != null)
                    drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            });
            customizeNavigationViewIcons();
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

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    onTabSelected(tab);
                }
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

            // Load cached items for instant rendering
            android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
            String cachedItemsJson = prefs.getString("cachedRecentItemsJson", "");
            if (!cachedItemsJson.isEmpty()) {
                try {
                    List<Item> cachedItems = new com.google.gson.Gson().fromJson(cachedItemsJson,
                            new com.google.gson.reflect.TypeToken<List<Item>>() {
                            }.getType());
                    if (cachedItems != null && !cachedItems.isEmpty()) {
                        fullItemList.addAll(cachedItems);
                        updateDisplayedList();
                    }
                } catch (Exception e) {
                    Log.e("Dashboard", "Error parsing cached items: " + e.getMessage());
                }
            }
        }
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(() -> {
                fetchUserData();
                fetchRecentItems();
            });
        }
    }

    private void fetchUserData() {
        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUniversityId = prefs.getString("universityId", null);
        String authId = prefs.getString("authId", null);

        // Load cached values for instant UI updates
        String cachedName = prefs.getString("cachedUserName", "");
        String cachedProfileUrl = prefs.getString("cachedProfileImageUrl", "");
        String cachedLostCount = prefs.getString("cachedLostCount", "0");
        String cachedFoundCount = prefs.getString("cachedFoundCount", "0");
        String cachedUserType = prefs.getString("userType", "Student");

        if (!cachedName.isEmpty()) {
            updateWelcomeMessage(cachedName, cachedUserType);
            if (tvNavHeaderName != null)
                tvNavHeaderName.setText(cachedName);
        }
        if (!cachedProfileUrl.isEmpty()) {
            loadNavHeaderProfileImageRectangular(cachedProfileUrl);
            setupProfileImageFullScreenViewer(cachedProfileUrl);
        }
        tvLostCount.setText(cachedLostCount);
        tvLostLabel.setText("1".equals(cachedLostCount) ? "Lost Report" : "Lost Reports");
        tvFoundCount.setText(cachedFoundCount);
        tvFoundLabel.setText("1".equals(cachedFoundCount) ? "Found Report" : "Found Reports");

        if (authId != null && currentUniversityId != null) {
            fetchUserStats(authId);
        }

        if (currentUniversityId != null) {
            SupabaseDatabaseHelper.select("profiles", "university_id=eq." + currentUniversityId + "&limit=1",
                    new TypeToken<List<User>>() {
                    }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
                        @Override
                        public void onSuccess(List<User> users) {
                            if (users != null && !users.isEmpty()) {
                                User user = users.get(0);
                                if (user != null) {
                                    String name = user.getName();
                                    if (name == null)
                                        name = user.getFullName();

                                    updateWelcomeMessage(name, user.getUserType());
                                    if (tvNavHeaderName != null)
                                        tvNavHeaderName.setText(name);
                                    loadNavHeaderProfileImageRectangular(user.getProfileImageUrl());
                                    setupProfileImageFullScreenViewer(user.getProfileImageUrl());

                                    // Save profile attributes to SharedPreferences cache
                                    prefs.edit()
                                            .putString("cachedUserName", name)
                                            .putString("cachedProfileImageUrl", user.getProfileImageUrl())
                                            .putString("cachedUserEmail", user.getEmail())
                                            .putString("cachedUserPhone", user.getPhone())
                                            .putString("cachedUserGender", user.getGender())
                                            .putString("cachedUserDepartment", user.getDepartment())
                                            .putString("cachedUserBatch", user.getBatch())
                                            .putString("cachedUserLevelTerm", user.getLevelTerm())
                                            .putString("cachedUserSection", user.getSection())
                                            .putString("cachedUserDesignation", user.getDesignation())
                                            .putString("userType", user.getUserType())
                                            .apply();

                                    listenForNotifications();
                                }
                            }
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                        }
                    });
        }
    }

    private void loadNavHeaderProfileImageRectangular(String imageUrl) {
        if (ivNavHeaderProfile == null)
            return;
        if (imageUrl != null && !imageUrl.isEmpty()) {
            GlideApp.with(this).load(imageUrl).placeholder(R.drawable.ic_user).diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop().into(ivNavHeaderProfile);
        } else {
            ivNavHeaderProfile.setImageResource(R.drawable.ic_user);
            ivNavHeaderProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }

    private void setupProfileImageFullScreenViewer(String imageUrl) {
        if (ivNavHeaderProfile == null || imageUrl == null || imageUrl.isEmpty())
            return;
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
        if (authId == null || currentUniversityId == null)
            return;
        String q = "deleted_by_user=eq.false&or=(user_id.eq." + authId + ",reporter_id.eq." + currentUniversityId + ")";
        SupabaseDatabaseHelper.select("lost_reports", q + "&select=count", new TypeToken<List<Map<String, Object>>>() {
        }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> res) {
                if (res != null && !res.isEmpty() && res.get(0).get("count") != null) {
                    Object countObj = res.get(0).get("count");
                    long count = (countObj instanceof Number) ? ((Number) countObj).longValue() : 0;
                    tvLostCount.setText(String.valueOf(count));
                    tvLostLabel.setText(count == 1 ? "Lost Report" : "Lost Reports");

                    // Update cache
                    getSharedPreferences("MyApp", MODE_PRIVATE).edit()
                            .putString("cachedLostCount", String.valueOf(count)).apply();
                }
            }

            @Override
            public void onFailure(String e) {
            }
        });
        SupabaseDatabaseHelper.select("found_reports", q + "&select=count", new TypeToken<List<Map<String, Object>>>() {
        }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> res) {
                if (res != null && !res.isEmpty() && res.get(0).get("count") != null) {
                    Object countObj = res.get(0).get("count");
                    long count = (countObj instanceof Number) ? ((Number) countObj).longValue() : 0;
                    tvFoundCount.setText(String.valueOf(count));
                    tvFoundLabel.setText(count == 1 ? "Found Report" : "Found Reports");

                    // Update cache
                    getSharedPreferences("MyApp", MODE_PRIVATE).edit()
                            .putString("cachedFoundCount", String.valueOf(count)).apply();
                }
            }

            @Override
            public void onFailure(String e) {
            }
        });
    }

    private void listenForNotifications() {
        if (currentUniversityId == null)
            return;
        // Filter for user types only
        String filter = "recipient_id=eq." + currentUniversityId
                + "&is_read=eq.false&type=in.(lost_item,found_item,item_claimed,item_return,admin_report)&select=count";
        SupabaseDatabaseHelper.select("notifications", filter, new TypeToken<List<Map<String, Object>>>() {
        }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> res) {
                if (res != null && !res.isEmpty() && res.get(0).get("count") != null) {
                    long count = ((Number) res.get(0).get("count")).longValue();
                    tvNotificationBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                    tvNotificationBadge.setText(String.valueOf(count));
                }
            }

            @Override
            public void onFailure(String e) {
            }
        });
    }

    private void fetchRecentItems() {
        if (isFetchingItems)
            return;
        isFetchingItems = true;

        Log.d("Dashboard", "Fetching recent lost reports...");
        SupabaseDatabaseHelper.select("lost_reports", "deleted_by_user=eq.false&order=timestamp.desc&limit=10",
                new TypeToken<List<Item>>() {
                }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
                    @Override
                    public void onSuccess(List<Item> lostItems) {
                        Log.d("Dashboard", "Lost reports fetched: " + (lostItems != null ? lostItems.size() : 0));
                        SupabaseDatabaseHelper.select("found_reports",
                                "deleted_by_user=eq.false&order=timestamp.desc&limit=10", new TypeToken<List<Item>>() {
                                }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
                                    @Override
                                    public void onSuccess(List<Item> foundItems) {
                                        Log.d("Dashboard", "Found reports fetched: "
                                                + (foundItems != null ? foundItems.size() : 0));
                                        List<Item> combined = new ArrayList<>();
                                        if (lostItems != null)
                                            combined.addAll(lostItems);
                                        if (foundItems != null)
                                            combined.addAll(foundItems);

                                        // Robust sorting by timestamp
                                        combined.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

                                        // Save to cache
                                        try {
                                            String json = new com.google.gson.Gson().toJson(combined);
                                            getSharedPreferences("MyApp", MODE_PRIVATE).edit()
                                                    .putString("cachedRecentItemsJson", json).apply();
                                        } catch (Exception e) {
                                            Log.e("Dashboard", "Error saving cached items: " + e.getMessage());
                                        }

                                        fullItemList.clear();
                                        fullItemList.addAll(combined);

                                        updateDisplayedList();
                                        isFetchingItems = false;
                                        if (swipeRefreshLayout != null)
                                            swipeRefreshLayout.setRefreshing(false);
                                    }

                                    @Override
                                    public void onFailure(String e) {
                                        Log.e("Dashboard", "Failed to fetch found reports: " + e);
                                        isFetchingItems = false;
                                        if (swipeRefreshLayout != null)
                                            swipeRefreshLayout.setRefreshing(false);
                                    }
                                });
                    }

                    @Override
                    public void onFailure(String e) {
                        Log.e("Dashboard", "Failed to fetch lost reports: " + e);
                        isFetchingItems = false;
                        if (swipeRefreshLayout != null)
                            swipeRefreshLayout.setRefreshing(false);
                    }
                });
    }

    private void updateDisplayedList() {
        List<Item> newDisplayedList = new ArrayList<>();
        int limit = Math.min(currentLimit, fullItemList.size());
        for (int i = 0; i < limit; i++) {
            newDisplayedList.add(fullItemList.get(i));
        }

        // Use DiffUtil to compute exact changes
        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil
                .calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return displayedItemList.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return newDisplayedList.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        Item oldItem = displayedItemList.get(oldItemPosition);
                        Item newItem = newDisplayedList.get(newItemPosition);
                        return oldItem.getId() != null && newItem.getId() != null
                                && oldItem.getId().equals(newItem.getId());
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        Item oldItem = displayedItemList.get(oldItemPosition);
                        Item newItem = newDisplayedList.get(newItemPosition);
                        return java.util.Objects.equals(oldItem.getName(), newItem.getName()) &&
                                java.util.Objects.equals(oldItem.getLocation(), newItem.getLocation()) &&
                                java.util.Objects.equals(oldItem.getDate(), newItem.getDate()) &&
                                java.util.Objects.equals(oldItem.getAdminStatus(), newItem.getAdminStatus()) &&
                                java.util.Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                                java.util.Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl()) &&
                                java.util.Objects.equals(oldItem.getImageUrls(), newItem.getImageUrls());
                    }
                });

        displayedItemList.clear();
        displayedItemList.addAll(newDisplayedList);
        diffResult.dispatchUpdatesTo(adapter);

        btnViewMore.setVisibility(fullItemList.size() > currentLimit ? View.VISIBLE : View.GONE);
        btnViewLess.setVisibility(currentLimit > 5 ? View.VISIBLE : View.GONE);
    }

    private void updateWelcomeMessage(String name, String userType) {
        if (name == null || name.trim().isEmpty()) {
            name = "User";
        }
        if (userType == null || userType.trim().isEmpty()) {
            userType = "Student";
        }

        // Apply role-based styling (background gradient & watermark icon)
        if (layoutWelcomeBg != null && ivWelcomeWatermark != null) {
            if ("Staff".equalsIgnoreCase(userType)) {
                layoutWelcomeBg.setBackgroundResource(R.drawable.staff_gradient_bg);
                ivWelcomeWatermark.setImageResource(R.drawable.ic_id_card);
            } else if ("Admin".equalsIgnoreCase(userType)) {
                layoutWelcomeBg.setBackgroundResource(R.drawable.admin_user_view_gradient_bg);
                ivWelcomeWatermark.setImageResource(R.drawable.ic_shield);
            } else {
                // Default: Student
                layoutWelcomeBg.setBackgroundResource(R.drawable.user_gradient_bg);
                ivWelcomeWatermark.setImageResource(R.drawable.ic_graduation_cap);
            }
        }

        // Apply dynamic button styling (background & text/icon colors) based on role
        int themeColor;
        if ("Staff".equalsIgnoreCase(userType)) {
            themeColor = ContextCompat.getColor(this, R.color.statusFound);
        } else if ("Admin".equalsIgnoreCase(userType)) {
            themeColor = ContextCompat.getColor(this, R.color.admin_accent);
        } else {
            themeColor = ContextCompat.getColor(this, R.color.primaryColor);
        }

        // Generate dynamic ColorStateLists for background and text/icon colors to
        // support active/pressed states with excellent contrast
        int pressedBgColor = (themeColor & 0x00FFFFFF) | 0x40000000; // 25% alpha
        int normalBgColor = (themeColor & 0x00FFFFFF) | 0x1A000000; // 10% alpha
        int disabledBgColor = android.graphics.Color.parseColor("#1A808080"); // 10% alpha gray
        int disabledTextColor = ContextCompat.getColor(this, R.color.textSecondary);

        android.content.res.ColorStateList bgStatesList = new android.content.res.ColorStateList(
                new int[][] {
                        new int[] { android.R.attr.state_pressed },
                        new int[] { -android.R.attr.state_enabled },
                        new int[] {}
                },
                new int[] {
                        pressedBgColor,
                        disabledBgColor,
                        normalBgColor
                });

        android.content.res.ColorStateList textStatesList = new android.content.res.ColorStateList(
                new int[][] {
                        new int[] { -android.R.attr.state_enabled },
                        new int[] {}
                },
                new int[] {
                        disabledTextColor,
                        themeColor
                });

        if (btnViewMore instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton mBtn = (com.google.android.material.button.MaterialButton) btnViewMore;
            mBtn.setBackgroundTintList(bgStatesList);
            mBtn.setTextColor(textStatesList);
            mBtn.setIconTint(textStatesList);
        }
        if (btnViewLess instanceof com.google.android.material.button.MaterialButton) {
            com.google.android.material.button.MaterialButton mBtn = (com.google.android.material.button.MaterialButton) btnViewLess;
            mBtn.setBackgroundTintList(bgStatesList);
            mBtn.setTextColor(textStatesList);
            mBtn.setIconTint(textStatesList);
        }

        String key = name + "_" + userType;
        if (key.equals(lastWelcomeKey)) {
            // Already running welcome loop for this name and type
            return;
        }
        lastWelcomeKey = key;

        // Stop any running animations and handlers
        if (tvWelcome != null) {
            tvWelcome.animate().cancel();
        }
        welcomeLoopHandler.removeCallbacksAndMessages(null);
        typingHandler.removeCallbacksAndMessages(null);

        String welcomeText = "Welcome back, " + name + "!";
        startWelcomeLoop(tvWelcome, welcomeText);
    }

    private void startWelcomeLoop(final TextView textView, final String welcomeText) {
        if (textView == null || isFinishing() || isDestroyed()) {
            return;
        }

        // Reset positions
        textView.setText("");
        textView.setAlpha(0f);
        textView.setTranslationY(20f);

        // Slide up + Fade in
        textView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> {
                    // When entrance completes, start typing
                    animateTextTyping(textView, welcomeText, () -> {
                        // When typing finishes, wait 4 seconds, then slide up + fade out
                        welcomeLoopRunnable = () -> {
                            if (isFinishing() || isDestroyed() || !textView.isAttachedToWindow()) {
                                return;
                            }
                            textView.animate()
                                    .alpha(0f)
                                    .translationY(-20f)
                                    .setDuration(800)
                                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                                    .withEndAction(() -> {
                                        // Restart the welcome loop
                                        startWelcomeLoop(textView, welcomeText);
                                    })
                                    .start();
                        };
                        welcomeLoopHandler.postDelayed(welcomeLoopRunnable, 4000);
                    });
                })
                .start();
    }

    private void animateTextTyping(final TextView textView, final String fullText, final Runnable onComplete) {
        textView.setText("");
        final int delay = 60; // ms per character
        typingRunnable = new Runnable() {
            private int index = 0;

            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (textView == null) {
                    return;
                }
                if (index > 0 && !textView.isAttachedToWindow()) {
                    return;
                }
                if (index <= fullText.length()) {
                    textView.setText(fullText.substring(0, index));
                    index++;
                    typingHandler.postDelayed(this, delay);
                } else if (onComplete != null) {
                    onComplete.run();
                }
            }
        };
        typingHandler.post(typingRunnable);
    }

    private class RecentItemsAdapter extends RecyclerView.Adapter<RecentItemsAdapter.ViewHolder> {
        private List<Item> items;
        private Map<Integer, Runnable> sliderRunnables = new HashMap<>();
        private Handler sliderHandler = new Handler(Looper.getMainLooper());

        public RecentItemsAdapter(List<Item> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_campus_reported_recent,
                    parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Item item = items.get(position);
            holder.tvTitle.setText(item.getName());
            holder.tvLocation.setText(ReportLocationDisplay.formatFullLocation(item.getLocation(),
                    item.getManualLocation(), item.getAdditionalLocationDetails()));
            holder.tvTime.setText(item.getDate());

            String displayId = item.getDisplayId();
            if (displayId == null || displayId.isEmpty()) {
                displayId = item.getReportId();
            }
            holder.tvReportId.setText(ReportIdFormatter.format(displayId));

            boolean res = "Claimed".equalsIgnoreCase(item.getAdminStatus())
                    || "Returned".equalsIgnoreCase(item.getAdminStatus());
            int color = ContextCompat.getColor(holder.itemView.getContext(), res ? R.color.badge_resolved_bg
                    : ("lost".equals(item.getStatus()) ? R.color.badge_lost_bg : R.color.badge_found_bg));
            holder.statusIndicator.setBackgroundColor(color);
            holder.tvBadge.setText(res ? "RESOLVED" : item.getStatus().toUpperCase());
            if (holder.cardBadge != null)
                holder.cardBadge.setCardBackgroundColor(color);
            setupImageOrSlider(holder, item, position);
            holder.itemView.setOnClickListener(v -> ItemNavigationUtils.navigateToDetail(v.getContext(), item));
        }

        private void setupImageOrSlider(ViewHolder h, Item item, int pos) {
            List<String> urls = item.getImageUrls();
            if (urls != null && urls.size() > 1 && h.viewPagerSlider != null) {
                h.ivIcon.setVisibility(View.GONE);
                h.viewPagerSlider.setVisibility(View.VISIBLE);
                if (h.tabLayoutIndicator != null)
                    h.tabLayoutIndicator.setVisibility(View.VISIBLE);
                ImageSliderAdapter sAdapter = new ImageSliderAdapter(urls, true);
                sAdapter.setOnImageClickListener(
                        p -> ItemNavigationUtils.navigateToDetail(h.itemView.getContext(), item));
                h.viewPagerSlider.setAdapter(sAdapter);
                if (h.tabLayoutIndicator != null)
                    new TabLayoutMediator(h.tabLayoutIndicator, h.viewPagerSlider, (t, p) -> {
                    }).attach();
                stopSlider(pos);
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if (h.viewPagerSlider != null) {
                            int c = h.viewPagerSlider.getCurrentItem();
                            h.viewPagerSlider.setCurrentItem((c + 1) % urls.size(), true);
                            sliderHandler.postDelayed(this, 3000);
                        }
                    }
                };
                sliderRunnables.put(pos, r);
                sliderHandler.postDelayed(r, 3000);
            } else {
                if (h.viewPagerSlider != null)
                    h.viewPagerSlider.setVisibility(View.GONE);
                if (h.tabLayoutIndicator != null)
                    h.tabLayoutIndicator.setVisibility(View.GONE);
                h.ivIcon.setVisibility(View.VISIBLE);
                stopSlider(pos);
                String url = (urls != null && !urls.isEmpty()) ? urls.get(0) : item.getImageUrl();
                if (url != null && !url.isEmpty()) {
                    h.ivIcon.setImageTintList(null);
                    h.ivIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    GlideApp.with(h.itemView.getContext()).load(url).placeholder(R.drawable.ic_package).thumbnail(0.1f)
                            .diskCacheStrategy(DiskCacheStrategy.ALL).centerCrop().into(h.ivIcon);
                    h.ivIcon.setOnClickListener(v -> ItemNavigationUtils.navigateToDetail(v.getContext(), item));
                } else {
                    h.ivIcon.setImageResource(R.drawable.ic_package);
                    h.ivIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    h.ivIcon.setImageTintList(android.content.res.ColorStateList
                            .valueOf(ContextCompat.getColor(h.itemView.getContext(), R.color.textSecondary)));
                    h.ivIcon.setOnClickListener(v -> ItemNavigationUtils.navigateToDetail(v.getContext(), item));
                }
            }
        }

        private void stopSlider(int pos) {
            Runnable r = sliderRunnables.get(pos);
            if (r != null) {
                sliderHandler.removeCallbacks(r);
                sliderRunnables.remove(pos);
            }
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder h) {
            super.onViewRecycled(h);
            stopSlider(h.getBindingAdapterPosition());
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

            public ViewHolder(@NonNull View v) {
                super(v);
                tvTitle = v.findViewById(R.id.tvItemTitle);
                tvLocation = v.findViewById(R.id.tvItemLocation);
                tvTime = v.findViewById(R.id.tvItemTime);
                ivIcon = v.findViewById(R.id.ivItemIcon);
                statusIndicator = v.findViewById(R.id.viewStatusIndicator);
                tvBadge = v.findViewById(R.id.tvBadge);
                cardBadge = v.findViewById(R.id.cardBadge);
                tvReportId = v.findViewById(R.id.tvReportId);
                viewPagerSlider = v.findViewById(R.id.viewPagerSlider);
                tabLayoutIndicator = v.findViewById(R.id.tabLayoutIndicator);
            }
        }
    }

    private void customizeNavigationViewIcons() {
        if (navigationView == null) return;
        navigationView.setItemIconTintList(null); // Clear layout override
        
        android.view.Menu menu = navigationView.getMenu();
        int uncheckedColor = ContextCompat.getColor(this, R.color.textSecondary);
        
        setItemIconTint(menu.findItem(R.id.nav_profile), ContextCompat.getColor(this, R.color.primaryColor), uncheckedColor);
        setItemIconTint(menu.findItem(R.id.nav_reported_items), ContextCompat.getColor(this, R.color.statusFound), uncheckedColor);
        setItemIconTint(menu.findItem(R.id.nav_find_items), ContextCompat.getColor(this, R.color.statusLost), uncheckedColor);
        setItemIconTint(menu.findItem(R.id.nav_resolved_items), ContextCompat.getColor(this, R.color.primaryColor), uncheckedColor);
        setItemIconTint(menu.findItem(R.id.nav_admin_reports), ContextCompat.getColor(this, R.color.admin_accent), uncheckedColor);
        setItemIconTint(menu.findItem(R.id.nav_admin_dashboard), ContextCompat.getColor(this, R.color.admin_accent), uncheckedColor);
        setItemIconTint(menu.findItem(R.id.nav_logout), ContextCompat.getColor(this, R.color.errorColor), uncheckedColor);
    }

    private void setItemIconTint(android.view.MenuItem item, int checkedColor, int uncheckedColor) {
        if (item == null) return;
        int[][] states = new int[][] {
            new int[] { android.R.attr.state_checked },
            new int[] { android.R.attr.state_selected },
            new int[] {}
        };
        int[] colors = new int[] {
            checkedColor,
            checkedColor,
            uncheckedColor
        };
        item.setIconTintList(new android.content.res.ColorStateList(states, colors));
    }
}
