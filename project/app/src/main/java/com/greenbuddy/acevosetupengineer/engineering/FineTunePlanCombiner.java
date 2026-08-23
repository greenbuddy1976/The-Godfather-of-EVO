package com.greenbuddy.acevosetupengineer.engineering;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class FineTunePlanCombiner {
    public FineTunePlan combine(FineTunePlan first, FineTunePlan second) {
        if (first.origin != second.origin) {
            throw new IllegalArgumentException("Fine-Tuning-Ursprünge stimmen nicht überein");
        }
        Map<ParameterKey, Accumulated> combined = new EnumMap<>(ParameterKey.class);
        for (ParameterAdjustment adjustment : first.adjustments) add(combined, adjustment);
        for (ParameterAdjustment adjustment : second.adjustments) add(combined, adjustment);
        List<ParameterAdjustment> adjustments = new ArrayList<>();
        for (Map.Entry<ParameterKey, Accumulated> entry : combined.entrySet()) {
            double delta = Math.max(-1.0, Math.min(1.0, entry.getValue().delta));
            if (Math.abs(delta) >= 0.05) {
                adjustments.add(new ParameterAdjustment(entry.getKey(), delta,
                        String.join("; ", entry.getValue().reasons)));
            }
        }
        List<HandlingIssue> issues = new ArrayList<>(first.interpretedIssues);
        for (HandlingIssue issue : second.interpretedIssues) if (!issues.contains(issue)) issues.add(issue);
        return new FineTunePlan(first.origin, issues, adjustments);
    }

    private static void add(Map<ParameterKey, Accumulated> target, ParameterAdjustment adjustment) {
        Accumulated value = target.computeIfAbsent(adjustment.parameter, ignored -> new Accumulated());
        value.delta += adjustment.normalizedDelta;
        value.reasons.add(adjustment.reason);
    }

    private static final class Accumulated {
        double delta;
        final List<String> reasons = new ArrayList<>();
    }
}
