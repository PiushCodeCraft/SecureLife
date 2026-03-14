package com.example.womensafteyapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionsActivity extends AppCompatActivity {

    private static final int PERM_REQUEST_CODE = 100;

    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_CONTACTS
    };

    Button btnGrantPermissions, btnContinue;
    TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        btnGrantPermissions = findViewById(R.id.btnGrantPermissions);
        btnContinue         = findViewById(R.id.btnContinue);
        tvStatus            = findViewById(R.id.tvStatus);

        // ── Continue always enabled (emulator compatibility) ──
        btnContinue.setEnabled(true);
        btnContinue.setAlpha(1f);

        updateStatus();

        btnGrantPermissions.setOnClickListener(v -> {
            for (String perm : REQUIRED_PERMISSIONS) {
                markPermissionRequested(perm);
            }

            boolean anyPermanentlyDenied = false;
            for (String perm : REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, perm)
                        != PackageManager.PERMISSION_GRANTED
                        && !ActivityCompat.shouldShowRequestPermissionRationale(this, perm)
                        && isPermissionRequestedBefore(perm)) {
                    anyPermanentlyDenied = true;
                    break;
                }
            }

            if (anyPermanentlyDenied) {
                Toast.makeText(this,
                        "Please enable permissions manually in App Settings",
                        Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERM_REQUEST_CODE);
            }
        });

        // ── Continue → save flag → go to MainActivity ──
        btnContinue.setOnClickListener(v -> {
            getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .edit()
                    .putBoolean("permissionsDone", true)
                    .apply();
            startActivity(new Intent(PermissionsActivity.this, MainActivity.class));
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        int granted = 0;
        for (String perm : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    == PackageManager.PERMISSION_GRANTED) {
                granted++;
            }
        }

        tvStatus.setText(granted + " / " + REQUIRED_PERMISSIONS.length + " permissions granted");

        if (granted == REQUIRED_PERMISSIONS.length) {
            tvStatus.setTextColor(0xFF00D68F);
            btnContinue.setEnabled(true);
            btnContinue.setAlpha(1f);
            btnGrantPermissions.setText("All permissions granted ✓");
            btnGrantPermissions.setEnabled(false);
        } else {
            tvStatus.setTextColor(0xFFFF2847);
            // ── Keep Continue enabled even if not all granted ──
            btnContinue.setEnabled(true);
            btnContinue.setAlpha(1f);
            btnGrantPermissions.setText("Grant Permissions");
            btnGrantPermissions.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM_REQUEST_CODE) {
            updateStatus();
        }
    }

    private boolean isPermissionRequestedBefore(String permission) {
        return getSharedPreferences("perm_prefs", MODE_PRIVATE)
                .getBoolean(permission + "_requested", false);
    }

    private void markPermissionRequested(String permission) {
        getSharedPreferences("perm_prefs", MODE_PRIVATE).edit()
                .putBoolean(permission + "_requested", true).apply();
    }
}