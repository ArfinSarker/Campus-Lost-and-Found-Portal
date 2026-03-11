package com.sas.lostandfound;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

public class ClaimDetailsActivity extends AppCompatActivity {

    private TextView tvName, tvPhone, tvEmail, tvItemName, tvAdditionalDetails, labelAdditionalDetails;
    private MaterialButton btnCall, btnEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_claim_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        tvName = findViewById(R.id.tvClaimantName);
        tvPhone = findViewById(R.id.tvClaimantPhone);
        tvEmail = findViewById(R.id.tvClaimantEmail);
        tvItemName = findViewById(R.id.tvClaimedItemName);
        tvAdditionalDetails = findViewById(R.id.tvAdditionalDetails);
        labelAdditionalDetails = findViewById(R.id.labelAdditionalDetails);
        btnCall = findViewById(R.id.btnCall);
        btnEmail = findViewById(R.id.btnEmail);

        String name = getIntent().getStringExtra("senderName");
        String phone = getIntent().getStringExtra("senderPhone");
        String email = getIntent().getStringExtra("senderEmail");
        String itemName = getIntent().getStringExtra("itemName");
        String additionalDetails = getIntent().getStringExtra("additionalDetails");

        tvName.setText(name);
        tvPhone.setText(phone);
        tvEmail.setText(email);
        tvItemName.setText(itemName);

        if (additionalDetails != null && !additionalDetails.isEmpty()) {
            tvAdditionalDetails.setText(additionalDetails);
            tvAdditionalDetails.setVisibility(View.VISIBLE);
            labelAdditionalDetails.setVisibility(View.VISIBLE);
        } else {
            tvAdditionalDetails.setVisibility(View.GONE);
            labelAdditionalDetails.setVisibility(View.GONE);
        }

        btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            startActivity(intent);
        });

        btnEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + email));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Regarding your claim for " + itemName);
            startActivity(Intent.createChooser(intent, "Send Email"));
        });
        
        tvPhone.setOnClickListener(v -> btnCall.performClick());
        tvEmail.setOnClickListener(v -> btnEmail.performClick());
    }
}
