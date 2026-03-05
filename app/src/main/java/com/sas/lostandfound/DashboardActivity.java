package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private MaterialButton btnReportLost, btnReportFound, btnSignIn;
    private TextView tvBrowseAll, tvDeveloperInfo;
    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private List<Item> itemList;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        // Check if already logged in - if so, go to CampusDashboard
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, CampusDashboardActivity.class));
            finish();
            return;
        }

        // Initialize views
        btnReportLost = findViewById(R.id.btnReportLost);
        btnReportFound = findViewById(R.id.btnReportFound);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvBrowseAll = findViewById(R.id.tvBrowseAll);
        tvDeveloperInfo = findViewById(R.id.tvDeveloperInfo);
        recyclerView = findViewById(R.id.recyclerViewRecent);

        itemList = new ArrayList<>();
        adapter = new ItemAdapter(itemList, item -> {
            // Redirect to Login Screen when an item is clicked
            startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
        });
        
        // Use LinearLayoutManager for a vertical scrollable list
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.setNestedScrollingEnabled(false);

        loadRecentItems();

        // Set click listeners
        btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
        });

        btnReportLost.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
        });

        btnReportFound.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
        });

        tvBrowseAll.setOnClickListener(v -> {
            // Redirect to Login Screen to see all items in detail
            startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
        });

        if (tvDeveloperInfo != null) {
            tvDeveloperInfo.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, DeveloperInfoActivity.class));
            });
        }
    }

    private void loadRecentItems() {
        ValueEventListener itemListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        Item item = data.getValue(Item.class);
                        if (item != null) {
                            updateOrAddItem(item);
                        }
                    }
                    // Sort items by timestamp (most recent first)
                    itemList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Log error if needed
            }
        };

        // Listen to both LostItems and FoundItems specifically
        mDatabase.child("LostItems").addValueEventListener(itemListener);
        mDatabase.child("FoundItems").addValueEventListener(itemListener);
    }

    private synchronized void updateOrAddItem(Item item) {
        for (int i = 0; i < itemList.size(); i++) {
            if (itemList.get(i).getId().equals(item.getId())) {
                itemList.set(i, item);
                return;
            }
        }
        itemList.add(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, CampusDashboardActivity.class));
            finish();
        }
    }
}
