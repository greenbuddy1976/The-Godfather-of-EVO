package com.greenbuddy.acevosetupengineer;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.greenbuddy.acevosetupengineer.data.OfficialInventory;
import com.greenbuddy.acevosetupengineer.engine.ProviderLoader;
import com.greenbuddy.acevosetupengineer.engine.VerifiedWriterProvider;
import com.greenbuddy.acevosetupengineer.live.LiveSetupSource;
import com.greenbuddy.acevosetupengineer.model.*;
import com.greenbuddy.acevosetupengineer.verification.BinaryDigest;
import com.greenbuddy.acevosetupengineer.verification.BinaryInspection;
import com.greenbuddy.acevosetupengineer.verification.VerifiedBinaryInspector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Runs only in protected release CI after the real writer AAR has been injected. */
@RunWith(AndroidJUnit4.class)
public final class VerifiedReleaseMatrixTest {
    @Test
    public void testAll12425ReleaseCombinationsProduceVerifiedBinary() throws Exception {
        VerifiedWriterProvider provider = ProviderLoader.load(BuildConfig.VERIFIED_WRITER_PROVIDER_CLASS);
        assertNotNull("Protected verified provider is mandatory", provider);
        assertEquals(OfficialInventory.GAME_VERSION, provider.supportedGameVersion());
        VerifiedBinaryInspector inspector = provider.binaryInspector();
        assertNotNull("Independent binary inspector is mandatory", inspector);
        int count = 0;
        for (CarIdentity car : OfficialInventory.cars()) {
            for (TrackLayout layout : OfficialInventory.layouts()) {
                Set<String> styleHashes = new HashSet<>();
                for (SetupStyle style : SetupStyle.values()) {
                    SetupRequest request = request(car, layout, style,
                            FineTuningProblem.NONE, FineTuningStrength.ONE);
                    assertTrue("Missing exact writer coverage: " + request.exactKey(), provider.supports(request));
                    GeneratedSetup setup = provider.generateEngineeringModel(request);
                    assertNotNull(setup);
                    assertTrue(setup.getBinary().length > 0);
                    assertFalse(setup.getValues().isEmpty());
                    assertTrue(setup.getVerification().isFullyVerified());
                    assertEquals(request.requestKey(), setup.getRequest().requestKey());
                    assertEquals(BinaryDigest.sha256(setup.getBinary()), setup.getVerification().getSha256());
                    assertInspection(inspector, request, setup);
                    assertTrue("Setup styles must produce distinct binaries: " + request.exactKey(),
                            styleHashes.add(setup.getVerification().getSha256()));
                    count++;
                }
            }
        }
        assertEquals(12425, count);
    }

    @Test
    public void testEveryFineTuningProblemAndStrengthActuallyChangesValues() throws Exception {
        VerifiedWriterProvider provider = ProviderLoader.load(BuildConfig.VERIFIED_WRITER_PROVIDER_CLASS);
        assertNotNull(provider);
        VerifiedBinaryInspector inspector = provider.binaryInspector();
        assertNotNull(inspector);
        CarIdentity car = OfficialInventory.requireCar("bmw_m2_coupe_g87");
        TrackLayout layout = OfficialInventory.layouts().get(0);
        GeneratedSetup baseline = provider.generateEngineeringModel(
                request(car, layout, SetupStyle.FAST_CONTROL,
                        FineTuningProblem.NONE, FineTuningStrength.ONE));
        int count = 0;
        for (FineTuningProblem problem : FineTuningProblem.values()) {
            if (problem == FineTuningProblem.NONE) continue;
            Set<String> strengthHashes = new HashSet<>();
            for (FineTuningStrength strength : FineTuningStrength.values()) {
                SetupRequest fineRequest = request(car, layout, SetupStyle.FAST_CONTROL,
                        problem, strength);
                GeneratedSetup setup = provider.generateEngineeringModel(fineRequest);
                assertTrue(setup.isExportable());
                assertFalse("Fine tuning must change real parameters", setup.getChanges().isEmpty());
                assertFalse("Fine tuning must change binary bytes",
                        baseline.getVerification().getSha256().equals(setup.getVerification().getSha256()));
                assertTrue("Strengths 1–3 must remain distinguishable",
                        strengthHashes.add(setup.getVerification().getSha256()));
                assertInspection(inspector, fineRequest, setup);
                count++;
            }
        }
        assertEquals(24, count);
    }

    @Test
    public void testMustangFastAttackTcOneOnAllLayouts() throws Exception {
        VerifiedWriterProvider provider = ProviderLoader.load(BuildConfig.VERIFIED_WRITER_PROVIDER_CLASS);
        assertNotNull(provider);
        VerifiedBinaryInspector inspector = provider.binaryInspector();
        assertNotNull(inspector);
        CarIdentity mustang = OfficialInventory.requireCar("ford_mustang_gt3");
        for (TrackLayout layout : OfficialInventory.layouts()) {
            SetupRequest attack = request(mustang, layout, SetupStyle.FAST_ATTACK,
                    FineTuningProblem.NONE, FineTuningStrength.ONE);
            GeneratedSetup setup = provider.generateEngineeringModel(attack);
            BinaryInspection inspection = inspector.inspect(attack, setup.getBinary());
            assertTrue(inspection.verifies(setup.getBinary()));
            boolean tcOne = false;
            for (SetupValue value : inspection.getDecodedValues()) {
                if ("tc".equals(value.getKey()) && "1".equals(value.getFormattedValue())) tcOne = true;
            }
            assertTrue("Mustang FAST ATTACK must have TC = 1 at " + layout.getId(), tcOne);
            assertTrue(setup.getVerification().isTractionControlRuleValid());
        }
    }

    @Test
    public void testExplicitRequiredBmwAndMustangIdentities() throws Exception {
        VerifiedWriterProvider provider = ProviderLoader.load(BuildConfig.VERIFIED_WRITER_PROVIDER_CLASS);
        assertNotNull(provider);
        VerifiedBinaryInspector inspector = provider.binaryInspector();
        assertNotNull(inspector);
        String[] ids = {"bmw_m2_coupe_g87", "bmw_m2_cs_racing", "bmw_m3_e30_sport_evo",
                "bmw_m3_e46_csl", "ford_mustang_gt3"};
        TrackLayout layout = OfficialInventory.layouts().get(0);
        for (String id : ids) {
            SetupRequest request = request(OfficialInventory.requireCar(id), layout,
                    SetupStyle.FAST_CONTROL, FineTuningProblem.NONE, FineTuningStrength.ONE);
            assertTrue("Missing required identity: " + id, provider.supports(request));
            GeneratedSetup setup = provider.generateEngineeringModel(request);
            assertEquals(id, setup.getRequest().getCar().getId());
            assertTrue(setup.isExportable());
            assertEquals(BinaryDigest.sha256(setup.getBinary()), setup.getVerification().getSha256());
            assertInspection(inspector, request, setup);
        }
    }

    @Test
    public void testRequiredModularLiveSourcesArePresent() {
        VerifiedWriterProvider provider = ProviderLoader.load(BuildConfig.VERIFIED_WRITER_PROVIDER_CLASS);
        assertNotNull(provider);
        assertNotNull(provider.liveSources());
        Set<String> names = new HashSet<>();
        for (LiveSetupSource source : provider.liveSources()) {
            assertNotNull(source);
            names.add(source.name());
        }
        assertTrue("SetupsMarket source adapter missing", names.contains("SetupsMarket"));
        assertTrue("RacePlace source adapter missing", names.contains("RacePlace"));
    }

    @Test
    public void testAdjustableMustangRearWingParticipatesInHighSpeedFineTuning() throws Exception {
        VerifiedWriterProvider provider = ProviderLoader.load(BuildConfig.VERIFIED_WRITER_PROVIDER_CLASS);
        assertNotNull(provider);
        VerifiedBinaryInspector inspector = provider.binaryInspector();
        assertNotNull(inspector);
        CarIdentity mustang = OfficialInventory.requireCar("ford_mustang_gt3");
        TrackLayout layout = OfficialInventory.layouts().get(0);
        SetupRequest baselineRequest = request(mustang, layout, SetupStyle.FAST_CONTROL,
                FineTuningProblem.NONE, FineTuningStrength.ONE);
        GeneratedSetup baseline = provider.generateEngineeringModel(baselineRequest);
        String baselineWing = value(inspector.inspect(baselineRequest, baseline.getBinary())
                .getDecodedValues(), "rear_wing");
        assertNotNull("Mustang rear wing must be represented as adjustable", baselineWing);

        FineTuningProblem[] problems = {FineTuningProblem.HIGH_SPEED_REAR_NERVOUS,
                FineTuningProblem.MORE_TOP_SPEED};
        for (FineTuningProblem problem : problems) {
            SetupRequest tunedRequest = request(mustang, layout, SetupStyle.FAST_CONTROL,
                    problem, FineTuningStrength.TWO);
            GeneratedSetup tuned = provider.generateEngineeringModel(tunedRequest);
            String tunedWing = value(inspector.inspect(tunedRequest, tuned.getBinary())
                    .getDecodedValues(), "rear_wing");
            assertNotNull(tunedWing);
            assertFalse("Rear wing must change for " + problem.name(),
                    baselineWing.equals(tunedWing));
        }
    }

    private static void assertInspection(VerifiedBinaryInspector inspector, SetupRequest request,
                                         GeneratedSetup setup) throws Exception {
        BinaryInspection inspection = inspector.inspect(request, setup.getBinary());
        assertNotNull(inspection);
        assertTrue(inspection.verifies(setup.getBinary()));
        assertValuesEqual(setup.getValues(), inspection.getDecodedValues());
    }

    private static void assertValuesEqual(List<SetupValue> expected, List<SetupValue> decoded) {
        assertEquals(expected.size(), decoded.size());
        for (int index = 0; index < expected.size(); index++) {
            SetupValue left = expected.get(index);
            SetupValue right = decoded.get(index);
            assertEquals(left.getSection(), right.getSection());
            assertEquals(left.getPosition(), right.getPosition());
            assertEquals(left.getKey(), right.getKey());
            assertEquals(left.getFormattedValue(), right.getFormattedValue());
            assertEquals(left.isAdjustable(), right.isAdjustable());
        }
    }

    private static String value(List<SetupValue> values, String key) {
        for (SetupValue value : values) {
            if (key.equals(value.getKey()) && value.isAdjustable()) {
                return value.getFormattedValue();
            }
        }
        return null;
    }

    private static SetupRequest request(CarIdentity car, TrackLayout layout, SetupStyle style,
                                        FineTuningProblem problem, FineTuningStrength strength) {
        return new SetupRequest(car, layout, style, problem, strength, OfficialInventory.GAME_VERSION);
    }
}
