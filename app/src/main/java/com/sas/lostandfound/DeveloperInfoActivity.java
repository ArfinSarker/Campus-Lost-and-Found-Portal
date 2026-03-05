package com.sas.lostandfound;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class DeveloperInfoActivity extends AppCompatActivity {

    private LinearLayout llEmail, llPhone, llGithub, llLinkedin;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_developer_info);

        btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> onBackPressed());
        }

        llEmail = findViewById(R.id.llEmail);
        llPhone = findViewById(R.id.llPhone);
        llGithub = findViewById(R.id.llGithub);
        llLinkedin = findViewById(R.id.llLinkedin);

        llEmail.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:m.shamsularfinsarkernayan@gmail.com"));
            startActivity(intent);
        });

        llPhone.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+880819966626"));
            startActivity(intent);
        });

        llGithub.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://github.com/ArfinSarker"));
            startActivity(intent);
        });

        llLinkedin.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://bd.linkedin.com/in/arfinsarkar"));
            startActivity(intent);
        });
    }
}
