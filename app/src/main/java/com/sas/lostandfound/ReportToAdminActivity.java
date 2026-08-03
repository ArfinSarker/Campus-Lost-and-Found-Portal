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
import android.graphics.Rect;
import android.view.ViewTreeObserver;

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
    private TextInputEditText etReportTitle, etReportDescription, etRelatedId, etReporterName, etUniversityId, etReporterPhone, etReporterEmail;
    private AutoCompleteTextView actvReportCategory, actvCountryCode;
    private TextInputLayout tilReportTitle, tilReportDescription, tilReporterName, tilReporterPhone, tilCountryCode, tilReporterEmail;
    private MaterialButton btnSubmit;
    private com.airbnb.lottie.LottieAnimationView loadingAnimation;
    private MaterialCardView uploadScreenshotCard;
    private ImageView ivScreenshot;
    private TextView tvScreenshotStatus;
    private View layoutUploadEmpty, layoutUploadSelected;
    private TextView tvUploadStatusSubtext;
    private android.widget.ImageButton btnDeleteImage;
    private Toolbar toolbar;
    private View keyboardSpacer;
    private View reportToAdminRoot;

    private List<Uri> selectedImageUris = new ArrayList<>();
    private String currentUniversityId;
    private String currentAuthId;
    private String currentProfileImageUrl = "";
    private static final int PICK_IMAGES_REQUEST = 1;

    private String contactNameState = "";
    private String contactPhoneState = "";
    private String contactEmailState = "";

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
        setupTextWatchers();
        setupKeyboardListener();
        fetchUserData();

        uploadScreenshotCard.setOnClickListener(v -> openGallery());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
        if (btnDeleteImage != null) {
            btnDeleteImage.setOnClickListener(v -> {
                selectedImageUris.clear();
                updateUploadUI();
            });
        }
    }

    private void setupTextWatchers() {
        etReporterName.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { contactNameState = s.toString().trim(); }
        });
        etReporterPhone.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String selectedCountryCode = actvCountryCode.getText().toString().trim();
                String code = ValidationUtils.extractCountryCode(selectedCountryCode);
                contactPhoneState = code + s.toString().trim();
            }
        });
        if (etReporterEmail != null) {
            etReporterEmail.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(android.text.Editable s) { contactEmailState = s.toString().trim(); }
            });
        }
    }

    private void initializeViews() {
        etReportTitle = findViewById(R.id.etReportTitle);
        etReportDescription = findViewById(R.id.etReportDescription);
        etRelatedId = findViewById(R.id.etRelatedId);
        etReporterName = findViewById(R.id.etReporterName);
        etUniversityId = findViewById(R.id.etUniversityId);
        etReporterPhone = findViewById(R.id.etReporterPhone);
        actvReportCategory = findViewById(R.id.actvReportCategory);

        tilReportTitle = findViewById(R.id.tilReportTitle);
        tilReportDescription = findViewById(R.id.tilReportDescription);
        tilReporterName = findViewById(R.id.tilReporterName);
        tilReporterPhone = findViewById(R.id.tilReporterPhone);
        tilCountryCode = findViewById(R.id.tilCountryCode);
        actvCountryCode = findViewById(R.id.actvCountryCode);
        etReporterEmail = findViewById(R.id.etReporterEmail);
        tilReporterEmail = findViewById(R.id.tilReporterEmail);
        btnSubmit = findViewById(R.id.btnSubmitReportAdmin);
        loadingAnimation = findViewById(R.id.loadingAnimation);
        uploadScreenshotCard = findViewById(R.id.uploadScreenshotCard);
        ivScreenshot = findViewById(R.id.ivScreenshot);
        tvScreenshotStatus = findViewById(R.id.tvScreenshotStatus);
        layoutUploadEmpty = findViewById(R.id.layoutUploadEmpty);
        layoutUploadSelected = findViewById(R.id.layoutUploadSelected);
        tvUploadStatusSubtext = findViewById(R.id.tvUploadStatusSubtext);
        btnDeleteImage = findViewById(R.id.btnDeleteImage);
        toolbar = findViewById(R.id.toolbar);
        reportToAdminRoot = findViewById(R.id.reportToAdminRoot);
        keyboardSpacer = findViewById(R.id.keyboardSpacer);

        ErrorHelper.attachToTextInputLayout(tilReportTitle);
        ErrorHelper.attachToTextInputLayout(tilReportDescription);
        ErrorHelper.attachToTextInputLayout(tilReporterName);
        ErrorHelper.attachToTextInputLayout(tilReporterPhone);
        ErrorHelper.attachToTextInputLayout(tilCountryCode);
        ErrorHelper.attachToTextInputLayout(tilReporterEmail);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> finish());

            // HeaderColorHelper setup to style the header dynamically/consistently
            com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
            if (appBarLayout != null) {
                int headerColor = androidx.core.content.ContextCompat.getColor(this, R.color.report_admin_header_bg);
                boolean isNight = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                HeaderColorHelper.setup(this, appBarLayout, headerColor, headerColor, !isNight);
            }
        }
    }

    private void setupDropdowns() {
        actvCountryCode.setFocusable(false);
        actvCountryCode.setClickable(true);
        actvCountryCode.setInputType(android.text.InputType.TYPE_NULL);
        actvCountryCode.setText(ValidationUtils.getCountryDisplayString("+880"), false);
        actvCountryCode.setOnClickListener(v -> CountryPickerDialog.show(this, country -> {
            actvCountryCode.setText(country.getFlagEmoji() + " " + country.getCode(), false);
            String phoneBody = etReporterPhone.getText().toString().trim();
            contactPhoneState = country.getCode() + phoneBody;
        }));

        String[] categories = {"Fake Report", "Spam / Misuse", "Harassment / Abuse", "Wrong Information", "Bug / Technical Issue", "Lost Item Issue", "Found Item Issue", "Other"};
        actvReportCategory.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item_report_admin, categories));


    }

    private void fetchUserData() {
        if (currentUniversityId == null) return;

        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + currentUniversityId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users != null && !users.isEmpty()) {
                    User user = users.get(0);
                    if (user != null) {
                        currentProfileImageUrl = user.getProfileImageUrl();
                        etReporterName.setText(user.getName());
                        contactNameState = user.getName() != null ? user.getName() : "";
                        etUniversityId.setText(user.getUniversityId());
                        
                        String fullPhone = user.getPhone();
                        String[] parsedPhone = ValidationUtils.parsePhoneNumber(fullPhone);
                        String code = parsedPhone[0];
                        String body = parsedPhone[1];
                        
                        if (actvCountryCode != null) {
                            actvCountryCode.setText(ValidationUtils.getCountryDisplayString(code), false);
                        }
                        etReporterPhone.setText(body);
                        contactPhoneState = code + body;
                        if (etReporterEmail != null) {
                            etReporterEmail.setText(user.getEmail());
                            contactEmailState = user.getEmail() != null ? user.getEmail() : "";
                        }
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

            updateUploadUI();
        }
    }

    private void updateUploadUI() {
        if (selectedImageUris != null && !selectedImageUris.isEmpty()) {
            if (layoutUploadEmpty != null) layoutUploadEmpty.setVisibility(View.GONE);
            if (layoutUploadSelected != null) layoutUploadSelected.setVisibility(View.VISIBLE);
            
            if (ivScreenshot != null) {
                ivScreenshot.setImageURI(selectedImageUris.get(0));
                ivScreenshot.clearColorFilter();
            }
            if (tvScreenshotStatus != null) {
                String status = selectedImageUris.size() + " Screenshot" + (selectedImageUris.size() > 1 ? "s" : "") + " Selected";
                tvScreenshotStatus.setText(status);
            }
            if (tvUploadStatusSubtext != null) {
                tvUploadStatusSubtext.setText("Tap card to change selection");
            }
        } else {
            if (layoutUploadEmpty != null) layoutUploadEmpty.setVisibility(View.VISIBLE);
            if (layoutUploadSelected != null) layoutUploadSelected.setVisibility(View.GONE);
            
            if (ivScreenshot != null) {
                ivScreenshot.setImageURI(null);
            }
        }
    }

    private void validateAndSubmit() {
        String title = etReportTitle.getText().toString().trim();
        String category = actvReportCategory.getText().toString();
        String description = etReportDescription.getText().toString().trim();
        String relatedId = etRelatedId.getText().toString().trim();
        String reporterName = contactNameState.trim();
        String universityId = etUniversityId.getText().toString().trim();
        String selectedCountryCode = actvCountryCode.getText().toString().trim();
        String code = ValidationUtils.extractCountryCode(selectedCountryCode);
        String phoneBody = etReporterPhone.getText().toString().trim();
        String phone = contactPhoneState.trim();
        String contactEmail = contactEmailState.trim();

        if (TextUtils.isEmpty(title)) { ErrorHelper.setFieldError(tilReportTitle, "Report title is required"); return; }
        if (TextUtils.isEmpty(description)) { ErrorHelper.setFieldError(tilReportDescription, "Report description is required"); return; }
        if (TextUtils.isEmpty(reporterName)) { ErrorHelper.setFieldError(tilReporterName, "Reporter name is required"); return; }
        if (TextUtils.isEmpty(universityId)) {
            ErrorHelper.showError(btnSubmit, "University ID is missing. Please log in again.");
            return;
        }

        if (!phoneBody.isEmpty() && !ValidationUtils.isValidPhone(code, phoneBody)) {
            ErrorHelper.setFieldError(tilReporterPhone, "Please enter a valid phone number");
            return;
        }

        if (TextUtils.isEmpty(contactEmail)) { ErrorHelper.setFieldError(tilReporterEmail, "Contact email is required"); return; }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(contactEmail).matches()) { ErrorHelper.setFieldError(tilReporterEmail, "Please enter a valid email address"); return; }

        setLoadingState(true);

        generateDisplayIdAndSubmit(title, category, description, relatedId, reporterName, universityId, phone, contactEmail);
    }

    private void generateDisplayIdAndSubmit(String title, String category, String description, String relatedId, String reporterName, String universityId, String phone, String email) {
        Map<String, Object> params = new HashMap<>();
        params.put("p_counter_name", "admin_reports");
        
        SupabaseDatabaseHelper.rpc("increment_counter", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    long newCount = Long.parseLong(result.trim());
                    String displayId = "R" + newCount;
                    uploadImagesAndReport(displayId, title, category, description, relatedId, reporterName, universityId, phone, email);
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

    private void uploadImagesAndReport(String displayId, String title, String category, String description, String relatedId, String reporterName, String universityId, String phone, String email) {
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
                            submitReport(displayId, title, category, description, relatedId, reporterName, universityId, phone, email, imageUrlStrings);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (remaining.decrementAndGet() == 0) {
                            submitReport(displayId, title, category, description, relatedId, reporterName, universityId, phone, email, imageUrlStrings);
                        }
                    }
                });
            }
        } else {
            submitReport(displayId, title, category, description, relatedId, reporterName, universityId, phone, email, new ArrayList<>());
        }
    }

    private void submitReport(String displayId, String title, String category, String description, String relatedId, String reporterName, String universityId, String phone, String email, List<String> imageUrls) {
        String reportId = UUID.randomUUID().toString();
        String firstImage = (imageUrls != null && !imageUrls.isEmpty()) ? imageUrls.get(0) : null;
        AdminReport report = new AdminReport(
                reportId, displayId, title, category, description, relatedId, reporterName, universityId, currentAuthId, phone, firstImage, "Pending", System.currentTimeMillis()
        );
        report.setEmail(email);
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
                
                String reporterImageUrl = currentProfileImageUrl;
                if (reporterImageUrl == null || reporterImageUrl.isEmpty()) {
                    android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
                    reporterImageUrl = prefs.getString("cachedProfileImageUrl", "");
                }
                
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
                        reporterImageUrl, // Image
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
            int loadingBg = androidx.core.content.ContextCompat.getColor(this, R.color.report_submit_loading_bg);
            int loadingStroke = androidx.core.content.ContextCompat.getColor(this, R.color.report_submit_loading_stroke);
            btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(loadingBg));
            btnSubmit.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
            btnSubmit.setStrokeColor(android.content.res.ColorStateList.valueOf(loadingStroke));
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

    private void setupKeyboardListener() {
        if (reportToAdminRoot == null || keyboardSpacer == null) return;

        reportToAdminRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                reportToAdminRoot.getWindowVisibleDisplayFrame(r);
                int screenHeight = reportToAdminRoot.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    if (keyboardSpacer.getVisibility() != View.VISIBLE) {
                        keyboardSpacer.setVisibility(View.VISIBLE);
                        keyboardSpacer.getLayoutParams().height = (int) (320 * getResources().getDisplayMetrics().density);
                        keyboardSpacer.requestLayout();
                    }
                } else {
                    if (keyboardSpacer.getVisibility() != View.GONE) {
                        keyboardSpacer.setVisibility(View.GONE);
                    }
                }
            }
        });
    }
}
