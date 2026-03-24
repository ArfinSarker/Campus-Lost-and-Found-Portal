package com.sas.lostandfound;

import android.content.Intent;
import android.net.Uri;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.UUID;

public class ReportToAdminActivity extends AppCompatActivity {

    private static final String TAG = "ReportToAdmin";
    private TextInputEditText etReportTitle, etReportDescription, etRelatedId, etReporterName, etUniversityId, etReporterPhone;
    private AutoCompleteTextView actvReportCategory, actvPriority;
    private TextInputLayout tilReportTitle, tilReportDescription, tilReporterName;
    private MaterialButton btnSubmit;
    private ProgressBar progressBar;
    private MaterialCardView uploadScreenshotCard;
    private ImageView ivScreenshot;
    private TextView tvScreenshotStatus;
    private Toolbar toolbar;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    private Uri screenshotUri;
    private String currentUniversityId;
    private String currentAuthId;
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_to_admin);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        if (mAuth.getCurrentUser() != null) {
            currentAuthId = mAuth.getCurrentUser().getUid();
        }

        initializeViews();
        setupToolbar();
        setupDropdowns();
        fetchUserData();

        uploadScreenshotCard.setOnClickListener(v -> openGallery());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void initializeViews() {
        etReportTitle = findViewById(R.id.etReportTitle);
        etReportDescription = findViewById(R.id.etReportDescription);
        etRelatedId = findViewById(R.id.etRelatedId);
        etReporterName = findViewById(R.id.etReporterName);
        etUniversityId = findViewById(R.id.etUniversityId);
        etReporterPhone = findViewById(R.id.etReporterPhone);
        actvReportCategory = findViewById(R.id.actvReportCategory);
        actvPriority = findViewById(R.id.actvPriority);
        tilReportTitle = findViewById(R.id.tilReportTitle);
        tilReportDescription = findViewById(R.id.tilReportDescription);
        tilReporterName = findViewById(R.id.tilReporterName);
        btnSubmit = findViewById(R.id.btnSubmitReportAdmin);
        progressBar = findViewById(R.id.progressBar);
        uploadScreenshotCard = findViewById(R.id.uploadScreenshotCard);
        ivScreenshot = findViewById(R.id.ivScreenshot);
        tvScreenshotStatus = findViewById(R.id.tvScreenshotStatus);
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

    private void setupDropdowns() {
        String[] categories = {"Fake Report", "Spam / Misuse", "Harassment / Abuse", "Wrong Information", "Bug / Technical Issue", "Lost Item Issue", "Found Item Issue", "Other"};
        actvReportCategory.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, categories));

        String[] priorities = {"Low", "Medium", "High"};
        actvPriority.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, priorities));
        actvPriority.setText("Medium", false);
    }

    private void fetchUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String authUid = mAuth.getCurrentUser().getUid();

        mDatabase.child("UIDToUniversityID").child(authUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentUniversityId = snapshot.getValue(String.class);
                    etUniversityId.setText(currentUniversityId);
                    
                    mDatabase.child("Users").child(currentUniversityId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            if (userSnapshot.exists()) {
                                User user = userSnapshot.getValue(User.class);
                                if (user != null) {
                                    etReporterName.setText(user.getName());
                                    etReporterPhone.setText(user.getPhone());
                                }
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                } else {
                    currentUniversityId = authUid;
                    etUniversityId.setText(currentUniversityId);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Screenshot"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            screenshotUri = data.getData();
            ivScreenshot.setImageURI(screenshotUri);
            tvScreenshotStatus.setText("Screenshot Selected");
        }
    }

    private void validateAndSubmit() {
        String title = etReportTitle.getText().toString().trim();
        String category = actvReportCategory.getText().toString();
        String description = etReportDescription.getText().toString().trim();
        String relatedId = etRelatedId.getText().toString().trim();
        String reporterName = etReporterName.getText().toString().trim();
        String universityId = etUniversityId.getText().toString().trim();
        String phone = etReporterPhone.getText().toString().trim();
        String priority = actvPriority.getText().toString();

        if (TextUtils.isEmpty(title)) {
            tilReportTitle.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(description)) {
            tilReportDescription.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(reporterName)) {
            tilReporterName.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(universityId)) {
            Toast.makeText(this, "University ID is missing. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        btnSubmit.setText(R.string.submitting_report);
        progressBar.setVisibility(View.VISIBLE);

        generateDisplayIdAndSubmit(title, category, description, relatedId, reporterName, universityId, phone, priority);
    }

    private void generateDisplayIdAndSubmit(String title, String category, String description, String relatedId, String reporterName, String universityId, String phone, String priority) {
        mDatabase.child("Counters").child("AdminReportCounter").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = 0;
                if (snapshot.exists()) {
                    Object val = snapshot.getValue();
                    if (val instanceof Long) count = (Long) val;
                    else if (val instanceof Integer) count = ((Integer) val).longValue();
                    else if (val != null) {
                        try { count = Long.parseLong(val.toString()); } catch (Exception e) { count = 0; }
                    }
                }
                count++;
                final String displayId = "R" + count;
                final long newCount = count;

                if (screenshotUri != null) {
                    uploadImageAndReport(displayId, newCount, title, category, description, relatedId, reporterName, universityId, phone, priority);
                } else {
                    submitReportToFirebase(displayId, newCount, title, category, description, relatedId, reporterName, universityId, phone, null, priority);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                resetButton();
                Toast.makeText(ReportToAdminActivity.this, "Error generating ID: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadImageAndReport(String displayId, long newCount, String title, String category, String description, String relatedId, String reporterName, String universityId, String phone, String priority) {
        String fileName = UUID.randomUUID().toString() + ".jpg";

        Log.d(TAG, "Starting image upload to Supabase...");

        SupabaseStorageHelper.uploadImage(this, screenshotUri, "AdminReports", fileName, new SupabaseStorageHelper.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                Log.d(TAG, "Upload successful: " + publicUrl);
                submitReportToFirebase(displayId, newCount, title, category, description, relatedId, reporterName, universityId, phone, publicUrl, priority);
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Upload failed: " + e.getMessage());
                showUploadFailedDialog(displayId, newCount, title, category, description, relatedId, reporterName, universityId, phone, priority, e.getMessage());
            }
        });
    }

    private void showUploadFailedDialog(String displayId, long newCount, String title, String category, String description, String relatedId, String reporterName, String universityId, String phone, String priority, String error) {
        progressBar.setVisibility(View.GONE);
        new AlertDialog.Builder(this)
                .setTitle("Image Upload Failed")
                .setMessage("Error: " + error + "\n\nWould you like to submit the report without an image?")
                .setPositiveButton("Submit Anyway", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    submitReportToFirebase(displayId, newCount, title, category, description, relatedId, reporterName, universityId, phone, null, priority);
                })
                .setNegativeButton("Retry", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);
                    uploadImageAndReport(displayId, newCount, title, category, description, relatedId, reporterName, universityId, phone, priority);
                })
                .setNeutralButton("Cancel", (dialog, which) -> resetButton())
                .setCancelable(false)
                .show();
    }

    private void submitReportToFirebase(String displayId, long newCount, String title, String category, String description, String relatedId, String reporterName, String universityId, String phone, String imageUrl, String priority) {
        String reportId = mDatabase.child("AdminReports").push().getKey();
        if (reportId == null) {
            resetButton();
            return;
        }

        if (currentAuthId == null && mAuth.getCurrentUser() != null) {
            currentAuthId = mAuth.getCurrentUser().getUid();
        }

        AdminReport report = new AdminReport(
                reportId, displayId, title, category, description, relatedId, reporterName, universityId, currentAuthId, phone, imageUrl, priority, "Pending", System.currentTimeMillis()
        );

        mDatabase.child("AdminReports").child(reportId).setValue(report)
                .addOnSuccessListener(aVoid -> {
                    mDatabase.child("Counters").child("AdminReportCounter").setValue(newCount);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.report_submitted_success, Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    resetButton();
                    Log.e(TAG, "Firebase Error: " + e.getMessage(), e);
                    Toast.makeText(this, "Error saving report: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void resetButton() {
        progressBar.setVisibility(View.GONE);
        btnSubmit.setEnabled(true);
        btnSubmit.setText(R.string.btn_submit_report_admin);
    }
}
