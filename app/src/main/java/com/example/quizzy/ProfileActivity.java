package com.example.quizzy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private TextView usernameTextView, emailTextView;
    private Button editButton, logoutButton;
    private ImageButton backButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

        initializeFirebase();
        initializeViews();
        setupEdgeToEdge();
        setupClickListeners();
        loadUserProfile();
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    private void initializeViews() {
        usernameTextView = findViewById(R.id.profile_usernameTextView);
        emailTextView = findViewById(R.id.profile_emailTextView);
        editButton = findViewById(R.id.profile_editButton);
        logoutButton = findViewById(R.id.profile_logoutButton);
        backButton = findViewById(R.id.backButton_profile);
    }

    private void setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        editButton.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));

        logoutButton.setOnClickListener(v -> {
            firebaseAuth.signOut();
            Intent intent = new Intent(this, LaunchActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUserProfile() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            showToast("No user logged in", Toast.LENGTH_SHORT);
            finish();
            return;
        }

        emailTextView.setText(user.getEmail());
        firestore.collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String username = documentSnapshot.getString("username");
                    usernameTextView.setText(username != null ? username : "");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load username", e);
                    usernameTextView.setText("");
                    showToast("Failed to load profile data", Toast.LENGTH_SHORT);
                });
    }

    private void showToast(String message, int duration) {
        Toast.makeText(this, message, duration).show();
    }
}