package com.greenbuddy.acevosetupengineer;

import com.greenbuddy.acevosetupengineer.data.OfficialInventory;
import com.greenbuddy.acevosetupengineer.model.CarIdentity;
import com.greenbuddy.acevosetupengineer.model.TrackLayout;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public final class OfficialInventoryTest {
    @Test public void inventoryHasRequiredVersionBoundCounts() {
        assertEquals(71, OfficialInventory.cars().size());
        assertEquals(35, OfficialInventory.layouts().size());
        assertEquals("0.8.1", OfficialInventory.GAME_VERSION);
    }

    @Test public void explicitlyRequiredCarsExist() {
        assertNotNull(OfficialInventory.requireCar("bmw_m2_coupe_g87"));
        assertNotNull(OfficialInventory.requireCar("bmw_m2_cs_racing"));
        assertNotNull(OfficialInventory.requireCar("bmw_m3_e30_sport_evo"));
        assertNotNull(OfficialInventory.requireCar("bmw_m3_e46_csl"));
        assertNotNull(OfficialInventory.requireCar("ford_mustang_gt3"));
    }

    @Test public void unverifiedSuzukaWestIsNotPresentedAsSupported() {
        try {
            OfficialInventory.requireLayout("suzuka_west");
            fail("Suzuka West lacks direct primary verification and must remain excluded");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("suzuka_west"));
        }
    }

    @Test public void displayIdentitiesAndExactLayoutPairsAreUnique() {
        Set<String> carNames = new HashSet<>();
        for (CarIdentity car : OfficialInventory.cars()) {
            assertTrue("Duplicate car display identity: " + car.getDisplayName(),
                    carNames.add(car.getDisplayName()));
        }
        Set<String> layoutPairs = new HashSet<>();
        for (TrackLayout layout : OfficialInventory.layouts()) {
            assertTrue("Duplicate track/layout pair: " + layout.getDisplayName(),
                    layoutPairs.add(layout.getDisplayName()));
        }
    }
}
