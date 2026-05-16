package com.sas.lostandfound;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.reflect.TypeToken;
import java.util.List;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etUniversityId, etEmail;
    private TextInputLayout tilUniversityId, tilEmail;
    private MaterialButton btnSubmit, btnOkay;
    private LinearLayout llInputFields, llSuccessState;
    private LottieAnimationView loader;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initializeViews();
        setupToolbar();

        btnSubmit.setOnClickListener(v -> validateAndReset());
        btnOkay.setOnClickListener(v -> finish());
    }

    private void initializeViews() {
        etUniversityId = findViewById(R.id.etUniversityId);
        etEmail = findViewById(R.id.etEmail);
        tilUniversityId = findViewById(R.id.tilUniversityId);
        tilEmail = findViewById(R.id.tilEmail);
        btnSubmit = findViewById(R.id.btnSubmit);
        btnOkay = findViewById(R.id.btnOkay);
        llInputFields = findViewById(R.id.llInputFields);
        llSuccessState = findViewById(R.id.llSuccessState);
        loader = findViewById(R.id.loader);
        toolbar = findViewById(R.id.toolbar);

        ErrorHelper.attachToTextInputLayout(tilUniversityId);
        ErrorHelper.attachToTextInputLayout(tilEmail);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void startLoading() {
        btnSubmit.setEnabled(false);
        loader.setVisibility(View.VISIBLE);
        loader.playAnimation();
    }

    private void stopLoading() {
        loader.cancelAnimation();
        loader.setVisibility(View.GONE);
        btnSubmit.setEnabled(true);
    }

    private void validateAndReset() {
        String universityId = etUniversityId.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (universityId.isEmpty()) {
            ErrorHelper.setFieldError(tilUniversityId, "University ID is required");
            return;
        }

        if (email.isEmpty()) {
            ErrorHelper.setFieldError(tilEmail, "Email address is required");
            return;
        }

        startLoading();
        checkUserExists(universityId, email);
    }

    private void checkUserExists(String universityId, String email) {
        // First check by University ID
        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + universityId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users != null && !users.isEmpty()) {
                    User user = users.get(0);
                    if (email.equalsIgnoreCase(user.getEmail())) {
                        // Case 4: Both correct
                        generateAndSendResetToken(user);
                    } else {
                        // Case 2: ID exists but Email is incorrect
                        stopLoading();
                        SnackbarManager.show(SnackbarManager.Type.ERROR, "Incorrect email address. Please try again with the correct email.");
                    }
                } else {
                    // University ID doesn't exist, now check if Email exists for Case 3
                    checkIfEmailExists(universityId, email);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                stopLoading();
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Database error: " + errorMessage);
            }
        });
    }

    private void checkIfEmailExists(String universityId, String email) {
        SupabaseDatabaseHelper.select("profiles", "email=eq." + email + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                stopLoading();
                if (users != null && !users.isEmpty()) {
                    // Case 3: Email exists but University ID is incorrect
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "Invalid University ID. Please try again with the correct University ID.");
                } else {
                    // Case 1: Neither exist (or at least not together)
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "This user account does not exist. Please create an account first.");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                stopLoading();
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Database error: " + errorMessage);
            }
        });
    }

    private void generateAndSendResetToken(User user) {
        String token = java.util.UUID.randomUUID().toString();
        // Set expiration to 1 hour from now
        long expiresAt = System.currentTimeMillis() + (60 * 60 * 1000);
        
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("reset_token", token);
        updates.put("reset_token_expires_at", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(new java.util.Date(expiresAt)));
        updates.put("reset_token_used", false);

        android.util.Log.d("ForgotPassword", "Saving reset token for user: " + user.getUniversityId());
        android.util.Log.d("ForgotPassword", "Token: " + token);

        SupabaseDatabaseHelper.update("profiles", "university_id=eq." + android.net.Uri.encode(user.getUniversityId()), updates, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                android.util.Log.d("ForgotPassword", "Update Success: " + result);
                sendResetEmail(user.getEmail(), token);
            }

            @Override
            public void onFailure(String errorMessage) {
                stopLoading();
                android.util.Log.e("ForgotPassword", "Update Failure: " + errorMessage);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Security check failed. Please try again or contact support.");
            }
        });
    }

    private void sendResetEmail(String email, String token) {
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("email", email);
        params.put("token", token);

        SupabaseDatabaseHelper.callEdgeFunction("send-reset-email", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                stopLoading();
                showSuccessState();
            }

            @Override
            public void onFailure(String errorMessage) {
                stopLoading();
                // Even if email fails, we show success to prevent email enumeration, 
                // but for debugging we can log it.
                showSuccessState();
            }
        });
    }

    private void showSuccessState() {
        llInputFields.setVisibility(View.GONE);
        llSuccessState.setVisibility(View.VISIBLE);
    }
}
