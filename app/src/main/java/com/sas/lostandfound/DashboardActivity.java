package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    private MaterialButton btnReportLost, btnReportFound, btnSignIn;
    private TextView tvBrowseAll, tvDeveloperInfo;
    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private List<Item> itemList;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();

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

        // Sample data
        itemList = new ArrayList<>();
        itemList.add(new Item("1", getString(R.string.sample_macbook),
                getString(R.string.sample_macbook_location),
                getString(R.string.sample_macbook_time), "lost", "📂"));
        itemList.add(new Item("2", getString(R.string.sample_found),
                getString(R.string.sample_found_location),
                getString(R.string.sample_found_time), "found", "📂"));
        itemList.add(new Item("3", getString(R.string.sample_airpods),
                getString(R.string.sample_airpods_location),
                getString(R.string.sample_airpods_time), "lost", "📂"));

        adapter = new ItemAdapter(itemList, item -> {
            startActivity(new Intent(DashboardActivity.this, UserLoginActivity.class));
        });
        recyclerView.setAdapter(adapter);

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

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, CampusDashboardActivity.class));
            finish();
        }
    }
}
