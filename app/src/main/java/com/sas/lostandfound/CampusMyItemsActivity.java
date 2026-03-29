package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CampusMyItemsActivity extends AppCompatActivity {

    private static final String TAG = "CampusMyItems";
    private RecyclerView rvMyItems;
    private MyItemsAdapter adapter;
    private List<Item> itemList;
    private String filterType;
    private TextView tvHeaderTitle;
    private Toolbar toolbar;
    private boolean fromDrawer = false;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_campus_my_items);

        filterType = getIntent().getStringExtra("filterType");
        if (filterType == null) filterType = "reported";

        fromDrawer = getIntent().getBooleanExtra("fromDrawer", false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        rvMyItems = findViewById(R.id.rvMyItems);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        toolbar = findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        View.OnClickListener backClickListener = v -> {
            Intent intent = new Intent(CampusMyItemsActivity.this, CampusDashboardActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            if (fromDrawer) {
                intent.putExtra("openDrawer", true);
            }
            startActivity(intent);
            finish();
        };

        toolbar.setNavigationOnClickListener(backClickListener);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                backClickListener.onClick(null);
            }
        });

        setupTitle();

        itemList = new ArrayList<>();
        adapter = new MyItemsAdapter(itemList);
        rvMyItems.setLayoutManager(new LinearLayoutManager(this));
        rvMyItems.setAdapter(adapter);

        fetchMyItems();
    }

    private void setupTitle() {
        switch (filterType) {
            case "reported":
                tvHeaderTitle.setText("My Found Reports");
                break;
            case "find":
                tvHeaderTitle.setText("My Lost Reports");
                break;
            case "resolved":
                tvHeaderTitle.setText("My Resolved Items");
                break;
            case "admin_reports":
                tvHeaderTitle.setText("My Admin Reports");
                break;
            default:
                tvHeaderTitle.setText("My Reports");
                break;
        }
    }

    private void fetchMyItems() {
        if (mAuth.getCurrentUser() == null) return;
        String authUid = mAuth.getCurrentUser().getUid();

        mDatabase.child("UIDToUniversityID").child(authUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final String resolvedUserId = snapshot.exists() ? snapshot.getValue(String.class) : authUid;
                loadItems(resolvedUserId, authUid);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                loadItems(authUid, authUid);
            }
        });
    }

    private void loadItems(String universityId, String authUid) {
        ValueEventListener itemListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    Item item = data.getValue(Item.class);
                    if (item != null) {
                        if (shouldInclude(item, universityId)) {
                            updateOrAddItem(item);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };

        mDatabase.child("LostItems").addValueEventListener(itemListener);
        mDatabase.child("FoundItems").addValueEventListener(itemListener);

        // Also fetch Admin Reports submitted by this user
        mDatabase.child("AdminReports").orderByChild("reporterAuthId").equalTo(authUid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot data : snapshot.getChildren()) {
                    AdminReport report = data.getValue(AdminReport.class);
                    if (report != null) {
                        Item item = convertToItem(report);
                        if (shouldInclude(item, universityId)) {
                            updateOrAddItem(item);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error fetching admin reports: " + error.getMessage());
            }
        });
    }

    private Item convertToItem(AdminReport report) {
        Item item = new Item();
        item.setId(report.getReportId());
        item.setDisplayId(report.getDisplayId());
        item.setName(report.getTitle());
        item.setDescription(report.getDescription());
        item.setCategory(report.getCategory());
        item.setUserId(report.getReporterAuthId());
        item.setStatus("admin_report");
        item.setAdminStatus(report.getStatus());
        item.setTimestamp(report.getTimestamp());
        item.setImageUrl(report.getImageUrl());
        item.setUserName(report.getReporterName());
        item.setUserPhone(report.getPhone());
        item.setLocation("Reported to Admin");
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        item.setDate(sdf.format(new Date(report.getTimestamp())));

        return item;
    }

    private boolean shouldInclude(Item item, String userId) {
        boolean isResolved = "Claimed".equalsIgnoreCase(item.getAdminStatus()) || "Returned".equalsIgnoreCase(item.getAdminStatus());

        if ("admin_report".equals(item.getStatus())) {
            return "reported".equals(filterType) || "admin_reports".equals(filterType);
        }

        switch (filterType) {
            case "reported":
                return "found".equals(item.getStatus()) && userId.equals(item.getUserId()) && !isResolved;
            case "find":
                return "lost".equals(item.getStatus()) && userId.equals(item.getUserId()) && !isResolved;
            case "resolved":
                return isResolved && (userId.equals(item.getUserId()) || userId.equals(item.getClaimedByUserId()));
            default:
                return false;
        }
    }

    private synchronized void updateOrAddItem(Item item) {
        for (int i = 0; i < itemList.size(); i++) {
            if (itemList.get(i).getId().equals(item.getId())) {
                itemList.set(i, item);
                return;
            }
        }
        itemList.add(item);
        itemList.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
    }

    private class MyItemsAdapter extends RecyclerView.Adapter<MyItemsAdapter.ViewHolder> {
        private List<Item> items;
        private Map<Integer, Runnable> sliderRunnables = new HashMap<>();
        private Handler sliderHandler = new Handler(Looper.getMainLooper());

        public MyItemsAdapter(List<Item> items) {
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
            
            if (holder.tvReportId != null) {
                holder.tvReportId.setText(item.getDisplayId() != null ? item.getDisplayId() : "");
            }

            boolean isResolved = "Claimed".equalsIgnoreCase(item.getAdminStatus()) || "Returned".equalsIgnoreCase(item.getAdminStatus());

            if ("admin_report".equals(item.getStatus())) {
                String status = item.getAdminStatus();
                int statusColor;
                if ("Pending".equalsIgnoreCase(status)) statusColor = 0xFF757575; // Gray
                else if ("Reviewed".equalsIgnoreCase(status)) statusColor = 0xFF1976D2; // Blue
                else statusColor = 0xFF2E7D32; // Green
                
                holder.statusIndicator.setBackgroundColor(statusColor);
                holder.tvBadge.setText(status.toUpperCase());
                holder.cardBadge.setCardBackgroundColor(statusColor);
            } else if (isResolved) {
                holder.statusIndicator.setBackgroundColor(0xFF2E7D32); // Green
                holder.tvBadge.setText("RESOLVED");
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_found_bg));
            } else if ("lost".equals(item.getStatus())) {
                holder.statusIndicator.setBackgroundColor(0xFFA31621); // Red
                holder.tvBadge.setText("LOST");
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_lost_bg));
            } else {
                holder.statusIndicator.setBackgroundColor(0xFF2E7D32); // Green
                holder.tvBadge.setText("FOUND");
                holder.cardBadge.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.badge_found_bg));
            }
            holder.tvBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));

            setupImageOrSlider(holder, item, position);

            holder.itemView.setOnClickListener(v -> {
                if ("admin_report".equals(item.getStatus())) {
                    Toast.makeText(v.getContext(), "Admin Report: " + item.getName(), Toast.LENGTH_SHORT).show();
                    return;
                }
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
                intent.putExtra("itemReportId", item.getDisplayId());
                v.getContext().startActivity(intent);
            });
        }

        private void setupImageOrSlider(ViewHolder holder, Item item, int position) {
            List<String> urls = item.getImageUrls();
            if (urls != null && urls.size() > 1 && holder.viewPagerSlider != null) {
                holder.ivIcon.setVisibility(View.GONE);
                holder.viewPagerSlider.setVisibility(View.VISIBLE);
                holder.tabLayoutIndicator.setVisibility(View.VISIBLE);

                ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(urls);
                holder.viewPagerSlider.setAdapter(sliderAdapter);
                holder.viewPagerSlider.setUserInputEnabled(true);

                new TabLayoutMediator(holder.tabLayoutIndicator, holder.viewPagerSlider, (tab, pos) -> {}).attach();

                stopSlider(position);
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (holder.viewPagerSlider != null) {
                            int current = holder.viewPagerSlider.getCurrentItem();
                            int next = (current + 1) % urls.size();
                            holder.viewPagerSlider.setCurrentItem(next, true);
                            sliderHandler.postDelayed(this, 3000);
                        }
                    }
                };
                sliderRunnables.put(position, runnable);
                sliderHandler.postDelayed(runnable, 3000);
            } else {
                if (holder.viewPagerSlider != null) holder.viewPagerSlider.setVisibility(View.GONE);
                if (holder.tabLayoutIndicator != null) holder.tabLayoutIndicator.setVisibility(View.GONE);
                holder.ivIcon.setVisibility(View.VISIBLE);
                stopSlider(position);

                if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                    Glide.with(holder.itemView.getContext())
                            .load(item.getImageUrl())
                            .placeholder(R.drawable.ic_package)
                            .centerCrop()
                            .into(holder.ivIcon);
                } else {
                    holder.ivIcon.setImageResource(R.drawable.ic_package);
                }
            }
        }

        private void stopSlider(int position) {
            Runnable runnable = sliderRunnables.get(position);
            if (runnable != null) {
                sliderHandler.removeCallbacks(runnable);
                sliderRunnables.remove(position);
            }
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            super.onViewRecycled(holder);
            stopSlider(holder.getBindingAdapterPosition());
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTitle, tvLocation, tvTime, tvBadge, tvReportId;
            ImageView ivIcon;
            View statusIndicator;
            MaterialCardView cardBadge;
            ViewPager2 viewPagerSlider;
            TabLayout tabLayoutIndicator;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvItemTitle);
                tvLocation = itemView.findViewById(R.id.tvItemLocation);
                tvTime = itemView.findViewById(R.id.tvItemTime);
                ivIcon = itemView.findViewById(R.id.ivItemIcon);
                statusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
                tvBadge = itemView.findViewById(R.id.tvBadge);
                cardBadge = itemView.findViewById(R.id.cardBadge);
                viewPagerSlider = itemView.findViewById(R.id.viewPagerSlider);
                tabLayoutIndicator = itemView.findViewById(R.id.tabLayoutIndicator);
                tvReportId = itemView.findViewById(R.id.tvReportId);
            }
        }
    }
}
