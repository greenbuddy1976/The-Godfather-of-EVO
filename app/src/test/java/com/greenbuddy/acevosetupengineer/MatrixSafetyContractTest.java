package com.greenbuddy.acevosetupengineer;

import com.greenbuddy.acevosetupengineer.data.OfficialInventory;
import com.greenbuddy.acevosetupengineer.engine.SetupGenerationService;
import com.greenbuddy.acevosetupengineer.model.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class MatrixSafetyContractTest {
    @Test public void publicTreeNeverManufacturesAnyOf12425Binaries() {
        SetupGenerationService service = new SetupGenerationService(null);
        int combinations = 0;
        for (CarIdentity car : OfficialInventory.cars()) {
            for (TrackLayout layout : OfficialInventory.layouts()) {
                for (SetupStyle style : SetupStyle.values()) {
                    SetupRequest request = new SetupRequest(car, layout, style,
                            FineTuningProblem.NONE, FineTuningStrength.ONE,
                            OfficialInventory.GAME_VERSION);
                    GenerationOutcome outcome = service.generate(request);
                    assertEquals(GenerationOutcome.State.BLOCKED_NOT_VERIFIED, outcome.getState());
                    assertNull(outcome.getSetup());
                    assertFalse(outcome.isExportable());
                    combinations++;
                }
            }
        }
        assertEquals(12425, combinations);
    }
}
