package com.greenbuddy.acevosetupengineer.model;

public enum SetupMode {
    HOTLAP_ATTACK("Michael Schumacher", "Very Fast", 1.0),
    FAST_CONTROL("Walter Röhrl", "Fast", 0.55),
    STABLE_LONGRUN("Dieter Düsel", "Normal", -0.25),
    SAFE("Oma Hertha", "Entspannt", -0.70);

    public final String buttonLabel;
    public final String subtitle;
    public final double engineeringFactor;

    SetupMode(String buttonLabel, String subtitle, double engineeringFactor) {
        this.buttonLabel = buttonLabel;
        this.subtitle = subtitle;
        this.engineeringFactor = engineeringFactor;
    }
}
