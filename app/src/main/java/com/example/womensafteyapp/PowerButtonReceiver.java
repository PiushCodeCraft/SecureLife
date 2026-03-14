package com.example.womensafteyapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.widget.Toast;

public class PowerButtonReceiver extends BroadcastReceiver {

    // ── Triple press detection ──
    private static int pressCount = 0;
    private static long lastPressTime = 0;
    private static final long TRIPLE_PRESS_WINDOW = 3000; // 3 seconds
    private static MediaPlayer alarmPlayer;

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())
                || Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {

            long currentTime = System.currentTimeMillis();

            // ── Reset count if outside time window ──
            if (currentTime - lastPressTime > TRIPLE_PRESS_WINDOW) {
                pressCount = 0;
            }

            pressCount++;
            lastPressTime = currentTime;

            if (pressCount >= 3) {
                pressCount = 0;

                // ── Show toast ──
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "🚨 SOS Triggered!", Toast.LENGTH_LONG).show()
                );

                // ── Play alarm ──
                triggerAlarm(context);

                // ── Send SOS SMS ──
                sendSOSSms(context);
            }
        }
    }

    // ── Play panic alarm ──
    private void triggerAlarm(Context context) {
        try {
            if (alarmPlayer != null) {
                alarmPlayer.release();
                alarmPlayer = null;
            }
            alarmPlayer = MediaPlayer.create(context, R.raw.panic_alarm);
            if (alarmPlayer != null) {
                alarmPlayer.setLooping(true);
                alarmPlayer.start();

                // ── Auto stop alarm after 60 seconds ──
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (alarmPlayer != null) {
                        alarmPlayer.stop();
                        alarmPlayer.release();
                        alarmPlayer = null;
                    }
                }, 60000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Send SOS SMS using saved contacts ──
    private void sendSOSSms(Context context) {
        try {
            // ── Load contacts from SharedPreferences ──
            SharedPreferences prefs = context.getSharedPreferences(
                    "sos_prefs", Context.MODE_PRIVATE);
            String contact1 = prefs.getString("contact1Number", "");
            String contact2 = prefs.getString("contact2Number", "");
            String lastLat  = prefs.getString("lastLat", "");
            String lastLng  = prefs.getString("lastLng", "");

            // ── Build message ──
            String message;
            if (!lastLat.isEmpty() && !lastLng.isEmpty()) {
                message = "🚨 SOS! I need help.\nMy Location:\nhttps://maps.google.com/?q="
                        + lastLat + "," + lastLng;
            } else {
                message = "🚨 SOS! I need help. Please contact me immediately!";
            }

            SmsManager smsManager = SmsManager.getDefault();

            if (!contact1.isEmpty()) {
                smsManager.sendTextMessage(contact1, null, message, null, null);
            }
            if (!contact2.isEmpty()) {
                smsManager.sendTextMessage(contact2, null, message, null, null);
            }

            if (!contact1.isEmpty() || !contact2.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(context, "SOS SMS Sent!", Toast.LENGTH_SHORT).show()
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Stop alarm (called from MainActivity) ──
    public static void stopAlarm() {
        if (alarmPlayer != null) {
            alarmPlayer.stop();
            alarmPlayer.release();
            alarmPlayer = null;
        }
    }
}