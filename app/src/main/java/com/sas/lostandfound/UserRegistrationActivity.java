package com.sas.lostandfound;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserRegistrationActivity extends AppCompatActivity {

    private EditText etFullName, etUniversityId, etEmail, etPhone,
            etDepartment, etBatch, etPassword, etConfirmPassword, etDesignation, etAdminCode;
    private AutoCompleteTextView actvLevelTerm, actvUserType;
    private TextInputLayout tilBatch, tilDepartment, tilLevelTerm, tilDesignation, tilUserType, tilAdminCode, tilUniversityId,
            tilFullName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private MaterialButton btnCreateAccount;
    private ProgressBar progressBar;
    private ImageView ivProfilePicture;
    private FloatingActionButton fabAddPhoto;
    private ImageButton btnBack;
    private TextView tvLogin, tvPolicyText;
    private CheckBox cbPolicy;
    private View keyboardSpacer;
    private View registrationRoot;

    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int CAMERA_PERMISSION_CODE = 200;

    private Uri profileImageUri;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);

        initializeViews();
        setupDropdowns();
        setupListeners();
        setupPolicyText();
        setupLoginLink();
        setupKeyboardListener();
        
        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout);
        }
    }

    private void initializeViews() {
        registrationRoot = findViewById(R.id.registrationRoot);
        keyboardSpacer = findViewById(R.id.keyboardSpacer);
        etUniversityId = findViewById(R.id.etUniversityId);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etDepartment = findViewById(R.id.etDepartment);
        etBatch = findViewById(R.id.etBatch);
        etDesignation = findViewById(R.id.etDesignation);
        etAdminCode = findViewById(R.id.etAdminCode);
        actvLevelTerm = findViewById(R.id.actvLevelTerm);
        actvUserType = findViewById(R.id.actvUserType);

        tilBatch = findViewById(R.id.tilBatch);
        tilDepartment = findViewById(R.id.tilDepartment);
        tilLevelTerm = findViewById(R.id.tilLevelTerm);
        tilDesignation = findViewById(R.id.tilDesignation);
        tilUserType = findViewById(R.id.tilUserType);
        tilAdminCode = findViewById(R.id.tilAdminCode);
        tilUniversityId = findViewById(R.id.tilUniversityId);
        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        ErrorHelper.attachToTextInputLayout(tilUserType);
        ErrorHelper.attachToTextInputLayout(tilUniversityId);
        ErrorHelper.attachToTextInputLayout(tilFullName);
        ErrorHelper.attachToTextInputLayout(tilEmail);
        ErrorHelper.attachToTextInputLayout(tilPhone);
        ErrorHelper.attachToTextInputLayout(tilDesignation);
        ErrorHelper.attachToTextInputLayout(tilAdminCode);
        ErrorHelper.attachToTextInputLayout(tilBatch);
        ErrorHelper.attachToTextInputLayout(tilDepartment);
        ErrorHelper.attachToTextInputLayout(tilLevelTerm);
        ErrorHelper.attachToTextInputLayout(tilPassword);
        ErrorHelper.attachToTextInputLayout(tilConfirmPassword);

        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        progressBar = findViewById(R.id.progressBar);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        fabAddPhoto = findViewById(R.id.fabAddPhoto);
        btnBack = findViewById(R.id.btnBack);
        tvLogin = findViewById(R.id.tvLogin);
        tvPolicyText = findViewById(R.id.tvPolicyText);
        cbPolicy = findViewById(R.id.cbPolicy);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
    }

    private void setupKeyboardListener() {
        if (registrationRoot == null || keyboardSpacer == null) return;

        registrationRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                registrationRoot.getWindowVisibleDisplayFrame(r);
                int screenHeight = registrationRoot.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    if (keyboardSpacer.getVisibility() != View.VISIBLE) {
                        keyboardSpacer.setVisibility(View.VISIBLE);
                        keyboardSpacer.getLayoutParams().height = (int) (200 * getResources().getDisplayMetrics().density);
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

    private void setupDropdowns() {
        String[] levelTermOptions = {
                "Level 1 Term I", "Level 1 Term II",
                "Level 2 Term I", "Level 2 Term II",
                "Level 3 Term I", "Level 3 Term II",
                "Level 4 Term I", "Level 4 Term II"
        };
        ArrayAdapter<String> levelTermAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, levelTermOptions);
        actvLevelTerm.setAdapter(levelTermAdapter);
        actvLevelTerm.setOnClickListener(v -> actvLevelTerm.showDropDown());

        String[] userTypeOptions = {"Student", "Staff", "Admin"};
        ArrayAdapter<String> userTypeAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, userTypeOptions);
        actvUserType.setAdapter(userTypeAdapter);
        actvUserType.setOnClickListener(v -> actvUserType.showDropDown());

        actvUserType.setOnItemClickListener((parent, view, position, id) -> {
            String selection = (String) parent.getItemAtPosition(position);
            updateUIForUserType(selection);
        });

        updateUIForUserType("Student");
    }

    private void updateUIForUserType(String userType) {
        if ("Student".equals(userType)) {
            tilUserType.setStartIconDrawable(R.drawable.ic_graduation_cap);
            tilBatch.setVisibility(View.VISIBLE);
            tilDepartment.setVisibility(View.VISIBLE);
            tilLevelTerm.setVisibility(View.VISIBLE);
            tilDesignation.setVisibility(View.GONE);
            tilAdminCode.setVisibility(View.GONE);
        } else if ("Staff".equals(userType)) {
            tilUserType.setStartIconDrawable(R.drawable.ic_user);
            tilBatch.setVisibility(View.GONE);
            tilDepartment.setVisibility(View.VISIBLE);
            tilLevelTerm.setVisibility(View.GONE);
            tilDesignation.setVisibility(View.VISIBLE);
            tilAdminCode.setVisibility(View.GONE);
        } else if ("Admin".equals(userType)) {
            tilUserType.setStartIconDrawable(R.drawable.ic_admin_id);
            tilBatch.setVisibility(View.GONE);
            tilDepartment.setVisibility(View.VISIBLE);
            tilLevelTerm.setVisibility(View.GONE);
            tilDesignation.setVisibility(View.VISIBLE);
            tilAdminCode.setVisibility(View.VISIBLE);
        }
    }

    private void setupListeners() {
        fabAddPhoto.setOnClickListener(v -> showImageSourceDialog());
        ivProfilePicture.setOnClickListener(v -> showImageSourceDialog());

        cbPolicy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnCreateAccount.setEnabled(isChecked);
        });

        btnCreateAccount.setOnClickListener(v -> {
            if (cbPolicy.isChecked()) {
                registerUser();
            } else {
                ErrorHelper.showError(btnCreateAccount, "Please agree to the policy first.");
            }
        });

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (tvLogin != null) {
            tvLogin.setOnClickListener(v -> {
                startActivity(new Intent(this, UserLoginActivity.class));
                finish();
            });
        }
    }

    private void setupPolicyText() {
        if (tvPolicyText == null) return;

        String fullText = getString(R.string.lost_and_found_policy);
        String clickablePart = getString(R.string.lost_and_found_policy_clickable);

        SpannableString ss = new SpannableString(fullText);

        int startIndex = fullText.indexOf(clickablePart);
        if (startIndex != -1) {
            int endIndex = startIndex + clickablePart.length();

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    startActivity(new Intent(UserRegistrationActivity.this, LostAndFoundPolicyActivity.class));
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            };

            ss.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primaryColor)),
                    startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new StyleSpan(Typeface.BOLD),
                    startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvPolicyText.setText(ss);
        tvPolicyText.setMovementMethod(LinkMovementMethod.getInstance());
        tvPolicyText.setHighlightColor(Color.TRANSPARENT);
    }

    private void setupLoginLink() {
        if (tvLogin == null) return;

        Spanned spanned = Html.fromHtml(getString(R.string.login_link), Html.FROM_HTML_MODE_LEGACY);
        SpannableString ss = new SpannableString(spanned);

        String fullText = spanned.toString();
        String loginText = "Sign In";
        int startIndex = fullText.indexOf(loginText);

        if (startIndex != -1) {
            int endIndex = startIndex + loginText.length();
            ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primaryColor)),
                    startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new StyleSpan(Typeface.BOLD),
                    startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvLogin.setText(ss);
    }

    private void showImageSourceDialog() {
        String[] options = {getString(R.string.take_photo), getString(R.string.choose_gallery), getString(R.string.cancel)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_profile_picture));
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) checkCameraPermission();
            else if (which == 1) openGallery();
        });
        builder.show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
        } else openCamera();
    }

    private void openCamera() {
        try {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            if (takePictureIntent.resolveActivity(getPackageManager()) == null) {
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Camera not available.");
                return;
            }

            File photoFile = createImageFile();
            if (photoFile == null) {
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to create image file.");
                return;
            }

            profileImageUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    photoFile
            );

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, profileImageUri);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);

        } catch (Exception e) {
            SnackbarManager.show(SnackbarManager.Type.ERROR, "Unable to open camera.");
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            try {
                if (requestCode == REQUEST_IMAGE_PICK) {
                    if (data == null || data.getData() == null) {
                        SnackbarManager.show(SnackbarManager.Type.WARNING, "No image selected.");
                        return;
                    }

                    profileImageUri = data.getData();
                }

                if (profileImageUri != null) {
                    GlideApp.with(this)
                            .load(profileImageUri)
                            .thumbnail(0.1f)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .circleCrop()
                            .into(ivProfilePicture);
                }

            } catch (Exception e) {
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to load image.");
            }
        }
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm != null ? cm.getActiveNetworkInfo() : null;
            return ni != null && ni.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private void showLoading(boolean isLoading) {
        btnCreateAccount.setEnabled(!isLoading && cbPolicy.isChecked());
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCreateAccount.setText(isLoading ? "" : "Create Account");
    }

    private void registerUser() {
        if (!isNetworkAvailable()) {
            ErrorHelper.showError(btnCreateAccount, "No internet connection. Please check your network.");
            return;
        }

        String universityId = etUniversityId.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String userType = actvUserType.getText().toString().trim();

        if (TextUtils.isEmpty(userType)) {
            ErrorHelper.setFieldError(tilUserType, "Please select user type");
            return;
        }

        if (TextUtils.isEmpty(universityId)) {
            ErrorHelper.setFieldError(tilUniversityId, "University ID is required");
            return;
        }

        // Firebase Key Validation: Cannot contain . $ # [ ] /
        if (universityId.matches(".*[\\.\\$#\\[\\]/].*")) {
            ErrorHelper.setFieldError(tilUniversityId, "University ID cannot contain characters like . $ # [ ] /");
            return;
        }

        if (TextUtils.isEmpty(fullName)) {
            ErrorHelper.setFieldError(tilFullName, "Full name is required");
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            ErrorHelper.setFieldError(tilEmail, "Please enter a valid email address");
            return;
        }

        if (!ValidationUtils.isValidPhone(phone)) {
            ErrorHelper.setFieldError(tilPhone, "Please enter a valid phone number (11 digits)");
            return;
        }

        if (!ValidationUtils.isValidPassword(password)) {
            ErrorHelper.setFieldError(tilPassword, ValidationUtils.getPasswordRequirements());
            return;
        }

        if (!password.equals(confirmPassword)) {
            ErrorHelper.setFieldError(tilConfirmPassword, "Passwords do not match");
            return;
        }

        if ("Student".equals(userType)) {
            if (TextUtils.isEmpty(etDepartment.getText().toString().trim())) {
                ErrorHelper.setFieldError(tilDepartment, "Department is required"); return;
            }
            if (TextUtils.isEmpty(etBatch.getText().toString().trim())) {
                ErrorHelper.setFieldError(tilBatch, "Batch is required"); return;
            }
            if (TextUtils.isEmpty(actvLevelTerm.getText().toString().trim())) {
                ErrorHelper.setFieldError(tilLevelTerm, "Level/Term is required"); return;
            }
        } else if ("Staff".equals(userType)) {
            if (TextUtils.isEmpty(etDesignation.getText().toString().trim())) {
                ErrorHelper.setFieldError(tilDesignation, "Required"); return;
            }
            if (TextUtils.isEmpty(etDepartment.getText().toString().trim())) {
                ErrorHelper.setFieldError(tilDepartment, "Required"); return;
            }
        } else if ("Admin".equals(userType)) {
            if (TextUtils.isEmpty(etDesignation.getText().toString().trim())) {
                ErrorHelper.setFieldError(tilDesignation, "Required"); return;
            }
            if (TextUtils.isEmpty(etDepartment.getText().toString().trim())) {
                ErrorHelper.setFieldError(tilDepartment, "Required"); return;
            }
            if (TextUtils.isEmpty(etAdminCode.getText().toString().trim())) {
                ErrorHelper.setFieldError(tilAdminCode, "Required"); return;
            }
        }

        showLoading(true);

        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + universityId, new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users != null && !users.isEmpty()) {
                    showLoading(false);
                    etUniversityId.requestFocus();
                    tilUniversityId.setError("An account with this University ID already exists. Please log in.");
                } else {
                    performAuthRegistration(email, password, universityId, fullName, userType);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                showLoading(false);
                ErrorHelper.showError(btnCreateAccount, "Database error: " + errorMessage);
            }
        });
    }

    private void redirectToLogin() {
        startActivity(new Intent(UserRegistrationActivity.this, UserLoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private void saveAdminRequest(String universityId, String fullName, String email, String phone, String designation, String department, String adminCode, String password, String imageUrl, String authId) {
        Log.d("UserRegistration", "Saving Admin Request to database for: " + universityId);
        AdminRequest request = new AdminRequest(universityId, authId, fullName, email, phone, designation, department, adminCode, password, imageUrl);

        SupabaseDatabaseHelper.insert("admin_requests", request, new SupabaseDatabaseHelper.DatabaseCallback<>() {
            @Override
            public void onSuccess(String result) {
                showLoading(false);
                Log.d("UserRegistration", "Admin request submitted successfully");
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Admin request submitted. Please wait for approval before logging in.");
                redirectToLogin();
            }

            @Override
            public void onFailure(String errorMessage) {
                showLoading(false);
                Log.e("UserRegistration", "Admin request failed: " + errorMessage);
                ErrorHelper.showError(btnCreateAccount, "Database error: " + errorMessage);
            }
        });
    }

    private void performAuthRegistration(String email, String password, String universityId, String fullName, String userType) {
        if (SupabaseConfig.SUPABASE_URL.isEmpty() || SupabaseConfig.SUPABASE_KEY.isEmpty()) {
            showLoading(false);
            ErrorHelper.showError(btnCreateAccount, "Supabase configuration is missing. Check local.properties.");
            return;
        }

        Log.d("UserRegistration", "Starting Supabase Auth for: " + email);
        SupabaseAuthHelper.signUp(email, password, new SupabaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String authId, String accessToken, String refreshToken) {
                Log.d("UserRegistration", "Supabase Auth success. Auth UID: " + authId);
                String dbUserId = universityId;

                if (profileImageUri != null) {
                    String fileName = dbUserId + "_" + System.currentTimeMillis() + ".jpg";
                    Log.d("UserRegistration", "Uploading profile image: " + fileName);
                    String folder = "Admin".equals(userType) ? "admin_requests" : "profiles";
                    SupabaseStorageHelper.uploadImage(UserRegistrationActivity.this, profileImageUri, folder, fileName, new SupabaseStorageHelper.UploadCallback() {
                        @Override
                        public void onSuccess(String publicUrl) {
                            Log.d("UserRegistration", "Profile image upload success: " + publicUrl);
                            if ("Admin".equals(userType)) {
                                saveAdminRequest(universityId, fullName, email, etPhone.getText().toString().trim(), etDesignation.getText().toString().trim(), etDepartment.getText().toString().trim(), etAdminCode.getText().toString().trim(), password, publicUrl, authId);
                            } else {
                                saveUser(dbUserId, authId, publicUrl, universityId, fullName, email, password, userType);
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Log.e("UserRegistration", "Profile image upload failed: " + e.getMessage());
                            SnackbarManager.show(
                                    SnackbarManager.Type.WARNING,
                                    "Profile image upload failed. Continuing without image."
                            );
                            if ("Admin".equals(userType)) {
                                saveAdminRequest(universityId, fullName, email, etPhone.getText().toString().trim(), etDesignation.getText().toString().trim(), etDepartment.getText().toString().trim(), etAdminCode.getText().toString().trim(), password, null, authId);
                            } else {
                                saveUser(dbUserId, authId, null, universityId, fullName, email, password, userType);
                            }
                        }
                    });
                } else {
                    Log.d("UserRegistration", "No profile image, saving user data");
                    if ("Admin".equals(userType)) {
                        saveAdminRequest(universityId, fullName, email, etPhone.getText().toString().trim(), etDesignation.getText().toString().trim(), etDepartment.getText().toString().trim(), etAdminCode.getText().toString().trim(), password, null, authId);
                    } else {
                        saveUser(dbUserId, authId, null, universityId, fullName, email, password, userType);
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                showLoading(false);
                Log.e("UserRegistration", "Supabase Auth failed: " + errorMessage);

                if (errorMessage == null || errorMessage.isEmpty()) {
                    errorMessage = "Registration failed. Please try again.";
                }

                ErrorHelper.showError(btnCreateAccount, errorMessage);
            }
        });
    }

    private void saveUser(String universityIdKey, String authId, String imageUrl, String universityId, String fullName, String email, String password, String userType) {
        Log.d("UserRegistration", "Saving user data. UID: " + authId + ", UnivID: " + universityIdKey + ", Type: " + userType);
        String phone = etPhone.getText().toString().trim();

        User user;
        if ("Student".equals(userType)) {
            String department = etDepartment.getText().toString().trim();
            String batch = etBatch.getText().toString().trim();
            String levelTerm = actvLevelTerm.getText().toString().trim();
            user = new User(universityId, authId, fullName, email, password, phone, department, batch, levelTerm, "Not Specified", imageUrl, "Not Specified");
        } else {
            String designation = etDesignation.getText().toString().trim();
            String department = etDepartment.getText().toString().trim();
            user = new User(universityId, authId, fullName, email, password, phone, designation, department, imageUrl, "Not Specified", userType);
        }

        SupabaseDatabaseHelper.insert("profiles", user, new SupabaseDatabaseHelper.DatabaseCallback<>() {
            @Override
            public void onSuccess(String result) {
                showLoading(false);
                Log.d("UserRegistration", "Registration data saved successfully");
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Your account has been successfully registered. Please log in.");
                redirectToLogin();
            }

            @Override
            public void onFailure(String errorMessage) {
                showLoading(false);
                Log.e("UserRegistration", "Database update failed: " + errorMessage);
                ErrorHelper.showError(btnCreateAccount, "Database error: " + errorMessage);
            }
        });
    }
}
