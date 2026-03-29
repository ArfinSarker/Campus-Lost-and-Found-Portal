package com.sas.lostandfound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface for system administrators to manage the application.
 */
public class AdminLoginActivity extends AppCompatActivity {

    private static final String TAG = "AdminLogin";
    private TextInputEditText etAdminId, etAdminPassword;
    private MaterialButton btnAdminSignIn;
    private TextView tvBackToUser;

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
                            Log.d(TAG, "Auth Success. UID: " + user.getUid());
                            checkStatusAndPrivileges(user.getUid(), dbId);
                        }
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Invalid credentials";
                        Toast.makeText(AdminLoginActivity.this, "Authentication failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkStatusAndPrivileges(String uid, String dbId) {
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
                        // Prepare updates to ensure role and mapping exist
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("Users/" + dbId + "/role", "admin");
                        updates.put("UIDToUniversityID/" + uid, dbId);

                        mDatabase.updateChildren(updates)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        loginSuccess(true, dbId);
                                    } else {
                                        mAuth.signOut();
                                        Toast.makeText(AdminLoginActivity.this,
                                                "Failed to update admin privileges", Toast.LENGTH_SHORT).show();
                                    }
                                });
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
