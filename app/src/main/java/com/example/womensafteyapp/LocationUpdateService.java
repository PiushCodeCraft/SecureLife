package com.example.womensafteyapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class LocationUpdateService extends Service {

    private static final String CHANNEL_ID    = "location_service";
    private static final int    NOTIF_ID      = 2001;
    private static final long   UPDATE_INTERVAL = 5 * 60 * 1000; // 5 minutes

    FusedLocationProviderClient fusedLocationClient;
    FirebaseFirestore db;
    Handler handler = new Handler();
    Runnable locationRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIF_ID, buildNotification());
        startLocationUpdates();
        return START_STICKY;
    }

    private void startLocationUpdates() {
        locationRunnable = new Runnable() {
            @Override
            public void run() {
                saveLocationToFirestore();
                handler.postDelayed(this, UPDATE_INTERVAL);
            }
        };
        handler.post(locationRunnable);
    }

    private void saveLocationToFirestore() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location == null) return;

            // ── Get FCM token and save with location ──
            FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> {
                Map<String, Object> data = new HashMap<>();
                data.put("userId",      user.getUid());
                data.put("latitude",    location.getLatitude());
                data.put("longitude",   location.getLongitude());
                data.put("lastUpdated", System.currentTimeMillis());
                data.put("fcmToken",    token);
                data.put("isActive",    true);

                db.collection("user_locations")
                    .document(user.getUid())
                    .set(data);
            });
        });
    }

    private Notification buildNotification() {
        NotificationManager manager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Location Service",
                NotificationManager.IMPORTANCE_LOW
            );
            manager.createNotificationChannel(channel);
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SecureLife Active")
            .setContentText("Keeping you safe in background")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && locationRunnable != null) {
            handler.removeCallbacks(locationRunnable);
        }
        // ── Mark user as inactive ──
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            db.collection("user_locations")
                .document(user.getUid())
                .update("isActive", false);
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
