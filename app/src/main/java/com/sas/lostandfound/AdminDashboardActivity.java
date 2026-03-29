package com.sas.lostandfound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";
    private TextView tvTotalLost, tvTotalFound, tvTotalUsers, tvTotalAdminRequests, tvTotalAdminReports, tvAdminTitle;
    private MaterialCardView cardLostItems, cardFoundItems, cardTotalUsers, cardAdminRequests, cardAdminReports;
    private MaterialButton btnManageItems, btnLogout, btnAdminRequests, btnManageUsers, btnAdminReports;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initializeViews();
        setupClickListeners();
        loadStats();
        setupAdminDashboard();
    }

    private void initializeViews() {
        tvTotalLost = findViewById(R.id.tvTotalLost);
        tvTotalFound = findViewById(R.id.tvTotalFound);
        tvTotalUsers = findViewById(R.id.tvTotalUsers);
        tvTotalAdminRequests = findViewById(R.id.tvTotalAdminRequests);
        tvTotalAdminReports = findViewById(R.id.tvTotalAdminReports);
        tvAdminTitle = findViewById(R.id.tvAdminTitle);
        
        cardLostItems = findViewById(R.id.cardLostItems);
        cardFoundItems = findViewById(R.id.cardFoundItems);
        cardTotalUsers = findViewById(R.id.cardTotalUsers);
        cardAdminRequests = findViewById(R.id.cardAdminRequests);
        cardAdminReports = findViewById(R.id.cardAdminReports);
        
        btnAdminRequests = findViewById(R.id.btnAdminRequests);
        btnAdminReports = findViewById(R.id.btnAdminReports);
        btnManageItems = findViewById(R.id.btnManageItems);
        btnManageUsers = findViewById(R.id.btnManageUsers);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupAdminDashboard() {
        if (tvAdminTitle != null) tvAdminTitle.setText("Admin Dashboard");
    }

    private void setupClickListeners() {
        cardLostItems.setOnClickListener(v -> {
            Intent intent = new Intent(this, AllReportedItemsActivity.class);
            intent.putExtra("isAdmin", true);
            intent.putExtra("filterStatus", "lost");
            startActivity(intent);
        });

        cardFoundItems.setOnClickListener(v -> {
            Intent intent = new Intent(this, AllReportedItemsActivity.class);
            intent.putExtra("isAdmin", true);
            intent.putExtra("filterStatus", "found");
            startActivity(intent);
        });

        cardTotalUsers.setOnClickListener(v -> {
            startActivity(new Intent(this, AllUsersActivity.class));
        });

        cardAdminRequests.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminRequestsActivity.class));
        });

        cardAdminReports.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminReportManagementActivity.class));
        });

        btnAdminRequests.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminRequestsActivity.class));
        });

        btnAdminReports.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminReportManagementActivity.class));
        });

        btnManageItems.setOnClickListener(v -> {
            Intent intent = new Intent(this, AllReportedItemsActivity.class);
            intent.putExtra("isAdmin", true);
            startActivity(intent);
        });

        btnManageUsers.setOnClickListener(v -> {
            startActivity(new Intent(this, AllUsersActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            getSharedPreferences("MyApp", MODE_PRIVATE).edit().clear().apply();
            Intent intent = new Intent(this, UserLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadStats() {
        mDatabase.child("LostItems").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvTotalLost.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching LostItems: " + error.getMessage());
            }
        });

        mDatabase.child("FoundItems").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvTotalFound.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching FoundItems: " + error.getMessage());
            }
        });

        mDatabase.child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvTotalUsers.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching Users: " + error.getMessage());
            }
        });

        mDatabase.child("adminRequests").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvTotalAdminRequests.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching adminRequests: " + error.getMessage());
            }
        });

        mDatabase.child("AdminReports").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                tvTotalAdminReports.setText(String.valueOf(snapshot.getChildrenCount()));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching AdminReports count: " + error.getMessage());
            }
        });
    }
}
