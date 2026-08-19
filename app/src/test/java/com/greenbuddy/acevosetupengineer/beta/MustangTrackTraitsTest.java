package com.greenbuddy.acevosetupengineer.beta;

import com.greenbuddy.acevosetupengineer.data.OfficialInventory;
import com.greenbuddy.acevosetupengineer.model.TrackLayout;
import org.junit.Test;
import static org.junit.Assert.*;

public final class MustangTrackTraitsTest {
    @Test public void everyExactInventoryLayoutHasOneExplicitTrait() {
        assertEquals(35, MustangTrackTraits.all().size());
        for (TrackLayout layout : OfficialInventory.layouts()) {
            assertNotNull(layout.getId(), MustangTrackTraits.require(layout.getId()));
        }
    }
}
