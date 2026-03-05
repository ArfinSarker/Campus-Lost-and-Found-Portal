package com.sas.lostandfound;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class LostAndFoundPolicyActivity extends AppCompatActivity {

    private LinearLayout llPolicyEmail, llPolicyPhone, llPolicyAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lost_and_found_policy);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        llPolicyEmail = findViewById(R.id.llPolicyEmail);
        llPolicyPhone = findViewById(R.id.llPolicyPhone);
        llPolicyAddress = findViewById(R.id.llPolicyAddress);

        llPolicyEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + getString(R.string.policy_email_address)));
            startActivity(intent);
        });

        llPolicyPhone.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + getString(R.string.policy_phone_number)));
            startActivity(intent);
        });

        llPolicyAddress.setOnClickListener(v -> {
            String query = getString(R.string.policy_address_query);
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            if (mapIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                // Fallback to browser if no maps app
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, 
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query)));
                startActivity(browserIntent);
            }
        });
    }
}
