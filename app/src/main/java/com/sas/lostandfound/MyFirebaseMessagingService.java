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

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "MyFirebaseMsgService";
    private static final String CHANNEL_ID = "campus_lost_found_notifications";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);

        // Save new token to SharedPreferences
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        prefs.edit().putString("fcm_token", token).apply();

        // Upload token to Supabase
        LostAndFoundApplication.uploadFcmToken(token);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleNow(remoteMessage.getData());
        }
    }

    @SuppressLint("MissingPermission")
    private void handleNow(Map<String, String> data) {
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        String currentUnivId = prefs.getString("universityId", "");
        String recipientId = data.get("recipient_id");

        if (currentUnivId == null || currentUnivId.isEmpty()) {
            Log.d(TAG, "No user is currently logged in. Dropping notification.");
            return;
        }

        if (recipientId != null && !recipientId.isEmpty() && !recipientId.equals(currentUnivId)) {
            Log.d(TAG, "Notification recipient (" + recipientId + ") does not match current user (" + currentUnivId + "). Dropping notification.");
            return;
        }

        String id = data.get("notification_id");
        String type = data.get("notification_type");
        String itemId = data.get("item_id");
        String senderId = data.get("sender_id");
        String claimerId = data.get("claimer_id");
        String senderName = data.get("sender_name");
        String senderPhone = data.get("sender_phone");
        String senderEmail = data.get("sender_email");
        String itemName = data.get("item_name");
        String additionalDetails = data.get("additional_details");
        String message = data.get("message");

        if ("chat_message".equals(type) && additionalDetails != null && !additionalDetails.isEmpty()) {
            Map<String, Object> updateData = new java.util.HashMap<>();
            updateData.put("is_delivered", true);
            String query = "conversation_id=eq." + additionalDetails + "&sender_id=neq." + currentUnivId + "&is_delivered=eq.false";
            SupabaseDatabaseHelper.update("messages", query, updateData, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                @Override
                public void onSuccess(String result) {
                    Log.d(TAG, "Marked message as delivered successfully");
                }
                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "Failed to mark message as delivered: " + errorMessage);
                }
            });
        }

        if (message == null || message.isEmpty()) {
            message = "You have a new update.";
        }

        // Build target intent pointing to SplashActivity
        Intent intent = new Intent(this, SplashActivity.class);
        intent.putExtra("from_notification", true);
        intent.putExtra("notification_id", id);
        intent.putExtra("notification_type", type);
        intent.putExtra("item_id", itemId);
        intent.putExtra("sender_id", senderId);
        intent.putExtra("claimer_id", claimerId);
        intent.putExtra("sender_name", senderName);
        intent.putExtra("sender_phone", senderPhone);
        intent.putExtra("sender_email", senderEmail);
        intent.putExtra("item_name", itemName);
        intent.putExtra("additional_details", additionalDetails);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int requestCode = id != null ? id.hashCode() : (int) System.currentTimeMillis();

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                flags
        );

        // Derive notification title
        String title = getNotificationTitle(type);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_package) // Use the existing app notification icon
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Broadcast badge update to active activities
        UnreadBadgeHelper.sendBadgeUpdateBroadcast(this);

        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission is not granted. Cannot show push notification.");
                return;
            }
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(requestCode, builder.build());
    }

    private String getNotificationTitle(String type) {
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
            case "chat_request":
                return "New Chat Request";
            case "chat_accepted":
                return "Chat Request Accepted";
            case "chat_message":
                return "New Message";
            default:
                return "Campus Lost & Found Update";
        }
    }
}
