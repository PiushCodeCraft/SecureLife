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

public class MainActivity extends AppCompatActivity {

    // ── Buttons from NEW dashboard layout ──
    Button sosBtn;
    LinearLayout locationBtn, policeBtn, contactBtn, hospitalBtn, petrolBtn;
    TextView sosHint;

    MediaPlayer alarmSound;
    FusedLocationProviderClient fusedLocationClient;

    String emergencyNumber = "9876543210";

    // For SOS hold-to-activate
    Handler holdHandler = new Handler();
    boolean isHolding = false;
    Runnable sosRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // ── Load NEW dashboard layout ──
        setContentView(R.layout.activity_dashboard);

        // ── Connect buttons to new layout IDs ──
        sosBtn      = findViewById(R.id.sosBtn);
        locationBtn = findViewById(R.id.locationBtn);
        policeBtn   = findViewById(R.id.policeBtn);
        contactBtn  = findViewById(R.id.contactBtn);
        hospitalBtn = findViewById(R.id.hospitalBtn);
        petrolBtn   = findViewById(R.id.petrolBtn);
        sosHint     = findViewById(R.id.sosHint);

        // ── Location provider ──
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // ── Alarm sound ──
        alarmSound = MediaPlayer.create(this, R.raw.panic_alarm);
        if (alarmSound != null) {
            alarmSound.setLooping(true);
        }

        // ── Permissions ──
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.SEND_SMS,
                        Manifest.permission.ACCESS_FINE_LOCATION
                }, 1);

        // ── SOS pulse animation ──
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse);
        sosBtn.startAnimation(pulse);

        // ── SOS Button — hold 3 seconds to activate ──
        sosRunnable = () -> {
            sosBtn.clearAnimation();
            sosBtn.setText("✓");
            sosHint.setText("Help is on the way!");
            sosHint.setTextColor(0xFFFF2847);
            sendSOS();

            // Reset button after 3 seconds
            new Handler().postDelayed(() -> {
                sosBtn.setText("SOS");
                sosHint.setText("HOLD 3 SECONDS TO ACTIVATE");
                sosHint.setTextColor(0xFF2E3A52);
                Animation p = AnimationUtils.loadAnimation(this, R.anim.pulse);
                sosBtn.startAnimation(p);
            }, 3000);
        };

        sosBtn.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    isHolding = true;
                    // Start sound immediately on touch
                    if (alarmSound != null) {
                        alarmSound.start();
                    }
                    sosHint.setText("HOLD... 3");
                    holdHandler.postDelayed(() -> sosHint.setText("HOLD... 2"), 1000);
                    holdHandler.postDelayed(() -> sosHint.setText("HOLD... 1"), 2000);
                    holdHandler.postDelayed(sosRunnable, 3000);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (isHolding) {
                        isHolding = false;
                        holdHandler.removeCallbacks(sosRunnable);
                        holdHandler.removeCallbacksAndMessages(null);
                        
                        // If they release before 3 seconds and SOS wasn't sent, stop the sound
                        if (!sosBtn.getText().toString().equals("✓")) {
                            if (alarmSound != null && alarmSound.isPlaying()) {
                                alarmSound.pause();
                                alarmSound.seekTo(0);
                            }
                        }
                        
                        sosHint.setText("HOLD 3 SECONDS TO ACTIVATE");
                        sosHint.setTextColor(0xFF2E3A52);
                    }
                    break;
            }
            return true;
        });

        // ── Share Location ──
        locationBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, MapActivity.class);
            startActivity(intent);
        });

        // ── Police Call ──
        policeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:100"));
            startActivity(intent);
        });

        // ── Emergency Contact ──
        contactBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:112"));
            startActivity(intent);
        });

        // ── Hospital ──
        hospitalBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:108"));
            startActivity(intent);
        });

        // ── Petrol (Panic Alarm toggle) ──
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
    }

    // ── Send SOS message with location ──
    private void sendSOS() {
        // Sound is already started in ACTION_DOWN
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                String message =
                        "🚨 SOS! I need help.\nMy Location:\nhttps://maps.google.com/?q="
                                + location.getLatitude()
                                + "," + location.getLongitude();

                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(emergencyNumber, null, message, null, null);
                    Toast.makeText(this, "SOS Sent!", Toast.LENGTH_LONG).show();

                } catch (Exception e) {
                    Toast.makeText(this, "SMS Failed", Toast.LENGTH_SHORT).show();
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
        if (alarmSound != null) {
            alarmSound.release();
            alarmSound = null;
        }
    }
}
