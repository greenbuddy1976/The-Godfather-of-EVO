package com.greenbuddy.acevosetupengineer.core;

import com.greenbuddy.acevosetupengineer.binary.CarSetupInspection;
import com.greenbuddy.acevosetupengineer.binary.CarSetupInspector;
import com.greenbuddy.acevosetupengineer.binary.CarSetupNumericCodec;
import com.greenbuddy.acevosetupengineer.engineering.EngineeringProfile;
import com.greenbuddy.acevosetupengineer.engineering.EngineeringSetup;
import com.greenbuddy.acevosetupengineer.engineering.FineTuneEngine;
import com.greenbuddy.acevosetupengineer.engineering.FineTunePlan;
import com.greenbuddy.acevosetupengineer.engineering.ParameterKey;
import com.greenbuddy.acevosetupengineer.engineering.ParameterDefinition;
import com.greenbuddy.acevosetupengineer.engineering.SetupValidationException;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.model.VerifiedExact;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ExactFineTuneService {
    public static final class Result {
        public final EngineeringSetup setup;
        public final byte[] bytes;

        Result(EngineeringSetup setup, byte[] bytes) {
            this.setup = setup;
            this.bytes = bytes;
        }
    }

    private final CarSetupNumericCodec codec = new CarSetupNumericCodec();
    private final FineTuneEngine fineTuneEngine = new FineTuneEngine();

    public Result apply(
            SetupRequest request,
            VerifiedExact exact,
            EngineeringProfile profile,
            FineTunePlan plan) {
        profile.requireFineTuneUsableFor(request.vehicle.id, request.gameVersion);
        Map<ParameterKey, Double> decoded = codec.decodeKnown(exact.bytes);
        Map<ParameterKey, ParameterDefinition> presentDefinitions = new EnumMap<>(ParameterKey.class);
        for (Map.Entry<ParameterKey, ParameterDefinition> entry : profile.parameters.entrySet()) {
            if (decoded.containsKey(entry.getKey())) presentDefinitions.put(entry.getKey(), entry.getValue());
        }
        codec.validateKnownRanges(exact.bytes, presentDefinitions);
        List<String> baseAudit = new ArrayList<>();
        baseAudit.add("EXACT verifiziert: " + exact.candidate.provider + " / " + exact.sha256);
        EngineeringSetup base = new EngineeringSetup(EngineeringSetup.Label.EXACT_DERIVATIVE,
                decoded, baseAudit);
        EngineeringSetup tuned = fineTuneEngine.apply(base, profile, plan);
        byte[] patched = codec.patchKnown(exact.bytes, tuned.values);

        CarSetupInspection roundtrip = CarSetupInspector.inspect(patched);
        if (!roundtrip.structurallyValid) {
            throw new SetupValidationException("Patch/Decode-Roundtrip fehlgeschlagen: "
                    + roundtrip.failureReason);
        }
        if (!roundtrip.vehicleSignature.equals(exact.decodedVehicleSignature)) {
            throw new SetupValidationException("Fahrzeugidentität hat sich beim Patchen verändert");
        }
        codec.validateKnownRanges(patched, presentDefinitions);
        Map<ParameterKey, Double> decodedAgain = codec.decodeKnown(patched);
        for (Map.Entry<ParameterKey, Double> entry : tuned.values.entrySet()) {
            Double actual = decodedAgain.get(entry.getKey());
            if (actual == null) {
                throw new SetupValidationException("Roundtrip-Parameter fehlt: "
                        + entry.getKey().displayName);
            }
            if (Math.abs(actual - entry.getValue()) > 0.001) {
                throw new SetupValidationException("Roundtrip-Wert weicht ab: " + entry.getKey().displayName);
            }
            ParameterDefinition definition = profile.parameters.get(entry.getKey());
            if (definition != null && !definition.contains(actual)) {
                throw new SetupValidationException("Roundtrip-Wert außerhalb des verifizierten Bereichs: "
                        + entry.getKey().displayName);
            }
        }
        return new Result(tuned, patched);
    }
}
