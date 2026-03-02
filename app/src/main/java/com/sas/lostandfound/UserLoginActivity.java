package com.sas.lostandfound;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
    private TextView tvForgotPassword, tvRegister, tvAdminLogin;
    private LinearLayout llAdminLogin;
    private ImageView ivAdminIcon;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        etUniversityId = findViewById(R.id.etEmail); // Using existing ID but mapping to University ID
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);
        tvAdminLogin = findViewById(R.id.tvAdminLogin);
        llAdminLogin = findViewById(R.id.llAdminLogin);
        ivAdminIcon = findViewById(R.id.ivAdminIcon);

        setupClickableRegister();

        btnSignIn.setOnClickListener(v -> loginWithUniversityId());

        tvForgotPassword.setOnClickListener(v ->
                Toast.makeText(UserLoginActivity.this, "Forgot password clicked", Toast.LENGTH_SHORT).show()
        );

        llAdminLogin.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    applyGradient(tvAdminLogin);
                    ivAdminIcon.setColorFilter(Color.parseColor("#2196F3"));
                } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                    removeGradient(tvAdminLogin);
                    ivAdminIcon.setColorFilter(Color.parseColor("#757575"));
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        v.performClick();
                        startActivity(new Intent(UserLoginActivity.this, AdminLoginActivity.class));
                    }
                }
                return true;
            }
        });
    }

    private void loginWithUniversityId() {
        String universityId = etUniversityId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (universityId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSignIn.setEnabled(false);
        Toast.makeText(this, "Verifying ID...", Toast.LENGTH_SHORT).show();

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
                btnSignIn.setEnabled(true);
                Toast.makeText(UserLoginActivity.this, "University ID not found", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnSignIn.setEnabled(true);
                Toast.makeText(UserLoginActivity.this, "Database error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performFirebaseLogin(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnSignIn.setEnabled(true);
                    if (task.isSuccessful()) {
                        Toast.makeText(UserLoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(UserLoginActivity.this, CampusDashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(UserLoginActivity.this,
                                "Login failed: " + (task.getException() != null ? task.getException().getMessage() : "Wrong password"),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupClickableRegister() {
        String text = getString(R.string.register_link);
        SpannableString ss = new SpannableString(text);

        String registerWord = "Register";
        int start = text.indexOf(registerWord);
        if (start == -1) {
            registerWord = "REGISTER";
            start = text.indexOf(registerWord);
        }
        
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
                }
            };
            ss.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        tvRegister.setText(ss);
        tvRegister.setMovementMethod(LinkMovementMethod.getInstance());

        tvRegister.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                tvRegister.setTextColor(Color.parseColor("#2196F3"));
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                tvRegister.setTextColor(Color.parseColor("#757575"));
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    v.performClick();
                }
            }
            return false;
        });
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