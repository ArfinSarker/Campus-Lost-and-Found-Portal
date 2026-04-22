package com.sas.lostandfound;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.ArrayList;
import java.util.List;

public class FullScreenImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen_image);

        ViewPager2 viewPager = findViewById(R.id.viewPagerFull);
        TabLayout tabLayout = findViewById(R.id.tabLayoutFull);
        ImageView btnBack = findViewById(R.id.btnBackFull);

        List<String> imageUrls = getIntent().getStringArrayListExtra("imageUrls");
        int position = getIntent().getIntExtra("position", 0);

        if (imageUrls == null || imageUrls.isEmpty()) {
            finish();
            return;
        }

        ImageSliderAdapter adapter = new ImageSliderAdapter(imageUrls, true);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position, false);

        if (imageUrls.size() > 1) {
            new TabLayoutMediator(tabLayout, viewPager, (tab, pos) -> {}).attach();
        } else {
            tabLayout.setVisibility(android.view.View.GONE);
        }

        btnBack.setOnClickListener(v -> finish());
    }
}
