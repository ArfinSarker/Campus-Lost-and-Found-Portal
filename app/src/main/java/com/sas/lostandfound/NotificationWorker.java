package com.sas.lostandfound;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NotificationWorker extends Worker {
    private static final String TAG = "NotificationWorker";
    private static final String CHANNEL_ID = "campus_lost_found_notifications";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences("MyApp", Context.MODE_PRIVATE);
        String universityId = prefs.getString("universityId", "");

        if (universityId.isEmpty()) {
            Log.d(TAG, "No logged in user found. Skipping worker execution.");
            return Result.success();
        }

        OkHttpClient client = SupabaseDatabaseHelper.getClient();
        if (client == null) {
            Log.e(TAG, "OkHttpClient is not initialized.");
            return Result.failure();
        }

        String baseUrl = SupabaseConfig.SUPABASE_URL;
        if (baseUrl == null || baseUrl.isEmpty()) {
            Log.e(TAG, "Supabase URL is not configured.");
            return Result.failure();
        }
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        String filter = "recipient_id=eq." + universityId + "&is_read=eq.false";
        String url = baseUrl + "/rest/v1/notifications?" + filter;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("apikey", SupabaseConfig.SUPABASE_KEY != null ? SupabaseConfig.SUPABASE_KEY : "")
                .addHeader("Authorization", SupabaseDatabaseHelper.getAuthHeader())
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "HTTP error while fetching notifications: " + response.code());
                return Result.retry();
            }

            if (response.body() == null) {
                return Result.success();
            }

            String body = response.body().string();
            List<Notification> notifications = new Gson().fromJson(body, new TypeToken<List<Notification>>() {}.getType());

            if (notifications != null && !notifications.isEmpty()) {
                SharedPreferences notifyPrefs = context.getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE);
                Set<String> notifiedIds = notifyPrefs.getStringSet("notified_ids", new HashSet<>());
                Set<String> newNotifiedIds = new HashSet<>(notifiedIds);

                boolean newNotificationShown = false;
                int notificationCount = 0;

                for (Notification n : notifications) {
                    if (!newNotifiedIds.contains(n.getId())) {
                        showSystemNotification(context, n);
                        newNotifiedIds.add(n.getId());
                        newNotificationShown = true;
                        notificationCount++;
                    }
                }

                if (newNotificationShown) {
                    notifyPrefs.edit().putStringSet("notified_ids", newNotifiedIds).apply();
                    Log.d(TAG, "Displayed " + notificationCount + " new system notifications.");
                }
            }

            return Result.success();

        } catch (IOException e) {
            Log.e(TAG, "Network error during notification check: " + e.getMessage());
            return Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in NotificationWorker: " + e.getMessage());
            return Result.failure();
        }
    }

    @SuppressLint("MissingPermission")
    private void showSystemNotification(Context context, Notification notification) {
        // Build the deep link intent pointing to SplashActivity
        Intent intent = new Intent(context, SplashActivity.class);
        intent.putExtra("from_notification", true);
        intent.putExtra("notification_id", notification.getId());
        intent.putExtra("notification_type", notification.getType());
        intent.putExtra("item_id", notification.getItemId());
        intent.putExtra("sender_id", notification.getSenderId());
        intent.putExtra("claimer_id", notification.getClaimerId());
        intent.putExtra("sender_name", notification.getSenderName());
        intent.putExtra("sender_phone", notification.getSenderPhone());
        intent.putExtra("sender_email", notification.getSenderEmail());
        intent.putExtra("item_name", notification.getItemName());
        intent.putExtra("additional_details", notification.getAdditionalDetails());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Unique request code based on notification ID hash to avoid intent collision
        int requestCode = notification.getId().hashCode();
        
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                flags
        );

        // Check if Android 13 notification permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission is not granted. Cannot show notification.");
                return;
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_package)
                .setContentTitle(getNotificationTitle(notification))
                .setContentText(notification.getMessage())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(requestCode, builder.build());
    }

    private String getNotificationTitle(Notification notification) {
        String type = notification.getType();
        if (type == null) return "Lost & Found Update";
        
        switch (type) {
            case "lost_item":
                return "New Lost Item Report";
            case "found_item":
                return "New Found Item Report";
            case "lost_claim":
            case "found_claim":
            case "item_claimed":
                return "Item Claim Request";
            case "item_return":
                return "Item Returned";
            case "admin_report":
            case "admin_report_new":
                return "Admin Report Update";
            case "admin_request":
                return "Admin Request Update";
            default:
                return "Campus Lost & Found Update";
        }
    }
}
