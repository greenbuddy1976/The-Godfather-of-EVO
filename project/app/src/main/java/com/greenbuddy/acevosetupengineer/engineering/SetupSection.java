package com.greenbuddy.acevosetupengineer.engineering;

/**
 * Neutral Fine-Tuning headings. They define where a parameter belongs, never a value.
 */
public enum SetupSection {
    TYRES("Reifen", 1),
    WHEEL_ALIGNMENT("Radgeometrie", 2),
    ELECTRONICS("Elektronik", 3),
    BRAKES("Bremsen", 4),
    FUEL("Kraftstoff", 5),
    MECHANICS("Mechanik / Fahrwerk", 6),
    DAMPERS("Dämpfer", 7),
    AERODYNAMICS("Aerodynamik", 8);

    public final String displayName;
    public final int fineTuneOrder;

    SetupSection(String displayName, int fineTuneOrder) {
        this.displayName = displayName;
        this.fineTuneOrder = fineTuneOrder;
    }
}
