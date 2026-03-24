package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AllReportedItemsActivity extends AppCompatActivity {

    private RecyclerView rvAllItems;
    private AllItemsAdapter adapter;
    private List<Item> itemList;
    private List<Item> filteredList;
    private ProgressBar progressBar;
    private Toolbar toolbar;
    private TextView tvHeaderTitle;
    private String filterStatus; // "lost", "found", "returned" or null
    private String targetUserId;
    private String userName;
    private boolean isAdmin = false;

    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reported_items);

        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();
        filterStatus = getIntent().getStringExtra("filterStatus");
        targetUserId = getIntent().getStringExtra("targetUserId");
        userName = getIntent().getStringExtra("userName");
        isAdmin = getIntent().getBooleanExtra("isAdmin", false);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        fetchAllItems();
    }

    private void initializeViews() {
        rvAllItems = findViewById(R.id.rvAllItems);
        progressBar = findViewById(R.id.progressBar);
        toolbar = findViewById(R.id.toolbar);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        
        String prefix = (userName != null && !userName.isEmpty()) ? userName + "'s " : "All ";
        
        if ("lost".equalsIgnoreCase(filterStatus)) {
            tvHeaderTitle.setText(prefix + "Lost Reports");
        } else if ("found".equalsIgnoreCase(filterStatus)) {
            tvHeaderTitle.setText(prefix + "Found Reports");
        } else if ("returned".equalsIgnoreCase(filterStatus)) {
            tvHeaderTitle.setText(prefix + "Returned Items");
        } else {
            tvHeaderTitle.setText("All Reported Items");
        }
    }

    private void setupToolbar() {
        if (toolbar != null) {
            toolbar.setTitle("");
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
    }

    private void setupRecyclerView() {
        itemList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new AllItemsAdapter(filteredList);
        rvAllItems.setLayoutManager(new LinearLayoutManager(this));
        rvAllItems.setAdapter(adapter);
    }

    private void fetchAllItems() {
        progressBar.setVisibility(View.VISIBLE);
        ValueEventListener itemListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Item item = data.getValue(Item.class);
                    if (item != null) {
                        updateOrAddItem(item);
                    }
                }
                applyFilter();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        };

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

    private void applyFilter() {
        filteredList.clear();
        for (Item item : itemList) {
            boolean matchesUser = (targetUserId == null || item.getUserId().equals(targetUserId));
            if (!matchesUser) continue;

            if (filterStatus == null) {
                filteredList.add(item);
            } else if ("returned".equalsIgnoreCase(filterStatus)) {
                String status = item.getAdminStatus();
                if ("Returned".equalsIgnoreCase(status) || "Claimed".equalsIgnoreCase(status)) {
                    filteredList.add(item);
                }
            } else if (item.getStatus().equalsIgnoreCase(filterStatus)) {
                filteredList.add(item);
            }
        }
        filteredList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
        adapter.notifyDataSetChanged();
    }

    private class AllItemsAdapter extends RecyclerView.Adapter<AllItemsAdapter.ViewHolder> {
        private List<Item> items;

        public AllItemsAdapter(List<Item> items) {
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
            
            if ("lost".equals(item.getStatus())) {
                holder.statusIndicator.setBackgroundColor(0xFFA31621);
                holder.tvBadge.setText("LOST");
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_lost_bg));
            } else {
                holder.statusIndicator.setBackgroundColor(0xFF2E7D32);
                holder.tvBadge.setText("FOUND");
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_found_bg));
            }
            holder.tvBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                holder.ivIcon.setImageTintList(null);
                holder.ivIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Glide.with(holder.itemView.getContext())
                        .load(item.getImageUrl())
                        .placeholder(R.drawable.ic_package)
                        .centerCrop()
                        .into(holder.ivIcon);
            } else {
                holder.ivIcon.setImageResource(R.drawable.ic_package);
                holder.ivIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                holder.ivIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(holder.itemView.getContext(), R.color.textSecondary)));
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ItemDetailActivity.class);
                intent.putExtra("itemId", item.getId());
                intent.putExtra("itemName", item.getName());
                intent.putExtra("itemDescription", item.getDescription());
                intent.putExtra("itemLocation", item.getLocation());
                intent.putExtra("itemDate", item.getDate());
                intent.putExtra("itemTime", item.getTime());
                intent.putExtra("itemStatus", item.getStatus());
                intent.putExtra("itemCategory", item.getCategory());
                intent.putExtra("itemImageUrl", item.getImageUrl());
                intent.putExtra("userName", item.getUserName());
                intent.putExtra("userDepartment", item.getUserDepartment());
                intent.putExtra("userPhone", item.getUserPhone());
                intent.putExtra("userId", item.getUserId());
                intent.putExtra("isAdmin", isAdmin);
                v.getContext().startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvLocation, tvTime, tvBadge;
            ImageView ivIcon;
            View statusIndicator;
            MaterialCardView cardBadge;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvItemTitle);
                tvLocation = itemView.findViewById(R.id.tvItemLocation);
                tvTime = itemView.findViewById(R.id.tvItemTime);
                ivIcon = itemView.findViewById(R.id.ivItemIcon);
                statusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
                tvBadge = itemView.findViewById(R.id.tvBadge);
                cardBadge = itemView.findViewById(R.id.cardBadge);
            }
        }
    }
}
