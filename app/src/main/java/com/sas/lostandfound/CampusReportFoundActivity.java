package com.sas.lostandfound;

import android.Manifest;
import android.app.AlertDialog;
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
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
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

public class CampusReportFoundActivity extends AppCompatActivity {

    private TextInputEditText etItemName, etDateFound, etTimeFound, etManualLocation, etLocationDetails, etDescription, etAuthorityName, etOfficeRoom, etHiddenQuestion, etContactName, etContactPhone;
    private AutoCompleteTextView actvCategory, actvLocation, actvHandlingStatus, actvPreferredContact;
    private TextInputLayout tilManualLocation, tilAuthorityName, tilOfficeRoom;
    private MaterialCheckBox cbConfirm;
    private MaterialButton btnSubmit;
    private com.google.android.material.card.MaterialCardView uploadCard;
    private ImageView ivUploadedImage;
    private TextView tvUploadPlaceholder;
    private Uri photoUri;
    private List<Uri> selectedImages = new ArrayList<>();
    private static final int PICK_IMAGE_MULTIPLE = 1;
    private static final int CAPTURE_IMAGE = 2;
    private static final int REQUEST_PERMISSIONS = 100;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";
    private String currentUniversityId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_report_found);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initViews();
        setupToolbar();
        setupDropdowns();
        setupPickers();
        fetchCurrentUserData();

        btnSubmit.setOnClickListener(v -> validateAndSubmit());
        uploadCard.setOnClickListener(v -> showImageSourceDialog());
    }

    private void initViews() {
        etItemName = findViewById(R.id.etItemName);
        etDateFound = findViewById(R.id.etDateFound);
        etTimeFound = findViewById(R.id.etTimeFound);
        actvCategory = findViewById(R.id.actvCategory);
        actvLocation = findViewById(R.id.actvLocation);
        etManualLocation = findViewById(R.id.etManualLocation);
        tilManualLocation = findViewById(R.id.tilManualLocation);
        etLocationDetails = findViewById(R.id.etLocationDetails);
        etDescription = findViewById(R.id.etDescription);
        actvHandlingStatus = findViewById(R.id.actvHandlingStatus);
        tilAuthorityName = findViewById(R.id.tilAuthorityName);
        etAuthorityName = findViewById(R.id.etAuthorityName);
        tilOfficeRoom = findViewById(R.id.tilOfficeRoom);
        etOfficeRoom = findViewById(R.id.etOfficeRoom);
        etHiddenQuestion = findViewById(R.id.etHiddenQuestion);
        etContactName = findViewById(R.id.etContactName);
        etContactPhone = findViewById(R.id.etContactPhone);
        actvPreferredContact = findViewById(R.id.actvPreferredContact);
        cbConfirm = findViewById(R.id.cbConfirm);
        btnSubmit = findViewById(R.id.btnSubmitReport);
        uploadCard = findViewById(R.id.uploadCard);
        ivUploadedImage = findViewById(R.id.ivUploadedImage);
        tvUploadPlaceholder = findViewById(R.id.tvUploadStatus);
    }

    private void fetchCurrentUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String authUid = mAuth.getCurrentUser().getUid();

        mDatabase.child("UIDToUniversityID").child(authUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    currentUniversityId = snapshot.getValue(String.class);
                    mDatabase.child("Users").child(currentUniversityId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                            if (userSnapshot.exists()) {
                                String name = userSnapshot.child("name").getValue(String.class);
                                String phone = userSnapshot.child("phone").getValue(String.class);
                                if (name != null) etContactName.setText(name);
                                if (phone != null) etContactPhone.setText(phone);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {}
                    });
                } else {
                    currentUniversityId = authUid;
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Report Found Item");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupDropdowns() {
        String[] categories = {"Electronics", "Documents", "Keys", "Wallets/Bags", "Clothing", "Books", "Other"};
        actvCategory.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, categories));

        String[] locations = {"Admin Building", "Library", "Cafeteria", "Sports Complex", "Block A", "Block B", "Block C", "Parking Lot", "Other"};
        actvLocation.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, locations));

        actvLocation.setOnItemClickListener((parent, view, position, id) -> {
            if ("Other".equals(locations[position])) {
                tilManualLocation.setVisibility(View.VISIBLE);
            } else {
                tilManualLocation.setVisibility(View.GONE);
                etManualLocation.setText("");
            }
        });

        String[] handlingStatuses = {"Handed over to authorities", "Keeping it with me"};
        actvHandlingStatus.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, handlingStatuses));

        actvHandlingStatus.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                tilAuthorityName.setVisibility(View.VISIBLE);
                tilOfficeRoom.setVisibility(View.VISIBLE);
            } else {
                tilAuthorityName.setVisibility(View.GONE);
                tilOfficeRoom.setVisibility(View.GONE);
                etAuthorityName.setText("");
                etOfficeRoom.setText("");
            }
        });

        String[] contactMethods = {"Phone", "Email", "In-app chat"};
        actvPreferredContact.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, contactMethods));
    }

    private void setupPickers() {
        etDateFound.setOnClickListener(v -> {
            CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
            constraintsBuilder.setValidator(DateValidatorPointBackward.now());

            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date")
                    .setCalendarConstraints(constraintsBuilder.build())
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .setTheme(R.style.ThemeOverlay_App_DatePicker)
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                etDateFound.setText(sdf.format(new Date(selection)));
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        etTimeFound.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            MaterialTimePicker picker = new MaterialTimePicker.Builder()
                    .setTimeFormat(TimeFormat.CLOCK_12H)
                    .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                    .setMinute(calendar.get(Calendar.MINUTE))
                    .setTitleText("Select Time")
                    .setTheme(R.style.ThemeOverlay_App_TimePicker)
                    .build();

            picker.addOnPositiveButtonClickListener(v1 -> {
                Calendar time = Calendar.getInstance();
                time.set(Calendar.HOUR_OF_DAY, picker.getHour());
                time.set(Calendar.MINUTE, picker.getMinute());
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                etTimeFound.setText(sdf.format(time.getTime()));
            });

            picker.show(getSupportFragmentManager(), "TIME_PICKER");
        });
    }

    private void showImageSourceDialog() {
        String[] options = {getString(R.string.take_photo), getString(R.string.choose_gallery), getString(R.string.cancel)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.label_upload_image)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        checkPermissionAndCamera();
                    } else if (which == 1) {
                        checkPermissionAndGallery();
                    }
                })
                .show();
    }

    private void checkPermissionAndCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
        } else {
            launchCamera();
        }
    }

    private void checkPermissionAndGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_MULTIPLE);
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this, "com.sas.lostandfound.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, CAPTURE_IMAGE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE_MULTIPLE && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    for (int i = 0; i < count; i++) {
                        selectedImages.add(data.getClipData().getItemAt(i).getUri());
                    }
                } else if (data.getData() != null) {
                    selectedImages.add(data.getData());
                }
                updateImagePreview();
            } else if (requestCode == CAPTURE_IMAGE) {
                selectedImages.add(photoUri);
                updateImagePreview();
            }
        }
    }

    private void updateImagePreview() {
        if (!selectedImages.isEmpty()) {
            ivUploadedImage.setImageURI(selectedImages.get(selectedImages.size() - 1));
            ivUploadedImage.setVisibility(View.VISIBLE);
            tvUploadPlaceholder.setText(getString(R.string.images_selected_format, selectedImages.size()));
        }
    }

    private void validateAndSubmit() {
        String itemName = etItemName.getText().toString().trim();
        String date = etDateFound.getText().toString().trim();
        String time = etTimeFound.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String location = actvLocation.getText().toString().trim();
        String manualLocation = etManualLocation.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String contactName = etContactName.getText().toString().trim();
        String contactPhone = etContactPhone.getText().toString().trim();
        String preferredContact = actvPreferredContact.getText().toString().trim();

        if (TextUtils.isEmpty(itemName) || TextUtils.isEmpty(date) || TextUtils.isEmpty(category) ||
                TextUtils.isEmpty(location) || TextUtils.isEmpty(description) ||
                TextUtils.isEmpty(contactName) || TextUtils.isEmpty(contactPhone)) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("Other".equals(location) && TextUtils.isEmpty(manualLocation)) {
            Toast.makeText(this, "Please specify the location", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!cbConfirm.isChecked()) {
            Toast.makeText(this, "Please confirm the information is accurate", Toast.LENGTH_SHORT).show();
            return;
        }

        submitToFirebase(itemName, date, time, category, location, manualLocation, description, contactName, contactPhone, preferredContact);
    }

    private void submitToFirebase(String itemName, String date, String time, String category,
                                  String location, String manualLocation, String description,
                                  String contactName, String contactPhone, String preferredContact) {
        btnSubmit.setEnabled(false);
        String reportId = mDatabase.child("FoundItems").push().getKey();
        
        if (reportId == null || currentUniversityId == null) {
            btnSubmit.setEnabled(true);
            Toast.makeText(this, "Error initializing submission", Toast.LENGTH_SHORT).show();
            return;
        }

        Item report = new Item(reportId, itemName, category, description,
                "Other".equals(location) ? manualLocation : location,
                date, "found", currentUniversityId);
        report.setTime(time);
        report.setAdditionalLocationDetails(etLocationDetails.getText().toString().trim());
        report.setItemHandlingStatus(actvHandlingStatus.getText().toString());
        report.setAuthorityName(etAuthorityName.getText().toString().trim());
        report.setOfficeRoomNumber(etOfficeRoom.getText().toString().trim());
        report.setHiddenIdentificationQuestion(etHiddenQuestion.getText().toString().trim());
        report.setUserName(contactName);
        report.setUserPhone(contactPhone);
        report.setPreferredContactMethod(preferredContact);

        if (selectedImages.isEmpty()) {
            saveReport(reportId, report);
        } else {
            uploadImagesAndSave(reportId, report);
        }
    }

    private void uploadImagesAndSave(String reportId, Item report) {
        List<String> imageUrls = new ArrayList<>();
        com.google.firebase.storage.StorageReference storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().getReference().child("found_item_images/" + reportId);

        AtomicInteger uploadedCount = new AtomicInteger(0);
        for (int i = 0; i < selectedImages.size(); i++) {
            com.google.firebase.storage.StorageReference fileRef = storageRef.child("image_" + i + ".jpg");
            fileRef.putFile(selectedImages.get(i)).addOnSuccessListener(taskSnapshot -> {
                fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    imageUrls.add(uri.toString());
                    if (uploadedCount.incrementAndGet() == selectedImages.size()) {
                        report.setImageUrls(imageUrls);
                        saveReport(reportId, report);
                    }
                });
            });
        }
    }

    private void saveReport(String reportId, Item report) {
        mDatabase.child("FoundItems").child(reportId).setValue(report)
                .addOnSuccessListener(aVoid -> {
                    mDatabase.child("UserItems").child(currentUniversityId).child(reportId).setValue(true);
                    Toast.makeText(this, "Report submitted successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmit.setEnabled(true);
                    Toast.makeText(this, "Failed to submit: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
