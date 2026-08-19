package com.greenbuddy.acevosetupengineer.live;

import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public final class LiveLookupCoordinator {
    public static final int MAX_COMPLETE_ROUNDS = 2;
    public static final long DEFAULT_ROUND_TIMEOUT_MILLIS = 8_000L;
    private final List<LiveSetupSource> sources;
    private final long roundTimeoutMillis;

    public LiveLookupCoordinator(List<LiveSetupSource> sources) {
        this(sources, DEFAULT_ROUND_TIMEOUT_MILLIS);
    }

    public LiveLookupCoordinator(List<LiveSetupSource> sources, long roundTimeoutMillis) {
        if (roundTimeoutMillis <= 0) throw new IllegalArgumentException("roundTimeoutMillis");
        this.sources = sources == null
                ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(sources));
        this.roundTimeoutMillis = roundTimeoutMillis;
    }

    public LiveLookupReport findExact(SetupRequest request) {
        Map<String, LiveCandidate> candidates = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        int rounds = 0;
        for (int round = 0; round < MAX_COMPLETE_ROUNDS; round++) {
            rounds++;
            LiveLookupReport result = findExactRound(request);
            errors.addAll(result.getTechnicalErrors());
            for (LiveCandidate candidate : result.getCandidates()) {
                candidates.putIfAbsent(candidateKey(candidate), candidate);
            }
        }
        return new LiveLookupReport(new ArrayList<>(candidates.values()), errors, rounds);
    }

    /** Performs one full parallel source round with one shared deadline. */
    public LiveLookupReport findExactRound(SetupRequest request) {
        if (sources.isEmpty()) {
            return new LiveLookupReport(Collections.emptyList(), Collections.emptyList(), 1);
        }

        AtomicInteger threadNumber = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(sources.size(), runnable -> {
            Thread thread = new Thread(runnable,
                    "acevo-live-source-" + threadNumber.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        List<Future<List<LiveCandidate>>> futures = new ArrayList<>(sources.size());
        for (LiveSetupSource source : sources) {
            futures.add(executor.submit(() -> source.exactLookup(request)));
        }

        List<LiveCandidate> candidates = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(roundTimeoutMillis);
        try {
            for (int index = 0; index < futures.size(); index++) {
                LiveSetupSource source = sources.get(index);
                Future<List<LiveCandidate>> future = futures.get(index);
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0 && !future.isDone()) {
                    future.cancel(true);
                    errors.add(source.name() + ": Timeout");
                    continue;
                }
                try {
                    List<LiveCandidate> found = future.get(Math.max(0L, remaining),
                            TimeUnit.NANOSECONDS);
                    if (found == null) continue;
                    for (LiveCandidate candidate : found) {
                        if (candidate != null && isExact(candidate, request)) candidates.add(candidate);
                    }
                } catch (TimeoutException error) {
                    future.cancel(true);
                    errors.add(source.name() + ": Timeout");
                } catch (ExecutionException error) {
                    Throwable cause = error.getCause();
                    errors.add(source.name() + ": "
                            + (cause == null ? "TechnicalError" : cause.getClass().getSimpleName()));
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    future.cancel(true);
                    errors.add(source.name() + ": Interrupted");
                }
            }
        } finally {
            for (Future<List<LiveCandidate>> future : futures) future.cancel(true);
            executor.shutdownNow();
        }
        return new LiveLookupReport(candidates, errors, 1);
    }

    private static boolean isExact(LiveCandidate candidate, SetupRequest request) {
        return request.getCar().getId().equals(candidate.getCarId())
                && request.getLayout().getId().equals(candidate.getLayoutId())
                && request.getGameVersion().equals(candidate.getGameVersion());
    }

    private static String candidateKey(LiveCandidate candidate) {
        return candidate.getSource() + "|" + candidate.getSetupId() + "|"
                + candidate.getDeclaredSha256().toLowerCase(java.util.Locale.ROOT);
    }
}
