package com.greenbuddy.acevosetupengineer.beta;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** Synthetic protobuf contract fixture; never packaged in the application. */
public final class MustangTestFixture {
    private MustangTestFixture() { }

    public static byte[] valid() {
        byte[] chassis = concat(
                packed(1, 40000, 30000),
                fixed(2, 14.3f),
                message(3, concat(fixed(1, 58), fixed(2, 95))),
                message(4, concat(fixed(1, .4f), fixed(2, .35f), fixed(3, 75))));
        ByteArrayOutputStream top = new ByteArrayOutputStream();
        append(top, message(1, chassis));
        for (float spring : new float[]{150000, 150000, 140000, 140000}) {
            append(top, message(2, fixed(1, spring)));
        }
        for (int index = 0; index < 4; index++) {
            append(top, message(3, concat(fixed(1, 5 + index), fixed(3, 7 + index))));
        }
        float[] pressure = {27, 27, 27.2f, 27.2f};
        float[] camber = {-3.5f, -3.5f, -3, -3};
        float[] toe = {-.05f, -.05f, .1f, .1f};
        for (int index = 0; index < 4; index++) {
            append(top, message(4, concat(fixed(1, pressure[index]),
                    fixed(2, camber[index]), fixed(3, toe[index]))));
        }
        append(top, message(5, concat(fixed(1, 5), fixed(2, 4), fixed(3, 5))));
        // Field 6.4 is deliberately unknown/fixed and must remain byte-identical.
        append(top, message(6, concat(fixed(2, 60), fixed(3, 65),
                fixed(4, 123.456f), fixed(5, 4))));
        append(top, message(7, fixed(1, 40)));
        append(top, bytes(9, MustangSetupInspector.MUSTANG_PRESET_ID
                .getBytes(StandardCharsets.US_ASCII)));
        append(top, varintField(10, 1));
        append(top, varintField(11, 987654));
        return top.toByteArray();
    }

    public static byte[] withoutMustangId() {
        byte[] result = valid();
        byte[] needle = MustangSetupInspector.MUSTANG_PRESET_ID.getBytes(StandardCharsets.US_ASCII);
        for (int offset = 0; offset <= result.length - needle.length; offset++) {
            boolean match = true;
            for (int index = 0; index < needle.length; index++) {
                if (result[offset + index] != needle[index]) { match = false; break; }
            }
            if (match) { result[offset] = 'x'; break; }
        }
        return result;
    }

    private static byte[] fixed(int number, float value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        varint(output, ((long) number << 3) | 5);
        int bits = Float.floatToRawIntBits(value);
        output.write(bits);
        output.write(bits >>> 8);
        output.write(bits >>> 16);
        output.write(bits >>> 24);
        return output.toByteArray();
    }

    private static byte[] packed(int number, float... values) {
        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        for (float value : values) {
            int bits = Float.floatToRawIntBits(value);
            payload.write(bits); payload.write(bits >>> 8); payload.write(bits >>> 16); payload.write(bits >>> 24);
        }
        return bytes(number, payload.toByteArray());
    }

    private static byte[] message(int number, byte[] payload) { return bytes(number, payload); }

    private static byte[] bytes(int number, byte[] payload) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        varint(output, ((long) number << 3) | 2);
        varint(output, payload.length);
        append(output, payload);
        return output.toByteArray();
    }

    private static byte[] varintField(int number, long value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        varint(output, (long) number << 3);
        varint(output, value);
        return output.toByteArray();
    }

    private static byte[] concat(byte[]... values) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] value : values) append(output, value);
        return output.toByteArray();
    }

    private static void append(ByteArrayOutputStream output, byte[] bytes) {
        output.write(bytes, 0, bytes.length);
    }

    private static void varint(ByteArrayOutputStream output, long value) {
        while ((value & ~0x7fL) != 0) {
            output.write((int) ((value & 0x7f) | 0x80));
            value >>>= 7;
        }
        output.write((int) value);
    }
}
