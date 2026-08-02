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
import android.os.Handler;
import android.os.Looper;
import java.util.HashMap;
import java.util.Map;

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
    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Map<Integer, Runnable> sliderRunnables = new HashMap<>();

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
                int headerColor = androidx.core.content.ContextCompat.getColor(this, R.color.profile_background);
                boolean isNight = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                HeaderColorHelper.setup(this, appBarLayout, headerColor, headerColor, !isNight);
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
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        chipGroupFilter.setOnCheckedChangeListener((group, checkedId) -> applyFilters());

        if (cardTotal != null)
            cardTotal.setOnClickListener(v -> chipGroupFilter.check(R.id.chipAll));
        if (cardPending != null)
            cardPending.setOnClickListener(v -> chipGroupFilter.check(R.id.chipPending));
        if (cardReviewed != null)
            cardReviewed.setOnClickListener(v -> chipGroupFilter.check(R.id.chipReviewed));
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(this::fetchReports);
        }
    }

    private void fetchReports() {
        if (isFetching)
            return;
        isFetching = true;

        if (swipeRefreshLayout == null || !swipeRefreshLayout.isRefreshing()) {
            progressBar.setVisibility(View.VISIBLE);
        }

        SupabaseDatabaseHelper.select("admin_reports", "select=*", new TypeToken<List<AdminReport>>() {
        }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<AdminReport>>() {
            @Override
            public void onSuccess(List<AdminReport> reports) {
                List<AdminReport> tempReports = new ArrayList<>();
                int total = 0, pending = 0, reviewed = 0;
                List<String> reporterIds = new ArrayList<>();

                if (reports != null) {
                    for (AdminReport report : reports) {
                        tempReports.add(report);
                        total++;
                        String status = report.getStatus();
                        if ("Pending".equalsIgnoreCase(status))
                            pending++;
                        else if ("Reviewed".equalsIgnoreCase(status))
                            reviewed++;

                        String repId = report.getUniversityId();
                        if (repId != null && !repId.trim().isEmpty() && !reporterIds.contains(repId)) {
                            reporterIds.add(repId);
                        }
                    }
                }

                if (!reporterIds.isEmpty()) {
                    StringBuilder queryBuilder = new StringBuilder();
                    queryBuilder.append("select=university_id,profile_image_url&university_id=in.(");
                    for (int i = 0; i < reporterIds.size(); i++) {
                        if (i > 0) {
                            queryBuilder.append(",");
                        }
                        queryBuilder.append(reporterIds.get(i));
                    }
                    queryBuilder.append(")");

                    final int finalTotal = total;
                    final int finalPending = pending;
                    final int finalReviewed = reviewed;

                    SupabaseDatabaseHelper.select("profiles", queryBuilder.toString(), new TypeToken<List<User>>() {
                    }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
                        @Override
                        public void onSuccess(List<User> users) {
                            Map<String, String> profileMap = new HashMap<>();
                            if (users != null) {
                                for (User u : users) {
                                    if (u.getUniversityId() != null && u.getProfileImageUrl() != null) {
                                        profileMap.put(u.getUniversityId(), u.getProfileImageUrl());
                                    }
                                }
                            }

                            for (AdminReport report : tempReports) {
                                String url = profileMap.get(report.getUniversityId());
                                if (url != null) {
                                    report.setReporterProfileImageUrl(url);
                                }
                            }

                            finalizeFetch(tempReports, finalTotal, finalPending, finalReviewed);
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            finalizeFetch(tempReports, finalTotal, finalPending, finalReviewed);
                        }
                    });
                } else {
                    finalizeFetch(tempReports, total, pending, reviewed);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                isFetching = false;
                progressBar.setVisibility(View.GONE);
                if (swipeRefreshLayout != null)
                    swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void finalizeFetch(List<AdminReport> tempReports, int total, int pending, int reviewed) {
        tvStatTotal.setText(String.valueOf(total));
        tvStatPending.setText(String.valueOf(pending));
        tvStatReviewed.setText(String.valueOf(reviewed));

        Collections.sort(tempReports, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

        allReports.clear();
        allReports.addAll(tempReports);
        applyFilters();

        isFetching = false;
        progressBar.setVisibility(View.GONE);
        if (swipeRefreshLayout != null)
            swipeRefreshLayout.setRefreshing(false);
    }

    private void applyFilters() {
        String query = etSearch.getText().toString().toLowerCase().trim();
        int checkedChipId = chipGroupFilter.getCheckedChipId();

        List<AdminReport> temp = new ArrayList<>();
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
            if (checkedChipId == R.id.chipPending)
                matchesStatus = "Pending".equalsIgnoreCase(report.getStatus());
            else if (checkedChipId == R.id.chipReviewed)
                matchesStatus = "Reviewed".equalsIgnoreCase(report.getStatus());

            if (matchesSearch && matchesStatus) {
                temp.add(report);
            }
        }
        adapter.updateReports(temp);
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

            SimpleDateFormat sdfDate = new SimpleDateFormat("dd MMM", Locale.getDefault());
            SimpleDateFormat sdfTime = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.tvDate.setText(sdfDate.format(new Date(report.getTimestamp())));
            holder.tvTime.setText(sdfTime.format(new Date(report.getTimestamp())));

            // Load submitter's profile picture if available, fallback to default user icon
            String profileUrl = report.getReporterProfileImageUrl();
            if (profileUrl != null && !profileUrl.isEmpty()) {
                holder.ivReporterAvatar.setImageTintList(null); // Clear tint for user photo
                GlideApp.with(AdminReportManagementActivity.this)
                        .load(profileUrl)
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(holder.ivReporterAvatar);
            } else {
                holder.ivReporterAvatar.setImageResource(R.drawable.ic_user);
                holder.ivReporterAvatar.setImageTintList(android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(AdminReportManagementActivity.this, R.color.textSecondary)));
            }

            // Handle card click to open details
            holder.itemView.setOnClickListener(v -> {
                if (!ItemNavigationUtils.canNavigate())
                    return;
                Intent intent = new Intent(AdminReportManagementActivity.this, AdminReportReviewActivity.class);
                intent.putExtra("reportId", report.getReportId());
                intent.putExtra("reportJson", new com.google.gson.Gson().toJson(report));
                startActivity(intent);
            });

            setupImageOrSlider(holder, report, position);
        }

        private void setupImageOrSlider(ViewHolder holder, AdminReport report, int position) {
            List<String> urls = report.getImageUrls();
            if (urls != null && urls.size() > 1) {
                holder.ivIcon.setVisibility(View.GONE);
                holder.viewPagerSlider.setVisibility(View.VISIBLE);
                holder.tabLayoutIndicator.setVisibility(View.VISIBLE);

                ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(urls, true);
                sliderAdapter.setOnImageClickListener(pos -> {
                    Intent intent = new Intent(AdminReportManagementActivity.this, AdminReportReviewActivity.class);
                    intent.putExtra("reportId", report.getReportId());
                    intent.putExtra("reportJson", new com.google.gson.Gson().toJson(report));
                    startActivity(intent);
                });
                holder.viewPagerSlider.setAdapter(sliderAdapter);
                new TabLayoutMediator(holder.tabLayoutIndicator, holder.viewPagerSlider, (tab, pos) -> {
                }).attach();

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
                holder.viewPagerSlider.setVisibility(View.GONE);
                holder.tabLayoutIndicator.setVisibility(View.GONE);
                holder.ivIcon.setVisibility(View.VISIBLE);
                stopSlider(position);

                String imageUrl = (urls != null && !urls.isEmpty()) ? urls.get(0) : report.getImageUrl();
                if (imageUrl != null && !imageUrl.isEmpty() && !"null".equalsIgnoreCase(imageUrl.trim())) {
                    holder.ivIcon.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    holder.ivIcon.setPadding(0, 0, 0, 0);
                    holder.ivIcon.setImageTintList(null);
                    holder.ivIcon.setBackground(null);
                    GlideApp.with(AdminReportManagementActivity.this)
                            .load(SupabaseStorageHelper.ensurePublicUrl(imageUrl))
                            .thumbnail(0.1f)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .centerCrop()
                            .into(holder.ivIcon);
                } else {
                    holder.ivIcon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    holder.ivIcon.setImageResource(R.drawable.ic_manage_no_image);
                    int bg = ContextCompat.getColor(AdminReportManagementActivity.this, R.color.manage_placeholder_bg);
                    int tint = ContextCompat.getColor(AdminReportManagementActivity.this, R.color.manage_placeholder_icon);
                    holder.ivIcon.setBackgroundColor(bg);
                    holder.ivIcon.setImageTintList(android.content.res.ColorStateList.valueOf(tint));
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
            return reports.size();
        }

        public void updateReports(List<AdminReport> newReports) {
            androidx.recyclerview.widget.DiffUtil.DiffResult diffResult = androidx.recyclerview.widget.DiffUtil
                    .calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
                        @Override
                        public int getOldListSize() {
                            return reports.size();
                        }

                        @Override
                        public int getNewListSize() {
                            return newReports.size();
                        }

                        @Override
                        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                            AdminReport oldItem = reports.get(oldItemPosition);
                            AdminReport newItem = newReports.get(newItemPosition);
                            return oldItem.getId() != null && newItem.getId() != null
                                    && oldItem.getId().equals(newItem.getId());
                        }

                        @Override
                        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                            AdminReport oldItem = reports.get(oldItemPosition);
                            AdminReport newItem = newReports.get(newItemPosition);
                            return java.util.Objects.equals(oldItem.getTitle(), newItem.getTitle()) &&
                                    java.util.Objects.equals(oldItem.getCategory(), newItem.getCategory()) &&
                                    java.util.Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                                    java.util.Objects.equals(oldItem.getReporterName(), newItem.getReporterName()) &&
                                    java.util.Objects.equals(oldItem.getDisplayId(), newItem.getDisplayId()) &&
                                    oldItem.getTimestamp() == newItem.getTimestamp() &&
                                    java.util.Objects.equals(oldItem.getImageUrls(), newItem.getImageUrls());
                        }
                    });

            this.reports.clear();
            this.reports.addAll(newReports);
            diffResult.dispatchUpdatesTo(this);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDisplayId, tvTitle, tvCategory, tvReporterInfo, tvStatus, tvDate, tvTime;
            ImageView ivIcon, ivReporterAvatar;
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
                tvDate = itemView.findViewById(R.id.tvDate);
                tvTime = itemView.findViewById(R.id.tvTime);
                ivIcon = itemView.findViewById(R.id.ivReportIcon);
                ivReporterAvatar = itemView.findViewById(R.id.ivReporterAvatar);
                viewPagerSlider = itemView.findViewById(R.id.viewPagerSlider);
                tabLayoutIndicator = itemView.findViewById(R.id.tabLayoutIndicator);
                cardStatusBadge = itemView.findViewById(R.id.cardStatusBadge);
            }
        }
    }
}
