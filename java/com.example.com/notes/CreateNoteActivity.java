package com.example.notes.notes;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

// 🔥 TAMBAHAN IMPORT (WAJIB UNTUK FIX HEADS-UP)
import android.app.PendingIntent;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.notes.R;
import com.example.notes.utils.LocaleHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.*;

public class CreateNoteActivity extends AppCompatActivity {

    EditText inputTitle, inputContent;
    Button btnSave, btnAi;

    private String dateKey = null;
    private final String CHANNEL_ID = "notes_channel";

    private final OkHttpClient client = new OkHttpClient();

    // 🔥 API KEY KAMU
    private final String API_KEY = "AIzaSyDu2uO2ebIRx2UpQFkE0E3LJFyZlhZ1RYo";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocaleHelper.loadLocale(this);
        setContentView(R.layout.activity_create_note);

        inputTitle = findViewById(R.id.inputTitle);
        inputContent = findViewById(R.id.inputContent);
        btnSave = findViewById(R.id.btnSave);
        btnAi = findViewById(R.id.btnAi);

        createNotificationChannel();

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        dateKey = getIntent().getStringExtra("dateKey");

        if (dateKey != null) {
            SharedPreferences prefs = getSharedPreferences("notes", MODE_PRIVATE);
            String raw = prefs.getString(dateKey, null);

            if (raw != null) {
                String[] parts = raw.split("\\|\\|");
                if (parts.length > 0) inputTitle.setText(parts[0]);
                if (parts.length > 1) inputContent.setText(parts[1]);
            }
        }

        btnSave.setOnClickListener(v -> saveNote());

        btnAi.setOnClickListener(v -> {
            String content = inputContent.getText().toString().trim();
            if (!content.isEmpty()) {
                summarizeWithAI(content);
            } else {
                Toast.makeText(this, "Isi catatan dulu!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =========================
    // SAVE NOTE
    // =========================
    private void saveNote() {
        String title = inputTitle.getText().toString().trim();
        String content = inputContent.getText().toString().trim();

        if (title.isEmpty() && content.isEmpty()) return;

        SharedPreferences prefs = getSharedPreferences("notes", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (dateKey == null)
            dateKey = String.valueOf(System.currentTimeMillis());

        editor.putString(dateKey, title + "||" + content).apply();

        showNotification("Tersimpan", "Catatan \"" + title + "\" disimpan");

        finish();
    }

    // =========================
    // DELETE NOTE
    // =========================
    private void deleteNote() {
        if (dateKey != null) {

            getSharedPreferences("notes", MODE_PRIVATE)
                    .edit().remove(dateKey).apply();

            // 🔥 TAMBAHAN NOTIF DELETE
            showNotification("Dihapus", "Catatan berhasil dihapus");

            finish();
        }
    }

    // =========================
    // NOTIFICATION (HEADS-UP FIX MAX)
    // =========================
    private void showNotification(String title, String message) {

        Intent intent = new Intent(this, CreateNoteActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_MAX)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {

            NotificationManagerCompat.from(this)
                    .notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    // =========================
    // NOTIFICATION CHANNEL
    // =========================
    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Notes",
                    NotificationManager.IMPORTANCE_HIGH
            );

            // 🔥 TAMBAHAN (FIX HEADS-UP)
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    // =========================
    // AI FUNCTION (TIDAK DIUBAH)
    // =========================
    private void summarizeWithAI(String text) {

        Toast.makeText(this, "AI sedang memproses...", Toast.LENGTH_SHORT).show();

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                + API_KEY;

        try {
            JSONObject bodyJson = new JSONObject();

            JSONArray contents = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject textPart = new JSONObject();

            textPart.put("text",
                    "Rangkum teks berikut dalam Bahasa Indonesia:\n\n" + text);

            parts.put(textPart);
            contentObj.put("parts", parts);
            contents.put(contentObj);

            bodyJson.put("contents", contents);

            RequestBody body = RequestBody.create(
                    bodyJson.toString(),
                    MediaType.get("application/json; charset=utf-8")
            );

            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {

                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(CreateNoteActivity.this,
                                    "Gagal koneksi AI", Toast.LENGTH_SHORT).show());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {

                    String res = response.body() != null ? response.body().string() : "";

                    runOnUiThread(() -> {

                        if (!response.isSuccessful()) {
                            new AlertDialog.Builder(CreateNoteActivity.this)
                                    .setTitle("Error " + response.code())
                                    .setMessage(res)
                                    .show();
                            return;
                        }

                        try {
                            JSONObject json = new JSONObject(res);

                            String result = json.getJSONArray("candidates")
                                    .getJSONObject(0)
                                    .getJSONObject("content")
                                    .getJSONArray("parts")
                                    .getJSONObject(0)
                                    .getString("text");

                            inputContent.setText(text + "\n\n--- RANGKUMAN AI ---\n" + result);

                        } catch (Exception e) {
                            Toast.makeText(CreateNoteActivity.this,
                                    "Parse error AI response", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================
    // MENU
    // =========================
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_create_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) finish();

        if (item.getItemId() == R.id.menu_delete)
            deleteNote();

        return true;
    }
}