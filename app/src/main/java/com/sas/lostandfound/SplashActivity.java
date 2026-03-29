package com.sas.lostandfound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Initial splash screen displayed when the app launches.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000;
    private static final String TAG = "SplashActivity";
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkSessionAndRedirect();
        }, SPLASH_DELAY);
    }

    private void checkSessionAndRedirect() {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        
        if (auth.getCurrentUser() != null) {
            String universityId = prefs.getString("universityId", "");
            
            if (!universityId.isEmpty()) {
                DatabaseReference mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();
                mDatabase.child("Users").child(universityId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Intent intent;
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                boolean dbIsAdmin = "admin".equalsIgnoreCase(user.getRole()) || user.isAdmin() || "Admin".equalsIgnoreCase(user.getUserType());
                                if (dbIsAdmin) {
                                    intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
                                } else {
                                    intent = new Intent(SplashActivity.this, CampusDashboardActivity.class);
                                }
                            } else {
                                intent = new Intent(SplashActivity.this, DashboardActivity.class);
                            }
                        } else {
                            intent = new Intent(SplashActivity.this, DashboardActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Database error: " + error.getMessage());
                        fallbackRedirect(prefs);
                    }
                });
            } else {
                fallbackRedirect(prefs);
            }
        } else {
            // Not logged in
            startActivity(new Intent(SplashActivity.this, DashboardActivity.class));
            finish();
        }
    }

    private void fallbackRedirect(SharedPreferences prefs) {
        String userType = prefs.getString("userType", "");
        boolean isAdminLoggedIn = prefs.getBoolean("isAdminLoggedIn", false);
        
        Intent intent;
        if (isAdminLoggedIn || "Admin".equalsIgnoreCase(userType)) {
            intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
        } else if ("Student".equalsIgnoreCase(userType) || "Staff".equalsIgnoreCase(userType)) {
            intent = new Intent(SplashActivity.this, CampusDashboardActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, DashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
