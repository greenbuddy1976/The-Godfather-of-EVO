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
        audit.add("Fahrprofil: " + request.mode.buttonLabel
                + " / Pace=" + request.mode.paceFactor
                + " / Stabilität=" + request.mode.stabilityFactor
                + " / Long-Run=" + request.mode.enduranceFactor);
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
        EngineeringSetup calculated = new EngineeringSetup(
                EngineeringSetup.Label.ENGINEERING_MODEL, values, audit);
        return new VehicleSetupPolicy().apply(request, vehicle, calculated);
    }

    private static double fractionFor(ParameterKey key, SetupRequest request, TrackProfile track) {
        double pace = request.mode.paceFactor;
        double stability = request.mode.stabilityFactor;
        double endurance = request.mode.enduranceFactor;
        double speed = track.speedDemand;
        double traction = track.tractionDemand;
        double braking = track.brakingDemand;
        double longTrack = clamp((track.lengthMeters - 3_000.0) / 18_000.0, 0, 1);
        double value = switch (key) {
            case TYRE_PRESSURE_FRONT ->
                    0.34 - 0.05 * speed + 0.02 * braking - 0.04 * endurance;
            case TYRE_PRESSURE_REAR ->
                    0.33 - 0.04 * speed + 0.03 * traction - 0.04 * endurance;
            case CAMBER_FRONT -> 0.44 - 0.14 * pace + 0.10 * endurance - 0.04 * speed;
            case CAMBER_REAR -> 0.48 - 0.12 * pace + 0.12 * endurance
                    - 0.03 * speed + 0.03 * stability;
            case TOE_FRONT -> 0.45 - 0.12 * pace + 0.05 * stability + 0.02 * speed;
            case TOE_REAR -> 0.50 + 0.15 * stability - 0.05 * pace + 0.05 * traction;
            case ABS -> 0.26 + 0.28 * stability - 0.10 * pace + 0.12 * braking;
            case TRACTION_CONTROL, TRACTION_CONTROL_2 ->
                    0.12 + 0.42 * stability - 0.14 * pace + 0.16 * traction;
            case ENGINE_MAP -> 0.70 + 0.18 * pace - 0.08 * endurance;
            case BRAKE_BIAS -> 0.45 + 0.14 * stability - 0.06 * pace + 0.08 * braking;
            case BRAKE_PRESSURE -> 0.76 + 0.08 * pace - 0.03 * braking;
            case FUEL -> 0.08 + 0.12 * endurance + 0.25 * longTrack;
            case DIFFERENTIAL_PRELOAD ->
                    0.38 + 0.08 * pace + 0.06 * stability - 0.08 * traction;
            case DIFFERENTIAL_POWER ->
                    0.36 + 0.08 * pace - 0.14 * stability - 0.08 * traction;
            case DIFFERENTIAL_COAST ->
                    0.44 - 0.08 * pace + 0.14 * stability + 0.08 * braking;
            case STEERING_RATIO -> 0.42 - 0.10 * pace + 0.10 * stability + 0.03 * speed;
            case ANTI_ROLL_BAR_FRONT ->
                    0.38 + 0.08 * speed - 0.05 * traction + 0.04 * stability;
            case ANTI_ROLL_BAR_REAR ->
                    0.36 + 0.08 * pace - 0.16 * stability + 0.04 * speed - 0.04 * traction;
            case SPRING_FRONT -> 0.33 + 0.08 * speed - 0.04 * traction + 0.02 * pace;
            case SPRING_REAR -> 0.31 + 0.06 * speed - 0.10 * traction
                    + 0.04 * pace - 0.08 * stability;
            case RIDE_HEIGHT_FRONT ->
                    0.25 - 0.08 * speed - 0.03 * pace + 0.06 * stability;
            case RIDE_HEIGHT_REAR ->
                    0.28 - 0.07 * speed - 0.02 * pace + 0.08 * stability;
            case SLOW_BUMP_FRONT ->
                    0.34 + 0.05 * speed - 0.04 * braking + 0.02 * stability;
            case SLOW_BUMP_REAR ->
                    0.31 + 0.04 * speed - 0.07 * traction - 0.06 * stability;
            case SLOW_REBOUND_FRONT ->
                    0.41 + 0.05 * speed + 0.03 * braking + 0.02 * stability;
            case SLOW_REBOUND_REAR ->
                    0.38 + 0.04 * speed + 0.04 * traction - 0.06 * stability;
            case FAST_BUMP_FRONT, FAST_BUMP_REAR, FAST_REBOUND_FRONT, FAST_REBOUND_REAR ->
                    throw new SetupValidationException("Schnelle Dämpfer sind im EVO-Binärschema nicht verifiziert");
            case FRONT_AERO -> 0.48 - 0.14 * speed + 0.03 * pace + 0.02 * stability;
            case REAR_WING -> 0.45 - 0.16 * speed - 0.05 * pace
                    + 0.22 * stability + 0.05 * traction;
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
