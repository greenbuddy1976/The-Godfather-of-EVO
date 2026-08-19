package com.greenbuddy.acevosetupengineer.live;

import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LiveLookupCoordinator {
    public static final int MAX_COMPLETE_ROUNDS = 2;
    private final List<LiveSetupSource> sources;

    public LiveLookupCoordinator(List<LiveSetupSource> sources) {
        this.sources = sources == null
                ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(sources));
    }

    public LiveLookupReport findExact(SetupRequest request) {
        List<LiveCandidate> candidates = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int rounds = 0;
        for (int round = 0; round < MAX_COMPLETE_ROUNDS; round++) {
            rounds++;
            for (LiveSetupSource source : sources) {
                try {
                    List<LiveCandidate> found = source.exactLookup(request);
                    if (found == null) continue;
                    for (LiveCandidate candidate : found) {
                        if (isExact(candidate, request)) candidates.add(candidate);
                    }
                } catch (Exception error) {
                    errors.add(source.name() + ": " + error.getClass().getSimpleName());
                }
            }
            if (!candidates.isEmpty()) break;
        }
        return new LiveLookupReport(candidates, errors, rounds);
    }

    private static boolean isExact(LiveCandidate candidate, SetupRequest request) {
        return request.getCar().getId().equals(candidate.getCarId())
                && request.getLayout().getId().equals(candidate.getLayoutId())
                && request.getGameVersion().equals(candidate.getGameVersion());
    }
}
