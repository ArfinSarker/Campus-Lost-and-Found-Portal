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
    private TextView tvStatus, tvItemName, tvCategory, tvDescription, tvLocation, tvDateTime, tvReporterName, tvReporterType, tvReporterDeptOrDesignation;
    private TextView tvHeaderTitle, tvHeaderSubtitle;
    private TextView tvProofOwnership, tvHandlingStatus, tvSecurityQuestion;
    private LinearLayout headerTitleContainer, llLostSpecifics, llFoundSpecifics;
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
        String itemStatus = getIntent().getStringExtra("itemStatus");
        String reporterId = getIntent().getStringExtra("userId");

        // First display what we have from Intent
        displayInitialData();
        
        // Fetch full details from database to get specific fields (Ownership, Handling, Security, Location)
        fetchItemDetails(itemId, itemStatus);
        
        // Fetch reporter's profile details
        if (reporterId != null) {
            fetchReporterProfile(reporterId);
        }
        
        btnClaim.setOnClickListener(v -> handleClaim(itemId, getIntent().getStringExtra("itemName"), itemStatus, reporterId));
        
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
        tvReporterType = findViewById(R.id.tvReporterType);
        tvReporterDeptOrDesignation = findViewById(R.id.tvReporterDeptOrDesignation);
        
        tvProofOwnership = findViewById(R.id.tvProofOwnership);
        tvHandlingStatus = findViewById(R.id.tvHandlingStatus);
        tvSecurityQuestion = findViewById(R.id.tvSecurityQuestion);
        llLostSpecifics = findViewById(R.id.llLostSpecifics);
        llFoundSpecifics = findViewById(R.id.llFoundSpecifics);
        
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

    private void displayInitialData() {
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

        String fullDateTime = itemDate;
        if (itemTime != null && !itemTime.isEmpty()) {
            fullDateTime += " - " + itemTime;
        }

        displayItemDetails(itemName, itemDescription, itemLocation, fullDateTime, itemStatus, itemCategory, itemImageUrl, userName, userDept, userPhone);
    }

    private void fetchItemDetails(String itemId, String status) {
        String path = "lost".equalsIgnoreCase(status) ? "LostItems" : "FoundItems";
        mDatabase.child(path).child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Item item = snapshot.getValue(Item.class);
                if (item != null) {
                    // Update Location with additional details
                    StringBuilder locationBuilder = new StringBuilder(item.getLocation());
                    if (item.getAdditionalLocationDetails() != null && !item.getAdditionalLocationDetails().isEmpty()) {
                        locationBuilder.append("\n").append(item.getAdditionalLocationDetails());
                    }
                    tvLocation.setText(locationBuilder.toString());

                    if ("lost".equalsIgnoreCase(status)) {
                        llLostSpecifics.setVisibility(View.VISIBLE);
                        llFoundSpecifics.setVisibility(View.GONE);
                        tvProofOwnership.setText(item.getProofOfOwnershipDetail() != null && !item.getProofOfOwnershipDetail().isEmpty() 
                                ? item.getProofOfOwnershipDetail() : "No details provided.");
                    } else {
                        llLostSpecifics.setVisibility(View.GONE);
                        llFoundSpecifics.setVisibility(View.VISIBLE);
                        
                        // Handling Status with full details (Type, Authority Name, Office Room)
                        StringBuilder handlingBuilder = new StringBuilder();
                        if (item.getItemHandlingStatus() != null && !item.getItemHandlingStatus().isEmpty()) {
                            handlingBuilder.append(item.getItemHandlingStatus());
                        }
                        if (item.getAuthorityName() != null && !item.getAuthorityName().isEmpty()) {
                            if (handlingBuilder.length() > 0) handlingBuilder.append("\n");
                            handlingBuilder.append("Authority/Person: ").append(item.getAuthorityName());
                        }
                        if (item.getOfficeRoomNumber() != null && !item.getOfficeRoomNumber().isEmpty()) {
                            if (handlingBuilder.length() > 0) handlingBuilder.append("\n");
                            handlingBuilder.append("Office/Room: ").append(item.getOfficeRoomNumber());
                        }
                        
                        String handlingStatusText = handlingBuilder.toString();
                        tvHandlingStatus.setText(!handlingStatusText.isEmpty() ? handlingStatusText : "Status not provided.");
                        
                        tvSecurityQuestion.setText(item.getHiddenIdentificationQuestion() != null && !item.getHiddenIdentificationQuestion().isEmpty() 
                                ? item.getHiddenIdentificationQuestion() : "No security question provided.");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchReporterProfile(String reporterId) {
        // Resolve University ID first to support old items
        mDatabase.child("UIDToUniversityID").child(reporterId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String resolvedId = snapshot.exists() ? snapshot.getValue(String.class) : reporterId;
                
                mDatabase.child("Users").child(resolvedId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                        if (userSnapshot.exists()) {
                            User user = userSnapshot.getValue(User.class);
                            if (user != null) {
                                tvReporterName.setText(user.getName());
                                tvReporterType.setText(user.getUserType());
                                
                                if ("Staff".equalsIgnoreCase(user.getUserType())) {
                                    tvReporterDeptOrDesignation.setText(user.getDesignation());
                                    tvReporterDeptOrDesignation.setVisibility(View.VISIBLE);
                                } else if ("Student".equalsIgnoreCase(user.getUserType())) {
                                    tvReporterDeptOrDesignation.setText(user.getDepartment());
                                    tvReporterDeptOrDesignation.setVisibility(View.VISIBLE);
                                } else {
                                    tvReporterDeptOrDesignation.setVisibility(View.GONE);
                                }

                                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                    Glide.with(ItemDetailActivity.this)
                                            .load(user.getProfileImageUrl())
                                            .placeholder(R.drawable.ic_user)
                                            .circleCrop()
                                            .into(ivUserPhoto);
                                } else {
                                    ivUserPhoto.setImageResource(R.drawable.ic_user);
                                }
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayItemDetails(String name, String desc, String loc, String dateTime, String status, String category, String imageUrl, String uName, String uDept, String uPhone) {
        tvItemName.setText(name);
        tvDescription.setText(desc != null && !desc.isEmpty() ? desc : "No description provided.");
        tvLocation.setText(loc);
        tvDateTime.setText(dateTime);
        tvCategory.setText(category);
        tvReporterName.setText(uName != null ? uName : "Anonymous");
        tvReporterDeptOrDesignation.setText(uDept != null ? uDept : "Department not specified");

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
        FirebaseUser authUser = mAuth.getCurrentUser();
        if (authUser == null) {
            Toast.makeText(this, "Please login to claim items", Toast.LENGTH_SHORT).show();
            return;
        }

        if (reporterId == null) {
            Toast.makeText(this, "Error: Reporter not identified", Toast.LENGTH_SHORT).show();
            return;
        }

        btnClaim.setEnabled(false);
        btnClaim.setText("Sending...");

        // Start chain of resolutions to ensure University ID is used
        mDatabase.child("UIDToUniversityID").child(authUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot senderMappingSnapshot) {
                final String senderUnivId = senderMappingSnapshot.exists() ? senderMappingSnapshot.getValue(String.class) : authUser.getUid();
                
                mDatabase.child("UIDToUniversityID").child(reporterId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot recipientMappingSnapshot) {
                        final String recipientUnivId = recipientMappingSnapshot.exists() ? recipientMappingSnapshot.getValue(String.class) : reporterId;

                        if (senderUnivId.equals(recipientUnivId)) {
                            Toast.makeText(ItemDetailActivity.this, "You cannot claim your own item", Toast.LENGTH_SHORT).show();
                            resetClaimButton(itemStatus);
                            return;
                        }

                        // Get Sender details for the notification object
                        mDatabase.child("Users").child(senderUnivId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot userSnap) {
                                if (userSnap.exists()) {
                                    proceedToCreateNotification(userSnap, recipientUnivId, senderUnivId, itemId, itemName, itemStatus);
                                } else {
                                    // Fallback to check if user is stored under Auth UID
                                    mDatabase.child("Users").child(authUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot authSnap) {
                                            if (authSnap.exists()) {
                                                proceedToCreateNotification(authSnap, recipientUnivId, senderUnivId, itemId, itemName, itemStatus);
                                            } else {
                                                Toast.makeText(ItemDetailActivity.this, "Could not load your profile details", Toast.LENGTH_SHORT).show();
                                                resetClaimButton(itemStatus);
                                            }
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError error) { resetClaimButton(itemStatus); }
                                    });
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) { resetClaimButton(itemStatus); }
                        });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { resetClaimButton(itemStatus); }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { resetClaimButton(itemStatus); }
        });
    }

    private void proceedToCreateNotification(DataSnapshot senderSnap, String recipientUnivId, String senderUnivId, String itemId, String itemName, String itemStatus) {
        String senderName = senderSnap.child("name").getValue(String.class);
        if (senderName == null) senderName = senderSnap.child("fullName").getValue(String.class);
        String senderPhone = senderSnap.child("phone").getValue(String.class);
        String senderEmail = senderSnap.child("email").getValue(String.class);

        String notificationId = mDatabase.child("Notifications").child(recipientUnivId).push().getKey();
        if (notificationId == null) {
            resetClaimButton(itemStatus);
            return;
        }

        String message = "lost".equalsIgnoreCase(itemStatus) ? 
                "Someone has claimed that they found your lost item: " + itemName :
                "Someone has claimed this item as theirs: " + itemName;
        String type = "lost".equalsIgnoreCase(itemStatus) ? "lost_claim" : "found_claim";

        Notification notification = new Notification(
                notificationId,
                recipientUnivId,
                senderUnivId,
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

        mDatabase.child("Notifications").child(recipientUnivId).child(notificationId).setValue(notification)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Store record of claim
                        mDatabase.child("ItemClaims").child(itemId).child(senderUnivId).setValue(System.currentTimeMillis());
                        Toast.makeText(ItemDetailActivity.this, "Claim request sent!", Toast.LENGTH_SHORT).show();
                        btnClaim.setText("Claim Sent");
                        btnClaim.setEnabled(false);
                    } else {
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(ItemDetailActivity.this, "Failed to send: " + error, Toast.LENGTH_SHORT).show();
                        resetClaimButton(itemStatus);
                    }
                });
    }

    private void resetClaimButton(String itemStatus) {
        btnClaim.setEnabled(true);
        btnClaim.setText("lost".equalsIgnoreCase(itemStatus) ? "I Found This Item" : "This is Mine");
    }

    private void checkIfAlreadyClaimed(String itemId, String reporterId) {
        FirebaseUser authUser = mAuth.getCurrentUser();
        if (authUser == null || itemId == null) return;

        mDatabase.child("UIDToUniversityID").child(authUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final String senderUnivId = snapshot.exists() ? snapshot.getValue(String.class) : authUser.getUid();
                
                // Also resolve reporterId to University ID for accurate comparison
                mDatabase.child("UIDToUniversityID").child(reporterId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot reporterSnap) {
                        String recipientUnivId = reporterSnap.exists() ? reporterSnap.getValue(String.class) : reporterId;

                        if (senderUnivId.equals(recipientUnivId)) {
                            btnClaim.setVisibility(View.GONE);
                            return;
                        }

                        mDatabase.child("ItemClaims").child(itemId).child(senderUnivId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    btnClaim.setEnabled(false);
                                    btnClaim.setText("Claim Sent");
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
