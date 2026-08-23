package com.greenbuddy.acevosetupengineer.engineering;

import com.greenbuddy.acevosetupengineer.model.SetupMode;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Explicit per-vehicle rules requested by the driver; every value still passes the car range. */
public final class VehicleSetupPolicy {
    private static final String MUSTANG_GT3 = "ford-mustang-gt3";

    public EngineeringSetup apply(
            SetupRequest request,
            EngineeringProfile profile,
            EngineeringSetup setup) {
        Map<ParameterKey, Double> values = new EnumMap<>(ParameterKey.class);
        values.putAll(setup.values);
        List<String> audit = new ArrayList<>(setup.audit);

        if (MUSTANG_GT3.equals(request.vehicle.id) && request.mode == SetupMode.FAST_ATTACK) {
            ParameterDefinition tc = profile.parameters.get(ParameterKey.TRACTION_CONTROL);
            if (tc == null || !tc.fineTuneWritable() || !tc.contains(1.0)) {
                throw new SetupValidationException(
                        "Mustang FAST ATTACK benötigt den verifizierten TC-Zielwert 1");
            }
            values.put(ParameterKey.TRACTION_CONTROL, tc.clampAndRound(1.0));
            audit.add("Fahrzeugregel Ford Mustang GT3: TC = 1 in FAST ATTACK");
        }

        return new EngineeringSetup(setup.label, values, audit);
    }
}
