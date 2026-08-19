package com.greenbuddy.acevosetupengineer.beta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Bounds-checked protobuf wire reader. Groups are deliberately rejected. */
final class ProtoWire {
    private ProtoWire() { }

    static Message parse(byte[] bytes, int start, int end) {
        if (bytes == null || start < 0 || end < start || end > bytes.length) {
            throw new IllegalArgumentException("Invalid protobuf bounds");
        }
        List<Field> fields = new ArrayList<>();
        int cursor = start;
        while (cursor < end) {
            Varint tag = varint(bytes, cursor, end);
            cursor = tag.next;
            int number = (int) (tag.value >>> 3);
            int wireType = (int) (tag.value & 7L);
            if (number <= 0) throw new IllegalArgumentException("Invalid protobuf field number");
            int payload;
            int length;
            switch (wireType) {
                case 0:
                    payload = cursor;
                    Varint value = varint(bytes, cursor, end);
                    cursor = value.next;
                    length = cursor - payload;
                    break;
                case 1:
                    payload = cursor;
                    length = 8;
                    cursor = checkedAdvance(cursor, length, end);
                    break;
                case 2:
                    Varint size = varint(bytes, cursor, end);
                    cursor = size.next;
                    if (size.value < 0 || size.value > Integer.MAX_VALUE) {
                        throw new IllegalArgumentException("Invalid protobuf length");
                    }
                    payload = cursor;
                    length = (int) size.value;
                    cursor = checkedAdvance(cursor, length, end);
                    break;
                case 5:
                    payload = cursor;
                    length = 4;
                    cursor = checkedAdvance(cursor, length, end);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported protobuf wire type " + wireType);
            }
            fields.add(new Field(number, wireType, payload, length));
        }
        if (cursor != end) throw new IllegalArgumentException("Incomplete protobuf message");
        return new Message(bytes, fields);
    }

    static float fixed32Float(byte[] bytes, int offset) {
        if (offset < 0 || offset + 4 > bytes.length) throw new IllegalArgumentException("fixed32");
        int bits = (bytes[offset] & 0xff)
                | ((bytes[offset + 1] & 0xff) << 8)
                | ((bytes[offset + 2] & 0xff) << 16)
                | ((bytes[offset + 3] & 0xff) << 24);
        return Float.intBitsToFloat(bits);
    }

    static void putFixed32Float(byte[] bytes, int offset, float value) {
        int bits = Float.floatToRawIntBits(value);
        bytes[offset] = (byte) bits;
        bytes[offset + 1] = (byte) (bits >>> 8);
        bytes[offset + 2] = (byte) (bits >>> 16);
        bytes[offset + 3] = (byte) (bits >>> 24);
    }

    private static int checkedAdvance(int cursor, int amount, int end) {
        long next = (long) cursor + amount;
        if (amount < 0 || next > end) throw new IllegalArgumentException("Truncated protobuf field");
        return (int) next;
    }

    private static Varint varint(byte[] bytes, int cursor, int end) {
        long value = 0;
        for (int shift = 0; shift < 64 && cursor < end; shift += 7) {
            int next = bytes[cursor++] & 0xff;
            value |= (long) (next & 0x7f) << shift;
            if ((next & 0x80) == 0) return new Varint(value, cursor);
        }
        throw new IllegalArgumentException("Invalid protobuf varint");
    }

    static final class Message {
        private final byte[] bytes;
        private final List<Field> fields;
        Message(byte[] bytes, List<Field> fields) {
            this.bytes = bytes;
            this.fields = Collections.unmodifiableList(fields);
        }
        List<Field> fields(int number) {
            List<Field> result = new ArrayList<>();
            for (Field field : fields) if (field.number == number) result.add(field);
            return result;
        }
        Field one(int number, int wireType) {
            List<Field> matches = fields(number);
            if (matches.size() != 1 || matches.get(0).wireType != wireType) {
                throw new IllegalArgumentException("Expected exactly one field " + number
                        + " with wire type " + wireType);
            }
            return matches.get(0);
        }
        Message message(Field field) {
            if (field.wireType != 2) throw new IllegalArgumentException("Not a message");
            return parse(bytes, field.payload, field.payload + field.length);
        }
        long varintValue(Field field) {
            if (field.wireType != 0) throw new IllegalArgumentException("Not a varint");
            Varint value = varint(bytes, field.payload, field.payload + field.length);
            if (value.next != field.payload + field.length) {
                throw new IllegalArgumentException("Malformed varint payload");
            }
            return value.value;
        }
        boolean payloadStartsWith(Field field, byte[] prefix) {
            if (field.wireType != 2 || prefix.length > field.length) return false;
            for (int index = 0; index < prefix.length; index++) {
                if (bytes[field.payload + index] != prefix[index]) return false;
            }
            return true;
        }
        boolean payloadEquals(Field field, byte[] expected) {
            return field.length == expected.length && payloadStartsWith(field, expected);
        }
    }

    static final class Field {
        final int number;
        final int wireType;
        final int payload;
        final int length;
        Field(int number, int wireType, int payload, int length) {
            this.number = number;
            this.wireType = wireType;
            this.payload = payload;
            this.length = length;
        }
    }

    private static final class Varint {
        final long value;
        final int next;
        Varint(long value, int next) { this.value = value; this.next = next; }
    }
}
