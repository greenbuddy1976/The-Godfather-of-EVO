package com.greenbuddy.godfatherlive;

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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(5, 5, 5);
    private static final int SURFACE = Color.rgb(20, 20, 20);
    private static final int SURFACE_2 = Color.rgb(31, 31, 31);
    private static final int YELLOW = Color.rgb(255, 212, 0);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(190, 190, 190);
    private static final int GREEN = Color.rgb(105, 220, 130);
    private static final int SAVE_REQUEST = 200;

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final List<SourceSetup> catalog = new ArrayList<>();
    private final List<SourceSetup> liveMatches = new ArrayList<>();

    private Spinner carSpinner;
    private Spinner trackSpinner;
    private Spinner styleSpinner;
    private Spinner fineSpinner;
    private Spinner sourceSpinner;
    private Button catalogButton;
    private Button searchButton;
    private Button saveButton;
    private Button browserButton;
    private TextView status;
    private TextView sourceDetails;
    private boolean catalogReady;
    private boolean suppressSelectionEvents;
    private byte[] pendingBytes;
    private String pendingFileName = "";
    private String pendingSha = "";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildScreen());
        loadLiveCatalog();
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = column();
        root.setPadding(dp(18), dp(22), dp(18), dp(38));

        root.addView(label("THE GODFATHER OF EVO", 26, TEXT, true));
        root.addView(label("LIVE .CARSETUP ENGINEER", 14, YELLOW, true), margins(0, 3, 0, 2));
        root.addView(label("VERSION 2.0.0 · NEUAUFBAU", 12, MUTED, false), margins(0, 0, 0, 18));

        TextView truth = label("Nur echte Online-Dateien: 2 vollständige LIVE-Runden, exaktes Auto, exaktes Layout und AC EVO 0.8.x. Keine Textdatei, kein Spenderauto, kein alter Setup-Cache.", 13, TEXT, false);
        truth.setPadding(dp(13), dp(12), dp(13), dp(12));
        truth.setBackground(rounded(SURFACE, YELLOW, 1));
        root.addView(truth, margins(0, 0, 0, 18));

        root.addView(section("1 · LIVE-KATALOG"));
        catalogButton = yellowButton("LIVE-KATALOG 2× NEU LADEN");
        catalogButton.setOnClickListener(v -> loadLiveCatalog());
        root.addView(catalogButton, margins(0, 7, 0, 16));

        root.addView(section("2 · FAHRZEUG"));
        carSpinner = spinner(new String[]{"Live-Katalog wird geladen …"});
        root.addView(carSpinner, margins(0, 7, 0, 16));

        root.addView(section("3 · STRECKE / EXAKTES LAYOUT"));
        trackSpinner = spinner(new String[]{"Bitte zuerst LIVE-Katalog laden"});
        root.addView(trackSpinner, margins(0, 7, 0, 16));

        root.addView(section("4 · STIL-PRÄFERENZ"));
        styleSpinner = spinner(enumLabels(QueryLogic.Style.values()));
        root.addView(styleSpinner, margins(0, 7, 0, 16));

        root.addView(section("5 · OPTIONALER WUNSCH"));
        fineSpinner = spinner(enumLabels(QueryLogic.FineTune.values()));
        root.addView(fineSpinner, margins(0, 7, 0, 5));
        root.addView(label("Der Stil und der optionale Wunsch sortieren nur passende Live-Treffer. Die originale Binärdatei wird niemals heimlich verändert.", 12, MUTED, false), margins(0, 0, 0, 16));

        searchButton = yellowButton("2× LIVE-SUCHE STARTEN");
        searchButton.setEnabled(false);
        searchButton.setAlpha(.45f);
        searchButton.setOnClickListener(v -> searchExactSetups());
        root.addView(searchButton, margins(0, 0, 0, 16));

        root.addView(section("6 · BESTÄTIGTE QUELLE"));
        sourceSpinner = spinner(new String[]{"Noch kein bestätigter Treffer"});
        sourceSpinner.setEnabled(false);
        root.addView(sourceSpinner, margins(0, 7, 0, 8));

        sourceDetails = label("Nach der doppelten Suche stehen hier nur Treffer, die in beiden Runden identisch vorhanden waren.", 13, MUTED, false);
        sourceDetails.setPadding(dp(13), dp(12), dp(13), dp(12));
        sourceDetails.setBackground(rounded(SURFACE, Color.rgb(62, 62, 62), 1));
        root.addView(sourceDetails, margins(0, 0, 0, 8));

        browserButton = yellowButton("QUELLE IM BROWSER ÖFFNEN");
        browserButton.setVisibility(View.GONE);
        browserButton.setOnClickListener(v -> openSelectedSource());
        root.addView(browserButton, margins(0, 0, 0, 8));

        saveButton = yellowButton("ECHTE .CARSETUP SPEICHERN");
        saveButton.setEnabled(false);
        saveButton.setAlpha(.45f);
        saveButton.setOnClickListener(v -> saveFreshBinary());
        root.addView(saveButton, margins(0, 0, 0, 18));

        root.addView(section("LIVE-PRÜFBERICHT"));
        status = label("Die Live-Prüfung startet …", 13, MUTED, false);
        status.setPadding(dp(13), dp(12), dp(13), dp(12));
        status.setTextIsSelectable(true);
        status.setBackground(rounded(SURFACE, Color.rgb(62, 62, 62), 1));
        root.addView(status, margins(0, 7, 0, 18));

        TextView footer = label("Quellen: SetupsMarket + RacePlace/DTVR · Speichern ausschließlich als .carsetup-Binärdatei · © 2026 Greenbuddy1976", 12, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);

        carSpinner.setOnItemSelectedListener(selection(() -> {
            if (!catalogReady) return;
            populateTracks(selected(carSpinner));
            invalidateLiveResult();
        }));
        trackSpinner.setOnItemSelectedListener(selection(this::invalidateLiveResult));
        styleSpinner.setOnItemSelectedListener(selection(this::invalidateLiveResult));
        fineSpinner.setOnItemSelectedListener(selection(this::invalidateLiveResult));
        sourceSpinner.setOnItemSelectedListener(selection(this::showSelectedSource));

        scroll.addView(root);
        return scroll;
    }

    private void loadLiveCatalog() {
        if (worker.isShutdown()) return;
        setBusy(true, "LIVE-Katalog: beide Quellen werden zweimal vollständig neu gelesen …");
        catalogReady = false;
        worker.execute(() -> {
            try {
                LiveSetupSearch.Result result = LiveSetupSearch.runTwoRounds("", "",
                        message -> runOnUiThread(() -> status.setText(message)));
                runOnUiThread(() -> applyCatalog(result));
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    status.setText("LIVE-Katalog fehlgeschlagen: " + message(ex)
                            + "\nEs wird kein alter Bestand verwendet.");
                    setBusy(false, null);
                });
            }
        });
    }

    private void applyCatalog(LiveSetupSearch.Result result) {
        catalog.clear();
        catalog.addAll(result.setups);
        Set<String> cars = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (SourceSetup setup : catalog) cars.add(setup.car);
        if (cars.isEmpty()) {
            status.setText("Beide LIVE-Runden sind beendet, aber es gibt keine doppelt bestätigten 0.8.x-Dateien.");
            setBusy(false, null);
            return;
        }
        suppressSelectionEvents = true;
        setValues(carSpinner, cars.toArray(new String[0]));
        catalogReady = true;
        populateTracks(selected(carSpinner));
        suppressSelectionEvents = false;
        setBusy(false, null);
        enableSearch(true);
        status.setText("✅ LIVE-Katalog bestätigt\nRunde 1: " + result.firstRoundCount
                + " aktuelle Dateien\nRunde 2: " + result.secondRoundCount
                + " aktuelle Dateien\nDoppelt bestätigt: " + catalog.size()
                + " Dateien / " + cars.size() + " Fahrzeuge"
                + noticeText(result.notices));
    }

    private void populateTracks(String car) {
        Set<String> tracks = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (SourceSetup setup : catalog) if (QueryLogic.exact(car, setup.car)) tracks.add(setup.track);
        setValues(trackSpinner, tracks.isEmpty()
                ? new String[]{"Kein aktuelles 0.8.x-Layout gefunden"}
                : tracks.toArray(new String[0]));
    }

    private void searchExactSetups() {
        if (!catalogReady) return;
        String car = selected(carSpinner);
        String track = selected(trackSpinner);
        if (car.isEmpty() || track.startsWith("Kein aktuelles")) return;
        QueryLogic.Style style = QueryLogic.Style.values()[styleSpinner.getSelectedItemPosition()];
        QueryLogic.FineTune fine = QueryLogic.FineTune.values()[fineSpinner.getSelectedItemPosition()];
        String fingerprint = fingerprint();
        setBusy(true, "Exakte Kombination wird in zwei neuen LIVE-Runden geprüft …");
        worker.execute(() -> {
            try {
                LiveSetupSearch.Result result = LiveSetupSearch.runTwoRounds(car, track,
                        message -> runOnUiThread(() -> status.setText(message)));
                List<SourceSetup> sorted = new ArrayList<>(result.setups);
                sorted.sort(QueryLogic.preferenceComparator(style, fine));
                runOnUiThread(() -> applySearchResult(result, sorted, fingerprint));
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    clearLiveMatches();
                    status.setText("LIVE-Suche fehlgeschlagen: " + message(ex)
                            + "\nSpeichern bleibt gesperrt; es wird nichts erfunden.");
                    setBusy(false, null);
                });
            }
        });
    }

    private void applySearchResult(LiveSetupSearch.Result result, List<SourceSetup> matches,
                                   String fingerprint) {
        if (!fingerprint.equals(fingerprint())) {
            clearLiveMatches();
            status.setText("Auswahl wurde während der Suche geändert. Bitte die 2× LIVE-Suche erneut starten.");
            setBusy(false, null);
            return;
        }
        liveMatches.clear();
        liveMatches.addAll(matches);
        if (matches.isEmpty()) {
            clearLiveMatches();
            status.setText("Kein exakter, in beiden LIVE-Runden bestätigter 0.8.x-Treffer für\n"
                    + selected(carSpinner) + " · " + selected(trackSpinner)
                    + "\nSpeichern bleibt gesperrt. Keine Ersatzdatei, kein ähnliches Layout."
                    + noticeText(result.notices));
            setBusy(false, null);
            return;
        }
        String[] labels = new String[matches.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = matches.get(i).displayLabel();
        suppressSelectionEvents = true;
        setValues(sourceSpinner, labels);
        sourceSpinner.setEnabled(true);
        suppressSelectionEvents = false;
        saveButton.setEnabled(true);
        saveButton.setAlpha(1f);
        browserButton.setVisibility(View.VISIBLE);
        showSelectedSource();
        status.setText("✅ 2× LIVE bestätigt\nRunde 1: " + result.firstRoundCount
                + " exakte Treffer\nRunde 2: " + result.secondRoundCount
                + " exakte Treffer\nFreigegeben: " + matches.size()
                + " echte .carsetup-Datei(en)"
                + noticeText(result.notices));
        setBusy(false, null);
    }

    private void showSelectedSource() {
        SourceSetup setup = selectedSetup();
        if (setup == null) return;
        String provider = setup.source == SourceSetup.Source.SETUPSMARKET
                ? "SetupsMarket" : "RacePlace/DTVR";
        sourceDetails.setText("Quelle: " + provider + "\nAuto: " + setup.car
                + "\nLayout: " + setup.track + "\nSpielversion: " + setup.gameVersion
                + "\nDatei: " + setup.fileName
                + "\n\nStil/Wunsch dienen nur zur Sortierung. Gespeichert wird die unveränderte Originaldatei.");
    }

    private void saveFreshBinary() {
        SourceSetup setup = selectedSetup();
        if (setup == null) return;
        setBusy(true, "Originaldatei wird jetzt frisch von der bestätigten Quelle geladen und geprüft …");
        worker.execute(() -> {
            try {
                byte[] bytes = LiveSetupSearch.downloadFresh(setup);
                String sha = LiveSetupSearch.sha256(bytes);
                runOnUiThread(() -> chooseSaveLocation(setup.fileName, bytes, sha));
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    pendingBytes = null;
                    status.setText("Speichern gesperrt: Die Quelle lieferte keine gültige .carsetup-Datei.\n"
                            + message(ex));
                    setBusy(false, null);
                });
            }
        });
    }

    private void chooseSaveLocation(String fileName, byte[] bytes, String sha) {
        pendingBytes = bytes;
        pendingFileName = QueryLogic.safeCarsetupName(fileName, selected(carSpinner), selected(trackSpinner));
        pendingSha = sha;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, pendingFileName);
        startActivityForResult(intent, SAVE_REQUEST);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SAVE_REQUEST) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            pendingBytes = null;
            setBusy(false, null);
            status.setText("Speichern abgebrochen. Es wurde keine Datei verändert.");
            return;
        }
        if (pendingBytes == null) {
            setBusy(false, null);
            status.setText("Speichern abgebrochen: Es liegen keine geprüften Binärdaten vor.");
            return;
        }
        try (OutputStream output = getContentResolver().openOutputStream(data.getData(), "w")) {
            if (output == null) throw new IllegalStateException("Zieldatei konnte nicht geöffnet werden");
            output.write(pendingBytes);
            output.flush();
            status.setText("✅ Echte .carsetup gespeichert\nDatei: " + pendingFileName
                    + "\nGröße: " + pendingBytes.length + " Bytes\nSHA-256: " + pendingSha);
        } catch (Exception ex) {
            status.setText("Speichern fehlgeschlagen: " + message(ex));
        } finally {
            pendingBytes = null;
            setBusy(false, null);
        }
    }

    private void openSelectedSource() {
        SourceSetup setup = selectedSetup();
        if (setup == null) return;
        String address = setup.source == SourceSetup.Source.SETUPSMARKET
                ? "https://setupsmarket.com/setup/" + setup.sourceId
                : "https://raceplace.racing/downloads/";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(address)));
    }

    private void invalidateLiveResult() {
        if (suppressSelectionEvents || !catalogReady) return;
        clearLiveMatches();
        status.setText("Auswahl geändert. Bitte 2× LIVE-Suche starten; alte Treffer werden nicht weiterverwendet.");
    }

    private void clearLiveMatches() {
        liveMatches.clear();
        setValues(sourceSpinner, new String[]{"Noch kein bestätigter Treffer"});
        sourceSpinner.setEnabled(false);
        saveButton.setEnabled(false);
        saveButton.setAlpha(.45f);
        browserButton.setVisibility(View.GONE);
        sourceDetails.setText("Nach der doppelten Suche stehen hier nur Treffer, die in beiden Runden identisch vorhanden waren.");
    }

    private SourceSetup selectedSetup() {
        int index = sourceSpinner.getSelectedItemPosition();
        return index >= 0 && index < liveMatches.size() ? liveMatches.get(index) : null;
    }

    private String fingerprint() {
        return QueryLogic.key(selected(carSpinner)) + "|" + QueryLogic.key(selected(trackSpinner))
                + "|" + styleSpinner.getSelectedItemPosition() + "|" + fineSpinner.getSelectedItemPosition();
    }

    private void setBusy(boolean busy, String message) {
        catalogButton.setEnabled(!busy);
        searchButton.setEnabled(!busy && catalogReady);
        if (busy) {
            catalogButton.setAlpha(.45f);
            searchButton.setAlpha(.45f);
            saveButton.setEnabled(false);
            saveButton.setAlpha(.45f);
            if (message != null) status.setText(message);
        } else {
            catalogButton.setAlpha(1f);
            searchButton.setAlpha(catalogReady ? 1f : .45f);
            if (!liveMatches.isEmpty()) {
                saveButton.setEnabled(true);
                saveButton.setAlpha(1f);
            }
        }
    }

    private void enableSearch(boolean enabled) {
        searchButton.setEnabled(enabled);
        searchButton.setAlpha(enabled ? 1f : .45f);
    }

    private String noticeText(List<String> notices) {
        if (notices == null || notices.isEmpty()) return "";
        Set<String> unique = new TreeSet<>(Comparator.naturalOrder());
        unique.addAll(notices);
        return "\nHinweise: " + String.join(" · ", unique);
    }

    private AdapterView.OnItemSelectedListener selection(Runnable action) {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!suppressSelectionEvents) action.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
    }

    private String[] enumLabels(Object[] values) {
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) labels[i] = values[i].toString();
        return labels;
    }

    private void setValues(Spinner spinner, String[] values) {
        spinner.setAdapter(new DarkAdapter(values));
    }

    private String selected(Spinner spinner) {
        Object value = spinner.getSelectedItem();
        return value == null ? "" : value.toString();
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
        DarkAdapter(String[] values) {
            super(MainActivity.this, android.R.layout.simple_spinner_item, values);
        }
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
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(Color.BLACK);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setMinHeight(dp(54));
        button.setBackground(rounded(YELLOW, YELLOW, 0));
        return button;
    }

    private TextView section(String value) { return label(value, 15, YELLOW, true); }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(BG);
        return layout;
    }

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private GradientDrawable rounded(int color, int stroke, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(9));
        if (strokeWidth > 0) drawable.setStroke(dp(strokeWidth), stroke);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String message(Exception ex) {
        String value = ex.getMessage();
        return value == null || value.isBlank() ? ex.getClass().getSimpleName() : value;
    }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
