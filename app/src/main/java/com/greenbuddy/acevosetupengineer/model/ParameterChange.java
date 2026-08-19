package com.greenbuddy.acevosetupengineer.model;

import java.util.Objects;

public final class ParameterChange {
    private final String displayName;
    private final String before;
    private final String after;

    public ParameterChange(String displayName, String before, String after) {
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.before = Objects.requireNonNull(before, "before");
        this.after = Objects.requireNonNull(after, "after");
    }

    public String getDisplayName() { return displayName; }
    public String getBefore() { return before; }
    public String getAfter() { return after; }
}
