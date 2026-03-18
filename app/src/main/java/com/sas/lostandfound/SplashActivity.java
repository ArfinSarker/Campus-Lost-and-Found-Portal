package com.sas.lostandfound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Initial splash screen displayed when the app launches.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000;

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
            String userType = prefs.getString("userType", "");
            boolean isAdminLoggedIn = prefs.getBoolean("isAdminLoggedIn", false);
            
            Intent intent;
            if (isAdminLoggedIn || "Admin".equalsIgnoreCase(userType)) {
                intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
            } else if ("Student".equalsIgnoreCase(userType) || "Staff".equalsIgnoreCase(userType)) {
                intent = new Intent(SplashActivity.this, CampusDashboardActivity.class);
            } else {
                // Fallback to main dashboard if type is unknown but auth exists
                intent = new Intent(SplashActivity.this, DashboardActivity.class);
            }
            startActivity(intent);
        } else {
            // Not logged in
            startActivity(new Intent(SplashActivity.this, DashboardActivity.class));
        }
        finish();
    }
}
