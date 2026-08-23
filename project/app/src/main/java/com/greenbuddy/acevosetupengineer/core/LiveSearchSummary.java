package com.greenbuddy.acevosetupengineer.core;

import com.greenbuddy.acevosetupengineer.model.VerifiedExact;

import java.util.Collections;
import java.util.List;

public final class LiveSearchSummary {
    public enum Status {
        EXACT,
        NO_EXACT_AFTER_2_ROUNDS,
        LIVE_FAILED_AFTER_2_ROUNDS,
        NO_EXACT_WITH_TECHNICAL_ERRORS_AFTER_2_ROUNDS
    }

    public final Status status;
    public final VerifiedExact exact;
    public final boolean liveUnverified;
    public final int completedRounds;
    public final List<String> auditLog;
    public final VerifiedStructureCarrier structureCarrier;

    public LiveSearchSummary(
            Status status,
            VerifiedExact exact,
            boolean liveUnverified,
            int completedRounds,
            List<String> auditLog,
            VerifiedStructureCarrier structureCarrier) {
        this.status = status;
        this.exact = exact;
        this.liveUnverified = liveUnverified;
        this.completedRounds = completedRounds;
        this.auditLog = Collections.unmodifiableList(auditLog);
        this.structureCarrier = structureCarrier;
    }
}
