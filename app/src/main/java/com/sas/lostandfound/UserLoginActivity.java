package com.sas.lostandfound;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class UserLoginActivity extends AppCompatActivity {

    private TextInputEditText etUniversityId, etPassword;
    private MaterialButton btnSignIn;
    private ProgressBar progressBar;
    private TextView tvForgotPassword, tvRegister, tvAdminLogin;
    private LinearLayout llAdminLogin;
    private ImageView ivAdminIcon;
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
        setupToolbar();
        setupScrollListener();
        setupClickableRegister();

        btnSignIn.setOnClickListener(v -> loginWithUniversityId());

        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(UserLoginActivity.this, "Reset link will be sent to your registered email.", Toast.LENGTH_SHORT).show()
        );

        llAdminLogin.setOnClickListener(v -> {
            startActivity(new Intent(UserLoginActivity.this, AdminLoginActivity.class));
        });

        llAdminLogin.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                applyGradient(tvAdminLogin);
                ivAdminIcon.setColorFilter(Color.parseColor("#2196F3"));
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                removeGradient(tvAdminLogin);
                ivAdminIcon.setColorFilter(Color.parseColor("#757575"));
            }
            return false;
        });
    }

    private void initializeViews() {
        etUniversityId = findViewById(R.id.etEmail); 
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        progressBar = findViewById(R.id.progressBar);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);
        tvAdminLogin = findViewById(R.id.tvAdminLogin);
        llAdminLogin = findViewById(R.id.llAdminLogin);
        ivAdminIcon = findViewById(R.id.ivAdminIcon);
        toolbar = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.appBarLayout);
        nestedScrollView = findViewById(R.id.nestedScrollView);
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
            // Initial state
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
            getWindow().getDecorView().setSystemUiVisibility(0); // White icons
        } else {
            getWindow().setStatusBarColor(Color.WHITE);
            appBarLayout.setBackgroundColor(Color.WHITE);
            toolbar.setBackgroundColor(Color.WHITE);
            toolbar.setNavigationIconTint(ContextCompat.getColor(this, R.color.textPrimary));
            // Dark icons for white status bar
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

    private void loginWithUniversityId() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_LONG).show();
            return;
        }

        String universityId = etUniversityId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (universityId.isEmpty()) {
            etUniversityId.setError("Required");
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Required");
            return;
        }

        showLoading(true);

        // Query database to find email associated with this University ID
        Query query = mDatabase.child("Users").orderByChild("universityId").equalTo(universityId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String email = userSnapshot.child("email").getValue(String.class);
                        if (email != null) {
                            performFirebaseLogin(email, password);
                            return;
                        }
                    }
                }
                showLoading(false);
                Toast.makeText(UserLoginActivity.this, "University ID not found", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showLoading(false);
                Toast.makeText(UserLoginActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performFirebaseLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(UserLoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(UserLoginActivity.this, CampusDashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        showLoading(false);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Invalid credentials";
                        Toast.makeText(UserLoginActivity.this, "Login failed: " + errorMsg, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupClickableRegister() {
        String text = getString(R.string.register_link);
        SpannableString ss = new SpannableString(android.text.Html.fromHtml(text));

        String rawText = ss.toString();
        String registerWord = "Register";
        int start = rawText.indexOf(registerWord);
        
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

    private void applyGradient(TextView textView) {
        Shader shader = new LinearGradient(0, 0, textView.getWidth(), textView.getLineHeight(),
                new int[]{Color.parseColor("#2196F3"), Color.parseColor("#673AB7")},
                null, Shader.TileMode.CLAMP);
        textView.getPaint().setShader(shader);
        textView.invalidate();
    }

    private void removeGradient(TextView textView) {
        textView.getPaint().setShader(null);
        textView.setTextColor(Color.parseColor("#757575"));
        textView.invalidate();
    }
}
