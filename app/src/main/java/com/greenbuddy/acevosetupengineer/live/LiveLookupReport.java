package com.greenbuddy.acevosetupengineer.live;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LiveLookupReport {
    private final List<LiveCandidate> candidates;
    private final List<String> technicalErrors;
    private final int completeRounds;

    public LiveLookupReport(List<LiveCandidate> candidates, List<String> technicalErrors,
                            int completeRounds) {
        this.candidates = Collections.unmodifiableList(new ArrayList<>(candidates));
        this.technicalErrors = Collections.unmodifiableList(new ArrayList<>(technicalErrors));
        this.completeRounds = completeRounds;
    }

    public List<LiveCandidate> getCandidates() { return candidates; }
    public List<String> getTechnicalErrors() { return technicalErrors; }
    public int getCompleteRounds() { return completeRounds; }
    public boolean hasTechnicalErrors() { return !technicalErrors.isEmpty(); }
}
