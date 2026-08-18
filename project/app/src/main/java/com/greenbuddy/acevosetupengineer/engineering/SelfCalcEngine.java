package com.greenbuddy.acevosetupengineer.engineering;

import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Creates an auditable range-derived engineering model. It never reads a setup
 * baseline or a carrier value. Every generated number is an explicit fraction
 * of the selected car's verified min/max/step definition.
 */
public final class SelfCalcEngine {
    public EngineeringSetup calculate(
            SetupRequest request,
            EngineeringProfile vehicle,
            TrackProfile track,
            boolean liveUnverified) {
        vehicle.requireRangeModelUsableFor(request.vehicle.id, request.gameVersion);
        track.requireUsableFor(request.layout.id, request.gameVersion);

        Map<ParameterKey, Double> values = new EnumMap<>(ParameterKey.class);
        List<String> audit = new ArrayList<>();
        audit.add("ENGINEERING MODEL: keine importierten Setup-Werte, kein Donor, kein Fallback");
        audit.add("Fahrzeugprofil-Fingerprint: " + vehicle.profileFingerprint);
        audit.add("Layoutprofil-Fingerprint: " + track.profileFingerprint);
        audit.add("Layoutprofil: " + track.layoutId + " / " + track.verificationSource);
        if (liveUnverified) audit.add("LIVE-UNVERIFIED: mindestens eine LIVE-Quelle war technisch nicht prüfbar");

        for (ParameterDefinition definition : vehicle.parameters.values()) {
            double fraction = fractionFor(definition.key, request, track);
            double raw = definition.minimum + fraction * (definition.maximum - definition.minimum);
            double value = definition.clampAndRound(raw);
            if (!definition.contains(value)) {
                throw new SetupValidationException("SELF CALC außerhalb des Bereichs: " + definition.key.displayName);
            }
            values.put(definition.key, value);
            audit.add(definition.key.displayName + " = " + value + " " + definition.unit
                    + " (Bereichsanteil " + round(fraction) + "; Track/Stil-Regel, geprüft)");
        }
        if (values.isEmpty()) throw new SetupValidationException("Fahrzeugprofil enthält keine Parameter");
        return new EngineeringSetup(EngineeringSetup.Label.ENGINEERING_MODEL, values, audit);
    }

    private static double fractionFor(ParameterKey key, SetupRequest request, TrackProfile track) {
        double s = request.mode.engineeringFactor;
        double speed = track.speedDemand;
        double traction = track.tractionDemand;
        double braking = track.brakingDemand;
        double longTrack = clamp((track.lengthMeters - 3_000.0) / 18_000.0, 0, 1);
        double value = switch (key) {
            case TYRE_PRESSURE_FRONT -> 0.30 - 0.07 * speed + 0.03 * braking;
            case TYRE_PRESSURE_REAR -> 0.29 - 0.06 * speed + 0.04 * traction;
            case CAMBER_FRONT -> 0.34 - 0.08 * s - 0.05 * speed;
            case CAMBER_REAR -> 0.38 - 0.06 * s - 0.04 * speed;
            case TOE_FRONT -> 0.43 - 0.07 * s + 0.03 * speed;
            case TOE_REAR -> 0.62 - 0.06 * s + 0.07 * traction;
            case ABS -> 0.56 - 0.18 * s + 0.10 * braking;
            case TRACTION_CONTROL, TRACTION_CONTROL_2 -> 0.54 - 0.20 * s + 0.13 * traction;
            case ENGINE_MAP -> 0.72 + 0.16 * s;
            case BRAKE_BIAS -> 0.53 - 0.07 * s + 0.09 * braking;
            case BRAKE_PRESSURE -> 0.80 + 0.10 * s - 0.04 * braking;
            case FUEL -> 0.08 - 0.025 * s + 0.24 * longTrack;
            case DIFFERENTIAL_PRELOAD -> 0.40 + 0.07 * s - 0.06 * traction;
            case DIFFERENTIAL_POWER -> 0.44 + 0.08 * s - 0.09 * traction;
            case DIFFERENTIAL_COAST -> 0.46 - 0.08 * s + 0.09 * braking;
            case STEERING_RATIO -> 0.45 - 0.09 * s + 0.04 * speed;
            case ANTI_ROLL_BAR_FRONT -> 0.43 + 0.06 * speed - 0.05 * traction;
            case ANTI_ROLL_BAR_REAR -> 0.39 + 0.08 * s + 0.04 * speed;
            case SPRING_FRONT -> 0.36 + 0.09 * speed - 0.06 * traction;
            case SPRING_REAR -> 0.34 + 0.07 * speed - 0.08 * traction + 0.04 * s;
            case RIDE_HEIGHT_FRONT -> 0.25 - 0.09 * speed - 0.03 * s;
            case RIDE_HEIGHT_REAR -> 0.29 - 0.08 * speed - 0.02 * s;
            case SLOW_BUMP_FRONT -> 0.36 + 0.06 * speed - 0.05 * braking;
            case SLOW_BUMP_REAR -> 0.34 + 0.05 * speed - 0.06 * traction;
            case SLOW_REBOUND_FRONT -> 0.42 + 0.06 * speed + 0.04 * braking;
            case SLOW_REBOUND_REAR -> 0.40 + 0.05 * speed + 0.05 * traction;
            case FAST_BUMP_FRONT, FAST_BUMP_REAR, FAST_REBOUND_FRONT, FAST_REBOUND_REAR ->
                    throw new SetupValidationException("Schnelle Dämpfer sind im EVO-Binärschema nicht verifiziert");
            case FRONT_AERO -> 0.52 - 0.17 * speed + 0.04 * s;
            case REAR_WING -> 0.57 - 0.19 * speed - 0.07 * s + 0.05 * traction;
        };
        // Hard plausibility guard: an engineering model never starts on an end stop.
        return clamp(value, 0.08, 0.92);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double round(double value) {
        return Math.rint(value * 1_000.0) / 1_000.0;
    }
}
