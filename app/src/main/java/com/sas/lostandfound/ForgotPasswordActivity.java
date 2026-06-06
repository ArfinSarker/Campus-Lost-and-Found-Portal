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
import android.graphics.Rect;
import android.view.ViewTreeObserver;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etUniversityId, etEmail;
    private TextInputLayout tilUniversityId, tilEmail;
    private MaterialButton btnSubmit, btnOkay;
    private LinearLayout llInputFields, llSuccessState;
    private LottieAnimationView loader;
    private android.content.res.ColorStateList originalBackgroundTint;
    private View keyboardSpacer;
    private View forgotPasswordRoot;
    private com.google.android.material.appbar.AppBarLayout appBarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        initializeViews();
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout);
        }
        setupToolbar();
        setupKeyboardListener();

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
        forgotPasswordRoot = findViewById(R.id.forgotPasswordRoot);
        keyboardSpacer = findViewById(R.id.keyboardSpacer);
        appBarLayout = findViewById(R.id.appBarLayout);

        ErrorHelper.attachToTextInputLayout(tilUniversityId);
        ErrorHelper.attachToTextInputLayout(tilEmail);
    }

    private void setupToolbar() {
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void startLoading() {
        btnSubmit.setEnabled(false);
        if (originalBackgroundTint == null) {
            originalBackgroundTint = btnSubmit.getBackgroundTintList();
        }
        btnSubmit.setText("");
        btnSubmit.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        loader.setVisibility(View.VISIBLE);
        loader.playAnimation();
    }

    private void stopLoading() {
        loader.cancelAnimation();
        loader.setVisibility(View.GONE);
        if (originalBackgroundTint != null) {
            btnSubmit.setBackgroundTintList(originalBackgroundTint);
        }
        btnSubmit.setText(R.string.btn_submit);
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
                        ErrorHelper.setFieldError(tilEmail, "Incorrect email address. Please try again with the correct email.");
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
                    ErrorHelper.setFieldError(tilUniversityId, "Invalid University ID. Please try again with the correct University ID.");
                } else {
                    // Case 1: Neither exist (or at least not together)
                    ErrorHelper.setFieldError(tilUniversityId, "This user account does not exist. Please create an account first.");
                    tilEmail.setError("This user account does not exist. Please create an account first.");
                    tilEmail.setBoxBackgroundColor(androidx.core.content.ContextCompat.getColor(ForgotPasswordActivity.this, R.color.error_light_bg));
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

    private void setupKeyboardListener() {
        if (forgotPasswordRoot == null || keyboardSpacer == null) return;

        forgotPasswordRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                forgotPasswordRoot.getWindowVisibleDisplayFrame(r);
                int screenHeight = forgotPasswordRoot.getRootView().getHeight();
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
