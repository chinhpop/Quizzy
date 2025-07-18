package com.example.quizzy;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FlashcardActivity extends AppCompatActivity {
    private TextView frontText, backText, tvCounter;
    private FrameLayout flashcard;
    private ImageView btnPrev, btnNext;
    private List<FlashCard> flashcards;
    private int currentIndex = 0;
    private boolean isFrontVisible = true;
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flashcard);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        flashcards = new ArrayList<>();

        // Validate user login
        if (!validateUser()) {
            return;
        }

        // Get setId from Intent
        String setId = getIntent().getStringExtra("setId");
        if (setId == null) {
            Toast.makeText(this, "Study set not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        initializeViews();

        // Set up button listeners
        setupButtonListeners();

        // Load flashcards
        loadFlashcards(setId);

        // Set camera distance for smoother 3D rotation
        float scale = getResources().getDisplayMetrics().density;
        flashcard.setCameraDistance(10000 * scale);
    }

    private boolean validateUser() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to view flashcards", Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        return true;
    }

    private void initializeViews() {
        frontText = findViewById(R.id.frontText);
        backText = findViewById(R.id.backText);
        flashcard = findViewById(R.id.flashcard);
        tvCounter = findViewById(R.id.tvCounter);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);
    }

    private void setupButtonListeners() {
        ImageView backButton = findViewById(R.id.backButton_signup);
        backButton.setOnClickListener(v -> finish());

        flashcard.setOnClickListener(v -> flipCard());

        btnNext.setOnClickListener(v -> {
            if (currentIndex < flashcards.size() - 1) {
                currentIndex++;
                showCard(currentIndex);
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                showCard(currentIndex);
            }
        });
    }

    private void loadFlashcards(String setId) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId)
                .collection("study_sets").document(setId)
                .collection("flashcards")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    flashcards.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String front = doc.getString("front");
                        String back = doc.getString("back");
                        if (front != null && back != null) {
                            flashcards.add(new FlashCard(front, back));
                        }
                    }
                    if (flashcards.isEmpty()) {
                        Toast.makeText(this, "No flashcards in this study set", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        showCard(0);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading flashcards: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    finish();
                });
    }

    private void showCard(int index) {
        if (index < 0 || index >= flashcards.size()) {
            return;
        }
        FlashCard card = flashcards.get(index);
        frontText.setText(card.getFront());
        backText.setText(card.getBack());
        tvCounter.setText(String.format("%d / %d", index + 1, flashcards.size()));

        frontText.setVisibility(isFrontVisible ? View.VISIBLE : View.GONE);
        backText.setVisibility(isFrontVisible ? View.GONE : View.VISIBLE);
        flashcard.setRotationY(isFrontVisible ? 0f : 180f);
    }

    private void flipCard() {
        FlipAnimation.flipCard(flashcard, frontText, backText, !isFrontVisible);
        isFrontVisible = !isFrontVisible;
    }
}