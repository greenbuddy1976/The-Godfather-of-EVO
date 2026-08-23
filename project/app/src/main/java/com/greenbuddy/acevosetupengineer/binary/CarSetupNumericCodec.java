package com.greenbuddy.acevosetupengineer.binary;

import com.greenbuddy.acevosetupengineer.engineering.ParameterKey;
import com.greenbuddy.acevosetupengineer.engineering.ParameterDefinition;
import com.greenbuddy.acevosetupengineer.engineering.SetupValidationException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Conservative codec for fields whose meaning was independently identified in
 * the public format decoder. Unknown fields are never guessed or changed.
 */
public final class CarSetupNumericCodec {
    private static final int MAX_DEPTH = 8;

    public Map<ParameterKey, Double> decodeKnown(byte[] bytes) {
        Node root = parse(bytes, 0, bytes.length, 0);
        Map<ParameterKey, Double> values = new EnumMap<>(ParameterKey.class);

        Node general = message(root.first(1));
        if (general != null) {
            Value arb = general.first(1);
            if (arb != null && arb.wire == 2 && arb.length == 8) {
                values.put(ParameterKey.ANTI_ROLL_BAR_FRONT, (double) readFloat(bytes, arb.offset));
                values.put(ParameterKey.ANTI_ROLL_BAR_REAR, (double) readFloat(bytes, arb.offset + 4));
            }
            putFloat(values, ParameterKey.STEERING_RATIO, general.first(2), bytes);
            putFloat(values, ParameterKey.BRAKE_BIAS, nested(general, 3, 1), bytes);
            putFloat(values, ParameterKey.BRAKE_PRESSURE, nested(general, 3, 2), bytes);
            putFloat(values, ParameterKey.DIFFERENTIAL_POWER, nested(general, 4, 1), bytes);
            putFloat(values, ParameterKey.DIFFERENTIAL_COAST, nested(general, 4, 2), bytes);
            putFloat(values, ParameterKey.DIFFERENTIAL_PRELOAD, nested(general, 4, 3), bytes);
        }

        putAxleAverage(values, ParameterKey.SPRING_FRONT, ParameterKey.SPRING_REAR,
                root.all(2), 1, bytes);
        putAxleAverage(values, ParameterKey.SLOW_BUMP_FRONT, ParameterKey.SLOW_BUMP_REAR,
                root.all(3), 1, bytes);
        putAxleAverage(values, ParameterKey.SLOW_REBOUND_FRONT, ParameterKey.SLOW_REBOUND_REAR,
                root.all(3), 3, bytes);
        putAxleAverage(values, ParameterKey.TYRE_PRESSURE_FRONT, ParameterKey.TYRE_PRESSURE_REAR,
                root.all(4), 1, bytes);
        putAxleAverage(values, ParameterKey.CAMBER_FRONT, ParameterKey.CAMBER_REAR,
                root.all(4), 2, bytes);
        putAxleAverage(values, ParameterKey.TOE_FRONT, ParameterKey.TOE_REAR,
                root.all(4), 3, bytes);

        putFloat(values, ParameterKey.TRACTION_CONTROL, nested(root, 5, 1), bytes);
        putFloat(values, ParameterKey.TRACTION_CONTROL_2, nested(root, 5, 2), bytes);
        putFloat(values, ParameterKey.ABS, nested(root, 5, 3), bytes);
        putFloat(values, ParameterKey.ENGINE_MAP, nested(root, 5, 4), bytes);
        putFloat(values, ParameterKey.RIDE_HEIGHT_FRONT, nested(root, 6, 2), bytes);
        putFloat(values, ParameterKey.RIDE_HEIGHT_REAR, nested(root, 6, 3), bytes);
        putFloat(values, ParameterKey.FRONT_AERO, nested(root, 6, 4), bytes);
        putFloat(values, ParameterKey.REAR_WING, nested(root, 6, 5), bytes);
        putFloat(values, ParameterKey.FUEL, nested(root, 7, 1), bytes);
        return values;
    }

    public byte[] patchKnown(byte[] original, Map<ParameterKey, Double> changedValues) {
        return patchKnown(original, changedValues, false);
    }

    /**
     * SELF CALC variant: axle values are written identically left/right. This is
     * deliberately different from EXACT fine-tuning, which preserves a verified
     * file's left/right delta. No numeric value from a structure carrier survives
     * as an input to an adjustable axle parameter.
     */
    public byte[] patchKnownAbsoluteAxles(byte[] original, Map<ParameterKey, Double> changedValues) {
        return patchKnown(original, changedValues, true);
    }

    private byte[] patchKnown(
            byte[] original,
            Map<ParameterKey, Double> changedValues,
            boolean absoluteAxles) {
        byte[] patched = original.clone();
        Node root = parse(patched, 0, patched.length, 0);
        Node general = message(root.first(1));
        if (general != null) {
            Value arb = general.first(1);
            if (arb != null && arb.wire == 2 && arb.length == 8) {
                writeIfPresent(patched, arb.offset, changedValues.get(ParameterKey.ANTI_ROLL_BAR_FRONT));
                writeIfPresent(patched, arb.offset + 4, changedValues.get(ParameterKey.ANTI_ROLL_BAR_REAR));
            }
            patchFloat(patched, general.first(2), changedValues.get(ParameterKey.STEERING_RATIO));
            patchFloat(patched, nested(general, 3, 1), changedValues.get(ParameterKey.BRAKE_BIAS));
            patchFloat(patched, nested(general, 3, 2), changedValues.get(ParameterKey.BRAKE_PRESSURE));
            patchFloat(patched, nested(general, 4, 1), changedValues.get(ParameterKey.DIFFERENTIAL_POWER));
            patchFloat(patched, nested(general, 4, 2), changedValues.get(ParameterKey.DIFFERENTIAL_COAST));
            patchFloat(patched, nested(general, 4, 3), changedValues.get(ParameterKey.DIFFERENTIAL_PRELOAD));
        }

        patchAxle(patched, root.all(2), 1,
                changedValues.get(ParameterKey.SPRING_FRONT), changedValues.get(ParameterKey.SPRING_REAR), absoluteAxles);
        patchAxle(patched, root.all(3), 1,
                changedValues.get(ParameterKey.SLOW_BUMP_FRONT), changedValues.get(ParameterKey.SLOW_BUMP_REAR), absoluteAxles);
        patchAxle(patched, root.all(3), 3,
                changedValues.get(ParameterKey.SLOW_REBOUND_FRONT), changedValues.get(ParameterKey.SLOW_REBOUND_REAR), absoluteAxles);
        patchAxle(patched, root.all(4), 1,
                changedValues.get(ParameterKey.TYRE_PRESSURE_FRONT), changedValues.get(ParameterKey.TYRE_PRESSURE_REAR), absoluteAxles);
        patchAxle(patched, root.all(4), 2,
                changedValues.get(ParameterKey.CAMBER_FRONT), changedValues.get(ParameterKey.CAMBER_REAR), absoluteAxles);
        patchAxle(patched, root.all(4), 3,
                changedValues.get(ParameterKey.TOE_FRONT), changedValues.get(ParameterKey.TOE_REAR), absoluteAxles);

        patchFloat(patched, nested(root, 5, 1), changedValues.get(ParameterKey.TRACTION_CONTROL));
        patchFloat(patched, nested(root, 5, 2), changedValues.get(ParameterKey.TRACTION_CONTROL_2));
        patchFloat(patched, nested(root, 5, 3), changedValues.get(ParameterKey.ABS));
        patchFloat(patched, nested(root, 5, 4), changedValues.get(ParameterKey.ENGINE_MAP));
        patchFloat(patched, nested(root, 6, 2), changedValues.get(ParameterKey.RIDE_HEIGHT_FRONT));
        patchFloat(patched, nested(root, 6, 3), changedValues.get(ParameterKey.RIDE_HEIGHT_REAR));
        patchFloat(patched, nested(root, 6, 4), changedValues.get(ParameterKey.FRONT_AERO));
        patchFloat(patched, nested(root, 6, 5), changedValues.get(ParameterKey.REAR_WING));
        patchFloat(patched, nested(root, 7, 1), changedValues.get(ParameterKey.FUEL));
        return patched;
    }

    /** Validates every raw wheel/scalar value, not only the decoded axle averages. */
    public void validateKnownRanges(byte[] bytes, Map<ParameterKey, ParameterDefinition> definitions) {
        Node root = parse(bytes, 0, bytes.length, 0);
        Node general = message(root.first(1));
        if (general != null) {
            validateFloat(definitions, ParameterKey.STEERING_RATIO, general.first(2), bytes);
            validateFloat(definitions, ParameterKey.BRAKE_BIAS, nested(general, 3, 1), bytes);
            validateFloat(definitions, ParameterKey.BRAKE_PRESSURE, nested(general, 3, 2), bytes);
            validateFloat(definitions, ParameterKey.DIFFERENTIAL_POWER, nested(general, 4, 1), bytes);
            validateFloat(definitions, ParameterKey.DIFFERENTIAL_COAST, nested(general, 4, 2), bytes);
            validateFloat(definitions, ParameterKey.DIFFERENTIAL_PRELOAD, nested(general, 4, 3), bytes);
        }
        validateAxle(definitions, ParameterKey.SPRING_FRONT, ParameterKey.SPRING_REAR,
                root.all(2), 1, bytes);
        validateAxle(definitions, ParameterKey.SLOW_BUMP_FRONT, ParameterKey.SLOW_BUMP_REAR,
                root.all(3), 1, bytes);
        validateAxle(definitions, ParameterKey.SLOW_REBOUND_FRONT, ParameterKey.SLOW_REBOUND_REAR,
                root.all(3), 3, bytes);
        validateAxle(definitions, ParameterKey.TYRE_PRESSURE_FRONT, ParameterKey.TYRE_PRESSURE_REAR,
                root.all(4), 1, bytes);
        validateAxle(definitions, ParameterKey.CAMBER_FRONT, ParameterKey.CAMBER_REAR,
                root.all(4), 2, bytes);
        validateAxle(definitions, ParameterKey.TOE_FRONT, ParameterKey.TOE_REAR,
                root.all(4), 3, bytes);
        validateFloat(definitions, ParameterKey.TRACTION_CONTROL, nested(root, 5, 1), bytes);
        validateFloat(definitions, ParameterKey.TRACTION_CONTROL_2, nested(root, 5, 2), bytes);
        validateFloat(definitions, ParameterKey.ABS, nested(root, 5, 3), bytes);
        validateFloat(definitions, ParameterKey.ENGINE_MAP, nested(root, 5, 4), bytes);
        validateFloat(definitions, ParameterKey.RIDE_HEIGHT_FRONT, nested(root, 6, 2), bytes);
        validateFloat(definitions, ParameterKey.RIDE_HEIGHT_REAR, nested(root, 6, 3), bytes);
        validateFloat(definitions, ParameterKey.FRONT_AERO, nested(root, 6, 4), bytes);
        validateFloat(definitions, ParameterKey.REAR_WING, nested(root, 6, 5), bytes);
        validateFloat(definitions, ParameterKey.FUEL, nested(root, 7, 1), bytes);
    }

    private static void validateAxle(
            Map<ParameterKey, ParameterDefinition> definitions,
            ParameterKey frontKey,
            ParameterKey rearKey,
            List<Value> corners,
            int childField,
            byte[] bytes) {
        ParameterDefinition front = definitions.get(frontKey);
        ParameterDefinition rear = definitions.get(rearKey);
        if (front == null && rear == null) return;
        if (corners.size() != 4) {
            throw new SetupValidationException("Vier Radfelder fehlen für die Bereichsprüfung");
        }
        if (front != null) {
            validateRaw(front, child(corners.get(0), childField), bytes);
            validateRaw(front, child(corners.get(1), childField), bytes);
        }
        if (rear != null) {
            validateRaw(rear, child(corners.get(2), childField), bytes);
            validateRaw(rear, child(corners.get(3), childField), bytes);
        }
    }

    private static void validateFloat(
            Map<ParameterKey, ParameterDefinition> definitions,
            ParameterKey key,
            Value value,
            byte[] bytes) {
        ParameterDefinition definition = definitions.get(key);
        if (definition != null) validateRaw(definition, value, bytes);
    }

    private static void validateRaw(ParameterDefinition definition, Value value, byte[] bytes) {
        if (!isFloat(value)) {
            throw new SetupValidationException("Bereichsparameter fehlt im Strukturträger: "
                    + definition.key.displayName);
        }
        double raw = readFloat(bytes, value.offset);
        if (!definition.contains(raw)) {
            throw new SetupValidationException("Rohwert außerhalb des verifizierten Bereichs: "
                    + definition.key.displayName + " = " + raw);
        }
    }

    private static void putAxleAverage(
            Map<ParameterKey, Double> target,
            ParameterKey frontKey,
            ParameterKey rearKey,
            List<Value> corners,
            int childField,
            byte[] bytes) {
        if (corners.size() != 4) return;
        Value fl = child(corners.get(0), childField);
        Value fr = child(corners.get(1), childField);
        Value rl = child(corners.get(2), childField);
        Value rr = child(corners.get(3), childField);
        if (isFloat(fl) && isFloat(fr)) {
            target.put(frontKey, ((double) readFloat(bytes, fl.offset) + readFloat(bytes, fr.offset)) / 2.0);
        }
        if (isFloat(rl) && isFloat(rr)) {
            target.put(rearKey, ((double) readFloat(bytes, rl.offset) + readFloat(bytes, rr.offset)) / 2.0);
        }
    }

    private static void patchAxle(
            byte[] bytes,
            List<Value> corners,
            int childField,
            Double front,
            Double rear,
            boolean absolute) {
        if (corners.size() != 4) return;
        if (front != null) {
            patchPair(bytes, child(corners.get(0), childField),
                    child(corners.get(1), childField), front, absolute);
        }
        if (rear != null) {
            patchPair(bytes, child(corners.get(2), childField),
                    child(corners.get(3), childField), rear, absolute);
        }
    }

    private static void patchPair(
            byte[] bytes,
            Value left,
            Value right,
            double targetAverage,
            boolean absolute) {
        if (!isFloat(left) || !isFloat(right)) {
            throw new SetupValidationException("Erwartete Radparameter fehlen im Strukturträger");
        }
        if (absolute) {
            writeIfPresent(bytes, left.offset, targetAverage);
            writeIfPresent(bytes, right.offset, targetAverage);
            return;
        }
        double leftBefore = readFloat(bytes, left.offset);
        double rightBefore = readFloat(bytes, right.offset);
        double delta = targetAverage - (leftBefore + rightBefore) / 2.0;
        if (delta == 0.0) return;
        writeIfPresent(bytes, left.offset, leftBefore + delta);
        writeIfPresent(bytes, right.offset, rightBefore + delta);
    }

    private static void putFloat(
            Map<ParameterKey, Double> target,
            ParameterKey key,
            Value value,
            byte[] bytes) {
        if (isFloat(value)) target.put(key, (double) readFloat(bytes, value.offset));
    }

    private static void patchFloat(byte[] bytes, Value value, Double number) {
        if (number == null) return;
        if (!isFloat(value)) throw new SetupValidationException("Erwartetes Float-Feld fehlt im Strukturträger");
        writeIfPresent(bytes, value.offset, number);
    }

    private static void writeIfPresent(byte[] bytes, int offset, Double value) {
        if (value == null) return;
        if (!Double.isFinite(value)) throw new SetupValidationException("Nicht-endlicher Patch-Wert");
        ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value.floatValue());
    }

    private static float readFloat(byte[] bytes, int offset) {
        float value = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        if (!Float.isFinite(value)) throw new SetupValidationException("Nicht-endlicher Wert im Setup");
        return value;
    }

    private static boolean isFloat(Value value) {
        return value != null && value.wire == 5;
    }

    private static Value child(Value parent, int field) {
        Node node = message(parent);
        return node == null ? null : node.first(field);
    }

    private static Value nested(Node parent, int outer, int inner) {
        return child(parent.first(outer), inner);
    }

    private static Node message(Value value) {
        return value == null ? null : value.message;
    }

    private static Node parse(byte[] bytes, int start, int end, int depth) {
        if (depth > MAX_DEPTH) throw new SetupValidationException("Maximale Binärtiefe überschritten");
        Node node = new Node();
        int[] cursor = {start};
        while (cursor[0] < end) {
            long tag = readVarint(bytes, cursor, end);
            int field = (int) (tag >>> 3);
            int wire = (int) (tag & 7);
            if (field <= 0) throw new SetupValidationException("Ungültige Feldnummer");
            switch (wire) {
                case 0 -> {
                    int offset = cursor[0];
                    readVarint(bytes, cursor, end);
                    node.add(field, new Value(wire, offset, cursor[0] - offset, null));
                }
                case 1 -> {
                    require(cursor[0] + 8 <= end);
                    node.add(field, new Value(wire, cursor[0], 8, null));
                    cursor[0] += 8;
                }
                case 2 -> {
                    long rawLength = readVarint(bytes, cursor, end);
                    require(rawLength >= 0 && rawLength <= Integer.MAX_VALUE);
                    int length = (int) rawLength;
                    require(cursor[0] + length <= end);
                    int offset = cursor[0];
                    Node child = tryParse(bytes, offset, offset + length, depth + 1);
                    node.add(field, new Value(wire, offset, length, child));
                    cursor[0] += length;
                }
                case 5 -> {
                    require(cursor[0] + 4 <= end);
                    node.add(field, new Value(wire, cursor[0], 4, null));
                    cursor[0] += 4;
                }
                default -> throw new SetupValidationException("Nicht unterstützter Wire-Type " + wire);
            }
        }
        require(cursor[0] == end);
        return node;
    }

    private static Node tryParse(byte[] bytes, int start, int end, int depth) {
        if (start == end) return null;
        try {
            return parse(bytes, start, end, depth);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static long readVarint(byte[] bytes, int[] cursor, int end) {
        long value = 0;
        int shift = 0;
        while (cursor[0] < end && shift <= 63) {
            int b = bytes[cursor[0]++] & 0xff;
            value |= (long) (b & 0x7f) << shift;
            if ((b & 0x80) == 0) return value;
            shift += 7;
        }
        throw new SetupValidationException("Ungültiges Varint");
    }

    private static void require(boolean condition) {
        if (!condition) throw new SetupValidationException("Abgeschnittene Binärnachricht");
    }

    private static final class Node {
        final Map<Integer, List<Value>> fields = new java.util.HashMap<>();

        void add(int field, Value value) {
            fields.computeIfAbsent(field, ignored -> new ArrayList<>()).add(value);
        }

        Value first(int field) {
            List<Value> values = fields.get(field);
            return values == null || values.isEmpty() ? null : values.get(0);
        }

        List<Value> all(int field) {
            List<Value> values = fields.get(field);
            return values == null ? Collections.emptyList() : values;
        }
    }

    private static final class Value {
        final int wire;
        final int offset;
        final int length;
        final Node message;

        Value(int wire, int offset, int length, Node message) {
            this.wire = wire;
            this.offset = offset;
            this.length = length;
            this.message = message;
        }
    }
}
