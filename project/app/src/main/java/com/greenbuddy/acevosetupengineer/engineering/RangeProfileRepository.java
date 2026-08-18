package com.greenbuddy.acevosetupengineer.engineering;

import android.content.Context;

import com.greenbuddy.acevosetupengineer.model.CatalogItem;
import com.greenbuddy.acevosetupengineer.util.Hashing;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/** Loads version-pinned ranges extracted from EVO carsetuplimits, never from setup values. */
public final class RangeProfileRepository {
    private static final String ASSET = "evo-carsetuplimits-0.8.1.json";
    private static final String GAME_VERSION = "0.8.1";
    private static final String SOURCE =
            "SpeedHQ/RaceIQ@0bb86a3: shared/games/ac-evo/setup-ranges.json; "
                    + "generated from installed EVO content.kspkg carsetuplimits";

    private final JSONObject profiles;
    private final String datasetSha256;

    public RangeProfileRepository(Context context) throws IOException {
        byte[] bytes = readAsset(context, ASSET);
        try {
            profiles = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
        } catch (JSONException invalid) {
            throw new IOException("EVO-Wertebereiche sind kein gültiges JSON", invalid);
        }
        datasetSha256 = Hashing.sha256(bytes);
    }

    public boolean hasVerifiedRanges(CatalogItem vehicle) {
        return vehicle.hasVerifiedRangeIdentity() && profiles.has(vehicle.rangeKey);
    }

    /**
     * Version-pinned range profile. SELF CALC may use it only with an independently
     * verified same-car binary structure; no values from that carrier become model input.
     */
    public EngineeringProfile loadRangeOnly(CatalogItem vehicle) {
        if (!hasVerifiedRanges(vehicle)) {
            throw new SetupValidationException("Kein verifiziertes EVO-carsetuplimits-Profil für " + vehicle.name);
        }
        JSONObject raw;
        try {
            raw = profiles.getJSONObject(vehicle.rangeKey);
        } catch (JSONException invalid) {
            throw new SetupValidationException("Verifiziertes EVO-Werteprofil ist beschädigt");
        }
        Map<ParameterKey, ParameterDefinition> parameters = new EnumMap<>(ParameterKey.class);

        add(parameters, raw, "steerRatio", ParameterKey.STEERING_RATIO, "ratio");
        addPhysicalArb(parameters, raw, "frontARB", ParameterKey.ANTI_ROLL_BAR_FRONT);
        addPhysicalArb(parameters, raw, "rearARB", ParameterKey.ANTI_ROLL_BAR_REAR);
        add(parameters, raw, "brakeBias", ParameterKey.BRAKE_BIAS, "% front");
        // 1.3.2 is only a provisional public decoder guess and is not released
        // as a writable brake-pressure control.
        add(parameters, raw, "diffPower", ParameterKey.DIFFERENTIAL_POWER, "ratio");
        add(parameters, raw, "diffCoast", ParameterKey.DIFFERENTIAL_COAST, "ratio");
        add(parameters, raw, "diffPreload", ParameterKey.DIFFERENTIAL_PRELOAD, "Nm");
        add(parameters, raw, "frontSpringRate", ParameterKey.SPRING_FRONT, "N/m");
        add(parameters, raw, "rearSpringRate", ParameterKey.SPRING_REAR, "N/m");
        add(parameters, raw, "frontBump", ParameterKey.SLOW_BUMP_FRONT, "click");
        add(parameters, raw, "rearBump", ParameterKey.SLOW_BUMP_REAR, "click");
        add(parameters, raw, "frontRebound", ParameterKey.SLOW_REBOUND_FRONT, "click");
        add(parameters, raw, "rearRebound", ParameterKey.SLOW_REBOUND_REAR, "click");
        addAxle(parameters, raw, "frontLeftTyrePressure", "frontRightTyrePressure",
                ParameterKey.TYRE_PRESSURE_FRONT, "psi");
        addAxle(parameters, raw, "rearLeftTyrePressure", "rearRightTyrePressure",
                ParameterKey.TYRE_PRESSURE_REAR, "psi");
        add(parameters, raw, "frontCamber", ParameterKey.CAMBER_FRONT, "deg");
        add(parameters, raw, "rearCamber", ParameterKey.CAMBER_REAR, "deg");
        add(parameters, raw, "frontToe", ParameterKey.TOE_FRONT, "deg");
        add(parameters, raw, "rearToe", ParameterKey.TOE_REAR, "deg");
        add(parameters, raw, "tc", ParameterKey.TRACTION_CONTROL, "click");
        add(parameters, raw, "tc2", ParameterKey.TRACTION_CONTROL_2, "click");
        add(parameters, raw, "abs", ParameterKey.ABS, "click");
        // No independently verified 0.8.1 binary field exists for engineMap.
        add(parameters, raw, "frontRideHeight", ParameterKey.RIDE_HEIGHT_FRONT, "mm");
        add(parameters, raw, "rearRideHeight", ParameterKey.RIDE_HEIGHT_REAR, "mm");
        add(parameters, raw, "frontWing", ParameterKey.FRONT_AERO, "click");
        add(parameters, raw, "rearWing", ParameterKey.REAR_WING, "click");
        add(parameters, raw, "fuel", ParameterKey.FUEL, "L");

        if (parameters.isEmpty()) {
            throw new SetupValidationException("EVO-carsetuplimits-Profil enthält keine sicher nutzbaren Bereiche");
        }
        return new EngineeringProfile(vehicle.id, GAME_VERSION, vehicle.expectedSignaturePrefix,
                datasetSha256 + ":" + vehicle.rangeKey, false, parameters);
    }

    private static void add(
            Map<ParameterKey, ParameterDefinition> target,
            JSONObject source,
            String sourceKey,
            ParameterKey key,
            String unit) {
        add(target, source, sourceKey, key, unit, true);
    }

    private static void addPhysicalArb(
            Map<ParameterKey, ParameterDefinition> target,
            JSONObject source,
            String sourceKey,
            ParameterKey key) {
        Range range = readRange(source, sourceKey);
        if (range == null) return;
        // The verified wire field stores physical stiffness. Some source rows
        // expose only UI click numbers (small single digits); those need a
        // separate per-car click-to-stiffness table and are therefore omitted.
        if (range.minimum < 1_000 || range.maximum < 1_000) return;
        target.put(key, ParameterDefinition.verifiedRangeOnly(key, range.minimum, range.maximum,
                range.step, "N/m", SOURCE + " field=" + sourceKey
                        + "; packed-float path verified by SpeedHQ/RaceIQ carsetup-writer", true));
    }

    private static void add(
            Map<ParameterKey, ParameterDefinition> target,
            JSONObject source,
            String sourceKey,
            ParameterKey key,
            String unit,
            boolean binaryWriteVerified) {
        Range range = readRange(source, sourceKey);
        if (range == null) return;
        target.put(key, ParameterDefinition.verifiedRangeOnly(key, range.minimum, range.maximum,
                range.step, unit, SOURCE + " field=" + sourceKey, binaryWriteVerified));
    }

    private static void addAxle(
            Map<ParameterKey, ParameterDefinition> target,
            JSONObject source,
            String leftKey,
            String rightKey,
            ParameterKey key,
            String unit) {
        Range left = readRange(source, leftKey);
        Range right = readRange(source, rightKey);
        if (left == null || right == null || Math.abs(left.step - right.step) > 1e-9) return;
        double minimum = Math.max(left.minimum, right.minimum);
        double maximum = Math.min(left.maximum, right.maximum);
        if (!(minimum < maximum) || !aligned(minimum, left.minimum, left.step)
                || !aligned(minimum, right.minimum, right.step)) return;
        target.put(key, ParameterDefinition.verifiedRangeOnly(key, minimum, maximum, left.step,
                unit, SOURCE + " fields=" + leftKey + "," + rightKey, true));
    }

    private static boolean aligned(double value, double origin, double step) {
        return Math.abs((value - origin) / step - Math.rint((value - origin) / step)) < 1e-6;
    }

    private static Range readRange(JSONObject source, String key) {
        if (!source.has(key) || source.isNull(key)) return null;
        JSONObject value = source.optJSONObject(key);
        if (value == null || !value.has("min") || !value.has("max") || !value.has("step")) {
            throw new SetupValidationException("Unvollständiger EVO-Wertebereich: " + key);
        }
        double minimum = value.optDouble("min", Double.NaN);
        double maximum = value.optDouble("max", Double.NaN);
        double step = value.optDouble("step", Double.NaN);
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || !Double.isFinite(step)
                || minimum >= maximum || step <= 0 || step > maximum - minimum) {
            throw new SetupValidationException("Ungültiger EVO-Wertebereich: " + key);
        }
        return new Range(minimum, maximum, step);
    }

    private static byte[] readAsset(Context context, String name) throws IOException {
        try (InputStream input = context.getAssets().open(name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private record Range(double minimum, double maximum, double step) {}
}
