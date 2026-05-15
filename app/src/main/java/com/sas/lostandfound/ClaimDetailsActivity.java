package com.sas.lostandfound;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClaimDetailsActivity extends AppCompatActivity {

    private TextView tvHeaderTitle, tvNameHeader, tvUniversityId, tvGender, tvBatch, tvLevelTerm, tvDepartment, tvSection, tvPhone, tvEmail, tvPreferredContact, tvDesignation, tvReportId;
    private TextView tvItemName, tvCategory, tvDescription, tvItemDetails, tvOwnershipVerification, tvHandlingStatus, tvSecurityQuestion;
    private TextView tvInformationLabel, tvContactLabel, tvItemHeaderLabel;
    private ImageView ivClaimantPhoto, ivItemImage;
    private ViewPager2 viewPagerImageSlider;
    private TabLayout tabLayoutIndicator;
    private LinearLayout llSection, llBatch, llLevelTerm, llDesignation, llDepartment, llOwnershipVerification, llFoundSpecifics;
    private MaterialButton btnCall, btnEmail, btnMarkReturned;
    private String itemId, senderId, itemStatus, notificationType, currentUnivId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_claim_details);

        initializeViews();
        setupToolbar();

        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUnivId = prefs.getString("universityId", null);

        String itemName = getIntent().getStringExtra("itemName");
        itemId = getIntent().getStringExtra("itemId");
        senderId = getIntent().getStringExtra("claimerId");
        if (senderId == null) {
            senderId = getIntent().getStringExtra("senderId");
        }
        
        notificationType = getIntent().getStringExtra("type");

        setupLabels();

        // Immediate visibility check to prevent flicker
        if (senderId != null && senderId.equals(currentUnivId)) {
            tvContactLabel.setVisibility(View.GONE);
            btnCall.setVisibility(View.GONE);
            btnEmail.setVisibility(View.GONE);
        }

        if (senderId != null) {
            setupClaimantProfile(senderId);
        }

        if (itemId != null) {
            fetchItemDetails(itemId);
        }

        startRealtimeProfileListener();

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
                startActivity(intent);
            }
        });
        
        btnMarkReturned.setOnClickListener(v -> markAsReturned());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
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

        tvInformationLabel = findViewById(R.id.tvInformationLabel);
        tvContactLabel = findViewById(R.id.tvContactLabel);
        tvItemHeaderLabel = findViewById(R.id.tvItemHeaderLabel);
        tvReportId = findViewById(R.id.tvReportId);
        viewPagerImageSlider = findViewById(R.id.viewPagerImageSlider);
        tabLayoutIndicator = findViewById(R.id.tabLayoutIndicator);
        
        btnCall = findViewById(R.id.btnCall);
        btnEmail = findViewById(R.id.btnEmail);
        btnMarkReturned = findViewById(R.id.btnMarkReturned);
    }

    private void setupLabels() {
        if ("lost_claimed_confirmed".equals(notificationType) || "item_returned_confirmed".equals(notificationType)) {
            tvHeaderTitle.setText(R.string.title_claimed_confirm);
            tvInformationLabel.setText("Person Information");
            tvItemHeaderLabel.setText(R.string.label_item_details);
        } else {
            tvHeaderTitle.setText("Claimer Details");
            tvInformationLabel.setText("Information");
            tvItemHeaderLabel.setText("Item Claimed");
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            HeaderColorHelper.setup(this, appBarLayout, toolbar);
        }
    }

    private void setupClaimantProfile(String claimantId) {
        String query = "university_id=eq." + claimantId + "&select=*&limit=1";
        SupabaseDatabaseHelper.select("profiles", query, new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users != null && !users.isEmpty()) {
                    User user = users.get(0);
                    if (user != null) {
                        String name = user.getName();
                        tvNameHeader.setText(!TextUtils.isEmpty(name) ? name : "Not Specified");
                        
                        String uId = user.getUniversityId();
                        tvUniversityId.setText(!TextUtils.isEmpty(uId) ? uId : "Not Specified");
                        
                        String gender = user.getGender();
                        tvGender.setText(!TextUtils.isEmpty(gender) ? gender : "Not Specified");
                        
                        String email = user.getEmail();
                        tvEmail.setText(!TextUtils.isEmpty(email) ? email : "Not Specified");
                        
                        String phone = user.getPhone();
                        tvPhone.setText(!TextUtils.isEmpty(phone) ? phone : "Not Specified");

                        String userType = user.getUserType();
                        boolean isStudent = "Student".equalsIgnoreCase(userType);
                        boolean isStaffOrAdmin = "Staff".equalsIgnoreCase(userType) || "Admin".equalsIgnoreCase(userType);

                        // Role-based visibility
                        llDesignation.setVisibility(isStaffOrAdmin ? View.VISIBLE : View.GONE);
                        llBatch.setVisibility(isStudent ? View.VISIBLE : View.GONE);
                        llLevelTerm.setVisibility(isStudent ? View.VISIBLE : View.GONE);
                        llSection.setVisibility(isStudent ? View.VISIBLE : View.GONE);
                        llDepartment.setVisibility(View.VISIBLE); // Always show department

                        if (isStaffOrAdmin) {
                            tvDesignation.setText(user.getDesignation() != null ? user.getDesignation() : "Not Specified");
                        }
                        if (isStudent) {
                            tvBatch.setText(user.getBatch() != null ? user.getBatch() : "Not Specified");
                            tvLevelTerm.setText(user.getLevelTerm() != null ? user.getLevelTerm() : "Not Specified");
                            tvSection.setText(user.getSection() != null ? user.getSection() : "Not Specified");
                        }
                        tvDepartment.setText(user.getDepartment() != null ? user.getDepartment() : "Not Specified");

                        // Contact visibility check
                        if (currentUnivId != null && currentUnivId.equals(claimantId)) {
                            tvContactLabel.setVisibility(View.GONE);
                            btnCall.setVisibility(View.GONE);
                            btnEmail.setVisibility(View.GONE);
                        } else {
                            tvContactLabel.setVisibility(View.VISIBLE);
                            btnCall.setVisibility(View.VISIBLE);
                            btnEmail.setVisibility(View.VISIBLE);
                        }

                        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                            GlideApp.with(ClaimDetailsActivity.this)
                                    .load(user.getProfileImageUrl())
                                    .placeholder(R.drawable.ic_user)
                                    .thumbnail(0.1f)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .circleCrop()
                                    .into(ivClaimantPhoto);
                        } else {
                            ivClaimantPhoto.setImageResource(R.drawable.ic_user);
                        }
                    }
                }
            }
            @Override public void onFailure(String e) {
                ErrorHelper.showError(tvNameHeader, "Failed to load profile: " + e);
            }
        });
    }

    private void startRealtimeProfileListener() {
        if (senderId == null) return;
        
        // Polling as a fallback for real-time since Supabase SDK in this project 
        // is implemented via OkHttp/REST rather than a persistent WebSocket client.
        // This ensures the reporter sees updates if they stay on this screen.
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) {
                    setupClaimantProfile(senderId);
                    new Handler(Looper.getMainLooper()).postDelayed(this, 10000); // Refresh every 10s
                }
            }
        }, 10000);
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
        SupabaseDatabaseHelper.select("found_reports", "id=eq." + itemId + "&limit=1", new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> items) {
                if (items != null && !items.isEmpty()) {
                    itemStatus = "found";
                    displayItem(items.get(0));
                } else {
                    SupabaseDatabaseHelper.select("lost_reports", "id=eq." + itemId + "&limit=1", new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
                        @Override
                        public void onSuccess(List<Item> items2) {
                            if (items2 != null && !items2.isEmpty()) {
                                itemStatus = "lost";
                                displayItem(items2.get(0));
                            }
                        }
                        @Override public void onFailure(String e) {}
                    });
                }
            }
            @Override public void onFailure(String e) {}
        });
    }

    private void displayItem(Item item) {
        if (item == null) return;
        String formattedId = ReportIdFormatter.format(item.getDisplayId() != null ? item.getDisplayId() : item.getReportId());
        
        if (!formattedId.isEmpty()) {
            tvReportId.setText(formattedId);
            tvReportId.setVisibility(View.VISIBLE);
        } else {
            tvReportId.setVisibility(View.GONE);
        }

        tvItemName.setText(item.getName());
        tvCategory.setText(item.getCategory());
        tvDescription.setText(item.getDescription());
        tvPhone.setText(item.getUserPhone() != null && !item.getUserPhone().isEmpty() ? item.getUserPhone() : "Not Specified");
        tvPreferredContact.setText(item.getPreferredContactMethod() != null && !item.getPreferredContactMethod().isEmpty() ? item.getPreferredContactMethod() : "Not Specified");

        String details = "Date: " + item.getDate();
        if (item.getTime() != null && !item.getTime().isEmpty()) {
            details += "\nTime: " + item.getTime();
        }
        String formattedLocation = ReportLocationDisplay.formatFullLocation(item.getLocation(), item.getManualLocation(), item.getAdditionalLocationDetails());
        details += "\nLocation: " + formattedLocation;
        tvItemDetails.setText(details);

        setupImageSlider(item.getImageUrls(), item.getImageUrl());

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

    private void setupImageSlider(List<String> imageUrls, String fallbackUrl) {
        if (imageUrls != null && imageUrls.size() > 1) {
            ivItemImage.setVisibility(View.GONE);
            viewPagerImageSlider.setVisibility(View.VISIBLE);
            tabLayoutIndicator.setVisibility(View.VISIBLE);
            ImageSliderAdapter adapter = new ImageSliderAdapter(imageUrls, true);
            adapter.setOnImageClickListener(pos -> ItemNavigationUtils.openFullScreenImage(this, imageUrls, pos));
            viewPagerImageSlider.setAdapter(adapter);
            new TabLayoutMediator(tabLayoutIndicator, viewPagerImageSlider, (t, p) -> {}).attach();
        } else {
            viewPagerImageSlider.setVisibility(View.GONE);
            tabLayoutIndicator.setVisibility(View.GONE);
            ivItemImage.setVisibility(View.VISIBLE);
            String url = (imageUrls != null && !imageUrls.isEmpty()) ? imageUrls.get(0) : fallbackUrl;
            if (url != null && !url.isEmpty()) {
                GlideApp.with(this).load(url).placeholder(R.drawable.ic_package).thumbnail(0.1f).diskCacheStrategy(DiskCacheStrategy.ALL).into(ivItemImage);
                ivItemImage.setOnClickListener(v -> {
                    ArrayList<String> urls = new ArrayList<>();
                    urls.add(url);
                    ItemNavigationUtils.openFullScreenImage(this, urls, 0);
                });
            } else {
                ivItemImage.setImageResource(R.drawable.ic_package);
                ivItemImage.setOnClickListener(null);
            }
        }
    }

    private void markAsReturned() {
        if (itemId == null || senderId == null || itemStatus == null || currentUnivId == null) return;
        btnMarkReturned.setEnabled(false);
        btnMarkReturned.setText("Updating...");
        String table = "found".equals(itemStatus) ? "found_reports" : "lost_reports";
        String statusToSet = "found".equals(itemStatus) ? "Returned" : "Claimed";
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("admin_status", statusToSet);
        updates.put("claimed_by_id", senderId);
        updates.put("status", "resolved");

        SupabaseDatabaseHelper.update(table, "id=eq." + itemId, updates, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                // Fetch reporter details to send in confirmation notification
                SupabaseDatabaseHelper.select("profiles", "university_id=eq." + currentUnivId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
                    @Override
                    public void onSuccess(List<User> reporters) {
                        User reporter = (reporters != null && !reporters.isEmpty()) ? reporters.get(0) : null;
                        String reporterName = (reporter != null) ? reporter.getName() : "A user";
                        
                        // Fetch recipient (the person who found/claimed) profile for their auth_id (RLS)
                        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + senderId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
                            @Override
                            public void onSuccess(List<User> recipients) {
                                if (recipients != null && !recipients.isEmpty()) {
                                    User recipient = recipients.get(0);
                                    String recipientAuthId = recipient.getAuthId();
                                    
                                    String notificationId = java.util.UUID.randomUUID().toString();
                                    String itemName = tvItemName.getText().toString();
                                    String type = "lost".equals(itemStatus) ? "item_claimed" : "item_return";
                                    String message = "lost".equals(itemStatus) 
                                        ? String.format("\"%s\" has marked that they received \"%s\" from you. Click to view details.", reporterName, itemName)
                                        : String.format("\"%s\" has marked that they returned \"%s\" from you. Click to view details.", reporterName, itemName);

                                    Notification notification = new Notification(notificationId, senderId, currentUnivId, 
                                        reporterName, reporter != null ? reporter.getPhone() : "", 
                                        reporter != null ? reporter.getEmail() : "", 
                                        reporter != null ? reporter.getProfileImageUrl() : "", 
                                        itemId, itemName, message, System.currentTimeMillis(), type, "");
                                    notification.setItemName(itemName); // Ensure itemName is explicitly set
                                    notification.setUserId(recipientAuthId);

                                    SupabaseDatabaseHelper.insert("notifications", notification, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                                        @Override
                                        public void onSuccess(String r) {
                                            finishWithSuccess(statusToSet);
                                        }
                                        @Override
                                        public void onFailure(String e) {
                                            SnackbarManager.show(SnackbarManager.Type.ERROR, "Status updated, but notification failed: " + e);
                                            finishWithSuccess(statusToSet);
                                        }
                                    });
                                } else {
                                    finishWithSuccess(statusToSet);
                                }
                            }
                            @Override public void onFailure(String e) { finishWithSuccess(statusToSet); }
                        });
                    }
                    @Override public void onFailure(String e) { finishWithSuccess(statusToSet); }
                });
            }

            @Override
            public void onFailure(String e) {
                SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to update status");
                btnMarkReturned.setEnabled(true);
                btnMarkReturned.setText("Mark as Returned");
            }
        });
    }

    private void finishWithSuccess(String statusToSet) {
        SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Item marked as " + statusToSet.toLowerCase());
        btnMarkReturned.setText("Marked as " + statusToSet);
        btnMarkReturned.setEnabled(false);
        // Refresh item details to show updated state
        fetchItemDetails(itemId);
    }
}
