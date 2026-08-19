package com.greenbuddy.acevosetupengineer;

import com.greenbuddy.acevosetupengineer.data.OfficialInventory;
import com.greenbuddy.acevosetupengineer.model.FineTuningProblem;
import com.greenbuddy.acevosetupengineer.model.FineTuningStrength;
import com.greenbuddy.acevosetupengineer.model.GeneratedSetup;
import com.greenbuddy.acevosetupengineer.model.ResultLabel;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.model.SetupStyle;
import com.greenbuddy.acevosetupengineer.model.VerificationReport;
import com.greenbuddy.acevosetupengineer.verification.BinaryDigest;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

public final class BinaryDigestContractTest {
    @Test public void sha256IsComputedFromActualBytes() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                BinaryDigest.sha256(new byte[] {'a', 'b', 'c'}));
    }

    @Test(expected = IllegalArgumentException.class)
    public void generatedSetupRejectsReportedHashThatDoesNotMatchBytes() {
        SetupRequest request = new SetupRequest(OfficialInventory.cars().get(0),
                OfficialInventory.layouts().get(0), SetupStyle.FAST_CONTROL,
                FineTuningProblem.NONE, FineTuningStrength.ONE, OfficialInventory.GAME_VERSION);
        VerificationReport falseHash = new VerificationReport(true, true, true, true,
                true, true, true, true, true, true, true,
                "0000000000000000000000000000000000000000000000000000000000000000",
                "test-only mismatch");
        new GeneratedSetup(request, new byte[] {1}, Collections.emptyList(),
                Collections.emptyList(), falseHash, ResultLabel.ENGINEERING_MODEL);
    }
}
