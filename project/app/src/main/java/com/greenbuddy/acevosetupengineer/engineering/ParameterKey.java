package com.greenbuddy.acevosetupengineer.engineering;

public enum ParameterKey {
    TYRE_PRESSURE_FRONT(SetupSection.TYRES, "Reifendruck vorn"),
    TYRE_PRESSURE_REAR(SetupSection.TYRES, "Reifendruck hinten"),
    CAMBER_FRONT(SetupSection.WHEEL_ALIGNMENT, "Sturz vorn"),
    CAMBER_REAR(SetupSection.WHEEL_ALIGNMENT, "Sturz hinten"),
    TOE_FRONT(SetupSection.WHEEL_ALIGNMENT, "Spur vorn"),
    TOE_REAR(SetupSection.WHEEL_ALIGNMENT, "Spur hinten"),
    ABS(SetupSection.ELECTRONICS, "ABS"),
    TRACTION_CONTROL(SetupSection.ELECTRONICS, "Traktionskontrolle"),
    TRACTION_CONTROL_2(SetupSection.ELECTRONICS, "Traktionskontrolle 2"),
    ENGINE_MAP(SetupSection.ELECTRONICS, "Motorkennfeld"),
    BRAKE_BIAS(SetupSection.BRAKES, "Bremsbalance"),
    BRAKE_PRESSURE(SetupSection.BRAKES, "Bremsdruck"),
    FUEL(SetupSection.FUEL, "Kraftstoff"),
    DIFFERENTIAL_PRELOAD(SetupSection.MECHANICS, "Differenzial Vorspannung"),
    DIFFERENTIAL_POWER(SetupSection.MECHANICS, "Differenzial Zug"),
    DIFFERENTIAL_COAST(SetupSection.MECHANICS, "Differenzial Schub"),
    STEERING_RATIO(SetupSection.MECHANICS, "Lenkübersetzung"),
    ANTI_ROLL_BAR_FRONT(SetupSection.MECHANICS, "Stabilisator vorn"),
    ANTI_ROLL_BAR_REAR(SetupSection.MECHANICS, "Stabilisator hinten"),
    SPRING_FRONT(SetupSection.MECHANICS, "Feder vorn"),
    SPRING_REAR(SetupSection.MECHANICS, "Feder hinten"),
    RIDE_HEIGHT_FRONT(SetupSection.MECHANICS, "Fahrhöhe vorn"),
    RIDE_HEIGHT_REAR(SetupSection.MECHANICS, "Fahrhöhe hinten"),
    SLOW_BUMP_FRONT(SetupSection.DAMPERS, "Langsame Druckstufe vorn"),
    SLOW_BUMP_REAR(SetupSection.DAMPERS, "Langsame Druckstufe hinten"),
    SLOW_REBOUND_FRONT(SetupSection.DAMPERS, "Langsame Zugstufe vorn"),
    SLOW_REBOUND_REAR(SetupSection.DAMPERS, "Langsame Zugstufe hinten"),
    FAST_BUMP_FRONT(SetupSection.DAMPERS, "Schnelle Druckstufe vorn"),
    FAST_BUMP_REAR(SetupSection.DAMPERS, "Schnelle Druckstufe hinten"),
    FAST_REBOUND_FRONT(SetupSection.DAMPERS, "Schnelle Zugstufe vorn"),
    FAST_REBOUND_REAR(SetupSection.DAMPERS, "Schnelle Zugstufe hinten"),
    FRONT_AERO(SetupSection.AERODYNAMICS, "Aero vorn"),
    REAR_WING(SetupSection.AERODYNAMICS, "Heckflügel");

    public final SetupSection section;
    public final String displayName;

    ParameterKey(SetupSection section, String displayName) {
        this.section = section;
        this.displayName = displayName;
    }
}
