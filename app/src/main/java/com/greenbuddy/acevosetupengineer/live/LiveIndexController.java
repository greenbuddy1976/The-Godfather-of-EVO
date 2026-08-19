package com.greenbuddy.acevosetupengineer.live;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        this.sources = sources == null ? Collections.emptyList() : sources;
    }

    public boolean hasConfirmedIndex() {
        return preferences.contains(KEY_INDEX);
    }

    public void refresh(Listener listener) {
        executor.execute(() -> {
            JSONArray confirmed = new JSONArray();
            int errors = 0;
            for (int index = 0; index < sources.size(); index++) {
                LiveSetupSource source = sources.get(index);
                listener.onProgress("LIVE-Index: " + source.name() + " ("
                        + (index + 1) + "/" + sources.size() + ")");
                try {
                    List<LiveCandidate> candidates = source.fetchConfirmedIndex();
                    if (candidates == null) continue;
                    for (LiveCandidate candidate : candidates) {
                        JSONObject item = metadata(candidate);
                        if (item != null) confirmed.put(item);
                    }
                } catch (Exception error) {
                    errors++;
                }
            }
            preferences.edit().putString(KEY_INDEX, confirmed.toString()).apply();
            String state = sources.isEmpty()
                    ? "LIVE-Index: keine verifizierte Quellenimplementierung geladen"
                    : "LIVE-Index: " + confirmed.length() + " bestätigte Metadaten, "
                        + errors + " technische Quellenfehler";
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
        if (candidate.getCarId().isEmpty() || candidate.getLayoutId().isEmpty()
                || candidate.getGameVersion().isEmpty() || candidate.getSetupId().isEmpty()
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
                    .put("verificationStatus", "METADATA_CONFIRMED")
                    .put("lastCheckedAt", candidate.getCheckedAtEpochMillis());
        } catch (JSONException impossible) {
            return null;
        }
    }
}
