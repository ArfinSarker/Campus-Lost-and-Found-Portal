package com.sas.lostandfound;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.List;

public class ItemDetailActivity extends AppCompatActivity {

    private ImageView ivItemImage, ivUserPhoto, ivResolvedUserPhoto;
    private TextView tvStatus, tvItemName, tvCategory, tvDescription, tvLocation, tvDateTime, tvDetailReportId;
    private TextView tvReporterName, tvReporterUniversityId, tvReporterType, tvReporterDeptOrDesignation, tvReporterPhone;
    private TextView tvHeaderTitle, tvHeaderSubtitle;
    private TextView tvProofOwnership, tvHandlingStatus, tvSecurityQuestion;
    private TextView tvResolutionTitle, tvResolvedUserName, tvResolvedUserUniversityId, tvResolvedUserType, tvResolvedUserDeptOrDesignation;
    private LinearLayout headerTitleContainer, llLostSpecifics, llFoundSpecifics, llReportedByContainer, llReporterActions, llResolutionContainer;
    private MaterialCardView cardBadge, cardEditedLabel, cardReportId;
    private MaterialButton btnContact, btnClaim, btnDelete;
    private MaterialButton btnEdit, btnReporterDelete, btnMarkAsClaimed, btnReturnToOwner;
    private Toolbar toolbar;
    private AppBarLayout appBarLayout;
    
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private static final String DATABASE_URL = "https://campus-lost-and-found-portal-default-rtdb.asia-southeast1.firebasedatabase.app";

    private String itemId, itemStatus, reporterId, currentAdminStatus;
    private boolean isAdminMode;
    private ValueEventListener itemListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance(DATABASE_URL).getReference();

        // Get initial data from intent
        itemId = getIntent().getStringExtra("itemId");
        itemStatus = getIntent().getStringExtra("itemStatus");
        reporterId = getIntent().getStringExtra("userId");
        isAdminMode = getIntent().getBooleanExtra("isAdmin", false);

        initializeViews();
        setupToolbar();
        setupScrollBehavior();
        displayInitialData(); // Show intent data first
        
        if (reporterId != null) {
            fetchReporterProfile(reporterId);
        }

        checkUserRoleAndSetupActions();
        startListeningToItemChanges();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListeningToItemChanges();
    }

    private void initializeViews() {
        ivItemImage = findViewById(R.id.ivItemImage);
        ivUserPhoto = findViewById(R.id.ivUserPhoto);
        ivResolvedUserPhoto = findViewById(R.id.ivResolvedUserPhoto);
        tvStatus = findViewById(R.id.tvStatus);
        tvItemName = findViewById(R.id.tvItemName);
        tvCategory = findViewById(R.id.tvCategory);
        tvDescription = findViewById(R.id.tvDescription);
        tvLocation = findViewById(R.id.tvLocation);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvDetailReportId = findViewById(R.id.tvDetailReportId);
        tvReporterName = findViewById(R.id.tvReporterName);
        tvReporterUniversityId = findViewById(R.id.tvReporterUniversityId);
        tvReporterType = findViewById(R.id.tvReporterType);
        tvReporterDeptOrDesignation = findViewById(R.id.tvReporterDeptOrDesignation);
        tvReporterPhone = findViewById(R.id.tvReporterPhone);
        
        tvProofOwnership = findViewById(R.id.tvProofOwnership);
        tvHandlingStatus = findViewById(R.id.tvHandlingStatus);
        tvSecurityQuestion = findViewById(R.id.tvSecurityQuestion);
        
        tvResolutionTitle = findViewById(R.id.tvResolutionTitle);
        tvResolvedUserName = findViewById(R.id.tvResolvedUserName);
        tvResolvedUserUniversityId = findViewById(R.id.tvResolvedUserUniversityId);
        tvResolvedUserType = findViewById(R.id.tvResolvedUserType);
        tvResolvedUserDeptOrDesignation = findViewById(R.id.tvResolvedUserDeptOrDesignation);
        
        llLostSpecifics = findViewById(R.id.llLostSpecifics);
        llFoundSpecifics = findViewById(R.id.llFoundSpecifics);
        llReportedByContainer = findViewById(R.id.llReportedByContainer);
        llReporterActions = findViewById(R.id.llReporterActions);
        llResolutionContainer = findViewById(R.id.llResolutionContainer);
        
        cardBadge = findViewById(R.id.cardBadge);
        cardEditedLabel = findViewById(R.id.cardEditedLabel);
        cardReportId = findViewById(R.id.cardReportId);
        btnContact = findViewById(R.id.btnContact);
        btnClaim = findViewById(R.id.btnClaim);
        btnDelete = findViewById(R.id.btnDelete);
        
        btnEdit = findViewById(R.id.btnEdit);
        btnReporterDelete = findViewById(R.id.btnReporterDelete);
        btnMarkAsClaimed = findViewById(R.id.btnMarkAsClaimed);
        btnReturnToOwner = findViewById(R.id.btnReturnToOwner);

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
        String itemCategory = getIntent().getStringExtra("itemCategory");
        String itemImageUrl = getIntent().getStringExtra("itemImageUrl");
        String reportId = getIntent().getStringExtra("itemReportId");

        if (tvDetailReportId != null && reportId != null) {
            tvDetailReportId.setText(reportId);
        }

        updateUI(itemName, itemDescription, itemLocation, itemCategory, itemDate, itemTime, itemImageUrl, false);
    }

    private void startListeningToItemChanges() {
        if (itemId == null || itemStatus == null) return;
        String path = "lost".equalsIgnoreCase(itemStatus) ? "LostItems" : "FoundItems";
        
        itemListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Item item = snapshot.getValue(Item.class);
                if (item != null) {
                    currentAdminStatus = item.getAdminStatus();
                    updateUI(item.getName(), item.getDescription(), item.getLocation(), item.getCategory(), 
                            item.getDate(), item.getTime(), item.getImageUrl(), item.isEdited());
                    
                    if (tvDetailReportId != null && item.getDisplayId() != null) {
                        tvDetailReportId.setText(item.getDisplayId());
                    }

                    // Priority: Show contact info from report document
                    if (item.getContactName() != null && !item.getContactName().isEmpty()) {
                        tvReporterName.setText(item.getContactName());
                    }
                    if (item.getContactPhone() != null && !item.getContactPhone().isEmpty()) {
                        tvReporterPhone.setText(item.getContactPhone());
                        tvReporterPhone.setVisibility(View.VISIBLE);
                    } else {
                        tvReporterPhone.setVisibility(View.GONE);
                    }

                    // Update reporterId if it changes or to ensure we have the latest
                    reporterId = item.getUserId();
                    checkUserRoleAndSetupActions();

                    // Specific fields
                    if (item.isEdited() && isAdminMode) {
                        cardEditedLabel.setVisibility(View.VISIBLE);
                    } else {
                        cardEditedLabel.setVisibility(View.GONE);
                    }

                    if ("lost".equalsIgnoreCase(itemStatus)) {
                        llLostSpecifics.setVisibility(View.VISIBLE);
                        llFoundSpecifics.setVisibility(View.GONE);
                        tvProofOwnership.setText(item.getProofOfOwnershipDetail() != null && !item.getProofOfOwnershipDetail().isEmpty()
                                ? item.getProofOfOwnershipDetail() : "No details provided.");
                    } else {
                        llLostSpecifics.setVisibility(View.GONE);
                        llFoundSpecifics.setVisibility(View.VISIBLE);

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
                        tvHandlingStatus.setText(handlingBuilder.length() > 0 ? handlingBuilder.toString() : "Status not provided.");
                        tvSecurityQuestion.setText(item.getHiddenIdentificationQuestion() != null && !item.getHiddenIdentificationQuestion().isEmpty()
                                ? item.getHiddenIdentificationQuestion() : "No security question provided.");
                    }
                    
                    // Show resolution info if claimed/returned
                    if ("Claimed".equalsIgnoreCase(currentAdminStatus) || "Returned".equalsIgnoreCase(currentAdminStatus)) {
                        String secondUserId = item.getClaimedByUserId();
                        if (secondUserId != null && !secondUserId.isEmpty()) {
                            fetchResolvedUserProfile(secondUserId);
                            llResolutionContainer.setVisibility(View.VISIBLE);
                            if ("lost".equalsIgnoreCase(itemStatus)) {
                                tvResolutionTitle.setText("Returned By");
                            } else {
                                tvResolutionTitle.setText("Returned To");
                            }
                        }
                    } else {
                        llResolutionContainer.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mDatabase.child(path).child(itemId).addValueEventListener(itemListener);
    }

    private void stopListeningToItemChanges() {
        if (itemListener != null && itemId != null && itemStatus != null) {
            String path = "lost".equalsIgnoreCase(itemStatus) ? "LostItems" : "FoundItems";
            mDatabase.child(path).child(itemId).removeEventListener(itemListener);
        }
    }

    private void updateUI(String name, String description, String location, String category, String date, String time, String imageUrl, boolean isEdited) {
        tvItemName.setText(name != null ? name : "No Name");
        tvDescription.setText(description != null ? description : "No Description");
        tvLocation.setText(location != null ? location : "No Location");
        tvCategory.setText(category != null ? category : "Uncategorized");
        
        tvHeaderTitle.setText(itemStatus != null && itemStatus.equalsIgnoreCase("lost") ? "Lost Item" : "Found Item");
        tvHeaderSubtitle.setText((category != null ? category : "Uncategorized") + " • " + (location != null ? location : "N/A"));

        String fullDateTime = date != null ? date : "";
        if (time != null && !time.isEmpty()) {
            fullDateTime += " - " + time;
        }
        tvDateTime.setText(fullDateTime);

        if (itemStatus != null) {
            boolean isResolved = "Claimed".equalsIgnoreCase(currentAdminStatus) || "Returned".equalsIgnoreCase(currentAdminStatus);
            if (isResolved) {
                tvStatus.setText(R.string.status_resolved);
                cardBadge.setCardBackgroundColor(getResources().getColor(R.color.found_bg));
                tvStatus.setTextColor(getResources().getColor(R.color.statusFound));
            } else {
                if (itemStatus.equalsIgnoreCase("lost")) {
                    tvStatus.setText(getString(R.string.status_lost_label));
                    cardBadge.setCardBackgroundColor(getResources().getColor(R.color.lost_bg));
                    tvStatus.setTextColor(getResources().getColor(R.color.statusLost));
                } else {
                    tvStatus.setText(getString(R.string.status_found_label));
                    cardBadge.setCardBackgroundColor(getResources().getColor(R.color.found_bg));
                    tvStatus.setTextColor(getResources().getColor(R.color.statusFound));
                }
            }
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_package).into(ivItemImage);
        }
    }

    private void checkUserRoleAndSetupActions() {
        FirebaseUser authUser = mAuth.getCurrentUser();
        if (authUser == null) return;

        mDatabase.child("UIDToUniversityID").child(authUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String currentUnivId = snapshot.exists() ? snapshot.getValue(String.class) : authUser.getUid();

                boolean isReporter = currentUnivId.equals(reporterId) || authUser.getUid().equals(reporterId);
                boolean isResolved = "Claimed".equalsIgnoreCase(currentAdminStatus) || "Returned".equalsIgnoreCase(currentAdminStatus);

                if (isReporter) {
                    llReportedByContainer.setVisibility(View.GONE);
                    btnClaim.setVisibility(View.GONE);
                    btnDelete.setVisibility(View.GONE);
                    llReporterActions.setVisibility(View.VISIBLE);
                    
                    if (isResolved) {
                        btnEdit.setVisibility(View.GONE);
                        btnMarkAsClaimed.setVisibility(View.GONE);
                        btnReturnToOwner.setVisibility(View.GONE);
                        btnReporterDelete.setVisibility(View.VISIBLE); // Keep only delete
                    } else {
                        btnEdit.setVisibility(View.VISIBLE);
                        btnReporterDelete.setVisibility(View.VISIBLE);
                        if ("lost".equalsIgnoreCase(itemStatus)) {
                            btnMarkAsClaimed.setVisibility(View.VISIBLE);
                            btnReturnToOwner.setVisibility(View.GONE);
                        } else {
                            btnReturnToOwner.setVisibility(View.VISIBLE);
                            btnMarkAsClaimed.setVisibility(View.GONE);
                        }
                    }
                    
                    setupReporterActions();
                } else if (isAdminMode) {
                    llReportedByContainer.setVisibility(View.VISIBLE);
                    llReporterActions.setVisibility(View.GONE);
                    btnClaim.setVisibility(View.GONE);
                    btnDelete.setVisibility(View.VISIBLE);
                    btnDelete.setOnClickListener(v -> deleteItem(itemId, itemStatus));
                } else {
                    llReporterActions.setVisibility(View.GONE);
                    btnDelete.setVisibility(View.GONE);

                    // Check if this is the "Second User" (Claimant/Helper)
                    if (isResolved && currentUnivId.equals(getIntent().getStringExtra("claimedByUserId"))) {
                        llReportedByContainer.setVisibility(View.VISIBLE);
                        btnContact.setVisibility(View.VISIBLE);
                        btnClaim.setVisibility(View.GONE);
                    } else if (isResolved) {
                        // Regular user view for resolved item
                        llReportedByContainer.setVisibility(View.VISIBLE);
                        btnContact.setVisibility(View.GONE);
                        btnClaim.setVisibility(View.GONE);
                    } else {
                        // Regular user view for active item
                        llReportedByContainer.setVisibility(View.VISIBLE);
                        btnContact.setVisibility(View.VISIBLE);
                        btnClaim.setVisibility(View.VISIBLE);
                        if ("lost".equalsIgnoreCase(itemStatus)) {
                            btnClaim.setText("I Found This Item");
                        } else {
                            btnClaim.setText("This is Mine");
                        }
                        btnClaim.setOnClickListener(v -> handleClaim(itemId, getIntent().getStringExtra("itemName"), itemStatus, reporterId));
                        checkIfAlreadyClaimed(itemId, reporterId);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void setupReporterActions() {
        btnEdit.setOnClickListener(v -> {
            Intent intent;
            if ("lost".equalsIgnoreCase(itemStatus)) {
                intent = new Intent(this, CampusReportLostActivity.class);
            } else {
                intent = new Intent(this, CampusReportFoundActivity.class);
            }
            intent.putExtra("editItemId", itemId);
            startActivity(intent);
        });

        btnReporterDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Delete Report")
                    .setMessage("Are you sure you want to delete this report?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteItem(itemId, itemStatus))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        
        btnMarkAsClaimed.setOnClickListener(v -> showMarkAsClaimedDialog());
        btnReturnToOwner.setOnClickListener(v -> showReturnToOwnerDialog());
    }

    private void showMarkAsClaimedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.btn_mark_as_claimed);
        
        final EditText input = new EditText(this);
        input.setHint(R.string.prompt_mark_as_claimed);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(padding, padding, padding, 0);
        container.addView(input);
        
        builder.setView(container);

        builder.setPositiveButton(R.string.btn_submit, (dialog, which) -> {
            String universityId = input.getText().toString().trim();
            if (!universityId.isEmpty()) {
                markItemAsClaimed(universityId);
            } else {
                Toast.makeText(this, "Please enter a University ID", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showReturnToOwnerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.btn_return_to_owner);
        
        final EditText input = new EditText(this);
        input.setHint(R.string.prompt_return_to_owner);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(padding, padding, padding, 0);
        container.addView(input);
        
        builder.setView(container);

        builder.setPositiveButton(R.string.btn_submit, (dialog, which) -> {
            String universityId = input.getText().toString().trim();
            if (!universityId.isEmpty()) {
                processReturnToOwner(universityId);
            } else {
                Toast.makeText(this, "Please enter a University ID", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void markItemAsClaimed(String universityId) {
        if (itemId == null || itemStatus == null) return;
        
        // Basic validation
        if (universityId.isEmpty()) {
            Toast.makeText(this, "University ID must not be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String path = "lost".equalsIgnoreCase(itemStatus) ? "LostItems" : "FoundItems";

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Check if receiver exists
        mDatabase.child("Users").child(universityId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                if (!userSnapshot.exists()) {
                    Toast.makeText(ItemDetailActivity.this, "Invalid University ID: User not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                mDatabase.child("UIDToUniversityID").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        final String reporterUnivId = snapshot.exists() ? snapshot.getValue(String.class) : currentUser.getUid();

                        if (universityId.equals(reporterUnivId)) {
                            Toast.makeText(ItemDetailActivity.this, "You cannot mark yourself as the receiver", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        mDatabase.child("Users").child(reporterUnivId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot reporterSnap) {
                                User reporter = reporterSnap.getValue(User.class);
                                String reporterName = (reporter != null) ? reporter.getName() : "A user";
                                String reporterPhone = (reporter != null) ? reporter.getPhone() : "";
                                String reporterEmail = (reporter != null) ? reporter.getEmail() : "";

                                // Update item status
                                mDatabase.child(path).child(itemId).child("adminStatus").setValue("Claimed");
                                mDatabase.child(path).child(itemId).child("claimedByUserId").setValue(universityId);

                                // Create notification for the receiver
                                String notificationId = mDatabase.child("Notifications").child(universityId).push().getKey();
                                if (notificationId != null) {
                                    String itemName = tvItemName.getText().toString();
                                    String message = String.format(getString(R.string.msg_item_claimed_notification), reporterName, itemName);
                                    Notification notification = new Notification(
                                            notificationId, universityId, reporterUnivId, reporterName, reporterPhone, reporterEmail,
                                            itemId, itemName, message, System.currentTimeMillis(), "lost_claimed_confirmed", ""
                                    );
                                    mDatabase.child("Notifications").child(universityId).child(notificationId).setValue(notification);
                                }

                                Toast.makeText(ItemDetailActivity.this, "Item marked as Resolved", Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ItemDetailActivity.this, "Error checking user: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processReturnToOwner(String ownerUniversityId) {
        if (itemId == null || itemStatus == null) return;
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Check if owner exists
        mDatabase.child("Users").child(ownerUniversityId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                if (!userSnapshot.exists()) {
                    Toast.makeText(ItemDetailActivity.this, "Invalid University ID: User not found", Toast.LENGTH_SHORT).show();
                    return;
                }

                mDatabase.child("UIDToUniversityID").child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        final String reporterUnivId = snapshot.exists() ? snapshot.getValue(String.class) : currentUser.getUid();

                        if (ownerUniversityId.equals(reporterUnivId)) {
                            Toast.makeText(ItemDetailActivity.this, "You cannot enter your own ID", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        mDatabase.child("Users").child(reporterUnivId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot reporterSnap) {
                                User reporter = reporterSnap.getValue(User.class);
                                String reporterName = (reporter != null) ? reporter.getName() : "A user";
                                String reporterPhone = (reporter != null) ? reporter.getPhone() : "";
                                String reporterEmail = (reporter != null) ? reporter.getEmail() : "";

                                String path = "found".equalsIgnoreCase(itemStatus) ? "FoundItems" : "LostItems";

                                // Update item status
                                mDatabase.child(path).child(itemId).child("adminStatus").setValue("Returned");
                                mDatabase.child(path).child(itemId).child("claimedByUserId").setValue(ownerUniversityId);

                                // Create notification for the owner
                                String notificationId = mDatabase.child("Notifications").child(ownerUniversityId).push().getKey();
                                if (notificationId != null) {
                                    String itemName = tvItemName.getText().toString();
                                    String message = String.format(getString(R.string.msg_item_returned_notification), reporterName, itemName);
                                    Notification notification = new Notification(
                                            notificationId, ownerUniversityId, reporterUnivId, reporterName, reporterPhone, reporterEmail,
                                            itemId, itemName, message, System.currentTimeMillis(), "item_returned_confirmed", ""
                                    );
                                    mDatabase.child("Notifications").child(ownerUniversityId).child(notificationId).setValue(notification);
                                }

                                Toast.makeText(ItemDetailActivity.this, "Item marked as Resolved", Toast.LENGTH_SHORT).show();
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ItemDetailActivity.this, "Error checking user: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchReporterProfile(String reporterId) {
        if (reporterId == null) return;
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
                                // Only set Name and Phone from profile if they weren't overridden in the report
                                if (tvReporterName.getText().toString().isEmpty() || tvReporterName.getText().toString().equals("No Name") || tvReporterName.getText().toString().equals("John Doe")) {
                                    tvReporterName.setText(user.getName());
                                }
                                
                                if (tvReporterPhone.getVisibility() == View.GONE || tvReporterPhone.getText().toString().isEmpty()) {
                                    tvReporterPhone.setText(user.getPhone());
                                    tvReporterPhone.setVisibility(View.VISIBLE);
                                }

                                tvReporterUniversityId.setText("ID: " + user.getUniversityId());
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

                                // If admin, allow clicking profile to see details
                                ivUserPhoto.setOnClickListener(v -> {
                                    if (getIntent().getBooleanExtra("isAdmin", false)) {
                                        Intent intent = new Intent(ItemDetailActivity.this, UserProfileActivity.class);
                                        intent.putExtra("targetUserId", resolvedId);
                                        intent.putExtra("isAdminViewing", true);
                                        startActivity(intent);
                                    }
                                });
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void fetchResolvedUserProfile(String secondUserId) {
        if (secondUserId == null) return;
        mDatabase.child("Users").child(secondUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        tvResolvedUserName.setText(user.getName());
                        tvResolvedUserUniversityId.setText("ID: " + user.getUniversityId());
                        tvResolvedUserType.setText(user.getUserType());

                        if ("Staff".equalsIgnoreCase(user.getUserType())) {
                            tvResolvedUserDeptOrDesignation.setText(user.getDesignation());
                            tvResolvedUserDeptOrDesignation.setVisibility(View.VISIBLE);
                        } else if ("Student".equalsIgnoreCase(user.getUserType())) {
                            tvResolvedUserDeptOrDesignation.setText(user.getDepartment());
                            tvResolvedUserDeptOrDesignation.setVisibility(View.VISIBLE);
                        } else {
                            tvResolvedUserDeptOrDesignation.setVisibility(View.GONE);
                        }

                        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                            Glide.with(ItemDetailActivity.this)
                                    .load(user.getProfileImageUrl())
                                    .placeholder(R.drawable.ic_user)
                                    .circleCrop()
                                    .into(ivResolvedUserPhoto);
                        } else {
                            ivResolvedUserPhoto.setImageResource(R.drawable.ic_user);
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void handleClaim(String itemId, String itemName, String itemStatus, String reporterId) {
        FirebaseUser authUser = mAuth.getCurrentUser();
        if (authUser == null) {
            Toast.makeText(this, "Please login to claim items", Toast.LENGTH_SHORT).show();
            return;
        }

        if (reporterId == null || itemId == null) {
            Toast.makeText(this, "Error: Invalid item or reporter", Toast.LENGTH_SHORT).show();
            return;
        }

        btnClaim.setEnabled(false);
        btnClaim.setText("Sending...");

        mDatabase.child("UIDToUniversityID").child(authUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final String senderId = snapshot.exists() ? snapshot.getValue(String.class) : authUser.getUid();

                mDatabase.child("UIDToUniversityID").child(reporterId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot reporterMapping) {
                        final String recipientId = reporterMapping.exists() ? reporterMapping.getValue(String.class) : reporterId;

                        if (senderId.equals(recipientId)) {
                            Toast.makeText(ItemDetailActivity.this, "You cannot claim your own item", Toast.LENGTH_SHORT).show();
                            resetClaimButton(itemStatus);
                            return;
                        }

                        mDatabase.child("Users").child(senderId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot senderSnap) {
                                if (senderSnap.exists()) {
                                    createAndSendNotification(senderSnap, senderId, recipientId, itemId, itemName, itemStatus);
                                } else {
                                    mDatabase.child("Users").child(authUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot authSnap) {
                                            createAndSendNotification(authSnap.exists() ? authSnap : null, senderId, recipientId, itemId, itemName, itemStatus);
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

    private void createAndSendNotification(DataSnapshot senderSnap, String senderId, String recipientId, String itemId, String itemName, String itemStatus) {
        String senderName = "A user";
        String senderPhone = "Not provided";
        String senderEmail = "";

        if (senderSnap != null) {
            senderName = senderSnap.child("name").getValue(String.class);
            if (senderName == null) senderName = senderSnap.child("fullName").getValue(String.class);
            if (senderName == null) senderName = "A user";

            senderPhone = senderSnap.child("phone").getValue(String.class);
            senderEmail = senderSnap.child("email").getValue(String.class);
        }

        String notificationId = mDatabase.child("Notifications").child(recipientId).push().getKey();
        if (notificationId == null) {
            resetClaimButton(itemStatus);
            return;
        }

        String type = "lost_claim";
        if (!"lost".equalsIgnoreCase(itemStatus)) {
            type = "found_claim";
        }

        String message = "lost".equalsIgnoreCase(itemStatus) ?
                "Someone claims they found your lost item (" + itemName + "). Click to view details." :
                "Someone claims this item (" + itemName + ") is theirs. Click to view details.";

        Notification notification = new Notification(
                notificationId, recipientId, senderId, senderName, senderPhone, senderEmail,
                itemId, itemName, message, System.currentTimeMillis(), type, ""
        );

        mDatabase.child("Notifications").child(recipientId).child(notificationId).setValue(notification)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        mDatabase.child("ItemClaims").child(itemId).child(senderId).setValue(System.currentTimeMillis());
                        Toast.makeText(ItemDetailActivity.this, "Request sent successfully!", Toast.LENGTH_SHORT).show();
                        btnClaim.setText("Request Sent");
                        btnClaim.setEnabled(false);
                    } else {
                        Toast.makeText(ItemDetailActivity.this, "Failed to send request.", Toast.LENGTH_SHORT).show();
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
        if (authUser == null || itemId == null || reporterId == null) return;

        mDatabase.child("UIDToUniversityID").child(authUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final String senderUnivId = snapshot.exists() ? snapshot.getValue(String.class) : authUser.getUid();

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
                                    btnClaim.setText("Request Sent");
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

    private void deleteItem(String itemId, String status) {
        String path = "lost".equalsIgnoreCase(status) ? "LostItems" : "FoundItems";

        mDatabase.child(path).child(itemId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Item item = snapshot.getValue(Item.class);
                    if (item != null) {
                        // 1. Delete images from storage (Supabase or Firebase)
                        deleteItemImages(item);

                        // 2. Delete from node (LostItems/FoundItems)
                        mDatabase.child(path).child(itemId).removeValue();

                        // 3. Delete from UserItems
                        if (item.getUserId() != null) {
                            mDatabase.child("UserItems").child(item.getUserId()).child(itemId).removeValue();
                        }

                        // 4. Delete claims and notifications related to this item if needed
                        // (Optional, but good for database hygiene)
                        mDatabase.child("ItemClaims").child(itemId).removeValue();

                        Toast.makeText(ItemDetailActivity.this, "Report updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ItemDetailActivity.this, "Delete failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteItemImages(Item item) {
        List<String> urls = item.getImageUrls();
        if (urls == null) urls = new ArrayList<>();
        if (item.getImageUrl() != null && !urls.contains(item.getImageUrl())) {
            urls.add(item.getImageUrl());
        }

        for (String url : urls) {
            if (url == null || url.isEmpty()) continue;

            if (url.contains("supabase.co")) {
                SupabaseStorageHelper.deleteImage(url, null);
            } else if (url.contains("firebasestorage.googleapis.com")) {
                try {
                    FirebaseStorage.getInstance().getReferenceFromUrl(url).delete();
                } catch (Exception ignored) {}
            }
        }
    }
}
