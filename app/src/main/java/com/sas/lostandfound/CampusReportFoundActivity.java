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
import com.google.gson.reflect.TypeToken;

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

public class CampusReportFoundActivity extends AppCompatActivity {

    private TextInputEditText etItemName, etDateFound, etTimeFound, etManualLocation, etLocationDetails, etDescription, etAuthorityName, etOfficeRoom, etHiddenQuestion, etContactName, etContactPhone;
    private AutoCompleteTextView actvCategory, actvLocation, actvHandlingStatus, actvPreferredContact;
    private TextInputLayout tilItemName, tilCategory, tilDescription, tilDate, tilLocation, tilManualLocation, tilHandlingStatus, tilAuthorityName, tilOfficeRoom, tilHiddenQuestion, tilContactName, tilContactPhone, tilPreferredContact;
    private MaterialCheckBox cbConfirm;
    private MaterialButton btnSubmit;
    private com.airbnb.lottie.LottieAnimationView loadingAnimation;
    private com.google.android.material.card.MaterialCardView uploadCard;
    private ImageView ivUploadedImage;
    private android.widget.TextView tvUploadPlaceholder;
    private Toolbar toolbar;

    private static final int PICK_IMAGES_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int CAMERA_PERMISSION_CODE = 100;

    private List<Uri> selectedImageUris = new ArrayList<>();
    private Uri cameraImageUri;
    private String currentPhotoPath;

    private String currentUniversityId;
    private User currentUser;

    private boolean isEditMode = false;
    private String editItemId;
    private Item existingItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_report_found);

        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUniversityId = prefs.getString("universityId", null);

        initViews();
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

        btnSubmit.setOnClickListener(v -> validateAndSubmit());
        uploadCard.setOnClickListener(v -> showImageSourceDialog());
    }

    private void initViews() {
        etItemName = findViewById(R.id.etItemName);
        tilItemName = findViewById(R.id.tilItemName);

        actvCategory = findViewById(R.id.actvCategory);
        tilCategory = findViewById(R.id.tilCategory);

        etDescription = findViewById(R.id.etDescription);
        tilDescription = findViewById(R.id.tilDescription);

        etDateFound = findViewById(R.id.etDateFound);
        tilDate = findViewById(R.id.tilDate);

        etTimeFound = findViewById(R.id.etTimeFound);

        actvLocation = findViewById(R.id.actvLocation);
        tilLocation = findViewById(R.id.tilLocation);

        etManualLocation = findViewById(R.id.etManualLocation);
        tilManualLocation = findViewById(R.id.tilManualLocation);

        etLocationDetails = findViewById(R.id.etLocationDetails);

        actvHandlingStatus = findViewById(R.id.actvHandlingStatus);
        tilHandlingStatus = findViewById(R.id.tilHandlingStatus);

        tilAuthorityName = findViewById(R.id.tilAuthorityName);
        etAuthorityName = findViewById(R.id.etAuthorityName);

        tilOfficeRoom = findViewById(R.id.tilOfficeRoom);
        etOfficeRoom = findViewById(R.id.etOfficeRoom);

        etHiddenQuestion = findViewById(R.id.etHiddenQuestion);
        tilHiddenQuestion = findViewById(R.id.tilHiddenQuestion);

        etContactName = findViewById(R.id.etContactName);
        tilContactName = findViewById(R.id.tilContactName);

        etContactPhone = findViewById(R.id.etContactPhone);
        tilContactPhone = findViewById(R.id.tilContactPhone);

        actvPreferredContact = findViewById(R.id.actvPreferredContact);
        tilPreferredContact = findViewById(R.id.tilPreferredContact);

        cbConfirm = findViewById(R.id.cbConfirm);
        btnSubmit = findViewById(R.id.btnSubmitReport);
        loadingAnimation = findViewById(R.id.loadingAnimation);
        uploadCard = findViewById(R.id.uploadCard);
        ivUploadedImage = findViewById(R.id.ivUploadedImage);
        tvUploadPlaceholder = findViewById(R.id.tvUploadStatus);
        toolbar = findViewById(R.id.toolbar);

        ErrorHelper.attachToTextInputLayout(tilItemName);
        ErrorHelper.attachToTextInputLayout(tilCategory);
        ErrorHelper.attachToTextInputLayout(tilDescription);
        ErrorHelper.attachToTextInputLayout(tilDate);
        ErrorHelper.attachToTextInputLayout(tilLocation);
        ErrorHelper.attachToTextInputLayout(tilManualLocation);
        ErrorHelper.attachToTextInputLayout(tilHandlingStatus);
        ErrorHelper.attachToTextInputLayout(tilAuthorityName);
        ErrorHelper.attachToTextInputLayout(tilOfficeRoom);
        ErrorHelper.attachToTextInputLayout(tilHiddenQuestion);
        ErrorHelper.attachToTextInputLayout(tilContactName);
        ErrorHelper.attachToTextInputLayout(tilContactPhone);
        ErrorHelper.attachToTextInputLayout(tilPreferredContact);
    }

    private void loadItemDataForEdit(String itemId) {
        SupabaseDatabaseHelper.select("found_reports", "id=eq." + itemId + "&limit=1", new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> items) {
                if (items != null && !items.isEmpty()) {
                    existingItem = items.get(0);
                    if (existingItem != null) {
                        populateFields(existingItem);
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                ErrorHelper.showError(btnSubmit, "Failed to load item for edit.");
            }
        });
    }

    private void populateFields(Item item) {
        etItemName.setText(item.getName());
        actvCategory.setText(item.getCategory(), false);
        etDescription.setText(item.getDescription());
        etDateFound.setText(item.getDate());
        etTimeFound.setText(item.getTime());

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
            etManualLocation.setText(item.getManualLocation());
        }

        etLocationDetails.setText(item.getAdditionalLocationDetails());

        actvHandlingStatus.setText(item.getItemHandlingStatus(), false);
        if ("Handed over to authorities".equals(item.getItemHandlingStatus())) {
            tilAuthorityName.setVisibility(View.VISIBLE);
            tilOfficeRoom.setVisibility(View.VISIBLE);
            etAuthorityName.setText(item.getAuthorityName());
            etOfficeRoom.setText(item.getOfficeRoomNumber());
        }

        etHiddenQuestion.setText(item.getHiddenIdentificationQuestion());
        etContactName.setText(item.getUserName());
        etContactPhone.setText(item.getUserPhone());
        actvPreferredContact.setText(item.getPreferredContactMethod(), false);

        currentUniversityId = item.getUserId();
        btnSubmit.setText("Save Changes");

        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            tvUploadPlaceholder.setText("Previous images exist. Upload new to replace.");
        }
    }

    private void setupTextWatchers() {
        // TextWatchers are now handled automatically by ErrorHelper.attachToTextInputLayout
    }

    private void fetchCurrentUserData() {
        if (currentUniversityId == null) return;

        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + currentUniversityId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users != null && !users.isEmpty()) {
                    currentUser = users.get(0);
                    if (currentUser != null) {
                        etContactName.setText(currentUser.getName());
                        etContactPhone.setText(currentUser.getPhone());
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {}
        });
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());

            com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
            if (appBarLayout != null) {
                HeaderColorHelper.setup(this, appBarLayout, toolbar);
            }
        }
    }

    private void setupDropdowns() {
        String[] categories = {"Electronics & Gadgets", "ID Cards", "Wallets & Purses", "Bank/Credit Cards", "Bags", "Study Materials", "Eyewear", "Keys & Access Devices", "Clothing & Accessories", "Others"};
        actvCategory.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item, categories));

        String[] locations = {"Academic Building", "Civil Building", "Library", "Cafeteria", "Medical Center", "Playground", "Abbas Uddin Ahmed Hall (AUAH)", "Shaheed Dr. Zikrul Haque Hall", "Bir Protik Taramon Bibi Hall", "Bir Protik Taramon Bibi (New Hall)", "Other"};
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
                tilDate.setError(null);
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
                    if (which == 0) checkCameraPermission();
                    else if (which == 1) openGallery();
                })
                .show();
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
            catch (IOException ex) { SnackbarManager.show(SnackbarManager.Type.ERROR, "Error creating file"); }
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
                tvUploadPlaceholder.setText(selectedImageUris.size() + " Image(s) Selected");
            }
        }
    }

    private void validateAndSubmit() {
        if (currentUniversityId == null) {
            ErrorHelper.showError(btnSubmit, "Please log in again to submit");
            return;
        }

        String itemName = etItemName.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = etDateFound.getText().toString().trim();
        String location = actvLocation.getText().toString().trim();
        String manualLocation = etManualLocation.getText().toString().trim();
        String handlingStatus = actvHandlingStatus.getText().toString().trim();
        String authorityName = etAuthorityName.getText().toString().trim();
        String hiddenQuestion = etHiddenQuestion.getText().toString().trim();
        String contactName = etContactName.getText().toString().trim();
        String contactPhone = etContactPhone.getText().toString().trim();
        String preferredContact = actvPreferredContact.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(itemName)) { ErrorHelper.setFieldError(tilItemName, "Item name is required"); isValid = false; }
        else if (TextUtils.isEmpty(category)) { ErrorHelper.setFieldError(tilCategory, "Category is required"); isValid = false; }
        else if (TextUtils.isEmpty(description)) { ErrorHelper.setFieldError(tilDescription, "Description is required"); isValid = false; }
        else if (TextUtils.isEmpty(date)) { ErrorHelper.setFieldError(tilDate, "Date is required"); isValid = false; }
        else if (TextUtils.isEmpty(location)) { ErrorHelper.setFieldError(tilLocation, "Location is required"); isValid = false; }
        else if ("Other".equals(location) && TextUtils.isEmpty(manualLocation)) { ErrorHelper.setFieldError(tilManualLocation, "Please specify location"); isValid = false; }
        
        else if (TextUtils.isEmpty(handlingStatus)) { ErrorHelper.setFieldError(tilHandlingStatus, "Status is required"); isValid = false; }
        else if (handlingStatus.equals("Handed over to authorities") && TextUtils.isEmpty(authorityName)) { ErrorHelper.setFieldError(tilAuthorityName, "Authority name is required"); isValid = false; }
        
        else if (TextUtils.isEmpty(hiddenQuestion)) { ErrorHelper.setFieldError(tilHiddenQuestion, "Security question is required"); isValid = false; }
        else if (TextUtils.isEmpty(contactName)) { ErrorHelper.setFieldError(tilContactName, "Your name is required"); isValid = false; }
        else if (TextUtils.isEmpty(contactPhone)) { ErrorHelper.setFieldError(tilContactPhone, "Contact phone is required"); isValid = false; }
        else if (!ValidationUtils.isValidPhone(contactPhone)) { ErrorHelper.setFieldError(tilContactPhone, "Please enter a valid phone number (11 digits)"); isValid = false; }
        else if (TextUtils.isEmpty(preferredContact)) { ErrorHelper.setFieldError(tilPreferredContact, "Preferred contact is required"); isValid = false; }

        if (isValid && !cbConfirm.isChecked()) {
            ErrorHelper.showError(btnSubmit, "Please confirm the information is accurate");
            isValid = false;
        }

        if (isValid) {
            submitReport(itemName, category, description, date, location, manualLocation, handlingStatus, authorityName, hiddenQuestion, contactName, contactPhone, preferredContact);
        }
    }

    private void submitReport(String itemName, String category, String description, String date, String location, String manualLocation, String handlingStatus, String authorityName, String hiddenQuestion, String contactName, String contactPhone, String preferredContact) {
        setLoadingState(true);

        String reportId = isEditMode ? editItemId : java.util.UUID.randomUUID().toString();
        String userId = currentUniversityId;

        if (reportId == null || userId == null) {
            resetButton();
            ErrorHelper.showError(btnSubmit, "Error initializing submission. Please login again.");
            return;
        }

        if (!selectedImageUris.isEmpty()) {
            List<String> imageUrlStrings = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger remaining = new AtomicInteger(selectedImageUris.size());

            for (int i = 0; i < selectedImageUris.size(); i++) {
                String fileName = reportId + "_" + i + "_" + System.currentTimeMillis() + ".jpg";
                SupabaseStorageHelper.uploadImage(this, selectedImageUris.get(i), "found_items", fileName, new SupabaseStorageHelper.UploadCallback() {
                    @Override
                    public void onSuccess(String publicUrl) {
                        imageUrlStrings.add(publicUrl);
                        if (remaining.decrementAndGet() == 0) {
                            if (isEditMode) {
                                saveReport(reportId, itemName, category, description, date, location, manualLocation, handlingStatus, authorityName, hiddenQuestion, contactName, contactPhone, preferredContact, imageUrlStrings, userId, null);
                            } else {
                                generateDisplayIdAndSave(reportId, itemName, category, description, date, location, manualLocation, handlingStatus, authorityName, hiddenQuestion, contactName, contactPhone, preferredContact, imageUrlStrings, userId);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (remaining.decrementAndGet() == 0) {
                            if (isEditMode) {
                                saveReport(reportId, itemName, category, description, date, location, manualLocation, handlingStatus, authorityName, hiddenQuestion, contactName, contactPhone, preferredContact, imageUrlStrings, userId, null);
                            } else {
                                generateDisplayIdAndSave(reportId, itemName, category, description, date, location, manualLocation, handlingStatus, authorityName, hiddenQuestion, contactName, contactPhone, preferredContact, imageUrlStrings, userId);
                            }
                        }
                    }
                });
            }
        } else {
            List<String> images = isEditMode && existingItem != null ? existingItem.getImageUrls() : new ArrayList<>();
            if (isEditMode) {
                saveReport(reportId, itemName, category, description, date, location, manualLocation, handlingStatus, authorityName, hiddenQuestion, contactName, contactPhone, preferredContact, images, userId, null);
            } else {
                generateDisplayIdAndSave(reportId, itemName, category, description, date, location, manualLocation, handlingStatus, authorityName, hiddenQuestion, contactName, contactPhone, preferredContact, images, userId);
            }
        }
    }

    private void generateDisplayIdAndSave(String reportId, String itemName, String category, String description, String date, String location, String manualLocation, String handlingStatus, String authorityName, String hiddenQuestion, String contactName, String contactPhone, String preferredContact, List<String> imageUrls, String userId) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("p_counter_name", "found_items");
        
        SupabaseDatabaseHelper.rpc("increment_counter", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    long count = Long.parseLong(result.trim());
                    String displayId = "F" + count;
                    saveReport(reportId, itemName, category, description, date, location, manualLocation, handlingStatus, authorityName, hiddenQuestion, contactName, contactPhone, preferredContact, imageUrls, userId, displayId);
                } catch (Exception e) {
                    resetButton();
                    ErrorHelper.showError(btnSubmit, "Failed to parse Report ID.");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                resetButton();
                ErrorHelper.showError(btnSubmit, "Failed to generate Report ID: " + errorMessage);
            }
        });
    }

    private void saveReport(String reportId, String itemName, String category, String description, String date, String location, String manualLocation, String handlingStatus, String authorityName, String hiddenQuestion, String contactName, String contactPhone, String preferredContact, List<String> imageUrls, String userId, String displayId) {
        Item report;
        if (isEditMode && existingItem != null) {
            report = existingItem;
            report.setName(itemName);
            report.setCategory(category);
            report.setDescription(description);
            report.setLocation(location);
            report.setManualLocation(manualLocation);
            report.setDate(date);
            report.setEdited(true);
        } else {
            report = new Item(reportId, itemName, category, description, location, date, "found", userId);
            report.setManualLocation(manualLocation);
            report.setDisplayId(displayId);
        }
        
        report.setTime(etTimeFound.getText().toString().trim());
        report.setAdditionalLocationDetails(etLocationDetails.getText().toString().trim());
        report.setItemHandlingStatus(handlingStatus);
        report.setAuthorityName(authorityName);
        report.setOfficeRoomNumber(etOfficeRoom.getText().toString().trim());
        report.setHiddenIdentificationQuestion(hiddenQuestion);
        report.setUserName(contactName);
        report.setUserPhone(contactPhone);
        report.setPreferredContactMethod(preferredContact);
        report.setImageUrls(imageUrls);
        if (!imageUrls.isEmpty()) {
            report.setImageUrl(imageUrls.get(0));
        }

        if (currentUser != null) {
            report.setAuthUserId(currentUser.getAuthId());
            report.setUserEmail(currentUser.getEmail());
            report.setUserUniversityId(currentUser.getUniversityId());
            report.setUserDepartment(currentUser.getDepartment());
        } else {
            android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
            report.setAuthUserId(prefs.getString("authId", null));
        }

        if (isEditMode) {
            SupabaseDatabaseHelper.update("found_reports", "id=eq." + reportId, report, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Report updated successfully");
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    resetButton();
                    ErrorHelper.showError(btnSubmit, "Failed to update report: " + errorMessage);
                }
            });
        } else {
            SupabaseDatabaseHelper.insert("found_reports", report, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    SnackbarManager.show(SnackbarManager.Type.SUCCESS, getString(R.string.success_report_submitted));
                    finish();
                }

                @Override
                public void onFailure(String errorMessage) {
                    resetButton();
                    ErrorHelper.showError(btnSubmit, "Failed to save report: " + errorMessage);
                }
            });
        }
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnSubmit.setEnabled(false);
            btnSubmit.setText("");
            btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
            btnSubmit.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
            btnSubmit.setStrokeColor(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.primaryColor)));
            loadingAnimation.setVisibility(View.VISIBLE);
            loadingAnimation.playAnimation();
        } else {
            loadingAnimation.setVisibility(View.GONE);
            loadingAnimation.pauseAnimation();
            btnSubmit.setEnabled(true);
            btnSubmit.setText(isEditMode ? "Save Changes" : "Submit Report");
            btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(this, R.color.primaryColor)));
            btnSubmit.setStrokeWidth(0);
        }
    }

    private void resetButton() {
        setLoadingState(false);
    }
}
