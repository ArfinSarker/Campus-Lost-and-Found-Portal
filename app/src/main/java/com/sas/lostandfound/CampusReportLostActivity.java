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
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;

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

public class CampusReportLostActivity extends AppCompatActivity {

    private TextInputEditText etItemName, etDateLost, etTimeLost, etManualLocation, etLocationDetails, etDescription, etProofOwnership, etContactName, etContactPhone, etContactEmail;
    private AutoCompleteTextView actvCategory, actvLocation, actvPreferredContact, actvCountryCode;
    private TextInputLayout tilItemName, tilCategory, tilDescription, tilDate, tilTime, tilLocation, tilManualLocation, tilContactName, tilContactPhone, tilPreferredContact, tilCountryCode, tilContactEmail;
    private MaterialCheckBox cbConfirm;
    private MaterialButton btnSubmit;
    private com.airbnb.lottie.LottieAnimationView loadingAnimation;
    private com.google.android.material.card.MaterialCardView uploadCard;
    private View layoutUploadEmpty;
    private android.widget.HorizontalScrollView scrollUploadImages;
    private android.widget.LinearLayout layoutUploadImagesContainer;
    private Toolbar toolbar;
    private View keyboardSpacer;
    private View reportLostRoot;

    private static final int PICK_IMAGES_REQUEST = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int CAMERA_PERMISSION_CODE = 100;

    private List<Object> imageItems = new ArrayList<>();
    private Uri cameraImageUri;
    private String currentPhotoPath;

    private String currentUniversityId;
    private User currentUser;

    private boolean isEditMode = false;
    private String editItemId;
    private Item existingItem;
    private java.util.Set<String> oldImageUrlsToDelete = new java.util.HashSet<>();

    private String contactNameState = "";
    private String contactPhoneState = "";
    private String contactEmailState = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_report_lost);

        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUniversityId = prefs.getString("universityId", null);

        initializeViews();
        setupToolbar();
        setupDropdowns();
        setupPickers();
        setupTextWatchers();
        setupKeyboardListener();

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
        actvCountryCode = findViewById(R.id.actvCountryCode);
        tilCountryCode = findViewById(R.id.tilCountryCode);

        etContactEmail = findViewById(R.id.etContactEmail);
        tilContactEmail = findViewById(R.id.tilContactEmail);

        actvPreferredContact = findViewById(R.id.actvPreferredContact);
        tilPreferredContact = findViewById(R.id.tilPreferredContact);

        cbConfirm = findViewById(R.id.cbConfirm);
        btnSubmit = findViewById(R.id.btnSubmitReport);
        loadingAnimation = findViewById(R.id.loadingAnimation);
        uploadCard = findViewById(R.id.uploadCard);
        layoutUploadEmpty = findViewById(R.id.layoutUploadEmpty);
        scrollUploadImages = findViewById(R.id.scrollUploadImages);
        layoutUploadImagesContainer = findViewById(R.id.layoutUploadImagesContainer);
        toolbar = findViewById(R.id.toolbar);
        reportLostRoot = findViewById(R.id.reportLostRoot);
        keyboardSpacer = findViewById(R.id.keyboardSpacer);

        ErrorHelper.attachToTextInputLayout(tilItemName);
        ErrorHelper.attachToTextInputLayout(tilCategory);
        ErrorHelper.attachToTextInputLayout(tilDescription);
        ErrorHelper.attachToTextInputLayout(tilDate);
        ErrorHelper.attachToTextInputLayout(tilTime);
        ErrorHelper.attachToTextInputLayout(tilLocation);
        ErrorHelper.attachToTextInputLayout(tilManualLocation);
        ErrorHelper.attachToTextInputLayout(tilContactName);
        ErrorHelper.attachToTextInputLayout(tilContactPhone);
        ErrorHelper.attachToTextInputLayout(tilCountryCode);
        ErrorHelper.attachToTextInputLayout(tilContactEmail);
        ErrorHelper.attachToTextInputLayout(tilPreferredContact);
    }

    private void loadItemDataForEdit(String itemId) {
        SupabaseDatabaseHelper.select("lost_reports", "id=eq." + itemId + "&limit=1", new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
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
            etManualLocation.setText(item.getManualLocation());
        }

        etLocationDetails.setText(item.getAdditionalLocationDetails());
        etProofOwnership.setText(item.getProofOfOwnershipDetail());
        etContactName.setText(item.getUserName());
        contactNameState = item.getUserName() != null ? item.getUserName() : "";
        
        String fullPhone = item.getUserPhone();
        String[] parsedPhone = ValidationUtils.parsePhoneNumber(fullPhone);
        String code = parsedPhone[0];
        String body = parsedPhone[1];
        
        if (actvCountryCode != null) {
            actvCountryCode.setText(ValidationUtils.getCountryDisplayString(code), false);
        }
        etContactPhone.setText(body);
        contactPhoneState = code + body;
        if (etContactEmail != null) {
            etContactEmail.setText(item.getUserEmail());
            contactEmailState = item.getUserEmail() != null ? item.getUserEmail() : "";
        }

        actvPreferredContact.setText(item.getPreferredContactMethod(), false);

        currentUniversityId = item.getUserId();
        btnSubmit.setText("Save Changes");

        oldImageUrlsToDelete.clear();
        imageItems.clear();
        if (item.getImageUrls() != null && !item.getImageUrls().isEmpty()) {
            for (String url : item.getImageUrls()) {
                if (url != null && !url.isEmpty()) {
                    imageItems.add(url);
                }
            }
        } else if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            imageItems.add(item.getImageUrl());
        }
        updateUploadUI();
    }

    private void setupTextWatchers() {
        etContactName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) { contactNameState = s.toString().trim(); }
        });
        etContactPhone.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                String selectedCountryCode = actvCountryCode.getText().toString().trim();
                String code = ValidationUtils.extractCountryCode(selectedCountryCode);
                contactPhoneState = code + s.toString().trim();
            }
        });
        if (etContactEmail != null) {
            etContactEmail.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) { contactEmailState = s.toString().trim(); }
            });
        }
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());

            com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
            if (appBarLayout != null) {
                int headerColor = ContextCompat.getColor(this, R.color.rl_header_bg);
                boolean isNightMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                HeaderColorHelper.setup(this, appBarLayout, headerColor, headerColor, !isNightMode);
            }
        }
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
                        contactNameState = currentUser.getName() != null ? currentUser.getName() : "";
                        
                        String fullPhone = currentUser.getPhone();
                        String[] parsedPhone = ValidationUtils.parsePhoneNumber(fullPhone);
                        String code = parsedPhone[0];
                        String body = parsedPhone[1];
                        
                        if (actvCountryCode != null) {
                            actvCountryCode.setText(ValidationUtils.getCountryDisplayString(code), false);
                        }
                        etContactPhone.setText(body);
                        contactPhoneState = code + body;
                        if (etContactEmail != null) {
                            etContactEmail.setText(currentUser.getEmail());
                            contactEmailState = currentUser.getEmail() != null ? currentUser.getEmail() : "";
                        }
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {}
        });
    }

    private void setupDropdowns() {
        actvCountryCode.setFocusable(false);
        actvCountryCode.setClickable(true);
        actvCountryCode.setInputType(android.text.InputType.TYPE_NULL);
        actvCountryCode.setText(ValidationUtils.getCountryDisplayString("+880"), false);
        actvCountryCode.setOnClickListener(v -> CountryPickerDialog.show(this, country -> {
            actvCountryCode.setText(country.getFlagEmoji() + " " + country.getCode(), false);
            String phoneBody = etContactPhone.getText().toString().trim();
            contactPhoneState = country.getCode() + phoneBody;
        }));

        String[] categories = {"Electronics & Gadgets", "ID Cards", "Wallets & Purses", "Bank/Credit Cards", "Bags", "Study Materials", "Eyewear", "Keys & Access Devices", "Clothing & Accessories", "Others"};
        actvCategory.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item_report_lost, categories));

        String[] locations = {"Academic Building", "Civil Building", "Library", "Cafeteria", "Medical Center", "Playground", "Abbas Uddin Ahmed Hall (AUAH)", "Shaheed Dr. Zikrul Haque Hall", "Bir Protik Taramon Bibi Hall", "Bir Protik Taramon Bibi (New Hall)", "Other"};
        actvLocation.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item_report_lost, locations));
        actvLocation.setOnItemClickListener((parent, view, position, id) -> {
            if (locations[position].equals("Other")) {
                tilManualLocation.setVisibility(View.VISIBLE);
            } else {
                tilManualLocation.setVisibility(View.GONE);
                etManualLocation.setText("");
            }
        });

        String[] contactMethods = {"Phone", "Email", "In-app chat"};
        actvPreferredContact.setAdapter(new ArrayAdapter<>(this, R.layout.dropdown_item_report_lost, contactMethods));

        int popupBgColor = ContextCompat.getColor(this, R.color.rl_dropdown_popup_bg);
        actvCategory.setDropDownBackgroundDrawable(new android.graphics.drawable.ColorDrawable(popupBgColor));
        actvLocation.setDropDownBackgroundDrawable(new android.graphics.drawable.ColorDrawable(popupBgColor));
        actvPreferredContact.setDropDownBackgroundDrawable(new android.graphics.drawable.ColorDrawable(popupBgColor));
        actvCountryCode.setDropDownBackgroundDrawable(new android.graphics.drawable.ColorDrawable(popupBgColor));
    }

    private void setupPickers() {
        etDateLost.setOnClickListener(v -> {
            CalendarConstraints.Builder constraintsBuilder = new CalendarConstraints.Builder();
            constraintsBuilder.setValidator(DateValidatorPointBackward.now());

            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Date")
                    .setCalendarConstraints(constraintsBuilder.build())
                    .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                    .setTheme(ThemeManager.getDatePickerTheme(this))
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
                    .setTheme(ThemeManager.getTimePickerTheme(this))
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
                if (data != null) {
                    if (data.getClipData() != null) {
                        ClipData clipData = data.getClipData();
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            imageItems.add(clipData.getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null) {
                        imageItems.add(data.getData());
                    }
                }
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                if (cameraImageUri != null) {
                    imageItems.add(cameraImageUri);
                }
            }

            updateUploadUI();
        }
    }

    private void updateUploadUI() {
        if (imageItems == null || imageItems.isEmpty()) {
            if (layoutUploadEmpty != null) layoutUploadEmpty.setVisibility(View.VISIBLE);
            if (scrollUploadImages != null) scrollUploadImages.setVisibility(View.GONE);
        } else {
            if (layoutUploadEmpty != null) layoutUploadEmpty.setVisibility(View.GONE);
            if (scrollUploadImages != null) scrollUploadImages.setVisibility(View.VISIBLE);

            if (layoutUploadImagesContainer != null) {
                layoutUploadImagesContainer.removeAllViews();
                android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);

                for (final Object item : imageItems) {
                    android.view.View thumbView = inflater.inflate(R.layout.item_upload_thumbnail, layoutUploadImagesContainer, false);
                    ImageView ivThumbnail = thumbView.findViewById(R.id.ivThumbnail);
                    View btnRemove = thumbView.findViewById(R.id.btnRemove);

                    com.bumptech.glide.Glide.with(this)
                            .load(item)
                            .centerCrop()
                            .placeholder(R.drawable.bg_report_placeholder)
                            .into(ivThumbnail);

                    btnRemove.setOnClickListener(v -> {
                        imageItems.remove(item);
                        if (item instanceof String) {
                            oldImageUrlsToDelete.add((String) item);
                        }
                        updateUploadUI();
                    });

                    layoutUploadImagesContainer.addView(thumbView);
                }

                // Append the "+" Add Image card
                android.view.View addCardView = inflater.inflate(R.layout.item_upload_add, layoutUploadImagesContainer, false);
                addCardView.setOnClickListener(v -> showImageSourceDialog());
                layoutUploadImagesContainer.addView(addCardView);
            }
        }
    }

    private void validateAndSubmit() {
        if (currentUniversityId == null) {
            ErrorHelper.showError(btnSubmit, "Please log in again to submit");
            return;
        }

        String name = etItemName.getText().toString().trim();
        String category = actvCategory.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        String date = etDateLost.getText().toString().trim();
        String location = actvLocation.getText().toString().trim();
        String manualLocation = etManualLocation.getText().toString().trim();
        String contactName = contactNameState.trim();
        
        String selectedCountryCode = actvCountryCode.getText().toString().trim();
        String code = ValidationUtils.extractCountryCode(selectedCountryCode);
        String phoneBody = etContactPhone.getText().toString().trim();
        String contactPhone = contactPhoneState.trim();
        String contactEmail = contactEmailState.trim();
        
        String preferredContact = actvPreferredContact.getText().toString().trim();

        boolean isValid = true;

        if (TextUtils.isEmpty(name)) { ErrorHelper.setFieldError(tilItemName, "Item name is required"); isValid = false; }
        else if (TextUtils.isEmpty(category)) { ErrorHelper.setFieldError(tilCategory, "Category is required"); isValid = false; }
        else if (TextUtils.isEmpty(description)) { ErrorHelper.setFieldError(tilDescription, "Description is required"); isValid = false; }
        else if (TextUtils.isEmpty(date)) { ErrorHelper.setFieldError(tilDate, "Date is required"); isValid = false; }
        else if (TextUtils.isEmpty(location)) { ErrorHelper.setFieldError(tilLocation, "Location is required"); isValid = false; }
        else if (location.equals("Other") && TextUtils.isEmpty(manualLocation)) { ErrorHelper.setFieldError(tilManualLocation, "Please specify location"); isValid = false; }
        
        else if (TextUtils.isEmpty(contactName)) { ErrorHelper.setFieldError(tilContactName, "Your name is required"); isValid = false; }
        else if (TextUtils.isEmpty(phoneBody)) { ErrorHelper.setFieldError(tilContactPhone, "Contact phone is required"); isValid = false; }
        else if (!ValidationUtils.isValidPhone(code, phoneBody)) { ErrorHelper.setFieldError(tilContactPhone, "Please enter a valid phone number"); isValid = false; }
        else if (TextUtils.isEmpty(contactEmail)) { ErrorHelper.setFieldError(tilContactEmail, "Contact email is required"); isValid = false; }
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(contactEmail).matches()) { ErrorHelper.setFieldError(tilContactEmail, "Please enter a valid email address"); isValid = false; }
        else if (TextUtils.isEmpty(preferredContact)) { ErrorHelper.setFieldError(tilPreferredContact, "Preferred contact is required"); isValid = false; }

        if (isValid && !cbConfirm.isChecked()) {
            ErrorHelper.showError(btnSubmit, "Please confirm the information is accurate");
            isValid = false;
        }

        if (isValid) {
            String finalManualLocation = TextUtils.isEmpty(manualLocation) ? null : manualLocation;
            submitReport(name, category, description, date, location, finalManualLocation, contactName, contactPhone, contactEmail, preferredContact);
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
        tilContactEmail.setError(null);
        tilPreferredContact.setError(null);
    }

    private void submitReport(String name, String category, String description, String date, String location, String manualLocation, String contactName, String contactPhone, String contactEmail, String preferredContact) {
        setLoadingState(true);

        String itemId = isEditMode ? editItemId : java.util.UUID.randomUUID().toString();
        String userId = currentUniversityId;

        if (itemId == null || userId == null) {
            resetButton();
            ErrorHelper.showError(btnSubmit, "Failed to initialize submission. Please login again.");
            return;
        }

        List<Uri> localUris = new ArrayList<>();
        List<String> remoteUrls = new ArrayList<>();
        for (Object item : imageItems) {
            if (item instanceof Uri) {
                localUris.add((Uri) item);
            } else if (item instanceof String) {
                remoteUrls.add((String) item);
            }
        }

        if (!localUris.isEmpty()) {
            List<String> uploadedUrls = Collections.synchronizedList(new ArrayList<>());
            AtomicInteger remaining = new AtomicInteger(localUris.size());

            for (int i = 0; i < localUris.size(); i++) {
                String fileName = itemId + "_" + i + "_" + System.currentTimeMillis() + ".jpg";
                SupabaseStorageHelper.uploadImage(this, localUris.get(i), "lost_items", fileName, new SupabaseStorageHelper.UploadCallback() {
                    @Override
                    public void onSuccess(String publicUrl) {
                        uploadedUrls.add(publicUrl);
                        if (remaining.decrementAndGet() == 0) {
                            List<String> combinedUrls = new ArrayList<>(remoteUrls);
                            combinedUrls.addAll(uploadedUrls);
                            if (isEditMode) {
                                saveToDatabase(itemId, name, category, description, date, location, manualLocation, contactName, contactPhone, contactEmail, preferredContact, combinedUrls, userId, null);
                            } else {
                                generateDisplayIdAndSave(itemId, name, category, description, date, location, manualLocation, contactName, contactPhone, contactEmail, preferredContact, combinedUrls, userId);
                            }
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        if (remaining.decrementAndGet() == 0) {
                            List<String> combinedUrls = new ArrayList<>(remoteUrls);
                            combinedUrls.addAll(uploadedUrls);
                            if (isEditMode) {
                                saveToDatabase(itemId, name, category, description, date, location, manualLocation, contactName, contactPhone, contactEmail, preferredContact, combinedUrls, userId, null);
                            } else {
                                generateDisplayIdAndSave(itemId, name, category, description, date, location, manualLocation, contactName, contactPhone, contactEmail, preferredContact, combinedUrls, userId);
                            }
                        }
                    }
                });
            }
        } else {
            if (isEditMode) {
                saveToDatabase(itemId, name, category, description, date, location, manualLocation, contactName, contactPhone, contactEmail, preferredContact, remoteUrls, userId, null);
            } else {
                generateDisplayIdAndSave(itemId, name, category, description, date, location, manualLocation, contactName, contactPhone, contactEmail, preferredContact, remoteUrls, userId);
            }
        }
    }

    private void generateDisplayIdAndSave(String itemId, String name, String category, String description, String date, String location, String manualLocation, String contactName, String contactPhone, String contactEmail, String preferredContact, List<String> imageUrls, String userId) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("p_counter_name", "lost_items");
        
        SupabaseDatabaseHelper.rpc("increment_counter", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    long count = Long.parseLong(result.trim());
                    String displayId = "L" + count;
                    saveToDatabase(itemId, name, category, description, date, location, manualLocation, contactName, contactPhone, contactEmail, preferredContact, imageUrls, userId, displayId);
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

    private void saveToDatabase(String itemId, String name, String category, String description, String date, String location, String manualLocation, String contactName, String contactPhone, String contactEmail, String preferredContact, List<String> imageUrls, String userId, String displayId) {
        Item item;
        if (isEditMode && existingItem != null) {
            item = existingItem;
            item.setName(name);
            item.setCategory(category);
            item.setDescription(description);
            item.setLocation(location);
            item.setManualLocation(manualLocation);
            item.setDate(date);
            item.setEdited(true);
        } else {
            item = new Item(itemId, name, category, description, location, date, "lost", userId);
            item.setManualLocation(manualLocation);
            item.setDisplayId(displayId);
        }
        
        item.setTime(etTimeLost.getText().toString().trim());
        item.setAdditionalLocationDetails(etLocationDetails.getText().toString().trim());
        item.setProofOfOwnershipDetail(etProofOwnership.getText().toString().trim());
        item.setImageUrls(imageUrls);
        if (imageUrls != null && !imageUrls.isEmpty()) {
            item.setImageUrl(imageUrls.get(0));
        } else {
            item.setImageUrl(null);
        }
        item.setPreferredContactMethod(preferredContact);
        item.setUserName(contactName);
        item.setUserPhone(contactPhone);
        item.setUserEmail(contactEmail);

        if (currentUser != null) {
            item.setAuthUserId(currentUser.getAuthId());
            item.setUserUniversityId(currentUser.getUniversityId());
            item.setUserDepartment(currentUser.getDepartment());
        } else {
            android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
            item.setAuthUserId(prefs.getString("authId", null));
        }

        if (isEditMode) {
            SupabaseDatabaseHelper.update("lost_reports", "id=eq." + itemId, item, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    if (oldImageUrlsToDelete != null && !oldImageUrlsToDelete.isEmpty()) {
                        for (String url : oldImageUrlsToDelete) {
                            if (url != null && url.contains("supabase.co")) {
                                SupabaseStorageHelper.deleteImage(url, null);
                            }
                        }
                    }
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
            SupabaseDatabaseHelper.insert("lost_reports", item, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
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
            int loadingBg = androidx.core.content.ContextCompat.getColor(this, R.color.report_submit_loading_bg);
            int loadingStroke = androidx.core.content.ContextCompat.getColor(this, R.color.report_submit_loading_stroke);
            btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(loadingBg));
            btnSubmit.setStrokeWidth((int) (2 * getResources().getDisplayMetrics().density));
            btnSubmit.setStrokeColor(android.content.res.ColorStateList.valueOf(loadingStroke));
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

    private void setupKeyboardListener() {
        if (reportLostRoot == null || keyboardSpacer == null) return;

        reportLostRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                reportLostRoot.getWindowVisibleDisplayFrame(r);
                int screenHeight = reportLostRoot.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    if (keyboardSpacer.getVisibility() != View.VISIBLE) {
                        keyboardSpacer.setVisibility(View.VISIBLE);
                        keyboardSpacer.getLayoutParams().height = (int) (320 * getResources().getDisplayMetrics().density);
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
}
