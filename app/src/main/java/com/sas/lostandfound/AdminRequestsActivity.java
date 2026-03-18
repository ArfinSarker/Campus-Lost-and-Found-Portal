package com.sas.lostandfound;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity for managing and approving admin requests.
 * Fixed to use University ID as the unique identifier and avoid email duplication issues.
 */
public class AdminRequestsActivity extends AppCompatActivity {

    private RecyclerView rvAdminRequests;
    private AdminRequestAdapter adapter;
    private List<AdminRequest> requestList;
    private ProgressBar progressBar;
    private LinearLayout llEmptyState;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_requests);

        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        fetchAdminRequests();
    }

    private void initializeViews() {
        rvAdminRequests = findViewById(R.id.rvAdminRequests);
        progressBar = findViewById(R.id.progressBar);
        llEmptyState = findViewById(R.id.llEmptyState);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        requestList = new ArrayList<>();
        adapter = new AdminRequestAdapter(requestList, new AdminRequestAdapter.OnRequestListener() {
            @Override
            public void onAccept(AdminRequest request) {
                approveAdmin(request);
            }

            @Override
            public void onDeny(AdminRequest request) {
                denyAdmin(request);
            }
        });
        rvAdminRequests.setLayoutManager(new LinearLayoutManager(this));
        rvAdminRequests.setAdapter(adapter);
    }

    private void fetchAdminRequests() {
        progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("adminRequests").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requestList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    AdminRequest request = data.getValue(AdminRequest.class);
                    if (request != null) {
                        requestList.add(request);
                    }
                }
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                llEmptyState.setVisibility(requestList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminRequestsActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void approveAdmin(AdminRequest request) {
        progressBar.setVisibility(View.VISIBLE);
        final String universityId = request.getUniversityId();

        // Step 1: Check if University ID already exists in the database
        mDatabase.child("Users").child(universityId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AdminRequestsActivity.this, "An account with this University ID already exists.", Toast.LENGTH_LONG).show();
                } else {
                    // Step 2: Proceed with creating the admin account in database
                    // Firebase Auth creation is handled during first login to avoid email duplication issues here.
                    saveAdminToUsers(request);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AdminRequestsActivity.this, "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveAdminToUsers(AdminRequest request) {
        final String universityId = request.getUniversityId();
        
        // Use a placeholder authId; it will be updated with the actual Firebase UID during first login.
        String placeholderAuthId = "PENDING_" + universityId;

        User adminUser = new User(
                universityId,
                placeholderAuthId,
                request.getFullName(),
                request.getEmail(),
                request.getPassword(),
                request.getPhoneNumber(),
                request.getDesignation() != null ? request.getDesignation() : "Administration",
                request.getProfileImageUrl(),
                "Not Specified",
                "Admin"
        );
        adminUser.setAdmin(true);
        adminUser.setRole("admin");
        adminUser.setRequestStatus("approved"); // Step 3: Set status as approved

        if (request.getCreated_at() != null) {
            adminUser.setCreated_at(request.getCreated_at());
        }

        mDatabase.child("Users").child(universityId).setValue(adminUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Move data from adminRequests -> Users (done by setValue)
                        mDatabase.child("adminRequests").child(universityId).removeValue();
                        mDatabase.child("DeniedAdminRequests").child(universityId).removeValue();
                        Toast.makeText(AdminRequestsActivity.this, "Admin approved successfully.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AdminRequestsActivity.this, "Failed to approve admin account.", Toast.LENGTH_SHORT).show();
                    }
                    progressBar.setVisibility(View.GONE);
                });
    }

    private void denyAdmin(AdminRequest request) {
        mDatabase.child("DeniedAdminRequests").child(request.getUniversityId()).setValue(true)
                .addOnSuccessListener(aVoid -> {
                    mDatabase.child("adminRequests").child(request.getUniversityId()).removeValue();
                    Toast.makeText(this, "Request denied.", Toast.LENGTH_SHORT).show();
                });
    }
}
