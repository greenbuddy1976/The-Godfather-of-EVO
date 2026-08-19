package com.greenbuddy.acevosetupengineer.model;

public enum FineTuningProblem {
    NONE("Kein Fine-Tuning"),
    BRAKING_REAR_INSTABILITY("Heck beim Bremsen instabil"),
    TURN_IN_UNDERSTEER("Untersteuern beim Einlenken"),
    EXIT_REAR_NERVOUS("Heck beim Herausbeschleunigen nervös"),
    KERBS_OR_CRESTS_UNSETTLED("Kerbs oder Kuppen machen das Auto unruhig"),
    SLOW_TURN_IN("Auto lenkt zu träge ein"),
    HIGH_TYRE_WEAR("Hoher Reifenverschleiß"),
    HIGH_SPEED_REAR_NERVOUS("Heck bei hoher Geschwindigkeit nervös"),
    MORE_TOP_SPEED("Mehr Topspeed gewünscht");

    private final String displayName;
    FineTuningProblem(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
