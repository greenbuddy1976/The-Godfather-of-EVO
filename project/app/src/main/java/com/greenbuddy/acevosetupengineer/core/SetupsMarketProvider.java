package com.greenbuddy.acevosetupengineer.core;

import com.greenbuddy.acevosetupengineer.model.ExactCandidate;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Public SetupsMarket index; every downloaded file is independently verified by the coordinator. */
public final class SetupsMarketProvider implements LiveProvider {
    private static final String API_ROOT = "https://zsxcienykmqaxtsiokgb.supabase.co";
    // Public Supabase publishable key used by the website. It is not a private credential.
    private static final String PUBLISHABLE_KEY = "sb_publishable_vxt0NiT3SOwO1VHFZoOHQw_m-n5nmrA";
    private static final int MAX_RESPONSE_BYTES = 1_000_000;

    @Override
    public String name() {
        return "SetupsMarket";
    }

    @Override
    public List<ExactCandidate> searchExact(SetupRequest request) throws IOException {
        String query = API_ROOT + "/rest/v1/setups?select=id,car,track,game_version,status,file_path"
                + "&status=eq.active"
                + "&car=eq." + encode(request.vehicle.providerSlug)
                + "&order=game_version.desc&limit=200";
        byte[] body = get(query, true);
        List<ExactCandidate> result = new ArrayList<>();
        try {
            JSONArray json = new JSONArray(new String(body, StandardCharsets.UTF_8));
            for (int i = 0; i < json.length(); i++) {
                JSONObject row = json.getJSONObject(i);
                String id = row.getString("id");
                String filePath = row.getString("file_path");
                result.add(new ExactCandidate(
                        name(),
                        id,
                        row.getString("car"),
                        row.getString("track"),
                        row.optString("game_version", ""),
                        "https://setupsmarket.com/setup/" + id,
                        API_ROOT + "/storage/v1/object/public/setups/" + encodePath(filePath),
                        id + ".carsetup"));
            }
        } catch (JSONException invalid) {
            throw new IOException("SetupsMarket lieferte ungültige Metadaten", invalid);
        }
        return result;
    }

    @Override
    public byte[] download(ExactCandidate candidate) throws IOException {
        return get(candidate.fileUrl, false);
    }

    private static byte[] get(String url, boolean apiHeaders) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(12_000);
        connection.setReadTimeout(18_000);
        connection.setRequestProperty("Accept", "application/json, application/octet-stream");
        connection.setRequestProperty("User-Agent", "The-Godfather-of-EVO/1.0");
        if (apiHeaders) {
            connection.setRequestProperty("apikey", PUBLISHABLE_KEY);
        }
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            throw new IOException("HTTP " + status);
        }
        try (InputStream input = connection.getInputStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > MAX_RESPONSE_BYTES) throw new IOException("Antwort ist zu groß");
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    private static String encode(String value) throws IOException {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name()).replace("+", "%20");
    }

    private static String encodePath(String value) throws IOException {
        String[] parts = value.split("/");
        StringBuilder encoded = new StringBuilder();
        for (String part : parts) {
            if (encoded.length() > 0) encoded.append('/');
            encoded.append(encode(part));
        }
        return encoded.toString();
    }
}
