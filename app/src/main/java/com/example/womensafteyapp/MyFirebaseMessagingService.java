package com.example.womensafteyapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "sos_alerts";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "⚠️ Nearby SOS Alert!";
        String body  = "Someone near you needs help!";
        String lat   = "";
        String lng   = "";

        // ── Get data from notification ──
        if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().getOrDefault("title", title);
            body  = remoteMessage.getData().getOrDefault("body", body);
            lat   = remoteMessage.getData().getOrDefault("lat", "");
            lng   = remoteMessage.getData().getOrDefault("lng", "");
        }

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body  = remoteMessage.getNotification().getBody();
        }

        showNotification(title, body, lat, lng);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        // ── Save new token to Firestore ──
        saveFCMToken(token);
    }

    private void saveFCMToken(String token) {
        com.google.firebase.auth.FirebaseUser user =
            com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .update("fcmToken", token);
        }
    }

    private void showNotification(String title, String body, String lat, String lng) {
        NotificationManager manager =
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // ── Create channel for Android 8+ ──
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "SOS Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Nearby SOS emergency alerts");
            channel.enableVibration(true);
            manager.createNotificationChannel(channel);
        }

        // ── Open map when notification tapped ──
        Intent intent = new Intent(this, MapActivity.class);
        if (!lat.isEmpty() && !lng.isEmpty()) {
            intent.putExtra("sosLat", lat);
            intent.putExtra("sosLng", lng);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(new long[]{0, 500, 200, 500});

        manager.notify(NOTIFICATION_ID, builder.build());
    }
}
