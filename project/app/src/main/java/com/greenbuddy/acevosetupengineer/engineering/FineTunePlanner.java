package com.greenbuddy.acevosetupengineer.engineering;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class FineTunePlanner {
    public FineTunePlan plan(FineTuneInterpretation interpretation, FineTunePlan.Origin origin) {
        Map<ParameterKey, Accumulated> combined = new EnumMap<>(ParameterKey.class);
        for (HandlingIssue issue : interpretation.issues) {
            for (ParameterAdjustment adjustment : rules(issue)) {
                combined.computeIfAbsent(adjustment.parameter, ignored -> new Accumulated())
                        .add(adjustment.normalizedDelta, adjustment.reason);
            }
        }

        List<ParameterAdjustment> safe = new ArrayList<>();
        for (Map.Entry<ParameterKey, Accumulated> entry : combined.entrySet()) {
            double limited = Math.max(-1.0, Math.min(1.0, entry.getValue().delta));
            if (Math.abs(limited) >= 0.05) {
                safe.add(new ParameterAdjustment(entry.getKey(), limited, entry.getValue().reason()));
            }
        }
        return new FineTunePlan(origin, interpretation.issues, safe);
    }

    private static List<ParameterAdjustment> rules(HandlingIssue issue) {
        List<ParameterAdjustment> r = new ArrayList<>();
        switch (issue) {
            case REAR_NERVOUS -> {
                add(r, ParameterKey.ANTI_ROLL_BAR_REAR, -0.35, "Heck mechanisch beruhigen");
                add(r, ParameterKey.TOE_REAR, 0.25, "mehr stabilisierende Vorspur hinten");
                add(r, ParameterKey.REAR_WING, 0.25, "mehr aerodynamische Heckstabilität");
                add(r, ParameterKey.SLOW_REBOUND_REAR, -0.20, "Lastwechsel am Heck entschärfen");
            }
            case REAR_SLUGGISH -> {
                add(r, ParameterKey.ANTI_ROLL_BAR_REAR, 0.30, "mehr Rotation über die Hinterachse");
                add(r, ParameterKey.TOE_REAR, -0.20, "stabilisierende Vorspur vorsichtig reduzieren");
                add(r, ParameterKey.REAR_WING, -0.15, "Aero-Balance vorsichtig nach vorn verschieben");
                add(r, ParameterKey.DIFFERENTIAL_COAST, -0.15, "Rotation beim Lupfen verbessern");
            }
            case ENTRY_UNDERSTEER -> {
                add(r, ParameterKey.BRAKE_BIAS, -0.20, "Bremsbalance vorsichtig nach hinten");
                add(r, ParameterKey.ANTI_ROLL_BAR_FRONT, -0.25, "mehr mechanischer Vorderachsgrip");
                add(r, ParameterKey.TOE_FRONT, -0.15, "mehr Vorspur nach außen für direkteres Einlenken");
                add(r, ParameterKey.DIFFERENTIAL_COAST, -0.15, "Einlenkrotation verbessern");
            }
            case MID_UNDERSTEER -> {
                add(r, ParameterKey.ANTI_ROLL_BAR_FRONT, -0.30, "Vorderachsgrip in der Kurvenmitte erhöhen");
                add(r, ParameterKey.ANTI_ROLL_BAR_REAR, 0.20, "Balance vorsichtig in Richtung Rotation");
                add(r, ParameterKey.FRONT_AERO, 0.20, "Aero-Balance nach vorn");
            }
            case EXIT_UNDERSTEER -> {
                add(r, ParameterKey.DIFFERENTIAL_POWER, -0.25, "Sperrwirkung unter Last reduzieren");
                add(r, ParameterKey.ANTI_ROLL_BAR_FRONT, -0.20, "Vorderachsgrip beim Herausbeschleunigen");
                add(r, ParameterKey.SLOW_REBOUND_FRONT, -0.15, "Vorderachse unter Last weniger entlasten");
            }
            case ENTRY_OVERSTEER -> {
                add(r, ParameterKey.BRAKE_BIAS, 0.25, "Bremsbalance nach vorn stabilisieren");
                add(r, ParameterKey.ANTI_ROLL_BAR_REAR, -0.25, "Hinterachsgrip beim Einlenken erhöhen");
                add(r, ParameterKey.TOE_REAR, 0.20, "Heck beim Einlenken stabilisieren");
                add(r, ParameterKey.DIFFERENTIAL_COAST, 0.20, "mehr Stabilität im Schubbetrieb");
            }
            case MID_OVERSTEER -> {
                add(r, ParameterKey.ANTI_ROLL_BAR_REAR, -0.30, "Hinterachsgrip in der Kurvenmitte erhöhen");
                add(r, ParameterKey.REAR_WING, 0.25, "Aero-Heckstabilität erhöhen");
                add(r, ParameterKey.SPRING_REAR, -0.15, "Hinterachse mechanisch entlasten");
            }
            case EXIT_OVERSTEER -> {
                add(r, ParameterKey.TRACTION_CONTROL, 0.25, "Schlupf am Kurvenausgang begrenzen");
                add(r, ParameterKey.DIFFERENTIAL_POWER, -0.25, "aggressive Sperrwirkung reduzieren");
                add(r, ParameterKey.TOE_REAR, 0.20, "Heck unter Last stabilisieren");
                add(r, ParameterKey.REAR_WING, 0.15, "zusätzliche Hochgeschwindigkeitsstabilität");
            }
            case BRAKING_INSTABILITY -> {
                add(r, ParameterKey.BRAKE_BIAS, 0.30, "Bremsstabilität durch mehr Vorderachsanteil");
                add(r, ParameterKey.ABS, 0.15, "Blockierneigung begrenzen");
                add(r, ParameterKey.SLOW_REBOUND_REAR, -0.20, "Hinterachse beim Bremsen weniger abrupt entlasten");
                add(r, ParameterKey.TOE_REAR, 0.15, "Spurstabilität hinten erhöhen");
            }
            case POOR_TRACTION -> {
                add(r, ParameterKey.TRACTION_CONTROL, 0.25, "Antriebsschlupf reduzieren");
                add(r, ParameterKey.DIFFERENTIAL_POWER, -0.20, "Lastsperre entschärfen");
                add(r, ParameterKey.SPRING_REAR, -0.20, "mechanischen Hinterachsgrip erhöhen");
                add(r, ParameterKey.SLOW_BUMP_REAR, -0.15, "Traktion über Lastaufbau verbessern");
            }
            case BUMP_INSTABILITY, CURB_INSTABILITY -> {
                add(r, ParameterKey.FAST_BUMP_FRONT, -0.25, "Schläge an der Vorderachse besser aufnehmen");
                add(r, ParameterKey.FAST_BUMP_REAR, -0.25, "Schläge an der Hinterachse besser aufnehmen");
                add(r, ParameterKey.FAST_REBOUND_FRONT, -0.20, "Vorderradkontakt über Unebenheiten verbessern");
                add(r, ParameterKey.FAST_REBOUND_REAR, -0.20, "Hinterradkontakt über Unebenheiten verbessern");
                add(r, ParameterKey.RIDE_HEIGHT_FRONT, 0.15, "zusätzliche Bodenfreiheit");
                add(r, ParameterKey.RIDE_HEIGHT_REAR, 0.15, "zusätzliche Bodenfreiheit");
            }
            case FRONT_TYRE_OVERHEAT -> {
                add(r, ParameterKey.CAMBER_FRONT, 0.15, "übermäßig negativen Sturz vorsichtig reduzieren");
            }
            case REAR_TYRE_OVERHEAT -> {
                add(r, ParameterKey.CAMBER_REAR, 0.15, "übermäßig negativen Sturz vorsichtig reduzieren");
                add(r, ParameterKey.TRACTION_CONTROL, 0.10, "Schlupfhitze begrenzen");
            }
            case STEERING_TOO_SHARP -> {
                add(r, ParameterKey.TOE_FRONT, 0.20, "Vorspur nach außen reduzieren und Einlenken beruhigen");
                add(r, ParameterKey.ANTI_ROLL_BAR_FRONT, 0.10, "Vorderachsreaktion leicht dämpfen");
            }
            case STEERING_TOO_SLOW -> {
                add(r, ParameterKey.TOE_FRONT, -0.20, "mehr Vorspur nach außen für direkteres Einlenken");
                add(r, ParameterKey.ANTI_ROLL_BAR_FRONT, -0.10, "Vorderachsgrip beim Einlenken erhöhen");
            }
            case DIRECT_REAR_WING_MORE ->
                    add(r, ParameterKey.REAR_WING, 0.20, "direkter Fahrerwunsch: mehr Heckflügel");
            case DIRECT_REAR_WING_LESS ->
                    add(r, ParameterKey.REAR_WING, -0.20, "direkter Fahrerwunsch: weniger Heckflügel");
        }
        return r;
    }

    private static void add(List<ParameterAdjustment> list, ParameterKey key, double delta, String reason) {
        list.add(new ParameterAdjustment(key, delta, reason));
    }

    private static final class Accumulated {
        double delta;
        final List<String> reasons = new ArrayList<>();

        void add(double amount, String reason) {
            delta += amount;
            reasons.add(reason);
        }

        String reason() {
            return String.join("; ", reasons);
        }
    }
}
