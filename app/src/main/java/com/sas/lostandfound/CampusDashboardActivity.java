package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CampusDashboardActivity extends AppCompatActivity {

    private RecyclerView rvRecentItems;
    private RecentItemsAdapter adapter;
    private List<Item> itemList;
    
    private TextView tvWelcome, tvActiveItems, tvDeveloperInfo;
    private MaterialButton btnReportLost, btnReportFound;
    private MaterialCardView cvProfile;
    private ImageView ivUserProfile;
    private ImageButton btnLogout;
    private TabLayout tabLayout;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_dashboard);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initializeViews();
        setupRecyclerView();
        setupTabLayout();
        fetchUserData();
        fetchRecentItems();

        cvProfile.setOnClickListener(v -> startActivity(new Intent(this, UserProfileActivity.class)));
        
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, UserLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        btnReportLost.setOnClickListener(v -> startActivity(new Intent(this, CampusReportLostActivity.class)));
        btnReportFound.setOnClickListener(v -> startActivity(new Intent(this, CampusReportFoundActivity.class)));

        if (tvDeveloperInfo != null) {
            tvDeveloperInfo.setOnClickListener(v -> {
                Intent intent = new Intent(CampusDashboardActivity.this, DeveloperInfoActivity.class);
                startActivity(intent);
            });
        }
    }

    private void initializeViews() {
        rvRecentItems = findViewById(R.id.rvRecentItems);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvActiveItems = findViewById(R.id.tvActiveItems);
        tvDeveloperInfo = findViewById(R.id.tvDeveloperInfo);
        btnReportLost = findViewById(R.id.btnReportLost);
        btnReportFound = findViewById(R.id.btnReportFound);
        cvProfile = findViewById(R.id.cvProfile);
        ivUserProfile = findViewById(R.id.ivUserProfile);
        btnLogout = findViewById(R.id.btnLogout);
        tabLayout = findViewById(R.id.tabLayout);
    }

    private void setupTabLayout() {
        if (tabLayout != null) {
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    String tabText = tab.getText() != null ? tab.getText().toString() : "";
                    switch (tabText) {
                        case "Home":
                            // Already on Home
                            break;
                        case "Browse Items":
                            startActivity(new Intent(CampusDashboardActivity.this, BrowseItemsActivity.class));
                            break;
                        case "Report Lost":
                            startActivity(new Intent(CampusDashboardActivity.this, CampusReportLostActivity.class));
                            break;
                        case "Report Found":
                            startActivity(new Intent(CampusDashboardActivity.this, CampusReportFoundActivity.class));
                            break;
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                    onTabSelected(tab);
                }
            });
        }
    }

    private void setupRecyclerView() {
        itemList = new ArrayList<>();
        adapter = new RecentItemsAdapter(itemList);
        rvRecentItems.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvRecentItems.setAdapter(adapter);
    }

    private void fetchUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            mDatabase.child("Users").child(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        tvWelcome.setText("Welcome back, " + name + "! 👋");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });

            mDatabase.child("UserItems").child(userId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    long count = snapshot.getChildrenCount();
                    tvActiveItems.setText("You have " + count + " active items");
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }
    }

    private void fetchRecentItems() {
        // Fetch both lost and found items for the recent list
        mDatabase.child("LostItems").limitToLast(5).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateItemList(snapshot);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        mDatabase.child("FoundItems").limitToLast(5).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                updateItemList(snapshot);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private synchronized void updateItemList(DataSnapshot snapshot) {
        for (DataSnapshot data : snapshot.getChildren()) {
            Item item = data.getValue(Item.class);
            if (item != null) {
                boolean exists = false;
                for (int i = 0; i < itemList.size(); i++) {
                    if (itemList.get(i).getId().equals(item.getId())) {
                        itemList.set(i, item);
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    itemList.add(0, item);
                }
            }
        }
        // Sort by timestamp if available
        itemList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
        adapter.notifyDataSetChanged();
    }

    private class RecentItemsAdapter extends RecyclerView.Adapter<RecentItemsAdapter.ViewHolder> {
        private List<Item> items;

        public RecentItemsAdapter(List<Item> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_campus_reported_recent, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Item item = items.get(position);
            holder.tvTitle.setText(item.getName());
            holder.tvLocation.setText(item.getLocation());
            holder.tvTime.setText(item.getDate());
            
            // Color code based on status
            if ("lost".equals(item.getStatus())) {
                holder.statusIndicator.setBackgroundColor(0xFFE53935); // Red
            } else {
                holder.statusIndicator.setBackgroundColor(0xFF2E7D32); // Green
            }
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvLocation, tvTime;
            ImageView ivIcon;
            View statusIndicator;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvItemTitle);
                tvLocation = itemView.findViewById(R.id.tvItemLocation);
                tvTime = itemView.findViewById(R.id.tvItemTime);
                ivIcon = itemView.findViewById(R.id.ivItemIcon);
                statusIndicator = itemView.findViewById(R.id.viewStatusIndicator); // Ensure this exists in layout
            }
        }
    }
}
