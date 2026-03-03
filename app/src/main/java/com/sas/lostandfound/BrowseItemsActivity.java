package com.sas.lostandfound;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class BrowseItemsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ItemAdapter adapter;
    private List<Item> allItems;
    private DatabaseReference mDatabase;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_items);

        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();
        recyclerView = findViewById(R.id.rvItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        allItems = new ArrayList<>();
        adapter = new ItemAdapter(allItems, item ->
                Toast.makeText(BrowseItemsActivity.this, "Opening " + item.getName(), Toast.LENGTH_SHORT).show()
        );
        recyclerView.setAdapter(adapter);

        loadItems();
    }

    private void loadItems() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allItems.clear();
                
                // Load Lost Items
                DataSnapshot lostSnapshot = snapshot.child("LostItems");
                for (DataSnapshot dataSnapshot : lostSnapshot.getChildren()) {
                    Item item = dataSnapshot.getValue(Item.class);
                    if (item != null) {
                        allItems.add(item);
                    }
                }
                
                // Load Found Items
                DataSnapshot foundSnapshot = snapshot.child("FoundItems");
                for (DataSnapshot dataSnapshot : foundSnapshot.getChildren()) {
                    Item item = dataSnapshot.getValue(Item.class);
                    if (item != null) {
                        allItems.add(item);
                    }
                }
                
                // Sort by timestamp if available
                allItems.sort((o1, o2) -> Long.compare(o2.getTimestamp(), o1.getTimestamp()));

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BrowseItemsActivity.this, "Failed to load items", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
