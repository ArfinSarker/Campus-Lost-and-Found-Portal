package com.sas.lostandfound;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminReportReviewActivity extends AppCompatActivity {

    private TextView tvHeaderTitle;
    private TextView tvReporterName, tvUniversityId, tvReporterRole, tvReporterDeptDesignation, tvReporterPhone;
    private TextView tvTitle, tvCategory, tvDescription, tvRelatedId, tvDate, tvPriority;
    private ImageView ivEvidence;
    private View cardEvidence;
    private TextView tvNoEvidence;
    
    private AutoCompleteTextView actvUpdateStatus;
    private TextInputEditText etAdminNote;
    private MaterialButton btnUpdate, btnDelete;
    
    private Toolbar toolbar;
    private ProgressBar progressBar;

    private DatabaseReference mDatabase;
    private String reportId;
    private AdminReport currentReport;
    private static final String DATABASE_URL = "FIREBASE_URL_PLACEHOLDER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report_review);

        reportId = getIntent().getStringExtra("reportId");
        if (reportId == null) {
            Toast.makeText(this, "Error: Report ID is missing", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initializeViews();
        setupToolbar();
        setupStatusDropdown();
        fetchReportDetails();

        btnUpdate.setOnClickListener(v -> updateReport());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

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
        cardEvidence = findViewById(R.id.cardEvidence);
        tvNoEvidence = findViewById(R.id.tvNoEvidence);
        
        actvUpdateStatus = findViewById(R.id.actvUpdateStatus);
        etAdminNote = findViewById(R.id.etAdminNote);
        btnUpdate = findViewById(R.id.btnUpdateReport);
        btnDelete = findViewById(R.id.btnDeleteReport);
        
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
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
                        } else {
                            tvReporterDeptDesignation.setText("Designation: " + (user.getDesignation() != null ? user.getDesignation() : "N/A"));
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
        tvRelatedId.setText("Related Report ID: " + (report.getRelatedId() != null ? report.getRelatedId() : "None"));
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault());
        tvDate.setText("Submitted: " + sdf.format(new java.util.Date(report.getTimestamp())));
        tvPriority.setText("Priority: " + report.getPriority());

        if (report.getImageUrl() != null && !report.getImageUrl().isEmpty()) {
            cardEvidence.setVisibility(View.VISIBLE);
            tvNoEvidence.setVisibility(View.GONE);
            Glide.with(this).load(report.getImageUrl()).into(ivEvidence);
        } else {
            cardEvidence.setVisibility(View.GONE);
            tvNoEvidence.setVisibility(View.VISIBLE);
        }

        actvUpdateStatus.setText(report.getStatus(), false);
        etAdminNote.setText(report.getAdminNote());
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
}
