package com.example.womensafteyapp;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
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
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

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

    // ── 1km radius in degrees (approx) ──
    private static final double RADIUS_KM = 1.0;

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

        // ── Load contacts from SharedPreferences instantly ──
        contact1Number = getSharedPreferences("sos_prefs", MODE_PRIVATE)
                .getString("contact1Number", "");
        contact2Number = getSharedPreferences("sos_prefs", MODE_PRIVATE)
                .getString("contact2Number", "");

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
                            if (c1 != null && !c1.isEmpty()) contact1Number = c1;
                            if (c2 != null && !c2.isEmpty()) contact2Number = c2;

                            getSharedPreferences("sos_prefs", MODE_PRIVATE).edit()
                                    .putString("contact1Number", contact1Number)
                                    .putString("contact2Number", contact2Number)
                                    .apply();
                        }
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Using saved contacts",
                                    Toast.LENGTH_SHORT).show()
                    );

            // ── Save FCM token ──
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token ->
                    db.collection("users").document(userId)
                            .update("fcmToken", token)
            );
        }

        // ── Location provider ──
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // ── Save last location ──
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    getSharedPreferences("sos_prefs", MODE_PRIVATE).edit()
                            .putString("lastLat", String.valueOf(location.getLatitude()))
                            .putString("lastLng", String.valueOf(location.getLongitude()))
                            .apply();
                }
            });
        }

        // ── Start location background service ──
        Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        // ── Listen for nearby SOS alerts from Firestore ──
        listenForNearbySOS();

        // ── Alarm sound ──
        alarmSound = MediaPlayer.create(this, R.raw.panic_alarm);
        if (alarmSound != null) alarmSound.setLooping(true);

        // ── Request permissions ──
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

        // ── Police → nearest police station on Maps ──
        policeBtn.setOnClickListener(v -> {
            Uri uri = Uri.parse("geo:0,0?q=police+station+near+me");
            Intent i = new Intent(Intent.ACTION_VIEW, uri);
            i.setPackage("com.google.android.apps.maps");
            if (i.resolveActivity(getPackageManager()) != null) {
                startActivity(i);
            } else {
                Intent dial = new Intent(Intent.ACTION_DIAL);
                dial.setData(Uri.parse("tel:100"));
                startActivity(dial);
            }
        });

        // ── Emergency → 112 ──
        contactBtn.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_DIAL);
            i.setData(Uri.parse("tel:112"));
            startActivity(i);
        });

        // ── Hospital → nearest hospital on Maps ──
        hospitalBtn.setOnClickListener(v -> {
            Uri uri = Uri.parse("geo:0,0?q=hospital+near+me");
            Intent i = new Intent(Intent.ACTION_VIEW, uri);
            i.setPackage("com.google.android.apps.maps");
            if (i.resolveActivity(getPackageManager()) != null) {
                startActivity(i);
            } else {
                Intent dial = new Intent(Intent.ACTION_DIAL);
                dial.setData(Uri.parse("tel:108"));
                startActivity(dial);
            }
        });

        // ── Petrol → nearest petrol pump on Maps ──
        petrolBtn.setOnClickListener(v -> {
            Uri uri = Uri.parse("geo:0,0?q=petrol+pump+near+me");
            Intent i = new Intent(Intent.ACTION_VIEW, uri);
            i.setPackage("com.google.android.apps.maps");
            if (i.resolveActivity(getPackageManager()) != null) {
                startActivity(i);
            } else {
                Intent browser = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/petrol+pump+near+me"));
                startActivity(browser);
            }
        });

        // ── Profile → ProfileEditActivity ──
        profileBtn.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileEditActivity.class)));

        // ── Logout ──
        btnLogout.setOnClickListener(v -> {
            if (alarmSound != null && alarmSound.isPlaying()) {
                alarmSound.pause();
                alarmSound.seekTo(0);
            }
            stopService(new Intent(this, LocationUpdateService.class));
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(MainActivity.this, LoginActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
        });
    }

    // ══════════════════════════════
    // Listen for nearby SOS alerts
    // ══════════════════════════════
    private void listenForNearbySOS() {
        if (userId == null) return;

        db.collection("sos_alerts")
                .whereEqualTo("active", true)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    String myLat = getSharedPreferences("sos_prefs", MODE_PRIVATE)
                            .getString("lastLat", "");
                    String myLng = getSharedPreferences("sos_prefs", MODE_PRIVATE)
                            .getString("lastLng", "");

                    if (myLat.isEmpty() || myLng.isEmpty()) return;

                    double myLatD = Double.parseDouble(myLat);
                    double myLngD = Double.parseDouble(myLng);

                    for (QueryDocumentSnapshot doc : snapshots) {
                        String triggeredBy = doc.getString("userId");

                        // ── Skip own SOS ──
                        if (userId.equals(triggeredBy)) continue;

                        Double sosLat = doc.getDouble("latitude");
                        Double sosLng = doc.getDouble("longitude");

                        if (sosLat == null || sosLng == null) continue;

                        // ── Calculate distance ──
                        double distance = calculateDistance(
                                myLatD, myLngD, sosLat, sosLng);

                        // ── Within 1km → show notification ──
                        if (distance <= RADIUS_KM) {
                            showNearbySOSNotification(sosLat, sosLng);
                        }
                    }
                });
    }

    // ══════════════════════════════
    // SOS trigger
    // ══════════════════════════════
    void sendSOS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted!", Toast.LENGTH_LONG).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted!", Toast.LENGTH_LONG).show();
            return;
        }
        if (contact1Number.isEmpty() && contact2Number.isEmpty()) {
            Toast.makeText(this, "No emergency contacts! Update your profile.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            double lat = 0, lng = 0;
            String message;

            if (location != null) {
                lat = location.getLatitude();
                lng = location.getLongitude();
                getSharedPreferences("sos_prefs", MODE_PRIVATE).edit()
                        .putString("lastLat", String.valueOf(lat))
                        .putString("lastLng", String.valueOf(lng))
                        .apply();
                message = "🚨 SOS ALERT! I am in danger!\nMy Location:\n"
                        + "https://maps.google.com/?q=" + lat + "," + lng
                        + "\nPlease help immediately!";
            } else {
                String savedLat = getSharedPreferences("sos_prefs", MODE_PRIVATE)
                        .getString("lastLat", "");
                String savedLng = getSharedPreferences("sos_prefs", MODE_PRIVATE)
                        .getString("lastLng", "");
                if (!savedLat.isEmpty()) {
                    lat = Double.parseDouble(savedLat);
                    lng = Double.parseDouble(savedLng);
                }
                message = "🚨 SOS ALERT! I am in danger!\nLast known location:\n"
                        + "https://maps.google.com/?q=" + lat + "," + lng
                        + "\nPlease help immediately!";
            }

            // ── Send SMS ──
            sendSMSToContacts(message);

            // ── Save SOS alert to Firestore for nearby users ──
            saveSOSToFirestore(lat, lng);
        });
    }

    private void sendSMSToContacts(String message) {
        try {
            String f1 = contact1Number.replaceAll("\\s+", "");
            if (!f1.isEmpty() && !f1.startsWith("+")) f1 = "+91" + f1;

            String f2 = contact2Number.replaceAll("\\s+", "");
            if (!f2.isEmpty() && !f2.startsWith("+")) f2 = "+91" + f2;

            SmsManager smsManager = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? getSystemService(SmsManager.class)
                    : SmsManager.getDefault();

            if (smsManager != null) {
                if (!f1.isEmpty()) {
                    if (message.length() > 160)
                        smsManager.sendMultipartTextMessage(f1, null,
                                smsManager.divideMessage(message), null, null);
                    else
                        smsManager.sendTextMessage(f1, null, message, null, null);
                }
                if (!f2.isEmpty()) {
                    if (message.length() > 160)
                        smsManager.sendMultipartTextMessage(f2, null,
                                smsManager.divideMessage(message), null, null);
                    else
                        smsManager.sendTextMessage(f2, null, message, null, null);
                }
            }
            Toast.makeText(this, "SOS SMS Sent! ✅", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "SMS Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // ── Save SOS to Firestore so nearby users get notified ──
    private void saveSOSToFirestore(double lat, double lng) {
        if (userId == null) return;

        Map<String, Object> sosData = new HashMap<>();
        sosData.put("userId",    userId);
        sosData.put("latitude",  lat);
        sosData.put("longitude", lng);
        sosData.put("timestamp", System.currentTimeMillis());
        sosData.put("active",    true);

        db.collection("sos_alerts").document(userId)
                .set(sosData)
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "Nearby users alerted! 🔔",
                                Toast.LENGTH_SHORT).show()
                );
    }

    // ── Show notification to nearby user ──
    private void showNearbySOSNotification(double sosLat, double sosLng) {
        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "nearby_sos";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Nearby SOS", NotificationManager.IMPORTANCE_HIGH);
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MapActivity.class);
        intent.putExtra("sosLat", String.valueOf(sosLat));
        intent.putExtra("sosLng", String.valueOf(sosLng));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("⚠️ Someone nearby needs help!")
                .setContentText("Tap to see their location on map")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 200, 500});

        manager.notify(3001, builder.build());
    }

    // ── Calculate distance between 2 coordinates in km ──
    private double calculateDistance(double lat1, double lng1,
                                     double lat2, double lng2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        holdHandler.removeCallbacksAndMessages(null);
        if (alarmSound != null) { alarmSound.release(); alarmSound = null; }
    }
}