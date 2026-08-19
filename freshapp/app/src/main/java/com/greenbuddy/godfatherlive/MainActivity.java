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
    private static final int SAVE_REQUEST = 210;

    private static final String[][] FINE_TUNE = {
            {"Reifendruck: Standard", "Reifendruck: vorne -0,2 psi", "Reifendruck: hinten -0,2 psi", "Reifendruck: alle -0,2 psi / Long Run"},
            {"Traktionskontrolle: Standard", "Traktionskontrolle: TC 1", "Traktionskontrolle: TC 2", "Traktionskontrolle: TC 3"},
            {"Bremsbalance: Standard", "Bremsbalance: +0,5 % nach vorn", "Bremsbalance: -0,5 % nach hinten"},
            {"Differenzial: Standard", "Differenzial-Vorspannung: +10", "Differenzial-Vorspannung: -10"},
            {"Stabilisatoren: Standard", "Stabilisator hinten: 5 % weicher", "Stabilisator vorne: 5 % weicher", "Stabilisator hinten: 5 % steifer"},
            {"Federn & Dämpfer: Standard", "Hinterachse: 3 % weicher", "Curbs/Bodenwellen: Fast Damping 8 % weicher", "Dämpfer: 5 % direkter"},
            {"Bodenfreiheit: Standard", "Bodenfreiheit hinten: +2 mm", "Bodenfreiheit vorne: +2 mm", "Bodenfreiheit vorne/hinten: +2 mm"},
            {"Heckflügel: Standard", "Heckflügel: +1", "Heckflügel: +2", "Heckflügel: -1"}
    };

    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Spinner[] fineSpinners = new Spinner[8];
    private final List<GeneratedSetup> generated = new ArrayList<>();

    private Spinner carSpinner;
    private Spinner trackSpinner;
    private Spinner generatedSpinner;
    private LinearLayout finePanel;
    private Button createButton;
    private Button saveButton;
    private Button sourceButton;
    private TextView status;
    private TextView details;
    private SourceSetup templateSource;
    private boolean templateWasExact;
    private int templateWritableFields;
    private byte[] pendingBytes;
    private String pendingFileName = "";
    private String pendingSha = "";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = column();
        root.setPadding(dp(18), dp(22), dp(18), dp(38));

        root.addView(label("THE GODFATHER OF EVO", 26, TEXT, true));
        root.addView(label("AC EVO SETUP ENGINEER", 14, YELLOW, true), margins(0, 3, 0, 2));
        root.addView(label("VERSION 2.1.0 · LIVE + GENERATOR", 12, MUTED, false), margins(0, 0, 0, 18));

        TextView truth = label(
                "Jeder Auftrag startet mit einer frischen 2× LIVE-Suche. Gibt es keinen exakten Treffer, verwendet die App nur eine bestätigte .carsetup-Struktur desselben Fahrzeugs und berechnet daraus fünf neue Setups für die gewählte Strecke. Keine Textdatei und kein fremdes Fahrzeug.",
                13, TEXT, false);
        truth.setPadding(dp(13), dp(12), dp(13), dp(12));
        truth.setBackground(rounded(SURFACE, YELLOW, 1));
        root.addView(truth, margins(0, 0, 0, 18));

        root.addView(section("1 · FAHRZEUG"));
        carSpinner = spinner(Catalog.CARS);
        root.addView(carSpinner, margins(0, 7, 0, 16));

        root.addView(section("2 · STRECKE / LAYOUT"));
        trackSpinner = spinner(Catalog.TRACKS);
        root.addView(trackSpinner, margins(0, 7, 0, 16));

        root.addView(section("3 · FEINTUNING"));
        Button accordion = yellowButton("FEINTUNING (8 BEREICHE)  ▼");
        root.addView(accordion, margins(0, 7, 0, 8));
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

        createButton = yellowButton("LIVE SUCHEN + 5 SETUPS ERSTELLEN");
        createButton.setOnClickListener(v -> createFiveSetups());
        root.addView(createButton, margins(0, 0, 0, 16));

        root.addView(section("4 · ERZEUGTE SETUPS"));
        generatedSpinner = spinner(new String[]{"Noch keine Setups erzeugt"});
        generatedSpinner.setEnabled(false);
        generatedSpinner.setOnItemSelectedListener(selection(this::showSelectedGenerated));
        root.addView(generatedSpinner, margins(0, 7, 0, 8));

        details = label("Nach der LIVE-Suche entstehen hier fünf Varianten: FAST / HOTLAP, FAST CONTROL, STABLE LEARNING, LONG RUN und STABLE + FAST.", 13, MUTED, false);
        details.setPadding(dp(13), dp(12), dp(13), dp(12));
        details.setTextIsSelectable(true);
        details.setBackground(rounded(SURFACE, Color.rgb(62, 62, 62), 1));
        root.addView(details, margins(0, 0, 0, 8));

        saveButton = yellowButton("AUSGEWÄHLTE .CARSETUP SPEICHERN");
        saveButton.setEnabled(false);
        saveButton.setAlpha(.45f);
        saveButton.setOnClickListener(v -> saveSelectedGenerated());
        root.addView(saveButton, margins(0, 0, 0, 8));

        sourceButton = yellowButton("VERWENDETE LIVE-QUELLE ÖFFNEN");
        sourceButton.setVisibility(View.GONE);
        sourceButton.setOnClickListener(v -> openTemplateSource());
        root.addView(sourceButton, margins(0, 0, 0, 18));

        root.addView(section("LIVE- / SELBSTPRÜFBERICHT"));
        status = label("Bereit. Fahrzeug und Strecke wählen, dann 5 Setups erstellen.", 13, MUTED, false);
        status.setPadding(dp(13), dp(12), dp(13), dp(12));
        status.setTextIsSelectable(true);
        status.setBackground(rounded(SURFACE, Color.rgb(62, 62, 62), 1));
        root.addView(status, margins(0, 7, 0, 18));

        TextView footer = label(
                "Quellen: SetupsMarket + RacePlace/DTVR · Binärformat: bestätigte Float32-Setupfelder · © 2026 Greenbuddy1976",
                12, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);

        carSpinner.setOnItemSelectedListener(selection(this::invalidateGenerated));
        trackSpinner.setOnItemSelectedListener(selection(this::invalidateGenerated));
        for (Spinner fine : fineSpinners) fine.setOnItemSelectedListener(selection(this::invalidateGenerated));

        scroll.addView(root);
        return scroll;
    }

    private void createFiveSetups() {
        if (worker.isShutdown()) return;
        final String car = selected(carSpinner);
        final String track = selected(trackSpinner);
        final int[] fine = fineSelections();
        final String fingerprint = fingerprint(car, track, fine);
        clearGenerated();
        setBusy(true, "LIVE-Suche startet: exaktes Fahrzeug + exaktes Layout, Runde 1/2 …");

        worker.execute(() -> {
            List<String> notes = new ArrayList<>();
            try {
                LiveSetupSearch.Result exact = LiveSetupSearch.runTwoRounds(car, track,
                        message -> runOnUiThread(() -> status.setText(message)));
                notes.addAll(exact.notices);
                TemplateChoice choice = findUsableTemplate(exact.setups, true, notes);
                LiveSetupSearch.Result sameCar = null;

                if (choice == null) {
                    runOnUiThread(() -> status.setText(
                            "Kein sicher nutzbarer exakter LIVE-Treffer. Suche jetzt 2× nach einer .carsetup-Struktur desselben Fahrzeugs …"));
                    sameCar = LiveSetupSearch.runTwoRounds(car, "",
                            message -> runOnUiThread(() -> status.setText(message)));
                    notes.addAll(sameCar.notices);
                    choice = findUsableTemplate(sameCar.setups, false, notes);
                }

                if (choice == null) {
                    final LiveSetupSearch.Result sameResult = sameCar;
                    runOnUiThread(() -> {
                        if (!fingerprint.equals(fingerprint(car, track, fine))) {
                            setBusy(false, null);
                            return;
                        }
                        status.setText("❌ Setupwerte wurden berechnet, aber für " + car
                                + " wurde in beiden LIVE-Quellen keine sichere 0.8.x-.carsetup-Struktur desselben Fahrzeugs gefunden.\n"
                                + "Es wird bewusst keine fremde Fahrzeugdatei als Fake-.carsetup ausgegeben."
                                + noticeText(notes));
                        details.setText("Die Berechnungslogik ist vorhanden, aber ohne bestätigte Binärstruktur desselben Fahrzeugs wäre eine ladbare .carsetup-Datei nicht verifizierbar."
                                + (sameResult == null ? "" : "\nSame-Car LIVE-Treffer: " + sameResult.setups.size()));
                        setBusy(false, null);
                    });
                    return;
                }

                List<GeneratedSetup> outputs = new ArrayList<>();
                for (SetupEngine.Profile profile : SetupEngine.Profile.values()) {
                    List<SetupEngine.Change> changes = SetupEngine.changes(car, track, profile, fine);
                    BinarySetupEditor.EditResult edit = BinarySetupEditor.apply(choice.bytes, changes);
                    if (edit.applied == 0) {
                        throw new IllegalStateException(profile.title + ": kein bestätigtes Feld konnte geschrieben werden");
                    }
                    String fileName = QueryLogic.generatedName(car, track, profile);
                    String sha = LiveSetupSearch.sha256(edit.bytes);
                    outputs.add(new GeneratedSetup(profile, fileName, edit.bytes, sha, edit));
                }

                TemplateChoice finalChoice = choice;
                runOnUiThread(() -> applyGenerated(outputs, finalChoice, fingerprint, car, track, fine, notes));
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    if (!fingerprint.equals(fingerprint(car, track, fine))) {
                        setBusy(false, null);
                        return;
                    }
                    clearGenerated();
                    status.setText("❌ Erzeugung fehlgeschlagen: " + message(ex) + noticeText(notes));
                    setBusy(false, null);
                });
            }
        });
    }

    private TemplateChoice findUsableTemplate(List<SourceSetup> candidates, boolean exact,
                                               List<String> notes) {
        if (candidates == null || candidates.isEmpty()) return null;
        List<SourceSetup> sorted = new ArrayList<>(candidates);
        sorted.sort(exact
                ? Comparator.comparingInt(s -> s.source == SourceSetup.Source.SETUPSMARKET ? 0 : 1)
                : Comparator.comparingInt(s -> s.source == SourceSetup.Source.RACEPLACE ? 0 : 1));

        int tries = 0;
        for (SourceSetup source : sorted) {
            if (tries++ >= 12) break;
            try {
                byte[] bytes = LiveSetupSearch.downloadFresh(source);
                int writable = BinarySetupEditor.countWritableFields(bytes);
                if (writable >= 6) return new TemplateChoice(source, bytes, exact, writable);
                notes.add(source.displayLabel() + ": nur " + writable + " sichere Felder");
            } catch (Exception ex) {
                notes.add(source.displayLabel() + ": " + message(ex));
            }
        }
        return null;
    }

    private void applyGenerated(List<GeneratedSetup> outputs, TemplateChoice choice,
                                String originalFingerprint, String car, String track,
                                int[] fine, List<String> notes) {
        if (!originalFingerprint.equals(fingerprint(car, track, fine))) {
            clearGenerated();
            status.setText("Auswahl wurde während der Suche geändert. Bitte die Erstellung erneut starten.");
            setBusy(false, null);
            return;
        }

        generated.clear();
        generated.addAll(outputs);
        templateSource = choice.source;
        templateWasExact = choice.exact;
        templateWritableFields = choice.writableFields;

        String[] labels = new String[generated.size()];
        for (int i = 0; i < labels.length; i++) labels[i] = generated.get(i).profile.toString();
        setValues(generatedSpinner, labels);
        generatedSpinner.setEnabled(true);
        saveButton.setEnabled(true);
        saveButton.setAlpha(1f);
        sourceButton.setVisibility(View.VISIBLE);
        showSelectedGenerated();

        String provider = templateSource.source == SourceSetup.Source.SETUPSMARKET
                ? "SetupsMarket" : "RacePlace/DTVR";
        status.setText("✅ 5 .carsetup-Dateien erzeugt und intern erneut geparst\n"
                + "LIVE-Basis: " + (choice.exact ? "EXAKTER Auto+Strecke-Treffer" : "SAME-CAR Struktur; Werte neu berechnet")
                + "\nQuelle: " + provider + " · " + templateSource.car + " · " + templateSource.track
                + " · v" + templateSource.gameVersion
                + "\nBestätigte schreibbare Felder in der Struktur: " + templateWritableFields
                + "\nProfile: FAST / HOTLAP · FAST CONTROL · STABLE LEARNING · LONG RUN · STABLE + FAST"
                + (QueryLogic.exact(car, "Ford Mustang GT3")
                    ? "\nMustang-Regel: STABLE + FAST erzwingt TC1 = 1, sofern das Feld im Fahrzeug vorhanden ist." : "")
                + noticeText(notes));
        setBusy(false, null);
    }

    private void showSelectedGenerated() {
        GeneratedSetup setup = selectedGenerated();
        if (setup == null) return;
        String provider = templateSource == null ? "" :
                (templateSource.source == SourceSetup.Source.SETUPSMARKET ? "SetupsMarket" : "RacePlace/DTVR");
        StringBuilder text = new StringBuilder();
        text.append(SetupEngine.summary(selected(carSpinner), selected(trackSpinner), setup.profile, setup.edit))
                .append("\n\nDatei: ").append(setup.fileName)
                .append("\nGröße: ").append(setup.bytes.length).append(" Bytes")
                .append("\nSHA-256: ").append(setup.sha);
        if (templateSource != null) {
            text.append("\n\nLIVE-Strukturbasis: ").append(provider)
                    .append(" · ").append(templateWasExact ? "exakte Strecke" : "gleiches Fahrzeug")
                    .append(" · ").append(templateSource.track)
                    .append(" · v").append(templateSource.gameVersion);
        }
        details.setText(text.toString());
    }

    private void saveSelectedGenerated() {
        GeneratedSetup setup = selectedGenerated();
        if (setup == null) return;
        pendingBytes = setup.bytes;
        pendingFileName = setup.fileName;
        pendingSha = setup.sha;
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
            status.setText("Speichern abgebrochen. Die erzeugten Setups bleiben in der App erhalten.");
            return;
        }
        if (pendingBytes == null) {
            status.setText("Speichern abgebrochen: keine geprüften Binärdaten vorhanden.");
            return;
        }
        try (OutputStream output = getContentResolver().openOutputStream(data.getData(), "w")) {
            if (output == null) throw new IllegalStateException("Zieldatei konnte nicht geöffnet werden");
            output.write(pendingBytes);
            output.flush();
            status.setText("✅ .carsetup gespeichert\nDatei: " + pendingFileName
                    + "\nGröße: " + pendingBytes.length + " Bytes\nSHA-256: " + pendingSha
                    + "\nDie Datei wurde vor dem Speichern als Binärstruktur selbstgeprüft.");
        } catch (Exception ex) {
            status.setText("Speichern fehlgeschlagen: " + message(ex));
        } finally {
            pendingBytes = null;
        }
    }

    private void openTemplateSource() {
        if (templateSource == null) return;
        String url = templateSource.source == SourceSetup.Source.SETUPSMARKET
                ? "https://setupsmarket.com/setup/" + templateSource.sourceId
                : "https://raceplace.racing/downloads/";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void invalidateGenerated() {
        if (generated.isEmpty()) return;
        clearGenerated();
        status.setText("Auswahl geändert. Für die neue Kombination wird beim nächsten Erstellen wieder frisch LIVE gesucht.");
    }

    private void clearGenerated() {
        generated.clear();
        templateSource = null;
        templateWasExact = false;
        templateWritableFields = 0;
        setValues(generatedSpinner, new String[]{"Noch keine Setups erzeugt"});
        generatedSpinner.setEnabled(false);
        saveButton.setEnabled(false);
        saveButton.setAlpha(.45f);
        sourceButton.setVisibility(View.GONE);
    }

    private GeneratedSetup selectedGenerated() {
        if (generated.isEmpty() || generatedSpinner == null) return null;
        int index = generatedSpinner.getSelectedItemPosition();
        return index >= 0 && index < generated.size() ? generated.get(index) : null;
    }

    private int[] fineSelections() {
        int[] out = new int[fineSpinners.length];
        for (int i = 0; i < fineSpinners.length; i++) out[i] = fineSpinners[i].getSelectedItemPosition();
        return out;
    }

    private String fingerprint(String car, String track, int[] fine) {
        StringBuilder out = new StringBuilder(QueryLogic.key(car)).append('|').append(QueryLogic.key(track));
        for (int value : fine) out.append('|').append(value);
        return out.toString();
    }

    private String noticeText(List<String> notices) {
        if (notices == null || notices.isEmpty()) return "";
        StringBuilder out = new StringBuilder("\n\nQuellenhinweise:");
        int shown = 0;
        for (String notice : notices) {
            if (notice == null || notice.isBlank()) continue;
            if (shown++ >= 6) {
                out.append("\n• weitere Hinweise ausgeblendet");
                break;
            }
            out.append("\n• ").append(notice);
        }
        return out.toString();
    }

    private void setBusy(boolean busy, String message) {
        createButton.setEnabled(!busy);
        createButton.setAlpha(busy ? .45f : 1f);
        if (busy) {
            saveButton.setEnabled(false);
            saveButton.setAlpha(.45f);
        } else if (!generated.isEmpty()) {
            saveButton.setEnabled(true);
            saveButton.setAlpha(1f);
        }
        if (message != null) status.setText(message);
    }

    private AdapterView.OnItemSelectedListener selection(Runnable action) {
        return new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                action.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new DarkAdapter(values));
        spinner.setPadding(dp(10), 0, dp(10), 0);
        spinner.setMinimumHeight(dp(52));
        spinner.setBackground(rounded(SURFACE_2, YELLOW, 1));
        return spinner;
    }

    private void setValues(Spinner spinner, String[] values) {
        if (spinner == null) return;
        spinner.setAdapter(new DarkAdapter(values));
    }

    private String selected(Spinner spinner) {
        Object value = spinner == null ? null : spinner.getSelectedItem();
        return value == null ? "" : String.valueOf(value).trim();
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
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String message(Exception ex) {
        String value = ex.getMessage();
        return value == null || value.isBlank() ? ex.getClass().getSimpleName() : value;
    }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private static final class TemplateChoice {
        final SourceSetup source;
        final byte[] bytes;
        final boolean exact;
        final int writableFields;

        TemplateChoice(SourceSetup source, byte[] bytes, boolean exact, int writableFields) {
            this.source = source;
            this.bytes = bytes;
            this.exact = exact;
            this.writableFields = writableFields;
        }
    }

    private static final class GeneratedSetup {
        final SetupEngine.Profile profile;
        final String fileName;
        final byte[] bytes;
        final String sha;
        final BinarySetupEditor.EditResult edit;

        GeneratedSetup(SetupEngine.Profile profile, String fileName, byte[] bytes,
                       String sha, BinarySetupEditor.EditResult edit) {
            this.profile = profile;
            this.fileName = fileName;
            this.bytes = bytes;
            this.sha = sha;
            this.edit = edit;
        }
    }
}
