package com.greenbuddy.acevosetupengineer.binary;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class CarSetupInspector {
    private static final int MAX_SIZE = 65_536;
    private static final int MAX_DEPTH = 8;
    private static final int MAX_FIELD_NUMBER = 1_000;

    private CarSetupInspector() {}

    public static CarSetupInspection inspect(byte[] bytes) {
        if (bytes == null || bytes.length < 16 || bytes.length > MAX_SIZE) {
            return invalid("Dateigröße außerhalb des verifizierbaren Bereichs");
        }
        try {
            Stats stats = new Stats();
            parse(bytes, 0, bytes.length, 0, true, stats);
            if (stats.topLevelFields < 4) {
                return invalid("Zu wenige Top-Level-Felder");
            }
            if (stats.floatCount < 4) {
                return invalid("Zu wenige numerische Setup-Felder");
            }
            if (stats.vehicleSignature == null || !stats.vehicleSignature.startsWith("ks_")) {
                return invalid("Keine verifizierbare Fahrzeug-Signatur in Feld 9");
            }
            return new CarSetupInspection(true, stats.vehicleSignature,
                    stats.topLevelFields, stats.floatCount, null);
        } catch (RuntimeException ex) {
            return invalid("Binär-Decodierung fehlgeschlagen: " + ex.getMessage());
        }
    }

    private static CarSetupInspection invalid(String reason) {
        return new CarSetupInspection(false, null, 0, 0, reason);
    }

    private static void parse(byte[] data, int start, int end, int depth, boolean top, Stats stats) {
        if (depth > MAX_DEPTH) throw new IllegalArgumentException("Maximale Tiefe überschritten");
        int[] cursor = {start};
        while (cursor[0] < end) {
            long tag = readVarint(data, cursor, end);
            int field = (int) (tag >>> 3);
            int wire = (int) (tag & 7);
            if (field <= 0 || field > MAX_FIELD_NUMBER) {
                throw new IllegalArgumentException("Ungültige Feldnummer");
            }
            if (top) stats.topLevelFields++;
            switch (wire) {
                case 0 -> readVarint(data, cursor, end);
                case 1 -> require(cursor[0] + 8 <= end, "Abgeschnittenes 64-Bit-Feld");
                case 2 -> {
                    int length = checkedLength(readVarint(data, cursor, end));
                    int messageStart = cursor[0];
                    int messageEnd = messageStart + length;
                    require(messageEnd <= end, "Abgeschnittenes Längenfeld");
                    if (top && field == 9) {
                        String signature = new String(data, messageStart, length, StandardCharsets.UTF_8);
                        if (signature.startsWith("ks_")) stats.vehicleSignature = signature;
                    }
                    if (length > 0 && depth < MAX_DEPTH && looksLikeMessage(data, messageStart, messageEnd)) {
                        // Length-delimited protobuf fields can also contain raw bytes or UTF-8.
                        // Only merge nested statistics when the complete child parses cleanly.
                        Stats nested = new Stats();
                        try {
                            parse(data, messageStart, messageEnd, depth + 1, false, nested);
                            stats.floatCount += nested.floatCount;
                        } catch (RuntimeException ignored) {
                            // Opaque data is valid here and must not invalidate the outer setup.
                        }
                    }
                    cursor[0] = messageEnd;
                }
                case 5 -> {
                    require(cursor[0] + 4 <= end, "Abgeschnittenes Float-Feld");
                    float value = ByteBuffer.wrap(data, cursor[0], 4)
                            .order(ByteOrder.LITTLE_ENDIAN).getFloat();
                    require(Float.isFinite(value), "Nicht-endlicher Setup-Wert");
                    stats.floatCount++;
                    cursor[0] += 4;
                }
                default -> throw new IllegalArgumentException("Nicht unterstützter Wire-Type " + wire);
            }
            if (wire == 1) cursor[0] += 8;
        }
        require(cursor[0] == end, "Nachrichtenende stimmt nicht");
    }

    private static boolean looksLikeMessage(byte[] data, int start, int end) {
        if (end - start < 2) return false;
        int[] c = {start};
        try {
            long tag = readVarint(data, c, end);
            int field = (int) (tag >>> 3);
            int wire = (int) (tag & 7);
            return field > 0 && field <= MAX_FIELD_NUMBER && (wire == 0 || wire == 1 || wire == 2 || wire == 5);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static long readVarint(byte[] data, int[] cursor, int end) {
        long value = 0;
        int shift = 0;
        while (cursor[0] < end && shift <= 63) {
            int b = data[cursor[0]++] & 0xff;
            value |= (long) (b & 0x7f) << shift;
            if ((b & 0x80) == 0) return value;
            shift += 7;
        }
        throw new IllegalArgumentException("Ungültiges Varint");
    }

    private static int checkedLength(long value) {
        if (value < 0 || value > MAX_SIZE) throw new IllegalArgumentException("Ungültige Länge");
        return (int) value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    private static final class Stats {
        int topLevelFields;
        int floatCount;
        String vehicleSignature;
    }
}
