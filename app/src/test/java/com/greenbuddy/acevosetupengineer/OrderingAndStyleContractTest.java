package com.greenbuddy.acevosetupengineer;

import com.greenbuddy.acevosetupengineer.model.SetupSection;
import com.greenbuddy.acevosetupengineer.model.SetupStyle;
import org.junit.Test;
import static org.junit.Assert.*;

public final class OrderingAndStyleContractTest {
    @Test public void fiveStylesArePresent() { assertEquals(5, SetupStyle.values().length); }

    @Test public void sectionsHaveRequiredDisplayOrder() {
        SetupSection[] sections = SetupSection.values();
        assertEquals(8, sections.length);
        for (int index = 0; index < sections.length; index++) {
            assertEquals(index + 1, sections[index].getOrder());
        }
    }
}
