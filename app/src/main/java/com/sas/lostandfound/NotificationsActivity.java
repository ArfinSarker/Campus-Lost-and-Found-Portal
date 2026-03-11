package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList;
    private LinearLayout llEmptyState;
    private ImageButton btnMarkAllRead;
    private DatabaseReference mDatabase;
    private String currentUserId;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        rvNotifications = findViewById(R.id.rvNotifications);
        llEmptyState = findViewById(R.id.llEmptyState);
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();
        currentUserId = FirebaseAuth.getInstance().getUid();

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList, notification -> {
            // Mark as read
            mDatabase.child("Notifications").child(currentUserId).child(notification.getId()).child("read").setValue(true);
            
            // Redirect to Claim Details
            Intent intent = new Intent(this, ClaimDetailsActivity.class);
            intent.putExtra("senderName", notification.getSenderName());
            intent.putExtra("senderPhone", notification.getSenderPhone());
            intent.putExtra("senderEmail", notification.getSenderEmail());
            intent.putExtra("itemName", notification.getItemName());
            intent.putExtra("additionalDetails", notification.getAdditionalDetails());
            intent.putExtra("type", notification.getType());
            startActivity(intent);
        });

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());

        fetchNotifications();
    }

    private void fetchNotifications() {
        if (currentUserId == null) return;

        mDatabase.child("Notifications").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                boolean hasUnread = false;
                for (DataSnapshot data : snapshot.getChildren()) {
                    Notification notification = data.getValue(Notification.class);
                    if (notification != null) {
                        notificationList.add(notification);
                        if (!notification.isRead()) {
                            hasUnread = true;
                        }
                    }
                }
                Collections.sort(notificationList, (n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                adapter.notifyDataSetChanged();
                
                llEmptyState.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                rvNotifications.setVisibility(notificationList.isEmpty() ? View.GONE : View.VISIBLE);
                btnMarkAllRead.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void markAllAsRead() {
        if (currentUserId == null || notificationList.isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();
        for (Notification notification : notificationList) {
            if (!notification.isRead()) {
                updates.put(notification.getId() + "/read", true);
            }
        }

        if (!updates.isEmpty()) {
            mDatabase.child("Notifications").child(currentUserId).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show());
        }
    }
}
