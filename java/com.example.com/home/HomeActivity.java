package com.example.notes.home;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.notes.R;
import com.example.notes.calendar.CalendarActivity;
import com.example.notes.notes.CreateNoteActivity;
import com.example.notes.search.SearchActivity;
import com.example.notes.utils.LocaleHelper;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

public class HomeActivity extends AppCompatActivity {

    ListView listNotes;
    TextView tvDate, tvTitle, tvRecent;
    EditText searchInput;

    ArrayList<String> noteTitles = new ArrayList<>();
    ArrayList<String> noteContents = new ArrayList<>();
    ArrayList<String> dateKeys = new ArrayList<>();

    HomeAdapter homeAdapter;

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocaleHelper.loadLocale(this);
        setContentView(R.layout.activity_home);

        listNotes = findViewById(R.id.listNotes);
        tvDate = findViewById(R.id.tvDate);
        tvTitle = findViewById(R.id.tvTitle);
        tvRecent = findViewById(R.id.tvRecent);
        searchInput = findViewById(R.id.searchInput);

        setLanguageText();
        startClock();
        loadNotes();
        setupNavigation();

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (homeAdapter != null) {
                    homeAdapter.filter(s.toString());
                }
            }
        });

        listNotes.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent(HomeActivity.this, CreateNoteActivity.class);
            intent.putExtra("dateKey", homeAdapter.keys.get(position));
            intent.putExtra("oldTitle", homeAdapter.titles.get(position));
            startActivity(intent);
        });

        // ✅ LONG PRESS FIX (PIN + HAPUS)
        listNotes.setOnItemLongClickListener((parent, view, position, id) -> {

            String key = homeAdapter.keys.get(position);

            SharedPreferences prefs = getSharedPreferences("pins", MODE_PRIVATE);
            boolean isPinned = prefs.getBoolean(key, false);

            String[] options = isPinned
                    ? new String[]{"Lepas Pin", "Hapus"}
                    : new String[]{"Sematkan", "Hapus"};

            new AlertDialog.Builder(this)
                    .setTitle("Pilih Aksi")
                    .setItems(options, (dialog, which) -> {

                        if (which == 0) {
                            prefs.edit().putBoolean(key, !isPinned).apply();

                            Toast.makeText(this,
                                    isPinned ? "Pin dilepas" : "Disematkan",
                                    Toast.LENGTH_SHORT).show();

                            loadNotes();
                        }

                        else if (which == 1) {
                            showDeleteDialog(position);
                        }

                    })
                    .show();

            return true;
        });
    }

    private void setLanguageText() {
        Locale current = getResources().getConfiguration().getLocales().get(0);
        String lang = current.getLanguage();

        switch (lang) {
            case "en":
                tvTitle.setText("Notes");
                tvRecent.setText("Recent Notes");
                searchInput.setHint("Search...");
                break;
            case "fr":
                tvTitle.setText("Notes");
                tvRecent.setText("Notes Récentes");
                searchInput.setHint("Rechercher...");
                break;
            case "ar":
                tvTitle.setText("ملاحظات");
                tvRecent.setText("ملاحظات حديثة");
                searchInput.setHint("بحث...");
                break;
            case "ja":
                tvTitle.setText("ノート");
                tvRecent.setText("最近のメモ");
                searchInput.setHint("検索...");
                break;
            default:
                tvTitle.setText("Catatan");
                tvRecent.setText("Catatan Terbaru");
                searchInput.setHint("Cari catatan...");
                break;
        }
    }

    // 🕒 JAM + DETIK REALTIME
    private void startClock() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Locale locale = getResources().getConfiguration().getLocales().get(0);

                String date = new java.text.SimpleDateFormat(
                        "dd MMMM yyyy • HH:mm:ss",
                        locale
                ).format(new java.util.Date());

                tvDate.setText(date);

                handler.postDelayed(this, 1000);
            }
        });
    }

    private boolean isPinned(String key) {
        SharedPreferences prefs = getSharedPreferences("pins", MODE_PRIVATE);
        return prefs.getBoolean(key, false);
    }

    private void loadNotes() {
        SharedPreferences prefs = getSharedPreferences("notes", MODE_PRIVATE);
        Map<String, ?> savedNotes = prefs.getAll();

        noteTitles.clear();
        noteContents.clear();
        dateKeys.clear();

        ArrayList<String> tempTitles = new ArrayList<>();
        ArrayList<String> tempContents = new ArrayList<>();
        ArrayList<String> tempDateKeys = new ArrayList<>();

        for (Map.Entry<String, ?> entry : savedNotes.entrySet()) {

            String dateKey = entry.getKey();
            String raw = (String) entry.getValue();

            if (raw == null) continue;

            String[] parts = raw.split("\\|\\|");

            String title = parts.length > 0 ? parts[0] : "(No Title)";
            String content = parts.length > 1 ? parts[1] : "";

            if (title.trim().isEmpty()) title = "(No Title)";

            tempTitles.add(title);
            tempContents.add(content);
            tempDateKeys.add(dateKey);
        }

        ArrayList<Integer> index = new ArrayList<>();
        for (int i = 0; i < tempDateKeys.size(); i++) index.add(i);

        // 📌 PIN DI ATAS
        index.sort((a, b) -> {
            boolean pinA = isPinned(tempDateKeys.get(a));
            boolean pinB = isPinned(tempDateKeys.get(b));

            if (pinA == pinB) {
                return tempDateKeys.get(b).compareTo(tempDateKeys.get(a));
            }
            return pinA ? -1 : 1;
        });

        for (int i : index) {
            noteTitles.add(tempTitles.get(i));
            noteContents.add(tempContents.get(i));
            dateKeys.add(tempDateKeys.get(i));
        }

        homeAdapter = new HomeAdapter(this, noteTitles, noteContents, dateKeys);
        listNotes.setAdapter(homeAdapter);
    }

    private void showDeleteDialog(int position) {
        String title = homeAdapter.titles.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Hapus Catatan?")
                .setMessage("Yakin ingin menghapus:\n\n\"" + title + "\" ?")
                .setPositiveButton("Hapus", (dialog, which) -> deleteNote(position))
                .setNegativeButton("Batal", null)
                .show();
    }

    private void deleteNote(int position) {
        SharedPreferences prefs = getSharedPreferences("notes", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.remove(homeAdapter.keys.get(position));
        editor.apply();

        Toast.makeText(this, "Catatan dihapus", Toast.LENGTH_SHORT).show();
        loadNotes();
    }

    private void setupNavigation() {
        ImageView navNotes = findViewById(R.id.navNotes);
        ImageView navCalendar = findViewById(R.id.navCalendar);
        ImageView navSearch = findViewById(R.id.navSearch);
        ImageView navCreate = findViewById(R.id.navCreate);

        if (navNotes != null) navNotes.setOnClickListener(v -> {});

        if (navCalendar != null)
            navCalendar.setOnClickListener(v ->
                    startActivity(new Intent(HomeActivity.this, CalendarActivity.class)));

        if (navSearch != null)
            navSearch.setOnClickListener(v ->
                    startActivity(new Intent(HomeActivity.this, SearchActivity.class)));

        if (navCreate != null)
            navCreate.setOnClickListener(v ->
                    startActivity(new Intent(HomeActivity.this, CreateNoteActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotes();
        setLanguageText();
    }
}