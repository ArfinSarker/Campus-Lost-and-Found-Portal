package com.sas.lostandfound;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemDetailActivity extends AppCompatActivity {

    private ImageView ivItemImage, ivUserPhoto, ivResolvedUserPhoto;
    private ViewPager2 viewPagerImageSlider;
    private TabLayout tabLayoutIndicator;
    private TextView tvStatusBadge, tvItemName, tvCategory, tvDescription, tvLocation, tvDateTime, tvDisplayId;
    private TextView tvReporterName, tvReporterUniversityId, tvReporterType, tvReporterDeptOrDesignation, tvPreferredContact;
    private TextView tvHeaderTitle;
    private TextView tvProofOwnership, tvHandlingStatus, tvSecurityQuestion;
    private TextView tvResolutionTitle, tvResolvedUserName, tvResolvedUserUniversityId, tvResolvedUserType, tvResolvedUserDeptOrDesignation, tvResolvedUserPreferredContact;
    private LinearLayout llLostSpecifics, llFoundSpecifics, llReportedByContainer, llReporterActions, llResolutionContainer;
    private MaterialCardView cardStatusBadge, cardEditedLabel, cardReportId;
    private MaterialButton btnContact, btnClaim, btnDelete;
    private MaterialButton btnEdit, btnReporterDelete, btnMarkAsClaimed, btnReturnToOwner, btnResolvedUserContact;
    private Toolbar toolbar;
    private AppBarLayout appBarLayout;
    private SwipeRefreshLayout swipeRefreshLayout;

    private String itemId, itemStatus, reporterId, currentAdminStatus, currentUnivId;
    private boolean isAdminMode;
    private Item currentItem;

    private Handler sliderHandler = new Handler(Looper.getMainLooper());
    private Runnable sliderRunnable;
    private boolean isUserInteracting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        android.content.SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUnivId = prefs.getString("universityId", null);
        isAdminMode = ModeManager.isAdminMode(this);

        itemId = getIntent().getStringExtra("itemId");
        itemStatus = getIntent().getStringExtra("itemStatus");
        reporterId = getIntent().getStringExtra("userId");

        initializeViews();
        setupToolbar();
        setupScrollBehavior();
        setupSwipeRefresh();
        displayInitialData();
        
        // Immediate visibility check to prevent flicker
        // Containers are GONE in XML. We show them if we have the IDs, but keep buttons GONE.
        if (reporterId != null) {
            llReportedByContainer.setVisibility(View.VISIBLE);
        }
        
        boolean isResolved = "Claimed".equalsIgnoreCase(currentAdminStatus) || "Returned".equalsIgnoreCase(currentAdminStatus);
        if (isResolved) {
            llResolutionContainer.setVisibility(View.VISIBLE);
        }

        startListeningToItemChanges();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopAutoSlide();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentItem != null && currentItem.getImageUrls() != null && currentItem.getImageUrls().size() > 1) {
            startAutoSlide(currentItem.getImageUrls().size());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAutoSlide();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initializeViews() {
        ivItemImage = findViewById(R.id.ivItemImage);
        viewPagerImageSlider = findViewById(R.id.viewPagerImageSlider);
        tabLayoutIndicator = findViewById(R.id.tabLayoutIndicator);
        ivUserPhoto = findViewById(R.id.ivUserPhoto);
        ivResolvedUserPhoto = findViewById(R.id.ivResolvedUserPhoto);
        tvStatusBadge = findViewById(R.id.tvStatusBadge);
        tvItemName = findViewById(R.id.tvItemName);
        tvCategory = findViewById(R.id.tvCategory);
        tvDescription = findViewById(R.id.tvDescription);
        tvLocation = findViewById(R.id.tvLocation);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvDisplayId = findViewById(R.id.tvDisplayId);
        tvReporterName = findViewById(R.id.tvReporterName);
        tvReporterUniversityId = findViewById(R.id.tvReporterUniversityId);
        tvReporterType = findViewById(R.id.tvReporterType);
        tvReporterDeptOrDesignation = findViewById(R.id.tvReporterDeptOrDesignation);
        tvPreferredContact = findViewById(R.id.tvPreferredContact);

        tvProofOwnership = findViewById(R.id.tvProofOwnership);
        tvHandlingStatus = findViewById(R.id.tvHandlingStatus);
        tvSecurityQuestion = findViewById(R.id.tvSecurityQuestion);
        
        tvResolutionTitle = findViewById(R.id.tvResolutionTitle);
        tvResolvedUserName = findViewById(R.id.tvResolvedUserName);
        tvResolvedUserUniversityId = findViewById(R.id.tvResolvedUserUniversityId);
        tvResolvedUserType = findViewById(R.id.tvResolvedUserType);
        tvResolvedUserDeptOrDesignation = findViewById(R.id.tvResolvedUserDeptOrDesignation);
        tvResolvedUserPreferredContact = findViewById(R.id.tvResolvedUserPreferredContact);
        
        llLostSpecifics = findViewById(R.id.llLostSpecifics);
        llFoundSpecifics = findViewById(R.id.llFoundSpecifics);
        llReportedByContainer = findViewById(R.id.llReportedByContainer);
        llReporterActions = findViewById(R.id.llReporterActions);
        llResolutionContainer = findViewById(R.id.llResolutionContainer);
        
        cardStatusBadge = findViewById(R.id.cardStatusBadge);
        cardEditedLabel = findViewById(R.id.cardEditedLabel);
        cardReportId = findViewById(R.id.cardReportId);
        btnContact = findViewById(R.id.btnContact);
        btnClaim = findViewById(R.id.btnClaim);
        btnDelete = findViewById(R.id.btnDelete);
        
        btnEdit = findViewById(R.id.btnEdit);
        btnReporterDelete = findViewById(R.id.btnReporterDelete);
        btnMarkAsClaimed = findViewById(R.id.btnMarkAsClaimed);
        btnReturnToOwner = findViewById(R.id.btnReturnToOwner);
        btnResolvedUserContact = findViewById(R.id.btnResolvedUserContact);

        toolbar = findViewById(R.id.toolbar);
        appBarLayout = findViewById(R.id.appBarLayout);
        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        viewPagerImageSlider.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    isUserInteracting = true;
                    stopAutoSlide();
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    if (isUserInteracting) {
                        isUserInteracting = false;
                        if (currentItem != null && currentItem.getImageUrls() != null && currentItem.getImageUrls().size() > 1) {
                            startAutoSlide(currentItem.getImageUrls().size());
                        }
                    }
                }
            }
        });

        ivItemImage.setOnClickListener(v -> {
            List<String> urls = new ArrayList<>();
            if (currentItem != null) {
                if (currentItem.getImageUrls() != null && !currentItem.getImageUrls().isEmpty()) {
                    urls.addAll(currentItem.getImageUrls());
                } else if (currentItem.getImageUrl() != null && !currentItem.getImageUrl().isEmpty()) {
                    urls.add(currentItem.getImageUrl());
                }
            }
            if (urls.isEmpty()) {
                String intentUrl = getIntent().getStringExtra("itemImageUrl");
                if (intentUrl != null) urls.add(intentUrl);
            }
            if (!urls.isEmpty()) {
                ItemNavigationUtils.openFullScreenImage(this, urls, 0);
            }
        });
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupScrollBehavior() {
        HeaderColorHelper.setup(this, appBarLayout, toolbar);
    }

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(this, R.color.primaryColor));
            swipeRefreshLayout.setOnRefreshListener(this::startListeningToItemChanges);
        }
    }

    private void displayInitialData() {
        String itemName = getIntent().getStringExtra("itemName");
        String itemDescription = getIntent().getStringExtra("itemDescription");
        String itemLocation = getIntent().getStringExtra("itemLocation");
        String manualLocation = getIntent().getStringExtra("manualLocation");
        String additionalDetails = getIntent().getStringExtra("additionalLocationDetails");
        String itemDate = getIntent().getStringExtra("itemDate");
        String itemTime = getIntent().getStringExtra("itemTime");
        String itemCategory = getIntent().getStringExtra("itemCategory");
        String itemImageUrl = getIntent().getStringExtra("itemImageUrl");
        String reportId = getIntent().getStringExtra("itemReportId");

        if (tvDisplayId != null && reportId != null) {
            tvDisplayId.setText(ReportIdFormatter.format(reportId));
        }

        updateUI(itemName, itemDescription, itemLocation, manualLocation, additionalDetails, itemCategory, itemDate, itemTime, itemImageUrl, false, null);
    }

    private void startListeningToItemChanges() {
        if (itemId == null || itemStatus == null) {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            return;
        }
        String table = "lost".equalsIgnoreCase(itemStatus) ? "lost_reports" : "found_reports";
        
        SupabaseDatabaseHelper.select(table, "id=eq." + itemId + "&limit=1", new TypeToken<List<Item>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Item>>() {
            @Override
            public void onSuccess(List<Item> items) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                if (items != null && !items.isEmpty()) {
                    currentItem = items.get(0);
                    if (currentItem != null) {
                        currentAdminStatus = currentItem.getAdminStatus();
                        updateUI(currentItem.getName(), currentItem.getDescription(), currentItem.getLocation(), 
                                currentItem.getManualLocation(), currentItem.getAdditionalLocationDetails(), currentItem.getCategory(),
                                currentItem.getDate(), currentItem.getTime(), currentItem.getImageUrl(), currentItem.isEdited(), currentItem.getImageUrls());
                        
                        if (tvDisplayId != null) {
                            tvDisplayId.setText(ReportIdFormatter.format(currentItem.getDisplayId()));
                        }

                        reporterId = currentItem.getUserId();
                        
                        if (currentItem.isEdited() && isAdminMode) {
                            cardEditedLabel.setVisibility(View.VISIBLE);
                        } else {
                            cardEditedLabel.setVisibility(View.GONE);
                        }

                        if ("lost".equalsIgnoreCase(itemStatus)) {
                            llLostSpecifics.setVisibility(View.VISIBLE);
                            llFoundSpecifics.setVisibility(View.GONE);
                            tvProofOwnership.setText(currentItem.getProofOfOwnershipDetail() != null && !currentItem.getProofOfOwnershipDetail().isEmpty()
                                    ? currentItem.getProofOfOwnershipDetail() : "No details provided.");
                        } else {
                            llLostSpecifics.setVisibility(View.GONE);
                            llFoundSpecifics.setVisibility(View.VISIBLE);

                            StringBuilder handlingBuilder = new StringBuilder();
                            if (currentItem.getItemHandlingStatus() != null && !currentItem.getItemHandlingStatus().isEmpty()) {
                                handlingBuilder.append(currentItem.getItemHandlingStatus());
                            }
                            if (currentItem.getAuthorityName() != null && !currentItem.getAuthorityName().isEmpty()) {
                                if (handlingBuilder.length() > 0) handlingBuilder.append("\n");
                                handlingBuilder.append("Authority/Person: ").append(currentItem.getAuthorityName());
                            }
                            if (currentItem.getOfficeRoomNumber() != null && !currentItem.getOfficeRoomNumber().isEmpty()) {
                                if (handlingBuilder.length() > 0) handlingBuilder.append("\n");
                                handlingBuilder.append("Office/Room: ").append(currentItem.getOfficeRoomNumber());
                            }
                            tvHandlingStatus.setText(handlingBuilder.length() > 0 ? handlingBuilder.toString() : "Status not provided.");
                            tvSecurityQuestion.setText(currentItem.getHiddenIdentificationQuestion() != null && !currentItem.getHiddenIdentificationQuestion().isEmpty()
                                    ? currentItem.getHiddenIdentificationQuestion() : "No security question provided.");
                        }

                        setupPreferredContactLink(currentItem, currentItem.getPreferredContactMethod());
                        refreshUIBasedOnRole();
                    }
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                ErrorHelper.showError(tvItemName, "Failed to load data: " + errorMessage);
            }
        });
    }

    private void setupPreferredContactLink(Item item, String method) {
        if (method == null || method.isEmpty()) {
            tvPreferredContact.setVisibility(View.GONE);
            return;
        }
        tvPreferredContact.setVisibility(View.VISIBLE);
        String fullText = "Preferred Contact: " + method;
        SpannableString spannableString = new SpannableString(fullText);
        
        int start = fullText.indexOf(method);
        if (start == -1) {
            tvPreferredContact.setText(fullText);
            return;
        }
        int end = start + method.length();

        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                if ("Email".equalsIgnoreCase(method)) {
                    String email = item.getUserEmail();
                    if (email != null && !email.isEmpty()) {
                        Intent intent = new Intent(Intent.ACTION_SENDTO);
                        intent.setData(Uri.parse("mailto:" + email));
                        intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding your reported item: " + item.getName());
                        startActivity(Intent.createChooser(intent, "Send Email"));
                    } else {
                        SnackbarManager.show(SnackbarManager.Type.ERROR, "Email address not available");
                    }
                } else if ("Phone".equalsIgnoreCase(method)) {
                    String phone = item.getUserPhone();
                    if (phone != null && !phone.isEmpty()) {
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + phone));
                        startActivity(intent);
                    } else {
                        SnackbarManager.show(SnackbarManager.Type.ERROR, "Phone number not available");
                    }
                } else if ("In-app chat".equalsIgnoreCase(method)) {
                    handleInAppChatContact();
                }
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
                ds.setColor(getResources().getColor(R.color.primaryColor));
            }
        };

        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvPreferredContact.setText(spannableString);
        tvPreferredContact.setMovementMethod(LinkMovementMethod.getInstance());
        tvPreferredContact.setHighlightColor(Color.TRANSPARENT);
    }

    private void handleInAppChatContact() {
        if (currentItem == null || currentUnivId == null) return;
        if (currentUnivId.equals(currentItem.getReporterId())) {
            SnackbarManager.show(SnackbarManager.Type.ERROR, "You cannot chat with yourself.");
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("p_report_id", UUID.fromString(currentItem.getId()));
        params.put("p_user_id", currentUnivId);

        SupabaseDatabaseHelper.rpc("check_chat_state", params, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                try {
                    org.json.JSONArray jsonArray = new org.json.JSONArray(result);
                    if (jsonArray.length() > 0) {
                        org.json.JSONObject obj = jsonArray.getJSONObject(0);
                        String status = obj.optString("request_status", "null");
                        String conversationId = obj.optString("conversation_id", "null");

                        if ("accepted".equalsIgnoreCase(status) && !"null".equals(conversationId) && !conversationId.isEmpty()) {
                            Intent intent = new Intent(ItemDetailActivity.this, ChatActivity.class);
                            intent.putExtra("conversationId", conversationId);
                            intent.putExtra("otherUserId", currentItem.getReporterId());
                            intent.putExtra("otherUserName", currentItem.getUserName());
                            intent.putExtra("reportId", currentItem.getId());
                            intent.putExtra("itemName", currentItem.getName());
                            startActivity(intent);
                        } else if ("pending".equalsIgnoreCase(status)) {
                            new AlertDialog.Builder(ItemDetailActivity.this)
                                    .setTitle("Request Pending")
                                    .setMessage("Your chat request is pending reporter approval.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        } else if ("rejected".equalsIgnoreCase(status)) {
                            new AlertDialog.Builder(ItemDetailActivity.this)
                                    .setTitle("Request Declined")
                                    .setMessage("The reporter has declined messaging for this request.")
                                    .setPositiveButton("OK", null)
                                    .show();
                        } else {
                            openChatRequestActivity();
                        }
                    } else {
                        openChatRequestActivity();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    openChatRequestActivity();
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                openChatRequestActivity();
            }
        });
    }

    private void openChatRequestActivity() {
        Intent intent = new Intent(this, ChatRequestActivity.class);
        intent.putExtra("reportId", currentItem.getId());
        intent.putExtra("reporterId", currentItem.getReporterId());
        intent.putExtra("itemName", currentItem.getName());
        intent.putExtra("itemDescription", currentItem.getDescription());
        intent.putExtra("itemImageUrl", currentItem.getImageUrl());
        intent.putExtra("reporterName", currentItem.getUserName());
        startActivity(intent);
    }

    private void updateUI(String name, String description, String location, String manualLocation, String additionalDetails, String category, String date, String time, String imageUrl, boolean isEdited, List<String> imageUrls) {
        tvItemName.setText(name != null ? name : "No Name");
        tvDescription.setText(description != null ? description : "No Description");
        
        String formattedLocation = ReportLocationDisplay.formatFullLocation(location, manualLocation, additionalDetails);
        
        tvLocation.setText(formattedLocation);
        tvCategory.setText(category != null ? category : "Uncategorized");
        
        tvHeaderTitle.setText(name != null ? name : (itemStatus != null && itemStatus.equalsIgnoreCase("lost") ? "Lost Item" : "Found Item"));

        String fullDateTime = date != null ? date : "";
        if (time != null && !time.isEmpty()) {
            fullDateTime += " - " + time;
        }
        tvDateTime.setText(fullDateTime);

        if (itemStatus != null) {
            boolean isResolved = "Claimed".equalsIgnoreCase(currentAdminStatus) || "Returned".equalsIgnoreCase(currentAdminStatus);
            String statusText;
            int statusColor;
            if (isResolved) {
                statusText = getString(R.string.status_resolved);
                statusColor = getResources().getColor(R.color.badge_resolved_bg);
            } else {
                if (itemStatus.equalsIgnoreCase("lost")) {
                    statusText = getString(R.string.status_lost_label);
                    statusColor = getResources().getColor(R.color.badge_lost_bg);
                } else {
                    statusText = getString(R.string.status_found_label);
                    statusColor = getResources().getColor(R.color.badge_found_bg);
                }
            }

            if (tvStatusBadge != null) {
                tvStatusBadge.setText(statusText.toUpperCase());
            }
            if (cardStatusBadge != null) {
                cardStatusBadge.setCardBackgroundColor(statusColor);
            }
        }

        setupImageSlider(imageUrls, imageUrl);
    }

    private void setupImageSlider(List<String> imageUrls, String fallbackUrl) {
        if (imageUrls != null && imageUrls.size() > 1) {
            ivItemImage.setVisibility(View.GONE);
            viewPagerImageSlider.setVisibility(View.VISIBLE);
            tabLayoutIndicator.setVisibility(View.VISIBLE);

            ImageSliderAdapter adapter = new ImageSliderAdapter(imageUrls, true);
            adapter.setOnImageClickListener(position -> ItemNavigationUtils.openFullScreenImage(this, imageUrls, position));
            
            viewPagerImageSlider.setAdapter(adapter);

            new TabLayoutMediator(tabLayoutIndicator, viewPagerImageSlider, (tab, position) -> {}).attach();

            startAutoSlide(imageUrls.size());
        } else {
            stopAutoSlide();
            viewPagerImageSlider.setVisibility(View.GONE);
            tabLayoutIndicator.setVisibility(View.GONE);
            ivItemImage.setVisibility(View.VISIBLE);

            String finalUrl = (imageUrls != null && !imageUrls.isEmpty()) ? imageUrls.get(0) : fallbackUrl;
            if (finalUrl != null && !finalUrl.isEmpty()) {
                GlideApp.with(this)
                        .load(finalUrl)
                        .placeholder(R.drawable.ic_package)
                        .thumbnail(0.1f)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(ivItemImage);
                
                ivItemImage.setOnClickListener(v -> {
                    ArrayList<String> urls = new ArrayList<>();
                    urls.add(finalUrl);
                    ItemNavigationUtils.openFullScreenImage(this, urls, 0);
                });
            } else {
                ivItemImage.setImageResource(R.drawable.ic_package);
                ivItemImage.setOnClickListener(null);
            }
        }
    }

    private void startAutoSlide(int size) {
        if (isUserInteracting) return;
        stopAutoSlide();
        sliderRunnable = () -> {
            int current = viewPagerImageSlider.getCurrentItem();
            int next = (current + 1) % size;
            viewPagerImageSlider.setCurrentItem(next, true);
            sliderHandler.postDelayed(sliderRunnable, 3000);
        };
        sliderHandler.postDelayed(sliderRunnable, 3000);
    }

    private void stopAutoSlide() {
        if (sliderRunnable != null) {
            sliderHandler.removeCallbacks(sliderRunnable);
        }
    }

    private void refreshUIBasedOnRole() {
        if (currentUnivId == null || currentItem == null) return;

        boolean isReporter = currentUnivId.equals(reporterId);
        boolean isResolved = "Claimed".equalsIgnoreCase(currentAdminStatus) || "Returned".equalsIgnoreCase(currentAdminStatus);

        updateResolvedSections();

        if (isAdminMode) {
            llReportedByContainer.setVisibility(View.VISIBLE);
            llReporterActions.setVisibility(View.VISIBLE);
            setupReporterActions();
            
            btnClaim.setVisibility(View.GONE);
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setOnClickListener(v -> deleteItem(itemId, itemStatus));
            
            // In Admin mode, reporter actions like Edit/Mark As Claimed should also work if applicable
            btnEdit.setVisibility(View.VISIBLE);
            btnReporterDelete.setVisibility(View.GONE); // Hide this to avoid duplicate with btnDelete
            if ("lost".equalsIgnoreCase(itemStatus)) {
                btnMarkAsClaimed.setVisibility(View.VISIBLE);
                btnReturnToOwner.setVisibility(View.GONE);
            } else {
                btnReturnToOwner.setVisibility(View.VISIBLE);
                btnMarkAsClaimed.setVisibility(View.GONE);
            }
            return;
        }

        if (isReporter) {
            btnClaim.setVisibility(View.GONE);
            
            if (isResolved) {
                llReporterActions.setVisibility(View.GONE);
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> deleteItem(itemId, itemStatus));
            } else {
                btnDelete.setVisibility(View.GONE);
                llReporterActions.setVisibility(View.VISIBLE);
                btnEdit.setVisibility(View.VISIBLE);
                btnReporterDelete.setVisibility(View.VISIBLE);
                if ("lost".equalsIgnoreCase(itemStatus)) {
                    btnMarkAsClaimed.setVisibility(View.VISIBLE);
                    btnReturnToOwner.setVisibility(View.GONE);
                } else {
                    btnReturnToOwner.setVisibility(View.VISIBLE);
                    btnMarkAsClaimed.setVisibility(View.GONE);
                }
                setupReporterActions();
            }
        } else {
            llReporterActions.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);

            if (isResolved) {
                btnClaim.setVisibility(View.GONE);
            } else {
                btnClaim.setVisibility(View.VISIBLE);
                if ("lost".equalsIgnoreCase(itemStatus)) {
                    btnClaim.setText("I Found This Item");
                } else {
                    btnClaim.setText("This is Mine");
                }
                btnClaim.setOnClickListener(v -> handleClaim(itemId, currentItem.getName(), itemStatus, reporterId));
                checkIfAlreadyClaimed(itemId, reporterId);
            }
        }
    }

    private void updateResolvedSections() {
        if (currentItem == null || currentUnivId == null) return;

        boolean isResolved = "Claimed".equalsIgnoreCase(currentAdminStatus) || "Returned".equalsIgnoreCase(currentAdminStatus);
        String secondUserId = currentItem.getClaimedByUserId();
        
        boolean isMeReporter = currentUnivId.equals(reporterId);
        boolean isMeSecond = (secondUserId != null && currentUnivId.equals(secondUserId));

        // Sections should be visible if IDs are present
        llReportedByContainer.setVisibility(View.VISIBLE);
        fetchReporterProfile(reporterId, isMeReporter, isMeSecond);

        if (isResolved && secondUserId != null && !secondUserId.isEmpty()) {
            llResolutionContainer.setVisibility(View.VISIBLE);
            fetchResolvedUserProfile(secondUserId, isMeSecond, isMeReporter);
        } else {
            llResolutionContainer.setVisibility(View.GONE);
        }

        if ("lost".equalsIgnoreCase(itemStatus)) {
            tvResolutionTitle.setText("Returned By");
        } else {
            tvResolutionTitle.setText("Returned To");
        }
    }

    private void fetchReporterProfile(String reporterId, boolean isMe, boolean isViewerReceiver) {
        if (reporterId == null) return;
        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + reporterId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users != null && !users.isEmpty()) {
                    User user = users.get(0);
                    if (user != null) {
                        // Populate transient fields for Preferred Contact Method link
                        if (currentItem != null) {
                            if (currentItem.getUserEmail() == null || currentItem.getUserEmail().isEmpty()) {
                                currentItem.setUserEmail(user.getEmail());
                            }
                            if (currentItem.getUserPhone() == null || currentItem.getUserPhone().isEmpty()) {
                                currentItem.setUserPhone(user.getPhone());
                            }
                            if (currentItem.getUserName() == null || currentItem.getUserName().isEmpty()) {
                                currentItem.setUserName(user.getName());
                            }
                        }

                        tvReporterName.setText((currentItem != null ? currentItem.getUserName() : user.getName()) + (isMe ? " (You)" : ""));
                        tvReporterUniversityId.setText("ID: " + user.getUniversityId());
                        tvReporterUniversityId.setVisibility(View.VISIBLE);
                        tvReporterType.setText(user.getUserType());
                        tvReporterType.setVisibility(View.VISIBLE);

                        // Requirement: Receiver View does not show Reporter's Department
                        if (isViewerReceiver && !isAdminMode) {
                            tvReporterDeptOrDesignation.setVisibility(View.GONE);
                        } else {
                            if ("Staff".equalsIgnoreCase(user.getUserType()) || "Admin".equalsIgnoreCase(user.getUserType())) {
                                tvReporterDeptOrDesignation.setText(user.getDesignation());
                                tvReporterDeptOrDesignation.setVisibility(View.VISIBLE);
                            } else if ("Student".equalsIgnoreCase(user.getUserType())) {
                                tvReporterDeptOrDesignation.setText(user.getDepartment());
                                tvReporterDeptOrDesignation.setVisibility(View.VISIBLE);
                            } else {
                                tvReporterDeptOrDesignation.setVisibility(View.GONE);
                            }
                        }

                        // Reporter Specific: Preferred Contact Method
                        if (currentItem != null && currentItem.getPreferredContactMethod() != null && !currentItem.getPreferredContactMethod().isEmpty()) {
                            setupPreferredContactLink(currentItem, currentItem.getPreferredContactMethod());
                            tvPreferredContact.setVisibility(View.VISIBLE);
                        } else {
                            tvPreferredContact.setVisibility(View.GONE);
                        }
                        
                        // Hide contact button if it's the current user (unless admin)
                        if (isMe && !isAdminMode) {
                            btnContact.setVisibility(View.GONE);
                        } else {
                            btnContact.setVisibility(View.VISIBLE);
                            btnContact.setOnClickListener(v -> showContactOptions(user));
                        }

                        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                            GlideApp.with(ItemDetailActivity.this).load(user.getProfileImageUrl()).placeholder(R.drawable.ic_user).thumbnail(0.1f).diskCacheStrategy(DiskCacheStrategy.ALL).circleCrop().into(ivUserPhoto);
                        } else {
                            ivUserPhoto.setImageResource(R.drawable.ic_user);
                        }
                    }
                }
            }
            @Override public void onFailure(String errorMessage) {}
        });
    }

    private void fetchResolvedUserProfile(String secondUserId, boolean isMe, boolean isViewerReporter) {
        if (secondUserId == null) return;
        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + secondUserId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users != null && !users.isEmpty()) {
                    User user = users.get(0);
                    if (user != null) {
                        tvResolvedUserName.setText(user.getName() + (isMe ? " (You)" : ""));
                        tvResolvedUserUniversityId.setText("ID: " + user.getUniversityId());
                        tvResolvedUserUniversityId.setVisibility(View.VISIBLE);
                        tvResolvedUserType.setText(user.getUserType());
                        tvResolvedUserType.setVisibility(View.VISIBLE);
                        
                        if ("Staff".equalsIgnoreCase(user.getUserType()) || "Admin".equalsIgnoreCase(user.getUserType())) {
                            tvResolvedUserDeptOrDesignation.setText(user.getDesignation());
                            tvResolvedUserDeptOrDesignation.setVisibility(View.VISIBLE);
                        } else if ("Student".equalsIgnoreCase(user.getUserType())) {
                            tvResolvedUserDeptOrDesignation.setText(user.getDepartment());
                            tvResolvedUserDeptOrDesignation.setVisibility(View.VISIBLE);
                        } else {
                            tvResolvedUserDeptOrDesignation.setVisibility(View.GONE);
                        }
                        
                        // Receiver typically doesn't have a report-level preferred contact method
                        tvResolvedUserPreferredContact.setVisibility(View.GONE);
                        
                        // Hide contact button if it's the current user (unless admin)
                        if (isMe && !isAdminMode) {
                            btnResolvedUserContact.setVisibility(View.GONE);
                        } else {
                            btnResolvedUserContact.setVisibility(View.VISIBLE);
                            btnResolvedUserContact.setOnClickListener(v -> showContactOptions(user));
                        }

                        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                            GlideApp.with(ItemDetailActivity.this).load(user.getProfileImageUrl()).placeholder(R.drawable.ic_user).thumbnail(0.1f).diskCacheStrategy(DiskCacheStrategy.ALL).circleCrop().into(ivResolvedUserPhoto);
                        } else {
                            ivResolvedUserPhoto.setImageResource(R.drawable.ic_user);
                        }
                    }
                }
            }
            @Override public void onFailure(String errorMessage) {}
        });
    }

    private void showContactOptions(User user) {
        boolean hasInAppChat = currentItem != null && "In-app chat".equalsIgnoreCase(currentItem.getPreferredContactMethod());
        String[] options = hasInAppChat ? new String[]{"Email", "Phone", "In-app Chat"} : new String[]{"Email", "Phone"};
        new AlertDialog.Builder(this)
                .setTitle("Contact Information")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        String email = (currentItem != null && currentItem.getUserEmail() != null && !currentItem.getUserEmail().isEmpty())
                                ? currentItem.getUserEmail()
                                : user.getEmail();
                        if (email != null && !email.isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_SENDTO);
                            intent.setData(Uri.parse("mailto:" + email));
                            intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding item: " + tvItemName.getText().toString());
                            startActivity(Intent.createChooser(intent, "Send Email"));
                        } else SnackbarManager.show(SnackbarManager.Type.ERROR, "Email not available");
                    } else if (which == 1) {
                        String phone = (currentItem != null && currentItem.getUserPhone() != null && !currentItem.getUserPhone().isEmpty())
                                ? currentItem.getUserPhone()
                                : user.getPhone();
                        if (phone != null && !phone.isEmpty()) {
                            Intent intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(Uri.parse("tel:" + phone));
                            startActivity(intent);
                        } else SnackbarManager.show(SnackbarManager.Type.ERROR, "Phone not available");
                    } else if (which == 2) {
                        handleInAppChatContact();
                    }
                }).show();
    }

    private void setupReporterActions() {
        btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, "lost".equalsIgnoreCase(itemStatus) ? CampusReportLostActivity.class : CampusReportFoundActivity.class);
            intent.putExtra("editItemId", itemId);
            startActivity(intent);
        });
        btnReporterDelete.setOnClickListener(v -> deleteItem(itemId, itemStatus));
        btnMarkAsClaimed.setOnClickListener(v -> showMarkAsClaimedDialog());
        btnReturnToOwner.setOnClickListener(v -> showReturnToOwnerDialog());
    }

    private void showMarkAsClaimedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.btn_mark_as_claimed);
        final EditText input = new EditText(this);
        input.setHint(R.string.prompt_mark_as_claimed);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, 0);
        container.addView(input);
        builder.setView(container);
        builder.setPositiveButton(R.string.btn_submit, (dialog, which) -> {
            String uid = input.getText().toString().trim();
            if (!uid.isEmpty()) markItemAsClaimed(uid);
            else ErrorHelper.showError(tvItemName, "Please enter a University ID");
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showReturnToOwnerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.btn_return_to_owner);
        final EditText input = new EditText(this);
        input.setHint(R.string.prompt_return_to_owner);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, 0);
        container.addView(input);
        builder.setView(container);
        builder.setPositiveButton(R.string.btn_submit, (dialog, which) -> {
            String uid = input.getText().toString().trim();
            if (!uid.isEmpty()) processReturnToOwner(uid);
            else ErrorHelper.showError(tvItemName, "Please enter a University ID");
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void markItemAsClaimed(String universityId) {
        if (itemId == null || itemStatus == null || currentUnivId == null) return;
        String table = "lost".equalsIgnoreCase(itemStatus) ? "lost_reports" : "found_reports";

        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + universityId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users == null || users.isEmpty()) {
                    ErrorHelper.showError(tvItemName, "Invalid University ID. Please enter a valid University ID.");
                    return;
                }
                if (universityId.equals(currentUnivId)) {
                    ErrorHelper.showError(tvItemName, "You cannot mark yourself as the receiver");
                    return;
                }
                
                User receiver = users.get(0);
                String recipientAuthId = receiver.getAuthId();

                SupabaseDatabaseHelper.select("profiles", "university_id=eq." + currentUnivId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
                    @Override
                    public void onSuccess(List<User> reporterList) {
                        User reporter = (reporterList != null && !reporterList.isEmpty()) ? reporterList.get(0) : null;
                        String reporterName = (reporter != null) ? reporter.getName() : "A user";
                        String reporterPhone = (currentItem != null && currentItem.getUserPhone() != null && !currentItem.getUserPhone().isEmpty()) ? currentItem.getUserPhone() : ((reporter != null) ? reporter.getPhone() : "");
                        String reporterEmail = (currentItem != null && currentItem.getUserEmail() != null && !currentItem.getUserEmail().isEmpty()) ? currentItem.getUserEmail() : ((reporter != null) ? reporter.getEmail() : "");

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("admin_status", "Claimed");
                        updates.put("claimed_by_id", universityId);
                        updates.put("status", "resolved");

                        SupabaseDatabaseHelper.update(table, "id=eq." + itemId, updates, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                            @Override
                            public void onSuccess(String result) {
                                String notificationId = UUID.randomUUID().toString();
                                String itemName = tvItemName.getText().toString();
                                String type = "lost".equalsIgnoreCase(itemStatus) ? "item_claimed" : "item_return";
                                String message = "lost".equalsIgnoreCase(itemStatus) 
                                    ? String.format("\"%s\" has marked that they received \"%s\" from you. Click to view details.", reporterName, itemName)
                                    : String.format("\"%s\" has marked that they returned \"%s\" from you. Click to view details.", reporterName, itemName);
                                String senderImg = (reporter != null) ? reporter.getProfileImageUrl() : "";

                                Notification notification = new Notification(notificationId, universityId, currentUnivId, reporterName, reporterPhone, reporterEmail, senderImg, itemId, itemName, message, System.currentTimeMillis(), type, "");
                                notification.setItemName(itemName); // Ensure itemName is explicitly set
                                notification.setUserId(recipientAuthId); // Set for RLS

                                SupabaseDatabaseHelper.insert("notifications", notification, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                                    @Override public void onSuccess(String r) {
                                        SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Item marked as Resolved");
                                        startListeningToItemChanges();
                                    }
                                    @Override public void onFailure(String e) {
                                        SnackbarManager.show(SnackbarManager.Type.ERROR, "Item resolved, but notification failed: " + e);
                                        startListeningToItemChanges();
                                    }
                                });
                            }
                            @Override public void onFailure(String e) { ErrorHelper.showError(tvItemName, "Update failed: " + e); }
                        });
                    }
                    @Override public void onFailure(String e) {}
                });
            }
            @Override public void onFailure(String e) { ErrorHelper.showError(tvItemName, "Error checking user: " + e); }
        });
    }

    private void processReturnToOwner(String ownerUniversityId) {
        if (itemId == null || itemStatus == null || currentUnivId == null) return;
        
        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + ownerUniversityId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> users) {
                if (users == null || users.isEmpty()) {
                    ErrorHelper.showError(tvItemName, "Invalid University ID. Please enter a valid University ID.");
                    return;
                }
                if (ownerUniversityId.equals(currentUnivId)) {
                    ErrorHelper.showError(tvItemName, "You cannot mark yourself as the owner");
                    return;
                }
                
                User receiver = users.get(0);
                String recipientAuthId = receiver.getAuthId();

                SupabaseDatabaseHelper.select("profiles", "university_id=eq." + currentUnivId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
                    @Override
                    public void onSuccess(List<User> reporterList) {
                        User reporter = (reporterList != null && !reporterList.isEmpty()) ? reporterList.get(0) : null;
                        String reporterName = (reporter != null) ? reporter.getName() : "A user";
                        String reporterPhone = (currentItem != null && currentItem.getUserPhone() != null && !currentItem.getUserPhone().isEmpty()) ? currentItem.getUserPhone() : ((reporter != null) ? reporter.getPhone() : "");
                        String reporterEmail = (currentItem != null && currentItem.getUserEmail() != null && !currentItem.getUserEmail().isEmpty()) ? currentItem.getUserEmail() : ((reporter != null) ? reporter.getEmail() : "");

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("admin_status", "Returned");
                        updates.put("claimed_by_id", ownerUniversityId);
                        updates.put("status", "resolved");

                        SupabaseDatabaseHelper.update("found_reports", "id=eq." + itemId, updates, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                            @Override
                            public void onSuccess(String result) {
                                String notificationId = UUID.randomUUID().toString();
                                String itemName = tvItemName.getText().toString();
                                String type = "item_return";
                                String message = String.format("\"%s\" has marked that they returned \"%s\" from you. Click to view details.", reporterName, itemName);
                                String senderImg = (reporter != null) ? reporter.getProfileImageUrl() : "";

                                Notification notification = new Notification(notificationId, ownerUniversityId, currentUnivId, reporterName, reporterPhone, reporterEmail, senderImg, itemId, itemName, message, System.currentTimeMillis(), type, "");
                                notification.setItemName(itemName); // Ensure itemName is explicitly set
                                notification.setUserId(recipientAuthId);

                                SupabaseDatabaseHelper.insert("notifications", notification, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                                    @Override public void onSuccess(String r) {
                                        SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Item marked as Resolved");
                                        startListeningToItemChanges();
                                    }
                                    @Override public void onFailure(String e) {
                                        SnackbarManager.show(SnackbarManager.Type.ERROR, "Item resolved, but notification failed: " + e);
                                        startListeningToItemChanges();
                                    }
                                });
                            }
                            @Override public void onFailure(String e) { ErrorHelper.showError(tvItemName, "Update failed: " + e); }
                        });
                    }
                    @Override public void onFailure(String e) {}
                });
            }
            @Override public void onFailure(String e) { ErrorHelper.showError(tvItemName, "Error checking user: " + e); }
        });
    }

    private void handleClaim(String itemId, String itemName, String itemStatus, String reporterId) {
        if (currentUnivId == null) return;
        btnClaim.setEnabled(false);
        btnClaim.setText("Sending...");

        // Fetch recipient's Auth ID for RLS
        SupabaseDatabaseHelper.select("profiles", "university_id=eq." + reporterId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
            @Override
            public void onSuccess(List<User> recipients) {
                if (recipients == null || recipients.isEmpty()) { resetClaimButton(itemStatus); return; }
                String recipientAuthId = recipients.get(0).getAuthId();

                SupabaseDatabaseHelper.select("profiles", "university_id=eq." + currentUnivId + "&limit=1", new TypeToken<List<User>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<User>>() {
                    @Override
                    public void onSuccess(List<User> users) {
                        if (users == null || users.isEmpty()) { resetClaimButton(itemStatus); return; }
                        User sender = users.get(0);
                        String notificationId = UUID.randomUUID().toString();
                        String type = "lost".equalsIgnoreCase(itemStatus) ? "lost_claim" : "found_claim";
                        String message;
                        if ("lost_claim".equals(type)) {
                            message = "\"" + sender.getName() + "\" has claimed that they found your \"" + itemName + "\". Click to view details";
                        } else {
                            message = "\"" + sender.getName() + "\" has claimed that the item \"" + itemName + "\" belongs to them. Click to view details";
                        }

                        Notification notification = new Notification(notificationId, reporterId, currentUnivId, sender.getName(), sender.getPhone(), sender.getEmail(), sender.getProfileImageUrl(), itemId, itemName, message, System.currentTimeMillis(), type, "");
                        notification.setUserId(recipientAuthId); // Set for RLS

                        SupabaseDatabaseHelper.insert("notifications", notification, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
                            @Override public void onSuccess(String result) {
                                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Request sent successfully!");
                                btnClaim.setText("Request Sent");
                                btnClaim.setEnabled(false);
                            }
                            @Override public void onFailure(String e) { ErrorHelper.showError(btnClaim, "Failed: " + e); resetClaimButton(itemStatus); }
                        });
                    }
                    @Override public void onFailure(String e) { resetClaimButton(itemStatus); }
                });
            }
            @Override public void onFailure(String e) { resetClaimButton(itemStatus); }
        });
    }

    private void resetClaimButton(String itemStatus) {
        btnClaim.setEnabled(true);
        btnClaim.setText("lost".equalsIgnoreCase(itemStatus) ? "I Found This Item" : "This is Mine");
    }

    private void checkIfAlreadyClaimed(String itemId, String reporterId) {
        if (currentUnivId == null || itemId == null) return;
        if (currentUnivId.equals(reporterId)) { btnClaim.setVisibility(View.GONE); return; }
        SupabaseDatabaseHelper.select("notifications", "sender_id=eq." + currentUnivId + "&report_id=eq." + itemId, new TypeToken<List<Notification>>(){}.getType(), new SupabaseDatabaseHelper.DatabaseCallback<List<Notification>>() {
            @Override public void onSuccess(List<Notification> res) { if (res != null && !res.isEmpty()) { btnClaim.setEnabled(false); btnClaim.setText("Request Sent"); } }
            @Override public void onFailure(String e) {}
        });
    }

    private void deleteItem(String itemId, String status) {
        new AlertDialog.Builder(this).setTitle("Delete Report").setMessage("Permanently delete this report?").setPositiveButton("Delete", (dialog, which) -> {
            if (currentItem != null) deleteItemImages(currentItem);
            String table = "lost".equalsIgnoreCase(status) ? "lost_reports" : "found_reports";
            SupabaseDatabaseHelper.delete(table, "id=eq." + itemId, new SupabaseDatabaseHelper.DatabaseCallback<Void>() {
                @Override public void onSuccess(Void r) { 
                    SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Report deleted successfully"); 
                    finish(); 
                }
                @Override public void onFailure(String e) { 
                    android.util.Log.e("ItemDetail", "Delete failed for " + table + " with ID " + itemId + ": " + e);
                    ErrorHelper.showError(tvItemName, "Delete failed: " + e); 
                }
            });
        }).setNegativeButton("Cancel", null).show();
    }

    private void deleteItemImages(Item item) {
        List<String> urls = item.getImageUrls();
        if (urls == null) urls = new ArrayList<>();
        if (item.getImageUrl() != null && !urls.contains(item.getImageUrl())) urls.add(item.getImageUrl());
        for (String url : urls) { if (url != null && url.contains("supabase.co")) SupabaseStorageHelper.deleteImage(url, null); }
    }
}
