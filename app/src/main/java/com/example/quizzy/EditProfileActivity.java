package com.example.quizzy;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {
    private EditText usernameEditText, emailEditText, currentPasswordEditText, newPasswordEditText;
    private Button saveButton;
    private ImageButton backButton;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Set up edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.edit_profile_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        initializeViews();

        // Load current user data
        loadUserData();

        // Set up button listeners
        setupButtonListeners();
    }

    private void initializeViews() {
        usernameEditText = findViewById(R.id.edit_profile_usernameEditText);
        emailEditText = findViewById(R.id.edit_profile_emailEditText);
        currentPasswordEditText = findViewById(R.id.edit_profile_currentPasswordEditText);
        newPasswordEditText = findViewById(R.id.edit_profile_newPasswordEditText);
        saveButton = findViewById(R.id.edit_profile_saveButton);
        backButton = findViewById(R.id.backButton_editProfile);

        // Ensure EditText fields are editable
        usernameEditText.setEnabled(true);
        emailEditText.setEnabled(true);
        currentPasswordEditText.setEnabled(true);
        newPasswordEditText.setEnabled(true);
    }

    private void loadUserData() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        emailEditText.setText(user.getEmail());
        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot ->
                        usernameEditText.setText(documentSnapshot.getString("username") != null
                                ? documentSnapshot.getString("username") : ""))
                .addOnFailureListener(e -> {
                    Log.e("EditProfileActivity", "Failed to load username: " + e.getMessage());
                    usernameEditText.setText("");
                });
    }

    private void setupButtonListeners() {
        backButton.setOnClickListener(v -> finish());

        saveButton.setOnClickListener(v -> {
            if (isLoading) return;
            DialogUtils.showConfirmationDialog(
                    this,
                    "Confirm Changes",
                    "Are you sure you want to save these changes?",
                    "Yes",
                    "No",
                    (dialog, which) -> saveProfileChanges(),
                    (dialog, which) -> dialog.dismiss()
            );
        });
    }

    private void saveProfileChanges() {
        String newUsername = usernameEditText.getText().toString().trim();
        String newEmail = emailEditText.getText().toString().trim();
        String currentPassword = currentPasswordEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(newUsername, newEmail, currentPassword, newPassword)) {
            return;
        }

        isLoading = true;
        saveButton.setEnabled(false);

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            handleError("No user logged in", null);
            return;
        }

        // Update only username if email and password are unchanged
        if (newEmail.equals(user.getEmail()) && newPassword.isEmpty()) {
            updateUsername(user, newUsername);
        } else {
            // Re-authenticate user for email or password changes
            reauthenticateAndUpdate(user, newUsername, newEmail, newPassword, currentPassword);
        }
    }

    private boolean validateInputs(String username, String email, String currentPassword, String newPassword) {
        if (username.isEmpty()) {
            usernameEditText.setError("Username is required");
            usernameEditText.requestFocus();
            return false;
        }

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Enter a valid email address");
            emailEditText.requestFocus();
            return false;
        }

        if ((!email.equals(auth.getCurrentUser().getEmail()) || !newPassword.isEmpty()) && currentPassword.isEmpty()) {
            currentPasswordEditText.setError("Current password is required to update email or password");
            currentPasswordEditText.requestFocus();
            return false;
        }

        if (!newPassword.isEmpty() && newPassword.length() < 6) {
            newPasswordEditText.setError("New password must be at least 6 characters");
            newPasswordEditText.requestFocus();
            return false;
        }

        return true;
    }

    private void reauthenticateAndUpdate(FirebaseUser user, String newUsername, String newEmail, String newPassword, String currentPassword) {
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential).addOnCompleteListener(reAuthTask -> {
            if (reAuthTask.isSuccessful()) {
                // Check if email is already in use
                auth.fetchSignInMethodsForEmail(newEmail).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean emailExists = !task.getResult().getSignInMethods().isEmpty();
                        if (emailExists && !newEmail.equals(user.getEmail())) {
                            handleError("Email is already registered", emailEditText);
                            return;
                        }
                        updateUsernameAndCredentials(user, newUsername, newEmail, newPassword);
                    } else {
                        handleError("Failed to check email availability: " + task.getException().getMessage(), null);
                    }
                });
            } else {
                handleReauthError(reAuthTask.getException());
            }
        });
    }

    private void updateUsernameAndCredentials(FirebaseUser user, String newUsername, String newEmail, String newPassword) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", newUsername);
        db.collection("users").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> updateEmailAndPassword(user, newEmail, newPassword))
                .addOnFailureListener(e -> handleError("Failed to update username: " + e.getMessage(), null));
    }

    private void updateUsername(FirebaseUser user, String newUsername) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", newUsername);
        db.collection("users").document(user.getUid())
                .set(userData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    handleSuccess("Profile updated successfully");
                })
                .addOnFailureListener(e -> handleError("Failed to update username: " + e.getMessage(), null));
    }

    private void updateEmailAndPassword(FirebaseUser user, String newEmail, String newPassword) {
        user.updateEmail(newEmail)
                .addOnCompleteListener(emailTask -> {
                    if (emailTask.isSuccessful()) {
                        if (!newPassword.isEmpty()) {
                            updatePassword(user, newPassword);
                        } else {
                            handleSuccess("Profile updated successfully");
                        }
                    } else {
                        if (emailTask.getException() instanceof FirebaseAuthUserCollisionException) {
                            handleError("Email is already registered", emailEditText);
                        } else {
                            handleError("Failed to update email: " + emailTask.getException().getMessage(), null);
                        }
                    }
                });
    }

    private void updatePassword(FirebaseUser user, String newPassword) {
        user.updatePassword(newPassword)
                .addOnCompleteListener(passwordTask -> {
                    if (passwordTask.isSuccessful()) {
                        handleSuccess("Profile updated successfully");
                    } else {
                        handleError("Failed to update password: " + passwordTask.getException().getMessage(), null);
                    }
                });
    }

    private void handleSuccess(String message) {
        isLoading = false;
        saveButton.setEnabled(true);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void handleError(String message, EditText editText) {
        isLoading = false;
        saveButton.setEnabled(true);
        if (editText != null) {
            editText.setError(message);
            editText.requestFocus();
        } else {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }
    }

    private void handleReauthError(Exception exception) {
        isLoading = false;
        saveButton.setEnabled(true);
        if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            currentPasswordEditText.setError("Incorrect current password");
            currentPasswordEditText.requestFocus();
        } else {
            Toast.makeText(this, "Re-authentication failed: " + exception.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}