package com.sas.lostandfound;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
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
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserRegistrationActivity extends AppCompatActivity {

    private EditText etFullName, etUniversityId, etEmail, etPhone,
            etDepartment, etBatch, etPassword, etConfirmPassword, etDesignation;
    private AutoCompleteTextView actvLevelTerm, actvUserType;
    private TextInputLayout tilBatch, tilDepartment, tilLevelTerm, tilDesignation, tilUserType;
    private MaterialButton btnCreateAccount;
    private ProgressBar progressBar;
    private ImageView ivProfilePicture;
    private FloatingActionButton fabAddPhoto;
    private ImageButton btnBack;
    private TextView tvLogin;
    private CheckBox cbPolicy;
    private View keyboardSpacer;
    private View registrationRoot;

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
        setupPolicyText();
        setupLoginLink();
        setupKeyboardListener();
    }

    private void initializeViews() {
        registrationRoot = findViewById(R.id.registrationRoot);
        keyboardSpacer = findViewById(R.id.keyboardSpacer);
        etUniversityId = findViewById(R.id.etUniversityId);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etDepartment = findViewById(R.id.etDepartment);
        etBatch = findViewById(R.id.etBatch);
        etDesignation = findViewById(R.id.etDesignation);
        actvLevelTerm = findViewById(R.id.actvLevelTerm);
        actvUserType = findViewById(R.id.actvUserType);
        
        tilBatch = findViewById(R.id.tilBatch);
        tilDepartment = findViewById(R.id.tilDepartment);
        tilLevelTerm = findViewById(R.id.tilLevelTerm);
        tilDesignation = findViewById(R.id.tilDesignation);
        tilUserType = findViewById(R.id.tilUserType);

        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        progressBar = findViewById(R.id.progressBar);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        fabAddPhoto = findViewById(R.id.fabAddPhoto);
        btnBack = findViewById(R.id.btnBack);
        tvLogin = findViewById(R.id.tvLogin);
        cbPolicy = findViewById(R.id.cbPolicy);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
    }

    private void setupKeyboardListener() {
        if (registrationRoot == null || keyboardSpacer == null) return;

        registrationRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                registrationRoot.getWindowVisibleDisplayFrame(r);
                int screenHeight = registrationRoot.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                // If keypad height is more than 15% of screen height, it's likely the keyboard is up
                if (keypadHeight > screenHeight * 0.15) {
                    if (keyboardSpacer.getVisibility() != View.VISIBLE) {
                        keyboardSpacer.setVisibility(View.VISIBLE);
                        // Setting a minimum height for the spacer when keyboard is on
                        keyboardSpacer.getLayoutParams().height = (int) (200 * getResources().getDisplayMetrics().density);
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

    private void setupDropdowns() {
        String[] levelTermOptions = {
                "Level 1 Term I", "Level 1 Term II",
                "Level 2 Term I", "Level 2 Term II",
                "Level 3 Term I", "Level 3 Term II",
                "Level 4 Term I", "Level 4 Term II"
        };
        ArrayAdapter<String> levelTermAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, levelTermOptions);
        actvLevelTerm.setAdapter(levelTermAdapter);
        actvLevelTerm.setOnClickListener(v -> actvLevelTerm.showDropDown());

        String[] userTypeOptions = {"Student", "Staff"};
        ArrayAdapter<String> userTypeAdapter = new ArrayAdapter<>(this, R.layout.dropdown_item, userTypeOptions);
        actvUserType.setAdapter(userTypeAdapter);
        actvUserType.setOnClickListener(v -> actvUserType.showDropDown());
        
        actvUserType.setOnItemClickListener((parent, view, position, id) -> {
            String selection = (String) parent.getItemAtPosition(position);
            updateUIForUserType(selection);
        });
        
        // Default UI
        updateUIForUserType("Student");
    }

    private void updateUIForUserType(String userType) {
        if ("Student".equals(userType)) {
            tilUserType.setStartIconDrawable(R.drawable.ic_graduation_cap);
            tilBatch.setVisibility(View.VISIBLE);
            tilDepartment.setVisibility(View.VISIBLE);
            tilLevelTerm.setVisibility(View.VISIBLE);
            tilDesignation.setVisibility(View.GONE);
        } else {
            tilUserType.setStartIconDrawable(R.drawable.ic_user);
            tilBatch.setVisibility(View.GONE);
            tilDepartment.setVisibility(View.GONE);
            tilLevelTerm.setVisibility(View.GONE);
            tilDesignation.setVisibility(View.VISIBLE);
        }
    }

    private void setupListeners() {
        fabAddPhoto.setOnClickListener(v -> showImageSourceDialog());
        ivProfilePicture.setOnClickListener(v -> showImageSourceDialog());
        
        cbPolicy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnCreateAccount.setEnabled(isChecked);
        });

        btnCreateAccount.setOnClickListener(v -> {
            if (cbPolicy.isChecked()) {
                registerUser();
            } else {
                Toast.makeText(this, "Please agree to the policy first.", Toast.LENGTH_SHORT).show();
            }
        });
        
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

    private void setupPolicyText() {
        String fullText = getString(R.string.lost_and_found_policy);
        String clickablePart = getString(R.string.lost_and_found_policy_clickable);
        
        SpannableString ss = new SpannableString(fullText);
        
        int startIndex = fullText.indexOf(clickablePart);
        if (startIndex != -1) {
            int endIndex = startIndex + clickablePart.length();
            
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    startActivity(new Intent(UserRegistrationActivity.this, LostAndFoundPolicyActivity.class));
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            };
            
            ss.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primaryColor)), 
                    startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new StyleSpan(Typeface.BOLD), 
                    startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        cbPolicy.setText(ss);
        cbPolicy.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setupLoginLink() {
        if (tvLogin == null) return;
        
        Spanned spanned = Html.fromHtml(getString(R.string.login_link), Html.FROM_HTML_MODE_LEGACY);
        SpannableString ss = new SpannableString(spanned);
        
        String fullText = spanned.toString();
        String loginText = "Login";
        int startIndex = fullText.indexOf(loginText);
        
        if (startIndex != -1) {
            int endIndex = startIndex + loginText.length();
            ss.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.primaryColor)), 
                    startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ss.setSpan(new StyleSpan(Typeface.BOLD),
                    startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        tvLogin.setText(ss);
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
        btnCreateAccount.setEnabled(!isLoading && cbPolicy.isChecked());
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
        String userType = actvUserType.getText().toString().trim();

        if (TextUtils.isEmpty(universityId)) { etUniversityId.setError("Required"); return; }
        if (TextUtils.isEmpty(fullName)) { etFullName.setError("Required"); return; }
        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email required"); return; }
        if (password.length() < 6) { etPassword.setError("Minimum 6 characters"); return; }
        if (!password.equals(confirmPassword)) { etConfirmPassword.setError("Passwords do not match"); return; }

        if ("Staff".equals(userType)) {
            if (TextUtils.isEmpty(etDesignation.getText().toString().trim())) {
                etDesignation.setError("Required"); return;
            }
        }

        showLoading(true);

        // Check for duplicate University ID in all possible formats (String, Number, etc.)
        List<Object> idVariations = new ArrayList<>();
        idVariations.add(universityId);
        if (universityId.startsWith("0")) idVariations.add(universityId.substring(1));
        try { idVariations.add(Long.parseLong(universityId)); } catch (NumberFormatException ignored) {}

        checkDuplicatesAndRegister(idVariations, 0, email, password, universityId, fullName, userType);
    }

    private void checkDuplicatesAndRegister(List<Object> variations, int index, String email, String password, String universityId, String fullName, String userType) {
        if (index >= variations.size()) {
            performAuthRegistration(email, password, universityId, fullName, userType);
            return;
        }

        Object currentVariation = variations.get(index);
        DatabaseReference usersRef = mDatabase.child("Users");
        Query query = usersRef.orderByChild("universityId").equalTo(currentVariation.toString());
        if (currentVariation instanceof Long) {
            query = usersRef.orderByChild("universityId").equalTo((Long) currentVariation);
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    showLoading(false);
                    etUniversityId.setError("ID already registered");
                    Toast.makeText(UserRegistrationActivity.this, "This University ID is already in use.", Toast.LENGTH_LONG).show();
                } else {
                    checkDuplicatesAndRegister(variations, index + 1, email, password, universityId, fullName, userType);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(UserRegistrationActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performAuthRegistration(String email, String password, String universityId, String fullName, String userType) {
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
                                    saveUser(userId, publicUrl, universityId, fullName, email, password, userType);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    saveUser(userId, null, universityId, fullName, email, password, userType);
                                }
                            });
                        } else {
                            saveUser(userId, null, universityId, fullName, email, password, userType);
                        }
                    }
                });
    }

    private void saveUser(String userId, String imageUrl, String universityId, String fullName, String email, String password, String userType) {
        String phone = etPhone.getText().toString().trim();
        
        User user;
        if ("Student".equals(userType)) {
            String department = etDepartment.getText().toString().trim();
            String batch = etBatch.getText().toString().trim();
            String levelTerm = actvLevelTerm.getText().toString().trim();
            user = new User(userId, fullName, universityId, email, password, phone, department, batch, levelTerm, "Not Specified", imageUrl, "Not Specified");
        } else {
            String designation = etDesignation.getText().toString().trim();
            user = new User(userId, fullName, universityId, email, password, phone, designation, imageUrl, "Not Specified", "Staff");
        }

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
