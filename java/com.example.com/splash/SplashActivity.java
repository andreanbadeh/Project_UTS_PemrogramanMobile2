package com.example.notes.splash;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.notes.MainActivity;
import com.example.notes.R;

import java.util.concurrent.Executor;

public class SplashActivity extends AppCompatActivity {

    View screen1, screen2, screen3, screen4;
    Handler handler = new Handler();

    private final String CHANNEL_ID = "login_channel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Request permission Android 13+
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        100
                );
            }
        }

        createNotificationChannel();

        screen1 = findViewById(R.id.screen1);
        screen2 = findViewById(R.id.screen2);
        screen3 = findViewById(R.id.screen3);
        screen4 = findViewById(R.id.screen4);

        startStep1();
    }

    private void startStep1() {
        screen1.setVisibility(View.VISIBLE);

        handler.postDelayed(() -> {
            screen1.setVisibility(View.GONE);
            screen2.setVisibility(View.VISIBLE);
            startStep2();
        }, 800);
    }

    private void startStep2() {
        View circle = screen2.findViewById(R.id.circle);
        Animation fall = AnimationUtils.loadAnimation(this, R.anim.fall_down);
        circle.startAnimation(fall);

        fall.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                screen2.setVisibility(View.GONE);
                screen3.setVisibility(View.VISIBLE);
                startStep3();
            }
        });
    }

    private void startStep3() {
        View circle = screen3.findViewById(R.id.bigCircle);
        Animation expand = AnimationUtils.loadAnimation(this, R.anim.expand);
        circle.startAnimation(expand);

        expand.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                screen3.setVisibility(View.GONE);
                screen4.setVisibility(View.VISIBLE);
                startStep4();
            }
        });
    }

    private void startStep4() {
        View panel = screen4.findViewById(R.id.panel);
        Animation up = AnimationUtils.loadAnimation(this, R.anim.bottom_up);
        panel.startAnimation(up);

        Button next = screen4.findViewById(R.id.btnNext);
        next.setOnClickListener(v -> showBiometric());
    }

    // ===============================
    // 🔐 BIOMETRIC
    // ===============================
    private void showBiometric() {

        BiometricManager biometricManager = BiometricManager.from(this);

        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );

        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this,
                    "Sidik jari atau PIN belum diaktifkan",
                    Toast.LENGTH_SHORT).show();

            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);

        BiometricPrompt biometricPrompt =
                new BiometricPrompt(this, executor,
                        new BiometricPrompt.AuthenticationCallback() {

                            @Override
                            public void onAuthenticationSucceeded(
                                    BiometricPrompt.AuthenticationResult result) {
                                super.onAuthenticationSucceeded(result);

                                showLoginNotification(); // 🔥 tampilkan notif

                                startActivity(new Intent(SplashActivity.this,
                                        MainActivity.class));
                                finish();
                            }

                            @Override
                            public void onAuthenticationFailed() {
                                super.onAuthenticationFailed();
                                Toast.makeText(SplashActivity.this,
                                        "Autentikasi gagal",
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onAuthenticationError(int errorCode,
                                                              CharSequence errString) {
                                super.onAuthenticationError(errorCode, errString);

                                Toast.makeText(SplashActivity.this,
                                        "Error: " + errString,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });

        BiometricPrompt.PromptInfo promptInfo =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Login Notes")
                        .setSubtitle("Gunakan sidik jari atau PIN")
                        .setAllowedAuthenticators(
                                BiometricManager.Authenticators.BIOMETRIC_STRONG
                                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL
                        )
                        .build();

        biometricPrompt.authenticate(promptInfo);
    }

    // ===============================
    // 🔔 NOTIFICATION
    // ===============================
    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Login Notification",
                            NotificationManager.IMPORTANCE_HIGH // 🔥 penting biar ngambang
                    );

            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setLockscreenVisibility(
                    android.app.Notification.VISIBILITY_PUBLIC);

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private void showLoginNotification() {

        Intent intent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("Login Berhasil")
                        .setContentText("Selamat datang kembali 👋")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        if (Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.POST_NOTIFICATIONS)
                        == PackageManager.PERMISSION_GRANTED) {

            NotificationManagerCompat.from(this)
                    .notify(1, builder.build());
        }
    }
}