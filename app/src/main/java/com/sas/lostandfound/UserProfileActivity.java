package com.sas.lostandfound;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class UserProfileActivity extends AppCompatActivity {

    private ImageView ivProfilePicture;
    private FloatingActionButton fabChangePhoto;
    private TextInputLayout tilEmail, tilPhone, tilDepartment, tilGender, tilBatch, tilLevelTerm, tilSection,
            tilOldPassword, tilNewPassword, tilConfirmPassword, tilDesignation, tilFullName, tilUniversityId,
            tilCountryCode;
    private TextInputEditText etEmail, etPhone, etFullName, etUniversityId, etBatch, etOldPassword, etNewPassword,
            etConfirmPassword, etDesignation, etDepartment;
    private AutoCompleteTextView actvGender, actvLevelTerm, actvSection, actvCountryCode;
    private MaterialButton btnSaveChanges, btnConfirmPasswordChange, btnDeleteUser;
    private com.airbnb.lottie.LottieAnimationView loadingAnimation;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private TextView tvHeaderTitle, tvLostReportsCount, tvFoundReportsCount, tvResolvedItemsCount;
    private View changePasswordSection, activitySection;
    private SwipeRefreshLayout swipeRefreshLayout;

    private static final int REQUEST_IMAGES_PICK = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int CAMERA_PERMISSION_CODE = 100;

    private final List<Uri> profileImageUris = new ArrayList<>();
    private Uri cameraImageUri;
    private String currentPhotoPath;
    private String currentUniversityId;
    private String userEmail;

    private User originalUser;
    private boolean isDataLoaded = false;
    private boolean isProfilePictureRemoved = false;
    private boolean isAdminViewing = false;
    private boolean isViewOnly = false;
    private boolean fromDrawer = false;
    private String targetUserId;
    private View keyboardSpacer;
    private View userProfileRoot;

    // Real-time listeners were removed in favor of Supabase REST calls
    // private ValueEventListener lostListener, foundListener, itemsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        isAdminViewing = getIntent().getBooleanExtra("isAdminViewing", false);
        isViewOnly = getIntent().getBooleanExtra("isViewOnly", false);
        targetUserId = getIntent().getStringExtra("targetUserId");
        fromDrawer = getIntent().getBooleanExtra("fromDrawer", false);

        initializeViews();
        setupToolbar();
        setupSwipeRefresh();
        setupKeyboardListener();

        // Initial Role-based UI setup from SharedPreferences for immediate response
        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        String savedUserType = prefs.getString("userType", "Student");
        if (!isAdminViewing && !isViewOnly) {
            ProfileRoleHelper.applyRoleVisibility(savedUserType, tilDesignation, tilBatch, tilLevelTerm, tilDepartment,
                    tilSection);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        // Critical: Initialize mode immediately to prevent flickering of edit icons
        if ((isAdminViewing || isViewOnly) && targetUserId != null) {
            currentUniversityId = targetUserId;
            setupAdminView();

            // Instantly render basic details from Intent to prevent lag/empty screen
            String intentName = getIntent().getStringExtra("intentFullName");
            String intentUserType = getIntent().getStringExtra("intentUserType");
            String intentProfileUrl = getIntent().getStringExtra("intentProfileImageUrl");

            if (intentName != null) {
                etFullName.setText(intentName);
                tvHeaderTitle.setText(intentName + "'s Profile");
            }
            if (currentUniversityId != null) {
                etUniversityId.setText(currentUniversityId);
            }
            if (intentUserType != null) {
                ProfileRoleHelper.applyRoleVisibility(intentUserType, tilDesignation, tilBatch, tilLevelTerm,
                        tilDepartment, tilSection);
            }
            if (intentProfileUrl != null && !intentProfileUrl.isEmpty()) {
                GlideApp.with(this)
                        .load(intentProfileUrl)
                        .placeholder(R.drawable.ic_default_avatar)
                        .thumbnail(0.1f)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .into(ivProfilePicture);
            } else {
                ivProfilePicture.setImageResource(R.drawable.ic_default_avatar);
            }
        } else {
            loadCachedUserData();
        }
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        // Heavy work deferred until after the activity transition animation is complete
        // Critical work started immediately, non-blocking UI setup deferred slightly
        setupDropdowns();

        if ((isAdminViewing || isViewOnly) && targetUserId != null) {
            currentUniversityId = targetUserId;
            loadUserData(targetUserId);
            setupParallelActivityTracking(targetUserId);
        } else {
            android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
            currentUniversityId = prefs.getString("universityId", null);
            userEmail = prefs.getString("cachedUserEmail", null);

            if (currentUniversityId != null) {
                loadUserData(currentUniversityId);
                setupParallelActivityTracking(currentUniversityId);
            } else {
                searchUserByEmail();
            }

            fabChangePhoto.setOnClickListener(v -> showImageSourceDialog());

            setupEditableToggles();
            setupChangeDetection();
            btnSaveChanges.setOnClickListener(v -> saveAllChanges());
            setupPasswordChangeLogic();
        }
    }

    /**
     * Dedicated function to handle back navigation.
     * If the user came from the navigation drawer, it ensures the drawer is open
     * and the correct item is highlighted when returning to the dashboard.
     */
    private void handleBackNavigation() {
        if (isAdminViewing || isViewOnly) {
            finish();
            return;
        }

        Intent intent = new Intent(this, CampusDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (fromDrawer) {
            intent.putExtra("openDrawer", true);
            intent.putExtra("selectedItemId", R.id.nav_profile);
        }
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.material_shared_axis_z_pop_enter, R.anim.material_shared_axis_z_pop_exit);
    }

    private void setupAdminView() {
        // Initial title, will be updated in loadUserData
        tvHeaderTitle.setText(isViewOnly ? "Profile Information" : "User Profile");
        fabChangePhoto.setVisibility(View.GONE);
        if (changePasswordSection != null)
            changePasswordSection.setVisibility(View.GONE);

        if (isViewOnly) {
            if (activitySection != null)
                activitySection.setVisibility(View.GONE);
            if (btnDeleteUser != null)
                btnDeleteUser.setVisibility(View.GONE);
        } else {
            if (activitySection != null)
                activitySection.setVisibility(View.VISIBLE);
            if (btnDeleteUser != null) {
                btnDeleteUser.setVisibility(View.GONE);
                btnDeleteUser.setOnClickListener(v -> confirmDeleteUser());
            }

            // Setup click listeners for activity sections
            tvLostReportsCount.setOnClickListener(v -> openFilteredItemList("lost"));
            tvFoundReportsCount.setOnClickListener(v -> openFilteredItemList("found"));
            tvResolvedItemsCount.setOnClickListener(v -> openFilteredItemList("returned"));
        }

        // Disable editing for Admin viewing or View Only mode
        disableAllFields();
    }

    private void openFilteredItemList(String status) {
        Intent intent = new Intent(this, AllReportedItemsActivity.class);
        intent.putExtra("filterStatus", status);
        intent.putExtra("targetUserId", targetUserId);
        intent.putExtra("userName", originalUser != null ? originalUser.getName() : "");
        startActivity(intent);
    }

    private void disableAllFields() {
        etFullName.setEnabled(false);
        etEmail.setEnabled(false);
        etPhone.setEnabled(false);
        if (actvCountryCode != null)
            actvCountryCode.setEnabled(false);
        actvGender.setEnabled(false);
        etBatch.setEnabled(false);
        actvLevelTerm.setEnabled(false);
        etDepartment.setEnabled(false);
        actvSection.setEnabled(false);
        etDesignation.setEnabled(false);

        tilFullName.setEndIconMode(TextInputLayout.END_ICON_NONE);
        tilEmail.setEndIconMode(TextInputLayout.END_ICON_NONE);
        tilPhone.setEndIconMode(TextInputLayout.END_ICON_NONE);
        tilGender.setEndIconMode(TextInputLayout.END_ICON_NONE);
        tilBatch.setEndIconMode(TextInputLayout.END_ICON_NONE);
        tilLevelTerm.setEndIconMode(TextInputLayout.END_ICON_NONE);
        tilDepartment.setEndIconMode(TextInputLayout.END_ICON_NONE);
        tilSection.setEndIconMode(TextInputLayout.END_ICON_NONE);
        tilDesignation.setEndIconMode(TextInputLayout.END_ICON_NONE);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(() -> {
                if ((isAdminViewing || isViewOnly) && targetUserId != null) {
                    loadUserData(targetUserId);
                    setupParallelActivityTracking(targetUserId);
                } else {
                    if (currentUniversityId != null) {
                        loadUserData(currentUniversityId);
                        setupParallelActivityTracking(currentUniversityId);
                    } else {
                        searchUserByEmail();
                    }
                }
            });
        }
    }

    private void setupParallelActivityTracking(String userId) {
        // Reset counts for visual feedback
        tvLostReportsCount.setText("Lost Reports: ...");
        tvFoundReportsCount.setText("Found Reports: ...");
        tvResolvedItemsCount.setText("Resolved Items: ...");

        AtomicLong resolvedCount = new AtomicLong(0);

        // 1. Parallel Lost Reports Count
        SupabaseDatabaseHelper.select("lost_reports",
                "reporter_id=eq." + userId + "&deleted_by_user=eq.false&select=count",
                new TypeToken<List<Map<String, Object>>>() {
                }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Map<String, Object>>>() {
                    @Override
                    public void onSuccess(List<Map<String, Object>> result) {
                        if (result != null && !result.isEmpty()) {
                            Object countObj = result.get(0).get("count");
                            long count = (countObj instanceof Number) ? ((Number) countObj).longValue() : 0;
                            tvLostReportsCount.setText("Lost Reports: " + count);
                            tvLostReportsCount
                                    .setTextColor(ContextCompat.getColor(UserProfileActivity.this, R.color.errorColor));
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                    }
                });

        // 2. Parallel Found Reports Count
        SupabaseDatabaseHelper.select("found_reports",
                "reporter_id=eq." + userId + "&deleted_by_user=eq.false&select=count",
                new TypeToken<List<Map<String, Object>>>() {
                }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Map<String, Object>>>() {
                    @Override
                    public void onSuccess(List<Map<String, Object>> result) {
                        if (result != null && !result.isEmpty()) {
                            Object countObj = result.get(0).get("count");
                            long count = (countObj instanceof Number) ? ((Number) countObj).longValue() : 0;
                            tvFoundReportsCount.setText("Found Reports: " + count);
                            tvFoundReportsCount.setTextColor(ContextCompat.getColor(UserProfileActivity.this, R.color.success));
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                    }
                });

        // 3. Parallel Returned Items Count
        String q = "or=(claimed_by_id.eq." + userId + ",and(reporter_id.eq." + userId + ",admin_status.eq.Returned))";

        SupabaseDatabaseHelper.select("reports", q + "&deleted_by_user=eq.false&select=count",
                new TypeToken<List<Map<String, Object>>>() {
                }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Map<String, Object>>>() {
                    @Override
                    public void onSuccess(List<Map<String, Object>> result) {
                        if (result != null && !result.isEmpty()) {
                            Object countObj = result.get(0).get("count");
                            long count = (countObj instanceof Number) ? ((Number) countObj).longValue() : 0;
                            updateResolvedUI(count);
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                    }
                });
    }

    private interface StatusCallback {
        void onResult(boolean isReturned);
    }

    private void checkItemStatus(String itemId, StatusCallback callback) {
        // This method was used by itemsListener which is now replaced by a consolidated
        // count query in setupRealTimeActivityTracking
        callback.onResult(false);
    }

    private void updateResolvedUI(long count) {
        tvResolvedItemsCount.setText("Resolved Items: " + count);
        tvResolvedItemsCount.setTextColor(ContextCompat.getColor(this, R.color.primaryColor));
    }

    private void confirmDeleteUser() {
        new AlertDialog.Builder(this)
                .setTitle("Delete User")
                .setMessage("Are you sure you want to delete this user permanently? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteUserFromDatabase())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deleteUserFromDatabase() {
        showLoading(true);
        Map<String, Object> params = new HashMap<>();
        params.put("target_university_id", currentUniversityId);

        SupabaseDatabaseHelper.rpc("delete_user_by_admin", params,
                new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        showLoading(false);
                        SnackbarManager.show(SnackbarManager.Type.SUCCESS, "User deleted successfully");
                        finish();
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showLoading(false);
                        SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to delete user: " + errorMessage);
                    }
                });
    }

    private void setupPasswordChangeLogic() {
        btnConfirmPasswordChange.setOnClickListener(v -> {
            String oldPass = etOldPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (TextUtils.isEmpty(oldPass)) {
                ErrorHelper.setFieldError(tilOldPassword, "Old password required");
                return;
            }
            if (!ValidationUtils.isValidPassword(newPass)) {
                ErrorHelper.setFieldError(tilNewPassword, ValidationUtils.getPasswordRequirements());
                return;
            }
            if (!newPass.equals(confirmPass)) {
                ErrorHelper.setFieldError(tilConfirmPassword, "Passwords do not match");
                return;
            }

            showLoading(true);
            reauthenticateAndChangePassword(oldPass, newPass, () -> {
                SupabaseAuthHelper.updateUserPassword(oldPass, newPass, new SupabaseAuthHelper.AuthCallback() {
                    @Override
                    public void onSuccess(String userId, String accessToken, String refreshToken) {
                        Map<String, Object> passUpdate = new HashMap<>();
                        passUpdate.put("password", newPass);
                        SupabaseDatabaseHelper.update("profiles", "university_id=eq." + currentUniversityId, passUpdate,
                                new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                                    @Override
                                    public void onSuccess(String result) {
                                        showLoading(false);
                                        etOldPassword.setText("");
                                        etNewPassword.setText("");
                                        etConfirmPassword.setText("");
                                        ErrorHelper.clearFieldError(tilOldPassword);
                                        ErrorHelper.clearFieldError(tilNewPassword);
                                        ErrorHelper.clearFieldError(tilConfirmPassword);
                                        btnConfirmPasswordChange.setVisibility(View.GONE);
                                        SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Password updated successfully");
                                        
                                        // Update auth token if a new one was provided
                                        if (accessToken != null) {
                                            SupabaseDatabaseHelper.setAuthToken(accessToken);
                                        }
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        showLoading(false);
                                        SnackbarManager.show(SnackbarManager.Type.ERROR,
                                                "Database sync failed: " + errorMessage);
                                    }
                                });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showLoading(false);
                        ErrorHelper.showError(btnConfirmPasswordChange, "Auth update failed: " + errorMessage);
                    }
                });
            });
        });
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        fabChangePhoto = findViewById(R.id.fabChangePhoto);
        progressBar = findViewById(R.id.progressBar);

        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilCountryCode = findViewById(R.id.tilCountryCode);
        tilDepartment = findViewById(R.id.tilDepartment);
        tilGender = findViewById(R.id.tilGender);
        tilBatch = findViewById(R.id.tilBatch);
        tilLevelTerm = findViewById(R.id.tilLevelTerm);
        tilSection = findViewById(R.id.tilSection);
        tilOldPassword = findViewById(R.id.tilOldPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        tilDesignation = findViewById(R.id.tilDesignation);
        tilUniversityId = findViewById(R.id.tilUniversityId);

        ErrorHelper.attachToTextInputLayout(tilFullName);
        ErrorHelper.attachToTextInputLayout(tilEmail);
        ErrorHelper.attachToTextInputLayout(tilPhone);
        ErrorHelper.attachToTextInputLayout(tilCountryCode);
        ErrorHelper.attachToTextInputLayout(tilDepartment);
        ErrorHelper.attachToTextInputLayout(tilGender);
        ErrorHelper.attachToTextInputLayout(tilBatch);
        ErrorHelper.attachToTextInputLayout(tilLevelTerm);
        ErrorHelper.attachToTextInputLayout(tilSection);
        ErrorHelper.attachToTextInputLayout(tilOldPassword);
        ErrorHelper.attachToTextInputLayout(tilNewPassword);
        ErrorHelper.attachToTextInputLayout(tilConfirmPassword);
        ErrorHelper.attachToTextInputLayout(tilDesignation);

        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        actvCountryCode = findViewById(R.id.actvCountryCode);
        etFullName = findViewById(R.id.etFullName);
        etUniversityId = findViewById(R.id.etUniversityId);
        actvGender = findViewById(R.id.actvGender);
        etBatch = findViewById(R.id.etBatch);
        actvLevelTerm = findViewById(R.id.actvLevelTerm);
        etDepartment = findViewById(R.id.etDepartment);
        actvSection = findViewById(R.id.actvSection);
        etOldPassword = findViewById(R.id.etOldPassword);
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etDesignation = findViewById(R.id.etDesignation);

        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnConfirmPasswordChange = findViewById(R.id.btnConfirmPasswordChange);
        btnDeleteUser = findViewById(R.id.btnDeleteUser);
        loadingAnimation = findViewById(R.id.loadingAnimation);

        changePasswordSection = findViewById(R.id.llChangePasswordSection);
        activitySection = findViewById(R.id.llActivitySection);
        tvLostReportsCount = findViewById(R.id.tvLostReportsCount);
        tvFoundReportsCount = findViewById(R.id.tvFoundReportsCount);
        tvResolvedItemsCount = findViewById(R.id.tvReturnedItemsCount);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        // Ensure fields are initially non-focusable and hide end icons if admin viewing
        // to prevent flickering
        if (isAdminViewing || isViewOnly) {
            disableAllFields();
        } else {
            etFullName.setFocusable(false);
            etEmail.setFocusable(false);
            etPhone.setFocusable(false);
            actvCountryCode.setFocusable(false);
            actvCountryCode.setEnabled(false);
            actvGender.setFocusable(false);
            etBatch.setFocusable(false);
            actvLevelTerm.setFocusable(false);
            etDepartment.setFocusable(false);
            actvSection.setFocusable(false);
            etDesignation.setFocusable(false);
            etUniversityId.setFocusable(false);
        }

        userProfileRoot = findViewById(R.id.userProfileRoot);
        keyboardSpacer = findViewById(R.id.keyboardSpacer);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setTitle("");
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> handleBackNavigation());

            // HeaderColorHelper setup to style the header dynamically/consistently
            com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
            if (appBarLayout != null) {
                HeaderColorHelper.setup(this, appBarLayout, toolbar);
            }
        }
    }

    private void setupDropdowns() {
        actvCountryCode.setFocusable(false);
        actvCountryCode.setClickable(true);
        actvCountryCode.setInputType(android.text.InputType.TYPE_NULL);
        actvCountryCode.setOnClickListener(v -> {
            if (actvCountryCode.isEnabled()) {
                CountryPickerDialog.show(this, country -> {
                    actvCountryCode.setText(country.getFlagEmoji() + " " + country.getCode(), false);
                });
            }
        });

        String[] genders = { "Male", "Female" };
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, genders);
        actvGender.setAdapter(genderAdapter);
        actvGender.setOnClickListener(v -> {
            if (actvGender.isEnabled())
                actvGender.showDropDown();
        });

        String[] levels = {
                "Level 1 Term I", "Level 1 Term II",
                "Level 2 Term I", "Level 2 Term II",
                "Level 3 Term I", "Level 3 Term II",
                "Level 4 Term I", "Level 4 Term II"
        };
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, levels);
        actvLevelTerm.setAdapter(levelAdapter);
        actvLevelTerm.setOnClickListener(v -> {
            if (actvLevelTerm.isEnabled())
                actvLevelTerm.showDropDown();
        });

        String[] sections = { "A", "B", "C", "D" };
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, sections);
        actvSection.setAdapter(sectionAdapter);
        actvSection.setOnClickListener(v -> {
            if (actvSection.isEnabled())
                actvSection.showDropDown();
        });
    }

    private void setupEditableToggles() {
        setupToggle(tilFullName, etFullName);
        setupToggle(tilEmail, etEmail);
        setupTogglePhone();
        setupToggle(tilGender, actvGender);
        setupToggle(tilBatch, etBatch);
        setupToggle(tilLevelTerm, actvLevelTerm);
        setupToggle(tilDepartment, etDepartment);
        setupToggle(tilSection, actvSection);
        setupToggle(tilDesignation, etDesignation);
    }

    private void setupTogglePhone() {
        if (tilPhone == null || etPhone == null || actvCountryCode == null)
            return;
        tilPhone.setEndIconOnClickListener(v -> {
            boolean isEnabled = etPhone.isEnabled();
            boolean newState = !isEnabled;
            etPhone.setEnabled(newState);
            etPhone.setFocusable(newState);
            etPhone.setFocusableInTouchMode(newState);

            actvCountryCode.setEnabled(newState);

            if (newState) {
                etPhone.requestFocus();
                if (etPhone.getText() != null) {
                    etPhone.setSelection(etPhone.getText().length());
                }
            }
            checkForChanges();
        });
    }

    private void setupToggle(TextInputLayout til, View field) {
        if (til == null || field == null)
            return;
        til.setEndIconOnClickListener(v -> {
            boolean isEnabled = field.isEnabled();
            boolean newState = !isEnabled;
            field.setEnabled(newState);
            field.setFocusable(newState);
            field.setFocusableInTouchMode(newState);

            if (newState) {
                field.requestFocus();

                // Position cursor at the end of the text
                if (field instanceof android.widget.EditText) {
                    android.widget.EditText editText = (android.widget.EditText) field;
                    if (editText.getText() != null) {
                        editText.setSelection(editText.getText().length());
                    }
                }

                if (field instanceof AutoCompleteTextView) {
                    ((AutoCompleteTextView) field).showDropDown();
                }
            }
            checkForChanges();
        });
    }

    private void setupChangeDetection() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isDataLoaded)
                    checkForChanges();
            }
        };

        etFullName.addTextChangedListener(watcher);
        etEmail.addTextChangedListener(watcher);
        etPhone.addTextChangedListener(watcher);
        if (actvCountryCode != null)
            actvCountryCode.addTextChangedListener(watcher);
        actvGender.addTextChangedListener(watcher);
        etBatch.addTextChangedListener(watcher);
        actvLevelTerm.addTextChangedListener(watcher);
        etDepartment.addTextChangedListener(watcher);
        actvSection.addTextChangedListener(watcher);
        etDesignation.addTextChangedListener(watcher);

        TextWatcher passWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String oldPass = etOldPassword.getText().toString();
                String newPass = etNewPassword.getText().toString();
                String confirmPass = etConfirmPassword.getText().toString();
                btnConfirmPasswordChange.setVisibility(
                        (!oldPass.isEmpty() && !newPass.isEmpty() && !confirmPass.isEmpty()) ? View.VISIBLE
                                : View.GONE);
            }
        };
        etOldPassword.addTextChangedListener(passWatcher);
        etNewPassword.addTextChangedListener(passWatcher);
        etConfirmPassword.addTextChangedListener(passWatcher);
    }

    private void checkForChanges() {
        if (originalUser == null || (isAdminViewing && targetUserId != null))
            return;

        boolean changed = false;

        if (!etFullName.getText().toString().equals(originalUser.getName()))
            changed = true;
        if (!etEmail.getText().toString().equals(originalUser.getEmail()))
            changed = true;
        if (!getFormattedPhoneNumber().equals(originalUser.getPhone()))
            changed = true;

        String gender = actvGender.getText().toString();
        String originalGender = originalUser.getGender() != null ? originalUser.getGender() : "";
        if (!gender.equals(originalGender))
            changed = true;

        if ("Staff".equalsIgnoreCase(originalUser.getUserType())
                || "Admin".equalsIgnoreCase(originalUser.getUserType())) {
            String designation = etDesignation.getText().toString();
            String originalDesignation = originalUser.getDesignation() != null ? originalUser.getDesignation() : "";
            if (!designation.equals(originalDesignation))
                changed = true;

            String dept = etDepartment.getText().toString();
            String originalDept = originalUser.getDepartment() != null ? originalUser.getDepartment() : "Not Specified";
            if (!dept.equals(originalDept))
                changed = true;
        } else if ("Student".equalsIgnoreCase(originalUser.getUserType())) {
            if (!etBatch.getText().toString().equals(originalUser.getBatch()))
                changed = true;
            if (!actvLevelTerm.getText().toString().equals(originalUser.getLevelTerm()))
                changed = true;
            if (!actvSection.getText().toString().equals(originalUser.getSection()))
                changed = true;

            String dept = etDepartment.getText().toString();
            String originalDept = originalUser.getDepartment() != null ? originalUser.getDepartment() : "Not Specified";
            if (!dept.equals(originalDept))
                changed = true;
        }

        boolean imageChanged = false;
        boolean originalHasImage = originalUser.getProfileImageUrl() != null
                && !originalUser.getProfileImageUrl().isEmpty();
        if (isProfilePictureRemoved) {
            if (originalHasImage)
                imageChanged = true;
        } else if (!profileImageUris.isEmpty()) {
            imageChanged = true;
        }

        if (imageChanged)
            changed = true;

        btnSaveChanges.setVisibility(changed ? View.VISIBLE : View.GONE);
    }

    private void searchUserByEmail() {
        if (userEmail == null) {
            android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
            userEmail = prefs.getString("cachedUserEmail", null);
        }

        if (userEmail == null) {
            showLoading(false);
            if (swipeRefreshLayout != null)
                swipeRefreshLayout.setRefreshing(false);
            return;
        }

        SupabaseDatabaseHelper.select("profiles", "email=eq." + userEmail + "&select=university_id&limit=1",
                new TypeToken<List<Map<String, String>>>() {
                }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Map<String, String>>>() {
                    @Override
                    public void onSuccess(List<Map<String, String>> result) {
                        if (result != null && !result.isEmpty()) {
                            currentUniversityId = result.get(0).get("university_id");
                            if (currentUniversityId != null) {
                                loadUserData(currentUniversityId);
                                setupParallelActivityTracking(currentUniversityId);
                            }
                        } else {
                            showLoading(false);
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showLoading(false);
                    }
                });
    }

    private void loadUserData(String userId) {
        // Optimized: Fetch only required columns to reduce payload
        String columns = "university_id,full_name,email,phone_number,gender,user_type,department,batch,level_term,section,designation,profile_image_url";
        showLoading(true);
        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + userId + "&select=" + columns + "&limit=1",
                new TypeToken<List<User>>() {
                }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
                    @Override
                    public void onSuccess(List<User> users) {
                        showLoading(false);
                        if (users != null && !users.isEmpty()) {
                            originalUser = users.get(0);
                            if (originalUser != null) {
                                isDataLoaded = false;
                                String name = originalUser.getName();
                                if (name == null)
                                    name = originalUser.getFullName();

                                etFullName.setText(name);

                                if (isAdminViewing && targetUserId != null) {
                                    tvHeaderTitle.setText(name + "'s Profile");
                                }

                                etUniversityId.setText(originalUser.getUniversityId());
                                etEmail.setText(originalUser.getEmail());
                                userEmail = originalUser.getEmail();

                                String fullPhone = originalUser.getPhone();
                                String[] parsedPhone = ValidationUtils.parsePhoneNumber(fullPhone);
                                String code = parsedPhone[0];
                                String body = parsedPhone[1];

                                if (actvCountryCode != null) {
                                    actvCountryCode.setText(ValidationUtils.getCountryDisplayString(code), false);
                                }
                                etPhone.setText(body);

                                actvGender.setText(originalUser.getGender(), false);

                                ProfileRoleHelper.applyRoleVisibility(originalUser.getUserType(),
                                        tilDesignation, tilBatch, tilLevelTerm, tilDepartment, tilSection);

                                String dept = originalUser.getDepartment();
                                if (dept == null || dept.trim().isEmpty()) {
                                    dept = "Not Specified";
                                }

                                if ("Staff".equals(originalUser.getUserType())
                                        || "Admin".equalsIgnoreCase(originalUser.getUserType())) {
                                    etDesignation.setText(originalUser.getDesignation());
                                    etDepartment.setText(dept);
                                } else {
                                    etBatch.setText(originalUser.getBatch());
                                    actvLevelTerm.setText(originalUser.getLevelTerm(), false);
                                    etDepartment.setText(dept);
                                    actvSection.setText(originalUser.getSection(), false);
                                }

                                if (originalUser.getProfileImageUrl() != null
                                        && !originalUser.getProfileImageUrl().isEmpty()) {
                                    GlideApp.with(UserProfileActivity.this)
                                            .load(originalUser.getProfileImageUrl())
                                            .placeholder(R.drawable.ic_default_avatar)
                                            .thumbnail(0.1f)
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .circleCrop()
                                            .into(ivProfilePicture);
                                } else {
                                    ivProfilePicture.setImageResource(R.drawable.ic_default_avatar);
                                }

                                isProfilePictureRemoved = false;
                                isDataLoaded = true;

                                if ((isAdminViewing || isViewOnly) && targetUserId != null) {
                                    disableAllFields();
                                    if (isAdminViewing) {
                                        setupInteractiveContactFields();

                                        if (!isViewOnly && btnDeleteUser != null) {
                                            android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
                                            String loggedInUniversityId = prefs.getString("universityId", "");
                                            boolean loggedInIsMainAdmin = "0802410205101019".equals(loggedInUniversityId);

                                            String targetId = originalUser.getUniversityId();
                                            String targetType = originalUser.getUserType();

                                            boolean isTargetAdmin = "Admin".equalsIgnoreCase(targetType);
                                            boolean isTargetMainAdmin = "0802410205101019".equals(targetId);
                                            boolean isSelf = targetId != null && targetId.equals(loggedInUniversityId);

                                            if (loggedInIsMainAdmin) {
                                                btnDeleteUser.setVisibility(View.VISIBLE);
                                            } else {
                                                if (isTargetMainAdmin || isTargetAdmin || isSelf) {
                                                    btnDeleteUser.setVisibility(View.GONE);
                                                } else {
                                                    btnDeleteUser.setVisibility(View.VISIBLE);
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    android.content.SharedPreferences.Editor editor = getSharedPreferences("MyApp",
                                            MODE_PRIVATE).edit();
                                    editor.putString("cachedUserName", originalUser.getName());
                                    editor.putString("cachedUserEmail", originalUser.getEmail());
                                    editor.putString("cachedUserPhone", originalUser.getPhone());
                                    editor.putString("cachedUserGender", originalUser.getGender());
                                    editor.putString("cachedUserDepartment", originalUser.getDepartment());
                                    editor.putString("cachedUserBatch", originalUser.getBatch());
                                    editor.putString("cachedUserLevelTerm", originalUser.getLevelTerm());
                                    editor.putString("cachedUserSection", originalUser.getSection());
                                    editor.putString("cachedUserDesignation", originalUser.getDesignation());
                                    editor.putString("cachedProfileImageUrl", originalUser.getProfileImageUrl());
                                    editor.apply();
                                }
                            }
                        }
                        if (swipeRefreshLayout != null)
                            swipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        showLoading(false);
                        if (swipeRefreshLayout != null)
                            swipeRefreshLayout.setRefreshing(false);
                        SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to load data: " + errorMessage);
                    }
                });
    }

    private void setupInteractiveContactFields() {
        if (!isAdminViewing)
            return;

        // 1. Email field click action, long click action and styling
        final String email = etEmail.getText().toString().trim();
        if (!email.isEmpty()) {
            android.text.SpannableString emailSpannable = new android.text.SpannableString(email);
            emailSpannable.setSpan(new android.text.style.UnderlineSpan(), 0, email.length(), 0);
            emailSpannable.setSpan(new android.text.style.ForegroundColorSpan(
                    ContextCompat.getColor(this, R.color.primaryColor)), 0, email.length(), 0);

            etEmail.setEnabled(true);
            etEmail.setFocusable(false);
            etEmail.setFocusableInTouchMode(false);
            etEmail.setCursorVisible(false);
            etEmail.setText(emailSpannable);

            etEmail.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + email));
                try {
                    startActivity(Intent.createChooser(intent, "Send Email"));
                } catch (android.content.ActivityNotFoundException e) {
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "No email client found on this device");
                }
            });

            etEmail.setOnLongClickListener(v -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(
                        android.content.Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Email Address", email);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Email copied to clipboard");
                }
                return true; // Consumes the long press, preventing selection cursor handle "|"
            });

            tilEmail.setEndIconMode(TextInputLayout.END_ICON_NONE);
        }

        // 2. Phone field click action, long click action and styling
        String phoneBody = etPhone.getText().toString().trim();
        String tempPhone = originalUser != null ? originalUser.getPhone() : "";
        if (tempPhone == null || tempPhone.isEmpty()) {
            String code = actvCountryCode != null ? actvCountryCode.getText().toString().trim() : "";
            tempPhone = ValidationUtils.extractCountryCode(code) + phoneBody;
        }
        final String fullPhone = tempPhone;

        if (!phoneBody.isEmpty()) {
            android.text.SpannableString phoneSpannable = new android.text.SpannableString(phoneBody);
            phoneSpannable.setSpan(new android.text.style.UnderlineSpan(), 0, phoneBody.length(), 0);
            phoneSpannable.setSpan(new android.text.style.ForegroundColorSpan(
                    ContextCompat.getColor(this, R.color.primaryColor)), 0, phoneBody.length(), 0);

            etPhone.setEnabled(true);
            etPhone.setFocusable(false);
            etPhone.setFocusableInTouchMode(false);
            etPhone.setCursorVisible(false);
            etPhone.setText(phoneSpannable);

            etPhone.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + fullPhone));
                try {
                    startActivity(intent);
                } catch (android.content.ActivityNotFoundException e) {
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "No phone dialer found on this device");
                }
            });

            etPhone.setOnLongClickListener(v -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(
                        android.content.Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Phone Number", fullPhone);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Phone number copied to clipboard");
                }
                return true; // Consumes the long press, preventing selection cursor handle "|"
            });

            tilPhone.setEndIconMode(TextInputLayout.END_ICON_NONE);

            // Setup country code to match phone body interactive styling
            if (actvCountryCode != null) {
                String countryText = actvCountryCode.getText().toString().trim();
                if (!countryText.isEmpty()) {
                    android.text.SpannableString countrySpannable = new android.text.SpannableString(countryText);
                    countrySpannable.setSpan(new android.text.style.UnderlineSpan(), 0, countryText.length(), 0);
                    countrySpannable.setSpan(new android.text.style.ForegroundColorSpan(
                            ContextCompat.getColor(this, R.color.primaryColor)), 0, countryText.length(), 0);

                    if (tilCountryCode != null) {
                        tilCountryCode.setEnabled(true);
                    }
                    actvCountryCode.setEnabled(true);
                    actvCountryCode.setFocusable(false);
                    actvCountryCode.setFocusableInTouchMode(false);
                    actvCountryCode.setCursorVisible(false);
                    actvCountryCode.setText(countrySpannable, false);

                    actvCountryCode.setOnClickListener(v -> {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + fullPhone));
                        try {
                            startActivity(intent);
                        } catch (android.content.ActivityNotFoundException e) {
                            SnackbarManager.show(SnackbarManager.Type.ERROR, "No phone dialer found on this device");
                        }
                    });

                    actvCountryCode.setOnLongClickListener(v -> {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(
                                android.content.Context.CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Phone Number",
                                fullPhone);
                        if (clipboard != null) {
                            clipboard.setPrimaryClip(clip);
                            SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Phone number copied to clipboard");
                        }
                        return true; // Consumes the long press, preventing selection cursor handle "|"
                    });
                }
            }
        }

        // 3. University ID long click action (for Admin)
        final String universityIdVal = etUniversityId.getText().toString().trim();
        if (!universityIdVal.isEmpty()) {
            tilUniversityId.setEnabled(true);
            etUniversityId.setEnabled(true);
            etUniversityId.setFocusable(false);
            etUniversityId.setFocusableInTouchMode(false);
            etUniversityId.setCursorVisible(false);

            etUniversityId.setOnLongClickListener(v -> {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(
                        android.content.Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("University ID", universityIdVal);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    SnackbarManager.show(SnackbarManager.Type.SUCCESS, "University ID copied to clipboard");
                }
                return true; // Consumes the long press, preventing selection cursor handle "|"
            });
        }
    }

    private void saveAllChanges() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        String selectedCountryCode = actvCountryCode.getText().toString().trim();
        String code = ValidationUtils.extractCountryCode(selectedCountryCode);
        String phoneBody = etPhone.getText().toString().trim();
        String phone = code + phoneBody;
        String gender = actvGender.getText().toString().trim();

        if (!validateInputs(name, email, code, phoneBody))
            return;

        setLoadingState(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("full_name", name);
        updates.put("display_name", name);
        updates.put("email", email);
        updates.put("phone_number", phone);
        updates.put("gender", gender);

        if ("Staff".equalsIgnoreCase(originalUser.getUserType())
                || "Admin".equalsIgnoreCase(originalUser.getUserType())) {
            updates.put("designation", etDesignation.getText().toString().trim());
            updates.put("department", etDepartment.getText().toString().trim());
        } else if ("Student".equalsIgnoreCase(originalUser.getUserType())) {
            updates.put("batch", etBatch.getText().toString().trim());
            updates.put("level_term", actvLevelTerm.getText().toString().trim());
            updates.put("department", etDepartment.getText().toString().trim());
            updates.put("section", actvSection.getText().toString().trim());
        }

        if (isProfilePictureRemoved && profileImageUris.isEmpty()) {
            updates.put("profile_image_url", "");
        }

        if (!profileImageUris.isEmpty()) {
            uploadImagesAndFinishUpdate(updates);
        } else {
            finalizeDatabaseUpdate(updates);
        }
    }

    private void uploadImagesAndFinishUpdate(Map<String, Object> updates) {
        if (profileImageUris.isEmpty())
            return;

        Uri uri = profileImageUris.get(0);
        String fileName = currentUniversityId + "_" + System.currentTimeMillis() + ".jpg";

        SupabaseStorageHelper.uploadImage(this, uri, "profiles", fileName, new SupabaseStorageHelper.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                updates.put("profile_image_url", publicUrl);
                finalizeDatabaseUpdate(updates);
            }

            @Override
            public void onFailure(Exception e) {
                setLoadingState(false);
                ErrorHelper.showError(btnSaveChanges, "Upload Failed: " + e.getMessage());
            }
        });
    }

    private void finalizeDatabaseUpdate(Map<String, Object> updates) {
        SupabaseDatabaseHelper.update("profiles", "university_id=eq." + currentUniversityId, updates,
                new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                    @Override
                    public void onSuccess(String result) {
                        // Update local session data if email changed
                        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
                        android.content.SharedPreferences.Editor editor = prefs.edit();
                        if (updates.containsKey("email")) {
                            String newEmail = (String) updates.get("email");
                            userEmail = newEmail;
                            editor.putString("cachedUserEmail", newEmail);
                        }
                        if (updates.containsKey("full_name"))
                            editor.putString("cachedUserName", (String) updates.get("full_name"));
                        if (updates.containsKey("phone_number"))
                            editor.putString("cachedUserPhone", (String) updates.get("phone_number"));
                        if (updates.containsKey("gender"))
                            editor.putString("cachedUserGender", (String) updates.get("gender"));
                        if (updates.containsKey("department"))
                            editor.putString("cachedUserDepartment", (String) updates.get("department"));
                        if (updates.containsKey("batch"))
                            editor.putString("cachedUserBatch", (String) updates.get("batch"));
                        if (updates.containsKey("level_term"))
                            editor.putString("cachedUserLevelTerm", (String) updates.get("level_term"));
                        if (updates.containsKey("section"))
                            editor.putString("cachedUserSection", (String) updates.get("section"));
                        if (updates.containsKey("designation"))
                            editor.putString("cachedUserDesignation", (String) updates.get("designation"));
                        if (updates.containsKey("profile_image_url"))
                            editor.putString("cachedProfileImageUrl", (String) updates.get("profile_image_url"));
                        editor.apply();

                        setLoadingState(false);
                        resetUIState();
                        SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Profile updated successfully");
                        loadUserData(currentUniversityId);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        setLoadingState(false);
                        ErrorHelper.showError(btnSaveChanges, "Update failed: " + errorMessage);
                    }
                });
    }

    private void resetUIState() {
        etFullName.setEnabled(false);
        etEmail.setEnabled(false);
        etPhone.setEnabled(false);
        if (actvCountryCode != null)
            actvCountryCode.setEnabled(false);
        actvGender.setEnabled(false);
        etBatch.setEnabled(false);
        actvLevelTerm.setEnabled(false);
        etDepartment.setEnabled(false);
        actvSection.setEnabled(false);
        etDesignation.setEnabled(false);
        etOldPassword.setText("");
        etNewPassword.setText("");
        etConfirmPassword.setText("");
        profileImageUris.clear();
        isProfilePictureRemoved = false;
        btnSaveChanges.setVisibility(View.GONE);
    }

    private boolean validateInputs(String name, String email, String countryCode, String phoneBody) {
        boolean valid = true;
        if (TextUtils.isEmpty(name)) {
            ErrorHelper.setFieldError(tilFullName, "Full name is required");
            valid = false;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            ErrorHelper.setFieldError(tilEmail, "Please enter a valid email address");
            valid = false;
        }

        if (!ValidationUtils.isValidPhone(countryCode, phoneBody)) {
            ErrorHelper.setFieldError(tilPhone, "Please enter a valid phone number");
            valid = false;
        }

        return valid;
    }

    private void reauthenticateAndChangePassword(String oldPass, String newPass, Runnable onComplete) {
        if (userEmail == null && originalUser != null) {
            userEmail = originalUser.getEmail();
        }
        if (userEmail == null || userEmail.isEmpty()) {
            userEmail = etEmail.getText().toString().trim();
        }
        if (userEmail == null || userEmail.isEmpty()) {
            showLoading(false);
            ErrorHelper.showError(btnConfirmPasswordChange, "Email not found. Please try again.");
            return;
        }

        SupabaseAuthHelper.login(userEmail, oldPass, new SupabaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String userId, String accessToken, String refreshToken) {
                // Update helper if needed, though this is just for verification here
                SupabaseDatabaseHelper.setAuthToken(accessToken);
                onComplete.run();
            }

            @Override
            public void onFailure(String errorMessage) {
                showLoading(false);
                ErrorHelper.setFieldError(tilOldPassword, "Incorrect password");
            }
        });
    }

    private void showImageSourceDialog() {
        List<String> options = new ArrayList<>();
        options.add(getString(R.string.take_photo));
        options.add(getString(R.string.choose_gallery));

        boolean hasExistingPhoto = originalUser != null && originalUser.getProfileImageUrl() != null
                && !originalUser.getProfileImageUrl().isEmpty();
        boolean hasSelectedPhoto = !profileImageUris.isEmpty();

        if ((hasExistingPhoto && !isProfilePictureRemoved) || hasSelectedPhoto) {
            options.add(getString(R.string.remove_photo));
        }
        options.add(getString(R.string.cancel));

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_profile_picture));
        builder.setItems(options.toArray(new String[0]), (dialog, which) -> {
            String selectedOption = options.get(which);
            if (selectedOption.equals(getString(R.string.take_photo))) {
                checkCameraPermission();
            } else if (selectedOption.equals(getString(R.string.choose_gallery))) {
                openGallery();
            } else if (selectedOption.equals(getString(R.string.remove_photo))) {
                removeProfilePicture();
            }
        });
        builder.show();
    }

    private void removeProfilePicture() {
        profileImageUris.clear();
        isProfilePictureRemoved = true;
        ivProfilePicture.setImageResource(R.drawable.ic_user);
        checkForChanges();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA },
                    CAMERA_PERMISSION_CODE);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Error occurred while creating file");
            }
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_IMAGES_PICK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Camera Permission Denied");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGES_PICK) {
                profileImageUris.clear();
                if (data != null) {
                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            profileImageUris.add(clipData.getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null) {
                        profileImageUris.add(data.getData());
                    }
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (cameraImageUri != null) {
                    profileImageUris.clear();
                    profileImageUris.add(cameraImageUri);
                }
            }

            if (!profileImageUris.isEmpty()) {
                isProfilePictureRemoved = false;
                ivProfilePicture.setImageURI(profileImageUris.get(0));
                checkForChanges();
            }
        }
    }

    private void loadCachedUserData() {
        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        String name = prefs.getString("cachedUserName", "");
        String id = prefs.getString("universityId", "");
        String email = prefs.getString("cachedUserEmail", "");
        String phone = prefs.getString("cachedUserPhone", "");
        String gender = prefs.getString("cachedUserGender", "");
        String type = prefs.getString("userType", "Student");
        String dept = prefs.getString("cachedUserDepartment", "");
        String batch = prefs.getString("cachedUserBatch", "");
        String levelTerm = prefs.getString("cachedUserLevelTerm", "");
        String section = prefs.getString("cachedUserSection", "");
        String designation = prefs.getString("cachedUserDesignation", "");
        String imgUrl = prefs.getString("cachedProfileImageUrl", "");

        if (!name.isEmpty()) {
            etFullName.setText(name);
            etUniversityId.setText(id);
            etEmail.setText(email);
            etPhone.setText(phone);
            actvGender.setText(gender, false);

            ProfileRoleHelper.applyRoleVisibility(type, tilDesignation, tilBatch, tilLevelTerm, tilDepartment,
                    tilSection);

            if (dept.isEmpty())
                dept = "Not Specified";

            if ("Staff".equals(type) || "Admin".equalsIgnoreCase(type)) {
                etDesignation.setText(designation);
                etDepartment.setText(dept);
            } else {
                etBatch.setText(batch);
                actvLevelTerm.setText(levelTerm, false);
                etDepartment.setText(dept);
                actvSection.setText(section, false);
            }

            if (!imgUrl.isEmpty()) {
                GlideApp.with(UserProfileActivity.this)
                        .load(imgUrl)
                        .placeholder(R.drawable.ic_default_avatar)
                        .thumbnail(0.1f)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .into(ivProfilePicture);
            }
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!isAdminViewing)
            btnSaveChanges.setEnabled(!show);
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnSaveChanges.setEnabled(false);
            btnSaveChanges.setText("");
            btnSaveChanges
                    .setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
            btnSaveChanges.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
            btnSaveChanges.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.primaryColor)));
            loadingAnimation.setVisibility(View.VISIBLE);
            loadingAnimation.playAnimation();
        } else {
            loadingAnimation.setVisibility(View.GONE);
            loadingAnimation.pauseAnimation();
            btnSaveChanges.setEnabled(true);
            btnSaveChanges.setText(R.string.btn_save_changes);
            btnSaveChanges.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.primaryColor)));
            btnSaveChanges.setStrokeWidth(0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Listeners were removed in favor of Supabase REST calls
    }

    private void setupKeyboardListener() {
        if (userProfileRoot == null || keyboardSpacer == null)
            return;

        userProfileRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                userProfileRoot.getWindowVisibleDisplayFrame(r);
                int screenHeight = userProfileRoot.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    if (keyboardSpacer.getVisibility() != View.VISIBLE) {
                        keyboardSpacer.setVisibility(View.VISIBLE);
                        keyboardSpacer
                                .getLayoutParams().height = (int) (320 * getResources().getDisplayMetrics().density);
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

    private String getFormattedPhoneNumber() {
        if (actvCountryCode == null || etPhone == null)
            return "";
        String selectedCountryCode = actvCountryCode.getText().toString().trim();
        String code = ValidationUtils.extractCountryCode(selectedCountryCode);
        String phoneBody = etPhone.getText().toString().trim();
        return code + phoneBody;
    }
}
