package com.sas.lostandfound;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import com.airbnb.lottie.LottieAnimationView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class UserLoginActivity extends AppCompatActivity {

    private TextInputEditText etUniversityId, etPassword;
    private TextInputLayout tilUniversityId, tilPassword, tilUserType;
    private AutoCompleteTextView actvUserType;
    private MaterialButton btnLogin;
    private LottieAnimationView loader;
    private TextView tvForgotPassword, tvRegister;
    private MaterialToolbar toolbar;
    private AppBarLayout appBarLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        initializeViews();
        setupUserTypeDropdown();
        setupToolbar();
        setupClickableRegister();

        btnLogin.setOnClickListener(v -> loginUser());

        tvForgotPassword.setOnClickListener(v ->
                startActivity(new Intent(UserLoginActivity.this, ForgotPasswordActivity.class))
        );
    }

    private void initializeViews() {
        actvUserType = findViewById(R.id.actvUserType);
        etUniversityId = findViewById(R.id.etUniversityId);
        etPassword = findViewById(R.id.etPassword);
        tilUniversityId = findViewById(R.id.tilUniversityId);
        tilPassword = findViewById(R.id.tilPassword);
        tilUserType = findViewById(R.id.tilUserType);

        ErrorHelper.attachToTextInputLayout(tilUniversityId);
        ErrorHelper.attachToTextInputLayout(tilPassword);
        ErrorHelper.attachToTextInputLayout(tilUserType);

        btnLogin = findViewById(R.id.btnLogin);
        loader = findViewById(R.id.loginLoader);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);
        toolbar = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.appBarLayout);
    }

    private void setupUserTypeDropdown() {
        String[] userTypes = {"Student", "Staff", "Admin"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, userTypes);
        actvUserType.setAdapter(adapter);
        actvUserType.setOnClickListener(v -> actvUserType.showDropDown());
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
            }
            toolbar.setNavigationOnClickListener(v -> {
                Intent intent = new Intent(UserLoginActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });

            if (appBarLayout != null) {
                HeaderColorHelper.setup(this, appBarLayout, toolbar);
            }
        }
    }

    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    private void startLoading() {
        btnLogin.setEnabled(false);
        btnLogin.setVisibility(View.INVISIBLE);
        loader.setVisibility(View.VISIBLE);
        loader.playAnimation();
    }

    private void stopLoading() {
        loader.cancelAnimation();
        loader.setVisibility(View.GONE);
        btnLogin.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(true);
    }

    private void loginUser() {

        if (!isNetworkAvailable()) {
            ErrorHelper.showError(btnLogin, "No internet connection. Please check your network.");
            return;
        }

        String userType = actvUserType.getText().toString().trim();
        String id = etUniversityId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (userType.isEmpty()) {
            ErrorHelper.setFieldError(tilUserType, "Please select User Type");
            return;
        }

        if (id.isEmpty()) {
            ErrorHelper.setFieldError(tilUniversityId, "University ID is required");
            return;
        }

        if (password.isEmpty()) {
            ErrorHelper.setFieldError(tilPassword, "Password is required");
            return;
        }

        startLoading();
        startLoginFlow(id, password, userType);
    }

    private void startLoginFlow(String universityId, String password, String userType) {
        String query = "university_id=eq." + universityId + "&select=*&limit=1";
        SupabaseDatabaseHelper.select("profiles", query, new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users == null || users.isEmpty()) {
                    if ("Admin".equalsIgnoreCase(userType)) {
                        checkPendingAdminRequest(universityId);
                    } else {
                        stopLoading();
                        ErrorHelper.showError(btnLogin, "No account found with this University ID.");
                    }
                    return;
                }

                try {
                    User user = users.get(0);

                    if (user == null || user.getEmail() == null) {
                        stopLoading();
                        ErrorHelper.showError(btnLogin, "Invalid user data. Please contact support.");
                        return;
                    }

                    boolean dbIsAdmin =
                            "admin".equalsIgnoreCase(user.getRole()) ||
                                    user.isAdmin() ||
                                    "Admin".equalsIgnoreCase(user.getUserType());

                    String actualRole = dbIsAdmin ? "Admin" : user.getUserType();

                    if (!userType.equalsIgnoreCase(actualRole)) {
                        stopLoading();
                        ErrorHelper.showError(btnLogin, "Selected role does not match your account.");
                        return;
                    }

                    performSupabaseLogin(user.getEmail(), password, userType, dbIsAdmin, universityId);

                } catch (Exception e) {
                    stopLoading();
                    ErrorHelper.showError(btnLogin, "Something went wrong. Please try again.");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                stopLoading();
                ErrorHelper.showError(btnLogin, "Database error: " + errorMessage);
            }
        });
    }

    private void checkPendingAdminRequest(String universityId) {
        String query = "university_id=eq." + universityId + "&select=*&limit=1";
        SupabaseDatabaseHelper.select("admin_requests", query, new TypeToken<List<AdminRequest>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<AdminRequest>>() {
            @Override
            public void onSuccess(List<AdminRequest> requests) {
                stopLoading();
                if (requests != null && !requests.isEmpty()) {
                    ErrorHelper.showError(btnLogin, "Your account request is still pending approval. Please try again later.");
                } else {
                    ErrorHelper.showError(btnLogin, "No account found with this University ID.");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                stopLoading();
                ErrorHelper.showError(btnLogin, "Database error: " + errorMessage);
            }
        });
    }

    private void performSupabaseLogin(String email, String password, String userType, boolean isMainAdmin, String dbId) {

        SupabaseAuthHelper.login(email, password, new SupabaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String userId, String accessToken, String refreshToken) {
                saveLoginState(userType, isMainAdmin, dbId, userId, accessToken, refreshToken);
                ModeManager.setMode(UserLoginActivity.this, ModeManager.MODE_USER);
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Login successful");

                Intent intent = new Intent(UserLoginActivity.this, CampusDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                stopLoading();
                ErrorHelper.showError(btnLogin, errorMessage);
            }
        });
    }

    private void saveLoginState(String userType, boolean isMainAdmin, String dbId, String authId, String accessToken, String refreshToken) {
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        prefs.edit()
                .putString("userType", userType)
                .putBoolean("isAdminLoggedIn", "Admin".equalsIgnoreCase(userType) || isMainAdmin)
                .putBoolean("isMainAdmin", "Admin".equalsIgnoreCase(userType) || isMainAdmin)
                .putString("universityId", dbId)
                .putString("adminId", dbId)
                .putString("authId", authId)
                .putString("accessToken", accessToken)
                .putString("refreshToken", refreshToken)
                .apply();
        
        // Update helper with token
        SupabaseDatabaseHelper.setAuthToken(accessToken);
    }

    private void setupClickableRegister() {
        String fullText = getString(R.string.register_link);
        Spanned spanned = Html.fromHtml(fullText, Html.FROM_HTML_MODE_LEGACY);
        SpannableString ss = new SpannableString(spanned);

        String registerWord = "Register";
        int start = spanned.toString().indexOf(registerWord);

        if (start != -1) {
            int end = start + registerWord.length();

            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    if (ItemNavigationUtils.canNavigate()) {
                        startActivity(new Intent(UserLoginActivity.this, UserRegistrationActivity.class));
                    }
                }

                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    ds.setUnderlineText(false);
                    ds.setColor(Color.parseColor("#2196F3"));
                    ds.setFakeBoldText(true);
                }
            };

            ss.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvRegister.setText(ss);
        tvRegister.setMovementMethod(LinkMovementMethod.getInstance());
    }
}