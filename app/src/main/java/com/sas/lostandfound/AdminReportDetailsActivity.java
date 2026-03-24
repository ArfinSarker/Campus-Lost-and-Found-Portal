package com.sas.lostandfound;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
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
        actvUpdateStatus.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, statuses));
    }

    private void fetchReportDetails() {
        mDatabase.child("AdminReports").child(reportId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentReport = snapshot.getValue(AdminReport.class);
                if (currentReport != null) {
                    displayReportDetails(currentReport);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayReportDetails(AdminReport report) {
        tvHeaderId.setText("Report Details: " + (report.getDisplayId() != null ? report.getDisplayId() : ""));
        tvReporterName.setText("Name: " + report.getReporterName());
        tvUniversityId.setText("University ID: " + report.getUniversityId());
        tvReporterPhone.setText("Phone: " + (report.getContactPhone() != null ? report.getContactPhone() : "N/A"));
        
        tvTitle.setText(report.getTitle());
        tvCategory.setText("Category: " + report.getCategory());
        tvDescription.setText(report.getDescription());
        tvRelatedId.setText("Related Report ID: " + (report.getRelatedReportId() != null ? report.getRelatedReportId() : "None"));
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
        tvDate.setText("Submitted: " + sdf.format(new Date(report.getCreatedAt())));
        tvPriority.setText("Priority: " + report.getPriority());
        tvStatus.setText("Status: " + report.getStatus());
        
        actvUpdateStatus.setText(report.getStatus(), false);
        etAdminNote.setText(report.getAdminNote());

        if (report.getImageUrl() != null && !report.getImageUrl().isEmpty()) {
            cardEvidence.setVisibility(View.VISIBLE);
            tvNoEvidence.setVisibility(View.GONE);
            Glide.with(this).load(report.getImageUrl()).into(ivEvidence);
        } else {
            cardEvidence.setVisibility(View.GONE);
            tvNoEvidence.setVisibility(View.VISIBLE);
        }
    }

    private void updateReport() {
        String newStatus = actvUpdateStatus.getText().toString();
        String adminNote = etAdminNote.getText().toString().trim();

        mDatabase.child("AdminReports").child(reportId).child("status").setValue(newStatus);
        mDatabase.child("AdminReports").child(reportId).child("adminNote").setValue(adminNote);
        mDatabase.child("AdminReports").child(reportId).child("updatedAt").setValue(System.currentTimeMillis());

        // Notify user - Ensure we use University ID if available
        String recipientId = currentReport.getUniversityId();
        if (recipientId != null) {
            sendNotificationToUser(recipientId, newStatus);
        }

        Toast.makeText(this, "Report updated successfully", Toast.LENGTH_SHORT).show();
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
                    mDatabase.child("AdminReports").child(reportId).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(AdminReportDetailsActivity.this, "Report deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
