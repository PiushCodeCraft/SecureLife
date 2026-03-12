package com.example.womensafteyapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class PowerButtonReceiver extends BroadcastReceiver {

    static int count = 0;

    @Override
    public void onReceive(Context context, Intent intent) {

        if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){

            count++;

            if(count == 3){

                Toast.makeText(context,"SOS Triggered!",Toast.LENGTH_LONG).show();

                count = 0;
            }
        }
    }
}