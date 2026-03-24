package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
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

public class AdminReportManagementActivity extends AppCompatActivity {

    private RecyclerView rvAdminReports;
    private ProgressBar progressBar;
    private TextView tvEmptyState;
    private TextView tvStatTotal, tvStatPending, tvStatReviewed, tvStatResolved;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;
    private Toolbar toolbar;
    
    private ReportAdapter adapter;
    private List<AdminReport> allReports;
    private List<AdminReport> filteredReports;

    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_report_management);

        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchAndFilter();
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
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
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

    private void fetchReports() {
        progressBar.setVisibility(View.VISIBLE);
        mDatabase.child("AdminReports").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allReports.clear();
                int total = 0, pending = 0, reviewed = 0, resolved = 0;
                
                for (DataSnapshot data : snapshot.getChildren()) {
                    AdminReport report = data.getValue(AdminReport.class);
                    if (report != null) {
                        allReports.add(report);
                        total++;
                        String status = report.getStatus();
                        if ("Pending".equalsIgnoreCase(status)) pending++;
                        else if ("Reviewed".equalsIgnoreCase(status)) reviewed++;
                        else if ("Resolved".equalsIgnoreCase(status)) resolved++;
                    }
                }
                
                tvStatTotal.setText(String.valueOf(total));
                tvStatPending.setText(String.valueOf(pending));
                tvStatReviewed.setText(String.valueOf(reviewed));
                tvStatResolved.setText(String.valueOf(resolved));
                
                Collections.sort(allReports, (o1, o2) -> Long.compare(o2.getCreatedAt(), o1.getCreatedAt()));
                applyFilters();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    private void applyFilters() {
        String query = etSearch.getText().toString().toLowerCase().trim();
        int checkedChipId = chipGroupFilter.getCheckedChipId();
        
        filteredReports.clear();
        for (AdminReport report : allReports) {
            boolean matchesSearch = report.getDisplayId().toLowerCase().contains(query) ||
                    report.getReporterName().toLowerCase().contains(query) ||
                    (report.getRelatedReportId() != null && report.getRelatedReportId().toLowerCase().contains(query)) ||
                    report.getTitle().toLowerCase().contains(query);
            
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
            holder.tvDisplayId.setText(report.getDisplayId() != null ? report.getDisplayId() : "N/A");
            holder.tvTitle.setText(report.getTitle());
            holder.tvCategory.setText("Category: " + report.getCategory());
            holder.tvReporterInfo.setText("Submitted By: " + report.getReporterName() + " (" + report.getUniversityId() + ")");
            holder.tvRelatedItemId.setText("Related Item: " + (report.getRelatedReportId() != null ? report.getRelatedReportId() : "None"));
            holder.tvStatus.setText(report.getStatus());
            holder.tvPriority.setText(report.getPriority());

            // Status Color Coding
            int statusColor;
            String status = report.getStatus();
            if ("Pending".equalsIgnoreCase(status)) statusColor = ContextCompat.getColor(AdminReportManagementActivity.this, R.color.textSecondary);
            else if ("Reviewed".equalsIgnoreCase(status)) statusColor = ContextCompat.getColor(AdminReportManagementActivity.this, R.color.primaryColor);
            else statusColor = ContextCompat.getColor(AdminReportManagementActivity.this, R.color.success);
            holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor));

            // Priority Color Coding
            int priorityColor;
            String priority = report.getPriority();
            if ("High".equalsIgnoreCase(priority)) priorityColor = ContextCompat.getColor(AdminReportManagementActivity.this, R.color.statusLost);
            else if ("Medium".equalsIgnoreCase(priority)) priorityColor = 0xFFFBC02D; // Yellow/Amber
            else priorityColor = ContextCompat.getColor(AdminReportManagementActivity.this, R.color.statusFound);
            holder.tvPriority.setBackgroundTintList(android.content.res.ColorStateList.valueOf(priorityColor));

            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
            holder.tvTimestamp.setText(sdf.format(new Date(report.getCreatedAt())));

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(AdminReportManagementActivity.this, AdminReportDetailsActivity.class);
                intent.putExtra("reportId", report.getReportId());
                startActivity(intent);
            });
            
            holder.btnViewDetails.setOnClickListener(v -> {
                Intent intent = new Intent(AdminReportManagementActivity.this, AdminReportDetailsActivity.class);
                intent.putExtra("reportId", report.getReportId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return reports.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvDisplayId, tvTitle, tvCategory, tvReporterInfo, tvRelatedItemId, tvStatus, tvPriority, tvTimestamp;
            MaterialButton btnViewDetails;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvDisplayId = itemView.findViewById(R.id.tvDisplayId);
                tvTitle = itemView.findViewById(R.id.tvReportTitle);
                tvCategory = itemView.findViewById(R.id.tvCategory);
                tvReporterInfo = itemView.findViewById(R.id.tvReporterInfo);
                tvRelatedItemId = itemView.findViewById(R.id.tvRelatedItemId);
                tvStatus = itemView.findViewById(R.id.tvStatusBadge);
                tvPriority = itemView.findViewById(R.id.tvPriorityBadge);
                tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
                btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            }
        }
    }
}
