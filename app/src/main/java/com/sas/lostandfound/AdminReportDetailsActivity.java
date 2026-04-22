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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
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
 * User version of Admin Report Details.
 * Displays submitted report info and admin's response.
 */
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

    private DatabaseReference mDatabase;
    private String reportId;
    private AdminReport currentReport;

    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
    private boolean isUserInteracting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report_details);

        reportId = getIntent().getStringExtra("reportId");
        if (reportId == null) {
            Toast.makeText(this, "Error: Report ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).getReference();

        initializeViews();
        setupToolbar();
        fetchReportDetails();
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
                    openFullScreenImage(urls, 0);
                } else if (currentReport.getImageUrl() != null) {
                    List<String> singleUrl = new ArrayList<>();
                    singleUrl.add(currentReport.getImageUrl());
                    openFullScreenImage(singleUrl, 0);
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

    private void openFullScreenImage(List<String> imageUrls, int position) {
        if (imageUrls == null || imageUrls.isEmpty()) return;
        Intent intent = new Intent(this, FullScreenImageActivity.class);
        intent.putStringArrayListExtra("imageUrls", new ArrayList<>(imageUrls));
        intent.putExtra("position", position);
        startActivity(intent);
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
                        fetchReporterExtraInfo(currentReport.getUniversityId());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void fetchReporterExtraInfo(String universityId) {
        if (universityId == null) return;
        mDatabase.child("Users").child(universityId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
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
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayReportDetails(AdminReport report) {
        tvHeaderTitle.setText("Report Details: " + (report.getTitle() != null ? report.getTitle() : "N/A"));
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
            tvRelatedId.setText("Related Report ID: " + related);
            tvRelatedId.setEnabled(true);
            tvRelatedId.setTextColor(getResources().getColor(R.color.primaryColor));
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        tvDate.setText("Submitted: " + sdf.format(new Date(report.getTimestamp())));
        tvPriority.setText("Priority: " + report.getPriority());

        setupImageSlider(report.getImageUrls(), report.getImageUrl());

        String displayId = report.getDisplayId() != null ? report.getDisplayId() : "N/A";
        if (!displayId.startsWith("#")) displayId = "#" + displayId;
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
            if (finalUrl != null && !finalUrl.isEmpty()) {
                tvNoEvidence.setVisibility(View.GONE);
            } else {
                cardEvidence.setVisibility(View.GONE);
                ivEvidence.setVisibility(View.GONE);
                tvNoEvidence.setVisibility(View.VISIBLE);
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

    private void confirmDeleteForUser() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Report")
                .setMessage("Remove this report from your history? It will still be visible to the Admin.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                    mDatabase.child("AdminReports").child(reportId).child("deletedByUser").setValue(true)
                            .addOnSuccessListener(aVoid -> {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Report removed from your view", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void navigateToRelatedItem(String relatedId) {
        if (relatedId == null || relatedId.isEmpty()) return;
        
        progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("FoundItems").orderByChild("displayId").equalTo(relatedId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Item item = data.getValue(Item.class);
                        if (item != null) openItemDetails(item);
                        return;
                    }
                }
                
                mDatabase.child("LostItems").orderByChild("displayId").equalTo(relatedId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                Item item = data.getValue(Item.class);
                                if (item != null) openItemDetails(item);
                                return;
                            }
                        } else {
                            Toast.makeText(AdminReportDetailsActivity.this, "Related item not found", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { progressBar.setVisibility(View.GONE); }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { progressBar.setVisibility(View.GONE); }
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
