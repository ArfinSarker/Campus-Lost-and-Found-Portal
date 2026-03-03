package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
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
            startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
        });
        
        // Use GridLayoutManager as specified in XML
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

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
            startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
        });

        if (tvDeveloperInfo != null) {
            tvDeveloperInfo.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, DeveloperInfoActivity.class));
            });
        }
    }

    private void loadRecentItems() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                
                // Load limited number of items for "Recent" section
                List<Item> tempItems = new ArrayList<>();
                
                DataSnapshot lostSnapshot = snapshot.child("LostItems");
                for (DataSnapshot dataSnapshot : lostSnapshot.getChildren()) {
                    Item item = dataSnapshot.getValue(Item.class);
                    if (item != null) tempItems.add(item);
                }
                
                DataSnapshot foundSnapshot = snapshot.child("FoundItems");
                for (DataSnapshot dataSnapshot : foundSnapshot.getChildren()) {
                    Item item = dataSnapshot.getValue(Item.class);
                    if (item != null) tempItems.add(item);
                }
                
                // Sort by timestamp
                tempItems.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                
                // Add top 6 to display
                for (int i = 0; i < Math.min(tempItems.size(), 6); i++) {
                    itemList.add(tempItems.get(i));
                }
                
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
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
