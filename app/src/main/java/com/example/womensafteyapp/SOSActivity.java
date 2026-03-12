package com.example.womensafteyapp;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SOSActivity extends AppCompatActivity {

    TextView countdownText;
    Button cancelBtn;

    MediaPlayer alarmSound;

    CountDownTimer timer;

    int seconds = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        countdownText = findViewById(R.id.countdownText);
        cancelBtn = findViewById(R.id.cancelBtn);

        alarmSound = MediaPlayer.create(this, R.raw.panic_alarm);

        // Start Alarm
        alarmSound.start();

        timer = new CountDownTimer(5000,1000) {

            public void onTick(long millisUntilFinished) {

                countdownText.setText("Sending SOS in " + seconds);
                seconds--;

            }

            public void onFinish() {

                sendSOS();

            }

        }.start();

        cancelBtn.setOnClickListener(v -> {

            timer.cancel();
            alarmSound.stop();
            finish();

        });
    }

    private void sendSOS() {

        // call MainActivity method or SMS logic here

    }
}