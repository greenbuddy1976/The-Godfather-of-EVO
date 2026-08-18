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

/** Executes exactly three complete LIVE rounds across every configured provider. */
public final class LiveSearchCoordinator {
    public static final int REQUIRED_ROUNDS = 3;

    private final List<LiveProvider> providers;
    private final ExactCache cache;
    private final ProgressListener progressListener;

    public interface ProgressListener {
        void onProgress(String message);
    }

    public LiveSearchCoordinator(List<LiveProvider> providers, ExactCache cache) {
        this(providers, cache, message -> { });
    }

    public LiveSearchCoordinator(
            List<LiveProvider> providers,
            ExactCache cache,
            ProgressListener progressListener) {
        if (providers == null || providers.isEmpty()) {
            throw new IllegalArgumentException("Mindestens ein LIVE-Provider ist erforderlich");
        }
        this.providers = Collections.unmodifiableList(new ArrayList<>(providers));
        this.cache = cache;
        this.progressListener = progressListener == null ? message -> { } : progressListener;
    }

    public LiveSearchSummary search(SetupRequest request) {
        List<String> audit = new ArrayList<>();
        VerifiedExact firstVerified = null;
        int successfulProviderCalls = 0;
        int technicalProviderFailures = 0;

        for (int round = 1; round <= REQUIRED_ROUNDS; round++) {
            record(audit, "LIVE-Runde " + round + "/" + REQUIRED_ROUNDS + " gestartet");
            for (LiveProvider provider : providers) {
                try {
                    List<ExactCandidate> candidates = provider.searchExact(request);
                    successfulProviderCalls++;
                    record(audit, provider.name() + ": " + candidates.size() + " Metadaten-Treffer");
                    for (ExactCandidate candidate : candidates) {
                        VerifiedExact verified = verifyAndDownload(provider, request, candidate, round, audit);
                        if (firstVerified == null && verified != null) firstVerified = verified;
                    }
                } catch (IOException | RuntimeException ex) {
                    technicalProviderFailures++;
                    record(audit, provider.name() + ": TECHNISCHER FEHLER: " + safeMessage(ex));
                }
            }
            record(audit, "LIVE-Runde " + round + "/" + REQUIRED_ROUNDS + " abgeschlossen");
        }

        if (firstVerified != null) {
            if (cache != null) cache.put(request, firstVerified);
            record(audit, "VERIFIZIERT: EXACT aus LIVE-Suche");
            return new LiveSearchSummary(LiveSearchSummary.Status.EXACT, firstVerified,
                    false, REQUIRED_ROUNDS, audit);
        }

        if (successfulProviderCalls == 0) {
            VerifiedExact cached = cache == null ? null : cache.get(request);
            if (cached != null) {
                VerifiedExact markedCache = new VerifiedExact(cached.candidate, cached.bytes, cached.sha256,
                        cached.decodedVehicleSignature, REQUIRED_ROUNDS, true);
                record(audit, "LIVE-SUCHE FEHLGESCHLAGEN NACH 3 VERSUCHEN; exakt passender verifizierter Cache verwendet");
                return new LiveSearchSummary(LiveSearchSummary.Status.EXACT, markedCache,
                        true, REQUIRED_ROUNDS, audit);
            }
            record(audit, "LIVE-SUCHE FEHLGESCHLAGEN NACH 3 VERSUCHEN; Existenz eines EXACT ist unbekannt");
            return new LiveSearchSummary(LiveSearchSummary.Status.LIVE_FAILED_AFTER_3_ROUNDS,
                    null, true, REQUIRED_ROUNDS, audit);
        }

        if (technicalProviderFailures > 0) {
            record(audit, "Kein verifiziertes EXACT; mindestens eine Quelle hatte technische Fehler");
            return new LiveSearchSummary(
                    LiveSearchSummary.Status.NO_EXACT_WITH_TECHNICAL_ERRORS_AFTER_3_ROUNDS,
                    null, true, REQUIRED_ROUNDS, audit);
        }

        record(audit, "Drei vollständige LIVE-Runden erfolgreich; kein verifiziertes EXACT");
        return new LiveSearchSummary(LiveSearchSummary.Status.NO_EXACT_AFTER_3_ROUNDS,
                null, false, REQUIRED_ROUNDS, audit);
    }

    private void record(List<String> audit, String message) {
        audit.add(message);
        progressListener.onProgress(message);
    }

    private static VerifiedExact verifyAndDownload(
            LiveProvider provider,
            SetupRequest request,
            ExactCandidate candidate,
            int round,
            List<String> audit) throws IOException {
        if (!same(candidate.vehicleSlug, request.vehicle.providerSlug)
                || !same(candidate.layoutSlug, request.layout.providerSlug)) {
            audit.add(provider.name() + ": abgelehnt – Fahrzeug oder exaktes Layout weicht ab");
            return null;
        }
        if (!same(candidate.gameVersion, request.gameVersion)) {
            audit.add(provider.name() + ": abgelehnt – Setup-Version " + candidate.gameVersion
                    + " ist nicht exakt Spielversion " + request.gameVersion);
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
        audit.add(provider.name() + ": EXACT verifiziert, SHA-256 " + sha);
        return new VerifiedExact(candidate, bytes, sha, inspection.vehicleSignature, round, false);
    }

    private static boolean same(String left, String right) {
        return left != null && right != null && left.trim().toLowerCase(Locale.ROOT)
                .equals(right.trim().toLowerCase(Locale.ROOT));
    }

    private static String safeMessage(Exception ex) {
        String value = ex.getMessage();
        return value == null || value.trim().isEmpty() ? ex.getClass().getSimpleName() : value;
    }
}
