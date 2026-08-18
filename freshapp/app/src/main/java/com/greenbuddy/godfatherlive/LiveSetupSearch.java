package com.greenbuddy.godfatherlive;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class LiveSetupSearch {
    private static final String MARKET_API = "https://zsxcienykmqaxtsiokgb.supabase.co/rest/v1/setups"
            + "?select=*&status=eq.active&limit=1000";
    private static final String MARKET_KEY = "sb_publishable_vxt0NiT3SOwO1VHFZoOHQw_m-n5nmrA";
    private static final String MARKET_DOWNLOAD = "https://setupsmarket.com/setup/%s/download";
    private static final String RACEPLACE_DOWNLOADS = "https://raceplace.racing/downloads/";
    private static final Pattern DOWNLOAD_LINK = Pattern.compile(
            "href\\s*=\\s*[\"']([^\"']*/download/\\d+/?(?:\\?[^\"']*)?)[\"']",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VERSION = Pattern.compile(
            "(?:version|vers\\.|v)\\s*([0-9]+(?:\\.[0-9]+){1,2})", Pattern.CASE_INSENSITIVE);

    interface Progress { void update(String message); }

    static final class Result {
        final List<SourceSetup> setups;
        final List<String> notices;
        final int firstRoundCount;
        final int secondRoundCount;

        Result(List<SourceSetup> setups, List<String> notices, int firstRoundCount, int secondRoundCount) {
            this.setups = setups;
            this.notices = notices;
            this.firstRoundCount = firstRoundCount;
            this.secondRoundCount = secondRoundCount;
        }
    }

    static Result runTwoRounds(String selectedCar, String selectedTrack, Progress progress) throws Exception {
        progress.update("LIVE-Runde 1/2: SetupsMarket + RacePlace …");
        Round first = fetchRound(selectedCar, selectedTrack, System.nanoTime());
        progress.update("LIVE-Runde 2/2: Quellen vollständig neu prüfen …");
        Round second = fetchRound(selectedCar, selectedTrack, System.nanoTime());

        Map<String, SourceSetup> secondByKey = new LinkedHashMap<>();
        for (SourceSetup setup : second.setups) secondByKey.put(setup.stableKey(), setup);
        List<SourceSetup> verified = new ArrayList<>();
        for (SourceSetup setup : first.setups) {
            SourceSetup repeated = secondByKey.get(setup.stableKey());
            if (repeated != null && QueryLogic.exact(setup.car, repeated.car)
                    && QueryLogic.exact(setup.track, repeated.track)) verified.add(repeated);
        }

        List<String> notices = new ArrayList<>();
        notices.addAll(first.notices);
        notices.addAll(second.notices);
        if (verified.isEmpty() && first.setups.isEmpty() && second.setups.isEmpty()) {
            throw new IllegalStateException(notices.isEmpty()
                    ? "Keine aktuelle Quelle lieferte verwertbare .carsetup-Treffer."
                    : String.join(" · ", notices));
        }
        return new Result(deduplicate(verified), notices, first.setups.size(), second.setups.size());
    }

    static byte[] downloadFresh(SourceSetup setup) throws Exception {
        byte[] bytes;
        if (setup.source == SourceSetup.Source.SETUPSMARKET) {
            bytes = getBytes(setup.downloadUrl, null, 2_000_000, "application/octet-stream");
        } else {
            byte[] archive = getBytes(setup.downloadUrl, null, 20_000_000, "application/zip");
            bytes = extractExactEntry(archive, setup.archiveEntry);
        }
        QueryLogic.requireRealCarsetup(bytes);
        return bytes;
    }

    static String sha256(byte[] bytes) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
        StringBuilder out = new StringBuilder();
        for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b));
        return out.toString();
    }

    private static Round fetchRound(String selectedCar, String selectedTrack, long nonce) {
        Round round = new Round();
        try {
            round.setups.addAll(fetchMarket(selectedCar, selectedTrack, nonce));
        } catch (Exception ex) {
            round.notices.add("SetupsMarket: " + safeMessage(ex));
        }
        try {
            RacePlaceArchive archive = discoverRacePlace(nonce);
            round.setups.addAll(scanRacePlace(archive, selectedCar, selectedTrack));
        } catch (Exception ex) {
            round.notices.add("RacePlace: " + safeMessage(ex));
        }
        return round;
    }

    private static List<SourceSetup> fetchMarket(String selectedCar, String selectedTrack, long nonce) throws Exception {
        byte[] payload = getBytes(MARKET_API + "&_live=" + nonce, MARKET_KEY, 3_000_000,
                "application/json");
        JSONArray rows = new JSONArray(new String(payload, StandardCharsets.UTF_8));
        List<SourceSetup> setups = new ArrayList<>();
        for (int i = 0; i < rows.length(); i++) {
            JSONObject row = rows.getJSONObject(i);
            String id = row.optString("id");
            String car = marketCarName(row.optString("car"));
            String track = marketTrackName(row.optString("track"));
            String version = row.optString("game_version");
            if (id.isEmpty() || car.isEmpty() || track.isEmpty() || !QueryLogic.currentVersion(version)) continue;
            if (!selectedCar.isEmpty() && !QueryLogic.exact(selectedCar, car)) continue;
            if (!selectedTrack.isEmpty() && !QueryLogic.exact(selectedTrack, track)) continue;
            String rawPath = first(row, "file_name", "filename", "file_path");
            String fileName = lastPathPart(rawPath);
            setups.add(new SourceSetup(SourceSetup.Source.SETUPSMARKET, id, car, track, version,
                    fileName, row.optString("title"), row.optString("description"),
                    String.format(Locale.ROOT, MARKET_DOWNLOAD, id), ""));
        }
        return deduplicate(setups);
    }

    private static RacePlaceArchive discoverRacePlace(long nonce) throws Exception {
        byte[] pageBytes = getBytes(RACEPLACE_DOWNLOADS + "?live=" + nonce, null, 2_000_000, "text/html");
        String page = new String(pageBytes, StandardCharsets.UTF_8);
        String lower = page.toLowerCase(Locale.ROOT);
        int start = lower.indexOf("assetto corsa evo free baseline");
        if (start < 0) start = lower.indexOf("assetto corsa evo");
        if (start < 0) throw new IllegalStateException("ACE-EVO-Baseline-Bereich nicht gefunden");
        String relevant = page.substring(start, Math.min(page.length(), start + 30_000));
        Matcher linkMatcher = DOWNLOAD_LINK.matcher(relevant);
        if (!linkMatcher.find()) throw new IllegalStateException("aktueller ZIP-Link nicht gefunden");
        URL resolved = new URL(new URL(RACEPLACE_DOWNLOADS), linkMatcher.group(1));
        Matcher versionMatcher = VERSION.matcher(relevant.substring(0, Math.min(relevant.length(), 5_000)));
        String version = versionMatcher.find() ? versionMatcher.group(1) : "";
        if (!QueryLogic.currentVersion(version)) {
            throw new IllegalStateException("Quellversion ist nicht als 0.8.x bestätigt");
        }
        byte[] archive = getBytes(resolved.toString(), null, 20_000_000, "application/zip");
        return new RacePlaceArchive(resolved.toString(), version, archive);
    }

    private static List<SourceSetup> scanRacePlace(RacePlaceArchive archive,
                                                    String selectedCar, String selectedTrack) throws Exception {
        List<SourceSetup> setups = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive.bytes), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                String name = entry.getName().replace('\\', '/');
                if (entry.isDirectory() || !name.toLowerCase(Locale.ROOT).endsWith(".carsetup")) {
                    zip.closeEntry();
                    continue;
                }
                String[] parts = name.split("/");
                if (parts.length < 3) {
                    zip.closeEntry();
                    continue;
                }
                String car = parts[parts.length - 3].trim();
                String file = parts[parts.length - 1];
                String track = exactRacePlaceTrack(parts[parts.length - 2].trim(), file);
                if (track.isEmpty()) {
                    zip.closeEntry();
                    continue;
                }
                if (!selectedCar.isEmpty() && !QueryLogic.exact(selectedCar, car)) {
                    zip.closeEntry();
                    continue;
                }
                if (!selectedTrack.isEmpty() && !QueryLogic.exact(selectedTrack, track)) {
                    zip.closeEntry();
                    continue;
                }
                setups.add(new SourceSetup(SourceSetup.Source.RACEPLACE,
                        archive.url + "#" + name, car, track, archive.version, file,
                        file, "RacePlace/DTVR Baseline", archive.url, name));
                zip.closeEntry();
            }
        }
        return deduplicate(setups);
    }

    private static byte[] extractExactEntry(byte[] archive, String target) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().replace('\\', '/').equals(target)) {
                    return readLimited(zip, 2_000_000);
                }
                zip.closeEntry();
            }
        }
        throw new IllegalStateException("Bestätigter ZIP-Eintrag ist nicht mehr vorhanden");
    }

    private static byte[] getBytes(String address, String apiKey, int maxBytes, String accept) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(45_000);
        connection.setInstanceFollowRedirects(true);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", accept);
        connection.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setRequestProperty("User-Agent", "The-Godfather-of-EVO-LIVE/2.0.0");
        if (apiKey != null) connection.setRequestProperty("apikey", apiKey);
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) {
            connection.disconnect();
            throw new IllegalStateException("HTTP " + status);
        }
        try (InputStream input = connection.getInputStream()) {
            return readLimited(input, maxBytes);
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] readLimited(InputStream input, int maxBytes) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            total += read;
            if (total > maxBytes) throw new IllegalStateException("Download überschreitet Sicherheitslimit");
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static List<SourceSetup> deduplicate(List<SourceSetup> input) {
        Map<String, SourceSetup> unique = new LinkedHashMap<>();
        for (SourceSetup setup : input) unique.put(setup.stableKey(), setup);
        return new ArrayList<>(unique.values());
    }

    private static String first(JSONObject row, String... names) {
        for (String name : names) {
            String value = row.optString(name);
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static String lastPathPart(String path) throws Exception {
        if (path == null || path.isEmpty()) return "";
        String normalized = path.replace('\\', '/');
        String last = normalized.substring(normalized.lastIndexOf('/') + 1);
        return URLDecoder.decode(last, StandardCharsets.UTF_8.name());
    }

    private static String marketCarName(String slug) {
        return prettySlug(slug, false);
    }

    private static String marketTrackName(String slug) {
        String key = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "brands-hatch" -> "Brands Hatch GP";
            case "donington-park" -> "Donington Park International";
            case "kyalami" -> "Kyalami Grand Prix";
            case "nurburgring" -> "Nürburgring GP";
            case "spa-francorchamps" -> "Spa-Francorchamps";
            case "watkins-glen" -> "Watkins Glen Grand Prix";
            default -> prettySlug(slug, true);
        };
    }

    private static String prettySlug(String slug, boolean track) {
        if (slug == null) return "";
        String[] parts = slug.trim().replace('_', '-').split("-+");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            String lower = part.toLowerCase(Locale.ROOT);
            String word = switch (lower) {
                case "bmw", "vw", "gt2", "gt3", "gt4", "rs", "lm", "gp", "nd", "mx" -> lower.toUpperCase(Locale.ROOT);
                case "r8" -> "R8";
                default -> Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
            };
            if (out.length() > 0) out.append(' ');
            out.append(word);
        }
        String value = out.toString();
        if (track && value.equals("Nurburgring")) return "Nürburgring GP";
        return value;
    }

    private static String exactRacePlaceTrack(String folder, String fileName) {
        String folderKey = QueryLogic.key(folder);
        String fileKey = QueryLogic.key(fileName);
        return switch (folderKey) {
            case "brandshatch" -> {
                if (fileKey.contains("bhtcindy")) yield "Brands Hatch Indy";
                if (fileKey.contains("bhatch")) yield "Brands Hatch GP";
                yield "";
            }
            case "doningtonpark" -> fileKey.contains("donint")
                    ? "Donington Park International" : "";
            case "kyalami" -> fileKey.contains("kya") ? "Kyalami Grand Prix" : "";
            case "monza" -> fileKey.contains("mon") ? "Monza" : "";
            case "nurburgring" -> {
                if (fileKey.contains("nos24h") || fileKey.startsWith("24h")) yield "Nürburgring 24h";
                if (fileKey.contains("nursprint")) yield "Nürburgring Sprint";
                yield "";
            }
            case "watkinsgleninternational" -> fileKey.contains("watsil")
                    ? "Watkins Glen Short Inner Loop" : "";
            default -> "";
        };
    }

    private static String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }

    private static final class Round {
        final List<SourceSetup> setups = new ArrayList<>();
        final List<String> notices = new ArrayList<>();
    }

    private static final class RacePlaceArchive {
        final String url;
        final String version;
        final byte[] bytes;
        RacePlaceArchive(String url, String version, byte[] bytes) {
            this.url = url;
            this.version = version;
            this.bytes = bytes;
        }
    }

    private LiveSetupSearch() {}
}
