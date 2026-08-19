package com.greenbuddy.acevosetupengineer.live;

import android.content.Context;
import android.content.SharedPreferences;

import com.greenbuddy.acevosetupengineer.data.OfficialInventory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/** Background-only first-start/manual metadata index. Binary files are never cached here. */
public final class LiveIndexController {
    private static final String PREFS = "confirmed_live_index_0_8_1";
    private static final String KEY_INDEX = "metadata_json";
    private final SharedPreferences preferences;
    private final List<LiveSetupSource> sources;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface Listener {
        void onProgress(String text);
        void onComplete(String text);
    }

    public LiveIndexController(Context context, List<LiveSetupSource> sources) {
        this.preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.sources = sources == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(sources));
    }

    public boolean hasConfirmedIndex() {
        return preferences.contains(KEY_INDEX);
    }

    public void refresh(Listener listener) {
        executor.execute(() -> {
            if (sources.isEmpty()) {
                listener.onComplete("LIVE-Index: keine verifizierte Quellenimplementierung geladen");
                return;
            }
            JSONArray confirmed = new JSONArray();
            int errors = 0;
            AtomicInteger threadNumber = new AtomicInteger();
            ExecutorService sourceExecutor = Executors.newFixedThreadPool(sources.size(), runnable -> {
                Thread thread = new Thread(runnable,
                        "acevo-live-index-" + threadNumber.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            });
            List<Future<List<LiveCandidate>>> futures = new ArrayList<>(sources.size());
            for (int index = 0; index < sources.size(); index++) {
                LiveSetupSource source = sources.get(index);
                listener.onProgress("LIVE-Index: " + source.name() + " ("
                        + (index + 1) + "/" + sources.size() + ")");
                futures.add(sourceExecutor.submit(source::fetchConfirmedIndex));
            }
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(
                    LiveLookupCoordinator.DEFAULT_ROUND_TIMEOUT_MILLIS);
            try {
                for (int index = 0; index < sources.size(); index++) {
                    Future<List<LiveCandidate>> future = futures.get(index);
                    long remaining = deadline - System.nanoTime();
                    if (remaining <= 0 && !future.isDone()) {
                        future.cancel(true);
                        errors++;
                        continue;
                    }
                    List<LiveCandidate> candidates;
                    try {
                        candidates = future.get(Math.max(0L, remaining), TimeUnit.NANOSECONDS);
                    } catch (TimeoutException | ExecutionException error) {
                        future.cancel(true);
                        errors++;
                        continue;
                    } catch (InterruptedException error) {
                        Thread.currentThread().interrupt();
                        future.cancel(true);
                        errors++;
                        continue;
                    }
                    if (candidates == null) continue;
                    for (LiveCandidate candidate : candidates) {
                        JSONObject item = metadata(candidate);
                        if (item != null) confirmed.put(item);
                    }
                }
            } finally {
                for (Future<List<LiveCandidate>> future : futures) future.cancel(true);
                sourceExecutor.shutdownNow();
            }
            if (errors == 0) {
                preferences.edit().putString(KEY_INDEX, confirmed.toString()).apply();
            }
            String state = errors == 0
                    ? "LIVE-Index: " + confirmed.length() + " bestätigte Metadaten, 0 technische Quellenfehler"
                    : "LIVE-Index: " + errors + " technische Quellenfehler – letzter bestätigter Bestand bleibt erhalten";
            listener.onComplete(state);
        });
    }

    public String cachedSummary() {
        String json = preferences.getString(KEY_INDEX, "[]");
        try {
            return "LIVE-Index: " + new JSONArray(json).length()
                    + " bestätigte Metadaten lokal bekannt";
        } catch (JSONException error) {
            return "LIVE-Index: lokale Metadaten beschädigt – Aktualisierung erforderlich";
        }
    }

    public void shutdown() { executor.shutdownNow(); }

    private static JSONObject metadata(LiveCandidate candidate) {
        if (candidate == null || candidate.getCarId().isEmpty() || candidate.getLayoutId().isEmpty()
                || !OfficialInventory.GAME_VERSION.equals(candidate.getGameVersion())
                || candidate.getSetupId().isEmpty()
                || candidate.getSource().isEmpty() || candidate.getDownloadAddress().isEmpty()
                || !candidate.getDeclaredSha256().matches("[0-9a-fA-F]{64}")) return null;
        try {
            return new JSONObject()
                    .put("carIdentity", candidate.getCarId())
                    .put("exactLayout", candidate.getLayoutId())
                    .put("gameVersion", candidate.getGameVersion())
                    .put("setupId", candidate.getSetupId())
                    .put("source", candidate.getSource())
                    .put("downloadAddress", candidate.getDownloadAddress())
                    .put("sha256", candidate.getDeclaredSha256().toLowerCase(Locale.ROOT))
                    .put("verificationStatus", "SOURCE_CONFIRMED_METADATA")
                    .put("lastCheckedAt", candidate.getCheckedAtEpochMillis());
        } catch (JSONException impossible) {
            return null;
        }
    }
}
