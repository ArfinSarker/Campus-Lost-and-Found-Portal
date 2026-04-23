package com.sas.lostandfound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

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

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private int currentLimit = 5;
    private String currentUniversityId;
    private ValueEventListener notificationListener;
    private ValueEventListener lostReportsListener, foundReportsListener;
    private ValueEventListener recentLostItemsListener, recentFoundItemsListener;
    private Query recentLostItemsQuery, recentFoundItemsQuery;

    // Hardcoded Admin UID
    private static final String ADMIN_UID = "1";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).getReference();

        // Ensure user is logged in
        RoleVerifier.checkUserAccess(this);

        // Check if admin is logged in and redirect if necessary
        checkSessionAndRedirect();

        setContentView(R.layout.activity_campus_dashboard);

        initializeViews();
        setupRecyclerView();
        setupTabLayout();
        setupNavigationView();
        setupNavigationDrawerBehavior();
        setupSwipeRefresh();
        fetchUserData();
        fetchRecentItems();

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
            });
        }

        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> {
                startActivity(new Intent(this, NotificationsActivity.class));
            });
        }

        if (btnReportLost != null) {
            btnReportLost.setOnClickListener(v -> startActivity(new Intent(this, CampusReportLostActivity.class)));
        }
        
        if (btnReportFound != null) {
            btnReportFound.setOnClickListener(v -> startActivity(new Intent(this, CampusReportFoundActivity.class)));
        }

        if (tvDeveloperInfo != null) {
            tvDeveloperInfo.setOnClickListener(v -> {
                Intent intent = new Intent(CampusDashboardActivity.this, DeveloperInfoActivity.class);
                startActivity(intent);
            });
        }

        if (btnReportProblem != null) {
            btnReportProblem.setOnClickListener(v -> {
                Intent intent = new Intent(CampusDashboardActivity.this, ReportToAdminActivity.class);
                startActivity(intent);
            });
        }

        if (tvBrowseAll != null) {
            tvBrowseAll.setOnClickListener(v -> {
                startActivity(new Intent(this, AllReportedItemsActivity.class));
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

        // Handle navigation state from intent
        applyNavigationStateFromIntent(getIntent());
    }

    private void checkSessionAndRedirect() {
        if (mAuth.getCurrentUser() != null) {
            SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
            String universityId = prefs.getString("universityId", "");

            if (!universityId.isEmpty()) {
                mDatabase.child("Users").child(universityId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                boolean dbIsAdmin = "admin".equalsIgnoreCase(user.getRole()) || user.isAdmin() || "Admin".equalsIgnoreCase(user.getUserType());
                                if (dbIsAdmin) {
                                    startActivity(new Intent(CampusDashboardActivity.this, AdminDashboardActivity.class));
                                    finish();
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            } else {
                boolean isAdminLoggedIn = prefs.getBoolean("isAdminLoggedIn", false);
                String userType = prefs.getString("userType", "");
                if (isAdminLoggedIn || "Admin".equalsIgnoreCase(userType)) {
                    startActivity(new Intent(this, AdminDashboardActivity.class));
                    finish();
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyNavigationStateFromIntent(intent);
    }

    /**
     * Dedicated function to apply navigation drawer state and selection from intent extras.
     * This ensures the drawer opens and the correct item is highlighted when returning from sub-activities.
     */
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

        // Setup Stat Cards
        View viewLost = findViewById(R.id.cardUserLostReports);
        if (viewLost != null) {
            tvLostCount = viewLost.findViewById(R.id.tvUserLostCount);
            tvLostLabel = viewLost.findViewById(R.id.tvUserLostLabel);
            cardLostReports = viewLost;
            cardLostReports.setOnClickListener(v -> {
                Intent intent = new Intent(this, CampusMyItemsActivity.class);
                intent.putExtra("filterType", "find");
                startActivity(intent);
            });
        }

        View viewFound = findViewById(R.id.cardUserFoundReports);
        if (viewFound != null) {
            tvFoundCount = viewFound.findViewById(R.id.tvUserFoundCount);
            tvFoundLabel = viewFound.findViewById(R.id.tvUserFoundLabel);
            cardFoundReports = viewFound;
            cardFoundReports.setOnClickListener(v -> {
                Intent intent = new Intent(this, CampusMyItemsActivity.class);
                intent.putExtra("filterType", "reported");
                startActivity(intent);
            });
        }
    }

    /**
     * Dedicated function to handle navigation drawer behavior,
     * specifically resetting the selection state when the drawer is closed.
     */
    private void setupNavigationDrawerBehavior() {
        if (drawerLayout != null && navigationView != null) {
            drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
                @Override
                public void onDrawerClosed(View drawerView) {
                    super.onDrawerClosed(drawerView);
                    resetNavigationSelection();
                }
            });
        }
    }

    /**
     * Resets the checked state of all items in the navigation menu.
     */
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
                } else {
                    item.setChecked(false);
                }
            }
        }
    }

    private void setupNavigationView() {
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_profile) {
                    Intent intent = new Intent(this, UserProfileActivity.class);
                    intent.putExtra("fromDrawer", true);
                    startActivity(intent);
                } else if (id == R.id.nav_reported_items) {
                    Intent intent = new Intent(this, CampusMyItemsActivity.class);
                    intent.putExtra("filterType", "reported");
                    intent.putExtra("fromDrawer", true);
                    startActivity(intent);
                } else if (id == R.id.nav_find_items) {
                    Intent intent = new Intent(this, CampusMyItemsActivity.class);
                    intent.putExtra("filterType", "find");
                    intent.putExtra("fromDrawer", true);
                    startActivity(intent);
                } else if (id == R.id.nav_resolved_items) {
                    Intent intent = new Intent(this, CampusMyItemsActivity.class);
                    intent.putExtra("filterType", "resolved");
                    intent.putExtra("fromDrawer", true);
                    startActivity(intent);
                } else if (id == R.id.nav_admin_reports) {
                    Intent intent = new Intent(this, CampusMyItemsActivity.class);
                    intent.putExtra("filterType", "admin_reports");
                    intent.putExtra("fromDrawer", true);
                    startActivity(intent);
                } else if (id == R.id.nav_logout) {
                    mAuth.signOut();
                    getSharedPreferences("MyApp", MODE_PRIVATE).edit().clear().apply();
                    Intent intent = new Intent(this, UserLoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
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
                    switch (tabText) {
                        case "Home":
                            break;
                        case "Browse Items":
                        case "Browse":
                            startActivity(new Intent(CampusDashboardActivity.this, BrowseItemsActivity.class));
                            break;
                        case "Report":
                            startActivity(new Intent(CampusDashboardActivity.this, ReportToAdminActivity.class));
                            break;
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String authUid = currentUser.getUid();

            if (ADMIN_UID.equals(authUid)) {
                promoteToAdmin(authUid);
            }

            mDatabase.child("UIDToUniversityID").child(authUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    currentUniversityId = snapshot.exists() ? snapshot.getValue(String.class) : authUid;

                    mDatabase.child("Users").child(currentUniversityId).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            if (userSnapshot.exists()) {
                                User user = userSnapshot.getValue(User.class);
                                if (user != null) {
                                    tvWelcome.setText("Welcome back, " + user.getName() + "!");
                                    if (tvNavHeaderName != null) tvNavHeaderName.setText(user.getName());
                                    if (ivNavHeaderProfile != null) {
                                        loadNavHeaderProfileImageRectangular(user.getProfileImageUrl());
                                        setupProfileImageFullScreenViewer(user.getProfileImageUrl());
                                    }
                                }
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });

                    fetchUserStats();
                    listenForNotifications();
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    /**
     * Dedicated function to ensure the navigation profile image remains rectangular.
     * This fix is persistent across refreshes and updates.
     */
    private void loadNavHeaderProfileImageRectangular(String imageUrl) {
        if (ivNavHeaderProfile == null) return;

        if (imageUrl != null && !imageUrl.isEmpty()) {
            GlideApp.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_user)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(ivNavHeaderProfile);
        } else {
            ivNavHeaderProfile.setImageResource(R.drawable.ic_user);
            ivNavHeaderProfile.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }
    }

    /**
     * Sets up the full-screen image viewer for the navigation profile image.
     * Provides zoom functionality and smooth navigation.
     */
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

    private void promoteToAdmin(String uid) {
        mDatabase.child("Users").child(uid).child("role").setValue("admin");
        mDatabase.child("Users").child(uid).child("userType").setValue("Admin");
    }

    private void fetchUserStats() {
        if (currentUniversityId == null) return;

        lostReportsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot itemSnap : snapshot.getChildren()) {
                    Item item = itemSnap.getValue(Item.class);
                    if (item != null && "lost".equalsIgnoreCase(item.getStatus()) && !"Claimed".equalsIgnoreCase(item.getAdminStatus()) && !"Returned".equalsIgnoreCase(item.getAdminStatus())) {
                        count++;
                    }
                }
                tvLostCount.setText(String.valueOf(count));
                tvLostLabel.setText(count == 1 ? "Lost Report" : "Lost Reports");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("LostItems").orderByChild("userId").equalTo(currentUniversityId).addValueEventListener(lostReportsListener);

        foundReportsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int count = 0;
                for (DataSnapshot itemSnap : snapshot.getChildren()) {
                    Item item = itemSnap.getValue(Item.class);
                    if (item != null && "found".equalsIgnoreCase(item.getStatus()) && !"Claimed".equalsIgnoreCase(item.getAdminStatus()) && !"Returned".equalsIgnoreCase(item.getAdminStatus())) {
                        count++;
                    }
                }
                tvFoundCount.setText(String.valueOf(count));
                tvFoundLabel.setText(count == 1 ? "Found Report" : "Found Reports");
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("FoundItems").orderByChild("userId").equalTo(currentUniversityId).addValueEventListener(foundReportsListener);
    }

    private void listenForNotifications() {
        if (currentUniversityId == null) return;

        notificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int unreadCount = 0;
                for (DataSnapshot noteSnap : snapshot.getChildren()) {
                    Notification notification = noteSnap.getValue(Notification.class);
                    if (notification != null && !notification.isRead()) {
                        unreadCount++;
                    }
                }

                if (unreadCount > 0) {
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(String.valueOf(unreadCount));
                } else {
                    tvNotificationBadge.setVisibility(View.GONE);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("Notifications").child(currentUniversityId).addValueEventListener(notificationListener);
    }

    private void fetchRecentItems() {
        recentLostItemsQuery = mDatabase.child("LostItems").orderByChild("timestamp").limitToLast(10);
        recentFoundItemsQuery = mDatabase.child("FoundItems").orderByChild("timestamp").limitToLast(10);

        recentLostItemsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mergeAndRefreshItems();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        recentFoundItemsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mergeAndRefreshItems();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };

        recentLostItemsQuery.addValueEventListener(recentLostItemsListener);
        recentFoundItemsQuery.addValueEventListener(recentFoundItemsListener);
    }

    private void mergeAndRefreshItems() {
        recentLostItemsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot lostSnapshot) {
                recentFoundItemsQuery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot foundSnapshot) {
                        List<Item> combined = new ArrayList<>();
                        for (DataSnapshot ds : lostSnapshot.getChildren()) {
                            Item item = ds.getValue(Item.class);
                            if (item != null) combined.add(item);
                        }
                        for (DataSnapshot ds : foundSnapshot.getChildren()) {
                            Item item = ds.getValue(Item.class);
                            if (item != null) combined.add(item);
                        }

                        // Sort by timestamp descending
                        combined.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

                        fullItemList.clear();
                        fullItemList.addAll(combined);
                        updateDisplayedList();
                        
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateDisplayedList() {
        displayedItemList.clear();
        int limit = Math.min(currentLimit, fullItemList.size());
        for (int i = 0; i < limit; i++) {
            displayedItemList.add(fullItemList.get(i));
        }
        adapter.notifyDataSetChanged();

        if (fullItemList.size() > currentLimit) {
            btnViewMore.setVisibility(View.VISIBLE);
        } else {
            btnViewMore.setVisibility(View.GONE);
        }

        if (currentLimit > 5) {
            btnViewLess.setVisibility(View.VISIBLE);
        } else {
            btnViewLess.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null && currentUniversityId != null) {
            mDatabase.child("Notifications").child(currentUniversityId).removeEventListener(notificationListener);
        }
        if (lostReportsListener != null) {
            mDatabase.child("LostItems").orderByChild("userId").equalTo(currentUniversityId).removeEventListener(lostReportsListener);
        }
        if (foundReportsListener != null) {
            mDatabase.child("FoundItems").orderByChild("userId").equalTo(currentUniversityId).removeEventListener(foundReportsListener);
        }
        if (recentLostItemsListener != null && recentLostItemsQuery != null) {
            recentLostItemsQuery.removeEventListener(recentLostItemsListener);
        }
        if (recentFoundItemsListener != null && recentFoundItemsQuery != null) {
            recentFoundItemsQuery.removeEventListener(recentFoundItemsListener);
        }
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
            holder.tvReportId.setText(item.getDisplayId() != null ? item.getDisplayId() : "");

            boolean isResolved = "Claimed".equalsIgnoreCase(item.getAdminStatus()) || "Returned".equalsIgnoreCase(item.getAdminStatus());

            if (isResolved) {
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_resolved_bg));
                holder.tvBadge.setText(R.string.status_resolved);
                if (holder.cardBadge != null) {
                    holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_resolved_bg));
                }
            } else if ("lost".equals(item.getStatus())) {
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_lost_bg));
                holder.tvBadge.setText(R.string.status_lost_label);
                if (holder.cardBadge != null) {
                    holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_lost_bg));
                }
            } else {
                holder.statusIndicator.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_found_bg));
                holder.tvBadge.setText(R.string.status_found_label);
                if (holder.cardBadge != null) {
                    holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_found_bg));
                }
            }
            holder.tvBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

            setupImageOrSlider(holder, item, position);

            // Unified click listener for the entire card
            holder.itemView.setOnClickListener(v -> ItemNavigationUtils.navigateToDetail(v.getContext(), item));
        }

        private void setupImageOrSlider(ViewHolder holder, Item item, int position) {
            List<String> urls = item.getImageUrls();
            if (urls != null && urls.size() > 1 && holder.viewPagerSlider != null) {
                holder.ivIcon.setVisibility(View.GONE);
                holder.viewPagerSlider.setVisibility(View.VISIBLE);
                if (holder.tabLayoutIndicator != null) {
                    holder.tabLayoutIndicator.setVisibility(View.VISIBLE);
                }

                // Use fitCenter (true) for multiple images to prevent zooming in cards
                ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(urls, true);
                // Redirect image taps to card click behavior
                sliderAdapter.setOnImageClickListener(pos -> ItemNavigationUtils.navigateToDetail(holder.itemView.getContext(), item));
                
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

                    // Redirect image click to card click behavior (detail view)
                    holder.ivIcon.setOnClickListener(v -> ItemNavigationUtils.navigateToDetail(v.getContext(), item));
                } else {
                    holder.ivIcon.setImageResource(R.drawable.ic_package);
                    holder.ivIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    holder.ivIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(holder.itemView.getContext(), R.color.textSecondary)));
                    // Still navigate to details even for placeholder icons
                    holder.ivIcon.setOnClickListener(v -> ItemNavigationUtils.navigateToDetail(v.getContext(), item));
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
                tvReportId = itemView.findViewById(R.id.tvReportId);
                viewPagerSlider = itemView.findViewById(R.id.viewPagerSlider);
                tabLayoutIndicator = itemView.findViewById(R.id.tabLayoutIndicator);
            }
        }
    }
}
