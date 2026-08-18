package com.greenbuddy.acevosetupengineer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(5, 5, 5);
    private static final int SURFACE = Color.rgb(20, 20, 20);
    private static final int SURFACE_2 = Color.rgb(31, 31, 31);
    private static final int YELLOW = Color.rgb(255, 212, 0);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(188, 188, 188);
    private static final int SAVE_REQUEST = 111;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Spinner[] fineSpinners = new Spinner[8];
    private Spinner carSpinner;
    private Spinner trackSpinner;
    private Spinner modeSpinner;
    private LinearLayout finePanel;
    private TextView result;
    private TextView liveStatus;
    private Button onlineButton;
    private Button saveButton;
    private String currentPlan = "";
    private String onlineId = "";

    private static final String[][] FINE_TUNE = {
            {"Reifendruck: Standard", "Reifendruck: Vorderachse -1 Klick", "Reifendruck: Hinterachse -1 Klick", "Reifendruck: alle -1 Klick für Langstrecke"},
            {"Traktionskontrolle: Standard", "Traktionskontrolle: TC 1", "Traktionskontrolle: TC 2", "Traktionskontrolle: TC 3"},
            {"Bremsbalance: Standard", "Bremsbalance: +1 Klick nach vorn", "Bremsbalance: -1 Klick nach hinten"},
            {"Differenzial: Standard", "Differenzial: Schub +1, Zug -1 für Stabilität", "Differenzial: Schub -1 für mehr Rotation"},
            {"Stabilisatoren: Standard", "Stabilisatoren: hinten -1 Klick", "Stabilisatoren: vorn -1 Klick", "Stabilisatoren: hinten +1 Klick"},
            {"Federn & Dämpfer: Standard", "Federn & Dämpfer: hinten weicher", "Federn & Dämpfer: Curbs weicher", "Federn & Dämpfer: direkter"},
            {"Bodenfreiheit: Standard", "Bodenfreiheit: hinten +1 Klick", "Bodenfreiheit: vorn +1 Klick", "Bodenfreiheit: beide +1 Klick"},
            {"Heckflügel: Standard", "Heckflügel: +1 Klick für Stabilität", "Heckflügel: +2 Klicks für maximale Ruhe", "Heckflügel: -1 Klick für Topspeed"}
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildScreen());
        refreshLiveIndex(LiveSetupIndex.cached(this).checkedAt() == 0);
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = column();
        root.setPadding(dp(18), dp(22), dp(18), dp(36));

        TextView title = label("THE GODFATHER OF EVO", 26, TEXT, true);
        root.addView(title);
        root.addView(label("AC EVO SETUP ENGINEER", 14, YELLOW, true), margins(0, 3, 0, 2));
        root.addView(label("VERSION 1.1.1 · CLEAN BUILD", 12, MUTED, false), margins(0, 0, 0, 20));

        root.addView(section("FAHRZEUG"));
        carSpinner = spinner(Catalog.CARS);
        root.addView(carSpinner, margins(0, 7, 0, 16));

        root.addView(section("STRECKE / LAYOUT"));
        trackSpinner = spinner(Catalog.TRACKS);
        root.addView(trackSpinner, margins(0, 7, 0, 16));

        root.addView(section("SETUP-PROFIL"));
        String[] modes = new String[SetupEngine.Mode.values().length];
        for (int i = 0; i < modes.length; i++) modes[i] = SetupEngine.Mode.values()[i].toString();
        modeSpinner = spinner(modes);
        root.addView(modeSpinner, margins(0, 7, 0, 16));

        Button accordion = yellowButton("FEINTUNING (8 BEREICHE)  ▼");
        root.addView(accordion, margins(0, 2, 0, 8));
        finePanel = column();
        finePanel.setVisibility(View.GONE);
        finePanel.setPadding(dp(12), dp(8), dp(12), dp(12));
        finePanel.setBackground(rounded(SURFACE, YELLOW, 1));
        for (int i = 0; i < FINE_TUNE.length; i++) {
            fineSpinners[i] = spinner(FINE_TUNE[i]);
            finePanel.addView(fineSpinners[i], margins(0, 4, 0, 4));
        }
        root.addView(finePanel, margins(0, 0, 0, 14));
        accordion.setOnClickListener(v -> {
            boolean open = finePanel.getVisibility() == View.VISIBLE;
            finePanel.setVisibility(open ? View.GONE : View.VISIBLE);
            accordion.setText(open ? "FEINTUNING (8 BEREICHE)  ▼" : "FEINTUNING (8 BEREICHE)  ▲");
        });

        Button create = yellowButton("STABILES SETUP ERSTELLEN");
        create.setOnClickListener(v -> createPlan());
        root.addView(create, margins(0, 0, 0, 12));

        result = label("FAST CONTROL ist vorausgewählt. Fahrzeug, Strecke und Feintuning wählen – dann Setup erstellen.", 14, TEXT, false);
        result.setPadding(dp(14), dp(14), dp(14), dp(14));
        result.setTextIsSelectable(true);
        result.setBackground(rounded(SURFACE, Color.rgb(62, 62, 62), 1));
        root.addView(result, margins(0, 0, 0, 10));

        saveButton = yellowButton("SETUP-PLAN SPEICHERN");
        saveButton.setEnabled(false);
        saveButton.setAlpha(.45f);
        saveButton.setOnClickListener(v -> savePlan());
        root.addView(saveButton, margins(0, 0, 0, 18));

        root.addView(section("LIVE-QUELLEN"));
        liveStatus = label("Online-Index wird geprüft …", 13, MUTED, false);
        liveStatus.setPadding(dp(12), dp(12), dp(12), dp(12));
        liveStatus.setBackground(rounded(SURFACE, Color.rgb(62, 62, 62), 1));
        root.addView(liveStatus, margins(0, 7, 0, 8));

        Button refresh = yellowButton("ONLINE-BESTAND AKTUALISIEREN");
        refresh.setOnClickListener(v -> refreshLiveIndex(true));
        root.addView(refresh, margins(0, 0, 0, 8));

        onlineButton = yellowButton("EXAKTEN ONLINE-TREFFER ÖFFNEN");
        onlineButton.setVisibility(View.GONE);
        onlineButton.setOnClickListener(v -> {
            if (!onlineId.isEmpty()) startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://setupsmarket.com/setup/" + onlineId)));
        });
        root.addView(onlineButton, margins(0, 0, 0, 20));

        TextView rule = label("Keine Notfall-Datei · kein Spenderauto · kein ähnliches Layout. Online-Treffer werden nur bei Fahrzeug + exaktem Layout + Spielversion 0.8.x angezeigt.", 12, MUTED, false);
        rule.setGravity(Gravity.CENTER);
        root.addView(rule);
        root.addView(label("© 2026 Greenbuddy1976", 12, MUTED, false), margins(0, 14, 0, 0));
        scroll.addView(root);
        return scroll;
    }

    private void createPlan() {
        String car = String.valueOf(carSpinner.getSelectedItem());
        String track = String.valueOf(trackSpinner.getSelectedItem());
        SetupEngine.Mode mode = SetupEngine.Mode.values()[modeSpinner.getSelectedItemPosition()];
        String[] fine = new String[fineSpinners.length];
        for (int i = 0; i < fine.length; i++) fine[i] = String.valueOf(fineSpinners[i].getSelectedItem());
        currentPlan = SetupEngine.build(car, track, mode, fine);
        result.setText(currentPlan);
        saveButton.setEnabled(true);
        saveButton.setAlpha(1f);
        showOnlineMatch(car, track);
    }

    private void showOnlineMatch(String car, String track) {
        LiveSetupIndex.Match match = LiveSetupIndex.find(this, car, track);
        onlineId = match.firstId();
        onlineButton.setVisibility(match.count() > 0 ? View.VISIBLE : View.GONE);
        if (match.count() > 0) {
            liveStatus.setText("Exakt gefunden: " + match.count() + " Online-Setup(s) für diese Kombination. Der Treffer ist im lokalen Index gespeichert.");
        }
    }

    private void refreshLiveIndex(boolean force) {
        LiveSetupIndex.Snapshot cached = LiveSetupIndex.cached(this);
        if (!force && cached.checkedAt() > 0) {
            showSnapshot(cached, true);
            return;
        }
        liveStatus.setText("SetupsMarket und RacePlace werden vollständig geprüft …");
        worker.execute(() -> {
            try {
                LiveSetupIndex.Snapshot snapshot = LiveSetupIndex.refresh(this);
                runOnUiThread(() -> showSnapshot(snapshot, false));
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    LiveSetupIndex.Snapshot old = LiveSetupIndex.cached(this);
                    if (old.checkedAt() > 0) showSnapshot(old, true);
                    else liveStatus.setText("Online-Prüfung derzeit nicht erreichbar. Es wird kein Treffer erfunden. Bitte später aktualisieren.");
                });
            }
        });
    }

    private void showSnapshot(LiveSetupIndex.Snapshot snapshot, boolean cached) {
        String when = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(new Date(snapshot.checkedAt()));
        liveStatus.setText((cached ? "Gespeicherter Online-Index" : "Online-Index aktualisiert")
                + " · " + when + "\nSetupsMarket: " + snapshot.marketSetups() + " Setups / "
                + snapshot.marketCars() + " Autos\nRacePlace: " + snapshot.racePlaceSetups() + " Setup-Dateien");
    }

    private void savePlan() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "Godfather-EVO-Setup-1.1.1.txt");
        startActivityForResult(intent, SAVE_REQUEST);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SAVE_REQUEST || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
            if (out == null) throw new IllegalStateException("Zieldatei nicht geöffnet");
            out.write(currentPlan.getBytes(StandardCharsets.UTF_8));
            liveStatus.setText("Setup-Plan erfolgreich gespeichert.");
        } catch (Exception ex) {
            liveStatus.setText("Speichern fehlgeschlagen: " + ex.getMessage());
        }
    }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new DarkAdapter(values));
        spinner.setPadding(dp(10), 0, dp(10), 0);
        spinner.setMinimumHeight(dp(52));
        spinner.setBackground(rounded(SURFACE_2, YELLOW, 1));
        return spinner;
    }

    private final class DarkAdapter extends ArrayAdapter<String> {
        DarkAdapter(String[] values) { super(MainActivity.this, android.R.layout.simple_spinner_item, values); }
        @Override public View getView(int position, View convertView, ViewGroup parent) {
            return styled(super.getView(position, convertView, parent));
        }
        @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView) super.getDropDownView(position, convertView, parent);
            view.setBackgroundColor(SURFACE_2);
            return styled(view);
        }
        private View styled(View raw) {
            TextView text = (TextView) raw;
            text.setTextColor(TEXT);
            text.setTextSize(15);
            text.setPadding(dp(12), dp(13), dp(12), dp(13));
            return text;
        }
    }

    private Button yellowButton(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(Color.BLACK);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setMinHeight(dp(54));
        b.setBackground(rounded(YELLOW, YELLOW, 0));
        return b;
    }

    private TextView section(String value) { return label(value, 15, YELLOW, true); }
    private TextView label(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(BG);
        return l;
    }
    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return p;
    }
    private GradientDrawable rounded(int color, int stroke, int strokeWidth) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(9));
        if (strokeWidth > 0) d.setStroke(dp(strokeWidth), stroke);
        return d;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
