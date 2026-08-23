package com.greenbuddy.acevosetupengineer.core;

import android.content.Context;
import android.content.SharedPreferences;

import com.greenbuddy.acevosetupengineer.binary.CarSetupInspection;
import com.greenbuddy.acevosetupengineer.binary.CarSetupInspector;
import com.greenbuddy.acevosetupengineer.model.CatalogItem;
import com.greenbuddy.acevosetupengineer.util.Hashing;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SetupStockSync {
    private static final String[] INDEX_URLS = {
            "https://setupsmarket.com/",
            "https://setupsmarket.com/setups"
    };
    private static final String STORAGE_ROOT =
            "https://zsxcienykmqaxtsiokgb.supabase.co/storage/v1/object/public/setups/";
    private static final Pattern SETUP_LINK = Pattern.compile(
            "(?:https://setupsmarket\\.com)?/setup/([0-9a-fA-F-]{36})");
    private static final Pattern ABS_CARSETUP = Pattern.compile(
            "https://[^\\\"'<>\\s]+\\.carsetup(?:\\?[^\\\"'<>\\s]*)?",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_PATH = Pattern.compile(
            "\\\"file_path\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final int MAX_PER_RUN = 32;

    private final SharedPreferences prefs;
    private final File stockDir;

    public SetupStockSync(Context context) {
        Context app = context.getApplicationContext();
        prefs = app.getSharedPreferences("setup_stock_sync_v3", Context.MODE_PRIVATE);
        stockDir = new File(app.getFilesDir(), "setup-stock");
        if (!stockDir.exists()) stockDir.mkdirs();
    }

    public synchronized Result syncOnce() {
        int added = 0;
        int failed = 0;
        int dead = 0;
        int processed = 0;
        try {
            Set<String> ids = discoverIds();
            for (String id : ids) {
                if (processed >= MAX_PER_RUN) break;
                if (prefs.getBoolean("done_" + id, false)
                        || prefs.getBoolean("dead_" + id, false)) continue;
                processed++;
                try {
                    byte[] bytes = downloadFromDetail(id);
                    CarSetupInspection inspection = CarSetupInspector.inspect(bytes);
                    if (!inspection.structurallyValid) {
                        throw new IllegalStateException("ungültige .carsetup-Struktur");
                    }
                    File out = new File(stockDir, id + ".carsetup");
                    try (FileOutputStream stream = new FileOutputStream(out, false)) {
                        stream.write(bytes);
                    }
                    prefs.edit()
                            .putBoolean("done_" + id, true)
                            .putString("sha_" + id, Hashing.sha256(bytes))
                            .remove("tries_" + id)
                            .apply();
                    added++;
                } catch (Exception error) {
                    failed++;
                    int tries = prefs.getInt("tries_" + id, 0) + 1;
                    if (tries >= 3) {
                        File partial = new File(stockDir, id + ".carsetup");
                        if (partial.exists()) partial.delete();
                        prefs.edit()
                                .putBoolean("dead_" + id, true)
                                .remove("tries_" + id)
                                .apply();
                        dead++;
                    } else {
                        prefs.edit().putInt("tries_" + id, tries).apply();
                    }
                }
            }
        } catch (Exception error) {
            failed++;
        }
        return new Result(added, failed, dead, localCount());
    }

    public VerifiedStructureCarrier findCarrier(CatalogItem vehicle) {
        if (vehicle == null || !vehicle.hasVerifiedBinaryIdentity()) return null;
        File[] files = stockDir.listFiles((dir, name) -> name.endsWith(".carsetup"));
        if (files == null) return null;
        for (File file : files) {
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                CarSetupInspection inspection = CarSetupInspector.inspect(bytes);
                if (!inspection.structurallyValid) continue;
                if (!inspection.vehicleSignature.startsWith(vehicle.expectedSignaturePrefix)) continue;
                return new VerifiedStructureCarrier(bytes, Hashing.sha256(bytes),
                        inspection.vehicleSignature,
                        "Synchronisierter Setup-Bestand / " + file.getName(), false);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Set<String> discoverIds() throws Exception {
        Set<String> ids = new LinkedHashSet<>();
        for (String url : INDEX_URLS) {
            try {
                String html = new String(get(url, 2_000_000), StandardCharsets.UTF_8);
                Matcher matcher = SETUP_LINK.matcher(html);
                while (matcher.find()) ids.add(matcher.group(1).toLowerCase(Locale.ROOT));
            } catch (Exception ignored) {
            }
        }
        if (ids.isEmpty()) throw new IllegalStateException("keine Setup-Links gefunden");
        return ids;
    }

    private byte[] downloadFromDetail(String id) throws Exception {
        String html = new String(get("https://setupsmarket.com/setup/" + id, 2_000_000),
                StandardCharsets.UTF_8)
                .replace("&amp;", "&")
                .replace("\\u002F", "/");
        String direct = null;
        Matcher absolute = ABS_CARSETUP.matcher(html);
        if (absolute.find()) direct = absolute.group();
        if (direct == null) {
            Matcher filePath = FILE_PATH.matcher(html);
            if (filePath.find()) direct = STORAGE_ROOT + encodePath(filePath.group(1));
        }
        if (direct == null) throw new IllegalStateException("kein Download-Link auf Detailseite");
        return get(direct, 131_072);
    }

    private static byte[] get(String url, int maxBytes) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(15_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", "The-Godfather-of-EVO/2.2");
        connection.setRequestProperty("Accept", "text/html,application/json,application/octet-stream,*/*");
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            throw new IllegalStateException("HTTP " + status);
        }
        try (InputStream input = connection.getInputStream();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int total = 0;
            int count;
            while ((count = input.read(buffer)) != -1) {
                total += count;
                if (total > maxBytes) throw new IllegalStateException("Antwort zu groß");
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    private int localCount() {
        File[] files = stockDir.listFiles((dir, name) -> name.endsWith(".carsetup"));
        return files == null ? 0 : files.length;
    }

    private static String encodePath(String path) throws Exception {
        StringBuilder output = new StringBuilder();
        for (String part : path.split("/")) {
            if (output.length() > 0) output.append('/');
            output.append(URLEncoder.encode(part, StandardCharsets.UTF_8.name())
                    .replace("+", "%20"));
        }
        return output.toString();
    }

    public record Result(int added, int failed, int dead, int localCount) {
    }
}
