package com.example.womensafteyapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class UserSetupActivity extends AppCompatActivity {

    EditText etName, etAge;
    EditText etContact1Name, etContact1Number;
    EditText etContact2Name, etContact2Number;
    Button btnNext;

    FirebaseFirestore db;
    String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_setup);

        db     = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        etName          = findViewById(R.id.etName);
        etAge           = findViewById(R.id.etAge);
        etContact1Name  = findViewById(R.id.etContact1Name);
        etContact1Number= findViewById(R.id.etContact1Number);
        etContact2Name  = findViewById(R.id.etContact2Name);
        etContact2Number= findViewById(R.id.etContact2Number);
        btnNext         = findViewById(R.id.btnNext);

        btnNext.setOnClickListener(v -> {
            String name    = etName.getText().toString().trim();
            String age     = etAge.getText().toString().trim();
            String c1Name  = etContact1Name.getText().toString().trim();
            String c1Num   = etContact1Number.getText().toString().trim();
            String c2Name  = etContact2Name.getText().toString().trim();
            String c2Num   = etContact2Number.getText().toString().trim();

            // ── Validation ──
            if (TextUtils.isEmpty(name)) {
                etName.setError("Enter your name"); etName.requestFocus(); return;
            }
            if (TextUtils.isEmpty(age)) {
                etAge.setError("Enter your age"); etAge.requestFocus(); return;
            }
            if (TextUtils.isEmpty(c1Name)) {
                etContact1Name.setError("Enter contact 1 name"); etContact1Name.requestFocus(); return;
            }
            if (c1Num.length() < 10) {
                etContact1Number.setError("Enter valid phone number"); etContact1Number.requestFocus(); return;
            }
            if (TextUtils.isEmpty(c2Name)) {
                etContact2Name.setError("Enter contact 2 name"); etContact2Name.requestFocus(); return;
            }
            if (c2Num.length() < 10) {
                etContact2Number.setError("Enter valid phone number"); etContact2Number.requestFocus(); return;
            }

            // ── Save to Firestore ──
            btnNext.setEnabled(false);
            btnNext.setText("Saving...");

            Map<String, Object> userData = new HashMap<>();
            userData.put("name", name);
            userData.put("age", age);
            userData.put("contact1Name", c1Name);
            userData.put("contact1Number", c1Num);
            userData.put("contact2Name", c2Name);
            userData.put("contact2Number", c2Num);
            userData.put("setupDone", true);

            db.collection("users").document(userId)
                    .set(userData)
                    .addOnSuccessListener(unused -> {
                        // ── Save setupDone locally for instant login next time ──
                        getSharedPreferences("app_prefs", MODE_PRIVATE)
                                .edit()
                                .putBoolean("setupDone", true)
                                .apply();

                        // ── Also save contacts locally for PowerButtonReceiver ──
                        getSharedPreferences("sos_prefs", MODE_PRIVATE)
                                .edit()
                                .putString("contact1Number", c1Num)
                                .putString("contact2Number", c2Num)
                                .apply();

                        // ── Go to permissions screen ──
                        startActivity(new Intent(UserSetupActivity.this, PermissionsActivity.class));
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        btnNext.setEnabled(true);
                        btnNext.setText("Next");
                        Toast.makeText(this, "Failed to save: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        });
    }
}