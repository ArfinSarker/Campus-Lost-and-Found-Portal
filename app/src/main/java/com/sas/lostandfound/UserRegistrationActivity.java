package com.sas.lostandfound;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
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

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserRegistrationActivity extends AppCompatActivity {

    private EditText etFullName, etUniversityId, etEmail, etPhone,
            etDepartment, etBatch, etPassword, etConfirmPassword;
    private AutoCompleteTextView actvLevelTerm;
    private MaterialButton btnCreateAccount;
    private ProgressBar progressBar;
    private ImageView ivProfilePicture;
    private FloatingActionButton fabAddPhoto;
    private ImageButton btnBack;
    private TextView tvLogin;

    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int CAMERA_PERMISSION_CODE = 200;

    private Uri profileImageUri;
    private String currentPhotoPath;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_registration);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initializeViews();
        setupDropdowns();
        setupListeners();
    }

    private void initializeViews() {
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
        progressBar = findViewById(R.id.progressBar);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        fabAddPhoto = findViewById(R.id.fabAddPhoto);
        btnBack = findViewById(R.id.btnBack);
        tvLogin = findViewById(R.id.tvLogin);
    }

    private void setupDropdowns() {
        String[] levelTermOptions = {
                "Level 1 Term I", "Level 1 Term II",
                "Level 2 Term I", "Level 2 Term II",
                "Level 3 Term I", "Level 3 Term II",
                "Level 4 Term I", "Level 4 Term II"
        };
        ArrayAdapter<String> levelTermAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, levelTermOptions);
        actvLevelTerm.setAdapter(levelTermAdapter);
        actvLevelTerm.setOnClickListener(v -> actvLevelTerm.showDropDown());
    }

    private void setupListeners() {
        fabAddPhoto.setOnClickListener(v -> showImageSourceDialog());
        ivProfilePicture.setOnClickListener(v -> showImageSourceDialog());
        btnCreateAccount.setOnClickListener(v -> registerUser());
        
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
                Glide.with(this).load(profileImageUri).circleCrop().into(ivProfilePicture);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                Glide.with(this).load(profileImageUri).circleCrop().into(ivProfilePicture);
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    private void showLoading(boolean isLoading) {
        btnCreateAccount.setEnabled(!isLoading);
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnCreateAccount.setText(isLoading ? "" : "Create Account");
    }

    private void registerUser() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_LONG).show();
            return;
        }

        String universityId = etUniversityId.getText().toString().trim();
        String fullName = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(universityId)) { etUniversityId.setError("Required"); return; }
        if (TextUtils.isEmpty(fullName)) { etFullName.setError("Required"); return; }
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email required"); return; }
        if (password.length() < 6) { etPassword.setError("Minimum 6 characters"); return; }
        if (!password.equals(confirmPassword)) { etConfirmPassword.setError("Passwords do not match"); return; }

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        showLoading(false);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Auth failed";
                        Toast.makeText(this, "Registration failed: " + errorMsg, Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (mAuth.getCurrentUser() != null) {
                        String userId = mAuth.getCurrentUser().getUid();
                        if (profileImageUri != null) {
                            String fileName = userId + "_" + System.currentTimeMillis() + ".jpg";
                            SupabaseStorageHelper.uploadImage(this, profileImageUri, "profiles", fileName, new SupabaseStorageHelper.UploadCallback() {
                                @Override
                                public void onSuccess(String publicUrl) {
                                    saveUser(userId, publicUrl, universityId, fullName, email);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    saveUser(userId, null, universityId, fullName, email);
                                }
                            });
                        } else {
                            saveUser(userId, null, universityId, fullName, email);
                        }
                    }
                });
    }

    private void saveUser(String userId, String imageUrl, String universityId, String fullName, String email) {
        String phone = etPhone.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String batch = etBatch.getText().toString().trim();
        String levelTerm = actvLevelTerm.getText().toString().trim();

        User user = new User(userId, fullName, universityId, email, phone, department, batch, levelTerm, "Not Specified", imageUrl, "Not Specified");

        mDatabase.child("Users").child(userId)
                .setValue(user)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Registration successful.", Toast.LENGTH_LONG).show();
                        startActivity(new Intent(this, UserLoginActivity.class)
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                        finish();
                    } else {
                        Toast.makeText(this, "Database error", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
