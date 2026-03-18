package com.sas.lostandfound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CampusDashboardActivity extends AppCompatActivity {

    private static final String TAG = "CampusDashboard";
    private RecyclerView rvRecentItems;
    private RecentItemsAdapter adapter;
    private List<Item> fullItemList;
    private List<Item> displayedItemList;
    
    private TextView tvWelcome, tvActiveItems, tvDeveloperInfo, tvBrowseAll;
    private MaterialButton btnReportLost, btnReportFound, btnViewMore, btnViewLess;
    private ImageButton btnMenu;
    private FrameLayout btnNotifications;
    private TextView tvNotificationBadge;
    private TabLayout tabLayout;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    
    private ImageView ivNavHeaderProfile;
    private TextView tvNavHeaderName;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    private int currentLimit = 5;
    private String currentUniversityId;
    private ValueEventListener notificationListener;

    // Hardcoded Admin UID
    private static final String ADMIN_UID = "NhwPcU2nOySmd2zHDiLlvsgMBYV2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check if admin is logged in and redirect if necessary
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        boolean isAdminLoggedIn = prefs.getBoolean("isAdminLoggedIn", false);
        String userType = prefs.getString("userType", "");
        
        if (isAdminLoggedIn || "Admin".equalsIgnoreCase(userType)) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_campus_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initializeViews();
        setupRecyclerView();
        setupTabLayout();
        setupNavigationView();
        fetchUserData();
        fetchRecentItems();

        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        
        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
        });

        btnReportLost.setOnClickListener(v -> startActivity(new Intent(this, CampusReportLostActivity.class)));
        btnReportFound.setOnClickListener(v -> startActivity(new Intent(this, CampusReportFoundActivity.class)));

        if (tvDeveloperInfo != null) {
            tvDeveloperInfo.setOnClickListener(v -> {
                Intent intent = new Intent(CampusDashboardActivity.this, DeveloperInfoActivity.class);
                startActivity(intent);
            });
        }

        if (tvBrowseAll != null) {
            tvBrowseAll.setOnClickListener(v -> {
                startActivity(new Intent(this, AllReportedItemsActivity.class));
            });
        }

        btnViewMore.setOnClickListener(v -> {
            currentLimit += 5;
            updateDisplayedList();
        });

        btnViewLess.setOnClickListener(v -> {
            if (currentLimit > 5) {
                currentLimit -= 5;
                updateDisplayedList();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra("openDrawer", false)) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    private void initializeViews() {
        rvRecentItems = findViewById(R.id.rvRecentItems);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvActiveItems = findViewById(R.id.tvActiveItems);
        tvDeveloperInfo = findViewById(R.id.tvDeveloperInfo);
        btnReportLost = findViewById(R.id.btnReportLost);
        btnReportFound = findViewById(R.id.btnReportFound);
        btnMenu = findViewById(R.id.btnMenu);
        btnNotifications = findViewById(R.id.btnNotifications);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        tabLayout = findViewById(R.id.tabLayout);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        tvBrowseAll = findViewById(R.id.tvBrowseAll);
        btnViewMore = findViewById(R.id.btnViewMore);
        btnViewLess = findViewById(R.id.btnViewLess);

        View headerView = navigationView.getHeaderView(0);
        ivNavHeaderProfile = headerView.findViewById(R.id.nav_header_imageView);
        tvNavHeaderName = headerView.findViewById(R.id.nav_header_name);
    }

    private void setupNavigationView() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, UserProfileActivity.class));
            } else if (id == R.id.nav_reported_items) {
                Intent intent = new Intent(this, CampusMyItemsActivity.class);
                intent.putExtra("filterType", "reported");
                startActivity(intent);
            } else if (id == R.id.nav_find_items) {
                Intent intent = new Intent(this, CampusMyItemsActivity.class);
                intent.putExtra("filterType", "find");
                startActivity(intent);
            } else if (id == R.id.nav_return_items) {
                Intent intent = new Intent(this, CampusMyItemsActivity.class);
                intent.putExtra("filterType", "return");
                startActivity(intent);
            } else if (id == R.id.nav_claimed_items) {
                Intent intent = new Intent(this, CampusMyItemsActivity.class);
                intent.putExtra("filterType", "claimed");
                startActivity(intent);
            } else if (id == R.id.nav_logout) {
                mAuth.signOut();
                getSharedPreferences("MyApp", MODE_PRIVATE).edit().clear().apply();
                Intent intent = new Intent(this, UserLoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
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
                            startActivity(new Intent(CampusDashboardActivity.this, CampusReportLostActivity.class));
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
        fullItemList = new ArrayList<>();
        displayedItemList = new ArrayList<>();
        adapter = new RecentItemsAdapter(displayedItemList);
        rvRecentItems.setLayoutManager(new LinearLayoutManager(this));
        rvRecentItems.setNestedScrollingEnabled(false);
        rvRecentItems.setAdapter(adapter);
    }

    private void fetchUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String authUid = currentUser.getUid();
            
            // Promote specific user to Admin if they login
            if (ADMIN_UID.equals(authUid)) {
                promoteToAdmin(authUid);
            }

            mDatabase.child("UIDToUniversityID").child(authUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        currentUniversityId = snapshot.getValue(String.class);
                        loadProfileAndNotifications(currentUniversityId);
                    } else {
                        searchUserByEmail(currentUser);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    searchUserByEmail(currentUser);
                }
            });
        }
    }

    private void promoteToAdmin(String uid) {
        mDatabase.child("UIDToUniversityID").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String universityId = snapshot.exists() ? snapshot.getValue(String.class) : null;
                if (universityId != null) {
                    mDatabase.child("Users").child(universityId).child("userType").setValue("Admin");
                    mDatabase.child("Users").child(universityId).child("role").setValue("admin");
                    mDatabase.child("Users").child(universityId).child("isAdmin").setValue(true);
                } else {
                    // If no mapping, create one if possible or use UID as fallback
                    mDatabase.child("Users").child(uid).child("userType").setValue("Admin");
                    mDatabase.child("Users").child(uid).child("role").setValue("admin");
                    mDatabase.child("Users").child(uid).child("isAdmin").setValue(true);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void searchUserByEmail(FirebaseUser currentUser) {
        String email = currentUser.getEmail();
        String authUid = currentUser.getUid();
        if (email != null) {
            mDatabase.child("Users").orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot userSnap : snapshot.getChildren()) {
                            currentUniversityId = userSnap.getKey();
                            mDatabase.child("UIDToUniversityID").child(authUid).setValue(currentUniversityId);
                            loadProfileAndNotifications(currentUniversityId);
                            return;
                        }
                    } else {
                        currentUniversityId = authUid;
                        loadUserDataAndMapping(authUid);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    currentUniversityId = authUid;
                    loadUserDataAndMapping(authUid);
                }
            });
        } else {
            currentUniversityId = authUid;
            loadUserDataAndMapping(authUid);
        }
    }

    private void loadUserDataAndMapping(String userId) {
        // Ensure mapping exists for the current session
        loadProfileAndNotifications(userId);
    }

    private void loadProfileAndNotifications(String userId) {
        mDatabase.child("Users").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                    
                    if (name != null) {
                        tvWelcome.setText("Welcome back, " + name + "! 👋");
                        if (tvNavHeaderName != null) tvNavHeaderName.setText(name);
                    }
                    
                    if (profileImageUrl != null && !profileImageUrl.isEmpty() && !isFinishing()) {
                        Glide.with(CampusDashboardActivity.this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_user)
                                .centerCrop()
                                .into(ivNavHeaderProfile);
                    } else {
                        ivNavHeaderProfile.setImageResource(R.drawable.ic_user);
                        ivNavHeaderProfile.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        mDatabase.child("UserItems").child(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();
                tvActiveItems.setText("You have " + count + " active items");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Setup real-time notification badge
        if (notificationListener != null) {
            mDatabase.child("Notifications").child(userId).removeEventListener(notificationListener);
        }
        
        notificationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int unreadCount = 0;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Boolean read = data.child("read").getValue(Boolean.class);
                    if (read != null && !read) {
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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("Notifications").child(userId).addValueEventListener(notificationListener);
    }

    private void fetchRecentItems() {
        long twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
        
        ValueEventListener itemListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Item item = data.getValue(Item.class);
                    if (item != null && item.getTimestamp() >= twentyFourHoursAgo) {
                        updateOrAddFullItem(item);
                    }
                }
                fullItemList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                updateDisplayedList();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        mDatabase.child("LostItems").orderByChild("timestamp").startAt(twentyFourHoursAgo).addValueEventListener(itemListener);
        mDatabase.child("FoundItems").orderByChild("timestamp").startAt(twentyFourHoursAgo).addValueEventListener(itemListener);
    }

    private synchronized void updateOrAddFullItem(Item item) {
        for (int i = 0; i < fullItemList.size(); i++) {
            if (fullItemList.get(i).getId().equals(item.getId())) {
                fullItemList.set(i, item);
                return;
            }
        }
        fullItemList.add(item);
    }

    private void updateDisplayedList() {
        displayedItemList.clear();
        int end = Math.min(currentLimit, fullItemList.size());
        for (int i = 0; i < end; i++) {
            displayedItemList.add(fullItemList.get(i));
        }
        adapter.notifyDataSetChanged();
        
        btnViewMore.setVisibility(fullItemList.size() > currentLimit ? View.VISIBLE : View.GONE);
        btnViewLess.setVisibility(currentLimit > 5 ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notificationListener != null && currentUniversityId != null) {
            mDatabase.child("Notifications").child(currentUniversityId).removeEventListener(notificationListener);
        }
    }

    private class RecentItemsAdapter extends RecyclerView.Adapter<RecentItemsAdapter.ViewHolder> {
        private List<Item> items;

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
            holder.tvLocation.setText(item.getLocation());
            holder.tvTime.setText(item.getDate());
            
            if ("lost".equals(item.getStatus())) {
                holder.statusIndicator.setBackgroundColor(0xFFA31621);
                holder.tvBadge.setText("LOST");
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_lost_bg));
            } else {
                holder.statusIndicator.setBackgroundColor(0xFF2E7D32);
                holder.tvBadge.setText("FOUND");
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_found_bg));
            }
            holder.tvBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                holder.ivIcon.setImageTintList(null);
                holder.ivIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(holder.itemView.getContext())
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.ic_package)
                        .centerCrop()
                        .into(holder.ivIcon);
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_package);
                holder.ivIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                holder.ivIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.textSecondary)));
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ItemDetailActivity.class);
                intent.putExtra("itemId", item.getId());
                intent.putExtra("itemName", item.getName());
                intent.putExtra("itemDescription", item.getDescription());
                intent.putExtra("itemLocation", item.getLocation());
                intent.putExtra("itemDate", item.getDate());
                intent.putExtra("itemTime", item.getTime());
                intent.putExtra("itemStatus", item.getStatus());
                intent.putExtra("itemCategory", item.getCategory());
                intent.putExtra("itemImageUrl", item.getImageUrl());
                intent.putExtra("userName", item.getUserName());
                intent.putExtra("userDepartment", item.getUserDepartment());
                intent.putExtra("userPhone", item.getUserPhone());
                intent.putExtra("userId", item.getUserId());
                v.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvLocation, tvTime, tvBadge;
            ImageView ivIcon;
            View statusIndicator;
            MaterialCardView cardBadge;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvItemTitle);
                tvLocation = itemView.findViewById(R.id.tvItemLocation);
                tvTime = itemView.findViewById(R.id.tvItemTime);
                ivIcon = itemView.findViewById(R.id.ivItemIcon);
                statusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
                tvBadge = itemView.findViewById(R.id.tvBadge);
                cardBadge = itemView.findViewById(R.id.cardBadge);
            }
        }
    }
}
