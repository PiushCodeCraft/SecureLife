package com.example.womensafteyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SignupActivity extends AppCompatActivity {

    EditText email, password, confirmPassword;
    Button signupBtn;
    TextView loginText;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();

        email           = findViewById(R.id.email);
        password        = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        signupBtn       = findViewById(R.id.signupBtn);
        loginText       = findViewById(R.id.loginText);

        signupBtn.setOnClickListener(v -> {

            String userEmail   = email.getText().toString().trim();
            String userPass    = password.getText().toString().trim();
            String confirmPass = confirmPassword.getText().toString().trim();

            // ── Validation ──
            if (TextUtils.isEmpty(userEmail)) {
                email.setError("Enter your email");
                email.requestFocus();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(userEmail).matches()) {
                email.setError("Enter a valid email address");
                email.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(userPass)) {
                password.setError("Enter a password");
                password.requestFocus();
                return;
            }

            if (userPass.length() < 6) {
                password.setError("Password must be at least 6 characters");
                password.requestFocus();
                return;
            }

            if (TextUtils.isEmpty(confirmPass)) {
                confirmPassword.setError("Confirm your password");
                confirmPassword.requestFocus();
                return;
            }

            if (!userPass.equals(confirmPass)) {
                confirmPassword.setError("Passwords do not match");
                confirmPassword.requestFocus();
                return;
            }

            // ── Firebase Signup ──
            signupBtn.setEnabled(false);
            signupBtn.setText("Creating account...");

            auth.createUserWithEmailAndPassword(userEmail, userPass)
                    .addOnCompleteListener(task -> {

                        signupBtn.setEnabled(true);
                        signupBtn.setText("Create Account");

                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                            finish();
                        } else {
                            // ── Show exact error reason ──
                            String error = task.getException() != null
                                    ? task.getException().getMessage()
                                    : "Signup failed";
                            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // ── Go back to Login ──
        loginText.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }
}