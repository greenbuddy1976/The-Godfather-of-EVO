package com.greenbuddy.acevosetupengineer;

import com.greenbuddy.acevosetupengineer.data.OfficialInventory;
import com.greenbuddy.acevosetupengineer.live.LiveCandidate;
import com.greenbuddy.acevosetupengineer.live.LiveLookupCoordinator;
import com.greenbuddy.acevosetupengineer.live.LiveLookupReport;
import com.greenbuddy.acevosetupengineer.live.LiveSetupSource;
import com.greenbuddy.acevosetupengineer.model.FineTuningProblem;
import com.greenbuddy.acevosetupengineer.model.FineTuningStrength;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.model.SetupStyle;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public final class LiveLookupContractTest {
    @Test public void exactHitIsAcceptedAndSimilarLayoutIsRejected() {
        SetupRequest request = request(0);
        LiveCandidate exact = candidate(request.getLayout().getId());
        LiveCandidate wrongLayout = candidate(OfficialInventory.layouts().get(1).getId());
        AtomicInteger calls = new AtomicInteger();
        LiveSetupSource source = source("source-a", calls, Arrays.asList(wrongLayout, exact), false);

        LiveLookupReport report = new LiveLookupCoordinator(Collections.singletonList(source))
                .findExact(request);

        assertEquals(1, report.getCompleteRounds());
        assertEquals(1, calls.get());
        assertEquals(1, report.getCandidates().size());
        assertEquals(exact.getSetupId(), report.getCandidates().get(0).getSetupId());
    }

    @Test public void technicalFailureIsNotReportedAsAnExactMissAndTwoRoundsAreComplete() {
        AtomicInteger failingCalls = new AtomicInteger();
        AtomicInteger emptyCalls = new AtomicInteger();
        LiveSetupSource failing = source("offline", failingCalls, Collections.emptyList(), true);
        LiveSetupSource empty = source("empty", emptyCalls, Collections.emptyList(), false);

        LiveLookupReport report = new LiveLookupCoordinator(Arrays.asList(failing, empty))
                .findExact(request(0));

        assertEquals(2, report.getCompleteRounds());
        assertEquals(2, failingCalls.get());
        assertEquals(2, emptyCalls.get());
        assertTrue(report.getCandidates().isEmpty());
        assertEquals(2, report.getTechnicalErrors().size());
        assertTrue(report.hasTechnicalErrors());
    }

    @Test public void exactCacheKeysDifferByLayout() {
        assertNotEquals(request(0).exactKey(), request(1).exactKey());
    }

    private static SetupRequest request(int layoutIndex) {
        return new SetupRequest(OfficialInventory.cars().get(0),
                OfficialInventory.layouts().get(layoutIndex), SetupStyle.FAST_CONTROL,
                FineTuningProblem.NONE, FineTuningStrength.ONE, OfficialInventory.GAME_VERSION);
    }

    private static LiveCandidate candidate(String layoutId) {
        return new LiveCandidate(OfficialInventory.cars().get(0).getId(), layoutId,
                OfficialInventory.GAME_VERSION, "setup-" + layoutId, "test-source",
                "https://example.invalid/" + layoutId,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                1L, new byte[] {1});
    }

    private static LiveSetupSource source(String name, AtomicInteger calls,
                                          List<LiveCandidate> candidates, boolean fail) {
        return new LiveSetupSource() {
            @Override public String name() { return name; }
            @Override public List<LiveCandidate> exactLookup(SetupRequest request) throws Exception {
                calls.incrementAndGet();
                if (fail) throw new IllegalStateException("offline");
                return candidates;
            }
            @Override public List<LiveCandidate> fetchConfirmedIndex() { return candidates; }
        };
    }
}
