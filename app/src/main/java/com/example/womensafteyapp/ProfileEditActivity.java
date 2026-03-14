package com.example.womensafteyapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ProfileEditActivity extends AppCompatActivity {

    // ── Views ──
    TextView tvName, tvAge, tvContact1Name, tvContact1Number, tvContact2Name, tvContact2Number;
    EditText etContact1Name, etContact1Number, etContact2Name, etContact2Number;
    Button btnEdit, btnSave;
    TextView btnBack;

    FirebaseFirestore db;
    String userId;
    boolean isEditing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_edit);

        db     = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // ── Connect views ──
        tvName          = findViewById(R.id.tvName);
        tvAge           = findViewById(R.id.tvAge);
        tvContact1Name  = findViewById(R.id.tvContact1Name);
        tvContact1Number= findViewById(R.id.tvContact1Number);
        tvContact2Name  = findViewById(R.id.tvContact2Name);
        tvContact2Number= findViewById(R.id.tvContact2Number);
        etContact1Name  = findViewById(R.id.etContact1Name);
        etContact1Number= findViewById(R.id.etContact1Number);
        etContact2Name  = findViewById(R.id.etContact2Name);
        etContact2Number= findViewById(R.id.etContact2Number);
        btnEdit         = findViewById(R.id.btnEdit);
        btnSave         = findViewById(R.id.btnSave);
        btnBack         = findViewById(R.id.btnBack);

        // ── Start in view mode ──
        showViewMode();

        // ── Load data from Firestore ──
        loadProfile();

        // ── Back button ──
        btnBack.setOnClickListener(v -> finish());

        // ── Edit button → switch to edit mode ──
        btnEdit.setOnClickListener(v -> showEditMode());

        // ── Save button → save changes ──
        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void loadProfile() {
        db.collection("users").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    // ── Show data in view mode ──
                    String name  = doc.getString("name");
                    String age   = doc.getString("age");
                    String c1n   = doc.getString("contact1Name");
                    String c1num = doc.getString("contact1Number");
                    String c2n   = doc.getString("contact2Name");
                    String c2num = doc.getString("contact2Number");

                    tvName.setText(name != null ? name : "-");
                    tvAge.setText(age != null ? age : "-");
                    tvContact1Name.setText(c1n != null ? c1n : "-");
                    tvContact1Number.setText(c1num != null ? c1num : "-");
                    tvContact2Name.setText(c2n != null ? c2n : "-");
                    tvContact2Number.setText(c2num != null ? c2num : "-");

                    // ── Pre-fill edit fields ──
                    etContact1Name.setText(c1n != null ? c1n : "");
                    etContact1Number.setText(c1num != null ? c1num : "");
                    etContact2Name.setText(c2n != null ? c2n : "");
                    etContact2Number.setText(c2num != null ? c2num : "");
                }
            })
            .addOnFailureListener(e ->
                Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
            );
    }

    private void showViewMode() {
        isEditing = false;

        // ── Show text views ──
        tvContact1Name.setVisibility(View.VISIBLE);
        tvContact1Number.setVisibility(View.VISIBLE);
        tvContact2Name.setVisibility(View.VISIBLE);
        tvContact2Number.setVisibility(View.VISIBLE);

        // ── Hide edit fields ──
        etContact1Name.setVisibility(View.GONE);
        etContact1Number.setVisibility(View.GONE);
        etContact2Name.setVisibility(View.GONE);
        etContact2Number.setVisibility(View.GONE);

        // ── Show Edit, hide Save ──
        btnEdit.setVisibility(View.VISIBLE);
        btnSave.setVisibility(View.GONE);
    }

    private void showEditMode() {
        isEditing = true;

        // ── Hide text views ──
        tvContact1Name.setVisibility(View.GONE);
        tvContact1Number.setVisibility(View.GONE);
        tvContact2Name.setVisibility(View.GONE);
        tvContact2Number.setVisibility(View.GONE);

        // ── Show edit fields ──
        etContact1Name.setVisibility(View.VISIBLE);
        etContact1Number.setVisibility(View.VISIBLE);
        etContact2Name.setVisibility(View.VISIBLE);
        etContact2Number.setVisibility(View.VISIBLE);

        // ── Hide Edit, show Save ──
        btnEdit.setVisibility(View.GONE);
        btnSave.setVisibility(View.VISIBLE);
    }

    private void saveChanges() {
        String c1Name = etContact1Name.getText().toString().trim();
        String c1Num  = etContact1Number.getText().toString().trim();
        String c2Name = etContact2Name.getText().toString().trim();
        String c2Num  = etContact2Number.getText().toString().trim();

        // ── Validation ──
        if (TextUtils.isEmpty(c1Name)) {
            etContact1Name.setError("Enter contact 1 name");
            etContact1Name.requestFocus(); return;
        }
        if (c1Num.length() < 10) {
            etContact1Number.setError("Enter valid number");
            etContact1Number.requestFocus(); return;
        }
        if (TextUtils.isEmpty(c2Name)) {
            etContact2Name.setError("Enter contact 2 name");
            etContact2Name.requestFocus(); return;
        }
        if (c2Num.length() < 10) {
            etContact2Number.setError("Enter valid number");
            etContact2Number.requestFocus(); return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        // ── Update only contact fields in Firestore ──
        Map<String, Object> updates = new HashMap<>();
        updates.put("contact1Name", c1Name);
        updates.put("contact1Number", c1Num);
        updates.put("contact2Name", c2Name);
        updates.put("contact2Number", c2Num);

        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener(unused -> {
                // ── Also update SharedPreferences ──
                getSharedPreferences("sos_prefs", MODE_PRIVATE).edit()
                    .putString("contact1Number", c1Num)
                    .putString("contact2Number", c2Num)
                    .apply();

                btnSave.setEnabled(true);
                btnSave.setText("Save");

                // ── Update display ──
                tvContact1Name.setText(c1Name);
                tvContact1Number.setText(c1Num);
                tvContact2Name.setText(c2Name);
                tvContact2Number.setText(c2Num);

                Toast.makeText(this, "Contacts updated! ✅", Toast.LENGTH_SHORT).show();
                showViewMode();
            })
            .addOnFailureListener(e -> {
                btnSave.setEnabled(true);
                btnSave.setText("Save");
                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
