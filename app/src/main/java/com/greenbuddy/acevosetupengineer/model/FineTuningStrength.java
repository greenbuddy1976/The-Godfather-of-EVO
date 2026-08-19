package com.greenbuddy.acevosetupengineer.model;

public enum FineTuningStrength {
    ONE(1, "Stärke 1 – kleine sichere Änderung"),
    TWO(2, "Stärke 2 – deutlich spürbare Änderung"),
    THREE(3, "Stärke 3 – maximale technisch sichere Änderung");

    private final int level;
    private final String displayName;
    FineTuningStrength(int level, String displayName) {
        this.level = level;
        this.displayName = displayName;
    }
    public int getLevel() { return level; }
    public String getDisplayName() { return displayName; }
    @Override public String toString() { return displayName; }
}
