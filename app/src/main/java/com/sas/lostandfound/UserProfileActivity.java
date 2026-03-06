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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private ImageView ivProfilePicture;
    private FloatingActionButton fabChangePhoto;
    private TextInputLayout tilEmail, tilPhone, tilDepartment, tilGender, tilBatch, tilLevelTerm, tilSection, tilOldPassword, tilNewPassword, tilConfirmPassword, tilDesignation, tilFullName;
    private TextInputEditText etEmail, etPhone, etDepartment, etFullName, etUniversityId, etBatch, etOldPassword, etNewPassword, etConfirmPassword, etDesignation;
    private AutoCompleteTextView actvGender, actvLevelTerm, actvSection;
    private MaterialButton btnSaveChanges, btnConfirmPasswordChange;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private TextView tvHeaderTitle;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final int REQUEST_IMAGES_PICK = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int CAMERA_PERMISSION_CODE = 100;

    private final List<Uri> profileImageUris = new ArrayList<>();
    private Uri cameraImageUri;
    private String currentPhotoPath;
    private String currentUserId;
    private String userEmail;
    
    private User originalUser;
    private boolean isDataLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        currentUserId = user.getUid();
        userEmail = user.getEmail();

        initializeViews();
        setupToolbar();
        setupDropdowns();
        loadUserData();
        
        fabChangePhoto.setOnClickListener(v -> showImageSourceDialog());

        setupEditableToggles();
        setupChangeDetection();

        btnSaveChanges.setOnClickListener(v -> saveAllChanges());
        
        btnConfirmPasswordChange.setOnClickListener(v -> {
            String oldPass = etOldPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();
            
            if (TextUtils.isEmpty(oldPass)) {
                tilOldPassword.setError("Old password required");
                return;
            }
            if (newPass.length() < 6) {
                tilNewPassword.setError("Min 6 characters required");
                return;
            }
            if (!newPass.equals(confirmPass)) {
                tilConfirmPassword.setError("Passwords do not match");
                return;
            } else {
                tilConfirmPassword.setError(null);
            }
            
            showLoading(true);
            reauthenticateAndChangePassword(oldPass, newPass, () -> {
                // Also update password in Database
                mDatabase.child("Users").child(currentUserId).child("password").setValue(newPass)
                        .addOnCompleteListener(dbTask -> {
                            showLoading(false);
                            if (dbTask.isSuccessful()) {
                                etOldPassword.setText("");
                                etNewPassword.setText("");
                                etConfirmPassword.setText("");
                                tilOldPassword.setError(null);
                                tilNewPassword.setError(null);
                                tilConfirmPassword.setError(null);
                                btnConfirmPasswordChange.setVisibility(View.GONE);
                                Snackbar.make(findViewById(android.R.id.content), "Password updated successfully in Auth and Database", Snackbar.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(this, "Database password update failed", Toast.LENGTH_SHORT).show();
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
        tilDepartment = findViewById(R.id.tilDepartment);
        tilGender = findViewById(R.id.tilGender);
        tilBatch = findViewById(R.id.tilBatch);
        tilLevelTerm = findViewById(R.id.tilLevelTerm);
        tilSection = findViewById(R.id.tilSection);
        tilOldPassword = findViewById(R.id.tilOldPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        tilDesignation = findViewById(R.id.tilDesignation);

        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
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
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setTitle("");
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
            toolbar.setNavigationOnClickListener(v -> {
                Intent intent = new Intent(this, CampusDashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                intent.putExtra("openDrawer", true);
                startActivity(intent);
                finish();
            });
        }
    }

    private void setupDropdowns() {
        String[] genders = {"Male", "Female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, genders);
        actvGender.setAdapter(genderAdapter);
        actvGender.setOnClickListener(v -> { if(actvGender.isEnabled()) actvGender.showDropDown(); });

        String[] levels = {
                "Level 1 Term I", "Level 1 Term II",
                "Level 2 Term I", "Level 2 Term II",
                "Level 3 Term I", "Level 3 Term II",
                "Level 4 Term I", "Level 4 Term II"
        };
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, levels);
        actvLevelTerm.setAdapter(levelAdapter);
        actvLevelTerm.setOnClickListener(v -> { if(actvLevelTerm.isEnabled()) actvLevelTerm.showDropDown(); });

        String[] sections = {"A", "B", "C", "D"};
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, sections);
        actvSection.setAdapter(sectionAdapter);
        actvSection.setOnClickListener(v -> { if(actvSection.isEnabled()) actvSection.showDropDown(); });
    }

    private void setupEditableToggles() {
        setupToggle(tilFullName, etFullName);
        setupToggle(tilEmail, etEmail);
        setupToggle(tilPhone, etPhone);
        setupToggle(tilGender, actvGender);
        setupToggle(tilBatch, etBatch);
        setupToggle(tilLevelTerm, actvLevelTerm);
        setupToggle(tilDepartment, etDepartment);
        setupToggle(tilSection, actvSection);
        setupToggle(tilDesignation, etDesignation);
    }

    private void setupToggle(TextInputLayout til, View field) {
        if (til == null || field == null) return;
        til.setEndIconOnClickListener(v -> {
            boolean isEnabled = field.isEnabled();
            field.setEnabled(!isEnabled);
            if (!isEnabled) {
                field.requestFocus();
                // If it's a dropdown, show it immediately
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (isDataLoaded) checkForChanges();
            }
        };

        etFullName.addTextChangedListener(watcher);
        etEmail.addTextChangedListener(watcher);
        etPhone.addTextChangedListener(watcher);
        actvGender.addTextChangedListener(watcher);
        etBatch.addTextChangedListener(watcher);
        actvLevelTerm.addTextChangedListener(watcher);
        etDepartment.addTextChangedListener(watcher);
        actvSection.addTextChangedListener(watcher);
        etDesignation.addTextChangedListener(watcher);
        
        TextWatcher passWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                String oldPass = etOldPassword.getText().toString();
                String newPass = etNewPassword.getText().toString();
                String confirmPass = etConfirmPassword.getText().toString();
                btnConfirmPasswordChange.setVisibility((!oldPass.isEmpty() && !newPass.isEmpty() && !confirmPass.isEmpty()) ? View.VISIBLE : View.GONE);
            }
        };
        etOldPassword.addTextChangedListener(passWatcher);
        etNewPassword.addTextChangedListener(passWatcher);
        etConfirmPassword.addTextChangedListener(passWatcher);
    }

    private void checkForChanges() {
        if (originalUser == null) return;

        boolean changed = false;
        
        if (!etFullName.getText().toString().equals(originalUser.getName())) changed = true;
        if (!etEmail.getText().toString().equals(originalUser.getEmail())) changed = true;
        if (!etPhone.getText().toString().equals(originalUser.getPhone())) changed = true;
        
        String gender = actvGender.getText().toString();
        if (originalUser.getGender() != null && !gender.equals(originalUser.getGender())) changed = true;
        
        if ("Staff".equals(originalUser.getUserType())) {
            String designation = etDesignation.getText().toString();
            if (originalUser.getDesignation() != null && !designation.equals(originalUser.getDesignation())) changed = true;
        } else {
            if (!etBatch.getText().toString().equals(originalUser.getBatch())) changed = true;
            if (!actvLevelTerm.getText().toString().equals(originalUser.getLevelTerm())) changed = true;
            if (!etDepartment.getText().toString().equals(originalUser.getDepartment())) changed = true;
            if (!actvSection.getText().toString().equals(originalUser.getSection())) changed = true;
        }
        
        if (!profileImageUris.isEmpty()) changed = true;

        btnSaveChanges.setVisibility(changed ? View.VISIBLE : View.GONE);
    }

    private void loadUserData() {
        showLoading(true);
        mDatabase.child("Users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                showLoading(false);
                if (snapshot.exists()) {
                    originalUser = snapshot.getValue(User.class);
                    if (originalUser != null) {
                        isDataLoaded = false;
                        etFullName.setText(originalUser.getName());
                        etUniversityId.setText(originalUser.getUniversityId());
                        etEmail.setText(originalUser.getEmail());
                        etPhone.setText(originalUser.getPhone());
                        actvGender.setText(originalUser.getGender(), false);
                        
                        if ("Staff".equals(originalUser.getUserType())) {
                            tilDesignation.setVisibility(View.VISIBLE);
                            etDesignation.setText(originalUser.getDesignation());
                            
                            tilBatch.setVisibility(View.GONE);
                            tilLevelTerm.setVisibility(View.GONE);
                            tilDepartment.setVisibility(View.GONE);
                            tilSection.setVisibility(View.GONE);
                        } else {
                            tilDesignation.setVisibility(View.GONE);
                            etBatch.setText(originalUser.getBatch());
                            actvLevelTerm.setText(originalUser.getLevelTerm(), false);
                            etDepartment.setText(originalUser.getDepartment());
                            actvSection.setText(originalUser.getSection(), false);
                        }
                        
                        if (originalUser.getProfileImageUrl() != null && !originalUser.getProfileImageUrl().isEmpty()) {
                            Glide.with(UserProfileActivity.this)
                                    .load(originalUser.getProfileImageUrl())
                                    .placeholder(R.drawable.ic_user)
                                    .circleCrop()
                                    .into(ivProfilePicture);
                        }
                        
                        isDataLoaded = true;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(UserProfileActivity.this, "Failed to load data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAllChanges() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String gender = actvGender.getText().toString().trim();

        if (!validateInputs(name, email, phone)) return;

        showLoading(true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("email", email);
        updates.put("phone", phone);
        updates.put("gender", gender);

        if ("Staff".equals(originalUser.getUserType())) {
            updates.put("designation", etDesignation.getText().toString().trim());
        } else {
            updates.put("batch", etBatch.getText().toString().trim());
            updates.put("levelTerm", actvLevelTerm.getText().toString().trim());
            updates.put("department", etDepartment.getText().toString().trim());
            updates.put("section", actvSection.getText().toString().trim());
        }

        if (!profileImageUris.isEmpty()) {
            uploadImagesAndFinishUpdate(updates);
        } else {
            finalizeDatabaseUpdate(updates);
        }
    }

    private void uploadImagesAndFinishUpdate(Map<String, Object> updates) {
        if (profileImageUris.isEmpty()) return;
        
        Uri uri = profileImageUris.get(0);
        String fileName = currentUserId + "_" + System.currentTimeMillis() + ".jpg";

        SupabaseStorageHelper.uploadImage(this, uri, "profiles", fileName, new SupabaseStorageHelper.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                updates.put("profileImageUrl", publicUrl);
                finalizeDatabaseUpdate(updates);
            }

            @Override
            public void onFailure(Exception e) {
                showLoading(false);
                Toast.makeText(UserProfileActivity.this, "Supabase Upload Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void finalizeDatabaseUpdate(Map<String, Object> updates) {
        mDatabase.child("Users").child(currentUserId).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        resetUIState();
                        Snackbar.make(findViewById(android.R.id.content), "Profile updated successfully", Snackbar.LENGTH_LONG).show();
                        loadUserData();
                    } else {
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void resetUIState() {
        etFullName.setEnabled(false);
        etEmail.setEnabled(false);
        etPhone.setEnabled(false);
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
        btnSaveChanges.setVisibility(View.GONE);
    }

    private boolean validateInputs(String name, String email, String phone) {
        boolean valid = true;
        if (TextUtils.isEmpty(name)) {
            tilFullName.setError("Name required");
            valid = false;
        } else tilFullName.setError(null);

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email");
            valid = false;
        } else tilEmail.setError(null);

        if (phone.length() < 10) {
            tilPhone.setError("Invalid phone");
            valid = false;
        } else tilPhone.setError(null);

        return valid;
    }

    private void reauthenticateAndChangePassword(String oldPass, String newPass, Runnable onComplete) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && userEmail != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(userEmail, oldPass);
            user.reauthenticate(credential).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    user.updatePassword(newPass).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            onComplete.run();
                        } else {
                            showLoading(false);
                            Toast.makeText(this, "Password update failed in Auth: " + updateTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    showLoading(false);
                    tilOldPassword.setError("Incorrect password");
                }
            });
        }
    }

    private void showImageSourceDialog() {
        String[] options = {getString(R.string.take_photo), getString(R.string.choose_gallery), getString(R.string.cancel)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.select_profile_picture));
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermission();
            } else if (which == 1) {
                openGallery();
            }
        });
        builder.show();
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
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
                Toast.makeText(this, "Error occurred while creating file", Toast.LENGTH_SHORT).show();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
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
                ivProfilePicture.setImageURI(profileImageUris.get(0));
                checkForChanges();
            }
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSaveChanges.setEnabled(!show);
    }
}
