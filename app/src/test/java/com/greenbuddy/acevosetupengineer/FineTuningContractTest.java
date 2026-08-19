package com.greenbuddy.acevosetupengineer;

import com.greenbuddy.acevosetupengineer.model.FineTuningProblem;
import com.greenbuddy.acevosetupengineer.model.FineTuningStrength;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public final class FineTuningContractTest {
    @Test public void exactMenuContainsDefaultPlusEightProblems() {
        assertEquals(9, FineTuningProblem.values().length);
        assertEquals("Kein Fine-Tuning", FineTuningProblem.values()[0].getDisplayName());
        assertEquals(Arrays.asList(
                "Kein Fine-Tuning",
                "Heck beim Bremsen instabil",
                "Untersteuern beim Einlenken",
                "Heck beim Herausbeschleunigen nervös",
                "Kerbs oder Kuppen machen das Auto unruhig",
                "Auto lenkt zu träge ein",
                "Hoher Reifenverschleiß",
                "Heck bei hoher Geschwindigkeit nervös",
                "Mehr Topspeed gewünscht"),
                Arrays.asList(Arrays.stream(FineTuningProblem.values())
                        .map(FineTuningProblem::getDisplayName).toArray(String[]::new)));
    }

    @Test public void eachProblemHasThreeStrengthContracts() {
        int cases = 0;
        for (FineTuningProblem problem : FineTuningProblem.values()) {
            if (problem == FineTuningProblem.NONE) continue;
            for (FineTuningStrength strength : FineTuningStrength.values()) {
                assertTrue(strength.getLevel() >= 1 && strength.getLevel() <= 3);
                cases++;
            }
        }
        assertEquals(24, cases);
    }
}
