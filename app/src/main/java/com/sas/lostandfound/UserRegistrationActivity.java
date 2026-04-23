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
import android.widget.Toast;

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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

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

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).getReference();

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
            tilDepartment.setVisibility(View.GONE);
            tilLevelTerm.setVisibility(View.GONE);
            tilDesignation.setVisibility(View.VISIBLE);
            tilAdminCode.setVisibility(View.GONE);
        } else if ("Admin".equals(userType)) {
            tilUserType.setStartIconDrawable(R.drawable.ic_admin_id);
            tilBatch.setVisibility(View.GONE);
            tilDepartment.setVisibility(View.GONE);
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
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try { photoFile = createImageFile(); }
            catch (IOException ex) { Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show(); }
            if (photoFile != null) {
                profileImageUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, profileImageUri);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
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
            if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
                profileImageUri = data.getData();
                GlideApp.with(this)
                        .load(profileImageUri)
                        .thumbnail(0.1f)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .into(ivProfilePicture);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                GlideApp.with(this)
                        .load(profileImageUri)
                        .thumbnail(0.1f)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .into(ivProfilePicture);
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    private void showLoading(boolean isLoading) {
        btnCreateAccount.setEnabled(!isLoading && cbPolicy.isChecked());
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCreateAccount.setText(isLoading ? "" : "Create Account");
    }

    private void registerUser() {
        if (!isNetworkAvailable()) {
            ErrorHelper.showError(btnCreateAccount, "No internet connection.");
            return;
        }

        String universityId = etUniversityId.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String userType = actvUserType.getText().toString().trim();

        // Clear previous errors
        tilUniversityId.setError(null);
        tilFullName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
        tilDesignation.setError(null);
        tilAdminCode.setError(null);

        if (TextUtils.isEmpty(universityId)) { tilUniversityId.setError("Required"); return; }
        if (TextUtils.isEmpty(fullName)) { tilFullName.setError("Required"); return; }
        if (!TextUtils.isEmpty(email) && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Valid email required"); return; }
        if (TextUtils.isEmpty(phone)) { tilPhone.setError("Required"); return; }
        if (password.length() < 6) { tilPassword.setError("Minimum 6 characters"); return; }
        if (!password.equals(confirmPassword)) { tilConfirmPassword.setError("Passwords do not match"); return; }

        if ("Staff".equals(userType)) {
            if (TextUtils.isEmpty(etDesignation.getText().toString().trim())) {
                tilDesignation.setError("Required"); return;
            }
        } else if ("Admin".equals(userType)) {
            if (TextUtils.isEmpty(etDesignation.getText().toString().trim())) {
                tilDesignation.setError("Required"); return;
            }
            if (TextUtils.isEmpty(etAdminCode.getText().toString().trim())) {
                tilAdminCode.setError("Required"); return;
            }
        }

        showLoading(true);

        mDatabase.child("Users").child(universityId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    showLoading(false);
                    etUniversityId.requestFocus();
                    tilUniversityId.setError("An account with this University ID already exists. Please log in.");
                } else {
                    performAuthRegistration(email, password, universityId, fullName, userType);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                ErrorHelper.showError(btnCreateAccount, "Database error: " + error.getMessage());
            }
        });
    }

    private void redirectToLogin() {
        startActivity(new Intent(UserRegistrationActivity.this, UserLoginActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    private void saveAdminRequest(String universityId, String fullName, String email, String phone, String designation, String adminCode, String password, String imageUrl, String authId) {
        Log.d("UserRegistration", "Saving Admin Request to database for: " + universityId);
        AdminRequest request = new AdminRequest(universityId, authId, fullName, email, phone, designation, adminCode, password, imageUrl);

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("adminRequests/" + universityId, request);
        updates.put("UIDToUniversityID/" + authId, universityId);

        mDatabase.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d("UserRegistration", "Admin request submitted successfully");
                        ErrorHelper.showError(btnCreateAccount, "Admin request submitted. Please wait for approval before logging in.");
                        redirectToLogin();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Permission denied";
                        Log.e("UserRegistration", "Admin request failed: " + error);
                        ErrorHelper.showError(btnCreateAccount, "Database error: " + error);
                    }
                });
    }

    private void performAuthRegistration(String email, String password, String universityId, String fullName, String userType) {
        if (TextUtils.isEmpty(email)) {
            showLoading(false);
            ErrorHelper.showError(btnCreateAccount, "Email is required for Student/Staff registration");
            return;
        }

        Log.d("UserRegistration", "Starting Firebase Auth for: " + email);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showLoading(false);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Auth failed";
                        Log.e("UserRegistration", "Firebase Auth failed: " + errorMsg);
                        ErrorHelper.showError(btnCreateAccount, "Registration failed: " + errorMsg);
                        return;
                    }

                    if (mAuth.getCurrentUser() != null) {
                        String authId = mAuth.getCurrentUser().getUid();
                        Log.d("UserRegistration", "Firebase Auth success. Auth UID: " + authId);
                        String dbUserId = universityId;

                        if (profileImageUri != null) {
                            String fileName = dbUserId + "_" + System.currentTimeMillis() + ".jpg";
                            Log.d("UserRegistration", "Uploading profile image: " + fileName);
                            String folder = "Admin".equals(userType) ? "admin_requests" : "profiles";
                            SupabaseStorageHelper.uploadImage(this, profileImageUri, folder, fileName, new SupabaseStorageHelper.UploadCallback() {
                                @Override
                                public void onSuccess(String publicUrl) {
                                    Log.d("UserRegistration", "Profile image upload success: " + publicUrl);
                                    if ("Admin".equals(userType)) {
                                        saveAdminRequest(universityId, fullName, email, etPhone.getText().toString().trim(), etDesignation.getText().toString().trim(), etAdminCode.getText().toString().trim(), password, publicUrl, authId);
                                    } else {
                                        saveUser(dbUserId, authId, publicUrl, universityId, fullName, email, password, userType);
                                    }
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e("UserRegistration", "Profile image upload failed: " + e.getMessage());
                                    if ("Admin".equals(userType)) {
                                        saveAdminRequest(universityId, fullName, email, etPhone.getText().toString().trim(), etDesignation.getText().toString().trim(), etAdminCode.getText().toString().trim(), password, null, authId);
                                    } else {
                                        saveUser(dbUserId, authId, null, universityId, fullName, email, password, userType);
                                    }
                                }
                            });
                        } else {
                            Log.d("UserRegistration", "No profile image, saving user data");
                            if ("Admin".equals(userType)) {
                                saveAdminRequest(universityId, fullName, email, etPhone.getText().toString().trim(), etDesignation.getText().toString().trim(), etAdminCode.getText().toString().trim(), password, null, authId);
                            } else {
                                saveUser(dbUserId, authId, null, universityId, fullName, email, password, userType);
                            }
                        }
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
            user = new User(universityId, authId, fullName, email, password, phone, designation, imageUrl, "Not Specified", userType);
        }

        // Use atomic update to write to both paths and UIDToUniversityID mapping
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("Users/" + universityIdKey, user);
        updates.put("UIDToUniversityID/" + authId, universityIdKey);

        Log.d("UserRegistration", "Performing atomic update for paths: Users, UIDToUniversityID");

        mDatabase.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d("UserRegistration", "Registration data saved successfully");
                        ErrorHelper.showError(btnCreateAccount, "Your account has been successfully registered. Please log in.");
                        redirectToLogin();
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Permission denied";
                        Log.e("UserRegistration", "Database update failed: " + error);
                        ErrorHelper.showError(btnCreateAccount, "Database error: " + error);
                    }
                });
    }
}
