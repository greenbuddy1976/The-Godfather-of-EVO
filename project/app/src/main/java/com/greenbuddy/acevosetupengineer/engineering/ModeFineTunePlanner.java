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
            case FAST_CONTROL -> {
                add(adjustments, ParameterKey.BRAKE_BIAS, 0.25,
                        "FAST CONTROL: ein verifizierter Schritt mehr Bremsstabilität");
                add(adjustments, ParameterKey.REAR_WING, 0.25,
                        "FAST CONTROL: ein verifizierter Schritt mehr Hochgeschwindigkeitsstabilität");
                add(adjustments, ParameterKey.TRACTION_CONTROL, 0.25,
                        "FAST CONTROL: ein verifizierter Schritt Traktionsreserve");
                add(adjustments, ParameterKey.ANTI_ROLL_BAR_REAR, -0.25,
                        "FAST CONTROL: Hinterachse mechanisch beruhigen");
                add(adjustments, ParameterKey.TOE_REAR, 0.25,
                        "FAST CONTROL: stabilisierende Hinterachsspur");
                add(adjustments, ParameterKey.SPRING_REAR, -0.25,
                        "FAST CONTROL: mehr mechanischer Hinterachsgrip");
            }
            case FAST_ATTACK -> {
                add(adjustments, ParameterKey.BRAKE_BIAS, -0.25,
                        "FAST ATTACK: ein verifizierter Schritt mehr Einlenkrotation beim Bremsen");
                add(adjustments, ParameterKey.REAR_WING, -0.25,
                        "FAST ATTACK: ein verifizierter Schritt weniger Luftwiderstand");
                add(adjustments, ParameterKey.TRACTION_CONTROL, -0.25,
                        "FAST ATTACK: ein verifizierter Schritt weniger Eingriff");
                add(adjustments, ParameterKey.ANTI_ROLL_BAR_REAR, 0.25,
                        "FAST ATTACK: direktere Rotation");
                add(adjustments, ParameterKey.TOE_REAR, -0.25,
                        "FAST ATTACK: stabilisierende Spur vorsichtig reduzieren");
                add(adjustments, ParameterKey.SPRING_REAR, 0.25,
                        "FAST ATTACK: direktere Hinterachsreaktion");
            }
            case FAST_STABLE -> {
                add(adjustments, ParameterKey.BRAKE_BIAS, 0.50,
                        "FAST STABLE: zwei verifizierte Schritte mehr Bremsstabilität");
                add(adjustments, ParameterKey.REAR_WING, 0.50,
                        "FAST STABLE: zwei verifizierte Schritte mehr Heckstabilität");
                add(adjustments, ParameterKey.TRACTION_CONTROL, 0.25,
                        "FAST STABLE: ein verifizierter Schritt mehr Traktionsreserve");
                add(adjustments, ParameterKey.ANTI_ROLL_BAR_REAR, -0.50,
                        "FAST STABLE: Hinterachse mechanisch entlasten");
                add(adjustments, ParameterKey.TOE_REAR, 0.50,
                        "FAST STABLE: mehr stabilisierende Hinterachsspur");
                add(adjustments, ParameterKey.SPRING_REAR, -0.50,
                        "FAST STABLE: mehr Grip auf Unebenheiten und Curbs");
            }
            case FAST_SAFE -> {
                add(adjustments, ParameterKey.BRAKE_BIAS, 0.75,
                        "FAST SAFE: drei verifizierte Schritte mehr Bremsstabilität");
                add(adjustments, ParameterKey.REAR_WING, 0.75,
                        "FAST SAFE: drei verifizierte Schritte mehr Heckstabilität");
                add(adjustments, ParameterKey.TRACTION_CONTROL, 0.50,
                        "FAST SAFE: zwei verifizierte Schritte mehr Traktionsreserve");
                add(adjustments, ParameterKey.ABS, 0.25,
                        "FAST SAFE: einen verifizierten Schritt mehr Blockierreserve");
                add(adjustments, ParameterKey.ANTI_ROLL_BAR_REAR, -0.75,
                        "FAST SAFE: maximale mechanische Hinterachsruhe im sicheren Bereich");
                add(adjustments, ParameterKey.TOE_REAR, 0.75,
                        "FAST SAFE: maximale stabilisierende Hinterachsspur im sicheren Bereich");
                add(adjustments, ParameterKey.SPRING_REAR, -0.75,
                        "FAST SAFE: maximale mechanische Traktionsreserve im sicheren Bereich");
            }
            case FAST_LONG_RUN -> {
                add(adjustments, ParameterKey.BRAKE_BIAS, 0.25,
                        "FAST LONG RUN: konstante Bremsstabilität");
                add(adjustments, ParameterKey.REAR_WING, 0.25,
                        "FAST LONG RUN: stabile Aero-Balance über den Stint");
                add(adjustments, ParameterKey.TRACTION_CONTROL, 0.25,
                        "FAST LONG RUN: Traktionsreserve für konstante Ausgänge");
                add(adjustments, ParameterKey.ABS, 0.25,
                        "FAST LONG RUN: Blockierreserve über den Stint");
                add(adjustments, ParameterKey.ANTI_ROLL_BAR_REAR, -0.25,
                        "FAST LONG RUN: Hinterreifen mechanisch entlasten");
                add(adjustments, ParameterKey.TOE_REAR, 0.25,
                        "FAST LONG RUN: berechenbares Heck über den Stint");
                add(adjustments, ParameterKey.SPRING_REAR, -0.25,
                        "FAST LONG RUN: Traktion und Reifenruhe");
            }
        }
        return new FineTunePlan(origin, Collections.emptyList(), adjustments);
    }

    private static void add(List<ParameterAdjustment> target, ParameterKey key, double delta, String reason) {
        target.add(new ParameterAdjustment(key, delta, reason));
    }
}
