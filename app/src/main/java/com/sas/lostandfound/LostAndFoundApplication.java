package com.sas.lostandfound;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

public class LostAndFoundApplication extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context instance;

    public static Context getContext() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = getApplicationContext();

        try {
            // Register SnackbarManager to track activity lifecycle
            registerActivityLifecycleCallbacks(SnackbarManager.getInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
                @Override public void onActivityCreated(android.app.Activity a, android.os.Bundle b) {}
                @Override public void onActivityStarted(android.app.Activity a) {}
                @Override
                public void onActivityResumed(android.app.Activity a) {
                    SupabaseDatabaseHelper.updateUserActivityStatus();
                }
                @Override public void onActivityPaused(android.app.Activity a) {}
                @Override public void onActivityStopped(android.app.Activity a) {}
                @Override public void onActivitySaveInstanceState(android.app.Activity a, android.os.Bundle b) {}
                @Override public void onActivityDestroyed(android.app.Activity a) {}
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Initialize Database Helper
            SupabaseDatabaseHelper.init(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            // Register notification channel and schedule periodic polling
            createNotificationChannel();
            scheduleNotificationWorker(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Programmatic Firebase Initialization
        if (SupabaseConfig.FIREBASE_API_KEY != null && !SupabaseConfig.FIREBASE_API_KEY.isEmpty() &&
            SupabaseConfig.FIREBASE_APP_ID != null && !SupabaseConfig.FIREBASE_APP_ID.isEmpty() &&
            SupabaseConfig.FIREBASE_SENDER_ID != null && !SupabaseConfig.FIREBASE_SENDER_ID.isEmpty() &&
            SupabaseConfig.FIREBASE_PROJECT_ID != null && !SupabaseConfig.FIREBASE_PROJECT_ID.isEmpty()) {
            try {
                com.google.firebase.FirebaseOptions options = new com.google.firebase.FirebaseOptions.Builder()
                        .setApiKey(SupabaseConfig.FIREBASE_API_KEY)
                        .setApplicationId(SupabaseConfig.FIREBASE_APP_ID)
                        .setGcmSenderId(SupabaseConfig.FIREBASE_SENDER_ID)
                        .setProjectId(SupabaseConfig.FIREBASE_PROJECT_ID)
                        .build();

                com.google.firebase.FirebaseApp.initializeApp(this, options);
                fetchAndUploadFcmToken();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void fetchAndUploadFcmToken() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        android.util.Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    android.util.Log.d("FCM", "FCM Registration Token: " + token);

                    // Save token locally in MyApp prefs
                    getSharedPreferences("MyApp", Context.MODE_PRIVATE)
                            .edit()
                            .putString("fcm_token", token)
                            .apply();

                    // Upload token to Supabase (if user is logged in)
                    uploadFcmToken(token);
                });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void uploadFcmToken(String token) {
        if (token == null || token.isEmpty()) return;
        Context context = getContext();
        if (context == null) return;

        SharedPreferences prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE);
        String universityId = prefs.getString("universityId", "");
        if (universityId.isEmpty()) {
            android.util.Log.d("FCM", "No logged-in user universityId found, skipping upload.");
            return;
        }

        java.util.HashMap<String, Object> data = new java.util.HashMap<>();
        data.put("fcm_token", token);

        String query = "university_id=eq." + android.net.Uri.encode(universityId);

        SupabaseDatabaseHelper.update("profiles", query, data, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                android.util.Log.d("FCM", "Successfully uploaded FCM token to database: " + result);
            }

            @Override
            public void onFailure(String errorMessage) {
                android.util.Log.e("FCM", "Failed to upload FCM token: " + errorMessage);
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Campus Lost & Found";
            String description = "Notifications for matches, return updates and admin reviews";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("campus_lost_found_notifications", name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void scheduleNotificationWorker(Context context) {
        try {
            androidx.work.WorkManager.getInstance(context).cancelUniqueWork("CampusNotificationWorker");
            android.util.Log.d("Application", "Canceled legacy NotificationWorker to prevent duplicates.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
