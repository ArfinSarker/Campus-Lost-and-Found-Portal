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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_users);

        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        fetchAllUsers();
    }

    private void initializeViews() {
        rvAllUsers = findViewById(R.id.rvAllUsers);
        progressBar = findViewById(R.id.progressBar);
        llEmptyState = findViewById(R.id.llEmptyState);
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
    }

    private void setupRecyclerView() {
        userList = new ArrayList<>();
        adapter = new UserAdapter(userList);
        rvAllUsers.setLayoutManager(new LinearLayoutManager(this));
        rvAllUsers.setAdapter(adapter);
    }

    private void fetchAllUsers() {
        progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("Users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    User user = data.getValue(User.class);
                    if (user != null) {
                        userList.add(user);
                    }
                }
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                llEmptyState.setVisibility(userList.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AllUsersActivity.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
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
                Glide.with(holder.itemView.getContext())
                        .load(user.getProfileImageUrl())
                        .placeholder(R.drawable.ic_user)
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
