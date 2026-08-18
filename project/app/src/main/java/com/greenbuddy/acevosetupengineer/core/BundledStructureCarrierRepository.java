package com.greenbuddy.acevosetupengineer.core;

import android.content.Context;

import com.greenbuddy.acevosetupengineer.binary.CarSetupInspection;
import com.greenbuddy.acevosetupengineer.binary.CarSetupInspector;
import com.greenbuddy.acevosetupengineer.model.CatalogItem;
import com.greenbuddy.acevosetupengineer.util.Hashing;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Release-pinned, integrity-checked same-car carriers for offline SELF CALC. */
public final class BundledStructureCarrierRepository {
    private static final String MANIFEST = "structure-carriers-0.8.1.json";
    private static final String GAME_VERSION = "0.8.1";

    private final Context context;
    private final Map<String, Entry> entries = new HashMap<>();

    public BundledStructureCarrierRepository(Context context) throws IOException {
        this.context = context.getApplicationContext();
        try {
            JSONObject manifest = new JSONObject(new String(
                    readAsset(MANIFEST), StandardCharsets.UTF_8));
            if (manifest.optInt("schema", 0) != 1
                    || !GAME_VERSION.equals(manifest.optString("gameVersion"))) {
                throw new IOException("Strukturträger-Manifest besitzt die falsche Version");
            }
            JSONArray rows = manifest.getJSONArray("vehicles");
            for (int index = 0; index < rows.length(); index++) {
                JSONObject row = rows.getJSONObject(index);
                Entry entry = new Entry(row.getString("vehicleId"), row.getString("asset"),
                        row.getString("sha256"), row.getString("signature"),
                        manifest.getString("packageSource"));
                if (entries.put(entry.vehicleId, entry) != null) {
                    throw new IOException("Doppelter Strukturträger: " + entry.vehicleId);
                }
            }
        } catch (IOException failure) {
            throw failure;
        } catch (Exception invalid) {
            throw new IOException("Strukturträger-Manifest ist ungültig", invalid);
        }
    }

    public boolean has(CatalogItem vehicle) {
        return vehicle != null && entries.containsKey(vehicle.id);
    }

    public VerifiedStructureCarrier load(CatalogItem vehicle) {
        Entry entry = entries.get(vehicle.id);
        if (entry == null) return null;
        try {
            byte[] bytes = readAsset(entry.asset);
            String sha = Hashing.sha256(bytes);
            CarSetupInspection inspection = CarSetupInspector.inspect(bytes);
            if (!sha.equals(entry.sha256)
                    || !inspection.structurallyValid
                    || !entry.signature.equals(inspection.vehicleSignature)
                    || !inspection.vehicleSignature.startsWith(vehicle.expectedSignaturePrefix)) {
                throw new IllegalStateException("Gebündelter Strukturträger hat die Integritätsprüfung nicht bestanden");
            }
            return new VerifiedStructureCarrier(bytes, sha, inspection.vehicleSignature,
                    "RacePlace 0.8.1 (gebündelt, nur Struktur)", false);
        } catch (IOException unavailable) {
            throw new IllegalStateException("Gebündelter Strukturträger fehlt", unavailable);
        }
    }

    private byte[] readAsset(String name) throws IOException {
        try (InputStream input = context.getAssets().open(name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private record Entry(
            String vehicleId,
            String asset,
            String sha256,
            String signature,
            String source) {}
}
