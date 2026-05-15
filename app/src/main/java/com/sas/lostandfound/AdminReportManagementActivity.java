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

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.reflect.TypeToken;

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
    private TextView tvStatTotal, tvStatPending, tvStatReviewed;
    private View cardTotal, cardPending, cardReviewed;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private ReportAdapter adapter;
    private List<AdminReport> allReports;
    private List<AdminReport> filteredReports;
    private boolean isFetching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ensure only admins can manage reports
        RoleVerifier.checkAdminAccess(this);

        setContentView(R.layout.activity_admin_report_management);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchAndFilter();
        setupSwipeRefresh();

        // Ensure back press always exits the activity immediately
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        fetchReports();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh when returning from review screen in case it was deleted or updated
        if (allReports != null && !allReports.isEmpty()) {
            fetchReports();
        }
    }

    private void initializeViews() {
        rvAdminReports = findViewById(R.id.rvAdminReports);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatPending = findViewById(R.id.tvStatPending);
        tvStatReviewed = findViewById(R.id.tvStatReviewed);
        cardTotal = findViewById(R.id.cardTotal);
        cardPending = findViewById(R.id.cardPending);
        cardReviewed = findViewById(R.id.cardReviewed);
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

        if (cardTotal != null) cardTotal.setOnClickListener(v -> chipGroupFilter.check(R.id.chipAll));
        if (cardPending != null) cardPending.setOnClickListener(v -> chipGroupFilter.check(R.id.chipPending));
        if (cardReviewed != null) cardReviewed.setOnClickListener(v -> chipGroupFilter.check(R.id.chipReviewed));
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(this::fetchReports);
        }
    }

    private void fetchReports() {
        if (isFetching) return;
        isFetching = true;

        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }
        
        SupabaseDatabaseHelper.select("admin_reports", "select=*", new TypeToken<List<AdminReport>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<AdminReport>>() {
            @Override
            public void onSuccess(List<AdminReport> reports) {
                List<AdminReport> tempReports = new ArrayList<>();
                int total = 0, pending = 0, reviewed = 0;
                
                if (reports != null) {
                    for (AdminReport report : reports) {
                        tempReports.add(report);
                        total++;
                        String status = report.getStatus();
                        if ("Pending".equalsIgnoreCase(status)) pending++;
                        else if ("Reviewed".equalsIgnoreCase(status)) reviewed++;
                    }
                }
                
                tvStatTotal.setText(String.valueOf(total));
                tvStatPending.setText(String.valueOf(pending));
                tvStatReviewed.setText(String.valueOf(reviewed));
                
                Collections.sort(tempReports, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
                
                allReports.clear();
                allReports.addAll(tempReports);
                applyFilters();
                
                isFetching = false;
                progressBar.setVisibility(View.GONE);
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(String errorMessage) {
                isFetching = false;
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
            String formattedId = "#" + displayId;
            String reporterName = report.getReporterName() != null ? report.getReporterName().toLowerCase() : "";
            String title = report.getTitle() != null ? report.getTitle().toLowerCase() : "";
            String category = report.getCategory() != null ? report.getCategory().toLowerCase() : "";
            String relatedId = report.getRelatedId() != null ? report.getRelatedId().toLowerCase() : "";
            String internalId = report.getId() != null ? report.getId().toLowerCase() : "";

            boolean matchesSearch = displayId.contains(query) ||
                    formattedId.contains(query) ||
                    reporterName.contains(query) ||
                    relatedId.contains(query) ||
                    category.contains(query) ||
                    internalId.contains(query) ||
                    title.contains(query);
            
            boolean matchesStatus = true;
            if (checkedChipId == R.id.chipPending) matchesStatus = "Pending".equalsIgnoreCase(report.getStatus());
            else if (checkedChipId == R.id.chipReviewed) matchesStatus = "Reviewed".equalsIgnoreCase(report.getStatus());
            
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
            
            holder.tvDisplayId.setText(ReportIdFormatter.format(report.getDisplayId()));
            
            holder.tvTitle.setText(report.getTitle());
            holder.tvCategory.setText("Category: " + report.getCategory());
            holder.tvReporterInfo.setText("Submitted By: " + report.getReporterName());
            
            String status = report.getStatus() != null ? report.getStatus() : "Pending";
            holder.tvStatus.setText(status.toUpperCase());

            int statusColor;
            if ("Pending".equalsIgnoreCase(status)) {
                statusColor = 0xFFFF9800; // Orange
            } else if ("Reviewed".equalsIgnoreCase(status)) {
                statusColor = 0xFF2AABEE; // Blue
            } else {
                statusColor = 0xFF757575; // Gray fallback
            }
            holder.cardStatusBadge.setCardBackgroundColor(statusColor);

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
            holder.tvTimestamp.setText(sdf.format(new Date(report.getTimestamp())));

            setupImageOrSlider(holder, report);

            View.OnClickListener navigateToDetails = v -> {
                Intent intent = new Intent(AdminReportManagementActivity.this, AdminReportReviewActivity.class);
                intent.putExtra("reportId", report.getReportId());
                startActivity(intent);
            };

            holder.itemView.setOnClickListener(navigateToDetails);
            // Ensure child components that might intercept clicks also navigate to details
            holder.ivIcon.setOnClickListener(navigateToDetails);
            
            // For ViewPager2, we need to set the click listener on the adapter
            if (holder.viewPagerSlider.getAdapter() instanceof ImageSliderAdapter) {
                ((ImageSliderAdapter) holder.viewPagerSlider.getAdapter()).setOnImageClickListener(pos -> {
                    Intent intent = new Intent(AdminReportManagementActivity.this, AdminReportReviewActivity.class);
                    intent.putExtra("reportId", report.getReportId());
                    startActivity(intent);
                });
            }
        }

        private void setupImageOrSlider(ViewHolder holder, AdminReport report) {
            List<String> urls = report.getImageUrls();
            if (urls != null && urls.size() > 1) {
                holder.ivIcon.setVisibility(View.GONE);
                holder.viewPagerSlider.setVisibility(View.VISIBLE);
                holder.tabLayoutIndicator.setVisibility(View.VISIBLE);

                // Use fitCenter (true) for multiple images to prevent zooming in cards
                ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(urls, true);
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
                } else {
                    holder.ivIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    holder.ivIcon.setPadding(48, 48, 48, 48);
                    holder.ivIcon.setImageResource(R.drawable.ic_shield);
                    holder.ivIcon.setImageTintList(android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(AdminReportManagementActivity.this, R.color.primaryColor)));
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
            MaterialCardView cardStatusBadge;

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
                cardStatusBadge = itemView.findViewById(R.id.cardStatusBadge);
            }
        }
    }
}
