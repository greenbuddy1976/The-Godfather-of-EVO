package com.greenbuddy.acevosetupengineer.engine;

import com.greenbuddy.acevosetupengineer.live.LiveCandidate;
import com.greenbuddy.acevosetupengineer.live.LiveLookupCoordinator;
import com.greenbuddy.acevosetupengineer.live.LiveLookupReport;
import com.greenbuddy.acevosetupengineer.model.GeneratedSetup;
import com.greenbuddy.acevosetupengineer.model.GenerationOutcome;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.model.SetupStyle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SetupGenerationService {
    private final VerifiedWriterProvider provider;
    private final LiveLookupCoordinator live;

    public SetupGenerationService(VerifiedWriterProvider provider) {
        this.provider = provider;
        this.live = new LiveLookupCoordinator(provider == null
                ? Collections.emptyList() : provider.liveSources());
    }

    public GenerationOutcome generate(SetupRequest request) {
        LiveLookupReport liveReport = live.findExact(request);
        String liveState = liveReport.hasTechnicalErrors()
                ? "LIVE-QUELLE TECHNISCH NICHT ERREICHBAR"
                : liveReport.getCandidates().isEmpty()
                    ? "KEIN EXAKTER LIVE-TREFFER" : "LIVE EXACT GEFUNDEN";

        if (provider != null) {
            for (LiveCandidate candidate : liveReport.getCandidates()) {
                try {
                    GeneratedSetup setup = provider.verifyLiveExact(request, candidate);
                    if (isExactVerified(setup, request)) {
                        return GenerationOutcome.success(GenerationOutcome.State.LIVE_EXACT_FOUND,
                                "LIVE EXACT GEFUNDEN", "LIVE EXACT – vollständig binär geprüft", setup);
                    }
                } catch (Exception ignored) {
                    // A malformed hit is rejected; the engineering path remains available.
                }
            }
        }

        if (provider == null || !provider.supports(request)
                || !request.getGameVersion().equals(provider.supportedGameVersion())) {
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

    private static boolean isExactVerified(GeneratedSetup setup, SetupRequest request) {
        return setup != null && setup.isExportable()
                && setup.getRequest().exactKey().equals(request.exactKey())
                && setup.getRequest().getStyle() == request.getStyle();
    }
}
