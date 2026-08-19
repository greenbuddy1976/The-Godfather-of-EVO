package com.greenbuddy.acevosetupengineer.model;

public enum ResultLabel {
    VERIFIED("VERIFIZIERT"),
    LIVE_EXACT("LIVE EXACT"),
    ENGINEERING_MODEL("ENGINEERING MODEL"),
    NOT_SAFE("NICHT SICHER"),
    TECHNICAL_LIVE_ERROR("TECHNISCHER LIVE-FEHLER");

    private final String displayName;
    ResultLabel(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
