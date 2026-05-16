package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";
    private TextView tvTotalAdminRequests, tvTotalAdminReports, tvAdminTitle;
    private MaterialCardView cardAdminRequests, cardAdminReports;
    private MaterialButton btnManageItems, btnLogout, btnAdminRequests, btnManageUsers, btnAdminReports;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure only admins can access this dashboard
        RoleVerifier.checkAdminAccess(this);

        // Switch to Admin Mode for moderation and management controls
        ModeManager.setMode(this, ModeManager.MODE_ADMIN);

        setContentView(R.layout.activity_admin_dashboard);

        initializeViews();
        setupClickListeners();
        setupSwipeRefresh();

        // Defer heavy operations to improve transition smoothness
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            loadStats();
            setupAdminDashboard();
        }, 150);

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh stats whenever admin returns to dashboard
        loadStats();
    }

    private void initializeViews() {
        tvAdminTitle = findViewById(R.id.tvAdminTitle);
        tvTotalAdminRequests = findViewById(R.id.tvTotalAdminRequests);
        tvTotalAdminReports = findViewById(R.id.tvTotalAdminReports);

        cardAdminRequests = findViewById(R.id.cardAdminRequests);
        cardAdminReports = findViewById(R.id.cardAdminReports);

        btnAdminRequests = findViewById(R.id.btnAdminRequests);
        btnAdminReports = findViewById(R.id.btnAdminReports);
        btnManageItems = findViewById(R.id.btnManageItems);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        btnLogout = findViewById(R.id.btnLogout);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupAdminDashboard() {
        if (tvAdminTitle != null)
            tvAdminTitle.setText(R.string.title_admin_dashboard);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.primaryColor);
            swipeRefreshLayout.setOnRefreshListener(() -> {
                loadStats();
                // Since we use addValueEventListener, it might already be fresh,
                // but this triggers a re-fetch. We'll stop refreshing after a short delay
                // or when the first listener completes.
                new android.os.Handler().postDelayed(() -> {
                    if (swipeRefreshLayout.isRefreshing()) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                }, 1500);
            });
        }
    }

    private void setupClickListeners() {
        cardAdminRequests.setOnClickListener(v -> {
            if (!ItemNavigationUtils.canNavigate())
                return;
            startActivity(new Intent(this, AdminRequestsActivity.class));
            overridePendingTransition(R.anim.material_shared_axis_z_enter, R.anim.material_shared_axis_z_exit);
        });

        cardAdminReports.setOnClickListener(v -> {
            if (!ItemNavigationUtils.canNavigate())
                return;
            startActivity(new Intent(this, AdminReportManagementActivity.class));
            overridePendingTransition(R.anim.material_shared_axis_z_enter, R.anim.material_shared_axis_z_exit);
        });

        btnAdminRequests.setOnClickListener(v -> {
            if (!ItemNavigationUtils.canNavigate())
                return;
            startActivity(new Intent(this, AdminRequestsActivity.class));
            overridePendingTransition(R.anim.material_shared_axis_z_enter, R.anim.material_shared_axis_z_exit);
        });

        btnAdminReports.setOnClickListener(v -> {
            if (!ItemNavigationUtils.canNavigate())
                return;
            startActivity(new Intent(this, AdminReportManagementActivity.class));
            overridePendingTransition(R.anim.material_shared_axis_z_enter, R.anim.material_shared_axis_z_exit);
        });

        btnManageItems.setOnClickListener(v -> {
            if (!ItemNavigationUtils.canNavigate())
                return;
            startActivity(new Intent(this, ManageItemsActivity.class));
            overridePendingTransition(R.anim.material_shared_axis_z_enter, R.anim.material_shared_axis_z_exit);
        });

        btnManageUsers.setOnClickListener(v -> {
            if (!ItemNavigationUtils.canNavigate())
                return;
            startActivity(new Intent(this, AllUsersActivity.class));
            overridePendingTransition(R.anim.material_shared_axis_z_enter, R.anim.material_shared_axis_z_exit);
        });

        btnLogout.setOnClickListener(v -> {
            if (!ItemNavigationUtils.canNavigate())
                return;
            
            // Switch back to User Mode instead of logging out
            ModeManager.setMode(this, ModeManager.MODE_USER);
            
            Intent intent = new Intent(this, CampusDashboardActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            overridePendingTransition(R.anim.material_shared_axis_z_pop_enter, R.anim.material_shared_axis_z_pop_exit);
        });

    }

    private void loadStats() {
        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setRefreshing(true);

        // 1. Parallel Admin Requests Count
        SupabaseDatabaseHelper.select("admin_requests", "select=count", new TypeToken<List<Map<String, Object>>>() {
        }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> result) {
                if (result != null && !result.isEmpty()) {
                    Object countObj = result.get(0).get("count");
                    long count = (countObj instanceof Number) ? ((Number) countObj).longValue() : 0;
                    tvTotalAdminRequests.setText(String.valueOf(count));
                }
                checkRefreshStatus();
            }

            @Override
            public void onFailure(String errorMessage) {
                checkRefreshStatus();
            }
        });

        // 2. Parallel User Reports Count
        SupabaseDatabaseHelper.select("admin_reports", "select=count", new TypeToken<List<Map<String, Object>>>() {
        }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Map<String, Object>>>() {
            @Override
            public void onSuccess(List<Map<String, Object>> result) {
                if (result != null && !result.isEmpty()) {
                    Object countObj = result.get(0).get("count");
                    long count = (countObj instanceof Number) ? ((Number) countObj).longValue() : 0;
                    tvTotalAdminReports.setText(String.valueOf(count));
                }
            }

            @Override
            public void onFailure(String errorMessage) {
            }
        });
    }

    private void checkRefreshStatus() {
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

}
