package com.example.quizzy;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText emailEditText, usernameEditText, passwordEditText, confirmPasswordEditText;
    private Button signupButton;
    private ImageButton backBtn;
    private ImageView passwordToggle, confirmPasswordToggle;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private boolean isLoading = false;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Handle window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.profile_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        emailEditText = findViewById(R.id.signup_emailEditText);
        usernameEditText = findViewById(R.id.signup_usernameEditText);
        passwordEditText = findViewById(R.id.signup_passwordEditText);
        confirmPasswordEditText = findViewById(R.id.signup_confirmPasswordEditText);
        signupButton = findViewById(R.id.signupButton);
        backBtn = findViewById(R.id.backButton_signup);
        passwordToggle = findViewById(R.id.passwordToggle);
        confirmPasswordToggle = findViewById(R.id.confirmPasswordToggle);

        // Set up password toggle click listener
        passwordToggle.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                passwordEditText.setTransformationMethod(SingleLineTransformationMethod.getInstance());
                passwordToggle.setImageResource(R.drawable.ic_eye_on);
            } else {
                passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                passwordToggle.setImageResource(R.drawable.ic_eye_off);
            }
            // Move cursor to the end of the text
            passwordEditText.setSelection(passwordEditText.getText().length());
        });

        // Set up confirm password toggle click listener
        confirmPasswordToggle.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            if (isConfirmPasswordVisible) {
                confirmPasswordEditText.setTransformationMethod(SingleLineTransformationMethod.getInstance());
                confirmPasswordToggle.setImageResource(R.drawable.ic_eye_on);
            } else {
                confirmPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                confirmPasswordToggle.setImageResource(R.drawable.ic_eye_off);
            }
            // Move cursor to the end of the text
            confirmPasswordEditText.setSelection(confirmPasswordEditText.getText().length());
        });

        backBtn.setOnClickListener(v -> {
            finish(); // Quay lại activity trước đó
        });

        // Set up signup button click listener
        signupButton.setOnClickListener(v -> {
            if (isLoading) return;

            String email = emailEditText.getText().toString().trim();
            String username = usernameEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            // Input validation
            if (email.isEmpty()) {
                emailEditText.setError("Email is required");
                emailEditText.requestFocus();
                return;
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailEditText.setError("Enter a valid email address");
                emailEditText.requestFocus();
                return;
            }

            if (username.isEmpty()) {
                usernameEditText.setError("Username is required");
                usernameEditText.requestFocus();
                return;
            }

            if (password.isEmpty()) {
                passwordEditText.setError("Password is required");
                passwordEditText.requestFocus();
                return;
            }

            if (password.length() < 6) {
                passwordEditText.setError("Password must be at least 6 characters");
                passwordEditText.requestFocus();
                return;
            }

            if (!password.equals(confirmPassword)) {
                confirmPasswordEditText.setError("Passwords do not match");
                confirmPasswordEditText.requestFocus();
                return;
            }

            // Create user and save username
            isLoading = true;
            signupButton.setEnabled(false);

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            // Save username to Firestore
                            String userId = mAuth.getCurrentUser().getUid();
                            Map<String, Object> user = new HashMap<>();
                            user.put("username", username);

                            db.collection("users").document(userId)
                                    .set(user, SetOptions.merge())
                                    .addOnCompleteListener(dbTask -> {
                                        isLoading = false;
                                        signupButton.setEnabled(true);

                                        if (dbTask.isSuccessful()) {
                                            Toast.makeText(SignUpActivity.this, "Sign Up Successful", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                            finish();
                                        } else {
                                            Toast.makeText(SignUpActivity.this, "Failed to save username: " + dbTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                                        }
                                    });
                        } else {
                            isLoading = false;
                            signupButton.setEnabled(true);

                            String errorMessage;
                            if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                errorMessage = "Email is already registered";
                            } else {
                                errorMessage = task.getException() != null ? task.getException().getMessage() : "Sign Up Failed";
                            }

                            Toast.makeText(SignUpActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}