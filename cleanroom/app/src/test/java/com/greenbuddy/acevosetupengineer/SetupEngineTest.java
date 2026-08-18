package com.greenbuddy.acevosetupengineer;

import org.junit.Test;
import static org.junit.Assert.*;

public final class SetupEngineTest {
    @Test public void inventoryIsCompleteAndUnique() {
        assertEquals(71, Catalog.CARS.length);
        assertEquals(71, java.util.Set.of(Catalog.CARS).size());
        assertEquals(24, Catalog.TRACKS.length);
        assertEquals(24, java.util.Set.of(Catalog.TRACKS).size());
        assertEquals(5, SetupEngine.Mode.values().length);
    }

    @Test public void mustangAttackAlwaysUsesTcOneOnEveryTrack() {
        String[] neutral = new String[8];
        java.util.Arrays.fill(neutral, "Test: Standard");
        for (String track : Catalog.TRACKS) {
            String plan = SetupEngine.build("Ford Mustang GT3", track, SetupEngine.Mode.FAST_ATTACK, neutral);
            assertTrue(track, plan.contains("TC 1 (Mustang-Pflicht auf jeder Strecke)"));
        }
    }

    @Test public void allCombinationsProduceANonEmptyStablePlan() {
        String[] neutral = new String[8];
        java.util.Arrays.fill(neutral, "Test: Standard");
        for (String car : Catalog.CARS) for (String track : Catalog.TRACKS) {
            for (SetupEngine.Mode mode : SetupEngine.Mode.values()) {
                String plan = SetupEngine.build(car, track, mode, neutral);
                assertTrue(plan.length() > 500);
                assertTrue(plan.contains(car));
                assertTrue(plan.contains(track));
                assertFalse(plan.toLowerCase().contains("fallback"));
                assertFalse(plan.toLowerCase().contains("notfall"));
            }
        }
    }
}
