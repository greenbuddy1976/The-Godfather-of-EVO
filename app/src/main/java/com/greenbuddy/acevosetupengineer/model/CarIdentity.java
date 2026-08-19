package com.greenbuddy.acevosetupengineer.model;

import java.util.Objects;

public final class CarIdentity {
    private final String id;
    private final String displayName;
    private final String gameVersion;
    private final String provenance;

    public CarIdentity(String id, String displayName, String gameVersion, String provenance) {
        this.id = require(id, "id");
        this.displayName = require(displayName, "displayName");
        this.gameVersion = require(gameVersion, "gameVersion");
        this.provenance = require(provenance, "provenance");
    }

    private static String require(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name);
        return value;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getGameVersion() { return gameVersion; }
    public String getProvenance() { return provenance; }

    @Override public String toString() { return displayName; }
    @Override public boolean equals(Object value) {
        return value instanceof CarIdentity && id.equals(((CarIdentity) value).id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
