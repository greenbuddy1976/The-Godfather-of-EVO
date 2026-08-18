package com.greenbuddy.acevosetupengineer.engineering;

import com.greenbuddy.acevosetupengineer.model.CatalogItem;
import com.greenbuddy.acevosetupengineer.model.SetupMode;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import org.junit.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public final class VehicleSetupPolicyTest {
    @Test
    public void mustangFastAttackAlwaysUsesTcOne() {
        CatalogItem mustang = new CatalogItem(CatalogItem.Kind.VEHICLE,
                "ford-mustang-gt3", "Ford Mustang GT3", "ford-mustang-gt3",
                "ks_ford_mustang_gt3_", "ford_mustang_gt3", true, true);
        CatalogItem layout = new CatalogItem(CatalogItem.Kind.LAYOUT,
                "test-layout", "Test Layout", "test-layout", "", true, true);
        Map<ParameterKey, ParameterDefinition> definitions = new EnumMap<>(ParameterKey.class);
        definitions.put(ParameterKey.TRACTION_CONTROL, ParameterDefinition.verifiedRangeOnly(
                ParameterKey.TRACTION_CONTROL, 0, 12, 1, "click", "verified-range", true));
        EngineeringProfile profile = new EngineeringProfile(mustang.id, "0.8.1",
                mustang.expectedSignaturePrefix, "profile", false, definitions);
        Map<ParameterKey, Double> values = new EnumMap<>(ParameterKey.class);
        values.put(ParameterKey.TRACTION_CONTROL, 7.0);
        EngineeringSetup setup = new EngineeringSetup(
                EngineeringSetup.Label.ENGINEERING_MODEL, values, List.of());

        for (int trackIndex = 0; trackIndex < 24; trackIndex++) {
            SetupRequest request = new SetupRequest(mustang, layout, SetupMode.FAST_ATTACK, "0.8.1");
            EngineeringSetup result = new VehicleSetupPolicy().apply(request, profile, setup);
            assertEquals(1.0, result.values.get(ParameterKey.TRACTION_CONTROL), 0.0);
        }
    }

    @Test
    public void otherMustangModesKeepCalculatedTc() {
        CatalogItem mustang = new CatalogItem(CatalogItem.Kind.VEHICLE,
                "ford-mustang-gt3", "Ford Mustang GT3", "ford-mustang-gt3",
                "ks_ford_mustang_gt3_", "ford_mustang_gt3", true, true);
        CatalogItem layout = new CatalogItem(CatalogItem.Kind.LAYOUT,
                "test-layout", "Test Layout", "test-layout", "", true, true);
        Map<ParameterKey, ParameterDefinition> definitions = new EnumMap<>(ParameterKey.class);
        definitions.put(ParameterKey.TRACTION_CONTROL, ParameterDefinition.verifiedRangeOnly(
                ParameterKey.TRACTION_CONTROL, 0, 12, 1, "click", "verified-range", true));
        EngineeringProfile profile = new EngineeringProfile(mustang.id, "0.8.1",
                mustang.expectedSignaturePrefix, "profile", false, definitions);
        Map<ParameterKey, Double> values = new EnumMap<>(ParameterKey.class);
        values.put(ParameterKey.TRACTION_CONTROL, 5.0);
        EngineeringSetup setup = new EngineeringSetup(
                EngineeringSetup.Label.ENGINEERING_MODEL, values, List.of());

        SetupRequest request = new SetupRequest(mustang, layout, SetupMode.FAST_CONTROL, "0.8.1");
        EngineeringSetup result = new VehicleSetupPolicy().apply(request, profile, setup);
        assertEquals(5.0, result.values.get(ParameterKey.TRACTION_CONTROL), 0.0);
    }
}
