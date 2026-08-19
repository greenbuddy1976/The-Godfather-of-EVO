package com.greenbuddy.acevosetupengineer.model;

import java.util.Objects;

public final class TrackLayout {
    private final String id;
    private final String trackName;
    private final String layoutName;
    private final String gameVersion;
    private final String provenance;

    public TrackLayout(String id, String trackName, String layoutName,
                       String gameVersion, String provenance) {
        this.id = require(id, "id");
        this.trackName = require(trackName, "trackName");
        this.layoutName = require(layoutName, "layoutName");
        this.gameVersion = require(gameVersion, "gameVersion");
        this.provenance = require(provenance, "provenance");
    }

    private static String require(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name);
        return value;
    }

    public String getId() { return id; }
    public String getTrackName() { return trackName; }
    public String getLayoutName() { return layoutName; }
    public String getGameVersion() { return gameVersion; }
    public String getProvenance() { return provenance; }
    public String getDisplayName() { return trackName + " – " + layoutName; }

    @Override public String toString() { return getDisplayName(); }
    @Override public boolean equals(Object value) {
        return value instanceof TrackLayout && id.equals(((TrackLayout) value).id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
