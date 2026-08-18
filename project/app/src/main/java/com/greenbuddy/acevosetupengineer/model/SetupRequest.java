package com.greenbuddy.acevosetupengineer.model;

public final class SetupRequest {
    public final CatalogItem vehicle;
    public final CatalogItem layout;
    public final SetupMode mode;
    public final String gameVersion;

    public SetupRequest(CatalogItem vehicle, CatalogItem layout, SetupMode mode, String gameVersion) {
        this.vehicle = vehicle;
        this.layout = layout;
        this.mode = mode;
        this.gameVersion = gameVersion;
    }

    public String cacheKey() {
        return vehicle.id + "__" + layout.id + "__" + mode.name() + "__" + gameVersion;
    }
}
