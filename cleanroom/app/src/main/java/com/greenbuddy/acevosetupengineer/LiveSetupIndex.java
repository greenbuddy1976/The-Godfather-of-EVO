package com.greenbuddy.acevosetupengineer;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class LiveSetupIndex {
    private static final String MARKET_API = "https://zsxcienykmqaxtsiokgb.supabase.co/rest/v1/setups?select=id,car,track,game_version,file_path,status&status=eq.active&limit=1000";
    private static final String MARKET_KEY = "sb_publishable_vxt0NiT3SOwO1VHFZoOHQw_m-n5nmrA";
    private static final String RACEPLACE_ZIP = "https://raceplace.racing/download/12287/";
    private static final String PREFS = "live_setup_index_v111";

    record Snapshot(int marketSetups, int marketCars, int racePlaceSetups, long checkedAt) {}
    record Match(int count, String firstId) {}

    static Snapshot refresh(Context context) throws Exception {
        byte[] market = get(MARKET_API, MARKET_KEY, 2_000_000);
        JSONArray rows = new JSONArray(new String(market, StandardCharsets.UTF_8));
        Set<String> cars = new HashSet<>();
        for (int i = 0; i < rows.length(); i++) cars.add(rows.getJSONObject(i).optString("car"));

        int racePlaceCount = countRacePlace();
        long now = System.currentTimeMillis();
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("market", rows.toString())
                .putInt("market_count", rows.length())
                .putInt("market_cars", cars.size())
                .putInt("raceplace_count", racePlaceCount)
                .putLong("checked_at", now)
                .apply();
        return new Snapshot(rows.length(), cars.size(), racePlaceCount, now);
    }

    static Snapshot cached(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return new Snapshot(p.getInt("market_count", 0), p.getInt("market_cars", 0),
                p.getInt("raceplace_count", 0), p.getLong("checked_at", 0));
    }

    static Match find(Context context, String car, String track) {
        try {
            String carKey = SetupEngine.key(car);
            String trackKey = SetupEngine.key(track);
            JSONArray rows = new JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString("market", "[]"));
            int count = 0;
            String first = "";
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                String rowCar = SetupEngine.key(row.optString("car"));
                String rowTrack = SetupEngine.key(row.optString("track"));
                String game = row.optString("game_version");
                if (same(carKey, rowCar) && same(trackKey, rowTrack) && game.startsWith("0.8")) {
                    count++;
                    if (first.isEmpty()) first = row.optString("id");
                }
            }
            return new Match(count, first);
        } catch (Exception ignored) {
            return new Match(0, "");
        }
    }

    private static boolean same(String a, String b) {
        return a.equals(b) || (a.length() > 8 && b.contains(a)) || (b.length() > 8 && a.contains(b));
    }

    private static int countRacePlace() throws Exception {
        HttpURLConnection c = open(RACEPLACE_ZIP, null);
        int count = 0;
        try (InputStream raw = c.getInputStream(); ZipInputStream zip = new ZipInputStream(raw)) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                if (!e.isDirectory() && e.getName().toLowerCase().endsWith(".carsetup")) count++;
                zip.closeEntry();
            }
        } finally { c.disconnect(); }
        return count;
    }

    private static byte[] get(String url, String key, int max) throws Exception {
        HttpURLConnection c = open(url, key);
        try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0, n;
            while ((n = in.read(buffer)) >= 0) {
                total += n;
                if (total > max) throw new IllegalStateException("Online-Index zu groß");
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } finally { c.disconnect(); }
    }

    private static HttpURLConnection open(String url, String key) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(12_000);
        c.setReadTimeout(30_000);
        c.setInstanceFollowRedirects(true);
        c.setRequestProperty("Accept", "application/json, application/zip");
        c.setRequestProperty("User-Agent", "The-Godfather-of-EVO/1.1.1");
        if (key != null) c.setRequestProperty("apikey", key);
        int status = c.getResponseCode();
        if (status < 200 || status >= 300) throw new IllegalStateException("Quelle HTTP " + status);
        return c;
    }

    private LiveSetupIndex() {}
}
