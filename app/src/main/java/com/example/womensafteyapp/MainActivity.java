package com.example.womensafteyapp;

import android.Manifest;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    // ── UI ──
    Button sosBtn;
    LinearLayout locationBtn, policeBtn, contactBtn, hospitalBtn, petrolBtn, profileBtn;
    TextView sosHint, btnLogout, userName;

    // ── Media & Location ──
    MediaPlayer alarmSound;
    FusedLocationProviderClient fusedLocationClient;

    // ── Firebase ──
    FirebaseFirestore db;
    String userId;

    // ── Emergency contacts ──
    String contact1Number = "";
    String contact2Number = "";

    // ── SOS hold handler ──
    Handler holdHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // ── Connect all views ──
        sosBtn      = findViewById(R.id.sosBtn);
        locationBtn = findViewById(R.id.locationBtn);
        policeBtn   = findViewById(R.id.policeBtn);
        contactBtn  = findViewById(R.id.contactBtn);
        hospitalBtn = findViewById(R.id.hospitalBtn);
        petrolBtn   = findViewById(R.id.petrolBtn);
        profileBtn  = findViewById(R.id.profileBtn);
        sosHint     = findViewById(R.id.sosHint);
        btnLogout   = findViewById(R.id.btnLogout);
        userName    = findViewById(R.id.userName);

        // ── Firebase setup ──
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            userId = currentUser.getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String name = doc.getString("name");
                            if (name != null && !name.isEmpty()) {
                                userName.setText("Welcome, " + name);
                            } else {
                                String email = currentUser.getEmail();
                                if (email != null) {
                                    String namePart = email.split("@")[0];
                                    String displayName = namePart.substring(0, 1).toUpperCase()
                                            + namePart.substring(1);
                                    userName.setText("Welcome, " + displayName);
                                }
                            }
                            String c1 = doc.getString("contact1Number");
                            String c2 = doc.getString("contact2Number");
                            if (c1 != null) contact1Number = c1;
                            if (c2 != null) contact2Number = c2;

                            // ── Save contacts for PowerButtonReceiver ──
                            getSharedPreferences("sos_prefs", MODE_PRIVATE).edit()
                                    .putString("contact1Number", contact1Number)
                                    .putString("contact2Number", contact2Number)
                                    .apply();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show()
                    );
        }

        // ── Location provider ──
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // ── Save last location for PowerButtonReceiver ──
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                getSharedPreferences("sos_prefs", MODE_PRIVATE).edit()
                        .putString("lastLat", String.valueOf(location.getLatitude()))
                        .putString("lastLng", String.valueOf(location.getLongitude()))
                        .apply();
            }
        });

        // ── Alarm sound ──
        alarmSound = MediaPlayer.create(this, R.raw.panic_alarm);
        if (alarmSound != null) alarmSound.setLooping(true);

        // ── Permissions ──
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, 1);

        // ── SOS pulse animation ──
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        sosBtn.startAnimation(pulse);

        // ── SOS — tap to activate, hold 3 sec to stop ──
        sosBtn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (alarmSound != null) { alarmSound.seekTo(0); alarmSound.start(); }
                    sendSOS();
                    sosHint.setText("HOLD 3 SEC TO STOP ALARM");
                    sosHint.setTextColor(0xFFFF2847);
                    holdHandler.postDelayed(() -> {
                        if (alarmSound != null && alarmSound.isPlaying()) {
                            alarmSound.pause();
                            alarmSound.seekTo(0);
                        }
                        sosHint.setText("TAP TO ACTIVATE · HOLD TO STOP");
                        sosHint.setTextColor(0xFF2E3A52);
                        Toast.makeText(this, "Alarm stopped", Toast.LENGTH_SHORT).show();
                    }, 3000);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    holdHandler.removeCallbacksAndMessages(null);
                    break;
            }
            return true;
        });

        // ── Share Location → MapActivity ──
        locationBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, MapActivity.class)));

        // ── Police → 100 ──
        policeBtn.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_DIAL);
            i.setData(Uri.parse("tel:100"));
            startActivity(i);
        });

        // ── Emergency → 112 ──
        contactBtn.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_DIAL);
            i.setData(Uri.parse("tel:112"));
            startActivity(i);
        });

        // ── Hospital → 108 ──
        hospitalBtn.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_DIAL);
            i.setData(Uri.parse("tel:108"));
            startActivity(i);
        });

        // ── Petrol → alarm toggle ──
        petrolBtn.setOnClickListener(v -> {
            if (alarmSound != null) {
                if (alarmSound.isPlaying()) {
                    alarmSound.pause();
                    alarmSound.seekTo(0);
                    Toast.makeText(this, "Alarm stopped", Toast.LENGTH_SHORT).show();
                } else {
                    alarmSound.start();
                    Toast.makeText(this, "Panic alarm activated!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // ── Profile → UserSetupActivity ──
        profileBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, UserSetupActivity.class)));

        // ── Logout ──
        btnLogout.setOnClickListener(v -> {
            if (alarmSound != null && alarmSound.isPlaying()) {
                alarmSound.pause();
                alarmSound.seekTo(0);
            }
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    // ── Send SOS SMS ──
    void sendSOS() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                getSharedPreferences("sos_prefs", MODE_PRIVATE).edit()
                        .putString("lastLat", String.valueOf(location.getLatitude()))
                        .putString("lastLng", String.valueOf(location.getLongitude()))
                        .apply();

                String message = "🚨 SOS! I need help.\nMy Location:\nhttps://maps.google.com/?q="
                        + location.getLatitude() + "," + location.getLongitude();
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    if (!contact1Number.isEmpty())
                        smsManager.sendTextMessage(contact1Number, null, message, null, null);
                    if (!contact2Number.isEmpty())
                        smsManager.sendTextMessage(contact2Number, null, message, null, null);
                    if (!contact1Number.isEmpty() || !contact2Number.isEmpty())
                        Toast.makeText(this, "SOS Sent!", Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(this, "No emergency contacts set!", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "SMS Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        holdHandler.removeCallbacksAndMessages(null);
        if (alarmSound != null) { alarmSound.release(); alarmSound = null; }
    }
}