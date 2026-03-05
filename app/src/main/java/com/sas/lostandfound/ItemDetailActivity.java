package com.sas.lostandfound;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class ItemDetailActivity extends AppCompatActivity {

    private ImageView ivItemImage, ivUserPhoto;
    private TextView tvStatus, tvItemName, tvCategory, tvDescription, tvLocation, tvDateTime, tvReporterName, tvReporterDept;
    private MaterialCardView cardBadge;
    private MaterialButton btnContact, btnClaim;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        initializeViews();
        setupToolbar();
        
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

        String fullDateTime = itemDate;
        if (itemTime != null && !itemTime.isEmpty()) {
            fullDateTime += " - " + itemTime;
        }

        displayItemDetails(itemName, itemDescription, itemLocation, fullDateTime, itemStatus, itemCategory, itemImageUrl, userName, userDept, userPhone);
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
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
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
        } else {
            tvStatus.setText("FOUND");
            cardBadge.setCardBackgroundColor(0xFFDCFCE7);
            tvStatus.setTextColor(0xFF2E7D32);
            btnClaim.setText("This is Mine");
        }

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

        btnClaim.setOnClickListener(v -> Toast.makeText(this, "Claim request sent!", Toast.LENGTH_SHORT).show());
    }
}
