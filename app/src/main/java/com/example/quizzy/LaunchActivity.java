package com.example.quizzy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;
import androidx.viewpager2.widget.ViewPager2;
import java.util.Arrays;
import java.util.List;

@SuppressLint("CustomSplashScreen")
public class LaunchActivity extends AppCompatActivity {

    private static final List<SlideItem> SLIDE_ITEMS = Arrays.asList(
            new SlideItem(R.drawable.study1, "Almost students used to use <b>Quizzy</b>."),
            new SlideItem(R.drawable.study2, "Study flashcards in a fluid and intuitive way."),
            new SlideItem(R.drawable.study3, "Customize flashcards to your exact needs."),
            new SlideItem(R.drawable.study4, "<b>Quizzy</b> is much simpler than Quizlet.")
    );

    private LinearLayout dotsLayout;
    private TextView descriptionTextView;
    private ImageView[] dots;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        dotsLayout = findViewById(R.id.dotsLayout);
        descriptionTextView = findViewById(R.id.textView_description);

        // Set up ViewPager with adapter
        viewPager.setAdapter(new ViewPagerAdapter(SLIDE_ITEMS));

        // Initialize dots and description
        setupDots();
        setupNavigation();

        // Update UI when page changes
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                updateDescription(position);
            }
        });
    }

    private void setupDots() {
        dots = new ImageView[SLIDE_ITEMS.size()];
        dotsLayout.removeAllViews();

        for (int i = 0; i < SLIDE_ITEMS.size(); i++) {
            dots[i] = new ImageView(this);
            dots[i].setImageResource(i == 0 ? R.drawable.dot_active : R.drawable.dot_inactive);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(8, 0, 8, 0);
            dots[i].setLayoutParams(params);
            dotsLayout.addView(dots[i]);
        }

        // Set initial description
        updateDescription(0);
    }

    private void setupNavigation() {
        findViewById(R.id.signUpButton).setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
        findViewById(R.id.loginText).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
    }

    private void updateDots(int position) {
        for (int i = 0; i < dots.length; i++) {
            dots[i].setImageResource(i == position ? R.drawable.dot_active : R.drawable.dot_inactive);
        }
    }

    private void updateDescription(int position) {
        descriptionTextView.setText(HtmlCompat.fromHtml(
                SLIDE_ITEMS.get(position).getDescription(),
                HtmlCompat.FROM_HTML_MODE_LEGACY
        ));
    }

    // Inner class for slide data
    public static class SlideItem {
        private final int imageResId;
        private final String description;

        public SlideItem(int imageResId, String description) {
            this.imageResId = imageResId;
            this.description = description;
        }

        public int getImageResId() {
            return imageResId;
        }

        public String getDescription() {
            return description;
        }
    }
}