package com.sas.lostandfound;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private Button btnReportLost, btnReportFound;
    private TextView tvBrowseAll;
    private RecyclerView recyclerView;
    private RecentItemsActivity adapter;
    private List<ItemActivity> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnReportLost = findViewById(R.id.btnReportLost);
        btnReportFound = findViewById(R.id.btnReportFound);
        tvBrowseAll = findViewById(R.id.tvBrowseAll);
        recyclerView = findViewById(R.id.recyclerViewRecent);

        // Sample data – now using the correct 6‑parameter constructor
        itemList = new ArrayList<>();
        // Parameters: (id, title, location, timeAgo, status, imageUrl)
        itemList.add(new ItemActivity(1, "MacBook Pro", "Library, 2nd floor", "2h ago", "lost", "📂"));
        itemList.add(new ItemActivity(2, "Found", "Cafeteria", "5h ago", "found", "📂"));
        itemList.add(new ItemActivity(3, "AirPods Pro", "Building A, Room 301", "1d ago", "lost", "📂"));

        adapter = new RecentItemsActivity(itemList, new RecentItemsActivity.OnItemClickListener() {
            @Override
            public void onItemClick(ItemActivity item) {
                if (!isLoggedIn()) {
                    Toast.makeText(HomeActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                } else {
                    // Use correct getter: getTitle() instead of getName()
                    Toast.makeText(HomeActivity.this, "Opening " + item.getTitle(), Toast.LENGTH_SHORT).show();
                    // TODO: Navigate to item details
                }
            }
        });
        recyclerView.setAdapter(adapter);

        btnReportLost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoggedIn()) {
                    Toast.makeText(HomeActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                } else {
                    Toast.makeText(HomeActivity.this, "Report Lost Item (requires login)", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnReportFound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoggedIn()) {
                    Toast.makeText(HomeActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                } else {
                    Toast.makeText(HomeActivity.this, "Report Found Item (requires login)", Toast.LENGTH_SHORT).show();
                }
            }
        });

        tvBrowseAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isLoggedIn()) {
                    Toast.makeText(HomeActivity.this, "Please login first", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(HomeActivity.this, LoginActivity.class));
                } else {
                    Toast.makeText(HomeActivity.this, "Browse all items (requires login)", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean isLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        return prefs.getBoolean("isLoggedIn", false);
    }
}