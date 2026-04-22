package com.sas.lostandfound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private MaterialButton btnReportLost, btnReportFound, btnSignIn, btnViewMore;
    private TextView tvDeveloperInfo;
    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private List<Item> itemList;
    private SwipeRefreshLayout swipeRefreshLayout;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private ValueEventListener lostItemsListener, foundItemsListener;
    private Query lostItemsQuery, foundItemsQuery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).getReference();

        // Check if already logged in and redirect accordingly
        checkSessionAndRedirect();

        // Initialize views
        btnReportLost = findViewById(R.id.btnReportLost);
        btnReportFound = findViewById(R.id.btnReportFound);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnViewMore = findViewById(R.id.btnViewMore);
        tvDeveloperInfo = findViewById(R.id.tvDeveloperInfo);
        recyclerView = findViewById(R.id.recyclerViewRecent);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        itemList = new ArrayList<>();
        // Use a specific card layout for the dashboard items
        adapter = new ItemAdapter(itemList, R.layout.item_dashboard_card, item -> {
            // Show toast message using application context to ensure it persists during transition
            Toast.makeText(getApplicationContext(), R.string.login_to_view_details, Toast.LENGTH_SHORT).show();
            // Redirect to Login Screen when an item is clicked
            startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
        });
        
        // Use GridLayoutManager for uniform card sizes in a grid
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);

        setupSwipeRefresh();
        loadRecentItems();
        
        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout);
        }

        // Set click listeners
        btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
        });

        btnReportLost.setOnClickListener(v -> {
            Toast.makeText(getApplicationContext(), R.string.login_to_report_lost, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
        });

        btnReportFound.setOnClickListener(v -> {
            Toast.makeText(getApplicationContext(), R.string.login_to_report_found, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
        });

        btnViewMore.setOnClickListener(v -> {
            Toast.makeText(getApplicationContext(), R.string.login_to_view_more, Toast.LENGTH_SHORT).show();
            startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
        });

        if (tvDeveloperInfo != null) {
            tvDeveloperInfo.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, DeveloperInfoActivity.class));
            });
        }
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(this::loadRecentItems);
        }
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
                                Intent intent;
                                if (dbIsAdmin) {
                                    intent = new Intent(DashboardActivity.this, AdminDashboardActivity.class);
                                } else {
                                    intent = new Intent(DashboardActivity.this, CampusDashboardActivity.class);
                                }
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            } else {
                // Fallback to local prefs if universityId is missing
                boolean isAdminLoggedIn = prefs.getBoolean("isAdminLoggedIn", false);
                String userType = prefs.getString("userType", "");
                Intent intent;
                if (isAdminLoggedIn || "Admin".equalsIgnoreCase(userType)) {
                    intent = new Intent(this, AdminDashboardActivity.class);
                } else {
                    intent = new Intent(this, CampusDashboardActivity.class);
                }
                startActivity(intent);
                finish();
            }
        }
    }

    private void loadRecentItems() {
        // Remove existing listeners if any
        if (lostItemsQuery != null && lostItemsListener != null) {
            lostItemsQuery.removeEventListener(lostItemsListener);
        }
        if (foundItemsQuery != null && foundItemsListener != null) {
            foundItemsQuery.removeEventListener(foundItemsListener);
        }

        ValueEventListener itemListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Item item = data.getValue(Item.class);
                        if (item != null) {
                            updateOrAddItem(item);
                        }
                    }
                    // Sort items by timestamp (most recent first)
                    itemList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                    
                    // Keep only the 6 most recent items
                    if (itemList.size() > 6) {
                        itemList.subList(6, itemList.size()).clear();
                    }

                    adapter.notifyDataSetChanged();
                }
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        };

        lostItemsListener = itemListener;
        foundItemsListener = itemListener;

        // Use queries with limits to improve performance and responsiveness
        lostItemsQuery = mDatabase.child("LostItems").orderByChild("timestamp").limitToLast(10);
        foundItemsQuery = mDatabase.child("FoundItems").orderByChild("timestamp").limitToLast(10);

        lostItemsQuery.addValueEventListener(lostItemsListener);
        foundItemsQuery.addValueEventListener(foundItemsListener);
    }

    private synchronized void updateOrAddItem(Item item) {
        for (int i = 0; i < itemList.size(); i++) {
            if (itemList.get(i).getId().equals(item.getId())) {
                itemList.set(i, item);
                return;
            }
        }
        itemList.add(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkSessionAndRedirect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners to prevent memory leaks and redundant background processing
        if (lostItemsQuery != null && lostItemsListener != null) {
            lostItemsQuery.removeEventListener(lostItemsListener);
        }
        if (foundItemsQuery != null && foundItemsListener != null) {
            foundItemsQuery.removeEventListener(foundItemsListener);
        }
    }
}
