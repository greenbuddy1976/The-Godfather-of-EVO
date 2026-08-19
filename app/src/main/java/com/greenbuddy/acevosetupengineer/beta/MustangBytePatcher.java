package com.greenbuddy.acevosetupengineer.beta;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class MustangBytePatcher {
    private static final Map<MustangField, MustangRange> RANGES = MustangRange.all();
    /**
     * Narrow BETA allowlist. Fields with a known path but conflicting or absent click anchors
     * remain inspectable and byte-identical; they cannot accidentally become writable here.
     */
    private static final Set<MustangField> WRITABLE = EnumSet.of(
            MustangField.FRONT_ARB, MustangField.REAR_ARB,
            MustangField.BRAKE_BIAS, MustangField.BRAKE_PRESSURE,
            MustangField.DIFF_POWER, MustangField.DIFF_COAST,
            MustangField.CAMBER_FL, MustangField.CAMBER_FR,
            MustangField.CAMBER_RL, MustangField.CAMBER_RR,
            MustangField.TOE_FL, MustangField.TOE_FR,
            MustangField.TOE_RL, MustangField.TOE_RR,
            MustangField.TC, MustangField.TC2, MustangField.ABS,
            MustangField.FRONT_RIDE_HEIGHT, MustangField.REAR_RIDE_HEIGHT,
            MustangField.REAR_WING);

    static boolean isWritable(MustangField field) {
        return WRITABLE.contains(field);
    }

    byte[] patch(byte[] base, Map<MustangField, Float> changes) {
        EnumMap<MustangField, Integer> offsets = MustangFieldLocator.locate(base);
        byte[] output = base.clone();
        Set<Integer> permitted = new HashSet<>();
        for (Map.Entry<MustangField, Float> entry : changes.entrySet()) {
            if (!isWritable(entry.getKey())) {
                throw new IllegalArgumentException("Field is inspect-only in this BETA: "
                        + entry.getKey());
            }
            MustangRange range = RANGES.get(entry.getKey());
            if (range == null) throw new IllegalArgumentException("No verified range for " + entry.getKey());
            float value = range.clamp(entry.getValue());
            if (!range.acceptsBounds(value)) throw new IllegalArgumentException("Unsafe value");
            int offset = offsets.get(entry.getKey());
            ProtoWire.putFixed32Float(output, offset, value);
            for (int index = 0; index < 4; index++) permitted.add(offset + index);
        }
        if (!unchangedOutside(base, output, permitted)) {
            throw new IllegalStateException("Bytes outside fixed32 payload changed");
        }
        return output;
    }

    static boolean unchangedOutside(byte[] base, byte[] output, Set<Integer> permitted) {
        if (base.length != output.length) return false;
        for (int index = 0; index < base.length; index++) {
            if (!permitted.contains(index) && base[index] != output[index]) return false;
        }
        return true;
    }

    static Set<Integer> changedBytePositions(byte[] base, byte[] output) {
        Set<Integer> changed = new HashSet<>();
        if (base.length != output.length) return changed;
        for (int index = 0; index < base.length; index++) {
            if (base[index] != output[index]) changed.add(index);
        }
        return changed;
    }
}
