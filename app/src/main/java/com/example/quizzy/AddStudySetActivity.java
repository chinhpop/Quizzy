package com.example.quizzy;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AddStudySetActivity extends AppCompatActivity {
    private LinearLayout termList;
    private Button btnAddTerm, btnCreate;
    private EditText etSetTitle;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_study_set);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to create a study set", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        termList = findViewById(R.id.termList);
        btnAddTerm = findViewById(R.id.btnAddTerm);
        btnCreate = findViewById(R.id.btnCreate);
        etSetTitle = findViewById(R.id.etSetTitle);

        // Add back button functionality
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            DialogUtils.showConfirmationDialog(
                    this,
                    "Confirm",
                    "Do you want to cancel creating the study set?",
                    "Yes",
                    "No",
                    (dialog, which) -> finish(),
                    (dialog, which) -> dialog.dismiss()
            );
        });

        // Add first flashcard input field by default
        addTermView();

        btnAddTerm.setOnClickListener(v -> addTermView());

        btnCreate.setOnClickListener(v -> {
            String setTitle = etSetTitle.getText().toString().trim();

            if (setTitle.isEmpty()) {
                Toast.makeText(this, "Please enter a title", Toast.LENGTH_SHORT).show();
                return;
            }

            List<FlashCard> flashCards = new ArrayList<>();
            for (int i = 0; i < termList.getChildCount(); i++) {
                View termView = termList.getChildAt(i);
                EditText etFront = termView.findViewById(R.id.etTerm);
                EditText etBack = termView.findViewById(R.id.etDefinition);

                String front = etFront.getText().toString().trim();
                String back = etBack.getText().toString().trim();

                if (!front.isEmpty() && !back.isEmpty()) {
                    flashCards.add(new FlashCard(front, back));
                }
            }

            if (flashCards.isEmpty()) {
                Toast.makeText(this, "Please add at least one valid flashcard", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save study set to Firestore
            saveStudySetToFirestore(setTitle, flashCards);
        });
    }

    private void addTermView() {
        View termView = getLayoutInflater().inflate(R.layout.item_term, null, false);

        ImageView btnRemove = termView.findViewById(R.id.btnRemove);
        btnRemove.setOnClickListener(v -> termList.removeView(termView));

        // Set margins for the term view
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) (8 * getResources().getDisplayMetrics().density); // 8dp margin
        params.setMargins(0, margin, 0, margin);
        termView.setLayoutParams(params);

        termList.addView(termView);
    }

    private void saveStudySetToFirestore(String title, List<FlashCard> flashCards) {
        String userId = mAuth.getCurrentUser().getUid();
        String studySetId = UUID.randomUUID().toString();

        // Get username from Firestore
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String username = documentSnapshot.getString("username");
                    if (username == null) {
                        username = "Unknown";
                    }

                    // Create study set data
                    Map<String, Object> studySet = new HashMap<>();
                    studySet.put("title", title);
                    studySet.put("termCount", flashCards.size());
                    studySet.put("accuracy", 0.0);
                    studySet.put("username", username);

                    // Save study set
                    db.collection("users").document(userId).collection("study_sets").document(studySetId)
                            .set(studySet, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                // Save flashcards to a subcollection
                                for (FlashCard flashCard : flashCards) {
                                    Map<String, Object> flashCardData = new HashMap<>();
                                    flashCardData.put("front", flashCard.getFront());
                                    flashCardData.put("back", flashCard.getBack());

                                    db.collection("users").document(userId)
                                            .collection("study_sets").document(studySetId)
                                            .collection("flashcards").document(UUID.randomUUID().toString())
                                            .set(flashCardData, SetOptions.merge());
                                }

                                Toast.makeText(this, "Study set created with " + flashCards.size() + " flashcards", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error creating study set: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error retrieving username: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}