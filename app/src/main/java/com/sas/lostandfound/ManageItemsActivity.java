package com.sas.lostandfound;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ManageItemsActivity extends AppCompatActivity implements ItemAdapter.OnItemClickListener {

    private RecyclerView rvItems;
    private ProgressBar progressBar;
    private View llEmptyState;
    private TextView tvStatAll, tvStatLost, tvStatFound, tvStatResolved;
    private MaterialCardView cardAll, cardLost, cardFound, cardResolved;
    private TextInputEditText etSearch;
    private ChipGroup chipGroupFilter;
    private Toolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;

    private ItemAdapter adapter;
    private List<Item> allItems = new ArrayList<>();
    private List<Item> filteredList = new ArrayList<>();
    private boolean isFetching = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RoleVerifier.checkAdminAccess(this);
        setContentView(R.layout.activity_manage_items);

        initializeViews();
        setupToolbar();
        setupRecyclerView();
        setupSearchAndFilter();
        setupSwipeRefresh();

        // Handle initial filter from intent
        String initialFilter = getIntent().getStringExtra("initialFilter");
        if (initialFilter != null) {
            if ("lost".equalsIgnoreCase(initialFilter)) chipGroupFilter.check(R.id.chipLost);
            else if ("found".equalsIgnoreCase(initialFilter)) chipGroupFilter.check(R.id.chipFound);
            else if ("resolved".equalsIgnoreCase(initialFilter)) chipGroupFilter.check(R.id.chipResolved);
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        fetchData();
    }

    @Override
    public void onItemClick(Item item) {
        ItemNavigationUtils.navigateToDetail(this, item, true);
    }

    private void initializeViews() {
        rvItems = findViewById(R.id.rvItems);
        progressBar = findViewById(R.id.progressBar);
        llEmptyState = findViewById(R.id.llEmptyState);
        tvStatAll = findViewById(R.id.tvStatAll);
        tvStatLost = findViewById(R.id.tvStatLost);
        tvStatFound = findViewById(R.id.tvStatFound);
        tvStatResolved = findViewById(R.id.tvStatResolved);
        cardAll = findViewById(R.id.cardAll);
        cardLost = findViewById(R.id.cardLost);
        cardFound = findViewById(R.id.cardFound);
        cardResolved = findViewById(R.id.cardResolved);
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
            toolbar.setNavigationOnClickListener(v -> finish());
            
            com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
            if (appBarLayout != null) {
                HeaderColorHelper.setup(this, appBarLayout, toolbar);
            }
        }
    }

    private void setupRecyclerView() {
        adapter = new ItemAdapter(filteredList, R.layout.item_manage_row, this, true); // true for admin mode
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(adapter);
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

        cardAll.setOnClickListener(v -> chipGroupFilter.check(R.id.chipAll));
        cardLost.setOnClickListener(v -> chipGroupFilter.check(R.id.chipLost));
        cardFound.setOnClickListener(v -> chipGroupFilter.check(R.id.chipFound));
        cardResolved.setOnClickListener(v -> chipGroupFilter.check(R.id.chipResolved));
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
        swipeRefreshLayout.setOnRefreshListener(this::fetchData);
    }

    private void fetchData() {
        if (isFetching) return;
        isFetching = true;
        progressBar.setVisibility(View.VISIBLE);
        llEmptyState.setVisibility(View.GONE);

        final List<Item> lostItems = new ArrayList<>();
        final List<Item> foundItems = new ArrayList<>();
        final boolean[] tasksCompleted = {false, false};

        // Parallel Fetch
        SupabaseDatabaseHelper.select("lost_reports", "select=*", new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> items) {
                if (items != null) lostItems.addAll(items);
                checkTasks(tasksCompleted, lostItems, foundItems);
            }
            @Override
            public void onFailure(String errorMessage) {
                checkTasks(tasksCompleted, lostItems, foundItems);
            }
        });

        SupabaseDatabaseHelper.select("found_reports", "select=*", new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> items) {
                if (items != null) foundItems.addAll(items);
                checkTasks(tasksCompleted, lostItems, foundItems);
            }
            @Override
            public void onFailure(String errorMessage) {
                checkTasks(tasksCompleted, lostItems, foundItems);
            }
        });
    }

    private synchronized void checkTasks(boolean[] tasks, List<Item> lost, List<Item> found) {
        if (!tasks[0]) {
            tasks[0] = true;
        } else {
            finalizeData(lost, found);
        }
    }

    private void finalizeData(List<Item> lost, List<Item> found) {
        allItems.clear();
        allItems.addAll(lost);
        
        // Deduplicate and merge
        for (Item f : found) {
            boolean exists = false;
            for (Item l : lost) {
                if (l.getId().equals(f.getId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) allItems.add(f);
        }

        Collections.sort(allItems, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
        
        updateStats(allItems);
        applyFilters();

        isFetching = false;
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void updateStats(List<Item> items) {
        int all = items.size();
        int lost = 0;
        int found = 0;
        int resolved = 0;

        for (Item item : items) {
            String adminStatus = item.getAdminStatus();
            boolean isResolved = "Returned".equalsIgnoreCase(adminStatus) || "Claimed".equalsIgnoreCase(adminStatus);
            
            if (isResolved) {
                resolved++;
            } else {
                if ("lost".equalsIgnoreCase(item.getStatus())) lost++;
                else if ("found".equalsIgnoreCase(item.getStatus())) found++;
            }
        }

        tvStatAll.setText(String.valueOf(all));
        tvStatLost.setText(String.valueOf(lost));
        tvStatFound.setText(String.valueOf(found));
        tvStatResolved.setText(String.valueOf(resolved));
    }

    private void applyFilters() {
        String query = etSearch.getText().toString().toLowerCase().trim();
        int checkedChipId = chipGroupFilter.getCheckedChipId();

        filteredList.clear();
        for (Item item : allItems) {
            String name = item.getName() != null ? item.getName().toLowerCase() : "";
            String userId = item.getUserId() != null ? item.getUserId().toLowerCase() : "";
            String displayId = item.getDisplayId() != null ? item.getDisplayId().toLowerCase() : "";

            boolean matchesSearch = name.contains(query) ||
                    userId.contains(query) ||
                    displayId.contains(query);

            boolean matchesStatus = true;
            String adminStatus = item.getAdminStatus();
            boolean isResolved = "Returned".equalsIgnoreCase(adminStatus) || "Claimed".equalsIgnoreCase(adminStatus);

            if (checkedChipId == R.id.chipLost) {
                matchesStatus = "lost".equalsIgnoreCase(item.getStatus()) && !isResolved;
            } else if (checkedChipId == R.id.chipFound) {
                matchesStatus = "found".equalsIgnoreCase(item.getStatus()) && !isResolved;
            } else if (checkedChipId == R.id.chipResolved) {
                matchesStatus = isResolved;
            }

            if (matchesSearch && matchesStatus) {
                filteredList.add(item);
            }
        }

        adapter.notifyDataSetChanged();
        llEmptyState.setVisibility(filteredList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
