package com.greenbuddy.acevosetupengineer;

import android.test.InstrumentationTestCase;
import com.greenbuddy.acevosetupengineer.data.OfficialInventory;
import com.greenbuddy.acevosetupengineer.engine.ProviderLoader;
import com.greenbuddy.acevosetupengineer.engine.VerifiedWriterProvider;
import com.greenbuddy.acevosetupengineer.model.*;

import java.security.MessageDigest;
import java.util.Locale;

/** Runs only in protected release CI after the real writer AAR has been injected. */
public final class VerifiedReleaseMatrixTest extends InstrumentationTestCase {
    public void testAll12425ReleaseCombinationsProduceVerifiedBinary() throws Exception {
        VerifiedWriterProvider provider = ProviderLoader.load(BuildConfig.VERIFIED_WRITER_PROVIDER_CLASS);
        assertNotNull("Protected verified provider is mandatory", provider);
        int count = 0;
        for (CarIdentity car : OfficialInventory.cars()) {
            for (TrackLayout layout : OfficialInventory.layouts()) {
                for (SetupStyle style : SetupStyle.values()) {
                    SetupRequest request = request(car, layout, style,
                            FineTuningProblem.NONE, FineTuningStrength.ONE);
                    assertTrue("Missing exact writer coverage: " + request.exactKey(), provider.supports(request));
                    GeneratedSetup setup = provider.generateEngineeringModel(request);
                    assertNotNull(setup);
                    assertTrue(setup.getBinary().length > 0);
                    assertTrue(setup.getVerification().isFullyVerified());
                    assertEquals(request.exactKey(), setup.getRequest().exactKey());
                    assertEquals(sha256(setup.getBinary()), setup.getVerification().getSha256());
                    count++;
                }
            }
        }
        assertEquals(12425, count);
    }

    public void testEveryFineTuningProblemAndStrengthActuallyChangesValues() throws Exception {
        VerifiedWriterProvider provider = ProviderLoader.load(BuildConfig.VERIFIED_WRITER_PROVIDER_CLASS);
        CarIdentity car = OfficialInventory.requireCar("bmw_m2_coupe_g87");
        TrackLayout layout = OfficialInventory.layouts().get(0);
        int count = 0;
        for (FineTuningProblem problem : FineTuningProblem.values()) {
            if (problem == FineTuningProblem.NONE) continue;
            for (FineTuningStrength strength : FineTuningStrength.values()) {
                GeneratedSetup setup = provider.generateEngineeringModel(
                        request(car, layout, SetupStyle.FAST_CONTROL, problem, strength));
                assertTrue(setup.isExportable());
                assertFalse("Fine tuning must change real parameters", setup.getChanges().isEmpty());
                count++;
            }
        }
        assertEquals(24, count);
    }

    public void testMustangFastAttackTcOneOnAllLayouts() throws Exception {
        VerifiedWriterProvider provider = ProviderLoader.load(BuildConfig.VERIFIED_WRITER_PROVIDER_CLASS);
        CarIdentity mustang = OfficialInventory.requireCar("ford_mustang_gt3");
        for (TrackLayout layout : OfficialInventory.layouts()) {
            GeneratedSetup setup = provider.generateEngineeringModel(
                    request(mustang, layout, SetupStyle.FAST_ATTACK,
                            FineTuningProblem.NONE, FineTuningStrength.ONE));
            boolean tcOne = false;
            for (SetupValue value : setup.getValues()) {
                if ("tc".equals(value.getKey()) && "1".equals(value.getFormattedValue())) tcOne = true;
            }
            assertTrue("Mustang FAST ATTACK must have TC = 1 at " + layout.getId(), tcOne);
            assertTrue(setup.getVerification().isTractionControlRuleValid());
        }
    }

    public void testExplicitRequiredBmwAndMustangIdentities() throws Exception {
        VerifiedWriterProvider provider = ProviderLoader.load(BuildConfig.VERIFIED_WRITER_PROVIDER_CLASS);
        assertNotNull(provider);
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
            assertEquals(sha256(setup.getBinary()), setup.getVerification().getSha256());
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder value = new StringBuilder(64);
        for (byte item : digest) value.append(String.format(Locale.ROOT, "%02x", item & 0xff));
        return value.toString();
    }

    private static SetupRequest request(CarIdentity car, TrackLayout layout, SetupStyle style,
                                        FineTuningProblem problem, FineTuningStrength strength) {
        return new SetupRequest(car, layout, style, problem, strength, OfficialInventory.GAME_VERSION);
    }
}
