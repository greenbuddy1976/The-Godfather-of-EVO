package com.greenbuddy.acevosetupengineer.core;

import com.greenbuddy.acevosetupengineer.binary.CarSetupInspection;
import com.greenbuddy.acevosetupengineer.binary.CarSetupInspector;
import com.greenbuddy.acevosetupengineer.binary.CarSetupNumericCodec;
import com.greenbuddy.acevosetupengineer.engineering.EngineeringProfile;
import com.greenbuddy.acevosetupengineer.engineering.EngineeringSetup;
import com.greenbuddy.acevosetupengineer.engineering.ParameterDefinition;
import com.greenbuddy.acevosetupengineer.engineering.ParameterKey;
import com.greenbuddy.acevosetupengineer.engineering.SetupValidationException;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.util.Hashing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Uses a same-car file only as a protobuf structure carrier, never as a numeric baseline. */
public final class SelfCalcExportService {
    public static final class Result {
        public final EngineeringSetup setup;
        public final byte[] bytes;
        public final String carrierSha256;

        Result(EngineeringSetup setup, byte[] bytes, String carrierSha256) {
            this.setup = setup;
            this.bytes = bytes;
            this.carrierSha256 = carrierSha256;
        }
    }

    private final CarSetupNumericCodec codec = new CarSetupNumericCodec();

    public Result apply(
            SetupRequest request,
            byte[] carrier,
            EngineeringProfile profile,
            EngineeringSetup generated) {
        profile.requireRangeModelUsableFor(request.vehicle.id, request.gameVersion);
        if (generated.label != EngineeringSetup.Label.ENGINEERING_MODEL) {
            throw new SetupValidationException("SELF CALC erwartet die Kennzeichnung ENGINEERING MODEL");
        }
        CarSetupInspection inspection = CarSetupInspector.inspect(carrier);
        if (!inspection.structurallyValid) {
            throw new SetupValidationException("Strukturträger ungültig: " + inspection.failureReason);
        }
        if (!inspection.vehicleSignature.startsWith(request.vehicle.expectedSignaturePrefix)) {
            throw new SetupValidationException("Strukturträger gehört nicht zum exakt gewählten Fahrzeug");
        }

        // Values are decoded only to prove field presence. They are never passed to the model.
        Map<ParameterKey, Double> carrierFields = codec.decodeKnown(carrier);
        for (ParameterKey key : profile.parameters.keySet()) {
            if (!carrierFields.containsKey(key)) {
                throw new SetupValidationException("Verstellbares Feld fehlt im Strukturträger: "
                        + key.displayName);
            }
            if (!generated.values.containsKey(key)) {
                throw new SetupValidationException("SELF CALC hat kein neues Ergebnis für: " + key.displayName);
            }
        }

        byte[] patched = codec.patchKnownAbsoluteAxles(carrier, generated.values);
        CarSetupInspection roundtrip = CarSetupInspector.inspect(patched);
        if (!roundtrip.structurallyValid || !inspection.vehicleSignature.equals(roundtrip.vehicleSignature)) {
            throw new SetupValidationException("SELF CALC Patch/Decode-Identitätsprüfung fehlgeschlagen");
        }
        codec.validateKnownRanges(patched, profile.parameters);
        Map<ParameterKey, Double> decodedAgain = codec.decodeKnown(patched);
        for (Map.Entry<ParameterKey, ParameterDefinition> entry : profile.parameters.entrySet()) {
            Double expected = generated.values.get(entry.getKey());
            Double actual = decodedAgain.get(entry.getKey());
            double tolerance = Math.max(0.002, entry.getValue().step * 0.002);
            if (actual == null || Math.abs(actual - expected) > tolerance) {
                throw new SetupValidationException("SELF CALC Roundtrip-Wert weicht ab: "
                        + entry.getKey().displayName);
            }
        }

        String carrierSha = Hashing.sha256(carrier);
        List<String> audit = new ArrayList<>(generated.audit);
        audit.add("Strukturträger SHA-256: " + carrierSha);
        audit.add("Strukturträger-Nutzdatenregel: Zahlen verworfen; alle verstellbaren Profilfelder neu geschrieben");
        audit.add("SELF CALC Roundtrip: Signatur, Bereiche, Felder und Schreibwerte verifiziert");
        return new Result(new EngineeringSetup(EngineeringSetup.Label.ENGINEERING_MODEL,
                generated.values, audit), patched, carrierSha);
    }
}
