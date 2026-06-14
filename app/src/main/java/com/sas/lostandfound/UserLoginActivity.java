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
import android.graphics.Rect;
import android.view.View;
import android.view.ViewTreeObserver;
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
    private android.widget.ImageButton btnBack;
    private AppBarLayout appBarLayout;
    private android.content.res.ColorStateList originalBackgroundTint;
    private View keyboardSpacer;
    private View loginRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        initializeViews();
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout);
        }
        setupUserTypeDropdown();
        setupToolbar();
        setupClickableRegister();
        setupKeyboardListener();

        btnLogin.setOnClickListener(v -> loginUser());

        tvForgotPassword.setOnClickListener(
                v -> startActivity(new Intent(UserLoginActivity.this, ForgotPasswordActivity.class)));
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
        btnBack = findViewById(R.id.btnBack);
        appBarLayout = findViewById(R.id.appBarLayout);
        loginRoot = findViewById(R.id.loginRoot);
        keyboardSpacer = findViewById(R.id.keyboardSpacer);
    }

    private void setupUserTypeDropdown() {
        String[] userTypes = { "Student", "Staff", "Admin" };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.dropdown_item, userTypes);
        actvUserType.setAdapter(adapter);
        actvUserType.setOnClickListener(v -> actvUserType.showDropDown());
    }

    private void setupToolbar() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Intent intent = new Intent(UserLoginActivity.this, DashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
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
        if (originalBackgroundTint == null) {
            originalBackgroundTint = btnLogin.getBackgroundTintList();
        }
        btnLogin.setText("");
        btnLogin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        loader.setVisibility(View.VISIBLE);
        loader.playAnimation();
    }

    private void stopLoading() {
        loader.cancelAnimation();
        loader.setVisibility(View.GONE);
        if (originalBackgroundTint != null) {
            btnLogin.setBackgroundTintList(originalBackgroundTint);
        }
        btnLogin.setText(R.string.sign_in);
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
        String query = "university_id=eq." + android.net.Uri.encode(universityId) +
                       "&user_type=eq." + android.net.Uri.encode(userType) +
                       "&password=eq." + android.net.Uri.encode(password) +
                       "&select=*&limit=1";
        SupabaseDatabaseHelper.select("profiles", query, new TypeToken<List<User>>() {
        }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users == null || users.isEmpty()) {
                    if ("Admin".equalsIgnoreCase(userType)) {
                        checkPendingAdminRequest(universityId);
                    } else {
                        stopLoading();
                        ErrorHelper.showError(btnLogin, "Invalid University ID, Role, or Password.");
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

                    boolean dbIsAdmin = "admin".equalsIgnoreCase(user.getRole()) ||
                            user.isAdmin() ||
                            "Admin".equalsIgnoreCase(user.getUserType());

                    String actualRole = dbIsAdmin ? "Admin" : user.getUserType();

                    if (!userType.equalsIgnoreCase(actualRole)) {
                        stopLoading();
                        ErrorHelper.showError(btnLogin, "Selected role does not match your account.");
                        return;
                    }

                    performSupabaseLogin(user.getEmail(), password, userType, dbIsAdmin, universityId, user);

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
        String query = "university_id=eq." + android.net.Uri.encode(universityId) + "&select=*&limit=1";
        SupabaseDatabaseHelper.select("admin_requests", query, new TypeToken<List<AdminRequest>>() {
        }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<AdminRequest>>() {
            @Override
            public void onSuccess(List<AdminRequest> requests) {
                stopLoading();
                if (requests != null && !requests.isEmpty()) {
                    ErrorHelper.showError(btnLogin,
                            "Your account request is still pending approval. Please try again later.");
                } else {
                    ErrorHelper.showError(btnLogin, "Invalid University ID, Role, or Password.");
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                stopLoading();
                ErrorHelper.showError(btnLogin, "Database error: " + errorMessage);
            }
        });
    }

    private void performSupabaseLogin(String email, String password, String userType, boolean isMainAdmin, String dbId,
            User user) {

        SupabaseAuthHelper.login(email, password, new SupabaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String userId, String accessToken, String refreshToken) {
                saveLoginState(userType, isMainAdmin, dbId, userId, accessToken, refreshToken, user);
                
                // Upload cached FCM token if present
                String fcmToken = getSharedPreferences("MyApp", MODE_PRIVATE).getString("fcm_token", "");
                if (!fcmToken.isEmpty()) {
                    LostAndFoundApplication.uploadFcmToken(fcmToken);
                }

                ModeManager.setMode(UserLoginActivity.this, ModeManager.MODE_USER);
                LostAndFoundApplication.scheduleNotificationWorker(UserLoginActivity.this);
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

    private void saveLoginState(String userType, boolean isMainAdmin, String dbId, String authId, String accessToken,
            String refreshToken, User user) {
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit()
                .putString("userType", userType)
                .putBoolean("isAdminLoggedIn", "Admin".equalsIgnoreCase(userType) || isMainAdmin)
                .putBoolean("isMainAdmin", "Admin".equalsIgnoreCase(userType) || isMainAdmin)
                .putString("universityId", dbId)
                .putString("adminId", dbId)
                .putString("authId", authId)
                .putString("accessToken", accessToken)
                .putString("refreshToken", refreshToken);

        if (user != null) {
            String name = user.getName();
            if (name == null || name.trim().isEmpty()) {
                name = user.getFullName();
            }
            editor.putString("cachedUserName", name)
                    .putString("cachedUserEmail", user.getEmail())
                    .putString("cachedUserPhone", user.getPhone())
                    .putString("cachedUserGender", user.getGender())
                    .putString("cachedUserDepartment", user.getDepartment())
                    .putString("cachedUserBatch", user.getBatch())
                    .putString("cachedUserLevelTerm", user.getLevelTerm())
                    .putString("cachedUserSection", user.getSection())
                    .putString("cachedUserDesignation", user.getDesignation())
                    .putString("cachedProfileImageUrl", user.getProfileImageUrl());
        }

        editor.apply();

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

    private void setupKeyboardListener() {
        if (loginRoot == null || keyboardSpacer == null)
            return;

        loginRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                loginRoot.getWindowVisibleDisplayFrame(r);
                int screenHeight = loginRoot.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    if (keyboardSpacer.getVisibility() != View.VISIBLE) {
                        keyboardSpacer.setVisibility(View.VISIBLE);
                    }
                    // tvRegister has android:layout_marginBottom="24dp".
                    // To stop scrolling EXACTLY at the Register text without any extra space,
                    // we set the spacer height to keypadHeight minus the tvRegister bottom margin.
                    int tvRegisterMarginBottom = (int) (24 * getResources().getDisplayMetrics().density);
                    int targetHeight = Math.max(0, keypadHeight - tvRegisterMarginBottom);
                    if (keyboardSpacer.getLayoutParams().height != targetHeight) {
                        keyboardSpacer.getLayoutParams().height = targetHeight;
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