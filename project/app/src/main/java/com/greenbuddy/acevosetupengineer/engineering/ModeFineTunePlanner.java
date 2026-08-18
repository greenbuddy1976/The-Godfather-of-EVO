package com.greenbuddy.acevosetupengineer.engineering;

import com.greenbuddy.acevosetupengineer.model.SetupMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Relative style changes only; every number is later converted through the car's verified step. */
public final class ModeFineTunePlanner {
    public FineTunePlan plan(SetupMode mode, FineTunePlan.Origin origin) {
        List<ParameterAdjustment> adjustments = new ArrayList<>();
        switch (mode) {
            case HOTLAP_ATTACK -> {
                add(adjustments, ParameterKey.BRAKE_BIAS, -0.50,
                        "Michael-Schumacher-Modus: zwei verifizierte Schritte mehr Einlenkrotation beim Bremsen");
                add(adjustments, ParameterKey.REAR_WING, -0.50,
                        "Michael-Schumacher-Modus: zwei verifizierte Schritte weniger stabilisierende Aero");
                add(adjustments, ParameterKey.TRACTION_CONTROL, -0.25,
                        "Michael-Schumacher-Modus: einen verifizierten Schritt weniger Eingriff");
            }
            case FAST_CONTROL -> {
                add(adjustments, ParameterKey.BRAKE_BIAS, -0.25,
                        "Walter-Röhrl-Modus: einen verifizierten Schritt mehr Bremsrotation");
                add(adjustments, ParameterKey.REAR_WING, -0.25,
                        "Walter-Röhrl-Modus: einen verifizierten Schritt direktere Aero-Balance");
            }
            case STABLE_LONGRUN -> {
                add(adjustments, ParameterKey.BRAKE_BIAS, 0.25,
                        "Dieter-Düsel-Modus: einen verifizierten Schritt mehr Bremsstabilität");
                add(adjustments, ParameterKey.REAR_WING, 0.25,
                        "Dieter-Düsel-Modus: einen verifizierten Schritt mehr Heckstabilität");
                add(adjustments, ParameterKey.TRACTION_CONTROL, 0.25,
                        "Dieter-Düsel-Modus: einen verifizierten Schritt mehr Traktionsreserve");
            }
            case SAFE -> {
                add(adjustments, ParameterKey.BRAKE_BIAS, 0.50,
                        "Oma-Hertha-Modus: zwei verifizierte Schritte mehr Bremsstabilität");
                add(adjustments, ParameterKey.REAR_WING, 0.50,
                        "Oma-Hertha-Modus: zwei verifizierte Schritte mehr Heckstabilität");
                add(adjustments, ParameterKey.TRACTION_CONTROL, 0.50,
                        "Oma-Hertha-Modus: zwei verifizierte Schritte mehr Traktionsreserve");
                add(adjustments, ParameterKey.ABS, 0.25,
                        "Oma-Hertha-Modus: einen verifizierten Schritt mehr Blockierreserve");
            }
        }
        return new FineTunePlan(origin, Collections.emptyList(), adjustments);
    }

    private static void add(List<ParameterAdjustment> target, ParameterKey key, double delta, String reason) {
        target.add(new ParameterAdjustment(key, delta, reason));
    }
}
