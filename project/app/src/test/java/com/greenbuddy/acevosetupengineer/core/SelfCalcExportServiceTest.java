package com.greenbuddy.acevosetupengineer.core;

import com.greenbuddy.acevosetupengineer.binary.CarSetupNumericCodec;
import com.greenbuddy.acevosetupengineer.engineering.EngineeringProfile;
import com.greenbuddy.acevosetupengineer.engineering.EngineeringSetup;
import com.greenbuddy.acevosetupengineer.engineering.ParameterDefinition;
import com.greenbuddy.acevosetupengineer.engineering.ParameterKey;
import com.greenbuddy.acevosetupengineer.model.CatalogItem;
import com.greenbuddy.acevosetupengineer.model.SetupMode;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class SelfCalcExportServiceTest {
    @Test
    public void absentFixedAeroIsSkippedWithoutBlockingTheCar() {
        CatalogItem car = new CatalogItem(CatalogItem.Kind.VEHICLE, "test-car", "Test Car",
                "test-car", "ks_test_car_", "test_car", true, true);
        CatalogItem layout = new CatalogItem(CatalogItem.Kind.LAYOUT, "test-layout", "Test Layout",
                "test-layout", "", true, true);
        SetupRequest request = new SetupRequest(car, layout, SetupMode.FAST_CONTROL, "0.8.1");

        Map<ParameterKey, ParameterDefinition> definitions = new EnumMap<>(ParameterKey.class);
        add(definitions, ParameterKey.BRAKE_BIAS, 45, 65, 1);
        add(definitions, ParameterKey.TYRE_PRESSURE_FRONT, 20, 45, 1);
        add(definitions, ParameterKey.TYRE_PRESSURE_REAR, 20, 45, 1);
        add(definitions, ParameterKey.TOE_REAR, -0.5, 0.5, 0.05);
        add(definitions, ParameterKey.FUEL, 1, 50, 1);
        add(definitions, ParameterKey.FRONT_AERO, 0, 20, 1);
        EngineeringProfile profile = new EngineeringProfile(car.id, "0.8.1",
                car.expectedSignaturePrefix, "profile-sha", false, definitions);

        Map<ParameterKey, Double> values = new EnumMap<>(ParameterKey.class);
        values.put(ParameterKey.BRAKE_BIAS, 56.0);
        values.put(ParameterKey.TYRE_PRESSURE_FRONT, 31.0);
        values.put(ParameterKey.TYRE_PRESSURE_REAR, 32.0);
        values.put(ParameterKey.TOE_REAR, 0.15);
        values.put(ParameterKey.FUEL, 20.0);
        values.put(ParameterKey.FRONT_AERO, 10.0);
        EngineeringSetup generated = new EngineeringSetup(
                EngineeringSetup.Label.ENGINEERING_MODEL, values, List.of("test model"));

        SelfCalcExportService.Result result = new SelfCalcExportService().apply(
                request, syntheticCarrier(), profile, generated);

        assertFalse(result.setup.values.containsKey(ParameterKey.FRONT_AERO));
        assertEquals(5, result.setup.values.size());
        assertTrue(result.setup.audit.stream().anyMatch(
                line -> line.contains("Aero vorn") && line.contains("nicht")));
        Map<ParameterKey, Double> decoded = new CarSetupNumericCodec().decodeKnown(result.bytes);
        assertEquals(56.0, decoded.get(ParameterKey.BRAKE_BIAS), 0.001);
        assertEquals(31.0, decoded.get(ParameterKey.TYRE_PRESSURE_FRONT), 0.001);
        assertEquals(32.0, decoded.get(ParameterKey.TYRE_PRESSURE_REAR), 0.001);
    }

    private static void add(
            Map<ParameterKey, ParameterDefinition> definitions,
            ParameterKey key,
            double minimum,
            double maximum,
            double step) {
        definitions.put(key, ParameterDefinition.verifiedRangeOnly(
                key, minimum, maximum, step, "test", "verified-test-range", true));
    }

    private static byte[] syntheticCarrier() {
        ByteArrayOutputStream root = new ByteArrayOutputStream();
        writeMessage(root, 1, message(fieldBytes(3, message(fieldFloat(1, 55)))));
        for (int corner = 0; corner < 4; corner++) {
            float pressure = corner < 2 ? 30 : 31;
            writeMessage(root, 4, message(
                    fieldFloat(1, pressure),
                    fieldFloat(3, 0.1f)));
        }
        writeMessage(root, 7, message(fieldFloat(1, 10)));
        writeMessage(root, 9, "ks_test_car_preset".getBytes(StandardCharsets.UTF_8));
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
        output.writeBytes(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                .putFloat(value).array());
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
}
