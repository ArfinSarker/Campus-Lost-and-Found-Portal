package com.sas.lostandfound;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.gson.reflect.TypeToken;

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
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isFetching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure only admins can review admin requests
        RoleVerifier.checkAdminAccess(this);

        setContentView(R.layout.activity_admin_requests);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        fetchAdminRequests();
    }

    private void initializeViews() {
        rvAdminRequests = findViewById(R.id.rvAdminRequests);
        progressBar = findViewById(R.id.progressBar);
        llEmptyState = findViewById(R.id.llEmptyState);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout, toolbar);
        }
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

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(this::fetchAdminRequests);
        }
    }

    private void fetchAdminRequests() {
        if (isFetching) return;
        isFetching = true;

        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        SupabaseDatabaseHelper.select("admin_requests", "select=*", new TypeToken<List<AdminRequest>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<AdminRequest>>() {
            @Override
            public void onSuccess(List<AdminRequest> requests) {
                List<AdminRequest> temp = new ArrayList<>();
                if (requests != null) {
                    temp.addAll(requests);
                }
                adapter.updateRequests(temp);
                progressBar.setVisibility(View.GONE);
                llEmptyState.setVisibility(requestList.isEmpty() ? View.VISIBLE : View.GONE);
                
                isFetching = false;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(String errorMessage) {
                isFetching = false;
                progressBar.setVisibility(View.GONE);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Database Error: " + errorMessage);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void approveAdmin(AdminRequest request) {
        progressBar.setVisibility(View.VISIBLE);
        final String universityId = request.getUniversityId();

        // Step 1: Check if University ID already exists
        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + universityId, new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users != null && !users.isEmpty()) {
                    progressBar.setVisibility(View.GONE);
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "An account with this University ID already exists.");
                } else {
                    saveAdminToUsers(request);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Database Error: " + errorMessage);
            }
        });
    }

    private void saveAdminToUsers(AdminRequest request) {
        final String universityId = request.getUniversityId();
        final String authId = request.getAuthId();

        Log.d("AdminRequests", "Approving admin: " + universityId + ", Password exists: " + (request.getPassword() != null));

        User adminUser = new User(
                universityId,
                authId,
                request.getFullName(),
                request.getEmail(),
                request.getPassword(),
                request.getPhoneNumber(),
                request.getDesignation() != null ? request.getDesignation() : "Administration",
                request.getDepartment() != null ? request.getDepartment() : "Administration",
                request.getProfileImageUrl(),
                "Not Specified",
                "Admin"
        );
        adminUser.setAdmin(true);
        adminUser.setRole("admin");
        adminUser.setRequestStatus("approved");

        if (request.getCreated_at() != null) {
            adminUser.setCreated_at(request.getCreated_at());
        }

        SupabaseDatabaseHelper.insert("profiles", adminUser, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                Log.d("AdminRequests", "Profile created for: " + universityId + ". Now deleting request.");
                // Move data from admin_requests -> profiles (done by insert)
                SupabaseDatabaseHelper.delete("admin_requests", "university_id=eq." + universityId, new SupabaseDatabaseHelper.DatabaseCallback<Void>() {
                    @Override
                    public void onSuccess(Void v) {
                        progressBar.setVisibility(View.GONE);
                        Log.d("AdminRequests", "Admin request deleted: " + universityId);
                        SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Admin approved successfully.");
                        fetchAdminRequests();
                    }
                    @Override public void onFailure(String e) {
                        progressBar.setVisibility(View.GONE);
                        Log.e("AdminRequests", "Cleanup failed for: " + universityId + ", error: " + e);
                        SnackbarManager.show(SnackbarManager.Type.WARNING, "Admin profile created, but request cleanup failed: " + e);
                        fetchAdminRequests();
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Log.e("AdminRequests", "Failed to create profile for: " + universityId + ", error: " + errorMessage);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to approve admin account: " + errorMessage);
            }
        });
    }

    private void denyAdmin(AdminRequest request) {
        progressBar.setVisibility(View.VISIBLE);
        SupabaseDatabaseHelper.delete("admin_requests", "university_id=eq." + request.getUniversityId(), new SupabaseDatabaseHelper.DatabaseCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                progressBar.setVisibility(View.GONE);
                SnackbarManager.show(SnackbarManager.Type.GENERAL, "Request denied.");
                fetchAdminRequests();
            }

            @Override
            public void onFailure(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Deny failed: " + errorMessage);
            }
        });
    }
}
