package com.example.womensafteyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    EditText email, password;
    Button loginBtn, googleBtn;
    TextView signupText;
    FirebaseAuth auth;
    GoogleSignInClient googleSignInClient;
    FirebaseFirestore db;

    // ── Google Sign-In launcher ──
    ActivityResultLauncher<Intent> googleLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Task<GoogleSignInAccount> task =
                        GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    Toast.makeText(this, "Google sign-in failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        email      = findViewById(R.id.email);
        password   = findViewById(R.id.password);
        loginBtn   = findViewById(R.id.loginBtn);
        googleBtn  = findViewById(R.id.googleBtn);
        signupText = findViewById(R.id.signupText);

        // ── Skip login if already logged in ──
        if (auth.getCurrentUser() != null) {
            checkSetupAndNavigate(auth.getCurrentUser().getUid());
            return;
        }

        // ── Google Sign-In setup ──
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // ── Email/Password Login ──
        loginBtn.setOnClickListener(v -> {
            String userEmail = email.getText().toString().trim();
            String userPass  = password.getText().toString().trim();

            if (TextUtils.isEmpty(userEmail)) {
                email.setError("Enter your email"); email.requestFocus(); return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                email.setError("Enter a valid email address"); email.requestFocus(); return;
            }
            if (TextUtils.isEmpty(userPass)) {
                password.setError("Enter your password"); password.requestFocus(); return;
            }
            if (userPass.length() < 6) {
                password.setError("Password must be at least 6 characters"); password.requestFocus(); return;
            }

            loginBtn.setEnabled(false);
            loginBtn.setText("Logging in...");

            auth.signInWithEmailAndPassword(userEmail, userPass)
                    .addOnCompleteListener(task -> {
                        loginBtn.setEnabled(true);
                        loginBtn.setText("Log In");

                        if (task.isSuccessful()) {
                            checkSetupAndNavigate(auth.getCurrentUser().getUid());
                        } else {
                            Exception e = task.getException();
                            if (e instanceof FirebaseAuthInvalidUserException) {
                                email.setError("No account found with this email");
                                email.requestFocus();
                            } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                password.setError("Incorrect password");
                                password.requestFocus();
                            } else {
                                String msg = e != null ? e.getMessage() : "Login failed";
                                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        });

        // ── Google Login ──
        googleBtn.setOnClickListener(v -> {
            googleSignInClient.signOut().addOnCompleteListener(task -> {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                googleLauncher.launch(signInIntent);
            });
        });

        signupText.setOnClickListener(v ->
                startActivity(new Intent(this, SignupActivity.class))
        );
    }

    // ── Check if user has completed setup ──
    private void checkSetupAndNavigate(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && Boolean.TRUE.equals(doc.getBoolean("setupDone"))) {
                        // Setup already done → go to main screen
                        startActivity(new Intent(this, MainActivity.class));
                    } else {
                        // First time → go to setup screen
                        startActivity(new Intent(this, UserSetupActivity.class));
                    }
                    finish();
                })
                .addOnFailureListener(e -> {
                    // On failure default to setup screen
                    startActivity(new Intent(this, UserSetupActivity.class));
                    finish();
                });
    }

    // ── Authenticate with Firebase using Google token ──
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkSetupAndNavigate(auth.getCurrentUser().getUid());
                    } else {
                        String msg = task.getException() != null ?
                                task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}