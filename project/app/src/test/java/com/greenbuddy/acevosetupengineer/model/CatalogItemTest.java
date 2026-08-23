package com.greenbuddy.acevosetupengineer.model;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CatalogItemTest {
    @Test
    public void acceptsOnlyVerifiedOfficialThumbnailReferences() {
        CatalogItem verified = item(
                "https://assettocorsa.gg/wp-content/uploads/car-300x169.jpg",
                "https://assettocorsa.gg/wp-json/wp/v2/media/123",
                true);
        assertTrue(verified.hasVerifiedThumbnail());
    }

    @Test
    public void rejectsUnverifiedOrForeignThumbnailReferences() {
        assertFalse(item("https://example.com/car.jpg",
                "https://assettocorsa.gg/wp-json/wp/v2/media/123", true).hasVerifiedThumbnail());
        assertFalse(item("https://assettocorsa.gg/wp-content/uploads/car.jpg",
                "https://assettocorsa.gg/wp-json/wp/v2/media/123", false).hasVerifiedThumbnail());
    }

    private static CatalogItem item(String thumbnail, String source, boolean verified) {
        return new CatalogItem(CatalogItem.Kind.VEHICLE, "car-id", "Car", "car-id",
                "ks_car_", "car", thumbnail, source, "Fahrzeugbild: Car",
                verified, true, true);
    }
}
