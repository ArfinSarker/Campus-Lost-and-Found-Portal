package com.sas.lostandfound;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class AllItemsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecentItemsActivity adapter;
    private List<ItemActivity> allItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_items);

        recyclerView = findViewById(R.id.recyclerViewAllItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load all items (expand the sample list)
        allItems = new ArrayList<>();
        allItems.add(new ItemActivity(1, "MacBook Pro", "Library, 2nd floor", "2h ago", "lost", "📂"));
        allItems.add(new ItemActivity(2, "Found", "Cafeteria", "5h ago", "found", "📂"));
        allItems.add(new ItemActivity(3, "AirPods Pro", "Building A, Room 301", "1d ago", "lost", "📂"));
        allItems.add(new ItemActivity(4, "Water Bottle", "Gym", "3d ago", "lost", "📂"));
        allItems.add(new ItemActivity(5, "Keys", "Parking Lot", "1w ago", "found", "📂"));
        allItems.add(new ItemActivity(6, "Backpack", "Student Union", "2d ago", "lost", "📂"));
        allItems.add(new ItemActivity(7, "Calculator", "Math Building", "5d ago", "found", "📂"));
        // Add more as needed

        adapter = new RecentItemsActivity(allItems, new RecentItemsActivity.OnItemClickListener() {
            @Override
            public void onItemClick(ItemActivity item) {
                Toast.makeText(AllItemsActivity.this, "Opening " + item.getTitle(), Toast.LENGTH_SHORT).show();
                // TODO: navigate to item detail activity
            }
        });
        recyclerView.setAdapter(adapter);
    }
}