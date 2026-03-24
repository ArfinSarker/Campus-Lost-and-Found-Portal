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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ClaimDetailsActivity extends AppCompatActivity {

    private TextView tvHeaderTitle, tvNameHeader, tvUniversityId, tvGender, tvBatch, tvLevelTerm, tvDepartment, tvSection, tvPhone, tvEmail, tvPreferredContact, tvDesignation;
    private TextView tvItemName, tvCategory, tvDescription, tvItemDetails, tvOwnershipVerification, tvHandlingStatus, tvSecurityQuestion;
    private TextView tvInformationLabel, tvContactLabel, tvItemHeaderLabel;
    private ImageView ivClaimantPhoto, ivItemImage;
    private LinearLayout llSection, llBatch, llLevelTerm, llDesignation, llDepartment, llOwnershipVerification, llFoundSpecifics;
    private MaterialButton btnCall, btnEmail, btnMarkReturned;
    private DatabaseReference mDatabase;
    private String itemId, senderId, itemStatus, notificationType;
    private ValueEventListener claimantProfileListener;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_claim_details);

        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        initializeViews();
        setupToolbar();

        String itemName = getIntent().getStringExtra("itemName");
        itemId = getIntent().getStringExtra("itemId");
        senderId = getIntent().getStringExtra("senderId");
        notificationType = getIntent().getStringExtra("type");

        setupLabels();

        if (senderId != null) {
            setupRealtimeClaimantProfile(senderId);
        }

        if (itemId != null) {
            fetchItemDetails(itemId);
        }

        btnCall.setOnClickListener(v -> {
            String phone = tvPhone.getText().toString();
            if (!phone.isEmpty() && !"Not Specified".equals(phone)) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            }
        });

        btnEmail.setOnClickListener(v -> {
            String email = tvEmail.getText().toString();
            if (!email.isEmpty() && !"Not Specified".equals(email)) {
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + email));
                intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding " + itemName);
                startActivity(Intent.createChooser(intent, "Send Email"));
            }
        });
        
        btnMarkReturned.setOnClickListener(v -> markAsReturned());
    }

    private void initializeViews() {
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        ivClaimantPhoto = findViewById(R.id.ivClaimantPhoto);
        ivItemImage = findViewById(R.id.ivItemImage);
        tvNameHeader = findViewById(R.id.tvClaimantNameHeader);
        tvUniversityId = findViewById(R.id.etUniversityId);
        tvGender = findViewById(R.id.etGender);
        
        tvDesignation = findViewById(R.id.etDesignation);
        llDesignation = findViewById(R.id.tilDesignation);
        
        tvBatch = findViewById(R.id.etBatch);
        llBatch = findViewById(R.id.tilBatch);
        
        tvLevelTerm = findViewById(R.id.etLevelTerm);
        llLevelTerm = findViewById(R.id.tilLevelTerm);
        
        tvDepartment = findViewById(R.id.etDepartment);
        llDepartment = findViewById(R.id.tilDepartment);

        tvSection = findViewById(R.id.etSection);
        llSection = findViewById(R.id.tilSection);
        
        tvPhone = findViewById(R.id.etPhone);
        tvEmail = findViewById(R.id.etEmail);
        tvPreferredContact = findViewById(R.id.etPreferredContact);
        
        tvItemName = findViewById(R.id.tvItemName);
        tvCategory = findViewById(R.id.tvCategory);
        tvDescription = findViewById(R.id.tvDescription);
        tvItemDetails = findViewById(R.id.tvItemDetails);
        
        llOwnershipVerification = findViewById(R.id.llOwnershipVerification);
        tvOwnershipVerification = findViewById(R.id.tvOwnershipVerification);
        
        llFoundSpecifics = findViewById(R.id.llFoundSpecifics);
        tvHandlingStatus = findViewById(R.id.tvHandlingStatus);
        tvSecurityQuestion = findViewById(R.id.tvSecurityQuestion);

        // Labels for dynamic text
        tvInformationLabel = findViewById(R.id.tvInformationLabel);
        tvContactLabel = findViewById(R.id.tvContactLabel);
        tvItemHeaderLabel = findViewById(R.id.tvItemHeaderLabel);
        
        btnCall = findViewById(R.id.btnCall);
        btnEmail = findViewById(R.id.btnEmail);
        btnMarkReturned = findViewById(R.id.btnMarkReturned);
    }

    private void setupLabels() {
        if ("lost_claimed_confirmed".equals(notificationType)) {
            tvHeaderTitle.setText("Claim Confirmed");
            tvInformationLabel.setText("Reporter Information");
            tvContactLabel.setText("Contact Reporter");
            tvItemHeaderLabel.setText("Item Details");
            btnCall.setText("Call Reporter");
        } else if ("item_returned_confirmed".equals(notificationType)) {
            tvHeaderTitle.setText("Item Returned");
            tvInformationLabel.setText("Finder Information");
            tvContactLabel.setText("Contact Finder");
            tvItemHeaderLabel.setText("Item Details");
            btnCall.setText("Call Finder");
        } else {
            tvHeaderTitle.setText("Claimer Details");
            tvInformationLabel.setText("Information");
            tvContactLabel.setText("Contact");
            tvItemHeaderLabel.setText("Item Claimed");
            btnCall.setText("Call Claimant");
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRealtimeClaimantProfile(String claimantId) {
        claimantProfileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String name = snapshot.child("name").getValue(String.class);
                    if (name == null) name = snapshot.child("fullName").getValue(String.class);
                    
                    String univId = snapshot.child("universityId").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);
                    String designation = snapshot.child("designation").getValue(String.class);
                    String batch = snapshot.child("batch").getValue(String.class);
                    String levelTerm = snapshot.child("levelTerm").getValue(String.class);
                    String dept = snapshot.child("department").getValue(String.class);
                    String section = snapshot.child("section").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    tvNameHeader.setText(name != null && !name.isEmpty() ? name : "Not Specified");
                    tvUniversityId.setText(univId != null && !univId.isEmpty() ? univId : "Not Specified");
                    tvGender.setText(gender != null && !gender.isEmpty() ? gender : "Not Specified");
                    tvEmail.setText(email != null && !email.isEmpty() ? email : "Not Specified");

                    // Dynamic Visibility based on field existence in profile
                    updateFieldVisibility(llDesignation, tvDesignation, designation);
                    updateFieldVisibility(llBatch, tvBatch, batch);
                    updateFieldVisibility(llLevelTerm, tvLevelTerm, levelTerm);
                    updateFieldVisibility(llDepartment, tvDepartment, dept);
                    updateFieldVisibility(llSection, tvSection, section);

                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        Glide.with(ClaimDetailsActivity.this)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_user)
                                .circleCrop()
                                .into(ivClaimantPhoto);
                    } else {
                        ivClaimantPhoto.setImageResource(R.drawable.ic_user);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child("Users").child(claimantId).addValueEventListener(claimantProfileListener);
    }

    private void updateFieldVisibility(LinearLayout layout, TextView textView, String value) {
        if (value != null && !value.isEmpty()) {
            textView.setText(value);
            layout.setVisibility(View.VISIBLE);
        } else {
            layout.setVisibility(View.GONE);
        }
    }

    private void fetchItemDetails(String itemId) {
        // First check FoundItems
        mDatabase.child("FoundItems").child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    itemStatus = "found";
                    displayItem(snapshot);
                } else {
                    // Then check LostItems
                    mDatabase.child("LostItems").child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                itemStatus = "lost";
                                displayItem(snapshot);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void displayItem(DataSnapshot snapshot) {
        Item item = snapshot.getValue(Item.class);
        if (item != null) {
            tvItemName.setText(item.getName());
            tvCategory.setText(item.getCategory());
            tvDescription.setText(item.getDescription());
            tvPhone.setText(item.getUserPhone() != null && !item.getUserPhone().isEmpty() ? item.getUserPhone() : "Not Specified");
            tvPreferredContact.setText(item.getPreferredContactMethod() != null && !item.getPreferredContactMethod().isEmpty() ? item.getPreferredContactMethod() : "Not Specified");

            String details = "Date: " + item.getDate();
            if (item.getTime() != null && !item.getTime().isEmpty()) {
                details += "\nTime: " + item.getTime();
            }
            details += "\nLocation: " + item.getLocation();
            tvItemDetails.setText(details);

            // Handle Item Image
            String imageUrl = item.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                ivItemImage.setVisibility(View.VISIBLE);
                Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_package)
                        .into(ivItemImage);
            } else {
                ivItemImage.setImageResource(R.drawable.ic_package);
            }

            if ("lost".equals(itemStatus)) {
                llOwnershipVerification.setVisibility(View.VISIBLE);
                llFoundSpecifics.setVisibility(View.GONE);
                tvOwnershipVerification.setText(item.getProofOfOwnershipDetail() != null && !item.getProofOfOwnershipDetail().isEmpty() ? item.getProofOfOwnershipDetail() : "Not Specified");
                
                if ("Claimed".equalsIgnoreCase(item.getAdminStatus()) || "Returned".equalsIgnoreCase(item.getAdminStatus())) {
                    btnMarkReturned.setEnabled(false);
                    btnMarkReturned.setText("Item Already Recovered");
                    btnMarkReturned.setVisibility("lost_claimed_confirmed".equals(notificationType) || "item_returned_confirmed".equals(notificationType) ? View.GONE : View.VISIBLE);
                } else {
                    btnMarkReturned.setVisibility("lost_claimed_confirmed".equals(notificationType) || "item_returned_confirmed".equals(notificationType) ? View.GONE : View.VISIBLE);
                    btnMarkReturned.setText("Mark as Recovered");
                }
            } else {
                llOwnershipVerification.setVisibility(View.GONE);
                llFoundSpecifics.setVisibility(View.VISIBLE);
                tvHandlingStatus.setText(item.getItemHandlingStatus() != null && !item.getItemHandlingStatus().isEmpty() ? item.getItemHandlingStatus() : "Not Specified");
                tvSecurityQuestion.setText(item.getHiddenIdentificationQuestion() != null && !item.getHiddenIdentificationQuestion().isEmpty() ? item.getHiddenIdentificationQuestion() : "Not Specified");

                if ("Claimed".equalsIgnoreCase(item.getAdminStatus()) || "Returned".equalsIgnoreCase(item.getAdminStatus())) {
                    btnMarkReturned.setEnabled(false);
                    btnMarkReturned.setText("Item Already Returned");
                    btnMarkReturned.setVisibility("item_returned_confirmed".equals(notificationType) ? View.GONE : View.VISIBLE);
                } else {
                    btnMarkReturned.setVisibility("item_returned_confirmed".equals(notificationType) ? View.GONE : View.VISIBLE);
                    btnMarkReturned.setText("Mark as Returned");
                }
            }
        }
    }

    private void markAsReturned() {
        if (itemId == null || senderId == null || itemStatus == null) return;

        btnMarkReturned.setEnabled(false);
        btnMarkReturned.setText("Updating...");

        String path = "found".equals(itemStatus) ? "FoundItems" : "LostItems";
        String statusToSet = "found".equals(itemStatus) ? "Returned" : "Claimed";

        mDatabase.child(path).child(itemId).child("adminStatus").setValue(statusToSet);
        mDatabase.child(path).child(itemId).child("claimedByUserId").setValue(senderId)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Item marked as " + statusToSet.toLowerCase(), Toast.LENGTH_SHORT).show();
                        btnMarkReturned.setText("Marked as " + statusToSet);
                        btnMarkReturned.setEnabled(false);
                    } else {
                        Toast.makeText(this, "Failed to update status", Toast.LENGTH_SHORT).show();
                        btnMarkReturned.setEnabled(true);
                        btnMarkReturned.setText("Mark as Returned");
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (claimantProfileListener != null && senderId != null) {
            mDatabase.child("Users").child(senderId).removeEventListener(claimantProfileListener);
        }
    }
}
