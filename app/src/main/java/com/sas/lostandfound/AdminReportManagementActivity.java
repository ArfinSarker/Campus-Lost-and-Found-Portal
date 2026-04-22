package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.viewpager2.widget.ViewPager2;

public class AdminReportManagementActivity extends AppCompatActivity {

    private static final String TAG = "AdminReportManagement";
    private RecyclerView rvAdminReports;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private TextView tvStatTotal, tvStatPending, tvStatReviewed, tvStatResolved;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private ReportAdapter adapter;
    private List<AdminReport> allReports;
    private List<AdminReport> filteredReports;

    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report_management);

        mDatabase = FirebaseDatabase.getInstance(FirebaseConfig.DATABASE_URL).getReference();

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchAndFilter();
        setupSwipeRefresh();
        fetchReports();
    }

    private void initializeViews() {
        rvAdminReports = findViewById(R.id.rvAdminReports);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatPending = findViewById(R.id.tvStatPending);
        tvStatReviewed = findViewById(R.id.tvStatReviewed);
        tvStatResolved = findViewById(R.id.tvStatResolved);
        etSearch = findViewById(R.id.etSearch);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        toolbar = findViewById(R.id.toolbar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
            
            com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
            if (appBarLayout != null) {
                HeaderColorHelper.setup(this, appBarLayout, toolbar);
            }
        }
    }

    private void setupRecyclerView() {
        allReports = new ArrayList<>();
        filteredReports = new ArrayList<>();
        adapter = new ReportAdapter(filteredReports);
        rvAdminReports.setLayoutManager(new LinearLayoutManager(this));
        rvAdminReports.setAdapter(adapter);
    }

    private void setupSearchAndFilter() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> applyFilters());
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(this::fetchReports);
        }
    }

    private void fetchReports() {
        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        mDatabase.child("AdminReports").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allReports.clear();
                int total = 0, pending = 0, reviewed = 0, resolved = 0;
                
                if (snapshot.exists()) {
                    for (DataSnapshot data : snapshot.getChildren()) {
                        try {
                            AdminReport report = data.getValue(AdminReport.class);
                            if (report != null) {
                                allReports.add(report);
                                total++;
                                String status = report.getStatus();
                                if ("Pending".equalsIgnoreCase(status)) pending++;
                                else if ("Reviewed".equalsIgnoreCase(status)) reviewed++;
                                else if ("Resolved".equalsIgnoreCase(status)) resolved++;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing report", e);
                        }
                    }
                }
                
                tvStatTotal.setText(String.valueOf(total));
                tvStatPending.setText(String.valueOf(pending));
                tvStatReviewed.setText(String.valueOf(reviewed));
                tvStatResolved.setText(String.valueOf(resolved));
                
                Collections.sort(allReports, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                applyFilters();
                progressBar.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void applyFilters() {
        String query = etSearch.getText().toString().toLowerCase().trim();
        int checkedChipId = chipGroupFilter.getCheckedChipId();
        
        filteredReports.clear();
        for (AdminReport report : allReports) {
            String displayId = report.getDisplayId() != null ? report.getDisplayId().toLowerCase() : "";
            String reporterName = report.getReporterName() != null ? report.getReporterName().toLowerCase() : "";
            String title = report.getTitle() != null ? report.getTitle().toLowerCase() : "";
            String relatedId = report.getRelatedId() != null ? report.getRelatedId().toLowerCase() : "";

            boolean matchesSearch = displayId.contains(query) ||
                    reporterName.contains(query) ||
                    relatedId.contains(query) ||
                    title.contains(query);
            
            boolean matchesStatus = true;
            if (checkedChipId == R.id.chipPending) matchesStatus = "Pending".equalsIgnoreCase(report.getStatus());
            else if (checkedChipId == R.id.chipReviewed) matchesStatus = "Reviewed".equalsIgnoreCase(report.getStatus());
            else if (checkedChipId == R.id.chipResolved) matchesStatus = "Resolved".equalsIgnoreCase(report.getStatus());
            
            if (matchesSearch && matchesStatus) {
                filteredReports.add(report);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmptyState.setVisibility(filteredReports.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {
        private List<AdminReport> reports;

        public ReportAdapter(List<AdminReport> reports) {
            this.reports = reports;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_report, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AdminReport report = reports.get(position);
            
            String displayId = report.getDisplayId() != null ? report.getDisplayId() : "N/A";
            if (!displayId.startsWith("#")) displayId = "#" + displayId;
            holder.tvDisplayId.setText(displayId);
            
            holder.tvTitle.setText(report.getTitle());
            holder.tvCategory.setText("Category: " + report.getCategory());
            holder.tvReporterInfo.setText("Submitted By: " + report.getReporterName());
            holder.tvStatus.setText(report.getStatus().toUpperCase());

            int statusColor;
            String status = report.getStatus();
            if ("Pending".equalsIgnoreCase(status)) statusColor = 0xFF757575; // Gray
            else if ("Reviewed".equalsIgnoreCase(status)) statusColor = 0xFF1976D2; // Blue
            else statusColor = 0xFF2E7D32; // Green
            
            holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor));

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
            holder.tvTimestamp.setText(sdf.format(new Date(report.getTimestamp())));

            setupImageOrSlider(holder, report);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(AdminReportManagementActivity.this, AdminReportReviewActivity.class);
                intent.putExtra("reportId", report.getReportId());
                startActivity(intent);
            });
        }

        private void setupImageOrSlider(ViewHolder holder, AdminReport report) {
            List<String> urls = report.getImageUrls();
            if (urls != null && urls.size() > 1) {
                holder.ivIcon.setVisibility(View.GONE);
                holder.viewPagerSlider.setVisibility(View.VISIBLE);
                holder.tabLayoutIndicator.setVisibility(View.VISIBLE);

                ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(urls);
                holder.viewPagerSlider.setAdapter(sliderAdapter);
                new TabLayoutMediator(holder.tabLayoutIndicator, holder.viewPagerSlider, (tab, pos) -> {}).attach();
            } else {
                holder.viewPagerSlider.setVisibility(View.GONE);
                holder.tabLayoutIndicator.setVisibility(View.GONE);
                holder.ivIcon.setVisibility(View.VISIBLE);

                String imageUrl = (urls != null && !urls.isEmpty()) ? urls.get(0) : report.getImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    holder.ivIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    holder.ivIcon.setPadding(0, 0, 0, 0);
                    holder.ivIcon.setImageTintList(null);
                    GlideApp.with(AdminReportManagementActivity.this)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_shield)
                            .thumbnail(0.1f)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(holder.ivIcon);

                    holder.ivIcon.setOnClickListener(v -> {
                        Intent intent = new Intent(v.getContext(), FullScreenImageActivity.class);
                        ArrayList<String> singleUrl = new ArrayList<>();
                        singleUrl.add(imageUrl);
                        intent.putStringArrayListExtra("imageUrls", singleUrl);
                        intent.putExtra("position", 0);
                        v.getContext().startActivity(intent);
                    });
                } else {
                    holder.ivIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    holder.ivIcon.setPadding(48, 48, 48, 48);
                    holder.ivIcon.setImageResource(R.drawable.ic_shield);
                    holder.ivIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(AdminReportManagementActivity.this, R.color.primaryColor)));
                    holder.ivIcon.setOnClickListener(null);
                }
            }
        }

        @Override
        public int getItemCount() {
            return reports.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDisplayId, tvTitle, tvCategory, tvReporterInfo, tvStatus, tvTimestamp;
            ImageView ivIcon;
            ViewPager2 viewPagerSlider;
            TabLayout tabLayoutIndicator;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDisplayId = itemView.findViewById(R.id.tvDisplayId);
                tvTitle = itemView.findViewById(R.id.tvReportTitle);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvReporterInfo = itemView.findViewById(R.id.tvReporterInfo);
                tvStatus = itemView.findViewById(R.id.tvStatusBadge);
                tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
                ivIcon = itemView.findViewById(R.id.ivReportIcon);
                viewPagerSlider = itemView.findViewById(R.id.viewPagerSlider);
                tabLayoutIndicator = itemView.findViewById(R.id.tabLayoutIndicator);
            }
        }
    }
}
