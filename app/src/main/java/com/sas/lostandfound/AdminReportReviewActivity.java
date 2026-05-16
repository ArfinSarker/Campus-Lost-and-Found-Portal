package com.sas.lostandfound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.reflect.TypeToken;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;

/**
 * Admin version of Report Details.
 * Focused on reviewing and updating reports.
 */
public class AdminReportReviewActivity extends AppCompatActivity {

    private TextView tvHeaderTitle;
    private TextView tvReporterName, tvDetailTitle, tvDetailCategory, tvDetailDescription, tvDetailRelatedId, tvDetailPriority;
    private ImageView ivEvidence;
    private ViewPager2 viewPagerEvidence;
    private TabLayout tabLayoutIndicator;
    private View cardEvidence;
    
    private AutoCompleteTextView actvUpdateStatus;
    private TextInputEditText etAdminNote;
    private MaterialButton btnUpdate, btnDelete;
    
    private Toolbar toolbar;
    private ProgressBar progressBar;
    private SwipeRefreshLayout swipeRefreshLayout;

    private String reportId;
    private AdminReport currentReport;
    private boolean isFetching = false;

    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
    private boolean isUserInteracting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ensure only admins can review reports
        RoleVerifier.checkAdminAccess(this);

        setContentView(R.layout.activity_admin_report_review);

        reportId = getIntent().getStringExtra("reportId");
        if (reportId == null) {
            SnackbarManager.show(SnackbarManager.Type.ERROR, "Error: Report ID is missing");
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        setupStatusDropdown();
        setupSwipeRefresh();
        fetchReportDetails();

        btnUpdate.setOnClickListener(v -> updateReport());
        btnDelete.setOnClickListener(v -> confirmDelete());

        // Ensure back press always exits the activity immediately
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
        
        tvReporterName = findViewById(R.id.tvReporterName);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailCategory = findViewById(R.id.tvDetailCategory);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailRelatedId = findViewById(R.id.tvDetailRelatedId);
        tvDetailPriority = findViewById(R.id.tvDetailPriority);

        ivEvidence = findViewById(R.id.ivDetailEvidence);
        viewPagerEvidence = findViewById(R.id.viewPagerEvidence);
        tabLayoutIndicator = findViewById(R.id.tabLayoutIndicator);
        cardEvidence = findViewById(R.id.cardEvidence);
        
        actvUpdateStatus = findViewById(R.id.actvUpdateStatus);
        etAdminNote = findViewById(R.id.etAdminNote);
        btnUpdate = findViewById(R.id.btnUpdateReport);
        btnDelete = findViewById(R.id.btnDeleteReport);
        
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Slider interaction handling
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
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
            
            com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
            if (appBarLayout != null) {
                HeaderColorHelper.setup(this, appBarLayout, toolbar);
            }
        }
    }

    private void setupStatusDropdown() {
        String[] statuses = {"Pending", "Reviewed"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, statuses);
        actvUpdateStatus.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(this::fetchReportDetails);
        }
    }

    private void fetchReportDetails() {
        if (isFetching) return;
        isFetching = true;
        
        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        }
        
        SupabaseDatabaseHelper.select("admin_reports", "id=eq." + reportId + "&limit=1", new TypeToken<List<AdminReport>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<AdminReport>>() {
            @Override
            public void onSuccess(List<AdminReport> reports) {
                isFetching = false;
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (reports != null && !reports.isEmpty()) {
                    currentReport = reports.get(0);
                    displayReportDetails(currentReport);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                isFetching = false;
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void displayReportDetails(AdminReport report) {
        tvHeaderTitle.setText("Report Details");
        
        // Reporter Name with Clickable Link (Spannable)
        String reporterName = report.getReporterName() != null ? report.getReporterName() : "N/A";
        applyStyledText(tvReporterName, "Name: ", reporterName);
        
        if (report.getUniversityId() != null) {
            tvReporterName.setOnClickListener(v -> {
                Intent intent = new Intent(this, UserProfileActivity.class);
                intent.putExtra("isAdminViewing", true);
                intent.putExtra("targetUserId", report.getUniversityId());
                startActivity(intent);
            });
        }

        // Report Information Section Styling
        applyStyledText(tvDetailTitle, "Report Title: ", report.getTitle() != null ? report.getTitle() : "N/A");
        applyStyledText(tvDetailCategory, "Category: ", report.getCategory() != null ? report.getCategory() : "N/A");
        applyStyledText(tvDetailDescription, "Description: ", report.getDescription() != null ? report.getDescription() : "No detailed information provided.");
        
        String relatedId = report.getRelatedId();
        if (relatedId == null || relatedId.isEmpty() || "None".equalsIgnoreCase(relatedId)) {
            applyStyledText(tvDetailRelatedId, "Related Report ID: ", "None");
            tvDetailRelatedId.setEnabled(false);
            tvDetailRelatedId.setOnClickListener(null);
        } else {
            applyStyledText(tvDetailRelatedId, "Related Report ID: ", ReportIdFormatter.format(relatedId));
            tvDetailRelatedId.setEnabled(true);
            tvDetailRelatedId.setOnClickListener(v -> navigateToRelatedItem(relatedId));
        }
        
        tvDetailPriority.setText("Priority: " + (report.getPriority() != null ? report.getPriority() : "N/A"));

        setupEvidenceSlider(report.getImageUrls(), report.getImageUrl());

        // Action section
        actvUpdateStatus.setText(report.getStatus(), false);
        etAdminNote.setText(report.getAdminNote());
    }

    /**
     * Helper to apply styling: Label in black, Value in primary theme color.
     */
    private void applyStyledText(TextView textView, String label, String value) {
        String fullText = label + value;
        SpannableString spannable = new SpannableString(fullText);
        
        // Label in Black
        spannable.setSpan(new ForegroundColorSpan(Color.BLACK), 0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        // Value in Theme Color
        int themeColor = ContextCompat.getColor(this, R.color.primaryColor);
        spannable.setSpan(new ForegroundColorSpan(themeColor), label.length(), fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        textView.setText(spannable);
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
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        Intent intent = new Intent(this, ItemDetailActivity.class);
        intent.putExtra("itemId", item.getId());
        intent.putExtra("itemStatus", item.getStatus());
        intent.putExtra("userId", item.getUserId());
        intent.putExtra("itemName", item.getName());
        startActivity(intent);
    }

    private void openAdminReportDetails(AdminReport report) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        Intent intent = new Intent(this, AdminReportReviewActivity.class);
        intent.putExtra("reportId", report.getId());
        startActivity(intent);
    }

    private void setupEvidenceSlider(List<String> urls, String fallbackUrl) {
        if (urls != null && urls.size() > 1) {
            ivEvidence.setVisibility(View.GONE);
            viewPagerEvidence.setVisibility(View.VISIBLE);
            tabLayoutIndicator.setVisibility(View.VISIBLE);
            cardEvidence.setVisibility(View.VISIBLE);

            // Use fitCenter (true) for multiple images to prevent zooming
            ImageSliderAdapter adapter = new ImageSliderAdapter(urls, true);
            // Default click behavior in ImageSliderAdapter now opens FullScreenImageActivity
            viewPagerEvidence.setAdapter(adapter);

            new TabLayoutMediator(tabLayoutIndicator, viewPagerEvidence, (tab, position) -> {}).attach();
            startAutoSlide(urls.size());
        } else {
            stopAutoSlide();
            viewPagerEvidence.setVisibility(View.GONE);
            tabLayoutIndicator.setVisibility(View.GONE);
            
            String finalUrl = (urls != null && !urls.isEmpty()) ? urls.get(0) : fallbackUrl;
            if (finalUrl != null && !finalUrl.isEmpty()) {
                cardEvidence.setVisibility(View.VISIBLE);
                ivEvidence.setVisibility(View.VISIBLE);
                GlideApp.with(this)
                        .load(finalUrl)
                        .placeholder(R.drawable.ic_package)
                        .thumbnail(0.1f)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivEvidence);
                ivEvidence.setOnClickListener(v -> {
                    List<String> singleUrl = new ArrayList<>();
                    singleUrl.add(finalUrl);
                    ItemNavigationUtils.openFullScreenImage(this, singleUrl, 0);
                });
            } else {
                cardEvidence.setVisibility(View.GONE);
            }
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

    private void stopAutoSlide() {
        if (sliderRunnable != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Report")
                .setMessage("Are you sure you want to permanently delete this report?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    SupabaseDatabaseHelper.delete("admin_reports", "id=eq." + reportId, new SupabaseDatabaseHelper.DatabaseCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            progressBar.setVisibility(View.GONE);
                            finish();
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            progressBar.setVisibility(View.GONE);
                            ErrorHelper.showError(btnUpdate, "Delete failed: " + errorMessage);
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateReport() {
        String newStatus = actvUpdateStatus.getText().toString();
        String adminNote = etAdminNote.getText().toString().trim();
        progressBar.setVisibility(View.VISIBLE);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("admin_note", adminNote);
        updates.put("updated_at", System.currentTimeMillis());

        SupabaseDatabaseHelper.update("admin_reports", "id=eq." + reportId, updates, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                progressBar.setVisibility(View.GONE);
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Report updated successfully");
                
                // Refresh local report data to get the latest title etc.
                if (currentReport != null) {
                    currentReport.setStatus(newStatus);
                    currentReport.setAdminNote(adminNote);
                }
                
                sendNotificationToUser(currentReport.getUniversityId(), newStatus);
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Update failed: " + errorMessage);
            }
        });
    }

    private void sendNotificationToUser(String userId, String status) {
        if (userId == null || !"Reviewed".equalsIgnoreCase(status)) return;
        
        // Use reporterAuthId directly from currentReport for RLS if available
        String recipientAuthId = currentReport.getReporterAuthId();
        
        if (recipientAuthId != null && !recipientAuthId.isEmpty()) {
            performNotificationInsert(userId, recipientAuthId);
        } else {
            // Fallback to fetch recipient's Auth ID from profiles if missing in report
            SupabaseDatabaseHelper.select("profiles", "university_id=eq." + userId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
                @Override
                public void onSuccess(List<User> recipients) {
                    if (recipients != null && !recipients.isEmpty()) {
                        performNotificationInsert(userId, recipients.get(0).getAuthId());
                    }
                }
                @Override public void onFailure(String e) {}
            });
        }
    }

    private void performNotificationInsert(String userId, String recipientAuthId) {
        // Required format: Your report "<Report Name>" has been reviewed. Click to see details.
        String reportTitle = currentReport.getTitle() != null ? currentReport.getTitle() : "Report";
        String message = "Your report \"" + reportTitle + "\" has been reviewed. Click to see details.";
        String notificationId = UUID.randomUUID().toString();
        
        Notification notification = new Notification(notificationId, userId, "admin", "Admin", "", "", 
            currentReport.getId(), reportTitle, message, System.currentTimeMillis(), "admin_report", "");
        notification.setUserId(recipientAuthId); // Set for RLS
        
        SupabaseDatabaseHelper.insert("notifications", notification, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override public void onSuccess(String r) {}
            @Override public void onFailure(String e) {
                android.util.Log.e("AdminReportReview", "Failed to send notification: " + e);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoSlide();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentReport != null && currentReport.getImageUrls() != null && currentReport.getImageUrls().size() > 1) {
            startAutoSlide(currentReport.getImageUrls().size());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoSlide();
    }
}
