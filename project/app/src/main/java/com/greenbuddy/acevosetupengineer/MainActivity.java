package com.greenbuddy.acevosetupengineer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.greenbuddy.acevosetupengineer.catalog.CatalogData;
import com.greenbuddy.acevosetupengineer.catalog.CatalogRepository;
import com.greenbuddy.acevosetupengineer.core.AndroidExactCache;
import com.greenbuddy.acevosetupengineer.core.ExactFineTuneService;
import com.greenbuddy.acevosetupengineer.core.LiveSearchCoordinator;
import com.greenbuddy.acevosetupengineer.core.LiveSearchSummary;
import com.greenbuddy.acevosetupengineer.core.RacePlacePackageProvider;
import com.greenbuddy.acevosetupengineer.core.SetupsMarketProvider;
import com.greenbuddy.acevosetupengineer.core.SelfCalcExportService;
import com.greenbuddy.acevosetupengineer.engineering.EngineeringProfile;
import com.greenbuddy.acevosetupengineer.engineering.EngineeringSetup;
import com.greenbuddy.acevosetupengineer.engineering.FineTuneInterpretation;
import com.greenbuddy.acevosetupengineer.engineering.FineTuneInterpreter;
import com.greenbuddy.acevosetupengineer.engineering.FineTunePlan;
import com.greenbuddy.acevosetupengineer.engineering.FineTunePlanCombiner;
import com.greenbuddy.acevosetupengineer.engineering.FineTunePlanner;
import com.greenbuddy.acevosetupengineer.engineering.FineTuneEngine;
import com.greenbuddy.acevosetupengineer.engineering.ModeFineTunePlanner;
import com.greenbuddy.acevosetupengineer.engineering.ParameterAdjustment;
import com.greenbuddy.acevosetupengineer.engineering.RangeProfileRepository;
import com.greenbuddy.acevosetupengineer.engineering.SetupValidationException;
import com.greenbuddy.acevosetupengineer.engineering.SetupSection;
import com.greenbuddy.acevosetupengineer.engineering.SelfCalcEngine;
import com.greenbuddy.acevosetupengineer.engineering.TrackProfile;
import com.greenbuddy.acevosetupengineer.engineering.TrackProfileRepository;
import com.greenbuddy.acevosetupengineer.binary.CarSetupInspection;
import com.greenbuddy.acevosetupengineer.binary.CarSetupInspector;
import com.greenbuddy.acevosetupengineer.model.CatalogItem;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.model.SetupMode;
import com.greenbuddy.acevosetupengineer.ui.VehicleThumbnailLoader;
import com.greenbuddy.acevosetupengineer.util.Hashing;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MainActivity extends Activity {
    private static final int EXPORT_REQUEST = 4107;
    private static final int CARRIER_IMPORT_REQUEST = 4108;
    private static final int MAX_CARRIER_BYTES = 65_536;
    private static final int BG = Color.rgb(9, 11, 15);
    private static final int CARD = Color.rgb(22, 26, 34);
    private static final int TEXT = Color.rgb(245, 247, 250);
    private static final int MUTED = Color.rgb(164, 171, 184);
    private static final int ACCENT = Color.rgb(215, 25, 32);

    private Spinner vehicleSpinner;
    private Spinner layoutSpinner;
    private ImageView vehicleThumbnail;
    private TextView selectedVehicleName;
    private EditText feedbackInput;
    private TextView status;
    private Button exportButton;
    private TextView carrierStatus;
    private CatalogData catalog;
    private VehicleThumbnailLoader thumbnailLoader;
    private RangeProfileRepository rangeProfiles;
    private TrackProfileRepository trackProfiles;
    private LiveSearchCoordinator liveSearch;
    private final ExecutorService setupExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean setupRunning = new AtomicBoolean();
    private byte[] pendingExportBytes;
    private String pendingExportName;
    private byte[] structureCarrierBytes;
    private String structureCarrierSignature;
    private String structureCarrierSha256;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            // No vehicle is released until its complete parameter and structure profile passes verification.
            catalog = CatalogRepository.load(this, Collections.emptySet());
            thumbnailLoader = new VehicleThumbnailLoader(this);
            rangeProfiles = new RangeProfileRepository(this);
            trackProfiles = new TrackProfileRepository(this);
            liveSearch = new LiveSearchCoordinator(
                    Arrays.asList(new RacePlacePackageProvider(), new SetupsMarketProvider()),
                    new AndroidExactCache(this),
                    message -> runOnUiThread(() -> setStatus(
                            "LIVE-SUCHE – drei vollständige Runden\n\n" + message, false)));
            setContentView(buildScreen());
        } catch (Exception ex) {
            TextView failure = text("Katalog konnte nicht verifiziert geladen werden: " + ex.getMessage(), 16, TEXT);
            failure.setPadding(dp(24), dp(24), dp(24), dp(24));
            failure.setBackgroundColor(BG);
            setContentView(failure);
        }
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        LinearLayout root = column();
        root.setPadding(dp(18), dp(22), dp(18), dp(32));

        TextView title = text("THE GODFATHER OF EVO", 25, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);
        root.addView(text("Assetto Corsa EVO Setup Engineer", 15, MUTED), matchWrap(0, dp(2)));
        TextView version = text("VERIFIZIERTER INHALTSSTAND " + catalog.gameVersion, 12, ACCENT);
        version.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(version, matchWrap(0, dp(18)));

        root.addView(sectionTitle("Fahrzeug"));
        vehicleThumbnail = new ImageView(this);
        vehicleThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        vehicleThumbnail.setAdjustViewBounds(true);
        vehicleThumbnail.setMinimumHeight(dp(169));
        vehicleThumbnail.setBackgroundColor(CARD);
        root.addView(vehicleThumbnail, fixedHeight(dp(169), 0, dp(8)));

        selectedVehicleName = text("", 18, TEXT);
        selectedVehicleName.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        selectedVehicleName.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(selectedVehicleName, matchWrap(0, dp(3)));
        TextView imageProvenance = text("Offizielles Assetto-Corsa-EVO-Fahrzeugbild", 11, MUTED);
        imageProvenance.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(imageProvenance, matchWrap(0, dp(10)));

        vehicleSpinner = spinner(catalog.vehicles);
        root.addView(vehicleSpinner, matchWrap(0, dp(14)));
        vehicleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                clearStructureCarrier();
                showSelectedVehicle();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                vehicleThumbnail.setImageResource(R.drawable.ic_car_placeholder);
                vehicleThumbnail.setContentDescription("Kein Fahrzeug ausgewählt");
                selectedVehicleName.setText("Kein Fahrzeug ausgewählt");
            }
        });
        showSelectedVehicle();

        root.addView(sectionTitle("Exakte Strecke / Layout"));
        layoutSpinner = spinner(catalog.layouts);
        root.addView(layoutSpinner, matchWrap(0, dp(18)));

        Button carrierButton = button("STRUKTURTRÄGER FÜR SELF-CALC WÄHLEN", CARD);
        carrierButton.setOnClickListener(ignored -> beginCarrierImport());
        root.addView(carrierButton, matchWrap(0, dp(5)));
        carrierStatus = text("Optional: eine im Spiel gespeicherte .carsetup desselben Autos. "
                + "Nur die Binärstruktur wird benutzt; sämtliche verstellbaren Zahlen werden verworfen.", 12, MUTED);
        root.addView(carrierStatus, matchWrap(0, dp(18)));

        root.addView(sectionTitle("Setup-Stil"));
        for (SetupMode mode : SetupMode.values()) root.addView(modeButton(mode), matchWrap(0, dp(9)));

        root.addView(sectionTitle("Fine-Tuning – Fahrverhalten beschreiben"), matchWrap(dp(12), dp(5)));
        feedbackInput = new EditText(this);
        feedbackInput.setHint("z. B. Heck wird nervös beim Anbremsen");
        feedbackInput.setHintTextColor(MUTED);
        feedbackInput.setTextColor(TEXT);
        feedbackInput.setTextSize(15);
        feedbackInput.setMinLines(3);
        feedbackInput.setGravity(Gravity.TOP);
        feedbackInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        feedbackInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        feedbackInput.setBackgroundColor(CARD);
        root.addView(feedbackInput, matchWrap(0, dp(12)));

        TextView heading = text("8 neutrale Prüfrubriken", 13, MUTED);
        root.addView(heading);
        StringBuilder sections = new StringBuilder();
        for (SetupSection section : SetupSection.values()) {
            if (sections.length() > 0) sections.append("  •  ");
            sections.append(section.displayName);
        }
        root.addView(text(sections.toString(), 13, MUTED), matchWrap(0, dp(12)));

        Button analyze = button("FAHRVERHALTEN ANALYSIEREN", CARD);
        analyze.setOnClickListener(ignored -> showFineTunePlan());
        root.addView(analyze, matchWrap(0, dp(14)));

        status = text("Bereit. Ein Export bleibt gesperrt, solange das gewählte Fahrzeugprofil nicht vollständig verifiziert ist.", 14, TEXT);
        status.setPadding(dp(14), dp(14), dp(14), dp(14));
        status.setBackgroundColor(CARD);
        root.addView(status, matchWrap(0, dp(12)));

        exportButton = button(".CARSETUP EXPORTIEREN", ACCENT);
        exportButton.setEnabled(false);
        exportButton.setAlpha(0.45f);
        exportButton.setOnClickListener(ignored -> beginExport());
        root.addView(exportButton, matchWrap(0, dp(24)));

        TextView copyright = text("© Greenbuddy1976", 12, MUTED);
        copyright.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(copyright);
        scroll.addView(root);
        return scroll;
    }

    private Button modeButton(SetupMode mode) {
        Button value = button(mode.buttonLabel + "\n" + mode.subtitle, ACCENT);
        value.setOnClickListener(ignored -> requestSetup(mode));
        return value;
    }

    private void requestSetup(SetupMode mode) {
        CatalogItem vehicle = (CatalogItem) vehicleSpinner.getSelectedItem();
        CatalogItem layout = (CatalogItem) layoutSpinner.getSelectedItem();
        if (vehicle == null || layout == null) {
            setStatus("NICHT SICHER: Fahrzeug oder Layout fehlt.", true);
            return;
        }
        if (!vehicle.selectable) {
            setStatus("NICHT SICHER: Für " + vehicle.name
                    + " fehlt noch die vollständig verifizierte Parameter-/Strukturabdeckung. "
                    + "Es wird kein Setup erfunden.", true);
            return;
        }
        if (!layout.selectable) {
            setStatus("NICHT SICHER: Die exakte Layoutidentität ist noch nicht verifiziert. "
                    + "Es wird kein ähnliches Layout verwendet.", true);
            return;
        }
        if (!setupRunning.compareAndSet(false, true)) {
            setStatus("Die drei LIVE-Runden laufen bereits. Bitte diesen Durchlauf abschließen lassen.", true);
            return;
        }

        RequestedPlans plans;
        try {
            plans = buildRequestedPlans(mode);
        } catch (SetupValidationException invalid) {
            setupRunning.set(false);
            setStatus("NICHT SICHER: " + invalid.getMessage(), true);
            return;
        }

        clearPendingExport();
        byte[] carrierSnapshot = structureCarrierBytes == null ? null : structureCarrierBytes.clone();
        SetupRequest request = new SetupRequest(vehicle, layout, mode, catalog.gameVersion);
        setStatus("LIVE-SUCHE STARTET\n\n" + vehicle.name + "\n" + layout.name
                + "\n" + mode.buttonLabel + "\n\nRunde 1/3 wird vorbereitet.", false);
        setupExecutor.execute(() -> {
            try {
                LiveSearchSummary summary = liveSearch.search(request);
                if (summary.status == LiveSearchSummary.Status.EXACT) {
                    finishExact(request, summary, plans.exact);
                } else {
                    finishWithoutExact(request, summary, plans.engineeringFeedback, carrierSnapshot);
                }
            } catch (RuntimeException failure) {
                runOnUiThread(() -> setStatus("NICHT SICHER: Der geprüfte Setup-Lauf wurde abgebrochen.\n\n"
                        + safeMessage(failure), true));
            } finally {
                setupRunning.set(false);
            }
        });
    }

    private RequestedPlans buildRequestedPlans(SetupMode mode) {
        FineTunePlan modePlan = new ModeFineTunePlanner().plan(
                mode, FineTunePlan.Origin.EXACT_DERIVATIVE);
        String feedback = feedbackInput.getText().toString().trim();
        if (feedback.isEmpty()) {
            return new RequestedPlans(modePlan, new FineTunePlan(
                    FineTunePlan.Origin.ENGINEERING_MODEL, Collections.emptyList(), Collections.emptyList()));
        }
        FineTuneInterpretation interpretation = new FineTuneInterpreter().interpret(feedback);
        if (!interpretation.understood) {
            throw new SetupValidationException(
                    "Das zusätzliche Fahrverhalten ist nicht eindeutig. Bitte Kurvenphase und Verhalten nennen.");
        }
        FineTunePlan feedbackPlan = new FineTunePlanner().plan(
                interpretation, FineTunePlan.Origin.EXACT_DERIVATIVE);
        FineTunePlan engineeringFeedback = new FineTunePlanner().plan(
                interpretation, FineTunePlan.Origin.ENGINEERING_MODEL);
        return new RequestedPlans(new FineTunePlanCombiner().combine(modePlan, feedbackPlan),
                engineeringFeedback);
    }

    private void finishExact(SetupRequest request, LiveSearchSummary summary, FineTunePlan plan) {
        try {
            if (!rangeProfiles.hasVerifiedRanges(request.vehicle)) {
                throw new SetupValidationException("EXACT wurde gefunden, aber für diese Fahrzeugvariante fehlt "
                        + "der eigene verifizierte EVO-Wertebereich. Der gewählte Modus wird nicht vorgetäuscht.");
            }
            EngineeringProfile profile = rangeProfiles.loadRangeOnly(request.vehicle);
            ExactFineTuneService.Result result = new ExactFineTuneService().apply(
                    request, summary.exact, profile, plan);
            String fileName = exportFileName(request);
            String tunedHash = Hashing.sha256(result.bytes);
            StringBuilder message = new StringBuilder();
            message.append("EXACT DERIVATIVE – EXPORT FREIGEGEBEN\n\n")
                    .append("LIVE-Runden: 3/3 vollständig\n")
                    .append("Quelle: ").append(summary.exact.candidate.provider).append('\n')
                    .append("Fahrzeug/Version/Layout: exakt verifiziert\n")
                    .append("Binärsignatur: ").append(summary.exact.decodedVehicleSignature).append('\n')
                    .append("Ausgabe SHA-256: ").append(tunedHash).append('\n');
            if (summary.exact.fromCache) {
                message.append("Status: LIVE-UNVERIFIED; nur exakt passender, zuvor verifizierter Cache\n");
            }
            message.append("\nAngewandte Änderungen:");
            for (String line : result.setup.audit) {
                if (line.contains(" -> ") || line.startsWith("Übersprungen:")) {
                    message.append("\n• ").append(line);
                }
            }
            runOnUiThread(() -> {
                pendingExportBytes = result.bytes.clone();
                pendingExportName = fileName;
                exportButton.setEnabled(true);
                exportButton.setAlpha(1.0f);
                setStatus(message.toString(), false);
            });
        } catch (RuntimeException unsafe) {
            runOnUiThread(() -> {
                clearPendingExport();
                setStatus("NICHT SICHER: " + safeMessage(unsafe), true);
            });
        }
    }

    private void finishWithoutExact(
            SetupRequest request,
            LiveSearchSummary summary,
            FineTunePlan engineeringFeedback,
            byte[] carrier) {
        String liveState = summary.liveUnverified
                ? "LIVE-UNVERIFIED: mindestens eine Quelle war technisch nicht vollständig prüfbar."
                : "Alle LIVE-Quellen waren erreichbar; kein exaktes 0.8.1-Setup gefunden.";
        try {
            if (!rangeProfiles.hasVerifiedRanges(request.vehicle)) {
                throw new SetupValidationException("Für diese Fahrzeugvariante fehlt der eigene, "
                        + "versionsgebundene EVO-Wertebereich. Kein Donor-Fahrzeug wird verwendet.");
            }
            if (!trackProfiles.hasVerifiedProfile(request.layout)) {
                throw new SetupValidationException("Für das exakte Layout fehlt das Geometrieprofil. "
                        + "Kein ähnlicher Kurs wird verwendet.");
            }
            if (carrier == null) {
                throw new SetupValidationException("Kein Strukturträger gewählt. Bitte eine im Spiel "
                        + "gespeicherte .carsetup desselben Autos auswählen; ihre Zahlen werden nicht übernommen.");
            }
            EngineeringProfile profile = rangeProfiles.loadRangeOnly(request.vehicle);
            TrackProfile track = trackProfiles.load(request.layout);
            EngineeringSetup generated = new SelfCalcEngine().calculate(
                    request, profile, track, summary.liveUnverified);
            if (!engineeringFeedback.adjustments.isEmpty()) {
                generated = new FineTuneEngine().apply(generated, profile, engineeringFeedback);
            }
            SelfCalcExportService.Result result = new SelfCalcExportService().apply(
                    request, carrier, profile, generated);
            String outputHash = Hashing.sha256(result.bytes);
            StringBuilder message = new StringBuilder("ENGINEERING MODEL – EXPORT FREIGEGEBEN\n\n")
                    .append("LIVE-Runden: 3/3 vollständig\n")
                    .append(liveState).append('\n')
                    .append("Fahrzeugbereich: exakt/versioniert\n")
                    .append("Layoutprofil: exakt/versioniert\n")
                    .append("Strukturträger: gleiche Fahrzeug-Signatur; Zahlen verworfen\n")
                    .append("Neu berechnete Parameter: ").append(result.setup.values.size()).append('\n')
                    .append("Strukturträger SHA-256: ").append(result.carrierSha256).append('\n')
                    .append("Ausgabe SHA-256: ").append(outputHash).append('\n')
                    .append("Kennzeichnung: ENGINEERING MODEL (nicht offiziell, kein kopiertes Setup)");
            for (String line : result.setup.audit) {
                if (line.contains(" -> ")) message.append("\n• ").append(line);
            }
            runOnUiThread(() -> {
                pendingExportBytes = result.bytes.clone();
                pendingExportName = exportFileName(request);
                exportButton.setEnabled(true);
                exportButton.setAlpha(1.0f);
                setStatus(message.toString(), false);
            });
        } catch (RuntimeException unsafe) {
            runOnUiThread(() -> {
                clearPendingExport();
                setStatus("ENGINEERING MODEL NICHT FREIGEGEBEN\n\nLIVE-Runden: 3/3 vollständig\n"
                        + liveState + "\n\nNICHT SICHER: " + safeMessage(unsafe), true);
            });
        }
    }

    private void beginCarrierImport() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        startActivityForResult(intent, CARRIER_IMPORT_REQUEST);
    }

    private void beginExport() {
        if (pendingExportBytes == null || pendingExportName == null) {
            setStatus("Kein vollständig verifiziertes Setup zum Export bereit.", true);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, pendingExportName);
        startActivityForResult(intent, EXPORT_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CARRIER_IMPORT_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                importStructureCarrier(data.getData());
            }
            return;
        }
        if (requestCode != EXPORT_REQUEST || resultCode != RESULT_OK || data == null) return;
        Uri target = data.getData();
        if (target == null || pendingExportBytes == null) {
            setStatus("Exportziel oder verifizierte Datei fehlt.", true);
            return;
        }
        try (OutputStream output = getContentResolver().openOutputStream(target, "w")) {
            if (output == null) throw new IllegalStateException("Exportziel konnte nicht geöffnet werden");
            output.write(pendingExportBytes);
            output.flush();
            setStatus("EXPORT ABGESCHLOSSEN\n\n" + pendingExportName
                    + "\nSHA-256: " + Hashing.sha256(pendingExportBytes), false);
        } catch (Exception failure) {
            setStatus("Export fehlgeschlagen: " + safeMessage(failure), true);
        }
    }

    private void importStructureCarrier(Uri source) {
        CatalogItem vehicle = (CatalogItem) vehicleSpinner.getSelectedItem();
        if (vehicle == null) {
            setStatus("Vor dem Strukturträger muss ein Fahrzeug gewählt sein.", true);
            return;
        }
        try (InputStream input = getContentResolver().openInputStream(source);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) throw new IllegalStateException("Strukturträger konnte nicht geöffnet werden");
            byte[] buffer = new byte[8192];
            int count;
            int total = 0;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > MAX_CARRIER_BYTES) throw new IllegalStateException("Strukturträger ist größer als 64 KiB");
                output.write(buffer, 0, count);
            }
            byte[] bytes = output.toByteArray();
            CarSetupInspection inspection = CarSetupInspector.inspect(bytes);
            if (!inspection.structurallyValid) {
                throw new IllegalStateException(inspection.failureReason);
            }
            if (!inspection.vehicleSignature.startsWith(vehicle.expectedSignaturePrefix)) {
                throw new IllegalStateException("Datei gehört nicht zu " + vehicle.name);
            }
            structureCarrierBytes = bytes;
            structureCarrierSignature = inspection.vehicleSignature;
            structureCarrierSha256 = Hashing.sha256(bytes);
            carrierStatus.setText("Verifiziert: " + vehicle.name + "\nSignatur: "
                    + structureCarrierSignature + "\nSHA-256: " + structureCarrierSha256
                    + "\nNur Struktur – gespeicherte verstellbare Zahlen werden verworfen.");
            carrierStatus.setTextColor(TEXT);
            clearPendingExport();
        } catch (Exception failure) {
            clearStructureCarrier();
            setStatus("NICHT SICHER: Strukturträger abgelehnt – " + safeMessage(failure), true);
        }
    }

    private void clearStructureCarrier() {
        structureCarrierBytes = null;
        structureCarrierSignature = null;
        structureCarrierSha256 = null;
        if (carrierStatus != null) {
            carrierStatus.setText("Optional: eine im Spiel gespeicherte .carsetup desselben Autos. "
                    + "Nur die Binärstruktur wird benutzt; sämtliche verstellbaren Zahlen werden verworfen.");
            carrierStatus.setTextColor(MUTED);
        }
        clearPendingExport();
    }

    private void clearPendingExport() {
        pendingExportBytes = null;
        pendingExportName = null;
        if (exportButton != null) {
            exportButton.setEnabled(false);
            exportButton.setAlpha(0.45f);
        }
    }

    private static String exportFileName(SetupRequest request) {
        return "EvoForge_" + request.vehicle.id + "_" + request.layout.id + "_"
                + request.mode.name().toLowerCase(java.util.Locale.ROOT) + ".carsetup";
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return message == null || message.trim().isEmpty() ? failure.getClass().getSimpleName() : message;
    }

    private void showFineTunePlan() {
        FineTuneInterpretation interpretation = new FineTuneInterpreter().interpret(feedbackInput.getText().toString());
        if (!interpretation.understood) {
            setStatus("NICHT SICHER: Das Fahrverhalten ist nicht eindeutig genug beschrieben. "
                    + "Bitte Kurvenphase und Verhalten nennen.", true);
            return;
        }
        FineTunePlan plan = new FineTunePlanner().plan(interpretation, FineTunePlan.Origin.ENGINEERING_MODEL);
        StringBuilder message = new StringBuilder("SCHLUSSFOLGERUNG – technischer Änderungsplan:\n");
        for (ParameterAdjustment adjustment : plan.adjustments) {
            message.append("\n• ").append(adjustment.parameter.section.displayName)
                    .append(" / ").append(adjustment.parameter.displayName)
                    .append(adjustment.normalizedDelta > 0 ? " erhöhen" : " verringern")
                    .append(" – ").append(adjustment.reason);
        }
        message.append("\n\nNoch keine Zahlen: Werte werden erst mit dem verifizierten Fahrzeugprofil angewendet.");
        setStatus(message.toString(), false);
    }

    private Spinner spinner(java.util.List<CatalogItem> values) {
        Spinner spinner = new Spinner(this);
        spinner.setBackgroundColor(CARD);
        ArrayAdapter<CatalogItem> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, values);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private void showSelectedVehicle() {
        if (vehicleSpinner == null || vehicleThumbnail == null || selectedVehicleName == null) return;
        CatalogItem vehicle = (CatalogItem) vehicleSpinner.getSelectedItem();
        if (vehicle == null) return;
        thumbnailLoader.load(vehicle, vehicleThumbnail, selectedVehicleName);
    }

    private TextView sectionTitle(String value) {
        TextView title = text(value, 14, TEXT);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return title;
    }

    private Button button(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(TEXT);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setMinHeight(dp(58));
        button.setBackgroundColor(color);
        return button;
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.08f);
        return view;
    }

    private LinearLayout column() {
        LinearLayout value = new LinearLayout(this);
        value.setOrientation(LinearLayout.VERTICAL);
        return value;
    }

    private LinearLayout.LayoutParams matchWrap(int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, top, 0, bottom);
        return params;
    }

    private LinearLayout.LayoutParams fixedHeight(int height, int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, height);
        params.setMargins(0, top, 0, bottom);
        return params;
    }

    @Override
    protected void onDestroy() {
        if (thumbnailLoader != null) thumbnailLoader.close();
        setupExecutor.shutdownNow();
        super.onDestroy();
    }

    private void setStatus(String message, boolean error) {
        status.setText(message);
        status.setTextColor(error ? Color.rgb(255, 166, 166) : TEXT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private record RequestedPlans(FineTunePlan exact, FineTunePlan engineeringFeedback) {}
}
