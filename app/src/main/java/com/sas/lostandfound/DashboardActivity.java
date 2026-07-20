package com.sas.lostandfound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private MaterialButton btnReportLost, btnReportFound, btnSignIn, btnViewMore;
    private TextView tvDeveloperInfo;
    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private List<Item> itemList;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isFetching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        checkSessionAndRedirect();

        btnReportLost = findViewById(R.id.btnReportLost);
        btnReportFound = findViewById(R.id.btnReportFound);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnViewMore = findViewById(R.id.btnViewMore);
        tvDeveloperInfo = findViewById(R.id.tvDeveloperInfo);
        recyclerView = findViewById(R.id.recyclerViewRecent);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        itemList = new ArrayList<>();
        adapter = new ItemAdapter(itemList, R.layout.item_dashboard_card, item -> {
            if (!ItemNavigationUtils.canNavigate()) return;
            SnackbarManager.show(SnackbarManager.Type.PRIMARY, getString(R.string.login_to_view_details));
            startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
        });
        
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);

        // Load cached guest items asynchronously to avoid blocking the main thread
        SharedPreferences cachedPrefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        final String cachedGuestJson = cachedPrefs.getString("cachedGuestRecentItemsJson", "");
        if (!cachedGuestJson.isEmpty()) {
            new Thread(() -> {
                try {
                    final List<Item> cachedItems = new com.google.gson.Gson().fromJson(cachedGuestJson, 
                            new com.google.gson.reflect.TypeToken<List<Item>>(){}.getType());
                    if (cachedItems != null && !cachedItems.isEmpty()) {
                        runOnUiThread(() -> adapter.updateItems(cachedItems));
                    }
                } catch (Exception e) {
                    // ignore
                }
            }).start();
        }

        setupSwipeRefresh();
        loadRecentItems();
        
        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout);
        }

        btnSignIn.setOnClickListener(v -> {
            if (ItemNavigationUtils.canNavigate()) {
                startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
            }
        });

        btnReportLost.setOnClickListener(v -> {
            if (ItemNavigationUtils.canNavigate()) {
                SnackbarManager.show(SnackbarManager.Type.PRIMARY, getString(R.string.login_to_report_lost));
                startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
            }
        });

        btnReportFound.setOnClickListener(v -> {
            if (ItemNavigationUtils.canNavigate()) {
                SnackbarManager.show(SnackbarManager.Type.PRIMARY, getString(R.string.login_to_report_found));
                startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
            }
        });

        btnViewMore.setOnClickListener(v -> {
            if (ItemNavigationUtils.canNavigate()) {
                SnackbarManager.show(SnackbarManager.Type.PRIMARY, getString(R.string.login_to_view_more));
                startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
            }
        });

        if (tvDeveloperInfo != null) {
            tvDeveloperInfo.setOnClickListener(v -> {
                if (ItemNavigationUtils.canNavigate()) {
                    startActivity(new Intent(DashboardActivity.this, DeveloperInfoActivity.class));
                }
            });
        }

        // Smooth continuous breathing & floating animation for the main dashboard headline
        TextView dashboardHeadline = findViewById(R.id.dashboardHeadline);
        if (dashboardHeadline != null) {
            // Fading alpha breathing
            android.animation.ObjectAnimator fadeAnim = android.animation.ObjectAnimator.ofFloat(
                    dashboardHeadline, "alpha", 0.8f, 1.0f);
            fadeAnim.setDuration(2200);
            fadeAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            fadeAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            fadeAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

            // Floating vertical translation
            android.animation.ObjectAnimator floatAnim = android.animation.ObjectAnimator.ofFloat(
                    dashboardHeadline, "translationY", 0f, -6f); // moves up gently
            floatAnim.setDuration(2200);
            floatAnim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            floatAnim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            floatAnim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());

            android.animation.AnimatorSet animSet = new android.animation.AnimatorSet();
            animSet.playTogether(fadeAnim, floatAnim);
            animSet.start();
        }
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(this::loadRecentItems);
        }
    }

    private void checkSessionAndRedirect() {
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        String universityId = prefs.getString("universityId", "");

        if (!universityId.isEmpty()) {
            SupabaseDatabaseHelper.select("profiles", "university_id=eq." + universityId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
                @Override
                public void onSuccess(List<User> users) {
                    if (users != null && !users.isEmpty()) {
                        User user = users.get(0);
                        if (user != null) {
                            boolean isAdmin = prefs.getBoolean("isAdminLoggedIn", false);
                            String activeMode = prefs.getString("activeMode", "user");
                            Intent intent;
                            if (isAdmin && "admin".equals(activeMode)) {
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
                @Override public void onFailure(String e) {}
            });
        } else {
            // Already handled in CampusDashboardActivity for logged in users.
            // If they reach here, it might be an old session or something.
            // For safety, redirect to CampusDashboardActivity (or AdminDashboardActivity if admin was active) which will then handle role-based navigation.
            SharedPreferences prefs1 = getSharedPreferences("MyApp", MODE_PRIVATE);
            if (!prefs1.getString("universityId", "").isEmpty()) {
                boolean isAdmin = prefs1.getBoolean("isAdminLoggedIn", false);
                String activeMode = prefs1.getString("activeMode", "user");
                Intent intent;
                if (isAdmin && "admin".equals(activeMode)) {
                    intent = new Intent(DashboardActivity.this, AdminDashboardActivity.class);
                } else {
                    intent = new Intent(DashboardActivity.this, CampusDashboardActivity.class);
                }
                startActivity(intent);
                finish();
            }
        }
    }

    private void loadRecentItems() {
        if (isFetching) return;
        isFetching = true;

        SupabaseDatabaseHelper.select("lost_reports", "deleted_by_user=eq.false&order=timestamp.desc&limit=10", new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> lostItems) {
                SupabaseDatabaseHelper.select("found_reports", "deleted_by_user=eq.false&order=timestamp.desc&limit=10", new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
                    @Override
                    public void onSuccess(List<Item> foundItems) {
                        List<Item> combined = new ArrayList<>();
                        if (lostItems != null) combined.addAll(lostItems);
                        if (foundItems != null) {
                            for (Item item : foundItems) {
                                boolean exists = false;
                                for (Item existing : combined) {
                                    if (existing.getId().equals(item.getId())) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) combined.add(item);
                            }
                        }
                        
                        combined.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                        if (combined.size() > 6) {
                            combined = new ArrayList<>(combined.subList(0, 6));
                        }
                        
                        // Save guest recent items to SharedPreferences cache
                        try {
                            String json = new com.google.gson.Gson().toJson(combined);
                            getSharedPreferences("MyApp", MODE_PRIVATE).edit().putString("cachedGuestRecentItemsJson", json).apply();
                        } catch (Exception e) {
                            // ignore
                        }

                        // Use DiffUtil to compute exact changes
                        List<Item> newCombined = combined;
                        androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
                            @Override
                            public int getOldListSize() {
                                return itemList.size();
                            }

                            @Override
                            public int getNewListSize() {
                                return newCombined.size();
                            }

                            @Override
                            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                                Item oldItem = itemList.get(oldItemPosition);
                                Item newItem = newCombined.get(newItemPosition);
                                return oldItem.getId() != null && newItem.getId() != null && oldItem.getId().equals(newItem.getId());
                            }

                            @Override
                            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                                Item oldItem = itemList.get(oldItemPosition);
                                Item newItem = newCombined.get(newItemPosition);
                                return java.util.Objects.equals(oldItem.getName(), newItem.getName()) &&
                                       java.util.Objects.equals(oldItem.getLocation(), newItem.getLocation()) &&
                                       java.util.Objects.equals(oldItem.getDate(), newItem.getDate()) &&
                                       java.util.Objects.equals(oldItem.getAdminStatus(), newItem.getAdminStatus()) &&
                                       java.util.Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                                       java.util.Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl()) &&
                                       java.util.Objects.equals(oldItem.getImageUrls(), newItem.getImageUrls());
                            }
                        });

                        itemList.clear();
                        itemList.addAll(newCombined);
                        diffResult.dispatchUpdatesTo(adapter);
                        
                        isFetching = false;
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    }
                    @Override public void onFailure(String e) { 
                        isFetching = false;
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false); 
                    }
                });
            }
            @Override public void onFailure(String errorMessage) { 
                isFetching = false;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false); 
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkSessionAndRedirect();
    }
}
