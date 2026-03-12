package com.example.womensafteyapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SignupActivity extends AppCompatActivity {

    EditText email, password;
    Button signupBtn;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        signupBtn = findViewById(R.id.signupBtn);

        auth = FirebaseAuth.getInstance();

        signupBtn.setOnClickListener(v -> {

            String userEmail = email.getText().toString();
            String userPass = password.getText().toString();

            auth.createUserWithEmailAndPassword(userEmail, userPass)
                    .addOnCompleteListener(task -> {

                        if(task.isSuccessful()){

                            Toast.makeText(this,"Account Created",Toast.LENGTH_SHORT).show();

                            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                            finish();

                        } else {

                            Toast.makeText(this,"Signup Failed",Toast.LENGTH_SHORT).show();

                        }
                    });

        });

    }
}