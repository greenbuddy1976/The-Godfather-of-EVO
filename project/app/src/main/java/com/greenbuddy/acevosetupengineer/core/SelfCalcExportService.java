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
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Uses a same-car file only as a protobuf structure carrier, never as a numeric baseline. */
public final class SelfCalcExportService {
    private static final int MIN_WRITABLE_PARAMETERS = 4;
    private static final Set<ParameterKey> BALANCE_PARAMETERS = EnumSet.of(
            ParameterKey.BRAKE_BIAS,
            ParameterKey.DIFFERENTIAL_POWER,
            ParameterKey.DIFFERENTIAL_COAST,
            ParameterKey.ANTI_ROLL_BAR_REAR,
            ParameterKey.SPRING_REAR,
            ParameterKey.TOE_REAR,
            ParameterKey.TRACTION_CONTROL,
            ParameterKey.RIDE_HEIGHT_REAR,
            ParameterKey.REAR_WING);

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
        // A protobuf may legitimately omit controls that this vehicle does not serialize
        // (for example fixed front aero). Such controls are left untouched instead of
        // blocking the whole otherwise valid same-car setup.
        Map<ParameterKey, Double> carrierFields = codec.decodeKnown(carrier);
        Map<ParameterKey, ParameterDefinition> presentDefinitions = new EnumMap<>(ParameterKey.class);
        Map<ParameterKey, Double> writableValues = new EnumMap<>(ParameterKey.class);
        List<String> audit = new ArrayList<>(generated.audit);
        for (Map.Entry<ParameterKey, ParameterDefinition> entry : profile.parameters.entrySet()) {
            ParameterKey key = entry.getKey();
            if (!carrierFields.containsKey(key)) {
                audit.add("Nicht verändert: " + key.displayName
                        + " – dieses Same-Car-Binärformat serialisiert das Feld nicht");
                continue;
            }
            if (!generated.values.containsKey(key)) {
                throw new SetupValidationException("SELF CALC hat kein neues Ergebnis für: " + key.displayName);
            }
            presentDefinitions.put(key, entry.getValue());
            writableValues.put(key, generated.values.get(key));
        }
        if (writableValues.size() < MIN_WRITABLE_PARAMETERS
                || !writableValues.containsKey(ParameterKey.TYRE_PRESSURE_FRONT)
                || !writableValues.containsKey(ParameterKey.TYRE_PRESSURE_REAR)) {
            throw new SetupValidationException(
                    "Same-Car-Struktur besitzt zu wenige sicher schreibbare Fahrwerksfelder");
        }
        boolean hasBalanceParameter = BALANCE_PARAMETERS.stream().anyMatch(writableValues::containsKey);
        if (!hasBalanceParameter) {
            throw new SetupValidationException(
                    "Same-Car-Struktur besitzt keinen sicher schreibbaren Balance-Parameter");
        }

        byte[] patched = codec.patchKnownAbsoluteAxles(carrier, writableValues);
        CarSetupInspection roundtrip = CarSetupInspector.inspect(patched);
        if (!roundtrip.structurallyValid || !inspection.vehicleSignature.equals(roundtrip.vehicleSignature)) {
            throw new SetupValidationException("SELF CALC Patch/Decode-Identitätsprüfung fehlgeschlagen");
        }
        codec.validateKnownRanges(patched, presentDefinitions);
        Map<ParameterKey, Double> decodedAgain = codec.decodeKnown(patched);
        for (Map.Entry<ParameterKey, ParameterDefinition> entry : presentDefinitions.entrySet()) {
            Double expected = writableValues.get(entry.getKey());
            Double actual = decodedAgain.get(entry.getKey());
            double tolerance = Math.max(0.002, entry.getValue().step * 0.002);
            if (actual == null || Math.abs(actual - expected) > tolerance) {
                throw new SetupValidationException("SELF CALC Roundtrip-Wert weicht ab: "
                        + entry.getKey().displayName);
            }
        }

        String carrierSha = Hashing.sha256(carrier);
        audit.add("Strukturträger SHA-256: " + carrierSha);
        audit.add("Strukturträger-Nutzdatenregel: Zahlen verworfen; " + writableValues.size()
                + " vorhandene verstellbare Profilfelder neu geschrieben");
        audit.add("SELF CALC Roundtrip: Signatur, Bereiche, Felder und Schreibwerte verifiziert");
        return new Result(new EngineeringSetup(EngineeringSetup.Label.ENGINEERING_MODEL,
                writableValues, audit), patched, carrierSha);
    }
}
