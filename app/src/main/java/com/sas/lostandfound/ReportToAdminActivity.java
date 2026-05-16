package com.sas.lostandfound;

import android.content.ClipData;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ReportToAdminActivity extends AppCompatActivity {

    private static final String TAG = "ReportToAdmin";
    private TextInputEditText etReportTitle, etReportDescription, etRelatedId, etReporterName, etUniversityId, etReporterPhone;
    private AutoCompleteTextView actvReportCategory, actvPriority;
    private TextInputLayout tilReportTitle, tilReportDescription, tilReporterName;
    private MaterialButton btnSubmit;
    private com.airbnb.lottie.LottieAnimationView loadingAnimation;
    private MaterialCardView uploadScreenshotCard;
    private ImageView ivScreenshot;
    private TextView tvScreenshotStatus;
    private Toolbar toolbar;

    private List<Uri> selectedImageUris = new ArrayList<>();
    private String currentUniversityId;
    private String currentAuthId;
    private static final int PICK_IMAGES_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_to_admin);

        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUniversityId = prefs.getString("universityId", null);
        currentAuthId = prefs.getString("authId", null);

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
        loadingAnimation = findViewById(R.id.loadingAnimation);
        uploadScreenshotCard = findViewById(R.id.uploadScreenshotCard);
        ivScreenshot = findViewById(R.id.ivScreenshot);
        tvScreenshotStatus = findViewById(R.id.tvScreenshotStatus);
        toolbar = findViewById(R.id.toolbar);

        ErrorHelper.attachToTextInputLayout(tilReportTitle);
        ErrorHelper.attachToTextInputLayout(tilReportDescription);
        ErrorHelper.attachToTextInputLayout(tilReporterName);
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

    private void setupDropdowns() {
        String[] categories = {"Fake Report", "Spam / Misuse", "Harassment / Abuse", "Wrong Information", "Bug / Technical Issue", "Lost Item Issue", "Found Item Issue", "Other"};
        actvReportCategory.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, categories));

        String[] priorities = {"Low", "Medium", "High"};
        actvPriority.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, priorities));
        actvPriority.setText("Medium", false);
    }

    private void fetchUserData() {
        if (currentUniversityId == null) return;

        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + currentUniversityId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users != null && !users.isEmpty()) {
                    User user = users.get(0);
                    if (user != null) {
                        etReporterName.setText(user.getName());
                        etUniversityId.setText(user.getUniversityId());
                        etReporterPhone.setText(user.getPhone());
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {}
        });
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Screenshot(s)"), PICK_IMAGES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGES_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUris.clear();
            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    selectedImageUris.add(clipData.getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                selectedImageUris.add(data.getData());
            }

            if (!selectedImageUris.isEmpty()) {
                ivScreenshot.setImageURI(selectedImageUris.get(0));
                tvScreenshotStatus.setText(selectedImageUris.size() + " Screenshot(s) Selected");
            }
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

        if (TextUtils.isEmpty(title)) { ErrorHelper.setFieldError(tilReportTitle, "Report title is required"); return; }
        if (TextUtils.isEmpty(description)) { ErrorHelper.setFieldError(tilReportDescription, "Report description is required"); return; }
        if (TextUtils.isEmpty(reporterName)) { ErrorHelper.setFieldError(tilReporterName, "Reporter name is required"); return; }
        if (TextUtils.isEmpty(universityId)) {
            ErrorHelper.showError(btnSubmit, "University ID is missing. Please log in again.");
            return;
        }

        if (phone != null && !phone.isEmpty() && !ValidationUtils.isValidPhone(phone)) {
            ErrorHelper.setFieldError(etReporterPhone.getParent() instanceof com.google.android.material.textfield.TextInputLayout ? (com.google.android.material.textfield.TextInputLayout) etReporterPhone.getParent() : null, "Please enter a valid phone number (10-14 digits)");
            return;
        }

        setLoadingState(true);

        generateDisplayIdAndSubmit(title, category, description, relatedId, reporterName, universityId, phone, priority);
    }

    private void generateDisplayIdAndSubmit(String title, String category, String description, String relatedId, String reporterName, String universityId, String phone, String priority) {
        Map<String, Object> params = new HashMap<>();
        params.put("p_counter_name", "admin_reports");
        
        SupabaseDatabaseHelper.rpc("increment_counter", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    long newCount = Long.parseLong(result.trim());
                    String displayId = "R" + newCount;
                    uploadImagesAndReport(displayId, title, category, description, relatedId, reporterName, universityId, phone, priority);
                } catch (Exception e) {
                    resetButton();
                    ErrorHelper.showError(btnSubmit, "Failed to parse Report ID.");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                resetButton();
                ErrorHelper.showError(btnSubmit, "Failed to generate Report ID: " + errorMessage);
            }
        });
    }

    private void uploadImagesAndReport(String displayId, String title, String category, String description, String relatedId, String reporterName, String universityId, String phone, String priority) {
        if (!selectedImageUris.isEmpty()) {
            List<String> imageUrlStrings = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger remaining = new AtomicInteger(selectedImageUris.size());

            for (int i = 0; i < selectedImageUris.size(); i++) {
                String fileName = "admin_report_" + System.currentTimeMillis() + "_" + i + ".jpg";
                SupabaseStorageHelper.uploadImage(this, selectedImageUris.get(i), "admin_reports", fileName, new SupabaseStorageHelper.UploadCallback() {
                    @Override
                    public void onSuccess(String publicUrl) {
                        imageUrlStrings.add(publicUrl);
                        if (remaining.decrementAndGet() == 0) {
                            submitReport(displayId, title, category, description, relatedId, reporterName, universityId, phone, imageUrlStrings, priority);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (remaining.decrementAndGet() == 0) {
                            submitReport(displayId, title, category, description, relatedId, reporterName, universityId, phone, imageUrlStrings, priority);
                        }
                    }
                });
            }
        } else {
            submitReport(displayId, title, category, description, relatedId, reporterName, universityId, phone, new ArrayList<>(), priority);
        }
    }

    private void submitReport(String displayId, String title, String category, String description, String relatedId, String reporterName, String universityId, String phone, List<String> imageUrls, String priority) {
        String reportId = UUID.randomUUID().toString();
        String firstImage = (imageUrls != null && !imageUrls.isEmpty()) ? imageUrls.get(0) : null;
        AdminReport report = new AdminReport(
                reportId, displayId, title, category, description, relatedId, reporterName, universityId, currentAuthId, phone, firstImage, priority, "Pending", System.currentTimeMillis()
        );
        report.setImageUrls(imageUrls != null ? imageUrls : new ArrayList<>());
        report.setUpdatedAt(System.currentTimeMillis());

        SupabaseDatabaseHelper.insert("admin_reports", report, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                setLoadingState(false);
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, getString(R.string.report_submitted_success));
                
                // Notify all admins about this new report
                notifyAdmins(report);
                
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                resetButton();
                ErrorHelper.showError(btnSubmit, "Error saving report: " + errorMessage);
            }
        });
    }

    private void notifyAdmins(AdminReport report) {
        Log.d(TAG, "Attempting to notify admins about report: " + report.getDisplayId());
        
        // Fetch all profiles with role 'admin'
        SupabaseDatabaseHelper.select("profiles", "role=eq.admin", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> admins) {
                if (admins == null || admins.isEmpty()) {
                    Log.w(TAG, "No admins found in the database to notify!");
                    return;
                }

                Log.d(TAG, "Found " + admins.size() + " admin(s) to notify.");
                String reporterName = report.getReporterName() != null ? report.getReporterName() : "A user";
                String message = String.format("\"%s\" has submitted a new report for review: \"%s\"", reporterName, report.getTitle());
                
                for (User admin : admins) {
                    Log.d(TAG, "Preparing notification for Admin: " + admin.getUniversityId() + " (AuthID: " + admin.getAuthId() + ")");

                    String notificationId = UUID.randomUUID().toString();
                    Notification notification = new Notification(
                        notificationId,
                        admin.getUniversityId(),
                        currentUniversityId != null ? currentUniversityId : "system",
                        reporterName,
                        report.getPhone(),
                        "", // Email
                        "", // Image
                        report.getId(),
                        report.getTitle(),
                        message,
                        System.currentTimeMillis(),
                        "admin_report_new",
                        ""
                    );
                    
                    // CRITICAL: Ensure the Auth ID is set so the admin can see it via RLS
                    if (admin.getAuthId() != null) {
                        notification.setUserId(admin.getAuthId());
                    }

                    SupabaseDatabaseHelper.insert("notifications", notification, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                        @Override public void onSuccess(String r) {
                            Log.d(TAG, "Notification successfully sent to admin: " + admin.getUniversityId());
                        }
                        @Override public void onFailure(String e) {
                            Log.e(TAG, "Failed to insert notification for admin " + admin.getUniversityId() + ": " + e);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to fetch admins from profiles table: " + errorMessage);
            }
        });
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnSubmit.setEnabled(false);
            btnSubmit.setText("");
            btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
            btnSubmit.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
            btnSubmit.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.primaryColor)));
            loadingAnimation.setVisibility(View.VISIBLE);
            loadingAnimation.playAnimation();
        } else {
            loadingAnimation.setVisibility(View.GONE);
            loadingAnimation.pauseAnimation();
            btnSubmit.setEnabled(true);
            btnSubmit.setText(R.string.btn_submit_report_admin);
            btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.primaryColor)));
            btnSubmit.setStrokeWidth(0);
        }
    }

    private void resetButton() {
        setLoadingState(false);
    }
}
