package com.greenbuddy.acevosetupengineer;

import com.greenbuddy.acevosetupengineer.data.OfficialInventory;
import com.greenbuddy.acevosetupengineer.engine.OutputFileName;
import com.greenbuddy.acevosetupengineer.model.FineTuningProblem;
import com.greenbuddy.acevosetupengineer.model.FineTuningStrength;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.model.SetupStyle;
import org.junit.Test;

import static org.junit.Assert.*;

public final class ExportContractTest {
    @Test public void fileNameContainsExactIdentitiesStyleAndVersion() {
        SetupRequest request = new SetupRequest(
                OfficialInventory.requireCar("ford_mustang_gt3"),
                OfficialInventory.requireLayout("spa_gp"), SetupStyle.FAST_ATTACK,
                FineTuningProblem.NONE, FineTuningStrength.ONE, OfficialInventory.GAME_VERSION);
        assertEquals("ford_mustang_gt3_spa_gp_fast_attack_0.8.1.carsetup",
                OutputFileName.forRequest(request));
    }
}
