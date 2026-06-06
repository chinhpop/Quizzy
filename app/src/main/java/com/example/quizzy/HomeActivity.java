package com.example.quizzy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private StudySetAdapter adapter;
    private StudySetModel viewModel;
    private FloatingActionButton addButton;
    private TextView avatarTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() == null) {
            Intent intent = new Intent(HomeActivity.this, LaunchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.home_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.studySetList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        avatarTextView = findViewById(R.id.avatarTextView);

        // Load user info
        FirebaseUser user = mAuth.getCurrentUser();
        String displayName = user.getDisplayName();
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String username = documentSnapshot.getString("username");
                    String finalUsername = username != null && !username.isEmpty() ? username : (displayName != null ? displayName : "Unknown");
                    if (avatarTextView != null) {
                        avatarTextView.setText(finalUsername.isEmpty() ? "U" : finalUsername.substring(0, 1).toUpperCase());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeActivity", "Failed to load username: " + e.getMessage());
                    if (avatarTextView != null) {
                        avatarTextView.setText(displayName != null && !displayName.isEmpty() ? displayName.substring(0, 1).toUpperCase() : "U");
                    }
                });

        // Set up avatar click listener to open ProfileActivity
        if (avatarTextView != null) {
            avatarTextView.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        } else {
            Log.e("HomeActivity", "avatarTextView is null");
        }

        addButton = findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AddStudySetActivity.class);
            startActivity(intent);
        });

        viewModel = new ViewModelProvider(this).get(StudySetModel.class);
        viewModel.getStudySets().observe(this, studySets -> {
            adapter = new StudySetAdapter(studySets,
                    setId -> {
                        Intent intent = new Intent(this, FlashcardActivity.class);
                        intent.putExtra("setId", setId);
                        startActivity(intent);
                    },
                    setId -> {
                        Intent intent = new Intent(this, EditStudySetActivity.class);
                        intent.putExtra("setId", setId);
                        startActivity(intent);
                    });
            recyclerView.setAdapter(adapter);
        });
    }
}