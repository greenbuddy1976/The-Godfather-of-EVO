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

        SetupRequest attack = new SetupRequest(car, layout, SetupMode.HOTLAP_ATTACK, "0.8.1");
        SetupRequest safe = new SetupRequest(car, layout, SetupMode.SAFE, "0.8.1");
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
}
