package com.sas.lostandfound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Interface for system administrators to manage the application.
 */

public class AdminLoginActivity extends AppCompatActivity {

    private TextInputEditText etAdminId, etAdminPassword;
    private MaterialButton btnAdminSignIn;
    private TextView tvBackToUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        etAdminId = findViewById(R.id.etAdminId);
        etAdminPassword = findViewById(R.id.etAdminPassword);
        btnAdminSignIn = findViewById(R.id.btnAdminSignIn);
        tvBackToUser = findViewById(R.id.tvBackToUser);

        btnAdminSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String adminId = etAdminId.getText().toString().trim();
                String password = etAdminPassword.getText().toString().trim();

                if (TextUtils.isEmpty(adminId) || TextUtils.isEmpty(password)) {
                    Toast.makeText(AdminLoginActivity.this, "Security credentials required", Toast.LENGTH_SHORT).show();
                } else {
                    // Demo Admin Login
                    SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
                    prefs.edit().putBoolean("isAdminLoggedIn", true).apply();

                    Toast.makeText(AdminLoginActivity.this, "Access Granted. Welcome Administrator.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(AdminLoginActivity.this, DashboardActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            }
        });

        tvBackToUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}