package com.sas.lostandfound;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminReportDetailsActivity extends AppCompatActivity {

    private static final String TAG = "AdminReportDetails";
    private TextView tvHeaderId;
    private TextView tvReporterName, tvUniversityId, tvReporterPhone;
    private TextView tvTitle, tvCategory, tvDescription, tvRelatedId;
    private TextView tvDate, tvPriority, tvStatus;
    private ImageView ivEvidence;
    private MaterialCardView cardEvidence;
    private TextView tvNoEvidence;
    private AutoCompleteTextView actvUpdateStatus;
    private TextInputEditText etAdminNote;
    private MaterialButton btnUpdate, btnDelete;
    private Toolbar toolbar;
    private ProgressBar progressBar;

    private DatabaseReference mDatabase;
    private String reportId;
    private AdminReport currentReport;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

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

        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initializeViews();
        setupToolbar();
        setupStatusDropdown();
        fetchReportDetails();

        btnUpdate.setOnClickListener(v -> updateReport());
        btnDelete.setOnClickListener(v -> confirmDelete());
    }

    private void initializeViews() {
        tvHeaderId = findViewById(R.id.tvHeaderId);
        tvReporterName = findViewById(R.id.tvDetailReporterName);
        tvUniversityId = findViewById(R.id.tvDetailUniversityId);
        tvReporterPhone = findViewById(R.id.tvDetailReporterPhone);
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvCategory = findViewById(R.id.tvDetailCategory);
        tvDescription = findViewById(R.id.tvDetailDescription);
        tvRelatedId = findViewById(R.id.tvDetailRelatedId);
        tvDate = findViewById(R.id.tvDetailDate);
        tvPriority = findViewById(R.id.tvDetailPriority);
        tvStatus = findViewById(R.id.tvDetailStatus);
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
        String[] statuses = {"Pending", "Reviewed", "Resolved"};
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
                    try {
                        currentReport = snapshot.getValue(AdminReport.class);
                        if (currentReport != null) {
                            displayReportDetails(currentReport);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing report details", e);
                        Toast.makeText(AdminReportDetailsActivity.this, "Error loading details", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AdminReportDetailsActivity.this, "Report no longer exists", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error fetching report details: " + error.getMessage());
            }
        });
    }

    private void displayReportDetails(AdminReport report) {
        if (tvHeaderId != null) tvHeaderId.setText("Report Details: " + (report.getDisplayId() != null ? report.getDisplayId() : "N/A"));
        if (tvReporterName != null) tvReporterName.setText("Name: " + (report.getReporterName() != null ? report.getReporterName() : "N/A"));
        if (tvUniversityId != null) tvUniversityId.setText("University ID: " + (report.getUniversityId() != null ? report.getUniversityId() : "N/A"));
        if (tvReporterPhone != null) tvReporterPhone.setText("Phone: " + (report.getPhone() != null ? report.getPhone() : "N/A"));
        
        if (tvTitle != null) tvTitle.setText(report.getTitle() != null ? report.getTitle() : "No Title");
        if (tvCategory != null) tvCategory.setText("Category: " + (report.getCategory() != null ? report.getCategory() : "N/A"));
        if (tvDescription != null) tvDescription.setText(report.getDescription() != null ? report.getDescription() : "No Description");
        if (tvRelatedId != null) tvRelatedId.setText("Related Report ID: " + (report.getRelatedId() != null ? report.getRelatedId() : "None"));
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        if (tvDate != null) tvDate.setText("Submitted: " + sdf.format(new Date(report.getTimestamp())));
        if (tvPriority != null) tvPriority.setText("Priority: " + (report.getPriority() != null ? report.getPriority() : "N/A"));
        if (tvStatus != null) tvStatus.setText("Current Status: " + (report.getStatus() != null ? report.getStatus() : "N/A"));
        
        if (actvUpdateStatus != null) actvUpdateStatus.setText(report.getStatus(), false);
        if (etAdminNote != null) etAdminNote.setText(report.getAdminNote());

        if (report.getImageUrl() != null && !report.getImageUrl().isEmpty()) {
            if (cardEvidence != null) cardEvidence.setVisibility(View.VISIBLE);
            if (tvNoEvidence != null) tvNoEvidence.setVisibility(View.GONE);
            if (ivEvidence != null) {
                Glide.with(this)
                        .load(report.getImageUrl())
                        .placeholder(R.drawable.ic_package)
                        .error(R.drawable.ic_error_outline)
                        .into(ivEvidence);
            }
        } else {
            if (cardEvidence != null) cardEvidence.setVisibility(View.GONE);
            if (tvNoEvidence != null) tvNoEvidence.setVisibility(View.VISIBLE);
        }
    }

    private void updateReport() {
        String newStatus = actvUpdateStatus.getText().toString();
        String adminNote = etAdminNote.getText().toString().trim();

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        mDatabase.child("AdminReports").child(reportId).child("status").setValue(newStatus);
        mDatabase.child("AdminReports").child(reportId).child("adminNote").setValue(adminNote);
        mDatabase.child("AdminReports").child(reportId).child("updatedAt").setValue(System.currentTimeMillis())
                .addOnSuccessListener(aVoid -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Report updated successfully", Toast.LENGTH_SHORT).show();
                    
                    // Notify user - Ensure we use University ID if available
                    String recipientId = currentReport.getUniversityId();
                    if (recipientId != null) {
                        sendNotificationToUser(recipientId, newStatus);
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendNotificationToUser(String userId, String status) {
        if ("Pending".equalsIgnoreCase(status)) return;

        String message = "Reviewed".equalsIgnoreCase(status) ? 
                getString(R.string.msg_report_reviewed) : getString(R.string.msg_report_resolved);
        
        String notificationId = mDatabase.child("Notifications").child(userId).push().getKey();
        if (notificationId != null) {
            Notification notification = new Notification(
                    notificationId,
                    userId,
                    "admin",
                    "Admin",
                    "",
                    "",
                    currentReport.getReportId(),
                    currentReport.getTitle(),
                    message + " (ID: " + currentReport.getDisplayId() + ")",
                    System.currentTimeMillis(),
                    "admin_report",
                    ""
            );
            mDatabase.child("Notifications").child(userId).child(notificationId).setValue(notification);
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Report")
                .setMessage("Are you sure you want to delete this report permanently?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
                    mDatabase.child("AdminReports").child(reportId).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                Toast.makeText(AdminReportDetailsActivity.this, "Report deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                if (progressBar != null) progressBar.setVisibility(View.GONE);
                                Toast.makeText(AdminReportDetailsActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
