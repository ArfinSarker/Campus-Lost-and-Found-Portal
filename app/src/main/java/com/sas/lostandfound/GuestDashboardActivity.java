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

/**
 * Main dashboard for guest users.
 */
public class GuestDashboardActivity extends AppCompatActivity {

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

        btnReportLost = findViewById(R.id.btnReportLost);
        btnReportFound = findViewById(R.id.btnReportFound);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvBrowseAll = findViewById(R.id.tvBrowseAll);
        tvDeveloperInfo = findViewById(R.id.tvDeveloperInfo);
        recyclerView = findViewById(R.id.recyclerViewRecent);

        // Check if already logged in - if so, go to CampusDashboard
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, CampusDashboardActivity.class));
            finish();
            return;
        }

        adapter = new ItemAdapter(itemList, item -> {
            startActivity(new Intent(GuestDashboardActivity.this, UserLoginActivity.class));
        });
        recyclerView.setAdapter(adapter);

        btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(GuestDashboardActivity.this, UserLoginActivity.class));
        });

        btnReportLost.setOnClickListener(v -> {
            startActivity(new Intent(GuestDashboardActivity.this, UserLoginActivity.class));
        });

        btnReportFound.setOnClickListener(v -> {
            startActivity(new Intent(GuestDashboardActivity.this, UserLoginActivity.class));
        });

        tvBrowseAll.setOnClickListener(v -> {
            startActivity(new Intent(GuestDashboardActivity.this, UserLoginActivity.class));
        });

        tvDeveloperInfo.setOnClickListener(v -> {
            startActivity(new Intent(GuestDashboardActivity.this, DeveloperInfoActivity.class));
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
