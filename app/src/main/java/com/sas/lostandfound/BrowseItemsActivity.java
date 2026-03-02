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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_items);

        mDatabase = FirebaseDatabase.getInstance().getReference("LostItems");
        recyclerView = findViewById(R.id.recyclerViewAllItems);
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
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Item item = dataSnapshot.getValue(Item.class);
                    if (item != null) {
                        allItems.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BrowseItemsActivity.this, "Failed to load items", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
