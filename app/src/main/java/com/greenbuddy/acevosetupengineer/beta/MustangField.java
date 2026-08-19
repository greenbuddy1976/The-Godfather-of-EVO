package com.greenbuddy.acevosetupengineer.beta;

import com.greenbuddy.acevosetupengineer.model.SetupSection;

public enum MustangField {
    FRONT_ARB(SetupSection.MECHANICS_AND_SUSPENSION, 1, "frontArb", "Stabilisator vorn"),
    REAR_ARB(SetupSection.MECHANICS_AND_SUSPENSION, 2, "rearArb", "Stabilisator hinten"),
    STEER_RATIO(SetupSection.WHEEL_GEOMETRY, 1, "steerRatio", "Lenkübersetzung"),
    BRAKE_BIAS(SetupSection.BRAKES, 1, "brakeBias", "Bremsbalance"),
    BRAKE_PRESSURE(SetupSection.BRAKES, 2, "brakePressure", "Bremsdruck"),
    DIFF_POWER(SetupSection.MECHANICS_AND_SUSPENSION, 3, "diffPower", "Differenzial Power"),
    DIFF_COAST(SetupSection.MECHANICS_AND_SUSPENSION, 4, "diffCoast", "Differenzial Coast"),
    DIFF_PRELOAD(SetupSection.MECHANICS_AND_SUSPENSION, 5, "diffPreload", "Differenzial Vorspannung"),
    SPRING_FL(SetupSection.MECHANICS_AND_SUSPENSION, 6, "springFl", "Feder vorne links"),
    SPRING_FR(SetupSection.MECHANICS_AND_SUSPENSION, 7, "springFr", "Feder vorne rechts"),
    SPRING_RL(SetupSection.MECHANICS_AND_SUSPENSION, 8, "springRl", "Feder hinten links"),
    SPRING_RR(SetupSection.MECHANICS_AND_SUSPENSION, 9, "springRr", "Feder hinten rechts"),
    BUMP_FL(SetupSection.DAMPERS, 1, "bumpFl", "Bump vorne links"),
    BUMP_FR(SetupSection.DAMPERS, 2, "bumpFr", "Bump vorne rechts"),
    BUMP_RL(SetupSection.DAMPERS, 3, "bumpRl", "Bump hinten links"),
    BUMP_RR(SetupSection.DAMPERS, 4, "bumpRr", "Bump hinten rechts"),
    REBOUND_FL(SetupSection.DAMPERS, 5, "reboundFl", "Rebound vorne links"),
    REBOUND_FR(SetupSection.DAMPERS, 6, "reboundFr", "Rebound vorne rechts"),
    REBOUND_RL(SetupSection.DAMPERS, 7, "reboundRl", "Rebound hinten links"),
    REBOUND_RR(SetupSection.DAMPERS, 8, "reboundRr", "Rebound hinten rechts"),
    TYRE_PRESSURE_FL(SetupSection.TYRES, 1, "tyrePressureFl", "Reifendruck vorne links"),
    TYRE_PRESSURE_FR(SetupSection.TYRES, 2, "tyrePressureFr", "Reifendruck vorne rechts"),
    TYRE_PRESSURE_RL(SetupSection.TYRES, 3, "tyrePressureRl", "Reifendruck hinten links"),
    TYRE_PRESSURE_RR(SetupSection.TYRES, 4, "tyrePressureRr", "Reifendruck hinten rechts"),
    CAMBER_FL(SetupSection.WHEEL_GEOMETRY, 2, "camberFl", "Sturz vorne links"),
    CAMBER_FR(SetupSection.WHEEL_GEOMETRY, 3, "camberFr", "Sturz vorne rechts"),
    CAMBER_RL(SetupSection.WHEEL_GEOMETRY, 4, "camberRl", "Sturz hinten links"),
    CAMBER_RR(SetupSection.WHEEL_GEOMETRY, 5, "camberRr", "Sturz hinten rechts"),
    TOE_FL(SetupSection.WHEEL_GEOMETRY, 6, "toeFl", "Spur vorne links"),
    TOE_FR(SetupSection.WHEEL_GEOMETRY, 7, "toeFr", "Spur vorne rechts"),
    TOE_RL(SetupSection.WHEEL_GEOMETRY, 8, "toeRl", "Spur hinten links"),
    TOE_RR(SetupSection.WHEEL_GEOMETRY, 9, "toeRr", "Spur hinten rechts"),
    TC(SetupSection.ELECTRONICS, 1, "tc", "Traktionskontrolle"),
    TC2(SetupSection.ELECTRONICS, 2, "tc2", "Traktionskontrolle 2"),
    ABS(SetupSection.ELECTRONICS, 3, "abs", "ABS"),
    FRONT_RIDE_HEIGHT(SetupSection.AERODYNAMICS, 1, "frontRideHeight", "Fahrhöhe vorn"),
    REAR_RIDE_HEIGHT(SetupSection.AERODYNAMICS, 2, "rearRideHeight", "Fahrhöhe hinten"),
    REAR_WING(SetupSection.AERODYNAMICS, 3, "rearWing", "Heckflügel"),
    FUEL(SetupSection.FUEL, 1, "fuel", "Kraftstoff");

    private final SetupSection section;
    private final int position;
    private final String key;
    private final String displayName;
    MustangField(SetupSection section, int position, String key, String displayName) {
        this.section = section;
        this.position = position;
        this.key = key;
        this.displayName = displayName;
    }
    public SetupSection section() { return section; }
    public int position() { return position; }
    public String key() { return key; }
    public String displayName() { return displayName; }
}
