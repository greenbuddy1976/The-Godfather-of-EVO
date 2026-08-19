package com.greenbuddy.acevosetupengineer.model;

public enum SetupSection {
    TYRES(1, "Reifen"),
    WHEEL_GEOMETRY(2, "Radgeometrie"),
    ELECTRONICS(3, "Elektronik"),
    BRAKES(4, "Bremsen"),
    FUEL(5, "Kraftstoff"),
    MECHANICS_AND_SUSPENSION(6, "Mechanik und Fahrwerk"),
    DAMPERS(7, "Dämpfer"),
    AERODYNAMICS(8, "Aerodynamik");

    private final int order;
    private final String displayName;
    SetupSection(int order, String displayName) {
        this.order = order;
        this.displayName = displayName;
    }
    public int getOrder() { return order; }
    public String getDisplayName() { return displayName; }
}
