package com.sas.lostandfound;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.reflect.TypeToken;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateNewPasswordActivity extends AppCompatActivity {

    private TextInputEditText etNewPassword, etConfirmPassword;
    private TextInputLayout tilNewPassword, tilConfirmPassword;
    private MaterialButton btnReset;
    private LottieAnimationView loader;
    private String resetToken;
    private User currentUser;
    private android.content.res.ColorStateList originalBackgroundTint;
    private android.widget.ImageButton btnBack;
    private View keyboardSpacer;
    private View createNewPasswordRoot;
    private com.google.android.material.appbar.AppBarLayout appBarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_password);

        initializeViews();
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout);
        }
        handleIntent(getIntent());
        setupKeyboardListener();

        btnReset.setOnClickListener(v -> validateAndReset());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void initializeViews() {
        etNewPassword = findViewById(R.id.etNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        tilNewPassword = findViewById(R.id.tilNewPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        btnReset = findViewById(R.id.btnReset);
        loader = findViewById(R.id.loader);
        btnBack = findViewById(R.id.btnBack);
        createNewPasswordRoot = findViewById(R.id.createNewPasswordRoot);
        keyboardSpacer = findViewById(R.id.keyboardSpacer);
        appBarLayout = findViewById(R.id.appBarLayout);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        ErrorHelper.attachToTextInputLayout(tilNewPassword);
        ErrorHelper.attachToTextInputLayout(tilConfirmPassword);
    }

    private void handleIntent(Intent intent) {
        Uri data = intent.getData();
        android.util.Log.d("CreateNewPassword", "Handling intent: " + (data != null ? data.toString() : "null"));
        
        // Priority 1: Query parameter from Data URI
        if (data != null) {
            resetToken = data.getQueryParameter("token");
            android.util.Log.d("CreateNewPassword", "Token from URI: " + (resetToken != null ? "'" + resetToken + "' (len:" + resetToken.length() + ")" : "null"));
        }
        
        // Priority 2: String extra
        if (resetToken == null || resetToken.isEmpty()) {
            resetToken = intent.getStringExtra("token");
            if (resetToken == null) resetToken = intent.getStringExtra("reset_token");
            android.util.Log.d("CreateNewPassword", "Token from Extra: " + (resetToken != null ? "'" + resetToken + "' (len:" + resetToken.length() + ")" : "null"));
        }

        if (resetToken != null) {
            resetToken = resetToken.trim();
        }

        if (resetToken == null || resetToken.isEmpty()) {
            android.util.Log.e("CreateNewPassword", "No reset token found in intent.");
            SnackbarManager.show(SnackbarManager.Type.ERROR, "Invalid reset link.");
            new android.os.Handler().postDelayed(this::finish, 2500);
            return;
        }

        validateToken();
    }

    private void validateToken() {
        startLoading();
        android.util.Log.d("CreateNewPassword", "Checking database for token: '" + resetToken + "'");
        
        // URL encode the token just in case it has special characters
        String encodedToken = Uri.encode(resetToken);
        String query = "reset_token=eq." + encodedToken;
        
        android.util.Log.d("CreateNewPassword", "Supabase Query: " + query);

        SupabaseDatabaseHelper.select("profiles", query, new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                stopLoading();
                android.util.Log.d("CreateNewPassword", "Success: Received " + (users != null ? users.size() : 0) + " users");

                if (users == null || users.isEmpty()) {
                    android.util.Log.e("CreateNewPassword", "FAILURE: Token not found in database: " + resetToken);
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "This reset link has expired or has already been used.");
                    new android.os.Handler().postDelayed(CreateNewPasswordActivity.this::finish, 2500);
                    return;
                }

                User user = users.get(0);
                android.util.Log.d("CreateNewPassword", "MATCH FOUND: Email=" + user.getEmail() + ", Used=" + user.getResetTokenUsed());

                // Check if token was already used
                if (user.getResetTokenUsed()) {
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "This reset link has already been used.");
                    new android.os.Handler().postDelayed(CreateNewPasswordActivity.this::finish, 2500);
                    return;
                }

                // Check if token expired (1 hour limit)
                if (user.getResetTokenExpiresAt() != null) {
                    try {
                        java.util.Date expiryDate = ValidationUtils.parseIso8601(user.getResetTokenExpiresAt());
                        
                        if (expiryDate != null && expiryDate.before(new java.util.Date())) {
                            android.util.Log.e("CreateNewPassword", "Token EXPIRED: " + user.getResetTokenExpiresAt());
                            SnackbarManager.show(SnackbarManager.Type.ERROR, "This reset link has expired (1 hour limit). Please request a new one.");
                            new android.os.Handler().postDelayed(CreateNewPasswordActivity.this::finish, 2500);
                            return;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("CreateNewPassword", "Expiry check error: " + e.getMessage());
                    }
                }
                
                currentUser = user;
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Link verified. You can now reset your password.");
            }

            @Override
            public void onFailure(String errorMessage) {
                stopLoading();
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Error validating link: " + errorMessage);
                new android.os.Handler().postDelayed(CreateNewPasswordActivity.this::finish, 2500);
            }
        });
    }

    private void startLoading() {
        btnReset.setEnabled(false);
        if (originalBackgroundTint == null) {
            originalBackgroundTint = btnReset.getBackgroundTintList();
        }
        btnReset.setText("");
        btnReset.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        loader.setVisibility(View.VISIBLE);
        loader.playAnimation();
    }

    private void stopLoading() {
        loader.cancelAnimation();
        loader.setVisibility(View.GONE);
        if (originalBackgroundTint != null) {
            btnReset.setBackgroundTintList(originalBackgroundTint);
        }
        btnReset.setText("Reset Password");
        btnReset.setEnabled(true);
    }

    private void validateAndReset() {
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (newPassword.isEmpty()) {
            ErrorHelper.setFieldError(tilNewPassword, "Password is required");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            SnackbarManager.show(SnackbarManager.Type.ERROR, "Passwords do not match. Please enter the same password.");
            return;
        }

        performReset(newPassword);
    }

    private void performReset(String newPassword) {
        startLoading();
        
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("action", "update-password");
        params.put("token", resetToken);
        params.put("password", newPassword);

        SupabaseDatabaseHelper.callEdgeFunction("send-reset-email", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                stopLoading();
                android.util.Log.d("CreateNewPassword", "Reset Success: " + result);
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Password updated successfully! You can now log in.");
                Intent intent = new Intent(CreateNewPasswordActivity.this, UserLoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                stopLoading();
                android.util.Log.e("CreateNewPassword", "Reset Failure: " + errorMessage);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to update password: " + errorMessage);
            }
        });
    }

    private void setupKeyboardListener() {
        if (createNewPasswordRoot == null || keyboardSpacer == null) return;

        createNewPasswordRoot.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                android.graphics.Rect r = new android.graphics.Rect();
                createNewPasswordRoot.getWindowVisibleDisplayFrame(r);
                int screenHeight = createNewPasswordRoot.getRootView().getHeight();
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
