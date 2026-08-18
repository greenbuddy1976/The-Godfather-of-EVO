package com.greenbuddy.acevosetupengineer.core;

import com.greenbuddy.acevosetupengineer.model.CatalogItem;
import com.greenbuddy.acevosetupengineer.model.ExactCandidate;
import com.greenbuddy.acevosetupengineer.model.SetupMode;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public final class LiveSearchCoordinatorTest {
    @Test
    public void earlyExactStillRunsAllThreeCompleteRounds() {
        FakeProvider provider = new FakeProvider(FakeProvider.Behaviour.EXACT);
        LiveSearchSummary result = new LiveSearchCoordinator(List.of(provider), new MemoryExactCache())
                .search(request());

        assertEquals(3, provider.searchCalls);
        assertEquals(LiveSearchSummary.Status.EXACT, result.status);
        assertEquals(3, result.completedRounds);
        assertNotNull(result.exact);
        assertFalse(result.liveUnverified);
    }

    @Test
    public void threeSuccessfulEmptyRoundsLeadToSelfCalcPath() {
        FakeProvider provider = new FakeProvider(FakeProvider.Behaviour.EMPTY);
        LiveSearchSummary result = new LiveSearchCoordinator(List.of(provider), new MemoryExactCache())
                .search(request());

        assertEquals(3, provider.searchCalls);
        assertEquals(LiveSearchSummary.Status.NO_EXACT_AFTER_3_ROUNDS, result.status);
        assertFalse(result.liveUnverified);
    }

    @Test
    public void threeTechnicalFailuresNeverClaimNoExact() {
        FakeProvider provider = new FakeProvider(FakeProvider.Behaviour.FAIL);
        LiveSearchSummary result = new LiveSearchCoordinator(List.of(provider), new MemoryExactCache())
                .search(request());

        assertEquals(3, provider.searchCalls);
        assertEquals(LiveSearchSummary.Status.LIVE_FAILED_AFTER_3_ROUNDS, result.status);
        assertTrue(result.liveUnverified);
        assertTrue(result.auditLog.stream().anyMatch(line -> line.contains("Existenz eines EXACT ist unbekannt")));
    }

    @Test
    public void mismatchingBinarySignatureIsRejected() {
        FakeProvider provider = new FakeProvider(FakeProvider.Behaviour.WRONG_SIGNATURE);
        LiveSearchSummary result = new LiveSearchCoordinator(List.of(provider), new MemoryExactCache())
                .search(request());
        assertEquals(LiveSearchSummary.Status.NO_EXACT_AFTER_3_ROUNDS, result.status);
        assertTrue(result.auditLog.stream().anyMatch(line -> line.contains("Signatur passt nicht")));
    }

    private static SetupRequest request() {
        CatalogItem vehicle = new CatalogItem(CatalogItem.Kind.VEHICLE, "vehicle", "Vehicle",
                "vehicle", "ks_vehicle_", true, true);
        CatalogItem layout = new CatalogItem(CatalogItem.Kind.LAYOUT, "layout", "Layout",
                "layout", "", true, true);
        return new SetupRequest(vehicle, layout, SetupMode.FAST_CONTROL, "0.8.1");
    }

    private static final class FakeProvider implements LiveProvider {
        enum Behaviour { EXACT, EMPTY, FAIL, WRONG_SIGNATURE }

        final Behaviour behaviour;
        int searchCalls;

        FakeProvider(Behaviour behaviour) {
            this.behaviour = behaviour;
        }

        @Override
        public String name() {
            return "TestProvider";
        }

        @Override
        public List<ExactCandidate> searchExact(SetupRequest request) throws IOException {
            searchCalls++;
            if (behaviour == Behaviour.FAIL) throw new IOException("offline");
            if (behaviour == Behaviour.EMPTY) return List.of();
            return List.of(new ExactCandidate(name(), "id", "vehicle", "layout", "0.8.1",
                    "https://example.invalid/setup/id", "https://example.invalid/file", "test.carsetup"));
        }

        @Override
        public byte[] download(ExactCandidate candidate) {
            return syntheticStructure(behaviour == Behaviour.WRONG_SIGNATURE
                    ? "ks_other_preset" : "ks_vehicle_preset");
        }
    }

    /** Test-only protobuf-like structure; not a setup baseline and never packaged in the app. */
    private static byte[] syntheticStructure(String signature) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (int field = 1; field <= 4; field++) {
                output.write((field << 3) | 5);
                output.write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
                        .putFloat((float) field).array());
            }
            byte[] signatureBytes = signature.getBytes(StandardCharsets.UTF_8);
            output.write((9 << 3) | 2);
            output.write(signatureBytes.length);
            output.write(signatureBytes);
            return output.toByteArray();
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
    }
}
