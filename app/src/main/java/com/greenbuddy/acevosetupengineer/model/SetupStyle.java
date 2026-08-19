package com.greenbuddy.acevosetupengineer.model;

public enum SetupStyle {
    FAST_CONTROL("FAST CONTROL – Walter Röhrl"),
    FAST_ATTACK("FAST ATTACK – Michael Schumacher"),
    FAST_STABLE("FAST STABLE – Dieter Düsel"),
    FAST_SAFE("FAST SAFE – Oma Hertha"),
    FAST_LONG_RUN("FAST LONG RUN");

    private final String displayName;
    SetupStyle(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
