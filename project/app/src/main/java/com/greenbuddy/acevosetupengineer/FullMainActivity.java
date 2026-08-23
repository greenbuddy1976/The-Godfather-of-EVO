package com.greenbuddy.acevosetupengineer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.greenbuddy.acevosetupengineer.binary.CarSetupInspection;
import com.greenbuddy.acevosetupengineer.binary.CarSetupInspector;
import com.greenbuddy.acevosetupengineer.catalog.CatalogData;
import com.greenbuddy.acevosetupengineer.catalog.CatalogRepository;
import com.greenbuddy.acevosetupengineer.core.AndroidExactCache;
import com.greenbuddy.acevosetupengineer.core.AndroidStructureCarrierCache;
import com.greenbuddy.acevosetupengineer.core.BundledStructureCarrierRepository;
import com.greenbuddy.acevosetupengineer.core.ExactFineTuneService;
import com.greenbuddy.acevosetupengineer.core.LiveSearchCoordinator;
import com.greenbuddy.acevosetupengineer.core.LiveSearchSummary;
import com.greenbuddy.acevosetupengineer.core.PreloadedSetupRepository;
import com.greenbuddy.acevosetupengineer.core.RacePlacePackageProvider;
import com.greenbuddy.acevosetupengineer.core.SelfCalcExportService;
import com.greenbuddy.acevosetupengineer.core.SetupsMarketProvider;
import com.greenbuddy.acevosetupengineer.core.SetupStockSync;
import com.greenbuddy.acevosetupengineer.core.VerifiedStructureCarrier;
import com.greenbuddy.acevosetupengineer.engineering.EngineeringProfile;
import com.greenbuddy.acevosetupengineer.engineering.EngineeringSetup;
import com.greenbuddy.acevosetupengineer.engineering.FineTuneEngine;
import com.greenbuddy.acevosetupengineer.engineering.FineTuneInterpretation;
import com.greenbuddy.acevosetupengineer.engineering.FineTuneInterpreter;
import com.greenbuddy.acevosetupengineer.engineering.FineTunePlan;
import com.greenbuddy.acevosetupengineer.engineering.FineTunePlanCombiner;
import com.greenbuddy.acevosetupengineer.engineering.FineTunePlanner;
import com.greenbuddy.acevosetupengineer.engineering.ModeFineTunePlanner;
import com.greenbuddy.acevosetupengineer.engineering.RangeProfileRepository;
import com.greenbuddy.acevosetupengineer.engineering.SelfCalcEngine;
import com.greenbuddy.acevosetupengineer.engineering.SetupValidationException;
import com.greenbuddy.acevosetupengineer.engineering.TrackProfile;
import com.greenbuddy.acevosetupengineer.engineering.TrackProfileRepository;
import com.greenbuddy.acevosetupengineer.model.CatalogItem;
import com.greenbuddy.acevosetupengineer.model.SetupMode;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.ui.VehicleThumbnailLoader;
import com.greenbuddy.acevosetupengineer.util.Hashing;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FullMainActivity extends Activity {
    private static final int EXPORT_REQUEST = 5107;
    private static final int CARRIER_IMPORT_REQUEST = 5108;
    private static final int BG = Color.rgb(5, 5, 5);
    private static final int CARD = Color.rgb(24, 24, 24);
    private static final int TEXT = Color.WHITE;
    private static final int MUTED = Color.rgb(190, 190, 190);
    private static final int YELLOW = Color.rgb(255, 212, 0);

    private static final String[] PROBLEMS = {
            "Kein Problem",
            "Untersteuern – Auto schiebt über die Vorderachse",
            "Übersteuern – Heck bricht aus",
            "Heck nervös / instabil",
            "Auto zu träge beim Einlenken",
            "Auto reagiert zu aggressiv / zu nervös",
            "Auto instabil beim Bremsen",
            "Schlechte Traktion beim Herausbeschleunigen",
            "Auto unruhig über Curbs / Bodenwellen",
            "Mehr Heckstabilität / mehr Abtrieb gewünscht",
            "Mehr Topspeed / weniger Abtrieb gewünscht"
    };

    private CatalogData catalog;
    private Spinner vehicleSpinner;
    private Spinner layoutSpinner;
    private Spinner generatedSpinner;
    private ImageView vehicleImage;
    private TextView vehicleName;
    private TextView status;
    private Button exportButton;
    private final CheckBox[] profileChecks = new CheckBox[SetupMode.values().length];
    private final Spinner[] problemSpinners = new Spinner[4];
    private final List<GeneratedExport> generated = new ArrayList<>();

    private VehicleThumbnailLoader thumbnailLoader;
    private RangeProfileRepository ranges;
    private TrackProfileRepository tracks;
    private BundledStructureCarrierRepository bundled;
    private PreloadedSetupRepository preloaded;
    private AndroidStructureCarrierCache structureCache;
    private SetupStockSync stockSync;
    private LiveSearchCoordinator liveSearch;

    private final ExecutorService setupExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean setupRunning = new AtomicBoolean();

    private byte[] manualCarrier;
    private String manualCarrierSignature;
    private byte[] pendingExportBytes;
    private String pendingExportName;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            catalog = CatalogRepository.load(this, Collections.emptySet());
            thumbnailLoader = new VehicleThumbnailLoader(this);
            ranges = new RangeProfileRepository(this);
            tracks = new TrackProfileRepository(this);
            bundled = new BundledStructureCarrierRepository(this);
            preloaded = new PreloadedSetupRepository(this);
            structureCache = new AndroidStructureCarrierCache(this);
            stockSync = new SetupStockSync(this);
            liveSearch = new LiveSearchCoordinator(
                    Arrays.asList(new RacePlacePackageProvider(), new SetupsMarketProvider()),
                    new AndroidExactCache(this), structureCache,
                    message -> runOnUiThread(() -> status.setText("LIVE: " + message)));
            setContentView(buildScreen());
            syncExecutor.execute(() -> stockSync.syncOnce());
        } catch (Exception ex) {
            TextView fail = text("Start fehlgeschlagen: " + safe(ex), 15, TEXT);
            fail.setPadding(dp(20), dp(20), dp(20), dp(20));
            fail.setBackgroundColor(BG);
            setContentView(fail);
        }
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = column();
        root.setPadding(dp(16), dp(18), dp(16), dp(32));

        TextView title = text("THE GODFATHER OF EVO", 25, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);
        root.addView(text("FULL SETUP ENGINEER · 2.2.0", 12, YELLOW), margins(0, 2, 0, 15));

        root.addView(section("1 · FAHRZEUG"));
        vehicleImage = new ImageView(this);
        vehicleImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        vehicleImage.setMinimumHeight(dp(150));
        vehicleImage.setBackgroundColor(CARD);
        root.addView(vehicleImage, fixed(dp(150), 0, 5));
        vehicleName = text("", 17, TEXT);
        vehicleName.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(vehicleName, margins(0, 2, 0, 5));
        vehicleSpinner = catalogSpinner(catalog.vehicles);
        root.addView(vehicleSpinner, margins(0, 3, 0, 14));
        vehicleSpinner.setOnItemSelectedListener(new SimpleSelection() {
            @Override public void selected() {
                manualCarrier = null;
                manualCarrierSignature = null;
                showVehicle();
                clearGenerated();
            }
        });
        showVehicle();

        root.addView(section("2 · STRECKE / LAYOUT"));
        layoutSpinner = catalogSpinner(catalog.layouts);
        root.addView(layoutSpinner, margins(0, 3, 0, 14));
        layoutSpinner.setOnItemSelectedListener(new SimpleSelection() {
            @Override public void selected() { clearGenerated(); }
        });

        root.addView(section("3 · PROFILE – 1 BIS 5 FREI AUSWÄHLEN"));
        SetupMode[] modes = SetupMode.values();
        for (int i = 0; i < modes.length; i++) {
            CheckBox box = new CheckBox(this);
            box.setText(modes[i].buttonLabel + " · " + modes[i].subtitle);
            box.setTextColor(TEXT);
            box.setTextSize(14);
            box.setPadding(dp(8), dp(7), dp(8), dp(7));
            box.setBackgroundColor(CARD);
            profileChecks[i] = box;
            root.addView(box, margins(0, 2, 0, 3));
        }
        profileChecks[0].setChecked(true);
        root.addView(text("Mustang GT3: FAST ATTACK erzwingt TC 1 auf jedem verifizierten Layout.",
                12, MUTED), margins(0, 3, 0, 13));

        root.addView(section("4 · FINE-TUNING – BIS ZU 4 PROBLEME"));
        root.addView(text("Nur das Fahrverhalten angeben. Technische Änderungen berechnet die App selbst.",
                12, MUTED), margins(0, 2, 0, 5));
        for (int i = 0; i < problemSpinners.length; i++) {
            root.addView(text("Problem " + (i + 1), 12, MUTED), margins(0, 3, 0, 1));
            problemSpinners[i] = stringSpinner(PROBLEMS);
            root.addView(problemSpinners[i], margins(0, 0, 0, 4));
        }

        Button create = yellowButton("AUSGEWÄHLTE PROFILE ERSTELLEN");
        create.setOnClickListener(v -> createSelected());
        root.addView(create, margins(0, 8, 0, 7));

        Button sync = yellowButton("SETUP-BESTAND AKTUALISIEREN");
        sync.setOnClickListener(v -> syncNow());
        root.addView(sync, margins(0, 0, 0, 7));

        Button own = yellowButton("OPTIONALE EIGENE .CARSETUP-STRUKTUR");
        own.setOnClickListener(v -> chooseCarrier());
        root.addView(own, margins(0, 0, 0, 14));

        root.addView(section("5 · ERZEUGTE SETUPS"));
        generatedSpinner = stringSpinner(new String[]{"Noch kein Setup erzeugt"});
        root.addView(generatedSpinner, margins(0, 3, 0, 6));
        exportButton = yellowButton("AUSGEWÄHLTE .CARSETUP SPEICHERN");
        exportButton.setEnabled(false);
        exportButton.setAlpha(.45f);
        exportButton.setOnClickListener(v -> beginExport());
        root.addView(exportButton, margins(0, 0, 0, 14));

        root.addView(section("LIVE- / SELBSTPRÜFBERICHT"));
        status = text("Bereit · " + (preloaded == null ? 0 : preloaded.count())
                + " Setups vorgeladen. LIVE-Fehler blockieren SELF-CALC nicht.", 13, TEXT);
        status.setPadding(dp(12), dp(12), dp(12), dp(12));
        status.setBackgroundColor(CARD);
        status.setTextIsSelectable(true);
        root.addView(status, margins(0, 4, 0, 12));

        scroll.addView(root);
        return scroll;
    }

    private void createSelected() {
        if (!uniqueProblems()) {
            setStatus("Jedes Fine-Tuning-Problem nur einmal auswählen.", true);
            return;
        }
        List<SetupMode> selectedModes = selectedModes();
        if (selectedModes.isEmpty()) {
            setStatus("Mindestens ein Profil auswählen.", true);
            return;
        }
        if (!setupRunning.compareAndSet(false, true)) {
            setStatus("Ein Setup-Lauf läuft bereits.", true);
            return;
        }
        CatalogItem vehicle = (CatalogItem) vehicleSpinner.getSelectedItem();
        CatalogItem layout = (CatalogItem) layoutSpinner.getSelectedItem();
        if (vehicle == null || layout == null) {
            setupRunning.set(false);
            setStatus("Fahrzeug oder Strecke fehlt.", true);
            return;
        }
        clearGenerated();
        String feedback = feedbackText();
        byte[] manualSnapshot = manualCarrier == null ? null : manualCarrier.clone();
        setStatus("LIVE-Suche startet. Danach werden genau " + selectedModes.size()
                + " ausgewählte Profile erzeugt.", false);

        setupExecutor.execute(() -> {
            LiveSearchSummary summary = null;
            String liveError = null;
            try {
                SetupRequest representative = new SetupRequest(vehicle, layout,
                        selectedModes.get(0), catalog.gameVersion);
                summary = liveSearch.search(representative);
            } catch (RuntimeException ex) {
                liveError = safe(ex);
            }

            List<GeneratedExport> outputs = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            for (SetupMode mode : selectedModes) {
                try {
                    SetupRequest request = new SetupRequest(vehicle, layout, mode, catalog.gameVersion);
                    RequestedPlans plans = plans(mode, feedback);
                    GeneratedExport out = generateOne(request, summary, plans, manualSnapshot);
                    outputs.add(out);
                } catch (RuntimeException ex) {
                    errors.add(mode.buttonLabel + ": " + safe(ex));
                }
            }
            LiveSearchSummary finalSummary = summary;
            String finalLiveError = liveError;
            runOnUiThread(() -> {
                generated.addAll(outputs);
                refreshGenerated();
                setupRunning.set(false);
                StringBuilder msg = new StringBuilder();
                msg.append("FERTIG: ").append(outputs.size()).append(" von ")
                        .append(selectedModes.size()).append(" ausgewählten Profilen erzeugt.");
                if (finalSummary != null) {
                    msg.append("\nLIVE-Runden: ").append(finalSummary.completedRounds).append("/2");
                    if (finalSummary.liveUnverified) msg.append(" · mindestens eine Quelle technisch fehlerhaft");
                } else if (finalLiveError != null) {
                    msg.append("\nLIVE technisch ausgefallen: ").append(finalLiveError)
                            .append(" · SELF-CALC wurde trotzdem versucht.");
                }
                if (!errors.isEmpty()) {
                    msg.append("\nNicht erzeugt:");
                    for (String e : errors) msg.append("\n• ").append(e);
                }
                setStatus(msg.toString(), outputs.isEmpty());
            });
        });
    }

    private GeneratedExport generateOne(SetupRequest request, LiveSearchSummary summary,
                                        RequestedPlans plans, byte[] manualSnapshot) {
        if (!ranges.hasVerifiedRanges(request.vehicle)) {
            throw new SetupValidationException("kein eigener verifizierter Fahrzeug-Wertebereich");
        }
        EngineeringProfile profile = ranges.loadRangeOnly(request.vehicle);

        if (summary != null && summary.status == LiveSearchSummary.Status.EXACT && summary.exact != null) {
            ExactFineTuneService.Result exact = new ExactFineTuneService().apply(
                    request, summary.exact, profile, plans.exact);
            return new GeneratedExport(fileName(request), exact.bytes,
                    "EXACT / " + summary.exact.candidate.provider);
        }

        if (!tracks.hasVerifiedProfile(request.layout)) {
            throw new SetupValidationException("kein exaktes Streckenprofil");
        }

        byte[] carrier = manualSnapshot;
        String carrierSource = manualSnapshot == null ? "" : "eigene verifizierte Same-Car-Datei";

        if (carrier == null) {
            VerifiedStructureCarrier p = preloaded == null ? null : preloaded.findCarrier(request.vehicle);
            if (p != null) {
                carrier = p.bytes;
                carrierSource = p.source;
            }
        }
        if (carrier == null) {
            VerifiedStructureCarrier b = bundled == null ? null : bundled.load(request.vehicle);
            if (b != null) {
                carrier = b.bytes;
                carrierSource = b.source;
            }
        }
        if (carrier == null && stockSync != null) {
            VerifiedStructureCarrier synced = stockSync.findCarrier(request.vehicle);
            if (synced != null) {
                carrier = synced.bytes;
                carrierSource = synced.source;
            }
        }
        if (carrier == null && summary != null && summary.structureCarrier != null) {
            carrier = summary.structureCarrier.bytes;
            carrierSource = summary.structureCarrier.source;
        }
        if (carrier == null) {
            throw new SetupValidationException("keine verifizierte Same-Car-.carsetup-Struktur verfügbar");
        }

        TrackProfile track = tracks.load(request.layout);
        boolean liveUnverified = summary == null || summary.liveUnverified;
        EngineeringSetup engineering = new SelfCalcEngine().calculate(request, profile, track, liveUnverified);
        if (!plans.engineering.adjustments.isEmpty()) {
            engineering = new FineTuneEngine().apply(engineering, profile, plans.engineering);
        }
        SelfCalcExportService.Result result = new SelfCalcExportService().apply(
                request, carrier, profile, engineering);
        return new GeneratedExport(fileName(request), result.bytes,
                "SELF-CALC / " + carrierSource);
    }

    private RequestedPlans plans(SetupMode mode, String feedback) {
        FineTunePlan modePlan = new ModeFineTunePlanner().plan(
                mode, FineTunePlan.Origin.EXACT_DERIVATIVE);
        if (feedback.isEmpty()) {
            return new RequestedPlans(modePlan,
                    new FineTunePlan(FineTunePlan.Origin.ENGINEERING_MODEL,
                            Collections.emptyList(), Collections.emptyList()));
        }
        FineTuneInterpretation interpretation = new FineTuneInterpreter().interpret(feedback);
        if (!interpretation.understood) {
            throw new SetupValidationException("Fine-Tuning-Auswahl konnte nicht eindeutig interpretiert werden");
        }
        FineTunePlan exactFeedback = new FineTunePlanner().plan(
                interpretation, FineTunePlan.Origin.EXACT_DERIVATIVE);
        FineTunePlan engineeringFeedback = new FineTunePlanner().plan(
                interpretation, FineTunePlan.Origin.ENGINEERING_MODEL);
        return new RequestedPlans(new FineTunePlanCombiner().combine(modePlan, exactFeedback),
                engineeringFeedback);
    }

    private String feedbackText() {
        StringBuilder out = new StringBuilder();
        for (Spinner spinner : problemSpinners) {
            if (spinner == null || spinner.getSelectedItemPosition() <= 0) continue;
            String value = String.valueOf(spinner.getSelectedItem());
            if (value.startsWith("Auto zu träge")) value = "lenkung zu langsam";
            else if (value.startsWith("Auto reagiert zu aggressiv")) value = "lenkung zu nervös";
            else if (value.startsWith("Heck nervös")) value = "heck nervös";
            else if (value.startsWith("Auto unruhig über Curbs")) value = "curbs bodenwellen";
            else if (value.startsWith("Mehr Heckstabilität")) value = "mehr heckflügel";
            else if (value.startsWith("Mehr Topspeed")) value = "weniger heckflügel";
            if (out.length() > 0) out.append("; ");
            out.append(value);
        }
        return out.toString();
    }

    private boolean uniqueProblems() {
        boolean[] used = new boolean[PROBLEMS.length];
        for (Spinner spinner : problemSpinners) {
            if (spinner == null) continue;
            int p = spinner.getSelectedItemPosition();
            if (p <= 0) continue;
            if (used[p]) return false;
            used[p] = true;
        }
        return true;
    }

    private List<SetupMode> selectedModes() {
        List<SetupMode> result = new ArrayList<>();
        SetupMode[] modes = SetupMode.values();
        for (int i = 0; i < modes.length; i++) {
            if (profileChecks[i] != null && profileChecks[i].isChecked()) result.add(modes[i]);
        }
        return result;
    }

    private void syncNow() {
        setStatus("Setup-Bestand wird aktualisiert …", false);
        syncExecutor.execute(() -> {
            SetupStockSync.Result result = stockSync.syncOnce();
            runOnUiThread(() -> setStatus("BESTAND: " + result.added() + " neu · "
                    + result.failed() + " fehlgeschlagen · " + result.dead()
                    + " nach 3 Synchronisationsläufen entfernt · " + result.localCount()
                    + " synchronisierte + " + preloaded.count() + " vorgeladene Setups.", false));
        });
    }

    private void chooseCarrier() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        startActivityForResult(intent, CARRIER_IMPORT_REQUEST);
    }

    private void importCarrier(Uri uri) {
        CatalogItem vehicle = (CatalogItem) vehicleSpinner.getSelectedItem();
        if (vehicle == null) return;
        try (InputStream input = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) throw new IllegalStateException("Datei nicht lesbar");
            byte[] buffer = new byte[8192];
            int total = 0;
            int n;
            while ((n = input.read(buffer)) != -1) {
                total += n;
                if (total > 65536) throw new IllegalStateException("Datei größer als 64 KiB");
                output.write(buffer, 0, n);
            }
            byte[] bytes = output.toByteArray();
            CarSetupInspection inspection = CarSetupInspector.inspect(bytes);
            if (!inspection.structurallyValid) throw new IllegalStateException(inspection.failureReason);
            if (!inspection.vehicleSignature.startsWith(vehicle.expectedSignaturePrefix)) {
                throw new IllegalStateException("Datei gehört nicht zum ausgewählten Fahrzeug");
            }
            manualCarrier = bytes;
            manualCarrierSignature = inspection.vehicleSignature;
            setStatus("Eigene Same-Car-Struktur verifiziert: " + manualCarrierSignature
                    + "\nSHA-256: " + Hashing.sha256(bytes), false);
        } catch (Exception ex) {
            manualCarrier = null;
            manualCarrierSignature = null;
            setStatus("Eigene Struktur abgelehnt: " + safe(ex), true);
        }
    }

    private void beginExport() {
        if (generated.isEmpty()) {
            setStatus("Noch kein Setup erzeugt.", true);
            return;
        }
        int position = generatedSpinner.getSelectedItemPosition();
        if (position < 0 || position >= generated.size()) position = 0;
        GeneratedExport chosen = generated.get(position);
        pendingExportBytes = chosen.bytes.clone();
        pendingExportName = chosen.name;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, chosen.name);
        startActivityForResult(intent, EXPORT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CARRIER_IMPORT_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                importCarrier(data.getData());
            }
            return;
        }
        if (requestCode != EXPORT_REQUEST || resultCode != RESULT_OK || data == null
                || data.getData() == null || pendingExportBytes == null) return;
        try (OutputStream output = getContentResolver().openOutputStream(data.getData(), "w")) {
            if (output == null) throw new IllegalStateException("Zieldatei nicht geöffnet");
            output.write(pendingExportBytes);
            output.flush();
            setStatus("GESPEICHERT: " + pendingExportName + "\nSHA-256: "
                    + Hashing.sha256(pendingExportBytes), false);
        } catch (Exception ex) {
            setStatus("Speichern fehlgeschlagen: " + safe(ex), true);
        }
    }

    private void clearGenerated() {
        generated.clear();
        if (generatedSpinner != null) {
            generatedSpinner.setAdapter(new ArrayAdapter<String>(this,
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{"Noch kein Setup erzeugt"}));
        }
        if (exportButton != null) {
            exportButton.setEnabled(false);
            exportButton.setAlpha(.45f);
        }
    }

    private void refreshGenerated() {
        List<String> labels = new ArrayList<>();
        for (GeneratedExport item : generated) labels.add(item.name + " · " + item.source);
        if (labels.isEmpty()) labels.add("Noch kein Setup erzeugt");
        generatedSpinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, labels));
        exportButton.setEnabled(!generated.isEmpty());
        exportButton.setAlpha(generated.isEmpty() ? .45f : 1f);
    }

    private void showVehicle() {
        if (vehicleSpinner == null || thumbnailLoader == null) return;
        CatalogItem vehicle = (CatalogItem) vehicleSpinner.getSelectedItem();
        if (vehicle != null) thumbnailLoader.load(vehicle, vehicleImage, vehicleName);
    }

    private Spinner catalogSpinner(List<CatalogItem> values) {
        Spinner spinner = new Spinner(this);
        spinner.setBackgroundColor(CARD);
        spinner.setAdapter(new ArrayAdapter<CatalogItem>(this,
                android.R.layout.simple_spinner_dropdown_item, values));
        return spinner;
    }

    private Spinner stringSpinner(String[] values) {
        Spinner spinner = new Spinner(this);
        spinner.setBackgroundColor(CARD);
        spinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, values));
        return spinner;
    }

    private Button yellowButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.BLACK);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextSize(14);
        button.setMinHeight(dp(52));
        button.setBackgroundColor(YELLOW);
        return button;
    }

    private TextView section(String value) {
        TextView title = text(value, 14, YELLOW);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return title;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout column() {
        LinearLayout value = new LinearLayout(this);
        value.setOrientation(LinearLayout.VERTICAL);
        value.setBackgroundColor(BG);
        return value;
    }

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return p;
    }

    private LinearLayout.LayoutParams fixed(int height, int top, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height);
        p.setMargins(0, dp(top), 0, dp(bottom));
        return p;
    }

    private void setStatus(String message, boolean error) {
        if (status == null) return;
        status.setText(message);
        status.setTextColor(error ? Color.rgb(255, 150, 150) : TEXT);
    }

    private static String fileName(SetupRequest request) {
        return "Godfather_" + request.vehicle.id + "_" + request.layout.id + "_"
                + request.mode.name().toLowerCase(java.util.Locale.ROOT) + ".carsetup";
    }

    private static String safe(Throwable ex) {
        String value = ex.getMessage();
        return value == null || value.trim().isEmpty() ? ex.getClass().getSimpleName() : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        if (thumbnailLoader != null) thumbnailLoader.close();
        setupExecutor.shutdownNow();
        syncExecutor.shutdownNow();
        super.onDestroy();
    }

    private abstract class SimpleSelection implements android.widget.AdapterView.OnItemSelectedListener {
        abstract void selected();
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                             int position, long id) { selected(); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
    }

    private record RequestedPlans(FineTunePlan exact, FineTunePlan engineering) { }
    private record GeneratedExport(String name, byte[] bytes, String source) { }
}
