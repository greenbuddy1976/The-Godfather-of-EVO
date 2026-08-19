package com.greenbuddy.acevosetupengineer.beta;

import com.greenbuddy.acevosetupengineer.data.OfficialInventory;
import com.greenbuddy.acevosetupengineer.model.*;
import com.greenbuddy.acevosetupengineer.verification.BinaryDigest;

import org.junit.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public final class MustangBetaEngineTest {
    private final MustangBetaEngine engine = new MustangBetaEngine();
    private final MustangSetupInspector inspector = new MustangSetupInspector();

    @Test public void fiveProfilesAreDistinctAndRemainBetaOnly() {
        Set<String> hashes = new HashSet<>();
        for (SetupStyle style : SetupStyle.values()) {
            GenerationOutcome outcome = engine.generate(MustangTestFixture.valid(), request(style,
                    FineTuningProblem.NONE, FineTuningStrength.ONE, "spa_gp"));
            assertTrue(outcome.hasSavableBinary());
            assertTrue(outcome.getSetup().isBetaExportable());
            assertFalse(outcome.getSetup().isExportable());
            assertFalse(outcome.getSetup().getVerification().hasGameLoadAcceptanceEvidence());
            assertTrue(outcome.getMessage().contains("SPIELAKZEPTANZ NOCH NICHT BESTÄTIGT"));
            hashes.add(BinaryDigest.sha256(outcome.getSetup().getBinary()));
        }
        assertEquals(5, hashes.size());
    }

    @Test public void fastAttackDecodesTcExactlyOne() {
        GeneratedSetup setup = engine.generate(MustangTestFixture.valid(), request(
                SetupStyle.FAST_ATTACK, FineTuningProblem.NONE, FineTuningStrength.ONE,
                "monza_gp")).getSetup();
        MustangImportInspection decoded = inspector.inspect(setup.getBinary());
        assertTrue(decoded.isValid());
        assertEquals(1f, decoded.getValues().get(MustangField.TC), .0001f);
    }

    @Test public void allEightProblemsAndThreeStrengthsChangeBinaryAndRoundTrip() {
        for (FineTuningProblem problem : FineTuningProblem.values()) {
            if (problem == FineTuningProblem.NONE) continue;
            for (FineTuningStrength strength : FineTuningStrength.values()) {
                GeneratedSetup baseline = engine.generate(MustangTestFixture.valid(), request(
                        SetupStyle.FAST_CONTROL, FineTuningProblem.NONE, strength,
                        "nurburgring_gp_strecke")).getSetup();
                GenerationOutcome tuned = engine.generate(MustangTestFixture.valid(), request(
                        SetupStyle.FAST_CONTROL, problem, strength,
                        "nurburgring_gp_strecke"));
                assertTrue(problem + "/" + strength, tuned.hasSavableBinary());
                assertFalse(java.util.Arrays.equals(baseline.getBinary(), tuned.getSetup().getBinary()));
                assertTrue(inspector.inspect(tuned.getSetup().getBinary()).isValid());
                assertFalse(tuned.getSetup().getChanges().isEmpty());
            }
        }
    }

    @Test public void unknownFrontWingAndAllBytesOutsideKnownPayloadsArePreserved() {
        byte[] base = MustangTestFixture.valid();
        byte[] output = engine.generate(base, request(SetupStyle.FAST_SAFE,
                FineTuningProblem.KERBS_OR_CRESTS_UNSETTLED, FineTuningStrength.THREE,
                "sebring_gp")).getSetup().getBinary();
        java.util.EnumMap<MustangField, Integer> offsets = MustangFieldLocator.locate(base);
        Set<Integer> allowed = new HashSet<>();
        for (MustangField field : MustangRange.all().keySet()) {
            int offset = offsets.get(field);
            for (int index = 0; index < 4; index++) allowed.add(offset + index);
        }
        for (int index = 0; index < base.length; index++) {
            if (!allowed.contains(index)) assertEquals("byte " + index, base[index], output[index]);
        }
        ProtoWire.Message top = ProtoWire.parse(base, 0, base.length);
        ProtoWire.Message aero = top.message(top.one(6, 2));
        int frontWingOffset = aero.one(4, 5).payload;
        for (int index = 0; index < 4; index++) {
            assertEquals(base[frontWingOffset + index], output[frontWingOffset + index]);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void unknownLayoutFailsWithoutNearestMatch() {
        MustangTrackTraits.require("spa_almost");
    }

    private static SetupRequest request(SetupStyle style, FineTuningProblem problem,
                                        FineTuningStrength strength, String layoutId) {
        return new SetupRequest(OfficialInventory.requireCar("ford_mustang_gt3"),
                OfficialInventory.requireLayout(layoutId), style, problem, strength,
                OfficialInventory.GAME_VERSION);
    }
}
