package com.example.womensafteyapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.*;
import com.google.firebase.FirebaseException;

import java.util.concurrent.TimeUnit;

public class PhoneLoginActivity extends AppCompatActivity {

    EditText phoneNumber, otp;
    Button sendOtpBtn, verifyOtpBtn;

    FirebaseAuth auth;
    String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        phoneNumber = findViewById(R.id.phoneNumber);
        otp = findViewById(R.id.otp);

        sendOtpBtn = findViewById(R.id.sendOtpBtn);
        verifyOtpBtn = findViewById(R.id.verifyOtpBtn);

        auth = FirebaseAuth.getInstance();

        sendOtpBtn.setOnClickListener(v -> sendOTP());

        verifyOtpBtn.setOnClickListener(v -> verifyOTP());
    }

    private void sendOTP(){

        String number = phoneNumber.getText().toString();

        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber("+91" + number)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(callbacks)
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    signInWithCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Toast.makeText(PhoneLoginActivity.this,"OTP Failed",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCodeSent(@NonNull String s,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {

                    verificationId = s;
                }
            };

    private void verifyOTP(){

        String code = otp.getText().toString();

        PhoneAuthCredential credential =
                PhoneAuthProvider.getCredential(verificationId,code);

        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential){

        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {

                    if(task.isSuccessful()){

                        Toast.makeText(this,"Login Successful",Toast.LENGTH_SHORT).show();
                        finish();

                    }else{

                        Toast.makeText(this,"OTP Wrong",Toast.LENGTH_SHORT).show();
                    }

                });
    }
}