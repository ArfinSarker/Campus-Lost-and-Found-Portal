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
import android.widget.Toast;
import android.net.Uri;

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
    private TextView tvReporterName, tvDetailTitle, tvDetailCategory, tvDetailDescription, tvDetailRelatedId;
    private TextView tvReporterUniversityId, tvReporterRole, tvReporterDeptDesignation, tvReporterPhone, tvReporterEmail;
    private ImageView ivEvidence;
    private ViewPager2 viewPagerEvidence;
    private TabLayout tabLayoutIndicator;
    private View cardEvidence;
    
    private AutoCompleteTextView actvUpdateStatus;
    private View tilUpdateStatus;
    private TextView tvReviewedStatus;
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

    private String currentAdminUnivId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Ensure only admins can review reports
        RoleVerifier.checkAdminAccess(this);

        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentAdminUnivId = prefs.getString("universityId", "admin");

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
        tvReporterUniversityId = findViewById(R.id.tvReporterUniversityId);
        tvReporterRole = findViewById(R.id.tvReporterRole);
        tvReporterDeptDesignation = findViewById(R.id.tvReporterDeptDesignation);
        tvReporterPhone = findViewById(R.id.tvReporterPhone);
        tvReporterEmail = findViewById(R.id.tvReporterEmail);

        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailCategory = findViewById(R.id.tvDetailCategory);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailRelatedId = findViewById(R.id.tvDetailRelatedId);

        ivEvidence = findViewById(R.id.ivDetailEvidence);
        viewPagerEvidence = findViewById(R.id.viewPagerEvidence);
        tabLayoutIndicator = findViewById(R.id.tabLayoutIndicator);
        cardEvidence = findViewById(R.id.cardEvidence);
        
        actvUpdateStatus = findViewById(R.id.actvUpdateStatus);
        tilUpdateStatus = findViewById(R.id.tilUpdateStatus);
        tvReviewedStatus = findViewById(R.id.tvReviewedStatus);
        etAdminNote = findViewById(R.id.etAdminNote);
        btnUpdate = findViewById(R.id.btnUpdateReport);
        btnDelete = findViewById(R.id.btnDeleteReport);
        
        MaterialButton btnViewProfile = findViewById(R.id.btnViewProfile);
        if (btnViewProfile != null) {
            btnViewProfile.setOnClickListener(v -> {
                if (currentReport != null && currentReport.getUniversityId() != null) {
                    Intent intent = new Intent(this, UserProfileActivity.class);
                    intent.putExtra("isAdminViewing", true);
                    intent.putExtra("targetUserId", currentReport.getUniversityId());
                    startActivity(intent);
                }
            });
        }

        View layoutReporterPhone = findViewById(R.id.layoutReporterPhone);
        if (layoutReporterPhone != null) {
            layoutReporterPhone.setOnClickListener(v -> {
                if (currentReport != null && currentReport.getPhone() != null && !currentReport.getPhone().trim().isEmpty() && !"N/A".equalsIgnoreCase(currentReport.getPhone().trim())) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + currentReport.getPhone().trim()));
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to open phone dialer", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        View layoutReporterEmail = findViewById(R.id.layoutReporterEmail);
        if (layoutReporterEmail != null) {
            layoutReporterEmail.setOnClickListener(v -> {
                if (currentReport != null && currentReport.getEmail() != null && !currentReport.getEmail().trim().isEmpty() && !"N/A".equalsIgnoreCase(currentReport.getEmail().trim())) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setData(Uri.parse("mailto:" + currentReport.getEmail().trim()));
                        String subject = "Regarding Admin Report: " + (currentReport.getDisplayId() != null ? currentReport.getDisplayId() : "");
                        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
                        startActivity(Intent.createChooser(intent, "Send Email"));
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to open email app", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        
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
        
        // Reporter info bindings
        tvReporterName.setText("Name: " + (report.getReporterName() != null ? report.getReporterName() : "N/A"));
        tvReporterUniversityId.setText("University ID: " + (report.getUniversityId() != null ? report.getUniversityId() : "N/A"));
        applyStyledLinkText(tvReporterPhone, "Phone: ", report.getPhone() != null ? report.getPhone() : "N/A");
        if (tvReporterEmail != null) {
            applyStyledLinkText(tvReporterEmail, "Email: ", report.getEmail() != null ? report.getEmail() : "N/A");
        }
        fetchReporterExtraInfo(report.getUniversityId());

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


        setupEvidenceSlider(report.getImageUrls(), report.getImageUrl());

        // Action section
        boolean isReviewed = "Reviewed".equalsIgnoreCase(report.getStatus());
        
        if (isReviewed) {
            tilUpdateStatus.setVisibility(View.GONE);
            tvReviewedStatus.setVisibility(View.VISIBLE);
            btnUpdate.setVisibility(View.GONE);
            etAdminNote.setEnabled(false);
            etAdminNote.setFocusable(false);
            etAdminNote.setCursorVisible(false);
            etAdminNote.setAlpha(0.8f);
        } else {
            tilUpdateStatus.setVisibility(View.VISIBLE);
            tvReviewedStatus.setVisibility(View.GONE);
            btnUpdate.setVisibility(View.VISIBLE);
            etAdminNote.setEnabled(true);
            etAdminNote.setFocusableInTouchMode(true);
            etAdminNote.setCursorVisible(true);
            etAdminNote.setAlpha(1.0f);
        }

        actvUpdateStatus.setText(report.getStatus(), false);
        etAdminNote.setText(report.getAdminNote());
    }

    private void fetchReporterExtraInfo(String universityId) {
        if (universityId == null || tvReporterRole == null) return;
        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + universityId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users != null && !users.isEmpty()) {
                    User user = users.get(0);
                    if (user != null) {
                        tvReporterRole.setText("Role: " + user.getUserType());
                        if ("Student".equalsIgnoreCase(user.getUserType())) {
                            tvReporterDeptDesignation.setText("Department: " + (user.getDepartment() != null ? user.getDepartment() : "N/A"));
                        } else {
                            tvReporterDeptDesignation.setText("Designation: " + (user.getDesignation() != null ? user.getDesignation() : "N/A"));
                        }
                    }
                }
            }
            @Override public void onFailure(String e) {}
        });
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

    /**
     * Helper to apply clickable link styling: Label in black, Value in primary theme color and underlined.
     */
    private void applyStyledLinkText(TextView textView, String label, String value) {
        String fullText = label + value;
        SpannableString spannable = new SpannableString(fullText);
        
        // Label in Black
        spannable.setSpan(new ForegroundColorSpan(Color.BLACK), 0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        if (value != null && !value.isEmpty() && !"N/A".equalsIgnoreCase(value)) {
            // Value in Theme Color & Underlined
            int themeColor = ContextCompat.getColor(this, R.color.primaryColor);
            spannable.setSpan(new ForegroundColorSpan(themeColor), label.length(), fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new android.text.style.UnderlineSpan(), label.length(), fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            // Muted color for N/A
            int mutedColor = ContextCompat.getColor(this, R.color.textSecondary);
            spannable.setSpan(new ForegroundColorSpan(mutedColor), label.length(), fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
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
        // If it was Pending and admin is saving, it should transition to Reviewed
        if ("Pending".equalsIgnoreCase(newStatus)) {
            newStatus = "Reviewed";
        }
        
        final String finalStatus = newStatus;
        final String finalAdminNote = etAdminNote.getText().toString().trim();
        progressBar.setVisibility(View.VISIBLE);
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", finalStatus);
        updates.put("admin_note", finalAdminNote);
        updates.put("updated_at_timestamp", System.currentTimeMillis());

        final Long reviewTime = System.currentTimeMillis();
        if ("Reviewed".equalsIgnoreCase(finalStatus)) {
            updates.put("reviewed_by", currentAdminUnivId);
            updates.put("review_timestamp", reviewTime);
        } else {
            updates.put("reviewed_by", null);
            updates.put("review_timestamp", null);
        }

        SupabaseDatabaseHelper.update("admin_reports", "id=eq." + reportId, updates, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                progressBar.setVisibility(View.GONE);
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Report updated successfully");
                
                // Refresh local report data to get the latest title etc.
                if (currentReport != null) {
                    currentReport.setStatus(finalStatus);
                    currentReport.setAdminNote(finalAdminNote);
                    if ("Reviewed".equalsIgnoreCase(finalStatus)) {
                        currentReport.setReviewedBy(currentAdminUnivId);
                        currentReport.setReviewTimestamp(reviewTime);
                    } else {
                        currentReport.setReviewedBy(null);
                        currentReport.setReviewTimestamp(null);
                    }
                    displayReportDetails(currentReport); // Refresh UI state immediately
                }
                
                sendNotificationToUser(currentReport.getUniversityId(), finalStatus);
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Update failed: " + errorMessage);
            }
        });
    }

    private void sendNotificationToUser(String userId, String status) {
        if (userId == null) return;
        
        // We always attempt to send. If recipientAuthId is null here, 
        // the database trigger 'fn_populate_notification_user_id' will 
        // automatically resolve it server-side using the University ID.
        String recipientAuthId = currentReport != null ? currentReport.getReporterAuthId() : null;
        
        android.util.Log.d("AdminReportReview", "Preparing notification for User: " + userId + " (AuthID: " + recipientAuthId + ")");
        performNotificationInsert(userId, recipientAuthId);
    }

    private void performNotificationInsert(String userId, String recipientAuthId) {
        // Format: "Admin" has reviewed your "report name". Click to view details.
        String reportTitle = currentReport.getTitle() != null ? currentReport.getTitle() : "Report";
        String message = "Admin has reviewed your \"" + reportTitle + "\". Click to view details";
        String notificationId = UUID.randomUUID().toString();
        
        Notification notification = new Notification(notificationId, userId, currentAdminUnivId, "Admin", "", "", "", 
            currentReport.getId(), reportTitle, message, System.currentTimeMillis(), "admin_report", "");
        notification.setUserId(recipientAuthId); // Set for RLS
        
        SupabaseDatabaseHelper.insert("notifications", notification, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override public void onSuccess(String r) {
                android.util.Log.d("AdminReportReview", "Notification sent successfully");
            }
            @Override public void onFailure(String e) {
                android.util.Log.e("AdminReportReview", "Failed to send notification: " + e);
                // Show error to admin so they know delivery failed
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Status updated, but notification failed: " + e);
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
