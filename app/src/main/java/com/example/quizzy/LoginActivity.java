package com.example.quizzy;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView forgotPasswordTextView;
    private ImageButton backButton;
    private SignInButton googleSignInButton;
    private ImageView passwordToggle;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private boolean isLoading = false;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        initializeFirebase();
        initializeViews();
        setupEdgeToEdge();
        setupGoogleSignIn();
        setupClickListeners();
    }

    private void initializeFirebase() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        forgotPasswordTextView = findViewById(R.id.forgot_password);
        googleSignInButton = findViewById(R.id.googleBtn);
        backButton = findViewById(R.id.backButton_signup);
        passwordToggle = findViewById(R.id.passwordToggle);
    }

    private void setupEdgeToEdge() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    isLoading = false;
                    setButtonsEnabled(true);
                    if (result.getResultCode() == RESULT_OK) {
                        handleGoogleSignInResult(GoogleSignIn.getSignedInAccountFromIntent(result.getData()));
                    } else {
                        showToast("Google Sign-In cancelled", Toast.LENGTH_SHORT);
                    }
                });
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        passwordToggle.setOnClickListener(v -> togglePasswordVisibility());

        loginButton.setOnClickListener(v -> {
            if (!isLoading) {
                loginWithEmail();
            }
        });

        googleSignInButton.setOnClickListener(v -> {
            if (!isLoading) {
                startGoogleSignIn();
            }
        });

        forgotPasswordTextView.setOnClickListener(v -> {
            if (!isLoading) {
                showForgotPasswordDialog();
            }
        });
    }

    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        passwordEditText.setTransformationMethod(isPasswordVisible ?
                SingleLineTransformationMethod.getInstance() :
                PasswordTransformationMethod.getInstance());
        passwordToggle.setImageResource(isPasswordVisible ?
                R.drawable.ic_eye_on : R.drawable.ic_eye_off);
        passwordEditText.setSelection(passwordEditText.getText().length());
    }

    private void loginWithEmail() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!validateInputs(email, password)) {
            return;
        }

        isLoading = true;
        setButtonsEnabled(false);
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    isLoading = false;
                    setButtonsEnabled(true);
                    if (task.isSuccessful()) {
                        showToast("Login successful!", Toast.LENGTH_SHORT);
                        navigateToHome();
                    } else {
                        showToast("Authentication failed: " + task.getException().getMessage(), Toast.LENGTH_LONG);
                    }
                });
    }

    private boolean validateInputs(String email, String password) {
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return false;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return false;
        }
        return true;
    }

    private void startGoogleSignIn() {
        isLoading = true;
        setButtonsEnabled(false);
        googleSignInLauncher.launch(googleSignInClient.getSignInIntent());
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            firebaseAuthWithGoogle(account.getIdToken());
        } catch (ApiException e) {
            showToast("Google Sign-In failed: " + e.getMessage(), Toast.LENGTH_LONG);
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    isLoading = false;
                    setButtonsEnabled(true);
                    if (task.isSuccessful()) {
                        showToast("Google Sign-In successful!", Toast.LENGTH_SHORT);
                        navigateToHome();
                    } else {
                        showToast("Google Sign-In failed: " + task.getException().getMessage(), Toast.LENGTH_LONG);
                    }
                });
    }

    private void showForgotPasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_forgot, null);
        EditText dialogEmailEditText = dialogView.findViewById(R.id.emailBox);
        Button sendButton = dialogView.findViewById(R.id.btnReset);
        Button cancelButton = dialogView.findViewById(R.id.btnCancel);

        String currentEmail = emailEditText.getText().toString().trim();
        if (!currentEmail.isEmpty()) {
            dialogEmailEditText.setText(currentEmail);
            dialogEmailEditText.setSelection(currentEmail.length());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        sendButton.setOnClickListener(v -> {
            String email = dialogEmailEditText.getText().toString().trim();
            if (email.isEmpty()) {
                dialogEmailEditText.setError("Email is required");
                dialogEmailEditText.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                dialogEmailEditText.setError("Enter a valid email address");
                dialogEmailEditText.requestFocus();
                return;
            }

            isLoading = true;
            setButtonsEnabled(false);
            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        isLoading = false;
                        setButtonsEnabled(true);
                        dialog.dismiss();
                        String message = task.isSuccessful() ?
                                "Password reset email sent to " + email :
                                "Failed to send reset email: " + task.getException().getMessage();
                        showToast(message, Toast.LENGTH_LONG);
                    });
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void navigateToHome() {
        startActivity(new Intent(this, HomeActivity.class));
        finish();
    }

    private void setButtonsEnabled(boolean enabled) {
        loginButton.setEnabled(enabled);
        googleSignInButton.setEnabled(enabled);
        forgotPasswordTextView.setEnabled(enabled);
    }

    private void showToast(String message, int duration) {
        Toast.makeText(this, message, duration).show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (firebaseAuth.getCurrentUser() != null) {
            navigateToHome();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (googleSignInClient != null) {
            googleSignInClient.signOut();
        }
    }
}