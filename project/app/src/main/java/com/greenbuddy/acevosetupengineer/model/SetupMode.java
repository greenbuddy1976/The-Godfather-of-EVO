package com.greenbuddy.acevosetupengineer.model;

public enum SetupMode {
    FAST_CONTROL("FAST CONTROL", "Empfohlen: schnell + berechenbar", 0.78, 0.72, 0.20),
    FAST_ATTACK("FAST ATTACK", "Qualifying / Hotlap · Mustang GT3: TC 1", 1.00, 0.28, 0.00),
    FAST_STABLE("FAST STABLE", "Ruhiges Heck + Curbs", 0.65, 0.88, 0.25),
    FAST_SAFE("FAST SAFE", "Maximale Sicherheitsreserve", 0.45, 1.00, 0.35),
    FAST_LONG_RUN("FAST LONG RUN", "Konstant + reifenschonend", 0.68, 0.82, 1.00);

    public final String buttonLabel;
    public final String subtitle;
    public final double paceFactor;
    public final double stabilityFactor;
    public final double enduranceFactor;

    SetupMode(
            String buttonLabel,
            String subtitle,
            double paceFactor,
            double stabilityFactor,
            double enduranceFactor) {
        this.buttonLabel = buttonLabel;
        this.subtitle = subtitle;
        this.paceFactor = paceFactor;
        this.stabilityFactor = stabilityFactor;
        this.enduranceFactor = enduranceFactor;
    }
}
