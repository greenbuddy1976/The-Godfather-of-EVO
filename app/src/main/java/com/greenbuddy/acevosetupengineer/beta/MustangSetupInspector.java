package com.greenbuddy.acevosetupengineer.beta;

import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Independent decoder/inspector; it does not call MustangFieldLocator or the patcher. */
public final class MustangSetupInspector {
    public static final String MUSTANG_PRESET_ID =
            "ks_ford_mustang_gt3_preset_msggt3_mech_1_preset_msggt3_visual_1";
    private static final Map<MustangField, MustangRange> RANGES = MustangRange.all();

    public MustangImportInspection inspect(byte[] binary) {
        try {
            if (binary == null || binary.length < 32 || binary.length > 1024 * 1024) {
                return invalid("Dateigröße ist keine plausible .carsetup.");
            }
            ProtoWire.Message top = ProtoWire.parse(binary, 0, binary.length);
            ProtoWire.Field preset = top.one(9, 2);
            byte[] expectedPreset = MUSTANG_PRESET_ID.getBytes(StandardCharsets.US_ASCII);
            if (!top.payloadEquals(preset, expectedPreset)) {
                return invalid("Falsches Fahrzeug: Mustang-Preset-ID in Feld 9 fehlt.");
            }
            if (top.varintValue(top.one(10, 0)) != 1L) {
                return invalid("Mustang-Preset-Versionmarker in Feld 10 fehlt.");
            }
            EnumMap<MustangField, Integer> offsets = decodeOffsets(binary);
            EnumMap<MustangField, Float> values = new EnumMap<>(MustangField.class);
            for (MustangField field : MustangField.values()) {
                Integer offset = offsets.get(field);
                if (offset == null) return invalid("Pflichtfeld fehlt: " + field.key());
                float value = ProtoWire.fixed32Float(binary, offset);
                if (!Float.isFinite(value)) return invalid("Ungültiger Zahlenwert: " + field.key());
                MustangRange range = RANGES.get(field);
                if (range != null && !range.acceptsBounds(value)) {
                    return invalid("Wert außerhalb Mustang-v0.8.1-Grenze: " + field.key());
                }
                values.put(field, value);
            }
            return new MustangImportInspection(true,
                    "Mustang-Basis strukturell erkannt; Spielversion und Spielannahme bleiben unbestätigt.",
                    values);
        } catch (RuntimeException error) {
            return invalid("Protobuf-Struktur ungültig oder unvollständig.");
        }
    }

    private static MustangImportInspection invalid(String message) {
        return new MustangImportInspection(false, message, new EnumMap<>(MustangField.class));
    }

    private static EnumMap<MustangField, Integer> decodeOffsets(byte[] bytes) {
        ProtoWire.Message top = ProtoWire.parse(bytes, 0, bytes.length);
        EnumMap<MustangField, Integer> map = new EnumMap<>(MustangField.class);
        ProtoWire.Message vehicle = child(top, 1);
        ProtoWire.Field packedArbs = vehicle.one(1, 2);
        if (packedArbs.length != 8) throw new IllegalArgumentException("ARB count");
        map.put(MustangField.FRONT_ARB, packedArbs.payload);
        map.put(MustangField.REAR_ARB, packedArbs.payload + 4);
        map.put(MustangField.STEER_RATIO, vehicle.one(2, 5).payload);
        ProtoWire.Message brake = child(vehicle, 3);
        map.put(MustangField.BRAKE_BIAS, brake.one(1, 5).payload);
        map.put(MustangField.BRAKE_PRESSURE, brake.one(2, 5).payload);
        ProtoWire.Message differential = child(vehicle, 4);
        map.put(MustangField.DIFF_POWER, differential.one(1, 5).payload);
        map.put(MustangField.DIFF_COAST, differential.one(2, 5).payload);
        map.put(MustangField.DIFF_PRELOAD, differential.one(3, 5).payload);

        MustangField[] spring = {MustangField.SPRING_FL, MustangField.SPRING_FR,
                MustangField.SPRING_RL, MustangField.SPRING_RR};
        decodeRepeated(top, 2, new int[]{1}, new MustangField[][]{spring}, map);
        MustangField[] bump = {MustangField.BUMP_FL, MustangField.BUMP_FR,
                MustangField.BUMP_RL, MustangField.BUMP_RR};
        MustangField[] rebound = {MustangField.REBOUND_FL, MustangField.REBOUND_FR,
                MustangField.REBOUND_RL, MustangField.REBOUND_RR};
        decodeRepeated(top, 3, new int[]{1, 3}, new MustangField[][]{bump, rebound}, map);
        MustangField[] pressure = {MustangField.TYRE_PRESSURE_FL, MustangField.TYRE_PRESSURE_FR,
                MustangField.TYRE_PRESSURE_RL, MustangField.TYRE_PRESSURE_RR};
        MustangField[] camber = {MustangField.CAMBER_FL, MustangField.CAMBER_FR,
                MustangField.CAMBER_RL, MustangField.CAMBER_RR};
        MustangField[] toe = {MustangField.TOE_FL, MustangField.TOE_FR,
                MustangField.TOE_RL, MustangField.TOE_RR};
        decodeRepeated(top, 4, new int[]{1, 2, 3},
                new MustangField[][]{pressure, camber, toe}, map);

        ProtoWire.Message assists = child(top, 5);
        map.put(MustangField.TC, assists.one(1, 5).payload);
        map.put(MustangField.TC2, assists.one(2, 5).payload);
        map.put(MustangField.ABS, assists.one(3, 5).payload);
        ProtoWire.Message platform = child(top, 6);
        map.put(MustangField.FRONT_RIDE_HEIGHT, platform.one(2, 5).payload);
        map.put(MustangField.REAR_RIDE_HEIGHT, platform.one(3, 5).payload);
        map.put(MustangField.REAR_WING, platform.one(5, 5).payload);
        ProtoWire.Message tank = child(top, 7);
        map.put(MustangField.FUEL, tank.one(1, 5).payload);
        if (map.size() != MustangField.values().length) throw new IllegalArgumentException("field count");
        return map;
    }

    private static ProtoWire.Message child(ProtoWire.Message parent, int number) {
        return parent.message(parent.one(number, 2));
    }

    private static void decodeRepeated(ProtoWire.Message top, int fieldNumber,
            int[] nestedNumbers, MustangField[][] targets, EnumMap<MustangField, Integer> map) {
        List<ProtoWire.Field> fields = top.fields(fieldNumber);
        if (fields.size() != 4) throw new IllegalArgumentException("wheel count");
        for (int wheel = 0; wheel < 4; wheel++) {
            ProtoWire.Field outer = fields.get(wheel);
            if (outer.wireType != 2) throw new IllegalArgumentException("message type");
            ProtoWire.Message message = top.message(outer);
            for (int value = 0; value < nestedNumbers.length; value++) {
                map.put(targets[value][wheel], message.one(nestedNumbers[value], 5).payload);
            }
        }
    }
}
