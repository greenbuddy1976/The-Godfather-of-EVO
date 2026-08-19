package com.greenbuddy.godfatherlive;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public final class QueryLogicTest {
    @Test public void exactMatchingNeverAcceptsDonorCarsOrSimilarLayouts() {
        assertTrue(QueryLogic.exact("Nürburgring GP", "Nurburgring GP"));
        assertTrue(QueryLogic.exact("Spa-Francorchamps", "SPA Francorchamps"));
        assertFalse(QueryLogic.exact("Ford Mustang GT3", "Ford Mustang"));
        assertFalse(QueryLogic.exact("Nürburgring GP", "Nürburgring Sprint"));
        assertFalse(QueryLogic.exact("Nordschleife", "Nordschleife Touristenfahrten"));
    }

    @Test public void usefulAliasesAndYearSuffixesNormalize() {
        assertTrue(QueryLogic.exact("Mazda MX-5 NA (1994)", "Mazda MX5 NA"));
        assertTrue(QueryLogic.exact("SPA Francorchamps", "Spa-Francorchamps"));
        assertTrue(QueryLogic.exact("Nürburgring GP", "Nuerburgring GP"));
    }

    @Test public void onlyCurrentZeroEightFilesAreAccepted() {
        assertTrue(QueryLogic.currentVersion("0.8"));
        assertTrue(QueryLogic.currentVersion("v0.8.1"));
        assertFalse(QueryLogic.currentVersion("0.7.1"));
        assertFalse(QueryLogic.currentVersion(""));
    }

    @Test public void saveNameIsAlwaysCarsetupNeverText() {
        assertEquals("Mustang Spa.carsetup",
                QueryLogic.safeCarsetupName("Mustang Spa.txt", "Ford Mustang GT3", "Spa"));
        assertEquals("race.carsetup",
                QueryLogic.safeCarsetupName("race.carsetup", "Ford Mustang GT3", "Spa"));
        assertTrue(QueryLogic.safeCarsetupName("", "Ford Mustang GT3", "Spa")
                .endsWith(".carsetup"));
    }

    @Test public void generatedNamesContainCarTrackProfileAndCarsetupExtension() {
        String name = QueryLogic.generatedName(
                "Ford Mustang GT3", "Nordschleife", SetupEngine.Profile.STABLE_FAST);
        assertTrue(name.contains("Ford Mustang GT3"));
        assertTrue(name.contains("Nordschleife"));
        assertTrue(name.endsWith(".carsetup"));
    }

    @Test public void binaryGuardRejectsWebErrorsAndZipFiles() {
        byte[] html = ("<html>" + "x".repeat(40)).getBytes(StandardCharsets.UTF_8);
        byte[] json = ("{\"error\":\"" + "x".repeat(40) + "\"}").getBytes(StandardCharsets.UTF_8);
        byte[] zip = new byte[64];
        zip[0] = 'P'; zip[1] = 'K';
        assertThrows(IllegalArgumentException.class, () -> QueryLogic.requireRealCarsetup(html));
        assertThrows(IllegalArgumentException.class, () -> QueryLogic.requireRealCarsetup(json));
        assertThrows(IllegalArgumentException.class, () -> QueryLogic.requireRealCarsetup(zip));
    }
}
