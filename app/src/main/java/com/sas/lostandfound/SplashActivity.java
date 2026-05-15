package com.sas.lostandfound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.reflect.TypeToken;

import java.util.List;

/**
 * Initial splash screen displayed when the app launches.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 2000;
    private static final String TAG = "SplashActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkSessionAndRedirect();
        }, SPLASH_DELAY);
    }

    private void checkSessionAndRedirect() {
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        String universityId = prefs.getString("universityId", "");
        String accessToken = prefs.getString("accessToken", "");
        String refreshToken = prefs.getString("refreshToken", "");

        if (!universityId.isEmpty()) {
            // First, update the helper with whatever token we have
            if (!accessToken.isEmpty()) {
                SupabaseDatabaseHelper.setAuthToken(accessToken);
            }

            // Attempt to fetch profile. This serves as a session check.
            SupabaseDatabaseHelper.select("profiles", "university_id=eq." + universityId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
                @Override
                public void onSuccess(List<User> users) {
                    if (users != null && !users.isEmpty()) {
                        proceedToDashboard(users.get(0));
                    } else {
                        // Profile not found but we have universityId? Something is wrong.
                        Log.e(TAG, "Logged in but profile not found.");
                        startActivity(new Intent(SplashActivity.this, DashboardActivity.class));
                        finish();
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.d(TAG, "Initial profile fetch failed: " + errorMessage);
                    
                    // If it's a 401 Unauthorized, we should try refreshing the token
                    if (errorMessage.contains("401") && !refreshToken.isEmpty()) {
                        refreshAndRetry(refreshToken, universityId);
                    } else if (errorMessage.contains("Network error")) {
                        // Network error: use fallback logic to avoid locking user out while offline
                        Log.w(TAG, "Network error during splash, using cached session data.");
                        fallbackRedirect(prefs);
                    } else {
                        // Other errors: try refresh just in case
                        if (!refreshToken.isEmpty()) {
                            refreshAndRetry(refreshToken, universityId);
                        } else {
                            fallbackRedirect(prefs);
                        }
                    }
                }
            });
        } else {
            // Not logged in
            startActivity(new Intent(SplashActivity.this, DashboardActivity.class));
            finish();
        }
    }

    private void refreshAndRetry(String refreshToken, String universityId) {
        Log.d(TAG, "Attempting to refresh session...");
        SupabaseAuthHelper.refreshSession(refreshToken, new SupabaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String userId, String accessToken, String newRefreshToken) {
                Log.d(TAG, "Session refreshed successfully.");
                
                // Save new tokens
                SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
                prefs.edit()
                        .putString("accessToken", accessToken)
                        .putString("refreshToken", newRefreshToken)
                        .apply();
                
                SupabaseDatabaseHelper.setAuthToken(accessToken);
                
                // Retry profile fetch
                SupabaseDatabaseHelper.select("profiles", "university_id=eq." + universityId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
                    @Override
                    public void onSuccess(List<User> users) {
                        if (users != null && !users.isEmpty()) {
                            proceedToDashboard(users.get(0));
                        } else {
                            startActivity(new Intent(SplashActivity.this, DashboardActivity.class));
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e(TAG, "Profile fetch failed after refresh: " + errorMessage);
                        fallbackRedirect(getSharedPreferences("MyApp", MODE_PRIVATE));
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Session refresh failed: " + errorMessage);
                // Refresh failed: session might be totally revoked or expired beyond refresh
                // Redirect to login or dashboard
                startActivity(new Intent(SplashActivity.this, UserLoginActivity.class));
                finish();
            }
        });
    }

    private void proceedToDashboard(User user) {
        Intent intent;
        if (user != null) {
            intent = new Intent(SplashActivity.this, CampusDashboardActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, DashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void fallbackRedirect(SharedPreferences prefs) {
        String userType = prefs.getString("userType", "");
        
        Intent intent;
        if (!userType.isEmpty()) {
            intent = new Intent(SplashActivity.this, CampusDashboardActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, DashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }
}
