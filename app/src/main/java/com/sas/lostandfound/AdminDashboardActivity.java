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
    private TextView tvTotalAdminRequests, tvTotalAdminReports, tvAdminTitle, tvNotificationBadge, tvWelcomeAdmin;
    private MaterialCardView cardAdminRequests, cardAdminReports;
    private MaterialButton btnManageItems, btnLogout, btnAdminRequests, btnManageUsers, btnAdminReports;
    private View btnNotifications;
    private SwipeRefreshLayout swipeRefreshLayout;
    private String currentUniversityId;

    private android.os.Handler badgeHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable badgeRunnable = new Runnable() {
        @Override
        public void run() {
            listenForAdminNotifications();
            badgeHandler.postDelayed(this, 10000);
        }
    };

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

        setupAdminDashboard();

        // Refresh all data immediately on creation
        refreshAllData();

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            int headerColor = androidx.core.content.ContextCompat.getColor(this, R.color.admin_dash_header_bg);
            boolean isNight = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            HeaderColorHelper.setup(this, appBarLayout, headerColor, headerColor, !isNight);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Switch to Admin Mode for moderation and management controls
        ModeManager.setMode(this, ModeManager.MODE_ADMIN);
        
        // Refresh all data whenever admin returns to dashboard
        refreshAllData();
        badgeHandler.post(badgeRunnable);
    }

    private void refreshAllData() {
        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUniversityId = prefs.getString("universityId", null);
        
        loadStats();
        listenForAdminNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        badgeHandler.removeCallbacks(badgeRunnable);
    }

    private void initializeViews() {
        tvAdminTitle = findViewById(R.id.tvAdminTitle);
        tvTotalAdminRequests = findViewById(R.id.tvTotalAdminRequests);
        tvTotalAdminReports = findViewById(R.id.tvTotalAdminReports);
        tvWelcomeAdmin = findViewById(R.id.tvWelcomeAdmin);

        cardAdminRequests = findViewById(R.id.cardAdminRequests);
        cardAdminReports = findViewById(R.id.cardAdminReports);

        btnAdminRequests = findViewById(R.id.btnAdminRequests);
        btnAdminReports = findViewById(R.id.btnAdminReports);
        btnManageItems = findViewById(R.id.btnManageItems);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        btnLogout = findViewById(R.id.btnLogout);
        btnNotifications = findViewById(R.id.btnNotifications);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUniversityId = prefs.getString("universityId", null);
    }

    private void setupAdminDashboard() {
        if (tvAdminTitle != null)
            tvAdminTitle.setText(R.string.title_admin_dashboard);

        if (tvWelcomeAdmin != null) {
            android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
            String adminName = prefs.getString("cachedUserName", "Admin");
            if (adminName == null || adminName.trim().isEmpty()) {
                adminName = "Admin";
            }
            String welcomeText = "Welcome, " + adminName;

            startWelcomeLoop(tvWelcomeAdmin, welcomeText);
        }
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
            overridePendingTransition(R.anim.dash_transition_pop_enter, R.anim.dash_transition_pop_exit);
        });

        btnNotifications.setOnClickListener(v -> {
            if (ItemNavigationUtils.canNavigate()) {
                Intent intent = new Intent(this, NotificationsActivity.class);
                intent.putExtra("isAdminMode", true);
                startActivity(intent);
            }
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

    private void listenForAdminNotifications() {
        if (currentUniversityId == null) return;
        // Filter for admin types only: admin_report_new, admin_request
        String filter = "recipient_id=eq." + currentUniversityId + "&is_read=eq.false&type=in.(admin_report_new,admin_request)&select=count";
        SupabaseDatabaseHelper.select("notifications", filter, new TypeToken<List<Map<String, Object>>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Map<String, Object>>>() {
            @Override public void onSuccess(List<Map<String, Object>> res) {
                if (res != null && !res.isEmpty() && res.get(0).get("count") != null) {
                    long count = ((Number) res.get(0).get("count")).longValue();
                    if (tvNotificationBadge != null) {
                        tvNotificationBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                        tvNotificationBadge.setText(String.valueOf(count));
                    }
                }
            }
            @Override public void onFailure(String e) {
                Log.e(TAG, "Failed to fetch admin notification count: " + e);
            }
        });
    }

    private void checkRefreshStatus() {
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
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
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
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
                    }, 4000);
                });
            })
            .start();
    }

    private void animateTextTyping(final TextView textView, final String fullText, final Runnable onComplete) {
        textView.setText("");
        final int delay = 60; // ms per character
        final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.post(new Runnable() {
            private int index = 0;
            @Override
            public void run() {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (textView == null) {
                    return;
                }
                // Allow the first iteration (index == 0) to run during onCreate layout attachment transition,
                // but protect subsequent recursive loops by checking isAttachedToWindow().
                if (index > 0 && !textView.isAttachedToWindow()) {
                    return;
                }
                if (index <= fullText.length()) {
                    textView.setText(fullText.substring(0, index));
                    index++;
                    handler.postDelayed(this, delay);
                } else if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Prevent going back to the User Dashboard by minimizing the app (moving the task to the back).
        // This makes the Admin Dashboard behave as the active root screen.
        moveTaskToBack(true);
    }

}
