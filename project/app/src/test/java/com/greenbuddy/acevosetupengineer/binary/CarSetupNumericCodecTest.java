package com.greenbuddy.acevosetupengineer.binary;

import com.greenbuddy.acevosetupengineer.engineering.ParameterKey;
import com.greenbuddy.acevosetupengineer.engineering.ParameterDefinition;
import com.greenbuddy.acevosetupengineer.engineering.SetupValidationException;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class CarSetupNumericCodecTest {
    @Test
    public void decodingAndReapplyingSameAxleAveragesIsByteStable() {
        byte[] original = syntheticSetup();
        CarSetupNumericCodec codec = new CarSetupNumericCodec();
        Map<ParameterKey, Double> decoded = codec.decodeKnown(original);

        assertEquals(17, decoded.size());
        assertEquals(31.0, decoded.get(ParameterKey.TYRE_PRESSURE_FRONT), 0.0001);
        assertEquals(41.0, decoded.get(ParameterKey.TYRE_PRESSURE_REAR), 0.0001);
        assertArrayEquals(original, codec.patchKnown(original, decoded));
    }

    @Test
    public void relativeAxlePatchRoundTripsWithoutFlatteningCarrier() {
        byte[] original = syntheticSetup();
        CarSetupNumericCodec codec = new CarSetupNumericCodec();
        Map<ParameterKey, Double> changed = new EnumMap<>(codec.decodeKnown(original));
        changed.put(ParameterKey.TYRE_PRESSURE_FRONT, 32.0);

        byte[] patched = codec.patchKnown(original, changed);
        assertTrue(!Arrays.equals(original, patched));
        assertEquals(32.0, codec.decodeKnown(patched).get(ParameterKey.TYRE_PRESSURE_FRONT), 0.0001);
    }

    @Test
    public void absoluteAxlePatchEliminatesStoredCarrierNumbers() {
        CarSetupNumericCodec codec = new CarSetupNumericCodec();
        byte[] carrierA = syntheticSetup();
        Map<ParameterKey, Double> differentStoredPressure = new EnumMap<>(ParameterKey.class);
        differentStoredPressure.put(ParameterKey.TYRE_PRESSURE_FRONT, 25.0);
        byte[] carrierB = codec.patchKnownAbsoluteAxles(carrierA, differentStoredPressure);
        assertTrue(!Arrays.equals(carrierA, carrierB));

        Map<ParameterKey, Double> generated = new EnumMap<>(ParameterKey.class);
        generated.put(ParameterKey.TYRE_PRESSURE_FRONT, 32.0);
        byte[] outputA = codec.patchKnownAbsoluteAxles(carrierA, generated);
        byte[] outputB = codec.patchKnownAbsoluteAxles(carrierB, generated);

        assertArrayEquals(outputA, outputB);
        assertEquals(32.0, codec.decodeKnown(outputA).get(ParameterKey.TYRE_PRESSURE_FRONT), 0.0001);
    }

    @Test
    public void inspectorTreatsOpaqueLengthDataAsOpaque() {
        byte[] setup = syntheticSetup();
        byte[] opaque = fieldBytes(8, new byte[]{0x08});
        byte[] withOpaque = new byte[setup.length + opaque.length];
        System.arraycopy(opaque, 0, withOpaque, 0, opaque.length);
        System.arraycopy(setup, 0, withOpaque, opaque.length, setup.length);

        CarSetupInspection inspection = CarSetupInspector.inspect(withOpaque);
        assertTrue(inspection.structurallyValid);
        assertEquals("ks_test_vehicle_preset", inspection.vehicleSignature);
    }

    @Test
    public void rawWheelRangeCheckRejectsAverageThatPushesOneWheelPastMaximum() {
        byte[] original = syntheticSetup();
        CarSetupNumericCodec codec = new CarSetupNumericCodec();
        Map<ParameterKey, Double> changed = new EnumMap<>(codec.decodeKnown(original));
        changed.put(ParameterKey.TYRE_PRESSURE_FRONT, 32.0);
        byte[] patched = codec.patchKnown(original, changed); // preserves 2 psi left/right delta -> 31/33
        Map<ParameterKey, ParameterDefinition> definitions = new EnumMap<>(ParameterKey.class);
        definitions.put(ParameterKey.TYRE_PRESSURE_FRONT,
                ParameterDefinition.verifiedRangeOnly(ParameterKey.TYRE_PRESSURE_FRONT,
                        20, 32, 1, "psi", "synthetic-test", true));
        boolean rejected = false;
        try {
            codec.validateKnownRanges(patched, definitions);
        } catch (SetupValidationException expected) {
            rejected = true;
        }
        assertTrue(rejected);
    }

    /** Test-only structure with unique asymmetric wheel values; never packaged as a setup baseline. */
    private static byte[] syntheticSetup() {
        ByteArrayOutputStream root = new ByteArrayOutputStream();
        writeMessage(root, 1, message(
                fieldBytes(1, floats(1, 2)),
                fieldBytes(3, message(fieldFloat(1, 55)))));
        for (int corner = 0; corner < 4; corner++) {
            writeMessage(root, 2, message(fieldFloat(1, 100 + corner * 2)));
            writeMessage(root, 3, message(
                    fieldFloat(1, 10 + corner * 2),
                    fieldFloat(2, 20 + corner * 2),
                    fieldFloat(3, 30 + corner * 2),
                    fieldFloat(4, 40 + corner * 2)));
            writeMessage(root, 4, message(
                    fieldFloat(1, corner < 2 ? 30 + corner * 2 : 40 + (corner - 2) * 2),
                    fieldFloat(2, -3 + corner),
                    fieldFloat(3, 0.1f + corner * 0.1f)));
        }
        writeMessage(root, 5, message(fieldFloat(3, 4)));
        writeMessage(root, 7, message(fieldFloat(1, 25)));
        writeMessage(root, 9, "ks_test_vehicle_preset".getBytes(StandardCharsets.UTF_8));
        return root.toByteArray();
    }

    private static byte[] message(byte[]... fields) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        for (byte[] field : fields) output.writeBytes(field);
        return output.toByteArray();
    }

    private static byte[] fieldFloat(int field, float value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write((field << 3) | 5);
        output.writeBytes(floats(value));
        return output.toByteArray();
    }

    private static byte[] fieldBytes(int field, byte[] value) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeMessage(output, field, value);
        return output.toByteArray();
    }

    private static void writeMessage(ByteArrayOutputStream output, int field, byte[] value) {
        output.write((field << 3) | 2);
        output.write(value.length);
        try {
            output.write(value);
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static byte[] floats(float... values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : values) buffer.putFloat(value);
        return buffer.array();
    }
}
