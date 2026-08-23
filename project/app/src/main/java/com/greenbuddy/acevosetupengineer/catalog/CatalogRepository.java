package com.greenbuddy.acevosetupengineer.catalog;

import android.content.Context;

import com.greenbuddy.acevosetupengineer.model.CatalogItem;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CatalogRepository {
    private CatalogRepository() {}

    public static CatalogData load(Context context, Set<String> fullyVerifiedVehicleIds)
            throws IOException, JSONException {
        JSONObject root = new JSONObject(readAsset(context, "catalog-0.8.1.json"));
        Map<String, JSONObject> thumbnails = readVerifiedThumbnails(context);
        String version = root.getString("gameVersion");
        List<CatalogItem> vehicles = new ArrayList<>();
        JSONArray vehicleRows = root.getJSONArray("vehicles");
        for (int i = 0; i < vehicleRows.length(); i++) {
            JSONObject row = vehicleRows.getJSONObject(i);
            String id = row.getString("id");
            String providerSlug = row.optString("providerSlug", "");
            String signature = row.optString("signaturePrefix", "");
            String rangeKey = row.optString("rangeKey", "");
            JSONObject thumbnail = thumbnails.get(id);
            boolean verifiedThumbnail = thumbnail != null
                    && row.getString("name").equals(thumbnail.optString("vehicleName"))
                    && thumbnail.optString("thumbnailUrl").startsWith("https://assettocorsa.gg/wp-content/uploads/")
                    && thumbnail.optString("sourceUrl").startsWith("https://assettocorsa.gg/wp-json/wp/v2/media/")
                    && thumbnail.optLong("mediaId", 0L) > 0L;
            // Selection/LIVE EXACT only needs an official provider identity and
            // binary signature. SELF CALC readiness is checked separately and
            // must never hide otherwise searchable cars from the user.
            boolean selectable = !providerSlug.trim().isEmpty() && !signature.trim().isEmpty();
            vehicles.add(new CatalogItem(CatalogItem.Kind.VEHICLE, id, row.getString("name"),
                    providerSlug, signature, rangeKey,
                    verifiedThumbnail ? thumbnail.optString("thumbnailUrl") : "",
                    verifiedThumbnail ? thumbnail.optString("sourceUrl") : "",
                    verifiedThumbnail ? thumbnail.optString("altText", "Fahrzeugbild: " + row.getString("name")) : "",
                    verifiedThumbnail, true, selectable));
        }

        List<CatalogItem> layouts = new ArrayList<>();
        JSONArray layoutRows = root.getJSONArray("layouts");
        for (int i = 0; i < layoutRows.length(); i++) {
            JSONObject row = layoutRows.getJSONObject(i);
            boolean exactVerified = row.getBoolean("exactLayoutVerified");
            layouts.add(new CatalogItem(CatalogItem.Kind.LAYOUT, row.getString("id"),
                    row.getString("name"), row.optString("providerSlug", ""), "",
                    exactVerified, exactVerified));
        }
        return new CatalogData(version, vehicles, layouts);
    }

    private static Map<String, JSONObject> readVerifiedThumbnails(Context context)
            throws IOException, JSONException {
        JSONObject root = new JSONObject(readAsset(context, "vehicle-thumbnails-0.8.1.json"));
        if (root.optInt("schema", 0) != 1
                || !"https://assettocorsa.gg/wp-json/wp/v2/media".equals(root.optString("source"))) {
            throw new IOException("Fahrzeugbild-Manifest ist nicht verifiziert");
        }
        Map<String, JSONObject> result = new HashMap<>();
        JSONArray rows = root.getJSONArray("vehicles");
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String id = row.getString("vehicleId");
            if (result.put(id, row) != null) {
                throw new IOException("Doppelte Fahrzeugbild-ID: " + id);
            }
        }
        return result;
    }

    private static String readAsset(Context context, String name) throws IOException {
        try (InputStream input = context.getAssets().open(name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
