package com.greenbuddy.acevosetupengineer.engineering;

import com.greenbuddy.acevosetupengineer.model.CatalogItem;
import com.greenbuddy.acevosetupengineer.model.SetupMode;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import org.junit.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class SelfCalcEngineTest {
    @Test
    public void rangeModelIsDeterministicBoundedAndModeSensitive() {
        CatalogItem car = new CatalogItem(CatalogItem.Kind.VEHICLE, "test-car", "Test Car",
                "test-car", "ks_test_car_", "test_car", true, true);
        CatalogItem layout = new CatalogItem(CatalogItem.Kind.LAYOUT, "test-layout", "Test Layout",
                "test-layout", "", true, true);
        Map<ParameterKey, ParameterDefinition> definitions = new EnumMap<>(ParameterKey.class);
        definitions.put(ParameterKey.REAR_WING, ParameterDefinition.verifiedRangeOnly(
                ParameterKey.REAR_WING, 0, 20, 1, "click", "verified-range", true));
        definitions.put(ParameterKey.BRAKE_BIAS, ParameterDefinition.verifiedRangeOnly(
                ParameterKey.BRAKE_BIAS, 45, 65, 0.5, "% front", "verified-range", true));
        EngineeringProfile profile = new EngineeringProfile("test-car", "0.8.1",
                "ks_test_car_", "profile-sha", false, definitions);
        TrackProfile track = new TrackProfile("test-layout", "0.8.1",
                0.6, 0, -0.2, -0.3, 5_000,
                "https://example.test/exact-geometry", "track-sha", true);

        SetupRequest attack = new SetupRequest(car, layout, SetupMode.FAST_ATTACK, "0.8.1");
        SetupRequest safe = new SetupRequest(car, layout, SetupMode.FAST_SAFE, "0.8.1");
        SelfCalcEngine engine = new SelfCalcEngine();
        EngineeringSetup attackA = engine.calculate(attack, profile, track, false);
        EngineeringSetup attackB = engine.calculate(attack, profile, track, false);
        EngineeringSetup safeSetup = engine.calculate(safe, profile, track, true);

        assertEquals(attackA.values, attackB.values);
        assertTrue(!attackA.values.equals(safeSetup.values));
        for (Map.Entry<ParameterKey, Double> value : attackA.values.entrySet()) {
            assertTrue(definitions.get(value.getKey()).contains(value.getValue()));
        }
        assertTrue(safeSetup.audit.stream().anyMatch(line -> line.startsWith("LIVE-UNVERIFIED")));
    }

    @Test
    public void fastControlKeepsMoreRearStabilityThanAttackAcrossTrackExtremes() {
        CatalogItem car = new CatalogItem(CatalogItem.Kind.VEHICLE, "test-car", "Test Car",
                "test-car", "ks_test_car_", "test_car", true, true);
        CatalogItem layout = new CatalogItem(CatalogItem.Kind.LAYOUT, "test-layout", "Test Layout",
                "test-layout", "", true, true);
        Map<ParameterKey, ParameterDefinition> definitions = new EnumMap<>(ParameterKey.class);
        definitions.put(ParameterKey.REAR_WING, ParameterDefinition.verifiedRangeOnly(
                ParameterKey.REAR_WING, 0, 20, 1, "click", "verified-range", true));
        definitions.put(ParameterKey.TRACTION_CONTROL, ParameterDefinition.verifiedRangeOnly(
                ParameterKey.TRACTION_CONTROL, 0, 12, 1, "click", "verified-range", true));
        definitions.put(ParameterKey.BRAKE_BIAS, ParameterDefinition.verifiedRangeOnly(
                ParameterKey.BRAKE_BIAS, 48, 70, 0.2, "% front", "verified-range", true));
        definitions.put(ParameterKey.ANTI_ROLL_BAR_REAR, ParameterDefinition.verifiedRangeOnly(
                ParameterKey.ANTI_ROLL_BAR_REAR, 10_000, 100_000, 1_000, "N/m", "verified-range", true));
        EngineeringProfile profile = new EngineeringProfile("test-car", "0.8.1",
                "ks_test_car_", "profile-sha", false, definitions);
        SelfCalcEngine engine = new SelfCalcEngine();

        for (double demand : new double[]{-1.0, 0.0, 1.0}) {
            TrackProfile track = new TrackProfile("test-layout", "0.8.1",
                    demand, 0, demand, demand, 5_000,
                    "https://example.test/exact-geometry", "track-sha-" + demand, true);
            EngineeringSetup attack = engine.calculate(new SetupRequest(
                    car, layout, SetupMode.FAST_ATTACK, "0.8.1"), profile, track, false);
            EngineeringSetup control = engine.calculate(new SetupRequest(
                    car, layout, SetupMode.FAST_CONTROL, "0.8.1"), profile, track, false);

            assertTrue(control.values.get(ParameterKey.REAR_WING)
                    >= attack.values.get(ParameterKey.REAR_WING));
            assertTrue(control.values.get(ParameterKey.TRACTION_CONTROL)
                    >= attack.values.get(ParameterKey.TRACTION_CONTROL));
            assertTrue(control.values.get(ParameterKey.BRAKE_BIAS)
                    >= attack.values.get(ParameterKey.BRAKE_BIAS));
            assertTrue(control.values.get(ParameterKey.ANTI_ROLL_BAR_REAR)
                    <= attack.values.get(ParameterKey.ANTI_ROLL_BAR_REAR));
        }
    }

    @Test
    public void longRunStartsWithMoreTyrePressureHeatReserve() {
        CatalogItem car = new CatalogItem(CatalogItem.Kind.VEHICLE, "test-car", "Test Car",
                "test-car", "ks_test_car_", "test_car", true, true);
        CatalogItem layout = new CatalogItem(CatalogItem.Kind.LAYOUT, "test-layout", "Test Layout",
                "test-layout", "", true, true);
        Map<ParameterKey, ParameterDefinition> definitions = new EnumMap<>(ParameterKey.class);
        definitions.put(ParameterKey.TYRE_PRESSURE_FRONT, ParameterDefinition.verifiedRangeOnly(
                ParameterKey.TYRE_PRESSURE_FRONT, 17, 35, 0.1, "psi", "verified-range", true));
        definitions.put(ParameterKey.TYRE_PRESSURE_REAR, ParameterDefinition.verifiedRangeOnly(
                ParameterKey.TYRE_PRESSURE_REAR, 17, 35, 0.1, "psi", "verified-range", true));
        EngineeringProfile profile = new EngineeringProfile("test-car", "0.8.1",
                "ks_test_car_", "profile-sha", false, definitions);
        TrackProfile track = new TrackProfile("test-layout", "0.8.1",
                0.2, 0, 0.4, -0.1, 6_000,
                "https://example.test/exact-geometry", "track-sha", true);
        SelfCalcEngine engine = new SelfCalcEngine();
        EngineeringSetup control = engine.calculate(new SetupRequest(
                car, layout, SetupMode.FAST_CONTROL, "0.8.1"), profile, track, false);
        EngineeringSetup longRun = engine.calculate(new SetupRequest(
                car, layout, SetupMode.FAST_LONG_RUN, "0.8.1"), profile, track, false);

        assertTrue(longRun.values.get(ParameterKey.TYRE_PRESSURE_FRONT)
                < control.values.get(ParameterKey.TYRE_PRESSURE_FRONT));
        assertTrue(longRun.values.get(ParameterKey.TYRE_PRESSURE_REAR)
                < control.values.get(ParameterKey.TYRE_PRESSURE_REAR));
    }
}
