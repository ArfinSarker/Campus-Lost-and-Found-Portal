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
                            Intent intent = new Intent(DashboardActivity.this, CampusDashboardActivity.class);
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
            // For safety, redirect to CampusDashboardActivity which will then handle role-based navigation.
            SharedPreferences prefs1 = getSharedPreferences("MyApp", MODE_PRIVATE);
            if (!prefs1.getString("universityId", "").isEmpty()) {
                startActivity(new Intent(this, CampusDashboardActivity.class));
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
                        
                        itemList.clear();
                        itemList.addAll(combined);
                        adapter.notifyDataSetChanged();
                        
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
