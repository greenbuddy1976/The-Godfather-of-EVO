package com.greenbuddy.acevosetupengineer.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class RacePlacePackageProviderTest {
    @Test
    public void mapsVerifiedVehicleFolders() {
        assertEquals("ferrari-296-gt3", RacePlacePackageProvider.vehicleSlug(
                "RacePlace/Car Setups/Ferrari 296 GT3/Monza/Baseline_MON_26_opt.carsetup"));
        assertEquals("bmw-m4-gt3-evo", RacePlacePackageProvider.vehicleSlug(
                "RacePlace/Car Setups/BMW M4 GT3 Evo/Monza/Baseline_MON_26_opt.carsetup"));
    }

    @Test
    public void mapsOnlyExactKnownLayouts() {
        assertEquals("brands-hatch-indy", RacePlacePackageProvider.layoutSlug(
                "Car Setups/Ford Mustang GT3/Brands Hatch/Baseline-BHTC_INDY-26-opt.carsetup"));
        assertEquals("brands-hatch", RacePlacePackageProvider.layoutSlug(
                "Car Setups/Ferrari 296 GT3/Brands Hatch/Baseline-BHATCH-26-opt.carsetup"));
        assertEquals("watkins-glen-short-inner-loop", RacePlacePackageProvider.layoutSlug(
                "Car Setups/Audi R8/Watkins Glen/Baseline-WAT-SIL-26-opt.carsetup"));
        assertEquals("", RacePlacePackageProvider.layoutSlug(
                "Car Setups/Audi R8/Unknown/Custom-Test.carsetup"));
    }
}
