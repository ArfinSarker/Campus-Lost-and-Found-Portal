package com.sas.lostandfound;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

public class ChatRequestActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ImageView ivItemPhoto;
    private TextView tvItemName, tvItemCategory, tvItemDescription, tvReporterName;
    private TextInputEditText etInitialMessage;
    private MaterialButton btnSubmitRequest;

    private String reportId, reporterId, itemName, itemDescription, itemImageUrl, reporterName;
    private String currentUnivId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_request);

        SharedPreferences prefs = getSharedPreferences("MyApp", MODE_PRIVATE);
        currentUnivId = prefs.getString("universityId", null);

        // Retrieve intent extras
        reportId = getIntent().getStringExtra("reportId");
        reporterId = getIntent().getStringExtra("reporterId");
        itemName = getIntent().getStringExtra("itemName");
        itemDescription = getIntent().getStringExtra("itemDescription");
        itemImageUrl = getIntent().getStringExtra("itemImageUrl");
        reporterName = getIntent().getStringExtra("reporterName");

        initializeViews();
        setupToolbar();
        displayDetails();

        btnSubmitRequest.setOnClickListener(v -> sendRequest());
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        ivItemPhoto = findViewById(R.id.ivItemPhoto);
        tvItemName = findViewById(R.id.tvItemName);
        tvItemCategory = findViewById(R.id.tvItemCategory);
        tvItemDescription = findViewById(R.id.tvItemDescription);
        tvReporterName = findViewById(R.id.tvReporterName);
        etInitialMessage = findViewById(R.id.etInitialMessage);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        
        com.google.android.material.appbar.AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        if (appBarLayout != null) {
            int headerColor = androidx.core.content.ContextCompat.getColor(this, R.color.chat_request_header_bg);
            boolean isNight = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            HeaderColorHelper.setup(this, appBarLayout, headerColor, headerColor, !isNight);
        }
    }

    private void displayDetails() {
        tvItemName.setText(itemName != null ? itemName : "Item Name");
        tvItemDescription.setText(itemDescription != null ? itemDescription : "No Description");
        tvReporterName.setText(reporterName != null ? reporterName : "Reporter Name");

        // Pre-fill initial message with a friendly prompt
        etInitialMessage.setText("Hi, I am interested in your report regarding this item.");

        if (itemImageUrl != null && !itemImageUrl.isEmpty()) {
            GlideApp.with(this)
                    .load(itemImageUrl)
                    .placeholder(R.drawable.ic_chat_request_placeholder)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivItemPhoto);
        } else {
            ivItemPhoto.setImageResource(R.drawable.ic_chat_request_placeholder);
        }
    }

    private void sendRequest() {
        String initialMessage = etInitialMessage.getText().toString().trim();
        if (TextUtils.isEmpty(initialMessage)) {
            etInitialMessage.setError("Please enter a message");
            return;
        }

        if (currentUnivId == null || reportId == null || reporterId == null) {
            SnackbarManager.show(SnackbarManager.Type.ERROR, "Submission error. Try logging in again.");
            return;
        }

        btnSubmitRequest.setEnabled(false);
        btnSubmitRequest.setText("Sending...");

        Map<String, Object> data = new HashMap<>();
        data.put("sender_id", currentUnivId);
        data.put("receiver_id", reporterId);
        data.put("report_id", reportId);
        data.put("initial_message", initialMessage);
        data.put("status", "pending");

        SupabaseDatabaseHelper.insert("chat_requests", data, new SupabaseDatabaseHelper.DatabaseCallback<String>() {
            @Override
            public void onSuccess(String result) {
                SnackbarManager.show(SnackbarManager.Type.SUCCESS, "Chat request sent!");
                UnreadBadgeHelper.sendBadgeUpdateBroadcast(ChatRequestActivity.this);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                btnSubmitRequest.setEnabled(true);
                btnSubmitRequest.setText("Send Chat Request");
                if (errorMessage.contains("unique_chat_request")) {
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "A chat request already exists for this report!");
                } else {
                    SnackbarManager.show(SnackbarManager.Type.ERROR, "Failed to send request: " + errorMessage);
                }
            }
        });
    }
}
