package com.greenbuddy.acevosetupengineer.model;

import java.util.Objects;

public final class SetupValue implements Comparable<SetupValue> {
    private final SetupSection section;
    private final int position;
    private final String key;
    private final String displayName;
    private final String formattedValue;
    private final boolean adjustable;

    public SetupValue(SetupSection section, int position, String key, String displayName,
                      String formattedValue, boolean adjustable) {
        this.section = Objects.requireNonNull(section, "section");
        this.position = position;
        this.key = Objects.requireNonNull(key, "key");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.formattedValue = Objects.requireNonNull(formattedValue, "formattedValue");
        this.adjustable = adjustable;
    }

    public SetupSection getSection() { return section; }
    public int getPosition() { return position; }
    public String getKey() { return key; }
    public String getDisplayName() { return displayName; }
    public String getFormattedValue() { return formattedValue; }
    public boolean isAdjustable() { return adjustable; }

    @Override public int compareTo(SetupValue other) {
        int bySection = Integer.compare(section.getOrder(), other.section.getOrder());
        return bySection != 0 ? bySection : Integer.compare(position, other.position);
    }
}
