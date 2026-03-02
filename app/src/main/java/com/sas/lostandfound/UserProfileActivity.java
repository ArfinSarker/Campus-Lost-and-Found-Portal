package com.sas.lostandfound;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class UserProfileActivity extends AppCompatActivity {

    private ImageView ivProfilePicture;
    private FloatingActionButton fabChangePhoto;
    private TextInputEditText etEmail, etPhone, etDepartment, etPassword, etFullName, etUniversityId, etBatch;
    private AutoCompleteTextView actvGender;
    private MaterialButton btnSaveProfile;
    private Toolbar toolbar;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private StorageReference mStorageRef;
    
    private static final int REQUEST_IMAGE_PICK = 2;
    private Uri profileImageUri = null;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference("profile_pictures");
        
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        currentUserId = user.getUid();

        initializeViews();
        setupGenderDropdown();
        loadUserData();

        toolbar.setNavigationOnClickListener(v -> onBackPressed());
        fabChangePhoto.setOnClickListener(v -> showImageSourceDialog());
        btnSaveProfile.setOnClickListener(v -> saveProfileChanges());
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        fabChangePhoto = findViewById(R.id.fabChangePhoto);
        
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etDepartment = findViewById(R.id.etDepartment);
        etPassword = findViewById(R.id.etPassword);
        
        etFullName = findViewById(R.id.etFullName);
        etUniversityId = findViewById(R.id.etUniversityId);
        etBatch = findViewById(R.id.etBatch);
        
        actvGender = findViewById(R.id.actvGender);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
    }

    private void setupGenderDropdown() {
        String[] genders = {"Male", "Female"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, genders);
        actvGender.setAdapter(adapter);
    }

    private void loadUserData() {
        mDatabase.child("Users").child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        etFullName.setText(user.getName());
                        etUniversityId.setText(user.getUniversityId());
                        etEmail.setText(user.getEmail());
                        etPhone.setText(user.getPhone());
                        etDepartment.setText(user.getDepartment());
                        etBatch.setText(user.getBatch());
                        actvGender.setText(user.getGender(), false);
                        
                        // Note: Password is not stored in DB, so field remains empty
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showImageSourceDialog() {
        String[] options = {"Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Profile Picture");
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

    private void saveProfileChanges() {
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String department = etDepartment.getText().toString().trim();
        String gender = actvGender.getText().toString().trim();
        String newPassword = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Required");
            return;
        }

        btnSaveProfile.setEnabled(false);
        Toast.makeText(this, "Updating profile...", Toast.LENGTH_SHORT).show();

        if (profileImageUri != null) {
            uploadImageAndUpdateData(email, phone, department, gender, newPassword);
        } else {
            updateDataInDatabase(null, email, phone, department, gender, newPassword);
        }
    }

    private void uploadImageAndUpdateData(String email, String phone, String department, String gender, String newPassword) {
        StorageReference fileRef = mStorageRef.child(currentUserId + ".jpg");
        fileRef.putFile(profileImageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    updateDataInDatabase(uri.toString(), email, phone, department, gender, newPassword);
                }))
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    updateDataInDatabase(null, email, phone, department, gender, newPassword);
                });
    }

    private void updateDataInDatabase(String imageUrl, String email, String phone, String department, String gender, String newPassword) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("email", email);
        updates.put("phone", phone);
        updates.put("department", department);
        updates.put("gender", gender);
        if (imageUrl != null) {
            updates.put("profileImageUrl", imageUrl);
        }

        mDatabase.child("Users").child(currentUserId).updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!TextUtils.isEmpty(newPassword)) {
                            updatePassword(newPassword);
                        } else {
                            onUpdateSuccess();
                        }
                    } else {
                        btnSaveProfile.setEnabled(true);
                        Toast.makeText(this, "Update failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updatePassword(String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.updatePassword(newPassword).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    onUpdateSuccess();
                } else {
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Password update failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void onUpdateSuccess() {
        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
        btnSaveProfile.setEnabled(true);
        finish();
    }
}