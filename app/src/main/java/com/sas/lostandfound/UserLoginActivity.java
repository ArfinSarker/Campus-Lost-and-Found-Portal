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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserLoginActivity extends AppCompatActivity {

    private TextInputEditText etUniversityId, etPassword;
    private AutoCompleteTextView actvUserType;
    private MaterialButton btnSignIn;
    private ProgressBar progressBar;
    private TextView tvForgotPassword, tvRegister;
    private MaterialToolbar toolbar;
    private AppBarLayout appBarLayout;
    private NestedScrollView nestedScrollView;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initializeViews();
        setupUserTypeDropdown();
        setupToolbar();
        setupScrollListener();
        setupClickableRegister();

        btnSignIn.setOnClickListener(v -> loginUser());

        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(UserLoginActivity.this, "Reset link will be sent to your registered email.", Toast.LENGTH_SHORT).show()
        );
    }

    private void initializeViews() {
        actvUserType = findViewById(R.id.actvUserType);
        etUniversityId = findViewById(R.id.etUniversityId);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        progressBar = findViewById(R.id.progressBar);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);
        toolbar = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.appBarLayout);
        nestedScrollView = findViewById(R.id.nestedScrollView);
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
        }
    }

    private void setupScrollListener() {
        if (nestedScrollView != null && appBarLayout != null) {
            updateStatusBarColor(false);
            nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (scrollY > 0) {
                    updateStatusBarColor(true);
                } else {
                    updateStatusBarColor(false);
                }
            });
        }
    }

    private void updateStatusBarColor(boolean isScrolled) {
        if (isScrolled) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.primaryDarkColor));
            appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor));
            toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryColor));
            toolbar.setNavigationIconTint(Color.WHITE);
            getWindow().getDecorView().setSystemUiVisibility(0);
        } else {
            getWindow().setStatusBarColor(Color.WHITE);
            appBarLayout.setBackgroundColor(Color.WHITE);
            toolbar.setBackgroundColor(Color.WHITE);
            toolbar.setNavigationIconTint(ContextCompat.getColor(this, R.color.textPrimary));
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }

    private void showLoading(boolean isLoading) {
        btnSignIn.setEnabled(!isLoading);
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnSignIn.setText(isLoading ? "" : getString(R.string.sign_in));
    }

    private void loginUser() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_LONG).show();
            return;
        }

        String userType = actvUserType.getText().toString().trim();
        String id = etUniversityId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (userType.isEmpty()) {
            Toast.makeText(this, "Please select User Type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (id.isEmpty()) {
            etUniversityId.setError("Required");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Required");
            return;
        }

        showLoading(true);

        startLoginFlow(id, password, userType);
    }

    private void startLoginFlow(String universityId, String password, String userType) {
        mDatabase.child("Users").child(universityId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        if (userType.equalsIgnoreCase(user.getUserType())) {
                            performFirebaseLogin(user.getEmail(), password, userType, user.isAdmin(), universityId);
                        } else {
                            showLoading(false);
                            Toast.makeText(UserLoginActivity.this, "User Type mismatch. You registered as " + user.getUserType(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        showLoading(false);
                        Toast.makeText(UserLoginActivity.this, "Error parsing user data.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showLoading(false);
                    // Updated error message to match requirement for deleted users or non-existent accounts
                    Toast.makeText(UserLoginActivity.this, "No account exists with this University ID. Please register again.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(UserLoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performFirebaseLogin(String email, String password, String userType, boolean isMainAdmin, String dbId) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        saveLoginState(userType, isMainAdmin, dbId);
                        Toast.makeText(UserLoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                        Intent intent;
                        if ("Admin".equalsIgnoreCase(userType)) {
                            intent = new Intent(this, AdminDashboardActivity.class);
                        } else {
                            intent = new Intent(this, CampusDashboardActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    } else {
                        showLoading(false);
                        String error = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(UserLoginActivity.this, "Login failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveLoginState(String userType, boolean isMainAdmin, String dbId) {
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        prefs.edit()
                .putString("userType", userType)
                .putBoolean("isAdminLoggedIn", "Admin".equalsIgnoreCase(userType))
                .putBoolean("isMainAdmin", "Admin".equalsIgnoreCase(userType))
                .putString("universityId", dbId)
                .putString("adminId", dbId)
                .apply();
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
                    startActivity(new Intent(UserLoginActivity.this, UserRegistrationActivity.class));
                }
                @Override
                public void updateDrawState(@NonNull TextPaint ds) {
                    super.updateDrawState(ds);
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
