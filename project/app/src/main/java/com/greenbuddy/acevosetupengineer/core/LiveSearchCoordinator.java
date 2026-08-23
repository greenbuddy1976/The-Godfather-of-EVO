package com.greenbuddy.acevosetupengineer.core;

import com.greenbuddy.acevosetupengineer.binary.CarSetupInspection;
import com.greenbuddy.acevosetupengineer.binary.CarSetupInspector;
import com.greenbuddy.acevosetupengineer.model.ExactCandidate;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.model.VerifiedExact;
import com.greenbuddy.acevosetupengineer.util.Hashing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Executes exactly two complete LIVE rounds across every configured provider. */
public final class LiveSearchCoordinator {
    public static final int REQUIRED_ROUNDS = 2;
    // Inspect enough same-car candidates to avoid selecting an unusually sparse
    // protobuf as the reusable structure when the provider offers richer files.
    private static final int MAX_STRUCTURE_DOWNLOADS_PER_PROVIDER_ROUND = 12;

    private final List<LiveProvider> providers;
    private final ExactCache cache;
    private final StructureCarrierCache structureCache;
    private final ProgressListener progressListener;

    public interface ProgressListener {
        void onProgress(String message);
    }

    public LiveSearchCoordinator(List<LiveProvider> providers, ExactCache cache) {
        this(providers, cache, new MemoryStructureCarrierCache(), message -> { });
    }

    public LiveSearchCoordinator(
            List<LiveProvider> providers,
            ExactCache cache,
            ProgressListener progressListener) {
        this(providers, cache, new MemoryStructureCarrierCache(), progressListener);
    }

    public LiveSearchCoordinator(
            List<LiveProvider> providers,
            ExactCache cache,
            StructureCarrierCache structureCache,
            ProgressListener progressListener) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalArgumentException("Mindestens ein LIVE-Provider ist erforderlich");
        }
        this.providers = Collections.unmodifiableList(new ArrayList<>(providers));
        this.cache = cache;
        this.structureCache = structureCache;
        this.progressListener = progressListener == null ? message -> { } : progressListener;
    }

    public LiveSearchSummary search(SetupRequest request) {
        List<String> audit = new ArrayList<>();
        VerifiedExact firstVerified = null;
        VerifiedStructureCarrier bestStructure = null;
        int bestStructureFloatCount = -1;
        int successfulProviderCalls = 0;
        int technicalProviderFailures = 0;

        for (int round = 1; round <= REQUIRED_ROUNDS; round++) {
            record(audit, "LIVE-Runde " + round + "/" + REQUIRED_ROUNDS + " gestartet");
            for (LiveProvider provider : providers) {
                try {
                    List<ExactCandidate> candidates = provider.searchExact(request);
                    successfulProviderCalls++;
                    record(audit, provider.name() + ": " + candidates.size() + " Metadaten-Treffer");
                    int structureDownloads = 0;
                    for (ExactCandidate candidate : candidates) {
                        boolean exactMetadata = exactMetadata(candidate, request);
                        boolean inspectForStructure = structureDownloads
                                < MAX_STRUCTURE_DOWNLOADS_PER_PROVIDER_ROUND;
                        if (!exactMetadata && !inspectForStructure) continue;
                        if (inspectForStructure) structureDownloads++;
                        try {
                            CandidateVerification verified = verifyAndDownload(
                                    provider, request, candidate, round, exactMetadata, audit);
                            if (verified == null) continue;
                            if (verified.inspection.floatCount > bestStructureFloatCount) {
                                bestStructureFloatCount = verified.inspection.floatCount;
                                bestStructure = verified.structureCarrier;
                            }
                            if (firstVerified == null && verified.exact != null) {
                                firstVerified = verified.exact;
                            }
                        } catch (IOException | RuntimeException candidateFailure) {
                            technicalProviderFailures++;
                            record(audit, provider.name() + ": Download technisch fehlgeschlagen: "
                                    + safeMessage(candidateFailure));
                        }
                    }
                } catch (IOException | RuntimeException ex) {
                    technicalProviderFailures++;
                    record(audit, provider.name() + ": TECHNISCHER FEHLER: " + safeMessage(ex));
                }
            }
            record(audit, "LIVE-Runde " + round + "/" + REQUIRED_ROUNDS + " abgeschlossen");
        }

        if (bestStructure != null && structureCache != null) {
            structureCache.put(request, bestStructure);
            record(audit, "AUTO-STRUKTUR: gleiche Fahrzeug-Signatur LIVE verifiziert");
        } else if (structureCache != null) {
            bestStructure = structureCache.get(request);
            if (bestStructure != null) {
                record(audit, "AUTO-STRUKTUR: integritätsgeprüfter Same-Car-Cache verfügbar");
            }
        }

        if (firstVerified != null) {
            if (cache != null) cache.put(request, firstVerified);
            record(audit, "VERIFIZIERT: EXACT aus LIVE-Suche");
            return new LiveSearchSummary(LiveSearchSummary.Status.EXACT, firstVerified,
                    technicalProviderFailures > 0, REQUIRED_ROUNDS, audit, bestStructure);
        }

        if (successfulProviderCalls == 0) {
            VerifiedExact cached = cache == null ? null : cache.get(request);
            if (cached != null) {
                VerifiedExact markedCache = new VerifiedExact(cached.candidate, cached.bytes, cached.sha256,
                        cached.decodedVehicleSignature, REQUIRED_ROUNDS, true);
                record(audit, "LIVE-SUCHE FEHLGESCHLAGEN NACH 2 VERSUCHEN; exakt passender verifizierter Cache verwendet");
                return new LiveSearchSummary(LiveSearchSummary.Status.EXACT, markedCache,
                        true, REQUIRED_ROUNDS, audit, bestStructure);
            }
            record(audit, "LIVE-SUCHE FEHLGESCHLAGEN NACH 2 VERSUCHEN; Existenz eines EXACT ist unbekannt");
            return new LiveSearchSummary(LiveSearchSummary.Status.LIVE_FAILED_AFTER_2_ROUNDS,
                    null, true, REQUIRED_ROUNDS, audit, bestStructure);
        }

        if (technicalProviderFailures > 0) {
            record(audit, "Kein verifiziertes EXACT; mindestens eine Quelle hatte technische Fehler");
            return new LiveSearchSummary(
                    LiveSearchSummary.Status.NO_EXACT_WITH_TECHNICAL_ERRORS_AFTER_2_ROUNDS,
                    null, true, REQUIRED_ROUNDS, audit, bestStructure);
        }

        record(audit, "Zwei vollständige LIVE-Runden erfolgreich; kein verifiziertes EXACT");
        return new LiveSearchSummary(LiveSearchSummary.Status.NO_EXACT_AFTER_2_ROUNDS,
                null, false, REQUIRED_ROUNDS, audit, bestStructure);
    }

    private void record(List<String> audit, String message) {
        audit.add(message);
        progressListener.onProgress(message);
    }

    private static CandidateVerification verifyAndDownload(
            LiveProvider provider,
            SetupRequest request,
            ExactCandidate candidate,
            int round,
            boolean exactMetadata,
            List<String> audit) throws IOException {
        if (!same(candidate.vehicleSlug, request.vehicle.providerSlug)) {
            audit.add(provider.name() + ": abgelehnt – Fahrzeug weicht ab");
            return null;
        }
        if (!request.vehicle.hasVerifiedBinaryIdentity()) {
            audit.add(provider.name() + ": abgelehnt – keine verifizierte Fahrzeug-Signatur im Manifest");
            return null;
        }

        byte[] bytes = provider.download(candidate);
        CarSetupInspection inspection = CarSetupInspector.inspect(bytes);
        if (!inspection.structurallyValid) {
            audit.add(provider.name() + ": Binärdatei abgelehnt – " + inspection.failureReason);
            return null;
        }
        if (!inspection.vehicleSignature.startsWith(request.vehicle.expectedSignaturePrefix)) {
            audit.add(provider.name() + ": Binärdatei abgelehnt – Signatur passt nicht zum Fahrzeug");
            return null;
        }
        String sha = Hashing.sha256(bytes);
        VerifiedStructureCarrier carrier = new VerifiedStructureCarrier(bytes, sha,
                inspection.vehicleSignature, provider.name() + " / " + candidate.sourceUrl, false);
        VerifiedExact exact = null;
        if (exactMetadata) {
            audit.add(provider.name() + ": EXACT verifiziert, SHA-256 " + sha);
            exact = new VerifiedExact(candidate, bytes, sha, inspection.vehicleSignature, round, false);
        } else {
            audit.add(provider.name() + ": Same-Car-Struktur verifiziert; Werte sind keine Modell-Eingabe");
        }
        return new CandidateVerification(exact, carrier, inspection);
    }

    private static boolean exactMetadata(ExactCandidate candidate, SetupRequest request) {
        return same(candidate.vehicleSlug, request.vehicle.providerSlug)
                && same(candidate.layoutSlug, request.layout.providerSlug)
                && same(candidate.gameVersion, request.gameVersion);
    }

    private static boolean same(String left, String right) {
        return left != null && right != null && left.trim().toLowerCase(Locale.ROOT)
                .equals(right.trim().toLowerCase(Locale.ROOT));
    }

    private static String safeMessage(Exception ex) {
        String value = ex.getMessage();
        return value == null || value.trim().isEmpty() ? ex.getClass().getSimpleName() : value;
    }

    private record CandidateVerification(
            VerifiedExact exact,
            VerifiedStructureCarrier structureCarrier,
            CarSetupInspection inspection) {}
}
