package com.sas.lostandfound;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class UserRegistrationActivity extends AppCompatActivity {

    private static final String TAG = "UserRegistration";
    private EditText etFullName, etUniversityId, etEmail, etPhone, etDepartment, etBatch, etPassword, etConfirmPassword;
    private AutoCompleteTextView actvLevelTerm;
    private Button btnCreateAccount;
    private TextView tvLogin;
    private ImageButton btnBack;
    private ImageView ivProfilePicture;
    private FloatingActionButton fabAddPhoto;

    private static final int REQUEST_IMAGE_PICK = 2;
    private Uri profileImageUri = null;

    // Firebase instances
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);

        // Initialize Firebase
        try {
            mAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance().getReference();
            mStorageRef = FirebaseStorage.getInstance().getReference("profile_pictures");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization error", e);
            Toast.makeText(this, "Firebase error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        initializeViews();
        setupLevelTermDropdown();
        setupClickableLogin();
        setupListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        fabAddPhoto = findViewById(R.id.fabAddPhoto);

        etUniversityId = findViewById(R.id.etUniversityId);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etDepartment = findViewById(R.id.etDepartment);
        etBatch = findViewById(R.id.etBatch);
        actvLevelTerm = findViewById(R.id.actvLevelTerm);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        tvLogin = findViewById(R.id.tvLogin);
    }

    private void setupLevelTermDropdown() {
        String[] options = {
                "Level 1 Term I", "Level 1 Term II",
                "Level 2 Term I", "Level 2 Term II",
                "Level 3 Term I", "Level 3 Term II",
                "Level 4 Term I", "Level 4 Term II"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, options);
        actvLevelTerm.setAdapter(adapter);
    }

    private void setupClickableLogin() {
        String text = getString(R.string.login_link); 
        SpannableString ss = new SpannableString(text);

        String loginWord = "Login";
        int start = text.indexOf(loginWord);
        if (start == -1) {
            loginWord = "LOGIN";
            start = text.indexOf(loginWord);
        }
        
        if (start != -1) {
            int end = start + loginWord.length();
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    finish();
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            };
            ss.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvLogin.setText(ss);
        tvLogin.setMovementMethod(LinkMovementMethod.getInstance());

        tvLogin.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                tvLogin.setTextColor(Color.parseColor("#2196F3"));
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                tvLogin.setTextColor(Color.parseColor("#757575"));
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
            }
            return false;
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        fabAddPhoto.setOnClickListener(v -> showImageSourceDialog());
        ivProfilePicture.setOnClickListener(v -> showImageSourceDialog());
        btnCreateAccount.setOnClickListener(v -> registerUser());
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void showImageSourceDialog() {
        String[] options = {getString(R.string.choose_gallery), getString(R.string.cancel)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.select_profile_picture);
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
            profileImageUri = data.getData();
            ivProfilePicture.setImageURI(profileImageUri);
        }
    }

    private void registerUser() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection. Please check your network.", Toast.LENGTH_LONG).show();
            return;
        }

        if (mAuth == null) {
            Toast.makeText(this, "Firebase initialization failed.", Toast.LENGTH_SHORT).show();
            return;
        }

        String universityId = etUniversityId.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String batch = etBatch.getText().toString().trim();
        String levelTerm = actvLevelTerm.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(universityId)) {
            etUniversityId.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Required");
            return;
        }
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            etPhone.setError("Required");
            return;
        }
        if (password.length() < 6) {
            etPassword.setError("Minimum 6 characters");
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            return;
        }

        btnCreateAccount.setEnabled(false);
        Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            if (profileImageUri != null) {
                                uploadProfileImage(userId, universityId, fullName, email, phone, department, batch, levelTerm);
                            } else {
                                saveUserToDatabase(userId, null, universityId, fullName, email, phone, department, batch, levelTerm);
                            }
                        }
                    } else {
                        btnCreateAccount.setEnabled(true);
                        String errorMsg = "Registration failed";
                        if (task.getException() != null) {
                            errorMsg = task.getException().getMessage();
                            Log.e(TAG, "Auth Error: " + errorMsg);
                            if (errorMsg.contains("network error")) {
                                errorMsg = "Network error. Please check your connection to Firebase.";
                            }
                        }
                        Toast.makeText(UserRegistrationActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void uploadProfileImage(String userId, String universityId, String fullName, String email,
                                    String phone, String department, String batch, String levelTerm) {
        StorageReference fileRef = mStorageRef.child(userId + ".jpg");
        fileRef.putFile(profileImageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    saveUserToDatabase(userId, uri.toString(), universityId, fullName, email, phone, department, batch, levelTerm);
                }))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Image upload failed: " + e.getMessage());
                    saveUserToDatabase(userId, null, universityId, fullName, email, phone, department, batch, levelTerm);
                });
    }

    private void saveUserToDatabase(String userId, String imageUrl, String universityId, String fullName,
                                    String email, String phone, String department, String batch, String levelTerm) {
        User newUser = new User(userId, fullName, universityId, email, phone, department, batch, levelTerm, imageUrl);
        
        mDatabase.child("Users").child(userId).setValue(newUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(UserRegistrationActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(UserRegistrationActivity.this, CampusDashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        btnCreateAccount.setEnabled(true);
                        String errorMsg = "Database error: " + (task.getException() != null ? task.getException().getMessage() : "unknown");
                        Log.e(TAG, errorMsg);
                        Toast.makeText(UserRegistrationActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}