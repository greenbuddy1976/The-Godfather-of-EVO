package com.greenbuddy.acevosetupengineer.engineering;

import android.content.Context;

import com.greenbuddy.acevosetupengineer.model.CatalogItem;
import com.greenbuddy.acevosetupengineer.util.Hashing;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Loads exact-layout geometry profiles. The asset contains no setup numbers. */
public final class TrackProfileRepository {
    private static final String ASSET = "track-engineering-profiles-0.8.1.json";
    private static final String GAME_VERSION = "0.8.1";

    private final Map<String, JSONObject> profiles = new HashMap<>();
    private final String datasetSha256;

    public TrackProfileRepository(Context context) throws IOException {
        byte[] bytes = readAsset(context, ASSET);
        datasetSha256 = Hashing.sha256(bytes);
        try {
            JSONObject root = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            if (root.optInt("schema", 0) != 1 || !GAME_VERSION.equals(root.optString("gameVersion"))) {
                throw new IOException("Streckenprofil-Schema oder Spielversion stimmt nicht");
            }
            JSONArray rows = root.getJSONArray("profiles");
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String id = row.getString("id");
                if (profiles.put(id, row) != null) {
                    throw new IOException("Doppeltes Streckenprofil: " + id);
                }
            }
        } catch (JSONException invalid) {
            throw new IOException("Streckenprofile sind kein gültiges JSON", invalid);
        }
    }

    public boolean hasVerifiedProfile(CatalogItem layout) {
        return layout != null && layout.kind == CatalogItem.Kind.LAYOUT && profiles.containsKey(layout.id);
    }

    public TrackProfile load(CatalogItem layout) {
        JSONObject row = profiles.get(layout.id);
        if (row == null) throw new SetupValidationException("Kein exaktes Streckenprofil für " + layout.name);
        try {
            String source = row.getString("source");
            if (!source.startsWith("https://")) {
                throw new SetupValidationException("Streckenprofil besitzt keine HTTPS-Quellenreferenz");
            }
            return new TrackProfile(layout.id, GAME_VERSION,
                    row.getDouble("speedDemand"), row.getDouble("bumpDemand"),
                    row.getDouble("tractionDemand"), row.getDouble("brakingDemand"),
                    row.getDouble("lengthMeters"), source,
                    datasetSha256 + ":" + layout.id, true);
        } catch (JSONException invalid) {
            throw new SetupValidationException("Streckenprofil ist unvollständig: " + layout.name);
        }
    }

    private static byte[] readAsset(Context context, String name) throws IOException {
        try (InputStream input = context.getAssets().open(name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }
}
