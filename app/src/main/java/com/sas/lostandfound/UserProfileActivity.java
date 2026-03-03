package com.sas.lostandfound;

import android.Manifest;
import android.app.AlertDialog;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private ImageView ivProfilePicture;
    private FloatingActionButton fabChangePhoto;
    private TextInputLayout tilEmail, tilPhone, tilDepartment, tilGender, tilBatch, tilLevelTerm, tilSection, tilOldPassword, tilNewPassword;
    private TextInputEditText etEmail, etPhone, etDepartment, etFullName, etUniversityId, etBatch, etOldPassword, etNewPassword;
    private AutoCompleteTextView actvGender, actvLevelTerm, actvSection;
    private MaterialButton btnSaveChanges;
    private ProgressBar progressBar;
    private Toolbar toolbar;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int CAMERA_PERMISSION_CODE = 100;

    private Uri profileImageUri = null;
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
        mStorageRef = FirebaseStorage.getInstance().getReference("profile_pictures");

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        currentUserId = user.getUid();
        userEmail = user.getEmail();

        initializeViews();
        setupDropdowns();
        loadUserData();

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        fabChangePhoto.setOnClickListener(v -> showImageSourceDialog());

        setupEditableToggles();
        setupChangeDetection();

        btnSaveChanges.setOnClickListener(v -> saveAllChanges());
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        fabChangePhoto = findViewById(R.id.fabChangePhoto);
        progressBar = findViewById(R.id.progressBar);

        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilGender = findViewById(R.id.tilGender);
        tilBatch = findViewById(R.id.tilBatch);
        tilLevelTerm = findViewById(R.id.tilLevelTerm);
        tilDepartment = findViewById(R.id.tilDepartment);
        tilSection = findViewById(R.id.tilSection);
        tilOldPassword = findViewById(R.id.tilOldPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);

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

        btnSaveChanges = findViewById(R.id.btnSaveChanges);
    }

    private void setupDropdowns() {
        String[] genders = {"Male", "Female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        actvGender.setAdapter(genderAdapter);
        actvGender.setOnClickListener(v -> { if(actvGender.isEnabled()) actvGender.showDropDown(); });

        String[] levels = {
                "Level 1 Term I", "Level 1 Term II",
                "Level 2 Term I", "Level 2 Term II",
                "Level 3 Term I", "Level 3 Term II",
                "Level 4 Term I", "Level 4 Term II"
        };
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, levels);
        actvLevelTerm.setAdapter(levelAdapter);
        actvLevelTerm.setOnClickListener(v -> { if(actvLevelTerm.isEnabled()) actvLevelTerm.showDropDown(); });

        String[] sections = {"A", "B", "C", "D"};
        ArrayAdapter<String> sectionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sections);
        actvSection.setAdapter(sectionAdapter);
        actvSection.setOnClickListener(v -> { if(actvSection.isEnabled()) actvSection.showDropDown(); });
    }

    private void setupEditableToggles() {
        setupToggle(tilEmail, etEmail);
        setupToggle(tilPhone, etPhone);
        setupToggle(tilGender, actvGender);
        setupToggle(tilBatch, etBatch);
        setupToggle(tilLevelTerm, actvLevelTerm);
        setupToggle(tilDepartment, etDepartment);
        setupToggle(tilSection, actvSection);
    }

    private void setupToggle(TextInputLayout til, View field) {
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

        etEmail.addTextChangedListener(watcher);
        etPhone.addTextChangedListener(watcher);
        actvGender.addTextChangedListener(watcher);
        etBatch.addTextChangedListener(watcher);
        actvLevelTerm.addTextChangedListener(watcher);
        etDepartment.addTextChangedListener(watcher);
        actvSection.addTextChangedListener(watcher);
        etOldPassword.addTextChangedListener(watcher);
        etNewPassword.addTextChangedListener(watcher);
    }

    private void checkForChanges() {
        if (originalUser == null) return;

        boolean changed = false;
        
        if (!etEmail.getText().toString().equals(originalUser.getEmail())) changed = true;
        if (!etPhone.getText().toString().equals(originalUser.getPhone())) changed = true;
        if (!actvGender.getText().toString().equals(originalUser.getGender())) changed = true;
        if (!etBatch.getText().toString().equals(originalUser.getBatch())) changed = true;
        if (!actvLevelTerm.getText().toString().equals(originalUser.getLevelTerm())) changed = true;
        if (!etDepartment.getText().toString().equals(originalUser.getDepartment())) changed = true;
        if (!actvSection.getText().toString().equals(originalUser.getSection())) changed = true;
        
        if (!TextUtils.isEmpty(etOldPassword.getText()) || !TextUtils.isEmpty(etNewPassword.getText())) changed = true;
        if (profileImageUri != null) changed = true;

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
                        etBatch.setText(originalUser.getBatch());
                        actvLevelTerm.setText(originalUser.getLevelTerm(), false);
                        etDepartment.setText(originalUser.getDepartment());
                        actvSection.setText(originalUser.getSection(), false);
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
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String gender = actvGender.getText().toString().trim();
        String batch = etBatch.getText().toString().trim();
        String levelTerm = actvLevelTerm.getText().toString().trim();
        String dept = etDepartment.getText().toString().trim();
        String section = actvSection.getText().toString().trim();
        String oldPass = etOldPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();

        if (!validateInputs(email, phone, oldPass, newPass)) return;

        showLoading(true);

        if (!TextUtils.isEmpty(oldPass) && !TextUtils.isEmpty(newPass)) {
            reauthenticateAndChangePassword(oldPass, newPass, () -> performDataUpdate(email, phone, gender, batch, levelTerm, dept, section));
        } else {
            performDataUpdate(email, phone, gender, batch, levelTerm, dept, section);
        }
    }

    private void performDataUpdate(String email, String phone, String gender, String batch, String levelTerm, String dept, String section) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("email", email);
        updates.put("phone", phone);
        updates.put("gender", gender);
        updates.put("batch", batch);
        updates.put("levelTerm", levelTerm);
        updates.put("department", dept);
        updates.put("section", section);

        if (profileImageUri != null) {
            uploadImageAndFinishUpdate(updates);
        } else {
            finalizeDatabaseUpdate(updates);
        }
    }

    private void uploadImageAndFinishUpdate(Map<String, Object> updates) {
        StorageReference fileRef = mStorageRef.child(currentUserId + ".jpg");
        fileRef.putFile(profileImageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    updates.put("profileImageUrl", uri.toString());
                    finalizeDatabaseUpdate(updates);
                }))
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        etEmail.setEnabled(false);
        etPhone.setEnabled(false);
        actvGender.setEnabled(false);
        etBatch.setEnabled(false);
        actvLevelTerm.setEnabled(false);
        etDepartment.setEnabled(false);
        actvSection.setEnabled(false);
        etOldPassword.setText("");
        etNewPassword.setText("");
        profileImageUri = null;
        btnSaveChanges.setVisibility(View.GONE);
    }

    private boolean validateInputs(String email, String phone, String oldPass, String newPass) {
        boolean valid = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email");
            valid = false;
        } else tilEmail.setError(null);

        if (phone.length() < 10) {
            tilPhone.setError("Invalid phone");
            valid = false;
        } else tilPhone.setError(null);

        if (!TextUtils.isEmpty(oldPass) || !TextUtils.isEmpty(newPass)) {
            if (TextUtils.isEmpty(oldPass)) {
                tilOldPassword.setError("Old password required");
                valid = false;
            } else tilOldPassword.setError(null);

            if (newPass.length() < 6) {
                tilNewPassword.setError("Min 6 characters required");
                valid = false;
            } else tilNewPassword.setError(null);
        }
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
                            Toast.makeText(this, "Password update failed", Toast.LENGTH_SHORT).show();
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
                Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK);
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
                profileImageUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, profileImageUri);
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
            if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
                profileImageUri = data.getData();
                ivProfilePicture.setImageURI(profileImageUri);
                checkForChanges();
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                ivProfilePicture.setImageURI(profileImageUri);
                checkForChanges();
            }
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSaveChanges.setEnabled(!show);
    }
}
