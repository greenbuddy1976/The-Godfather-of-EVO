package com.greenbuddy.acevosetupengineer;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.greenbuddy.acevosetupengineer.beta.MustangBaseRepository;
import com.greenbuddy.acevosetupengineer.beta.MustangBetaEngine;
import com.greenbuddy.acevosetupengineer.beta.MustangImportInspection;
import com.greenbuddy.acevosetupengineer.beta.MustangSetupInspector;
import com.greenbuddy.acevosetupengineer.data.OfficialInventory;
import com.greenbuddy.acevosetupengineer.engine.OutputFileName;
import com.greenbuddy.acevosetupengineer.engine.ProviderLoader;
import com.greenbuddy.acevosetupengineer.engine.SetupGenerationService;
import com.greenbuddy.acevosetupengineer.engine.VerifiedWriterProvider;
import com.greenbuddy.acevosetupengineer.live.LiveIndexController;
import com.greenbuddy.acevosetupengineer.live.LiveSetupSource;
import com.greenbuddy.acevosetupengineer.model.CarIdentity;
import com.greenbuddy.acevosetupengineer.model.FineTuningProblem;
import com.greenbuddy.acevosetupengineer.model.FineTuningStrength;
import com.greenbuddy.acevosetupengineer.model.GeneratedSetup;
import com.greenbuddy.acevosetupengineer.model.GenerationOutcome;
import com.greenbuddy.acevosetupengineer.model.ParameterChange;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.model.SetupSection;
import com.greenbuddy.acevosetupengineer.model.SetupStyle;
import com.greenbuddy.acevosetupengineer.model.SetupValue;
import com.greenbuddy.acevosetupengineer.model.TrackLayout;
import com.greenbuddy.acevosetupengineer.model.VerificationReport;
import com.greenbuddy.acevosetupengineer.ui.DarkSpinnerAdapter;
import com.greenbuddy.acevosetupengineer.verification.BinaryDigest;
import com.greenbuddy.acevosetupengineer.verification.VerifiedBinaryInspector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int CREATE_CARSETUP = 7111;
    private static final int OPEN_MUSTANG_BASE = 7112;
    private static final int MAX_IMPORT_BYTES = 1024 * 1024;

    private Spinner vehicleSpinner;
    private Spinner layoutSpinner;
    private Spinner problemSpinner;
    private Spinner strengthSpinner;
    private Spinner resultSpinner;
    private RadioGroup styleGroup;
    private LinearLayout fineTuningPanel;
    private Button fineTuningToggle;
    private Button generateButton;
    private Button generateAllButton;
    private Button liveRefreshButton;
    private Button importMustangButton;
    private Button saveButton;
    private TextView resultText;
    private TextView saveNote;
    private TextView liveStatus;
    private TextView importMustangStatus;

    private final ExecutorService background = Executors.newSingleThreadExecutor();
    private final List<GenerationOutcome> outcomes = new ArrayList<>();
    private SetupGenerationService generationService;
    private LiveIndexController liveIndexController;
    private GeneratedSetup selectedExport;
    private MustangBaseRepository mustangBases;
    private MustangBetaEngine mustangBetaEngine;
    private MustangSetupInspector mustangInspector;
    private String pendingImportLayoutId;
    private boolean mustangBeta;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        bindViews();

        mustangBeta = BuildConfig.MUSTANG_BETA;
        VerifiedWriterProvider provider = mustangBeta ? null
                : ProviderLoader.load(BuildConfig.VERIFIED_WRITER_PROVIDER_CLASS);
        VerifiedBinaryInspector inspector = mustangBeta ? null : ProviderLoader.loadInspector(
                BuildConfig.VERIFIED_BINARY_INSPECTOR_CLASS);
        generationService = new SetupGenerationService(provider, inspector);
        liveIndexController = new LiveIndexController(this, safeLiveSources(provider));

        List<CarIdentity> displayedCars = mustangBeta
                ? Collections.singletonList(OfficialInventory.requireCar("ford_mustang_gt3"))
                : OfficialInventory.cars();
        vehicleSpinner.setAdapter(new DarkSpinnerAdapter<>(this, displayedCars));
        vehicleSpinner.setEnabled(!mustangBeta);
        layoutSpinner.setAdapter(new DarkSpinnerAdapter<>(this, OfficialInventory.layouts()));
        problemSpinner.setAdapter(new DarkSpinnerAdapter<>(this,
                Arrays.asList(FineTuningProblem.values())));
        strengthSpinner.setAdapter(new DarkSpinnerAdapter<>(this,
                Arrays.asList(FineTuningStrength.values())));

        fineTuningToggle.setOnClickListener(view -> toggleFineTuning());
        generateButton.setOnClickListener(view -> generate(false));
        generateAllButton.setOnClickListener(view -> generate(true));
        saveButton.setOnClickListener(view -> chooseExportDestination());
        liveRefreshButton.setOnClickListener(view -> refreshLiveIndex());
        importMustangButton.setOnClickListener(view -> chooseMustangBase());
        layoutSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                                 int position, long id) {
                if (mustangBeta) updateMustangBaseStatus();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        resultSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view,
                                                 int position, long id) {
                if (position >= 0 && position < outcomes.size()) render(outcomes.get(position));
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        if (mustangBeta) {
            mustangBases = new MustangBaseRepository(this);
            mustangBetaEngine = new MustangBetaEngine();
            mustangInspector = new MustangSetupInspector();
            liveStatus.setText(R.string.beta_online_unavailable);
            liveRefreshButton.setVisibility(View.GONE);
            importMustangButton.setVisibility(View.VISIBLE);
            importMustangStatus.setVisibility(View.VISIBLE);
            updateMustangBaseStatus();
        } else if (liveIndexController.hasConfirmedIndex()) {
            liveStatus.setText(liveIndexController.cachedSummary());
        } else {
            refreshLiveIndex();
        }
    }

    private void bindViews() {
        vehicleSpinner = findViewById(R.id.vehicleSpinner);
        layoutSpinner = findViewById(R.id.layoutSpinner);
        problemSpinner = findViewById(R.id.problemSpinner);
        strengthSpinner = findViewById(R.id.strengthSpinner);
        resultSpinner = findViewById(R.id.resultSpinner);
        styleGroup = findViewById(R.id.styleGroup);
        fineTuningPanel = findViewById(R.id.fineTuningPanel);
        fineTuningToggle = findViewById(R.id.fineTuningToggle);
        generateButton = findViewById(R.id.generateButton);
        generateAllButton = findViewById(R.id.generateAllButton);
        liveRefreshButton = findViewById(R.id.liveRefreshButton);
        importMustangButton = findViewById(R.id.importMustangButton);
        saveButton = findViewById(R.id.saveButton);
        resultText = findViewById(R.id.resultText);
        saveNote = findViewById(R.id.saveNote);
        liveStatus = findViewById(R.id.liveStatus);
        importMustangStatus = findViewById(R.id.importMustangStatus);
    }

    private void toggleFineTuning() {
        boolean open = fineTuningPanel.getVisibility() == View.VISIBLE;
        fineTuningPanel.setVisibility(open ? View.GONE : View.VISIBLE);
        fineTuningToggle.setText(open ? R.string.fine_tuning_open : R.string.fine_tuning_close);
    }

    private void refreshLiveIndex() {
        if (mustangBeta) {
            liveStatus.setText(R.string.beta_online_unavailable);
            return;
        }
        liveIndexController.refresh(new LiveIndexController.Listener() {
            @Override public void onProgress(String text) { runOnUiThread(() -> liveStatus.setText(text)); }
            @Override public void onComplete(String text) { runOnUiThread(() -> liveStatus.setText(text)); }
        });
    }

    private void generate(boolean allFive) {
        setBusy(true);
        selectedExport = null;
        saveButton.setEnabled(false);
        resultSpinner.setVisibility(View.GONE);
        resultText.setText(mustangBeta
                ? R.string.beta_generation_running : R.string.live_check_running);
        SetupRequest request = currentRequest();
        background.execute(() -> {
            List<GenerationOutcome> generated = generateSafely(request, allFive);
            runOnUiThread(() -> {
                outcomes.clear();
                outcomes.addAll(generated);
                if (generated.size() > 1) {
                    List<String> labels = new ArrayList<>();
                    for (int index = 0; index < generated.size(); index++) {
                        GenerationOutcome outcome = generated.get(index);
                        String style = outcome.getSetup() == null
                                ? SetupStyle.values()[index].getDisplayName()
                                : outcome.getSetup().getRequest().getStyle().getDisplayName();
                        String status = outcome.getSetup() != null
                                && outcome.getSetup().isBetaExportable()
                                ? "BETA_SAME_CAR"
                                : outcome.isExportable() ? "VERIFIZIERT" : "NICHT SICHER";
                        labels.add(style + " – " + status);
                    }
                    resultSpinner.setAdapter(new DarkSpinnerAdapter<>(this, labels));
                    resultSpinner.setVisibility(View.VISIBLE);
                }
                render(generated.get(0));
                setBusy(false);
            });
        });
    }

    private List<GenerationOutcome> generateSafely(SetupRequest request, boolean allFive) {
        try {
            if (mustangBeta) return generateMustangBeta(request, allFive);
            return allFive
                    ? generationService.generateAllFive(request)
                    : Collections.singletonList(generationService.generate(request));
        } catch (RuntimeException | IOException error) {
            return blockedOutcomes(allFive);
        }
    }

    private List<GenerationOutcome> generateMustangBeta(SetupRequest request,
                                                         boolean allFive) throws IOException {
        String layoutId = request.getLayout().getId();
        if (!mustangBases.has(layoutId)) {
            return blockedOutcomes(allFive, "BETA-BASIS FEHLT",
                    "NICHT SICHER – Für dieses exakte Layout wurde keine eigene Mustang-.carsetup zugeordnet.");
        }
        byte[] base = mustangBases.load(layoutId);
        if (!allFive) return Collections.singletonList(mustangBetaEngine.generate(base, request));
        List<GenerationOutcome> results = new ArrayList<>(SetupStyle.values().length);
        for (SetupStyle style : SetupStyle.values()) {
            SetupRequest styled = new SetupRequest(request.getCar(), request.getLayout(), style,
                    request.getFineTuningProblem(), request.getFineTuningStrength(), request.getGameVersion());
            results.add(mustangBetaEngine.generate(base, styled));
        }
        return results;
    }

    private static List<GenerationOutcome> blockedOutcomes(boolean allFive) {
        return blockedOutcomes(allFive, "LIVE-QUELLE TECHNISCH NICHT ERREICHBAR",
                "NICHT SICHER – Die Berechnung wurde nach einem technischen Fehler beendet. "
                        + "Es wurde keine Datei erzeugt.");
    }

    private static List<GenerationOutcome> blockedOutcomes(boolean allFive,
                                                            String state, String message) {
        int count = allFive ? SetupStyle.values().length : 1;
        List<GenerationOutcome> blocked = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            blocked.add(GenerationOutcome.blocked(state, message));
        }
        return blocked;
    }

    private static List<LiveSetupSource> safeLiveSources(VerifiedWriterProvider provider) {
        if (provider == null) return Collections.emptyList();
        try {
            List<LiveSetupSource> sources = provider.liveSources();
            return sources == null ? Collections.emptyList() : sources;
        } catch (RuntimeException error) {
            return Collections.emptyList();
        }
    }

    private SetupRequest currentRequest() {
        CarIdentity car = (CarIdentity) vehicleSpinner.getSelectedItem();
        TrackLayout layout = (TrackLayout) layoutSpinner.getSelectedItem();
        FineTuningProblem problem = (FineTuningProblem) problemSpinner.getSelectedItem();
        FineTuningStrength strength = (FineTuningStrength) strengthSpinner.getSelectedItem();
        return new SetupRequest(car, layout, selectedStyle(), problem, strength,
                BuildConfig.SUPPORTED_GAME_VERSION);
    }

    private SetupStyle selectedStyle() {
        int checked = styleGroup.getCheckedRadioButtonId();
        if (checked == R.id.styleAttack) return SetupStyle.FAST_ATTACK;
        if (checked == R.id.styleStable) return SetupStyle.FAST_STABLE;
        if (checked == R.id.styleSafe) return SetupStyle.FAST_SAFE;
        if (checked == R.id.styleLongRun) return SetupStyle.FAST_LONG_RUN;
        return SetupStyle.FAST_CONTROL;
    }

    private void render(GenerationOutcome outcome) {
        StringBuilder text = new StringBuilder();
        text.append(outcome.getLiveStateText()).append('\n');
        text.append(outcome.getMessage()).append("\n\n");
        GeneratedSetup setup = outcome.getSetup();
        if (setup == null || !setup.hasSavableBinary()) {
            selectedExport = null;
            saveButton.setEnabled(false);
            saveNote.setText(R.string.save_disabled);
            resultText.setText(text.toString());
            return;
        }

        selectedExport = setup;
        text.append(setup.isBetaExportable() ? MustangBetaEngine.WARNING : "VERIFIZIERT")
                .append('\n')
                .append(setup.getRequest().getCar().getDisplayName()).append('\n')
                .append(setup.getRequest().getLayout().getDisplayName()).append('\n')
                .append(setup.getRequest().getStyle().getDisplayName()).append("\n\n");

        SetupSection last = null;
        for (SetupValue value : setup.getValues()) {
            if (value.getSection() != last) {
                text.append(value.getSection().getDisplayName()).append("\n");
                last = value.getSection();
            }
            text.append("• ").append(value.getDisplayName()).append(": ")
                    .append(value.getFormattedValue());
            if (!value.isAdjustable()) {
                text.append(setup.isBetaExportable()
                        ? " (in dieser Beta unverändert)"
                        : " (fest – übersprungen)");
            }
            text.append('\n');
        }
        if (!setup.getChanges().isEmpty()) {
            text.append("\nFeintuning-Änderungen\n");
            for (ParameterChange change : setup.getChanges()) {
                text.append("• ").append(change.getDisplayName()).append(": ")
                        .append(change.getBefore()).append(" → ").append(change.getAfter()).append('\n');
            }
        }
        VerificationReport report = setup.getVerification();
        text.append("\nSHA-256: ").append(report.getSha256()).append('\n')
                .append(report.getDetails());
        resultText.setText(text.toString());
        saveButton.setEnabled(true);
        saveNote.setText(setup.isBetaExportable()
                ? R.string.beta_warning : R.string.binary_roundtrip_passed);
    }

    private void chooseExportDestination() {
        if (selectedExport == null || !selectedExport.hasSavableBinary()) return;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        intent.putExtra(Intent.EXTRA_TITLE, OutputFileName.forSetup(selectedExport));
        startActivityForResult(intent, CREATE_CARSETUP);
    }

    private void chooseMustangBase() {
        if (!mustangBeta) return;
        TrackLayout layout = (TrackLayout) layoutSpinner.getSelectedItem();
        if (layout == null) return;
        pendingImportLayoutId = layout.getId();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/octet-stream");
        startActivityForResult(intent, OPEN_MUSTANG_BASE);
    }

    private void updateMustangBaseStatus() {
        if (!mustangBeta || mustangBases == null || layoutSpinner.getSelectedItem() == null) return;
        String layoutId = ((TrackLayout) layoutSpinner.getSelectedItem()).getId();
        importMustangStatus.setText(mustangBases.has(layoutId)
                ? R.string.beta_import_ready_full : R.string.beta_import_missing_full);
    }

    private void importMustangBase(Uri source) {
        final String layoutId = pendingImportLayoutId;
        pendingImportLayoutId = null;
        if (!mustangBeta || layoutId == null) return;
        importMustangStatus.setText(R.string.beta_import_checking);
        background.execute(() -> {
            boolean saved = false;
            try {
                String name = displayName(source);
                if (name == null || !name.toLowerCase(java.util.Locale.ROOT).endsWith(".carsetup")) {
                    throw new IOException("Not a .carsetup name");
                }
                byte[] binary = readImport(source);
                MustangImportInspection inspection = mustangInspector.inspect(binary);
                if (!inspection.isValid()) throw new IOException("Mustang structure rejected");
                mustangBases.save(layoutId, binary);
                saved = true;
            } catch (IOException | SecurityException ignored) {
                // The UI reports a generic rejection and never persists rejected bytes.
            }
            final boolean result = saved;
            runOnUiThread(() -> {
                updateMustangBaseStatus();
                Toast.makeText(this, result ? R.string.beta_import_ok
                        : R.string.beta_import_failed, Toast.LENGTH_LONG).show();
            });
        });
    }

    private String displayName(Uri source) {
        try (Cursor cursor = getContentResolver().query(source,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        }
        return null;
    }

    private byte[] readImport(Uri source) throws IOException {
        try (InputStream input = getContentResolver().openInputStream(source);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (input == null) throw new IOException("Cannot open import");
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (output.size() + read > MAX_IMPORT_BYTES) throw new IOException("Import too large");
                output.write(buffer, 0, read);
            }
            if (output.size() == 0) throw new IOException("Empty import");
            return output.toByteArray();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_MUSTANG_BASE) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                importMustangBase(data.getData());
            }
            return;
        }
        if (requestCode != CREATE_CARSETUP || resultCode != RESULT_OK || data == null
                || selectedExport == null || !selectedExport.hasSavableBinary()) return;
        Uri destination = data.getData();
        if (destination == null) return;
        GeneratedSetup export = selectedExport;
        byte[] expected = export.getBinary();
        try {
            try (OutputStream output = getContentResolver().openOutputStream(destination, "wt")) {
                if (output == null) throw new IOException("No output stream");
                output.write(expected);
                output.flush();
            }
            if (!destinationMatches(destination, expected)
                    || !BinaryDigest.sha256(expected).equals(export.getVerification().getSha256())) {
                throw new IOException("Saved binary verification mismatch");
            }
            Toast.makeText(this, R.string.binary_saved, Toast.LENGTH_LONG).show();
        } catch (IOException | SecurityException error) {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_LONG).show();
        }
    }

    private boolean destinationMatches(Uri destination, byte[] expected) throws IOException {
        try (InputStream input = getContentResolver().openInputStream(destination);
             ByteArrayOutputStream copy = new ByteArrayOutputStream(expected.length)) {
            if (input == null) return false;
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (copy.size() + read > expected.length) return false;
                copy.write(buffer, 0, read);
            }
            return Arrays.equals(expected, copy.toByteArray());
        }
    }

    private void setBusy(boolean busy) {
        generateButton.setEnabled(!busy);
        generateAllButton.setEnabled(!busy);
        vehicleSpinner.setEnabled(!busy && !mustangBeta);
        layoutSpinner.setEnabled(!busy);
        problemSpinner.setEnabled(!busy);
        strengthSpinner.setEnabled(!busy);
        fineTuningToggle.setEnabled(!busy);
        liveRefreshButton.setEnabled(!busy && !mustangBeta);
        importMustangButton.setEnabled(!busy && mustangBeta);
        resultSpinner.setEnabled(!busy);
        for (int index = 0; index < styleGroup.getChildCount(); index++) {
            styleGroup.getChildAt(index).setEnabled(!busy);
        }
        saveButton.setEnabled(!busy && selectedExport != null && selectedExport.hasSavableBinary());
    }

    @Override protected void onDestroy() {
        background.shutdownNow();
        liveIndexController.shutdown();
        super.onDestroy();
    }
}
