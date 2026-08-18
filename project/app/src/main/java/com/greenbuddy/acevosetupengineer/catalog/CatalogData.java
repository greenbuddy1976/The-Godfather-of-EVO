package com.greenbuddy.acevosetupengineer.catalog;

import com.greenbuddy.acevosetupengineer.model.CatalogItem;

import java.util.Collections;
import java.util.List;

public final class CatalogData {
    public final String gameVersion;
    public final List<CatalogItem> vehicles;
    public final List<CatalogItem> layouts;

    public CatalogData(String gameVersion, List<CatalogItem> vehicles, List<CatalogItem> layouts) {
        this.gameVersion = gameVersion;
        this.vehicles = Collections.unmodifiableList(vehicles);
        this.layouts = Collections.unmodifiableList(layouts);
    }
}
