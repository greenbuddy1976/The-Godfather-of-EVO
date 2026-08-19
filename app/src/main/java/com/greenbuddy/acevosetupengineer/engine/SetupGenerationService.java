package com.greenbuddy.acevosetupengineer.engine;

import com.greenbuddy.acevosetupengineer.live.LiveCandidate;
import com.greenbuddy.acevosetupengineer.live.LiveLookupCoordinator;
import com.greenbuddy.acevosetupengineer.live.LiveLookupReport;
import com.greenbuddy.acevosetupengineer.live.LiveSetupSource;
import com.greenbuddy.acevosetupengineer.model.GeneratedSetup;
import com.greenbuddy.acevosetupengineer.model.GenerationOutcome;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.model.SetupStyle;
import com.greenbuddy.acevosetupengineer.model.SetupValue;
import com.greenbuddy.acevosetupengineer.verification.BinaryInspection;
import com.greenbuddy.acevosetupengineer.verification.VerifiedBinaryInspector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SetupGenerationService {
    private final VerifiedWriterProvider provider;
    private final VerifiedBinaryInspector inspector;
    private final LiveLookupCoordinator live;

    public SetupGenerationService(VerifiedWriterProvider provider) {
        this(provider, null);
    }

    public SetupGenerationService(VerifiedWriterProvider provider,
                                  VerifiedBinaryInspector inspector) {
        this.provider = provider;
        this.inspector = inspector;
        this.live = new LiveLookupCoordinator(safeLiveSources(provider));
    }

    public GenerationOutcome generate(SetupRequest request) {
        boolean providerVersionMatches = provider != null
                && inspector != null
                && request.getGameVersion().equals(provider.supportedGameVersion())
                && request.getGameVersion().equals(inspector.supportedGameVersion());
        List<String> liveErrors = new ArrayList<>();
        for (int round = 0; round < LiveLookupCoordinator.MAX_COMPLETE_ROUNDS; round++) {
            LiveLookupReport liveReport = live.findExactRound(request);
            liveErrors.addAll(liveReport.getTechnicalErrors());
            if (providerVersionMatches) {
                for (LiveCandidate candidate : liveReport.getCandidates()) {
                    try {
                        GeneratedSetup setup = provider.verifyLiveExact(request, candidate);
                        if (isExactVerified(setup, request)) {
                            return GenerationOutcome.success(GenerationOutcome.State.LIVE_EXACT_FOUND,
                                    "LIVE EXACT GEFUNDEN", "LIVE EXACT – vollständig binär geprüft", setup);
                        }
                    } catch (Exception ignored) {
                        // An invalid round-one candidate does not suppress the second complete round.
                    }
                }
            }
        }
        String liveState = !liveErrors.isEmpty()
                ? "LIVE-QUELLE TECHNISCH NICHT ERREICHBAR"
                : "KEIN EXAKTER LIVE-TREFFER";

        if (!providerVersionMatches || !provider.supports(request)) {
            return GenerationOutcome.blocked(liveState,
                    "NICHT SICHER – Für diese exakte Fahrzeug-/Layout-/Versionskombination "
                    + "ist kein verifizierter Writer mit Engineering-Profil geladen. Es wurde "
                    + "keine Datei erfunden.");
        }

        try {
            GeneratedSetup setup = provider.generateEngineeringModel(request);
            if (!isExactVerified(setup, request)) {
                return GenerationOutcome.blocked(liveState,
                        "NICHT SICHER – Binär-Roundtrip oder Identitätsprüfung fehlgeschlagen.");
            }
            return GenerationOutcome.success(GenerationOutcome.State.ENGINEERING_MODEL_RECALCULATED,
                    liveState, "ENGINEERING MODEL – VOLLSTÄNDIG NEU BERECHNET", setup);
        } catch (Exception error) {
            return GenerationOutcome.blocked(liveState,
                    "NICHT SICHER – Engineering-Berechnung wurde ohne verifiziertes Ergebnis abgebrochen.");
        }
    }

    public List<GenerationOutcome> generateAllFive(SetupRequest base) {
        List<GenerationOutcome> results = new ArrayList<>(SetupStyle.values().length);
        for (SetupStyle style : SetupStyle.values()) {
            SetupRequest request = new SetupRequest(base.getCar(), base.getLayout(), style,
                    base.getFineTuningProblem(), base.getFineTuningStrength(), base.getGameVersion());
            results.add(generate(request));
        }
        return Collections.unmodifiableList(results);
    }

    private boolean isExactVerified(GeneratedSetup setup, SetupRequest request) throws Exception {
        if (setup == null || !setup.isExportable() || setup.getValues().isEmpty()
                || !setup.getRequest().requestKey().equals(request.requestKey())) return false;
        BinaryInspection inspection = inspector.inspect(request, setup.getBinary());
        return inspection != null && inspection.verifies(setup.getBinary())
                && sameValues(setup.getValues(), inspection.getDecodedValues());
    }

    private static boolean sameValues(List<SetupValue> expected, List<SetupValue> decoded) {
        if (expected.size() != decoded.size()) return false;
        for (int index = 0; index < expected.size(); index++) {
            SetupValue left = expected.get(index);
            SetupValue right = decoded.get(index);
            if (left.getSection() != right.getSection()
                    || left.getPosition() != right.getPosition()
                    || !left.getKey().equals(right.getKey())
                    || !left.getFormattedValue().equals(right.getFormattedValue())
                    || left.isAdjustable() != right.isAdjustable()) return false;
        }
        return true;
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
}
