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

    private TextView tvNameHeader, tvUniversityId, tvGender, tvBatch, tvLevelTerm, tvDepartment, tvSection, tvPhone, tvEmail, tvPreferredContact;
    private TextView tvItemName, tvCategory, tvDescription, tvItemDetails, tvOwnershipVerification, tvHandlingStatus, tvSecurityQuestion;
    private ImageView ivClaimantPhoto;
    private LinearLayout llSection, llOwnershipVerification, llFoundSpecifics;
    private MaterialButton btnCall, btnEmail, btnMarkReturned;
    private DatabaseReference mDatabase;
    private String itemId, senderId, itemStatus;
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
                intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding your claim for " + itemName);
                startActivity(Intent.createChooser(intent, "Send Email"));
            }
        });
        
        btnMarkReturned.setOnClickListener(v -> markAsReturned());
    }

    private void initializeViews() {
        ivClaimantPhoto = findViewById(R.id.ivClaimantPhoto);
        tvNameHeader = findViewById(R.id.tvClaimantNameHeader);
        tvUniversityId = findViewById(R.id.etUniversityId);
        tvGender = findViewById(R.id.etGender);
        tvBatch = findViewById(R.id.etBatch);
        tvLevelTerm = findViewById(R.id.etLevelTerm);
        tvDepartment = findViewById(R.id.etDepartment);
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
        
        btnCall = findViewById(R.id.btnCall);
        btnEmail = findViewById(R.id.btnEmail);
        btnMarkReturned = findViewById(R.id.btnMarkReturned);
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
                    String univId = snapshot.child("universityId").getValue(String.class);
                    String gender = snapshot.child("gender").getValue(String.class);
                    String batch = snapshot.child("batch").getValue(String.class);
                    String levelTerm = snapshot.child("levelTerm").getValue(String.class);
                    String dept = snapshot.child("department").getValue(String.class);
                    String section = snapshot.child("section").getValue(String.class);
                    String email = snapshot.child("email").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    tvNameHeader.setText(name != null && !name.isEmpty() ? name : "Not Specified");
                    tvUniversityId.setText(univId != null && !univId.isEmpty() ? univId : "Not Specified");
                    tvGender.setText(gender != null && !gender.isEmpty() ? gender : "Not Specified");
                    tvBatch.setText(batch != null && !batch.isEmpty() ? batch : "Not Specified");
                    tvLevelTerm.setText(levelTerm != null && !levelTerm.isEmpty() ? levelTerm : "Not Specified");
                    tvDepartment.setText(dept != null && !dept.isEmpty() ? dept : "Not Specified");
                    tvEmail.setText(email != null && !email.isEmpty() ? email : "Not Specified");

                    if (section != null && !section.isEmpty()) {
                        tvSection.setText(section);
                        llSection.setVisibility(View.VISIBLE);
                    } else {
                        tvSection.setText("Not Specified");
                        llSection.setVisibility(View.VISIBLE);
                    }

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

            if ("lost".equals(itemStatus)) {
                llOwnershipVerification.setVisibility(View.VISIBLE);
                llFoundSpecifics.setVisibility(View.GONE);
                tvOwnershipVerification.setText(item.getProofOfOwnershipDetail() != null && !item.getProofOfOwnershipDetail().isEmpty() ? item.getProofOfOwnershipDetail() : "Not Specified");
                
                if ("Claimed".equalsIgnoreCase(item.getAdminStatus()) || "Returned".equalsIgnoreCase(item.getAdminStatus())) {
                    btnMarkReturned.setEnabled(false);
                    btnMarkReturned.setText("Item Already Recovered");
                    btnMarkReturned.setVisibility(View.VISIBLE);
                } else {
                    btnMarkReturned.setVisibility(View.VISIBLE);
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
                    btnMarkReturned.setVisibility(View.VISIBLE);
                } else {
                    btnMarkReturned.setVisibility(View.VISIBLE);
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
