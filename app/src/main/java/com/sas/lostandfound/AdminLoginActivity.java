package com.sas.lostandfound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface for system administrators to manage the application.
 */
public class AdminLoginActivity extends AppCompatActivity {

    private TextInputEditText etAdminId, etAdminPassword;
    private MaterialButton btnAdminSignIn;
    private TextView tvBackToUser;

    // Hardcoded Admin credentials
    private static final String PRIMARY_ADMIN_ID = "0802410205101019";
    private static final String PRIMARY_ADMIN_PASS = "123456789";
    private static final String PRIMARY_ADMIN_EMAIL = "m.shamsularfinsarkernayan@gmail.com";
    private static final String PRIMARY_ADMIN_NAME = "Md. Shamsul Arfin Sarker";
    private static final String PRIMARY_ADMIN_PHONE = "+8801819966626";
    private static final String PRIMARY_ADMIN_DESIGNATION = "Admin";
    
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        etAdminId = findViewById(R.id.etAdminId);
        etAdminPassword = findViewById(R.id.etAdminPassword);
        btnAdminSignIn = findViewById(R.id.btnAdminSignIn);
        tvBackToUser = findViewById(R.id.tvBackToUser);

        btnAdminSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptAdminLogin();
            }
        });

        tvBackToUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void attemptAdminLogin() {
        final String adminId = etAdminId.getText().toString().trim();
        final String password = etAdminPassword.getText().toString().trim();

        if (TextUtils.isEmpty(adminId) || TextUtils.isEmpty(password)) {
            Toast.makeText(AdminLoginActivity.this, "University ID and Password required", Toast.LENGTH_SHORT).show();
            return;
        }

        // Enforce login via University ID only
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(adminId).matches()) {
            etAdminId.setError("Login with University ID only");
            return;
        }

        // Special handling for hardcoded Admin
        if (PRIMARY_ADMIN_ID.equals(adminId) && PRIMARY_ADMIN_PASS.equals(password)) {
            performAuthLogin(PRIMARY_ADMIN_EMAIL, password, PRIMARY_ADMIN_ID);
            return;
        }

        // Regular admin login flow
        searchIdAndLogin(adminId, password);
    }

    private void searchIdAndLogin(String adminId, String password) {
        List<Object> variations = new ArrayList<>();
        variations.add(adminId);
        if (adminId.startsWith("0")) variations.add(adminId.substring(1));
        else variations.add("0" + adminId);
        
        try { variations.add(Long.parseLong(adminId)); } catch (Exception ignored) {}

        collectResultsAndLogin(variations, 0, password);
    }

    private void collectResultsAndLogin(List<Object> variations, int index, String password) {
        if (index >= variations.size()) {
            Toast.makeText(AdminLoginActivity.this, "Account not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        Object val = variations.get(index);
        Query query = mDatabase.child("Users").orderByChild("universityId").equalTo(val.toString());
        if (val instanceof Long) query = mDatabase.child("Users").orderByChild("universityId").equalTo((Long) val);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.hasChildren()) {
                    DataSnapshot userSnap = snapshot.getChildren().iterator().next();
                    String email = userSnap.child("email").getValue(String.class);
                    String dbId = userSnap.getKey();
                    if (email != null) performAuthLogin(email, password, dbId);
                    else collectResultsAndLogin(variations, index + 1, password);
                } else {
                    collectResultsAndLogin(variations, index + 1, password);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                collectResultsAndLogin(variations, index + 1, password);
            }
        });
    }

    private void performAuthLogin(String email, String password, String dbId) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkStatusAndPrivileges(user.getUid(), dbId);
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Invalid credentials";
                        Toast.makeText(AdminLoginActivity.this, "Authentication failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkStatusAndPrivileges(String uid, String dbId) {
        // Special Admin Bypass
        if (PRIMARY_ADMIN_ID.equals(dbId) || PRIMARY_ADMIN_ID.equals(etAdminId.getText().toString().trim())) {
            ensureAdminInDatabase(uid, dbId);
            return;
        }

        // Regular admin checks
        mDatabase.child("adminRequests").child(dbId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    mAuth.signOut();
                    Toast.makeText(AdminLoginActivity.this, "Access Denied: Request pending.", Toast.LENGTH_LONG).show();
                } else {
                    mDatabase.child("DeniedAdminRequests").child(dbId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot deniedSnap) {
                            if (deniedSnap.exists()) {
                                mAuth.signOut();
                                Toast.makeText(AdminLoginActivity.this, "Access Denied: Admin request rejected.", Toast.LENGTH_LONG).show();
                            } else {
                                validateAdminAccess(uid, dbId);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) { validateAdminAccess(uid, dbId); }
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { validateAdminAccess(uid, dbId); }
        });
    }

    private void ensureAdminInDatabase(String uid, String dbId) {
        // Force the DB record to match the admin details
        User admin = new User();
        admin.setUserId(PRIMARY_ADMIN_ID);
        admin.setFullName(PRIMARY_ADMIN_NAME);
        admin.setUniversityId(PRIMARY_ADMIN_ID);
        admin.setEmail(PRIMARY_ADMIN_EMAIL);
        admin.setPassword(PRIMARY_ADMIN_PASS);
        admin.setPhoneNumber(PRIMARY_ADMIN_PHONE);
        admin.setDesignation(PRIMARY_ADMIN_DESIGNATION);
        admin.setRole("admin");
        admin.setUserType("Admin");
        admin.setRequestStatus("approved");
        admin.setAdmin(true);

        mDatabase.child("Users").child(PRIMARY_ADMIN_ID).setValue(admin)
                .addOnCompleteListener(task -> {
                    mDatabase.child("UIDToUniversityID").child(uid).setValue(PRIMARY_ADMIN_ID);
                    loginSuccess(true, PRIMARY_ADMIN_ID);
                });
    }

    private void validateAdminAccess(String uid, String dbId) {
        mDatabase.child("Users").child(dbId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isAdmin = snapshot.child("isAdmin").getValue(Boolean.class);
                    String type = snapshot.child("userType").getValue(String.class);
                    String role = snapshot.child("role").getValue(String.class);

                    boolean isAdminRole = "admin".equals(role) || "Admin".equalsIgnoreCase(type) || (isAdmin != null && isAdmin);

                    if (isAdminRole) {
                        loginSuccess(true, dbId);
                    } else {
                        mAuth.signOut();
                        Toast.makeText(AdminLoginActivity.this, "Access Denied: Unauthorized.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    mAuth.signOut();
                    Toast.makeText(AdminLoginActivity.this, "Account details not found.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                mAuth.signOut();
                Toast.makeText(AdminLoginActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginSuccess(boolean isAdmin, String adminId) {
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        prefs.edit().putBoolean("isAdminLoggedIn", true)
                .putBoolean("isAdmin", isAdmin)
                .putString("adminId", adminId)
                .putString("universityId", adminId)
                .putString("userType", "Admin")
                .apply();

        Toast.makeText(AdminLoginActivity.this, "Welcome, Administrator", Toast.LENGTH_LONG).show();
        startActivity(new Intent(AdminLoginActivity.this, AdminDashboardActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
