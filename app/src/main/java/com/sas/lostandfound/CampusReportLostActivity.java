package com.sas.lostandfound;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class CampusReportLostActivity extends AppCompatActivity {

    private TextInputEditText etItemName, etDateLost, etTimeLost, etManualLocation, etLocationDetails, etDescription, etProofOwnership, etConfidentialDetail, etContactName, etContactPhone;
    private AutoCompleteTextView actvCategory, actvLocation, actvPreferredContact;
    private TextInputLayout tilManualLocation;
    private MaterialCheckBox cbConfirm;
    private MaterialButton btnSubmit;
    private com.google.android.material.card.MaterialCardView uploadCard;
    private ImageView ivUploadedImage;
    private TextView tvUploadStatus;
    private Toolbar toolbar;

    private static final int PICK_IMAGES_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int CAMERA_PERMISSION_CODE = 100;

    private List<Uri> selectedImageUris = new ArrayList<>();
    private Uri cameraImageUri;
    private String currentPhotoPath;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_report_lost);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initializeViews();
        setupToolbar();
        setupDropdowns();
        setupPickers();
        fetchCurrentUserData();
        
        uploadCard.setOnClickListener(v -> showImageSourceDialog());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void initializeViews() {
        etItemName = findViewById(R.id.etItemName);
        actvCategory = findViewById(R.id.actvCategory);
        etDescription = findViewById(R.id.etDescription);
        
        etDateLost = findViewById(R.id.etDateLost);
        etTimeLost = findViewById(R.id.etTimeLost);
        actvLocation = findViewById(R.id.actvLocation);
        tilManualLocation = findViewById(R.id.tilManualLocation);
        etManualLocation = findViewById(R.id.etManualLocation);
        etLocationDetails = findViewById(R.id.etLocationDetails);
        
        etProofOwnership = findViewById(R.id.etProofOwnership);
        etConfidentialDetail = findViewById(R.id.etConfidentialDetail);
        
        etContactName = findViewById(R.id.etContactName);
        etContactPhone = findViewById(R.id.etContactPhone);
        actvPreferredContact = findViewById(R.id.actvPreferredContact);
        
        cbConfirm = findViewById(R.id.cbConfirm);
        btnSubmit = findViewById(R.id.btnSubmitReport);
        uploadCard = findViewById(R.id.uploadCard);
        ivUploadedImage = findViewById(R.id.ivUploadedImage);
        tvUploadStatus = findViewById(R.id.tvUploadStatus);
        toolbar = findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void fetchCurrentUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        mDatabase.child("Users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentUser = snapshot.getValue(User.class);
                    if (currentUser != null) {
                        etContactName.setText(currentUser.getName());
                        etContactPhone.setText(currentUser.getPhone());
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
        });
    }

    private void setupDropdowns() {
        String[] categories = {"Electronics", "ID Cards", "Bags", "Documents", "Mobile Phone", "Accessories", "Others"};
        actvCategory.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, categories));

        String[] locations = {"Academic Building", "Library", "Cafeteria", "Playground", "Lab Room", "Dormitory", "Other"};
        actvLocation.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, locations));
        actvLocation.setOnItemClickListener((parent, view, position, id) -> {
            if (locations[position].equals("Other")) {
                tilManualLocation.setVisibility(View.VISIBLE);
            } else {
                tilManualLocation.setVisibility(View.GONE);
            }
        });

        String[] contactMethods = {"Phone", "Email", "In-app chat"};
        actvPreferredContact.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, contactMethods));
    }

    private void setupPickers() {
        etDateLost.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        String selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", dayOfMonth, month + 1, year);
                        etDateLost.setText(selectedDate);
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        etTimeLost.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                    (view, hourOfDay, minute) -> {
                        String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                        etTimeLost.setText(selectedTime);
                    }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
            timePickerDialog.show();
        });
    }

    private void showImageSourceDialog() {
        String[] options = {getString(R.string.take_photo), getString(R.string.choose_gallery), getString(R.string.cancel)};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.label_upload_image));
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) checkCameraPermission();
            else if (which == 1) openGallery();
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
            try { photoFile = createImageFile(); }
            catch (IOException ex) { Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show(); }
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
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
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGES_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGES_REQUEST) {
                selectedImageUris.clear();
                if (data != null) {
                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            selectedImageUris.add(clipData.getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null) {
                        selectedImageUris.add(data.getData());
                    }
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (cameraImageUri != null) {
                    selectedImageUris.add(cameraImageUri);
                }
            }
            
            if (!selectedImageUris.isEmpty()) {
                ivUploadedImage.setImageResource(R.drawable.ic_check_circle);
                ivUploadedImage.setColorFilter(ContextCompat.getColor(this, R.color.primaryColor));
                String status = selectedImageUris.size() + " Image(s) Selected";
                tvUploadStatus.setText(status);
            }
        }
    }

    private void validateAndSubmit() {
        String name = etItemName.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = etDateLost.getText().toString().trim();
        String location = actvLocation.getText().toString().trim();
        String phone = etContactPhone.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { etItemName.setError("Required"); return; }
        if (TextUtils.isEmpty(category)) { actvCategory.setError("Required"); return; }
        if (TextUtils.isEmpty(description)) { etDescription.setError("Required"); return; }
        if (TextUtils.isEmpty(date)) { etDateLost.setError("Required"); return; }
        if (TextUtils.isEmpty(location)) { actvLocation.setError("Required"); return; }
        if (TextUtils.isEmpty(phone)) { etContactPhone.setError("Required"); return; }
        if (!cbConfirm.isChecked()) { Toast.makeText(this, "Please confirm accuracy", Toast.LENGTH_SHORT).show(); return; }

        if (location.equals("Other")) {
            location = etManualLocation.getText().toString().trim();
            if (TextUtils.isEmpty(location)) { etManualLocation.setError("Required"); return; }
        }

        submitReport(name, category, description, date, location);
    }

    private void submitReport(String name, String category, String description, String date, String location) {
        btnSubmit.setEnabled(false);
        Toast.makeText(this, "Uploading images and submitting report...", Toast.LENGTH_SHORT).show();
        
        DatabaseReference lostRef = mDatabase.child("LostItems");
        String itemId = lostRef.push().getKey();
        String currentUserId = mAuth.getUid();

        if (itemId == null || currentUserId == null) {
            btnSubmit.setEnabled(true);
            Toast.makeText(this, "Error: Could not generate ID", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!selectedImageUris.isEmpty()) {
            List<String> imageUrlStrings = new ArrayList<>();
            AtomicInteger remaining = new AtomicInteger(selectedImageUris.size());
            
            for (int i = 0; i < selectedImageUris.size(); i++) {
                String fileName = itemId + "_" + i + "_" + System.currentTimeMillis() + ".jpg";
                SupabaseStorageHelper.uploadImage(this, selectedImageUris.get(i), "lost_items", fileName, new SupabaseStorageHelper.UploadCallback() {
                    @Override
                    public void onSuccess(String publicUrl) {
                        imageUrlStrings.add(publicUrl);
                        if (remaining.decrementAndGet() == 0) {
                            saveToDatabase(itemId, name, category, description, date, location, imageUrlStrings, currentUserId);
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (remaining.decrementAndGet() == 0) {
                            saveToDatabase(itemId, name, category, description, date, location, imageUrlStrings, currentUserId);
                        }
                    }
                });
            }
        } else {
            saveToDatabase(itemId, name, category, description, date, location, new ArrayList<>(), currentUserId);
        }
    }

    private void saveToDatabase(String itemId, String name, String category, String description, String date, String location, List<String> imageUrls, String userId) {
        Item item = new Item(itemId, name, category, description, location, date, "lost", userId);
        item.setTime(etTimeLost.getText().toString().trim());
        item.setAdditionalLocationDetails(etLocationDetails.getText().toString().trim());
        item.setImageUrls(imageUrls);
        if (!imageUrls.isEmpty()) {
            item.setImageUrl(imageUrls.get(0));
        }
        item.setConfidentialIdentificationDetail(etConfidentialDetail.getText().toString().trim());
        item.setPreferredContactMethod(actvPreferredContact.getText().toString().trim());
        item.setUserPhone(etContactPhone.getText().toString().trim());
        
        if (currentUser != null) {
            item.setUserName(currentUser.getName());
            item.setUserEmail(currentUser.getEmail());
            item.setUserUniversityId(currentUser.getUniversityId());
            item.setUserDepartment(currentUser.getDepartment());
        }

        mDatabase.child("LostItems").child(itemId).setValue(item).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                mDatabase.child("UserItems").child(userId).child(itemId).setValue(true);
                Toast.makeText(this, R.string.success_report_submitted, Toast.LENGTH_SHORT).show();
                finish();
            } else {
                btnSubmit.setEnabled(true);
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                Toast.makeText(this, "Database Error: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
