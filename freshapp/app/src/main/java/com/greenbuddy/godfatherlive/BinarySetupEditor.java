package com.greenbuddy.godfatherlive;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BinarySetupEditor {
    static final class EditResult {
        final byte[] bytes;
        final int requested;
        final int applied;
        final List<String> skipped;

        EditResult(byte[] bytes, int requested, int applied, List<String> skipped) {
            this.bytes = bytes;
            this.requested = requested;
            this.applied = applied;
            this.skipped = skipped;
        }
    }

    private static final class FlatValue {
        final int offset;
        float value;
        FlatValue(int offset, float value) { this.offset = offset; this.value = value; }
    }

    private static final class Node {
        final int field;
        final int wireType;
        final int offset;
        final float f32;
        final List<Node> children;
        final List<FlatValue> packed;

        Node(int field, int wireType, int offset, float f32,
             List<Node> children, List<FlatValue> packed) {
            this.field = field;
            this.wireType = wireType;
            this.offset = offset;
            this.f32 = f32;
            this.children = children;
            this.packed = packed;
        }
    }

    static EditResult apply(byte[] source, List<SetupEngine.Change> changes) {
        QueryLogic.requireRealCarsetup(source);
        List<Node> parsed = parseMessage(source, 0, source.length, 0);
        if (parsed == null || parsed.isEmpty()) {
            throw new IllegalArgumentException(".carsetup-Binaerstruktur konnte nicht sicher gelesen werden");
        }

        Map<String, FlatValue> fields = new LinkedHashMap<>();
        flatten(parsed, "", fields);
        int known = 0;
        for (String key : fields.keySet()) if (isKnownWritable(key)) known++;
        if (known < 6) {
            throw new IllegalArgumentException("Zu wenige bestaetigte Setup-Felder in dieser .carsetup-Datei");
        }

        byte[] out = source.clone();
        ByteBuffer writer = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN);
        List<String> skipped = new ArrayList<>();
        int applied = 0;

        for (SetupEngine.Change change : changes) {
            if (!isKnownWritable(change.path)) {
                if (!skipped.contains(change.path)) skipped.add(change.path);
                continue;
            }
            FlatValue field = fields.get(change.path);
            if (field == null || field.offset < 0 || field.offset + 4 > out.length) {
                if (!skipped.contains(change.path)) skipped.add(change.path);
                continue;
            }
            float before = field.value;
            float after;
            switch (change.operation) {
                case ADD -> after = before + change.value;
                case SCALE -> after = before * change.value;
                case SET -> after = change.value;
                default -> throw new IllegalStateException("Unbekannte Setup-Operation");
            }
            after = clamp(change.path, before, after);
            if (!Float.isFinite(after)) {
                if (!skipped.contains(change.path)) skipped.add(change.path);
                continue;
            }
            writer.putFloat(field.offset, after);
            field.value = after;
            applied++;
        }

        QueryLogic.requireRealCarsetup(out);
        List<Node> verify = parseMessage(out, 0, out.length, 0);
        if (verify == null || verify.isEmpty()) {
            throw new IllegalStateException("Selbstpruefung der erzeugten .carsetup-Datei fehlgeschlagen");
        }
        return new EditResult(out, changes.size(), applied, skipped);
    }

    static int countWritableFields(byte[] source) {
        List<Node> parsed = parseMessage(source, 0, source.length, 0);
        if (parsed == null) return 0;
        Map<String, FlatValue> fields = new LinkedHashMap<>();
        flatten(parsed, "", fields);
        int count = 0;
        for (String path : fields.keySet()) if (isKnownWritable(path)) count++;
        return count;
    }

    private static boolean isKnownWritable(String path) {
        if (path == null) return false;
        if (path.equals("1.1[0]") || path.equals("1.1[1]") || path.equals("1.3.1")
                || path.equals("1.4.3") || path.equals("5.1") || path.equals("5.2")
                || path.equals("5.3") || path.equals("6.2") || path.equals("6.3")
                || path.equals("6.5") || path.equals("7.1")) return true;
        if (path.matches("2\\[[0-3]]\\.1")) return true;
        if (path.matches("2\\[[0-3]]\\.2\\.[12]")) return true;
        if (path.matches("3\\[[0-3]]\\.[1-4]")) return true;
        return path.matches("4\\[[0-3]]\\.[1-4]");
    }

    private static float clamp(String path, float before, float value) {
        if (path.matches("4\\[[0-3]]\\.1")) return between(value, 15f, 40f);
        if (path.matches("4\\[[0-3]]\\.2")) return between(value, -8f, 2f);
        if (path.matches("4\\[[0-3]]\\.3")) return between(value, -2f, 2f);
        if (path.matches("4\\[[0-3]]\\.4")) return between(value, 0f, 20f);
        if (path.equals("1.3.1")) return between(value, 45f, 80f);
        if (path.equals("1.4.3")) return between(value, 0f, Math.max(500f, before * 2f + 100f));
        if (path.equals("5.1") || path.equals("5.2") || path.equals("5.3")) return between(value, 0f, 20f);
        if (path.equals("6.2") || path.equals("6.3")) return between(value, 0f, 250f);
        if (path.equals("6.5")) return between(value, 0f, 40f);
        if (path.equals("7.1")) return between(value, 0f, 250f);
        if (path.matches("1\\.1\\[[01]]")) return between(value, 0f, 2_000_000f);
        if (path.matches("2\\[[0-3]]\\.1")) return between(value, 1_000f, 2_000_000f);
        if (path.matches("2\\[[0-3]]\\.2\\.[12]")) return between(value, 0f, 2_000_000f);
        if (path.matches("3\\[[0-3]]\\.[1-4]")) return between(value, 0f, 2_000_000f);
        return between(value, -100_000_000f, 100_000_000f);
    }

    private static float between(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static List<Node> parseMessage(byte[] bytes, int start, int length, int depth) {
        if (depth > 12 || length < 0 || start < 0 || start + length > bytes.length) return null;
        int end = start + length;
        int pos = start;
        List<Node> out = new ArrayList<>();
        ByteBuffer reader = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

        while (pos < end) {
            Varint tag = readVarint(bytes, pos, end);
            if (tag == null) return null;
            pos = tag.next;
            int field = (int) (tag.value >>> 3);
            int wire = (int) (tag.value & 7);
            if (field <= 0 || field > 100000) return null;

            if (wire == 0) {
                Varint value = readVarint(bytes, pos, end);
                if (value == null) return null;
                pos = value.next;
                out.add(new Node(field, wire, -1, Float.NaN, null, null));
            } else if (wire == 5) {
                if (pos + 4 > end) return null;
                float value = reader.getFloat(pos);
                if (!Float.isFinite(value) || Math.abs(value) > 100_000_000f) return null;
                out.add(new Node(field, wire, pos, value, null, null));
                pos += 4;
            } else if (wire == 1) {
                if (pos + 8 > end) return null;
                double value = reader.getDouble(pos);
                if (!Double.isFinite(value) || Math.abs(value) > 1e16) return null;
                out.add(new Node(field, wire, -1, Float.NaN, null, null));
                pos += 8;
            } else if (wire == 2) {
                Varint len = readVarint(bytes, pos, end);
                if (len == null || len.value < 0 || len.value > Integer.MAX_VALUE) return null;
                pos = len.next;
                int n = (int) len.value;
                if (pos + n > end) return null;

                List<Node> child = n == 0 ? new ArrayList<>() : parseMessage(bytes, pos, n, depth + 1);
                boolean childBad = child == null || child.isEmpty() || containsWireType(child, 1);
                List<FlatValue> packed = null;
                if (childBad && n > 0 && n % 4 == 0) {
                    packed = parsePackedFloats(bytes, pos, n, reader);
                }

                if (packed != null && !packed.isEmpty()) {
                    out.add(new Node(field, wire, -1, Float.NaN, null, packed));
                } else if (child != null && !child.isEmpty() && !containsWireType(child, 1)) {
                    out.add(new Node(field, wire, -1, Float.NaN, child, null));
                } else {
                    out.add(new Node(field, wire, -1, Float.NaN, null, null));
                }
                pos += n;
            } else {
                return null;
            }
        }
        return pos == end ? out : null;
    }

    private static List<FlatValue> parsePackedFloats(byte[] bytes, int start, int length, ByteBuffer reader) {
        List<FlatValue> values = new ArrayList<>();
        for (int i = 0; i < length; i += 4) {
            int off = start + i;
            float value = reader.getFloat(off);
            if (!Float.isFinite(value) || Math.abs(value) > 100_000_000f) return null;
            values.add(new FlatValue(off, value));
        }
        return values;
    }

    private static boolean containsWireType(List<Node> nodes, int wireType) {
        for (Node n : nodes) {
            if (n.wireType == wireType) return true;
            if (n.children != null && containsWireType(n.children, wireType)) return true;
        }
        return false;
    }

    private static void flatten(List<Node> nodes, String prefix, Map<String, FlatValue> out) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (Node n : nodes) counts.put(n.field, counts.getOrDefault(n.field, 0) + 1);
        Map<Integer, Integer> seen = new HashMap<>();

        for (Node n : nodes) {
            int index = seen.getOrDefault(n.field, 0);
            seen.put(n.field, index + 1);
            String suffix = counts.getOrDefault(n.field, 0) > 1 ? "[" + index + "]" : "";
            String path = prefix.isEmpty() ? String.valueOf(n.field) + suffix
                    : prefix + "." + n.field + suffix;
            if (n.children != null) {
                flatten(n.children, path, out);
            } else if (n.packed != null) {
                for (int i = 0; i < n.packed.size(); i++) out.put(path + "[" + i + "]", n.packed.get(i));
            } else if (n.wireType == 5 && n.offset >= 0) {
                out.put(path, new FlatValue(n.offset, n.f32));
            }
        }
    }

    private static final class Varint {
        final long value;
        final int next;
        Varint(long value, int next) { this.value = value; this.next = next; }
    }

    private static Varint readVarint(byte[] bytes, int start, int end) {
        long value = 0;
        int shift = 0;
        int pos = start;
        while (pos < end && shift <= 63) {
            int b = bytes[pos++] & 0xff;
            value |= (long) (b & 0x7f) << shift;
            if ((b & 0x80) == 0) return new Varint(value, pos);
            shift += 7;
        }
        return null;
    }

    private BinarySetupEditor() {}
}
