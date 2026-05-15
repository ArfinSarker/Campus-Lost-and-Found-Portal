package com.sas.lostandfound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminReportDetailsActivity extends AppCompatActivity {

    private TextView tvHeaderTitle;
    private TextView tvReporterName, tvUniversityId, tvReporterRole, tvReporterDeptDesignation, tvReporterPhone;
    private TextView tvTitle, tvCategory, tvDescription, tvRelatedId, tvDate, tvPriority;
    private TextView tvFinalReportId, tvFinalStatus, tvFinalAdminNote;
    private ImageView ivEvidence;
    private ViewPager2 viewPagerEvidence;
    private TabLayout tabLayoutIndicator;
    private View cardEvidence;
    private TextView tvNoEvidence;
    private MaterialButton btnDelete;
    
    private Toolbar toolbar;
    private ProgressBar progressBar;

    private String reportId;
    private AdminReport currentReport;

    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
    private boolean isUserInteracting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        RoleVerifier.checkUserAccess(this);

        setContentView(R.layout.activity_admin_report_details);

        reportId = getIntent().getStringExtra("reportId");
        if (reportId == null) {
            SnackbarManager.show(SnackbarManager.Type.ERROR, "Error: Report ID is missing");
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        fetchReportDetails();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initializeViews() {
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvReporterName = findViewById(R.id.tvDetailReporterName);
        tvUniversityId = findViewById(R.id.tvDetailUniversityId);
        tvReporterRole = findViewById(R.id.tvDetailReporterRole);
        tvReporterDeptDesignation = findViewById(R.id.tvDetailReporterDeptDesignation);
        tvReporterPhone = findViewById(R.id.tvDetailReporterPhone);
        
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvRelatedId = findViewById(R.id.tvDetailRelatedId);
        tvDate = findViewById(R.id.tvDetailDate);
        tvPriority = findViewById(R.id.tvDetailPriority);
        
        ivEvidence = findViewById(R.id.ivDetailEvidence);
        viewPagerEvidence = findViewById(R.id.viewPagerEvidence);
        tabLayoutIndicator = findViewById(R.id.tabLayoutIndicator);
        cardEvidence = findViewById(R.id.cardEvidence);
        tvNoEvidence = findViewById(R.id.tvNoEvidence);
        
        tvFinalReportId = findViewById(R.id.tvFinalReportId);
        tvFinalStatus = findViewById(R.id.tvFinalStatus);
        tvFinalAdminNote = findViewById(R.id.tvFinalAdminNote);
        
        btnDelete = findViewById(R.id.btnDeleteUserReport);
        
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);

        ivEvidence.setOnClickListener(v -> {
            if (currentReport != null) {
                List<String> urls = currentReport.getImageUrls();
                if (urls != null && !urls.isEmpty()) {
                    ItemNavigationUtils.openFullScreenImage(this, urls, 0);
                } else if (currentReport.getImageUrl() != null) {
                    List<String> singleUrl = new ArrayList<>();
                    singleUrl.add(currentReport.getImageUrl());
                    ItemNavigationUtils.openFullScreenImage(this, singleUrl, 0);
                }
            }
        });

        tvRelatedId.setOnClickListener(v -> {
            if (currentReport != null && currentReport.getRelatedId() != null && !currentReport.getRelatedId().isEmpty()) {
                navigateToRelatedItem(currentReport.getRelatedId());
            }
        });

        btnDelete.setOnClickListener(v -> confirmDeleteForUser());

        viewPagerEvidence.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    isUserInteracting = true;
                    stopAutoSlide();
                }
            }
        });

        viewPagerEvidence.getChildAt(0).setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
                isUserInteracting = true;
                stopAutoSlide();
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                isUserInteracting = false;
                if (currentReport != null && currentReport.getImageUrls() != null && currentReport.getImageUrls().size() > 1) {
                    startAutoSlide(currentReport.getImageUrls().size());
                }
            }
            return false;
        });
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
            com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
            if (appBarLayout != null) {
                HeaderColorHelper.setup(this, appBarLayout, toolbar);
            }
        }
    }

    private void fetchReportDetails() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        SupabaseDatabaseHelper.select("admin_reports", "id=eq." + reportId + "&limit=1", new TypeToken<List<AdminReport>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<AdminReport>>() {
            @Override
            public void onSuccess(List<AdminReport> reports) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (reports != null && !reports.isEmpty()) {
                    currentReport = reports.get(0);
                    displayReportDetails(currentReport);
                    fetchReporterExtraInfo(currentReport.getUniversityId());
                } else finish();
            }
            @Override public void onFailure(String e) { if (progressBar != null) progressBar.setVisibility(View.GONE); }
        });
    }

    private void fetchReporterExtraInfo(String universityId) {
        if (universityId == null) return;
        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + universityId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users != null && !users.isEmpty()) {
                    User user = users.get(0);
                    if (user != null) {
                        tvReporterRole.setText("Role: " + user.getUserType());
                        if ("Student".equalsIgnoreCase(user.getUserType())) {
                            tvReporterDeptDesignation.setText("Department: " + (user.getDepartment() != null ? user.getDepartment() : "N/A"));
                            tvReporterDeptDesignation.setVisibility(View.VISIBLE);
                        } else {
                            tvReporterDeptDesignation.setText("Designation: " + (user.getDesignation() != null ? user.getDesignation() : "N/A"));
                            tvReporterDeptDesignation.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
            @Override public void onFailure(String e) {}
        });
    }

    private void displayReportDetails(AdminReport report) {
        tvHeaderTitle.setText("Report Details");
        tvReporterName.setText("Name: " + report.getReporterName());
        tvUniversityId.setText("University ID: " + report.getUniversityId());
        tvReporterPhone.setText("Phone: " + report.getPhone());
        tvTitle.setText(report.getTitle());
        tvCategory.setText("Category: " + report.getCategory());
        tvDescription.setText(report.getDescription());
        String related = report.getRelatedId();
        if (related == null || related.isEmpty() || "None".equalsIgnoreCase(related)) {
            tvRelatedId.setText("Related Report ID: None");
            tvRelatedId.setEnabled(false);
            tvRelatedId.setTextColor(Color.GRAY);
        } else {
            tvRelatedId.setText("Related Report ID: " + ReportIdFormatter.format(related));
            tvRelatedId.setEnabled(true);
            tvRelatedId.setTextColor(getResources().getColor(R.color.primaryColor));
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        tvDate.setText("Submitted: " + sdf.format(new Date(report.getTimestamp())));
        tvPriority.setText("Priority: " + report.getPriority());
        setupImageSlider(report.getImageUrls(), report.getImageUrl());
        String displayId = ReportIdFormatter.format(report.getDisplayId());
        tvFinalReportId.setText("Report ID: " + displayId);
        tvFinalStatus.setText("Report Status: " + (report.getStatus() != null ? report.getStatus() : "Pending"));
        tvFinalAdminNote.setText("Admin Note: " + (report.getAdminNote() != null ? report.getAdminNote() : "None"));
    }

    private void setupImageSlider(List<String> urls, String fallbackUrl) {
        ItemNavigationUtils.setupImageOrSlider(this, urls, fallbackUrl, ivEvidence, viewPagerEvidence, tabLayoutIndicator);
        if (urls != null && urls.size() > 1) {
            tvNoEvidence.setVisibility(View.GONE);
            startAutoSlide(urls.size());
        } else {
            stopAutoSlide();
            String finalUrl = (urls != null && !urls.isEmpty()) ? urls.get(0) : fallbackUrl;
            if (finalUrl != null && !finalUrl.isEmpty()) tvNoEvidence.setVisibility(View.GONE);
            else { cardEvidence.setVisibility(View.GONE); ivEvidence.setVisibility(View.GONE); tvNoEvidence.setVisibility(View.VISIBLE); }
        }
    }

    private void startAutoSlide(int size) {
        if (isUserInteracting) return;
        stopAutoSlide();
        sliderRunnable = () -> {
            int current = viewPagerEvidence.getCurrentItem();
            int next = (current + 1) % size;
            viewPagerEvidence.setCurrentItem(next, true);
            sliderHandler.postDelayed(sliderRunnable, 3000);
        };
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    private void stopAutoSlide() { if (sliderRunnable != null) sliderHandler.removeCallbacks(sliderRunnable); }

    private void confirmDeleteForUser() {
        new AlertDialog.Builder(this).setTitle("Delete Report").setMessage("Permanently remove this report?").setPositiveButton("Delete", (dialog, which) -> {
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
            
            SupabaseDatabaseHelper.delete("admin_reports", "id=eq." + reportId, new SupabaseDatabaseHelper.DatabaseCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    ErrorHelper.showError(tvTitle, "Failed to delete: " + errorMessage);
                }
            });
        }).setNegativeButton("Cancel", null).show();
    }

    private void navigateToRelatedItem(String relatedId) {
        if (relatedId == null || relatedId.isEmpty()) return;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        ReportNavigationHelper.resolve(relatedId, new ReportNavigationHelper.ResolutionCallback() {
            @Override
            public void onResolved(Object report) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (report instanceof Item) {
                    openItemDetails((Item) report);
                } else if (report instanceof AdminReport) {
                    openAdminReportDetails((AdminReport) report);
                }
            }

            @Override
            public void onError(String message) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Error: " + message);
            }

            @Override
            public void onNotFound() {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Related item not found: " + relatedId);
            }
        });
    }

    private void openItemDetails(Item item) {
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("itemId", item.getId());
        intent.putExtra("itemStatus", item.getStatus());
        intent.putExtra("userId", item.getUserId());
        intent.putExtra("itemName", item.getName());
        startActivity(intent);
    }

    private void openAdminReportDetails(AdminReport report) {
        Intent intent = new Intent(this, AdminReportDetailsActivity.class);
        intent.putExtra("reportId", report.getId());
        startActivity(intent);
    }

    @Override protected void onPause() { super.onPause(); stopAutoSlide(); }
    @Override protected void onResume() { super.onResume(); if (currentReport != null && currentReport.getImageUrls() != null && currentReport.getImageUrls().size() > 1) startAutoSlide(currentReport.getImageUrls().size()); }
    @Override protected void onDestroy() { super.onDestroy(); stopAutoSlide(); }
}
