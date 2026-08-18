package com.greenbuddy.acevosetupengineer.core;

import com.greenbuddy.acevosetupengineer.model.ExactCandidate;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * RacePlace publishes a versioned 0.8.1 setup package. Each complete LIVE round
 * fetches and scans the current package; the coordinator still verifies every
 * extracted file's binary vehicle signature.
 */
public final class RacePlacePackageProvider implements LiveProvider {
    private static final String PACKAGE_URL = "https://raceplace.racing/download/12287/";
    private static final String SOURCE_URL = "https://raceplace.racing/downloads/";
    private static final String GAME_VERSION = "0.8.1";
    private static final int MAX_ZIP_BYTES = 12_000_000;
    private static final int MAX_ENTRY_BYTES = 65_536;
    private static final int MAX_ENTRIES = 2_000;

    private final Map<String, byte[]> currentRoundFiles = new HashMap<>();

    @Override
    public String name() {
        return "RacePlace 0.8.1";
    }

    @Override
    public synchronized List<ExactCandidate> searchExact(SetupRequest request) throws IOException {
        currentRoundFiles.clear();
        byte[] zip = fetchPackage();
        List<ExactCandidate> matches = new ArrayList<>();
        try (ZipInputStream input = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry entry;
            int entries = 0;
            while ((entry = input.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) throw new IOException("ZIP enthält zu viele Einträge");
                if (entry.isDirectory() || !entry.getName().toLowerCase(Locale.ROOT).endsWith(".carsetup")) {
                    input.closeEntry();
                    continue;
                }
                String vehicleSlug = vehicleSlug(entry.getName());
                String layoutSlug = layoutSlug(entry.getName());
                // Return every same-car entry. The coordinator alone decides
                // whether metadata is EXACT; a non-exact entry may only become
                // a verified binary structure carrier.
                if (request.vehicle.providerSlug.equals(vehicleSlug)) {
                    byte[] bytes = readLimited(input, MAX_ENTRY_BYTES);
                    String providerId = entry.getName();
                    currentRoundFiles.put(providerId, bytes);
                    matches.add(new ExactCandidate(name(), providerId, vehicleSlug, layoutSlug,
                            GAME_VERSION, SOURCE_URL, PACKAGE_URL, fileName(entry.getName())));
                }
                input.closeEntry();
            }
        }
        return matches;
    }

    @Override
    public synchronized byte[] download(ExactCandidate candidate) throws IOException {
        byte[] bytes = currentRoundFiles.get(candidate.providerId);
        if (bytes == null) throw new IOException("RacePlace-Datei ist nicht mehr in derselben LIVE-Runde verfügbar");
        return bytes.clone();
    }

    private static byte[] fetchPackage() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(PACKAGE_URL).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(30_000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Accept", "application/zip");
        connection.setRequestProperty("User-Agent", "The-Godfather-of-EVO/1.0");
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            throw new IOException("HTTP " + status);
        }
        try (InputStream input = connection.getInputStream()) {
            return readLimited(input, MAX_ZIP_BYTES);
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] readLimited(InputStream input, int maximum) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > maximum) throw new IOException("Download überschreitet Sicherheitslimit");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    static String vehicleSlug(String path) {
        if (path.contains("/Ferrari 296 GT3/")) return "ferrari-296-gt3";
        if (path.contains("/Ford Mustang GT3/")) return "ford-mustang-gt3";
        if (path.contains("/Audi R8 LMS GT3 Evo II/")) return "audi-r8-lms-gt3-evo-ii";
        if (path.contains("/Porsche 992 GT3 R Rennsport/")) return "porsche-992-gt3-r-rennsport";
        if (path.contains("/BMW M4 GT3 Evo/")) return "bmw-m4-gt3-evo";
        return "";
    }

    static String layoutSlug(String path) {
        String upper = fileName(path).toUpperCase(Locale.ROOT);
        if (upper.contains("BHTC_INDY")) return "brands-hatch-indy";
        if (upper.contains("BHATCH")) return "brands-hatch";
        if (upper.contains("DON_INT")) return "donington-park-international";
        if (upper.contains("KYA")) return "kyalami";
        if (upper.contains("MON")) return "monza";
        if (upper.contains("NURSPRINT")) return "nurburgring-sprint";
        if (upper.contains("NOS24H") || upper.startsWith("24H-")) return "nurburgring-24h";
        if (upper.contains("WAT")) return "watkins-glen-short-inner-loop";
        return "";
    }

    private static String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }
}
