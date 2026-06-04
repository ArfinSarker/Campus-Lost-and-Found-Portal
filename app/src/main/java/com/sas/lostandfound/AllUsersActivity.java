package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Activity for Admins to manage all registered users.
 * Displays a list of users with stats, search, and filtering capabilities.
 */
public class AllUsersActivity extends AppCompatActivity {

    private RecyclerView rvAllUsers;
    private UserAdapter adapter;
    private List<User> fullUserList = new ArrayList<>();
    private List<User> filteredUserList = new ArrayList<>();
    private ProgressBar progressBar;
    private LinearLayout llEmptyState;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;
    
    private TextView tvStatTotalUsers, tvStatStudents, tvStatStaffs, tvStatAdmins;
    
    private boolean isFetching = false;
    private String currentRoleFilter = "All";
    private String currentSearchQuery = "";
    private boolean isEnterAnimationCompleted = false;

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
        setupSearchAndFilter();

        // Load cached users for instant rendering
        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        String cachedUsersJson = prefs.getString("cachedAllUsersJson", "");
        if (!cachedUsersJson.isEmpty()) {
            try {
                List<User> cachedUsers = new com.google.gson.Gson().fromJson(cachedUsersJson, new TypeToken<List<User>>(){}.getType());
                if (cachedUsers != null && !cachedUsers.isEmpty()) {
                    fullUserList.clear();
                    fullUserList.addAll(cachedUsers);
                    updateStats();
                    applyFilter();
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isEnterAnimationCompleted) {
            fetchAllUsers();
        }
    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        isEnterAnimationCompleted = true;
        fetchAllUsers();
    }

    private void initializeViews() {
        rvAllUsers = findViewById(R.id.rvAllUsers);
        progressBar = findViewById(R.id.progressBar);
        llEmptyState = findViewById(R.id.llEmptyState);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        etSearch = findViewById(R.id.etSearch);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        
        tvStatTotalUsers = findViewById(R.id.tvStatTotalUsers);
        tvStatStudents = findViewById(R.id.tvStatStudents);
        tvStatStaffs = findViewById(R.id.tvStatStaffs);
        tvStatAdmins = findViewById(R.id.tvStatAdmins);
    }

    private void setupToolbar() {
        // Handle custom back button in the ConstraintLayout header
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout);
        }
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter(filteredUserList);
        rvAllUsers.setLayoutManager(new LinearLayoutManager(this));
        rvAllUsers.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(this::fetchAllUsers);
        }
    }

    private void setupSearchAndFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase(Locale.getDefault()).trim();
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                currentRoleFilter = "All";
            } else {
                int id = checkedIds.get(0);
                if (id == R.id.chipStudents) currentRoleFilter = "Student";
                else if (id == R.id.chipStaffs) currentRoleFilter = "Staff";
                else if (id == R.id.chipAdmins) currentRoleFilter = "Admin";
                else currentRoleFilter = "All";
            }
            applyFilter();
        });

        // Statistics Card Click Listeners for quick filtering
        findViewById(R.id.cardTotalUsers).setOnClickListener(v -> chipGroupFilter.check(R.id.chipAll));
        findViewById(R.id.cardStudents).setOnClickListener(v -> chipGroupFilter.check(R.id.chipStudents));
        findViewById(R.id.cardStaffs).setOnClickListener(v -> chipGroupFilter.check(R.id.chipStaffs));
        findViewById(R.id.cardAdmins).setOnClickListener(v -> chipGroupFilter.check(R.id.chipAdmins));
    }

    private void fetchAllUsers() {
        if (isFetching) return;
        isFetching = true;

        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        // Optimized: Fetch only essential columns for the management list
        String columns = "university_id,full_name,user_type,profile_image_url";
        SupabaseDatabaseHelper.select("profiles", "select=" + columns, new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                fullUserList.clear();
                if (users != null) {
                    fullUserList.addAll(users);
                    // Save to cache
                    try {
                        String json = new com.google.gson.Gson().toJson(users);
                        getSharedPreferences("MyApp", MODE_PRIVATE).edit().putString("cachedAllUsersJson", json).apply();
                    } catch (Exception e) {
                        // ignore
                    }
                }
                
                updateStats();
                applyFilter();
                
                progressBar.setVisibility(View.GONE);
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

    private void updateStats() {
        int total = fullUserList.size();
        int students = 0;
        int staffs = 0;
        int admins = 0;
        
        for (User user : fullUserList) {
            String role = user.getUserType();
            if ("Student".equalsIgnoreCase(role)) students++;
            else if ("Staff".equalsIgnoreCase(role)) staffs++;
            else if ("Admin".equalsIgnoreCase(role)) admins++;
        }
        
        tvStatTotalUsers.setText(String.valueOf(total));
        tvStatStudents.setText(String.valueOf(students));
        tvStatStaffs.setText(String.valueOf(staffs));
        tvStatAdmins.setText(String.valueOf(admins));
    }

    private void applyFilter() {
        List<User> temp = new ArrayList<>();
        for (User user : fullUserList) {
            boolean matchesRole = "All".equalsIgnoreCase(currentRoleFilter) || 
                                 user.getUserType().equalsIgnoreCase(currentRoleFilter);
            
            if (!matchesRole) continue;
            
            boolean matchesSearch = currentSearchQuery.isEmpty() || 
                                   (user.getFullName() != null && user.getFullName().toLowerCase().contains(currentSearchQuery)) ||
                                   (user.getName() != null && user.getName().toLowerCase().contains(currentSearchQuery)) ||
                                   (user.getUniversityId() != null && user.getUniversityId().toLowerCase().contains(currentSearchQuery));
            
            if (matchesSearch) {
                temp.add(user);
            }
        }
        
        adapter.updateUsers(temp);
        llEmptyState.setVisibility(filteredUserList.isEmpty() ? View.VISIBLE : View.GONE);
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
                intent.putExtra("intentFullName", user.getFullName() != null ? user.getFullName() : user.getName());
                intent.putExtra("intentUserType", user.getUserType());
                intent.putExtra("intentProfileImageUrl", user.getProfileImageUrl());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        public void updateUsers(List<User> newUsers) {
            androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return users.size();
                }

                @Override
                public int getNewListSize() {
                    return newUsers.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    User oldItem = users.get(oldItemPosition);
                    User newItem = newUsers.get(newItemPosition);
                    return oldItem.getUniversityId() != null && newItem.getUniversityId() != null && oldItem.getUniversityId().equals(newItem.getUniversityId());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    User oldItem = users.get(oldItemPosition);
                    User newItem = newUsers.get(newItemPosition);
                    return java.util.Objects.equals(oldItem.getFullName(), newItem.getFullName()) &&
                           java.util.Objects.equals(oldItem.getName(), newItem.getName()) &&
                           java.util.Objects.equals(oldItem.getProfileImageUrl(), newItem.getProfileImageUrl()) &&
                           java.util.Objects.equals(oldItem.getUserType(), newItem.getUserType());
                }
            });

            this.users.clear();
            this.users.addAll(newUsers);
            diffResult.dispatchUpdatesTo(this);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvUnivId;
            ImageView ivProfile;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvUserName);
                tvUnivId = itemView.findViewById(R.id.tvUserUniversityId);
                ivProfile = itemView.findViewById(R.id.ivUserProfile);
            }
        }
    }
}
