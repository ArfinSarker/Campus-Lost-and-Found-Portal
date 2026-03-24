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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class CampusReportLostActivity extends AppCompatActivity {

    private TextInputEditText etItemName, etDateLost, etTimeLost, etManualLocation, etLocationDetails, etDescription, etProofOwnership, etContactName, etContactPhone;
    private AutoCompleteTextView actvCategory, actvLocation, actvPreferredContact;
    private TextInputLayout tilItemName, tilCategory, tilDescription, tilDate, tilTime, tilLocation, tilManualLocation, tilContactName, tilContactPhone, tilPreferredContact;
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

    private String currentUniversityId;
    private User currentUser;
    
    private boolean isEditMode = false;
    private String editItemId;
    private Item existingItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_report_lost);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        if (mAuth.getCurrentUser() != null) {
            currentUniversityId = mAuth.getCurrentUser().getUid();
        }

        initializeViews();
        setupToolbar();
        setupDropdowns();
        setupPickers();
        setupTextWatchers();

        editItemId = getIntent().getStringExtra("editItemId");
        if (editItemId != null) {
            isEditMode = true;
            loadItemDataForEdit(editItemId);
        } else {
            fetchCurrentUserData();
        }

        uploadCard.setOnClickListener(v -> showImageSourceDialog());
        btnSubmit.setOnClickListener(v -> validateAndSubmit());
    }

    private void initializeViews() {
        etItemName = findViewById(R.id.etItemName);
        tilItemName = findViewById(R.id.tilItemName);
        
        actvCategory = findViewById(R.id.actvCategory);
        tilCategory = findViewById(R.id.tilCategory);
        
        etDescription = findViewById(R.id.etDescription);
        tilDescription = findViewById(R.id.tilDescription);

        etDateLost = findViewById(R.id.etDateLost);
        tilDate = findViewById(R.id.tilDate);
        
        etTimeLost = findViewById(R.id.etTimeLost);
        tilTime = findViewById(R.id.tilTime);
        
        actvLocation = findViewById(R.id.actvLocation);
        tilLocation = findViewById(R.id.tilLocation);
        
        tilManualLocation = findViewById(R.id.tilManualLocation);
        etManualLocation = findViewById(R.id.etManualLocation);
        etLocationDetails = findViewById(R.id.etLocationDetails);

        etProofOwnership = findViewById(R.id.etProofOwnership);

        etContactName = findViewById(R.id.etContactName);
        tilContactName = findViewById(R.id.tilContactName);
        
        etContactPhone = findViewById(R.id.etContactPhone);
        tilContactPhone = findViewById(R.id.tilContactPhone);
        
        actvPreferredContact = findViewById(R.id.actvPreferredContact);
        tilPreferredContact = findViewById(R.id.tilPreferredContact);

        cbConfirm = findViewById(R.id.cbConfirm);
        btnSubmit = findViewById(R.id.btnSubmitReport);
        uploadCard = findViewById(R.id.uploadCard);
        ivUploadedImage = findViewById(R.id.ivUploadedImage);
        tvUploadStatus = findViewById(R.id.tvUploadStatus);
        toolbar = findViewById(R.id.toolbar);
    }

    private void loadItemDataForEdit(String itemId) {
        mDatabase.child("LostItems").child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                existingItem = snapshot.getValue(Item.class);
                if (existingItem != null) {
                    populateFields(existingItem);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void populateFields(Item item) {
        etItemName.setText(item.getName());
        actvCategory.setText(item.getCategory(), false);
        etDescription.setText(item.getDescription());
        etDateLost.setText(item.getDate());
        etTimeLost.setText(item.getTime());
        
        String location = item.getLocation();
        String[] predefinedLocations = {"Academic Building", "Civil Building", "Library", "Cafeteria", "Medical Center", "Playground", "Abbas Uddin Ahmed Hall (AUAH)", "Shaheed Dr. Zikrul Haque Hall", "Bir Protik Taramon Bibi Hall", "Bir Protik Taramon Bibi (New Hall)"};
        boolean isPredefined = false;
        for (String loc : predefinedLocations) {
            if (loc.equals(location)) {
                isPredefined = true;
                break;
            }
        }
        
        if (isPredefined) {
            actvLocation.setText(location, false);
        } else {
            actvLocation.setText("Other", false);
            tilManualLocation.setVisibility(View.VISIBLE);
            etManualLocation.setText(location);
        }
        
        etLocationDetails.setText(item.getAdditionalLocationDetails());
        etProofOwnership.setText(item.getProofOfOwnershipDetail());
        etContactName.setText(item.getUserName());
        etContactPhone.setText(item.getUserPhone());
        actvPreferredContact.setText(item.getPreferredContactMethod(), false);
        
        currentUniversityId = item.getUserId();
        btnSubmit.setText("Save Changes");
        
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            tvUploadStatus.setText("Previous images exist. Upload new to replace.");
        }
    }

    private void setupTextWatchers() {
        etItemName.addTextChangedListener(new SimpleTextWatcher(tilItemName));
        etDescription.addTextChangedListener(new SimpleTextWatcher(tilDescription));
        etManualLocation.addTextChangedListener(new SimpleTextWatcher(tilManualLocation));
        etContactName.addTextChangedListener(new SimpleTextWatcher(tilContactName));
        etContactPhone.addTextChangedListener(new SimpleTextWatcher(tilContactPhone));
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
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
                                currentUser = userSnapshot.getValue(User.class);
                                if (currentUser != null) {
                                    etContactName.setText(currentUser.getName());
                                    etContactPhone.setText(currentUser.getPhone());
                                }
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

    private void setupDropdowns() {
        String[] categories = {"Electronics & Gadgets", "ID Cards", "Wallets & Purses", "Bank/Credit Cards", "Bags", "Study Materials", "Eyewear", "Keys & Access Devices", "Clothing & Accessories", "Others"};
        actvCategory.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, categories));
        actvCategory.setOnItemClickListener((parent, view, position, id) -> tilCategory.setError(null));

        String[] locations = {"Academic Building", "Civil Building", "Library", "Cafeteria", "Medical Center", "Playground", "Abbas Uddin Ahmed Hall (AUAH)", "Shaheed Dr. Zikrul Haque Hall", "Bir Protik Taramon Bibi Hall", "Bir Protik Taramon Bibi (New Hall)", "Other"};
        actvLocation.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, locations));
        actvLocation.setOnItemClickListener((parent, view, position, id) -> {
            tilLocation.setError(null);
            if (locations[position].equals("Other")) {
                tilManualLocation.setVisibility(View.VISIBLE);
            } else {
                tilManualLocation.setVisibility(View.GONE);
                etManualLocation.setText("");
                tilManualLocation.setError(null);
            }
        });

        String[] contactMethods = {"Phone", "Email", "In-app chat"};
        actvPreferredContact.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, contactMethods));
        actvPreferredContact.setOnItemClickListener((parent, view, position, id) -> tilPreferredContact.setError(null));
    }

    private void setupPickers() {
        etDateLost.setOnClickListener(v -> {
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
                etDateLost.setText(sdf.format(new Date(selection)));
                tilDate.setError(null);
            });

            datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        });

        etTimeLost.setOnClickListener(v -> {
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
                etTimeLost.setText(sdf.format(time.getTime()));
            });

            picker.show(getSupportFragmentManager(), "TIME_PICKER");
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
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in again to submit", Toast.LENGTH_SHORT).show();
            return;
        }

        clearErrors();
        
        String name = etItemName.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = etDateLost.getText().toString().trim();
        String location = actvLocation.getText().toString().trim();
        String manualLocation = etManualLocation.getText().toString().trim();
        String contactName = etContactName.getText().toString().trim();
        String contactPhone = etContactPhone.getText().toString().trim();
        String preferredContact = actvPreferredContact.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(name)) { tilItemName.setError("Item name is required"); isValid = false; }
        if (TextUtils.isEmpty(category)) { tilCategory.setError("Category is required"); isValid = false; }
        if (TextUtils.isEmpty(description)) { tilDescription.setError("Description is required"); isValid = false; }
        if (TextUtils.isEmpty(date)) { tilDate.setError("Date is required"); isValid = false; }
        if (TextUtils.isEmpty(location)) { tilLocation.setError("Location is required"); isValid = false; }
        else if (location.equals("Other") && TextUtils.isEmpty(manualLocation)) { tilManualLocation.setError("Please specify location"); isValid = false; }
        
        if (TextUtils.isEmpty(contactName)) { tilContactName.setError("Your name is required"); isValid = false; }
        if (TextUtils.isEmpty(contactPhone)) { tilContactPhone.setError("Contact phone is required"); isValid = false; }
        if (TextUtils.isEmpty(preferredContact)) { tilPreferredContact.setError("Preferred contact is required"); isValid = false; }

        if (!cbConfirm.isChecked()) {
            Toast.makeText(this, "Please confirm the information is accurate", Toast.LENGTH_SHORT).show();
            isValid = false;
        }

        if (isValid) {
            submitReport(name, category, description, date, "Other".equals(location) ? manualLocation : location, contactName, contactPhone, preferredContact);
        }
    }

    private void clearErrors() {
        tilItemName.setError(null);
        tilCategory.setError(null);
        tilDescription.setError(null);
        tilDate.setError(null);
        tilLocation.setError(null);
        tilManualLocation.setError(null);
        tilContactName.setError(null);
        tilContactPhone.setError(null);
        tilPreferredContact.setError(null);
    }

    private void submitReport(String name, String category, String description, String date, String location, String contactName, String contactPhone, String preferredContact) {
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Submitting report, please wait...");
        Toast.makeText(this, "Submitting report, please wait...", Toast.LENGTH_SHORT).show();

        String itemId = isEditMode ? editItemId : mDatabase.child("LostItems").push().getKey();
        String userId = currentUniversityId != null ? currentUniversityId : (mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null);

        if (itemId == null || userId == null) {
            resetButton();
            Toast.makeText(this, "Failed to initialize submission. Try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!selectedImageUris.isEmpty()) {
            List<String> imageUrlStrings = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger remaining = new AtomicInteger(selectedImageUris.size());

            for (int i = 0; i < selectedImageUris.size(); i++) {
                String fileName = itemId + "_" + i + "_" + System.currentTimeMillis() + ".jpg";
                SupabaseStorageHelper.uploadImage(this, selectedImageUris.get(i), "lost_items", fileName, new SupabaseStorageHelper.UploadCallback() {
                    @Override
                    public void onSuccess(String publicUrl) {
                        imageUrlStrings.add(publicUrl);
                        if (remaining.decrementAndGet() == 0) {
                            if (isEditMode) {
                                saveToDatabase(itemId, name, category, description, date, location, contactName, contactPhone, preferredContact, imageUrlStrings, userId, null);
                            } else {
                                generateDisplayIdAndSave(itemId, name, category, description, date, location, contactName, contactPhone, preferredContact, imageUrlStrings, userId);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (remaining.decrementAndGet() == 0) {
                            if (isEditMode) {
                                saveToDatabase(itemId, name, category, description, date, location, contactName, contactPhone, preferredContact, imageUrlStrings, userId, null);
                            } else {
                                generateDisplayIdAndSave(itemId, name, category, description, date, location, contactName, contactPhone, preferredContact, imageUrlStrings, userId);
                            }
                        }
                    }
                });
            }
        } else {
            List<String> images = isEditMode && existingItem != null ? existingItem.getImageUrls() : new ArrayList<>();
            if (isEditMode) {
                saveToDatabase(itemId, name, category, description, date, location, contactName, contactPhone, preferredContact, images, userId, null);
            } else {
                generateDisplayIdAndSave(itemId, name, category, description, date, location, contactName, contactPhone, preferredContact, images, userId);
            }
        }
    }

    private void generateDisplayIdAndSave(String itemId, String name, String category, String description, String date, String location, String contactName, String contactPhone, String preferredContact, List<String> imageUrls, String userId) {
        DatabaseReference counterRef = mDatabase.child("Counters").child("LostItemsCount");
        counterRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                Object val = currentData.getValue();
                long count = 0;
                if (val instanceof Long) {
                    count = (Long) val;
                } else if (val instanceof Integer) {
                    count = ((Integer) val).longValue();
                } else if (val != null) {
                    try { count = Long.parseLong(val.toString()); } catch (Exception e) {}
                }
                currentData.setValue(count + 1);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (committed && currentData != null) {
                    Object val = currentData.getValue();
                    long count = 1;
                    if (val instanceof Long) count = (Long) val;
                    else if (val instanceof Integer) count = ((Integer) val).longValue();
                    
                    String displayId = "L" + count;
                    saveToDatabase(itemId, name, category, description, date, location, contactName, contactPhone, preferredContact, imageUrls, userId, displayId);
                } else {
                    resetButton();
                    String msg = error != null ? error.getMessage() : "Database busy or connection issue.";
                    Toast.makeText(CampusReportLostActivity.this, "Failed to generate Report ID: " + msg, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveToDatabase(String itemId, String name, String category, String description, String date, String location, String contactName, String contactPhone, String preferredContact, List<String> imageUrls, String userId, String displayId) {
        Item item;
        if (isEditMode && existingItem != null) {
            item = existingItem;
            item.setName(name);
            item.setCategory(category);
            item.setDescription(description);
            item.setLocation(location);
            item.setDate(date);
            item.setEdited(true);
        } else {
            item = new Item(itemId, name, category, description, location, date, "lost", userId);
            item.setDisplayId(displayId);
        }
        
        item.setTime(etTimeLost.getText().toString().trim());
        item.setAdditionalLocationDetails(etLocationDetails.getText().toString().trim());
        item.setProofOfOwnershipDetail(etProofOwnership.getText().toString().trim());
        item.setImageUrls(imageUrls);
        if (!imageUrls.isEmpty()) {
            item.setImageUrl(imageUrls.get(0));
        }
        item.setPreferredContactMethod(preferredContact);
        item.setUserName(contactName);
        item.setUserPhone(contactPhone);

        if (currentUser != null) {
            item.setUserEmail(currentUser.getEmail());
            item.setUserUniversityId(currentUser.getUniversityId());
            item.setUserDepartment(currentUser.getDepartment());
        }

        mDatabase.child("LostItems").child(itemId).setValue(item).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (!isEditMode) {
                    if (mAuth.getCurrentUser() != null) {
                        mDatabase.child("UserItems").child(mAuth.getCurrentUser().getUid()).child(itemId).setValue(true);
                    }
                }
                Toast.makeText(this, isEditMode ? "Report updated successfully" : getString(R.string.success_report_submitted), Toast.LENGTH_SHORT).show();
                finish();
            } else {
                resetButton();
                String error = task.getException() != null ? task.getException().getMessage() : "Unknown database error";
                Toast.makeText(this, "Failed to save report: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void resetButton() {
        btnSubmit.setEnabled(true);
        btnSubmit.setText(isEditMode ? "Save Changes" : "Submit Report");
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final TextInputLayout textInputLayout;

        public SimpleTextWatcher(TextInputLayout textInputLayout) {
            this.textInputLayout = textInputLayout;
        }

        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            textInputLayout.setError(null);
        }
        @Override public void afterTextChanged(Editable s) {}
    }
}
