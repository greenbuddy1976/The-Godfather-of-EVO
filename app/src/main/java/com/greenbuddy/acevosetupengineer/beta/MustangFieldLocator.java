package com.greenbuddy.acevosetupengineer.beta;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Writer-side path resolver. The decoder below resolves paths independently. */
final class MustangFieldLocator {
    private MustangFieldLocator() { }

    static EnumMap<MustangField, Integer> locate(byte[] bytes) {
        ProtoWire.Message top = ProtoWire.parse(bytes, 0, bytes.length);
        EnumMap<MustangField, Integer> map = new EnumMap<>(MustangField.class);

        ProtoWire.Message chassis = nested(top, 1);
        ProtoWire.Field arbs = chassis.one(1, 2);
        if (arbs.length != 8) throw new IllegalArgumentException("Expected two packed ARB floats");
        map.put(MustangField.FRONT_ARB, arbs.payload);
        map.put(MustangField.REAR_ARB, arbs.payload + 4);
        map.put(MustangField.STEER_RATIO, chassis.one(2, 5).payload);
        ProtoWire.Message brakes = nested(chassis, 3);
        map.put(MustangField.BRAKE_BIAS, brakes.one(1, 5).payload);
        map.put(MustangField.BRAKE_PRESSURE, brakes.one(2, 5).payload);
        ProtoWire.Message diff = nested(chassis, 4);
        map.put(MustangField.DIFF_POWER, diff.one(1, 5).payload);
        map.put(MustangField.DIFF_COAST, diff.one(2, 5).payload);
        map.put(MustangField.DIFF_PRELOAD, diff.one(3, 5).payload);

        MustangField[] springs = {MustangField.SPRING_FL, MustangField.SPRING_FR,
                MustangField.SPRING_RL, MustangField.SPRING_RR};
        List<ProtoWire.Field> springFields = top.fields(2);
        requireFour(springFields, 2);
        for (int index = 0; index < 4; index++) {
            map.put(springs[index], top.message(springFields.get(index)).one(1, 5).payload);
        }

        MustangField[] bumps = {MustangField.BUMP_FL, MustangField.BUMP_FR,
                MustangField.BUMP_RL, MustangField.BUMP_RR};
        MustangField[] rebounds = {MustangField.REBOUND_FL, MustangField.REBOUND_FR,
                MustangField.REBOUND_RL, MustangField.REBOUND_RR};
        List<ProtoWire.Field> dampers = top.fields(3);
        requireFour(dampers, 3);
        for (int index = 0; index < 4; index++) {
            ProtoWire.Message damper = top.message(dampers.get(index));
            map.put(bumps[index], damper.one(1, 5).payload);
            map.put(rebounds[index], damper.one(3, 5).payload);
        }

        MustangField[] pressures = {MustangField.TYRE_PRESSURE_FL, MustangField.TYRE_PRESSURE_FR,
                MustangField.TYRE_PRESSURE_RL, MustangField.TYRE_PRESSURE_RR};
        MustangField[] cambers = {MustangField.CAMBER_FL, MustangField.CAMBER_FR,
                MustangField.CAMBER_RL, MustangField.CAMBER_RR};
        MustangField[] toes = {MustangField.TOE_FL, MustangField.TOE_FR,
                MustangField.TOE_RL, MustangField.TOE_RR};
        List<ProtoWire.Field> wheels = top.fields(4);
        requireFour(wheels, 4);
        for (int index = 0; index < 4; index++) {
            ProtoWire.Message wheel = top.message(wheels.get(index));
            map.put(pressures[index], wheel.one(1, 5).payload);
            map.put(cambers[index], wheel.one(2, 5).payload);
            map.put(toes[index], wheel.one(3, 5).payload);
        }

        ProtoWire.Message electronics = nested(top, 5);
        map.put(MustangField.TC, electronics.one(1, 5).payload);
        map.put(MustangField.TC2, electronics.one(2, 5).payload);
        map.put(MustangField.ABS, electronics.one(3, 5).payload);
        ProtoWire.Message aero = nested(top, 6);
        map.put(MustangField.FRONT_RIDE_HEIGHT, aero.one(2, 5).payload);
        map.put(MustangField.REAR_RIDE_HEIGHT, aero.one(3, 5).payload);
        map.put(MustangField.REAR_WING, aero.one(5, 5).payload);
        ProtoWire.Message fuel = nested(top, 7);
        map.put(MustangField.FUEL, fuel.one(1, 5).payload);

        if (map.size() != MustangField.values().length) {
            throw new IllegalArgumentException("Incomplete Mustang field map");
        }
        return map;
    }

    private static ProtoWire.Message nested(ProtoWire.Message parent, int field) {
        return parent.message(parent.one(field, 2));
    }

    private static void requireFour(List<ProtoWire.Field> fields, int number) {
        if (fields.size() != 4) throw new IllegalArgumentException("Expected four field " + number);
        for (ProtoWire.Field field : fields) {
            if (field.wireType != 2) throw new IllegalArgumentException("Repeated message wire type");
        }
    }
}
