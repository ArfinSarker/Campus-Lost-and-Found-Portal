package com.sas.lostandfound;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ItemDetailActivity extends AppCompatActivity {

    private ImageView ivItemImage, ivUserPhoto;
    private TextView tvStatus, tvItemName, tvCategory, tvDescription, tvLocation, tvDateTime, tvReporterName, tvReporterDept;
    private TextView tvHeaderTitle, tvHeaderSubtitle;
    private LinearLayout headerTitleContainer;
    private MaterialCardView cardBadge;
    private MaterialButton btnContact, btnClaim;
    private Toolbar toolbar;
    private AppBarLayout appBarLayout;
    
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initializeViews();
        setupToolbar();
        setupScrollBehavior();
        
        String itemId = getIntent().getStringExtra("itemId");
        String itemName = getIntent().getStringExtra("itemName");
        String itemDescription = getIntent().getStringExtra("itemDescription");
        String itemLocation = getIntent().getStringExtra("itemLocation");
        String itemDate = getIntent().getStringExtra("itemDate");
        String itemTime = getIntent().getStringExtra("itemTime");
        String itemStatus = getIntent().getStringExtra("itemStatus");
        String itemCategory = getIntent().getStringExtra("itemCategory");
        String itemImageUrl = getIntent().getStringExtra("itemImageUrl");
        String userName = getIntent().getStringExtra("userName");
        String userDept = getIntent().getStringExtra("userDepartment");
        String userPhone = getIntent().getStringExtra("userPhone");
        String reporterId = getIntent().getStringExtra("userId");

        String fullDateTime = itemDate;
        if (itemTime != null && !itemTime.isEmpty()) {
            fullDateTime += " - " + itemTime;
        }

        displayItemDetails(itemName, itemDescription, itemLocation, fullDateTime, itemStatus, itemCategory, itemImageUrl, userName, userDept, userPhone);
        
        btnClaim.setOnClickListener(v -> handleClaim(itemId, itemName, itemStatus, reporterId));
        
        checkIfAlreadyClaimed(itemId, reporterId);
    }

    private void initializeViews() {
        ivItemImage = findViewById(R.id.ivItemImage);
        ivUserPhoto = findViewById(R.id.ivUserPhoto);
        tvStatus = findViewById(R.id.tvStatus);
        tvItemName = findViewById(R.id.tvItemName);
        tvCategory = findViewById(R.id.tvCategory);
        tvDescription = findViewById(R.id.tvDescription);
        tvLocation = findViewById(R.id.tvLocation);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvReporterName = findViewById(R.id.tvReporterName);
        tvReporterDept = findViewById(R.id.tvReporterDept);
        cardBadge = findViewById(R.id.cardBadge);
        btnContact = findViewById(R.id.btnContact);
        btnClaim = findViewById(R.id.btnClaim);
        toolbar = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.appBarLayout);
        headerTitleContainer = findViewById(R.id.headerTitleContainer);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupScrollBehavior() {
        appBarLayout.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            float totalScrollRange = appBarLayout.getTotalScrollRange();
            float percentage = (float) Math.abs(verticalOffset) / totalScrollRange;
            
            // Show header title container only when collapsed (percentage close to 1)
            if (percentage > 0.8f) {
                float alpha = (percentage - 0.8f) / 0.2f;
                headerTitleContainer.setAlpha(alpha);
                headerTitleContainer.setVisibility(View.VISIBLE);
            } else {
                headerTitleContainer.setAlpha(0);
                headerTitleContainer.setVisibility(View.GONE);
            }
        });
    }

    private void displayItemDetails(String name, String desc, String loc, String dateTime, String status, String category, String imageUrl, String uName, String uDept, String uPhone) {
        tvItemName.setText(name);
        tvDescription.setText(desc != null && !desc.isEmpty() ? desc : "No description provided.");
        tvLocation.setText(loc);
        tvDateTime.setText(dateTime);
        tvCategory.setText(category);
        tvReporterName.setText(uName != null ? uName : "Anonymous");
        tvReporterDept.setText(uDept != null ? uDept : "Department not specified");

        if ("lost".equalsIgnoreCase(status)) {
            tvStatus.setText("LOST");
            cardBadge.setCardBackgroundColor(0xFFFEE2E2);
            tvStatus.setTextColor(0xFFE53935);
            btnClaim.setText("I Found This Item");
            tvHeaderTitle.setText("Lost Item");
        } else {
            tvStatus.setText("FOUND");
            cardBadge.setCardBackgroundColor(0xFFDCFCE7);
            tvStatus.setTextColor(0xFF2E7D32);
            btnClaim.setText("This is Mine");
            tvHeaderTitle.setText("Found Item");
        }
        
        tvHeaderSubtitle.setText(category + " • " + loc);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_package)
                    .centerCrop()
                    .into(ivItemImage);
        }

        btnContact.setOnClickListener(v -> {
            if (uPhone != null && !uPhone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + uPhone));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Contact number not available.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleClaim(String itemId, String itemName, String itemStatus, String reporterId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please login to claim items", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser.getUid().equals(reporterId)) {
            Toast.makeText(this, "You cannot claim your own item", Toast.LENGTH_SHORT).show();
            return;
        }

        btnClaim.setEnabled(false);
        btnClaim.setText("Sending...");

        mDatabase.child("Users").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String senderName = snapshot.child("name").getValue(String.class);
                    String senderPhone = snapshot.child("phone").getValue(String.class);
                    String senderEmail = snapshot.child("email").getValue(String.class);

                    String notificationId = mDatabase.child("Notifications").child(reporterId).push().getKey();
                    String message = "lost".equalsIgnoreCase(itemStatus) ? 
                            "Someone has claimed that they found your lost item: " + itemName :
                            "Someone has claimed this item as theirs: " + itemName;
                    String type = "lost".equalsIgnoreCase(itemStatus) ? "lost_claim" : "found_claim";

                    Notification notification = new Notification(
                            notificationId,
                            reporterId,
                            currentUser.getUid(),
                            senderName,
                            senderPhone,
                            senderEmail,
                            itemId,
                            itemName,
                            message,
                            System.currentTimeMillis(),
                            type,
                            ""
                    );

                    if (notificationId != null) {
                        mDatabase.child("Notifications").child(reporterId).child(notificationId).setValue(notification)
                                .addOnSuccessListener(aVoid -> {
                                    // Record the claim in a separate node to prevent duplicates
                                    mDatabase.child("ItemClaims").child(itemId).child(currentUser.getUid()).setValue(System.currentTimeMillis());
                                    
                                    Toast.makeText(ItemDetailActivity.this, "Claim request sent!", Toast.LENGTH_SHORT).show();
                                    btnClaim.setText("Claim Sent");
                                })
                                .addOnFailureListener(e -> {
                                    btnClaim.setEnabled(true);
                                    btnClaim.setText("Retry Claim");
                                    Toast.makeText(ItemDetailActivity.this, "Failed to send claim request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                } else {
                    btnClaim.setEnabled(true);
                    btnClaim.setText("Try Again");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                btnClaim.setEnabled(true);
                btnClaim.setText("Try Again");
            }
        });
    }

    private void checkIfAlreadyClaimed(String itemId, String reporterId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || itemId == null) return;

        if (currentUser.getUid().equals(reporterId)) {
            btnClaim.setVisibility(View.GONE);
            return;
        }

        mDatabase.child("ItemClaims").child(itemId).child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    btnClaim.setEnabled(false);
                    btnClaim.setText("Claim Sent");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
