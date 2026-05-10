package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for Admins to manage all registered users.
 * Displays a list of users in a social-media style (similar to Facebook's friend list).
 */
public class AllUsersActivity extends AppCompatActivity {

    private RecyclerView rvAllUsers;
    private UserAdapter adapter;
    private List<User> userList;
    private ProgressBar progressBar;
    private LinearLayout llEmptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isFetching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure only admins can access user management
        RoleVerifier.checkAdminAccess(this);

        setContentView(R.layout.activity_all_users);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        fetchAllUsers();
    }

    private void initializeViews() {
        rvAllUsers = findViewById(R.id.rvAllUsers);
        progressBar = findViewById(R.id.progressBar);
        llEmptyState = findViewById(R.id.llEmptyState);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage All Users");
        }
        
        // Handle custom back button if present in layout
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout, toolbar);
        }
    }

    private void setupRecyclerView() {
        userList = new ArrayList<>();
        adapter = new UserAdapter(userList);
        rvAllUsers.setLayoutManager(new LinearLayoutManager(this));
        rvAllUsers.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(this::fetchAllUsers);
        }
    }

    private void fetchAllUsers() {
        if (isFetching) return;
        isFetching = true;

        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        SupabaseDatabaseHelper.select("profiles", "select=*", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                userList.clear();
                if (users != null) {
                    userList.addAll(users);
                }
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                llEmptyState.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
                
                isFetching = false;
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(String errorMessage) {
                isFetching = false;
                progressBar.setVisibility(View.GONE);
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Error: " + errorMessage);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    /**
     * Adapter for displaying users in the list.
     */
    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {
        private List<User> users;

        public UserAdapter(List<User> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = users.get(position);
            holder.tvName.setText(user.getFullName() != null ? user.getFullName() : user.getName());
            holder.tvType.setText(user.getUserType());
            holder.tvUnivId.setText(user.getUniversityId());

            if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                GlideApp.with(holder.itemView.getContext())
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.ic_user)
                        .thumbnail(0.1f)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .into(holder.ivProfile);
            } else {
                holder.ivProfile.setImageResource(R.drawable.ic_user);
            }

            holder.itemView.setOnClickListener(v -> {
                // Open user profile in admin viewing mode
                Intent intent = new Intent(AllUsersActivity.this, UserProfileActivity.class);
                intent.putExtra("targetUserId", user.getUniversityId());
                intent.putExtra("isAdminViewing", true);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvType, tvUnivId;
            ImageView ivProfile;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvUserName);
                tvType = itemView.findViewById(R.id.tvUserType);
                tvUnivId = itemView.findViewById(R.id.tvUserUniversityId);
                ivProfile = itemView.findViewById(R.id.ivUserProfile);
            }
        }
    }
}
