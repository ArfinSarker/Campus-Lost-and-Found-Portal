package com.sas.lostandfound;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BrowseItemsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private final List<Item> allItems = new ArrayList<>();
    private final List<Item> lostItemsList = new ArrayList<>();
    private final List<Item> foundItemsList = new ArrayList<>();
    private final List<Item> filteredItems = new ArrayList<>();

    private TextInputEditText etSearch;
    private AutoCompleteTextView actvCategoryFilter;
    private ChipGroup chipGroup;
    private View layoutEmptyState;
    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean isFetching = false;

    private String currentSearchQuery = "";
    private String currentCategory = "All Categories";
    private int currentStatusFilterId = R.id.chipAll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_items);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupFilters();
        setupSwipeRefresh();

        // Ensure back press always exits the activity immediately
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        loadItems();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rvItems);
        etSearch = findViewById(R.id.etSearch);
        actvCategoryFilter = findViewById(R.id.actvCategoryFilter);
        chipGroup = findViewById(R.id.chipGroup);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        toolbar = findViewById(R.id.toolbar);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationOnClickListener(v -> finish());

            com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
            if (appBarLayout != null) {
                int headerColor = androidx.core.content.ContextCompat.getColor(this, R.color.search_header_bg);
                boolean isNightMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                HeaderColorHelper.setup(this, appBarLayout, headerColor, headerColor, !isNightMode);
            }
        }
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // Using centralized navigation utility
        adapter = new ItemAdapter(filteredItems, R.layout.item_browse_card, item -> {
            ItemNavigationUtils.navigateToDetail(BrowseItemsActivity.this, item);
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupFilters() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().trim().toLowerCase();
                filterItems();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        String[] categories = { "All Categories", "Electronics & Gadgets", "ID Cards", "Wallets & Purses",
                "Bank/Credit Cards", "Bags", "Study Materials", "Eyewear", "Keys & Access Devices",
                "Clothing & Accessories", "Others" };
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, R.layout.search_dropdown_item, categories);
        actvCategoryFilter.setAdapter(categoryAdapter);
        actvCategoryFilter.setText(categories[0], false);
        actvCategoryFilter.setOnItemClickListener((parent, view, position, id) -> {
            currentCategory = categories[position];
            filterItems();
        });

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (!checkedIds.isEmpty()) {
                currentStatusFilterId = checkedIds.get(0);
                filterItems();
            } else {
                chipGroup.check(R.id.chipAll);
            }
        });
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(this::loadItems);
        }
    }

    private void loadItems() {
        if (isFetching)
            return;
        isFetching = true;

        // Fetch Lost Items
        SupabaseDatabaseHelper.select("lost_reports", "deleted_by_user=eq.false", new TypeToken<List<Item>>() {
        }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> lostItems) {
                // Fetch Found Items
                SupabaseDatabaseHelper.select("found_reports", "deleted_by_user=eq.false", new TypeToken<List<Item>>() {
                }.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
                    @Override
                    public void onSuccess(List<Item> foundItems) {
                        lostItemsList.clear();
                        if (lostItems != null)
                            lostItemsList.addAll(lostItems);

                        foundItemsList.clear();
                        if (foundItems != null)
                            foundItemsList.addAll(foundItems);

                        combineAndFilter();
                        isFetching = false;
                        if (swipeRefreshLayout != null)
                            swipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        isFetching = false;
                        if (!isFinishing()) {
                            SnackbarManager.show(SnackbarManager.Type.ERROR, "FoundReports Error: " + errorMessage);
                        }
                        if (swipeRefreshLayout != null)
                            swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                isFetching = false;
                if (!isFinishing()) {
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "LostReports Error: " + errorMessage);
                }
                if (swipeRefreshLayout != null)
                    swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private synchronized void combineAndFilter() {
        allItems.clear();
        allItems.addAll(lostItemsList);
        allItems.addAll(foundItemsList);
        Collections.sort(allItems, (o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));
        filterItems();
    }

    private void filterItems() {
        List<Item> temp = new ArrayList<>();
        for (Item item : allItems) {
            String name = item.getName() != null ? item.getName().toLowerCase() : "";
            String desc = item.getDescription() != null ? item.getDescription().toLowerCase() : "";
            String displayId = item.getDisplayId() != null ? item.getDisplayId().toLowerCase() : "";
            String formattedId = ("#" + displayId);

            boolean matchesSearch = currentSearchQuery.isEmpty() ||
                    name.contains(currentSearchQuery) ||
                    desc.contains(currentSearchQuery) ||
                    displayId.contains(currentSearchQuery) ||
                    formattedId.contains(currentSearchQuery);

            boolean matchesCategory = currentCategory.equals("All Categories") ||
                    (item.getCategory() != null && item.getCategory().equals(currentCategory));

            boolean matchesStatus = false;
            String adminStatus = item.getAdminStatus();
            boolean isClaimed = "Claimed".equalsIgnoreCase(adminStatus) || "Returned".equalsIgnoreCase(adminStatus);

            if (currentStatusFilterId == R.id.chipAll) {
                matchesStatus = true;
            } else if (currentStatusFilterId == R.id.chipLost) {
                matchesStatus = "lost".equalsIgnoreCase(item.getStatus()) && !isClaimed;
            } else if (currentStatusFilterId == R.id.chipFound) {
                matchesStatus = "found".equalsIgnoreCase(item.getStatus()) && !isClaimed;
            } else if (currentStatusFilterId == R.id.chipClaimed) {
                matchesStatus = isClaimed;
            }

            if (matchesSearch && matchesCategory && matchesStatus) {
                temp.add(item);
            }
        }
        adapter.updateItems(temp);

        if (filteredItems.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
