package com.sas.lostandfound;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;

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
    private ValueAnimator shimmerAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final com.airbnb.lottie.LottieAnimationView loadingIndicator = findViewById(R.id.loadingIndicator);
        if (loadingIndicator != null) {
            // Enable hardware acceleration for seamless 60fps rendering
            loadingIndicator.setRenderMode(com.airbnb.lottie.RenderMode.HARDWARE);
            // Ensure infinite loop behavior
            loadingIndicator.setRepeatCount(com.airbnb.lottie.LottieDrawable.INFINITE);
            
            // Preload the animation composition asynchronously to eliminate lag between loops
            com.airbnb.lottie.LottieCompositionFactory.fromRawRes(this, R.raw.loading_animation)
                .addListener(new com.airbnb.lottie.LottieListener<com.airbnb.lottie.LottieComposition>() {
                    @Override
                    public void onResult(com.airbnb.lottie.LottieComposition composition) {
                        if (composition != null && !isDestroyed() && !isFinishing()) {
                            loadingIndicator.setComposition(composition);
                            loadingIndicator.playAnimation();
                        }
                    }
                });
        }

        // Start entrance animations & developer text shimmer
        startEntranceAnimations();
        startShimmerAnimation(findViewById(R.id.tvDevName));

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            checkSessionAndRedirect();
        }, SPLASH_DELAY);
    }

    private void startEntranceAnimations() {
        View logoContainer = findViewById(R.id.logoContainer);
        View loadingIndicator = findViewById(R.id.loadingIndicator);
        View tvAppName = findViewById(R.id.tvAppName);
        View developerContainer = findViewById(R.id.developerContainer);

        if (logoContainer == null || loadingIndicator == null || tvAppName == null || developerContainer == null) {
            return;
        }

        // Set initial invisible & scaled-down states for smooth entrance animation
        logoContainer.setAlpha(0f);
        logoContainer.setScaleX(0.7f);
        logoContainer.setScaleY(0.7f);

        loadingIndicator.setAlpha(0f);
        tvAppName.setAlpha(0f);
        developerContainer.setAlpha(0f);

        // Scale up and overshoot pop-in of circular logo container
        logoContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1000)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        // Staggered fade-in of other elements
        loadingIndicator.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(300)
                .start();

        tvAppName.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(450)
                .start();

        developerContainer.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(600)
                .start();
    }

    private void startShimmerAnimation(final TextView textView) {
        if (textView == null) return;
        textView.post(() -> {
            final float width = textView.getPaint().measureText(textView.getText().toString());
            if (width <= 0) return;
            final int baseColor = textView.getCurrentTextColor();
            final int shimmerColor = 0xFFFFFFFF; // Metallic white highlight

            shimmerAnimator = ValueAnimator.ofFloat(-width, 2f * width);
            shimmerAnimator.setDuration(1800);
            shimmerAnimator.setRepeatCount(ValueAnimator.INFINITE);
            shimmerAnimator.setRepeatMode(ValueAnimator.RESTART);
            shimmerAnimator.addUpdateListener(animation -> {
                float value = (Float) animation.getAnimatedValue();
                Shader shader = new LinearGradient(
                    value, 0, value + (width / 2f), 0,
                    new int[] { baseColor, shimmerColor, baseColor },
                    new float[] { 0.0f, 0.5f, 1.0f },
                    Shader.TileMode.CLAMP
                );
                textView.getPaint().setShader(shader);
                textView.invalidate();
            });
            shimmerAnimator.start();
        });
    }

    private void navigateTo(Intent intent) {
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        // Defer finishing the activity slightly so the Splash window continues rendering
        // its Lottie animation smoothly during the transition cross-fade.
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 150);
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
                        navigateTo(new Intent(SplashActivity.this, DashboardActivity.class));
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
            navigateTo(new Intent(SplashActivity.this, DashboardActivity.class));
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
                            navigateTo(new Intent(SplashActivity.this, DashboardActivity.class));
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
                // Redirect to login or dashboard
                navigateTo(new Intent(SplashActivity.this, UserLoginActivity.class));
            }
        });
    }

    private void proceedToDashboard(User user) {
        Intent intent;
        if (user != null) {
            SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
            boolean isAdmin = prefs.getBoolean("isAdminLoggedIn", false);
            String activeMode = prefs.getString("activeMode", "user");
            if (isAdmin && "admin".equals(activeMode)) {
                intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, CampusDashboardActivity.class);
            }
            copyNotificationExtras(getIntent(), intent);
        } else {
            intent = new Intent(SplashActivity.this, DashboardActivity.class);
        }
        navigateTo(intent);
    }

    private void fallbackRedirect(SharedPreferences prefs) {
        String userType = prefs.getString("userType", "");
        
        Intent intent;
        if (!userType.isEmpty()) {
            boolean isAdmin = prefs.getBoolean("isAdminLoggedIn", false);
            String activeMode = prefs.getString("activeMode", "user");
            if (isAdmin && "admin".equals(activeMode)) {
                intent = new Intent(SplashActivity.this, AdminDashboardActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, CampusDashboardActivity.class);
            }
            copyNotificationExtras(getIntent(), intent);
        } else {
            intent = new Intent(SplashActivity.this, DashboardActivity.class);
        }
        navigateTo(intent);
    }

    private void copyNotificationExtras(Intent source, Intent target) {
        if (source != null && (source.getBooleanExtra("from_notification", false) || "true".equals(source.getStringExtra("from_notification")))) {
            target.putExtra("from_notification", true);
            target.putExtra("notification_id", source.getStringExtra("notification_id"));
            target.putExtra("notification_type", source.getStringExtra("notification_type"));
            target.putExtra("item_id", source.getStringExtra("item_id"));
            target.putExtra("sender_id", source.getStringExtra("sender_id"));
            target.putExtra("claimer_id", source.getStringExtra("claimer_id"));
            target.putExtra("sender_name", source.getStringExtra("sender_name"));
            target.putExtra("sender_phone", source.getStringExtra("sender_phone"));
            target.putExtra("sender_email", source.getStringExtra("sender_email"));
            target.putExtra("item_name", source.getStringExtra("item_name"));
            target.putExtra("additional_details", source.getStringExtra("additional_details"));
        }
    }

    @Override
    protected void onDestroy() {
        if (shimmerAnimator != null) {
            shimmerAnimator.cancel();
        }
        super.onDestroy();
    }
}
