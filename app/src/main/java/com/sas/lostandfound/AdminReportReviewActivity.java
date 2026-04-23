package com.sas.lostandfound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Admin version of Report Details.
 * Focused on reviewing and updating reports.
 */
public class AdminReportReviewActivity extends AppCompatActivity {

    private TextView tvHeaderTitle;
    private ImageView ivEvidence;
    private ViewPager2 viewPagerEvidence;
    private TabLayout tabLayoutIndicator;
    private View cardEvidence;
    
    // Views from included report card
    private TextView tvCardStatus, tvCardId, tvCardTitle, tvCardReporter, tvCardCategory, tvCardTime;
    private ViewPager2 cardViewPagerSlider;
    private TabLayout cardTabLayoutIndicator;
    private ImageView cardIvIcon;
    
    private AutoCompleteTextView actvUpdateStatus;
    private TextInputEditText etAdminNote;
    private MaterialButton btnUpdate, btnDelete;
    
    private Toolbar toolbar;
    private ProgressBar progressBar;

    private DatabaseReference mDatabase;
    private String reportId;
    private AdminReport currentReport;

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
            Toast.makeText(this, "Error: Report ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).getReference();

        initializeViews();
        setupToolbar();
        setupStatusDropdown();
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
        
        ivEvidence = findViewById(R.id.ivDetailEvidence);
        viewPagerEvidence = findViewById(R.id.viewPagerEvidence);
        tabLayoutIndicator = findViewById(R.id.tabLayoutIndicator);
        cardEvidence = findViewById(R.id.cardEvidence);
        
        // Find views in the included layout
        View reportCard = findViewById(R.id.includedReportCard);
        tvCardStatus = reportCard.findViewById(R.id.tvStatusBadge);
        tvCardId = reportCard.findViewById(R.id.tvDisplayId);
        tvCardTitle = reportCard.findViewById(R.id.tvReportTitle);
        tvCardReporter = reportCard.findViewById(R.id.tvReporterInfo);
        tvCardCategory = reportCard.findViewById(R.id.tvCategory);
        tvCardTime = reportCard.findViewById(R.id.tvTimestamp);
        cardViewPagerSlider = reportCard.findViewById(R.id.viewPagerSlider);
        cardTabLayoutIndicator = reportCard.findViewById(R.id.tabLayoutIndicator);
        cardIvIcon = reportCard.findViewById(R.id.ivReportIcon);
        
        actvUpdateStatus = findViewById(R.id.actvUpdateStatus);
        etAdminNote = findViewById(R.id.etAdminNote);
        btnUpdate = findViewById(R.id.btnUpdateReport);
        btnDelete = findViewById(R.id.btnDeleteReport);
        
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);

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

    private void fetchReportDetails() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("AdminReports").child(reportId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    currentReport = snapshot.getValue(AdminReport.class);
                    if (currentReport != null) {
                        displayReportDetails(currentReport);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void displayReportDetails(AdminReport report) {
        tvHeaderTitle.setText("Report Details: " + (report.getTitle() != null ? report.getTitle() : "N/A"));
        
        // Populate the report card area
        String displayId = report.getDisplayId() != null ? report.getDisplayId() : "N/A";
        if (!displayId.startsWith("#")) displayId = "#" + displayId;
        tvCardId.setText(displayId);
        
        tvCardTitle.setText(report.getTitle());
        tvCardCategory.setText("Category: " + report.getCategory());
        tvCardReporter.setText("Submitted By: " + report.getReporterName());
        
        String status = report.getStatus() != null ? report.getStatus() : "Pending";
        tvCardStatus.setText(status.toUpperCase());
        
        int statusColor = "Reviewed".equalsIgnoreCase(status) ? 0xFF1976D2 : 0xFF757575;
        tvCardStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor));
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        tvCardTime.setText(sdf.format(new Date(report.getTimestamp())));

        setupReportCardSlider(report.getImageUrls(), report.getImageUrl());
        setupEvidenceSlider(report.getImageUrls(), report.getImageUrl());

        // Action section
        actvUpdateStatus.setText(report.getStatus(), false);
        etAdminNote.setText(report.getAdminNote());
    }

    private void setupReportCardSlider(List<String> urls, String fallbackUrl) {
        ItemNavigationUtils.setupImageOrSlider(this, urls, fallbackUrl, cardIvIcon, cardViewPagerSlider, cardTabLayoutIndicator);
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
                .setMessage("Are you sure you want to delete this report for both you and the reporter?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    mDatabase.child("AdminReports").child(reportId).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                finish();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateReport() {
        String newStatus = actvUpdateStatus.getText().toString();
        String adminNote = etAdminNote.getText().toString().trim();
        progressBar.setVisibility(View.VISIBLE);
        
        mDatabase.child("AdminReports").child(reportId).child("status").setValue(newStatus);
        mDatabase.child("AdminReports").child(reportId).child("adminNote").setValue(adminNote);
        mDatabase.child("AdminReports").child(reportId).child("updatedAt").setValue(System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Report updated successfully", Toast.LENGTH_SHORT).show();
                    sendNotificationToUser(currentReport.getUniversityId(), newStatus);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotificationToUser(String userId, String status) {
        if (userId == null || "Pending".equalsIgnoreCase(status)) return;
        String message = "Your report \"" + currentReport.getTitle() + "\" has been reviewed by the admin.";
        String notificationId = mDatabase.child("Notifications").child(userId).push().getKey();
        if (notificationId != null) {
            Notification notification = new Notification(notificationId, userId, "admin", "Admin", "", "", 
                currentReport.getReportId(), currentReport.getTitle(), message, System.currentTimeMillis(), "admin_report", "");
            mDatabase.child("Notifications").child(userId).child(notificationId).setValue(notification);
        }
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
