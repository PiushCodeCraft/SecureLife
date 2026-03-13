package com.example.womensafteyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;


public class LoginActivity extends AppCompatActivity {

    EditText email, password;
    TextView signupText;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        email      = findViewById(R.id.email);
        password   = findViewById(R.id.password);
        loginBtn   = findViewById(R.id.loginBtn);
        signupText = findViewById(R.id.signupText);

        loginBtn.setOnClickListener(v -> {
            String userEmail = email.getText().toString().trim();
            String userPass  = password.getText().toString().trim();

            if (TextUtils.isEmpty(userEmail)) {
            }
            if (TextUtils.isEmpty(userPass)) {
            }

            auth.signInWithEmailAndPassword(userEmail, userPass)
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {
                            finish();
                        } else {
                        }

            });
        });


                });
    }
}