package com.greenbuddy.acevosetupengineer.beta;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

final class MustangRange {
    final float min;
    final float max;
    final float step;

    private MustangRange(float min, float max, float step) {
        this.min = min; this.max = max; this.step = step;
    }

    float snapClamp(float value) {
        float clamped = clamp(value);
        float snapped = min + Math.round((clamped - min) / step) * step;
        return Math.max(min, Math.min(max, snapped));
    }

    float clamp(float value) { return Math.max(min, Math.min(max, value)); }

    boolean acceptsBounds(float value) {
        return Float.isFinite(value) && value >= min - tolerance() && value <= max + tolerance();
    }

    boolean accepts(float value) {
        if (!acceptsBounds(value)) return false;
        float snapped = snapClamp(value);
        return Math.abs(snapped - value) <= tolerance();
    }

    private float tolerance() { return Math.max(0.002f, step / 500f); }

    static Map<MustangField, MustangRange> all() {
        EnumMap<MustangField, MustangRange> map = new EnumMap<>(MustangField.class);
        put(map, MustangField.FRONT_ARB, 20000, 60000, 1000);
        put(map, MustangField.REAR_ARB, 10000, 50000, 1000);
        put(map, MustangField.STEER_RATIO, 11.3f, 18.299999f, 1);
        put(map, MustangField.BRAKE_BIAS, 50, 80, .2f);
        put(map, MustangField.BRAKE_PRESSURE, 80, 100, 1);
        put(map, MustangField.DIFF_POWER, .1f, .6f, .05f);
        put(map, MustangField.DIFF_COAST, .1f, .6f, .05f);
        put(map, MustangField.DIFF_PRELOAD, 20, 300, 10);
        for (MustangField field : new MustangField[]{MustangField.TYRE_PRESSURE_FL,
                MustangField.TYRE_PRESSURE_FR, MustangField.TYRE_PRESSURE_RL,
                MustangField.TYRE_PRESSURE_RR}) put(map, field, 20, 35, .1f);
        put(map, MustangField.CAMBER_FL, -4, -2.5f, .1f);
        put(map, MustangField.CAMBER_FR, -4, -2.5f, .1f);
        put(map, MustangField.CAMBER_RL, -3.5f, -2, .1f);
        put(map, MustangField.CAMBER_RR, -3.5f, -2, .1f);
        for (MustangField field : new MustangField[]{MustangField.TOE_FL,
                MustangField.TOE_FR, MustangField.TOE_RL, MustangField.TOE_RR}) {
            put(map, field, -.2f, .2f, .01f);
        }
        put(map, MustangField.TC, 0, 12, 1);
        put(map, MustangField.TC2, 0, 11, 1);
        put(map, MustangField.ABS, 0, 11, 1);
        put(map, MustangField.FRONT_RIDE_HEIGHT, 50, 85, 1);
        put(map, MustangField.REAR_RIDE_HEIGHT, 50, 85, 1);
        put(map, MustangField.REAR_WING, 0, 7, 1);
        put(map, MustangField.FUEL, 1, 120, 1);
        return Collections.unmodifiableMap(map);
    }

    private static void put(Map<MustangField, MustangRange> map, MustangField field,
                            float min, float max, float step) {
        map.put(field, new MustangRange(min, max, step));
    }
}
