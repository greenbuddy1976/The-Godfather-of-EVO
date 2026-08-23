package com.greenbuddy.acevosetupengineer.engineering;

import java.util.Collections;
import java.util.List;

public final class FineTunePlan {
    public enum Origin {
        EXACT_DERIVATIVE,
        ENGINEERING_MODEL
    }

    public final Origin origin;
    public final List<HandlingIssue> interpretedIssues;
    public final List<ParameterAdjustment> adjustments;
    public final boolean requiresVerifiedVehicleProfile;

    public FineTunePlan(
            Origin origin,
            List<HandlingIssue> interpretedIssues,
            List<ParameterAdjustment> adjustments) {
        this.origin = origin;
        this.interpretedIssues = Collections.unmodifiableList(interpretedIssues);
        this.adjustments = Collections.unmodifiableList(adjustments);
        this.requiresVerifiedVehicleProfile = true;
    }
}
