package com.greenbuddy.acevosetupengineer.engineering;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Applies engineering intent only through verified per-car step/range definitions. */
public final class FineTuneEngine {
    public EngineeringSetup apply(
            EngineeringSetup base,
            EngineeringProfile profile,
            FineTunePlan plan) {
        if (plan.adjustments.isEmpty()) {
            throw new SetupValidationException("Fahrverhalten wurde nicht eindeutig verstanden");
        }
        profile.requireFineTuneUsableFor(profile.vehicleId, profile.gameVersion);

        Map<ParameterKey, Double> result = new EnumMap<>(ParameterKey.class);
        result.putAll(base.values);
        List<String> audit = new ArrayList<>(base.audit);
        audit.add("Fine-Tuning-Ursprung: " + plan.origin.name());

        int applied = 0;
        for (ParameterAdjustment adjustment : plan.adjustments) {
            ParameterDefinition definition = profile.parameters.get(adjustment.parameter);
            if (definition == null || !definition.fineTuneWritable()) {
                audit.add("Übersprungen: " + adjustment.parameter.displayName
                        + " – Bereich oder Binärschreibweg nicht verifiziert");
                continue;
            }
            Double before = result.get(adjustment.parameter);
            if (before == null) {
                audit.add("Übersprungen: " + adjustment.parameter.displayName
                        + " – im Ausgangssetup nicht sicher decodiert");
                continue;
            }
            double requestedSteps = adjustment.normalizedDelta * definition.maximumFineTuneSteps;
            double after = definition.clampAndRound(before + requestedSteps * definition.step);
            if (!definition.contains(after)) {
                throw new SetupValidationException("Fine-Tuning außerhalb des Bereichs: "
                        + adjustment.parameter.displayName);
            }
            if (Math.abs(after - before) < 1e-9) {
                audit.add("Übersprungen: " + adjustment.parameter.displayName
                        + " – verifizierte Grenze bereits erreicht");
                continue;
            }
            result.put(adjustment.parameter, after);
            applied++;
            audit.add(adjustment.parameter.section.displayName + " / " + adjustment.parameter.displayName
                    + ": " + before + " -> " + after + " " + definition.unit
                    + " – " + adjustment.reason);
        }

        if (applied == 0) {
            throw new SetupValidationException(
                    "Keiner der erforderlichen Fine-Tuning-Parameter ist für dieses Fahrzeug sicher verfügbar");
        }

        EngineeringSetup.Label label = plan.origin == FineTunePlan.Origin.EXACT_DERIVATIVE
                ? EngineeringSetup.Label.EXACT_DERIVATIVE
                : EngineeringSetup.Label.ENGINEERING_MODEL;
        audit.add("Roundtrip vor Export zwingend: patch -> decode -> Identität -> Bereiche -> Plausibilität");
        return new EngineeringSetup(label, result, audit);
    }
}
