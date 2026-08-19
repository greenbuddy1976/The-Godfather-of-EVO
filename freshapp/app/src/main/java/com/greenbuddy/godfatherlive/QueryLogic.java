package com.greenbuddy.godfatherlive;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

final class QueryLogic {
    static final String CURRENT_VERSION_PREFIX = "0.8";

    static String key(String value) {
        if (value == null) return "";
        String normalized = value
                .replace("β", "beta")
                .replace("–", "-")
                .replace("—", "-");
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\((?:19|20)\\d{2}\\)", "")
                .replace("volkswagen", "vw")
                .replace("mercedes-benz", "mercedes")
                .replace("mercedes-amg", "mercedes")
                .replace("spa francorchamps", "spafrancorchamps")
                .replace("spa-francorchamps", "spafrancorchamps")
                .replace("nürburgring", "nurburgring")
                .replace("nuerburgring", "nurburgring")
                .replace("mount panorama", "bathurst")
                .replace("suzuka grand prix", "suzuka")
                .replace("kyalami grand prix", "kyalami")
                .replace("circuit of the americas", "cota")
                .replace("donington park international", "doningtonpark")
                .replace("watkins glen international", "watkinsglen")
                .replace("watkins glen short inner loop", "watkinsglenshortinnerloop")
                .replace("mazda mx-5", "mazda mx5")
                .replace("honda s-2000", "honda s2000");
        return normalized.replaceAll("[^a-z0-9]+", "");
    }

    static boolean exact(String left, String right) {
        String a = key(left);
        String b = key(right);
        return !a.isEmpty() && a.equals(b);
    }

    static boolean currentVersion(String version) {
        String value = version == null ? "" : version.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("v")) value = value.substring(1);
        return value.startsWith(CURRENT_VERSION_PREFIX);
    }

    static String safeCarsetupName(String candidate, String car, String track) {
        String value = candidate == null ? "" : candidate.trim();
        value = value.replace('\\', '_').replace('/', '_')
                .replaceAll("[\\p{Cntrl}:*?\"<>|]+", "_")
                .replaceAll("\\s+", " ").trim();
        if (value.isEmpty()) value = cleanPart(car) + " - " + cleanPart(track);
        value = value.replaceAll("(?i)\\.(txt|zip|json|html?)$", "");
        if (!value.toLowerCase(Locale.ROOT).endsWith(".carsetup")) value += ".carsetup";
        if (value.length() > 120) value = value.substring(0, 111).trim() + ".carsetup";
        return value;
    }

    static String generatedName(String car, String track, SetupEngine.Profile profile) {
        String base = cleanPart(car) + " - " + cleanPart(track) + " - "
                + cleanPart(profile.title).replace(' ', '_');
        return safeCarsetupName(base, car, track);
    }

    static void requireRealCarsetup(byte[] bytes) {
        if (bytes == null || bytes.length < 32) throw new IllegalArgumentException("Datei ist zu klein");
        if (bytes.length > 2_000_000) throw new IllegalArgumentException("Datei ist ungewoehnlich gross");
        if (bytes.length >= 2 && bytes[0] == 'P' && bytes[1] == 'K') {
            throw new IllegalArgumentException("ZIP statt .carsetup empfangen");
        }
        String prefix = new String(bytes, 0, Math.min(bytes.length, 96), StandardCharsets.UTF_8)
                .trim().toLowerCase(Locale.ROOT);
        if (prefix.startsWith("<html") || prefix.startsWith("<!doctype")
                || prefix.startsWith("{") || prefix.startsWith("[")) {
            throw new IllegalArgumentException("Text/Fehlerseite statt .carsetup empfangen");
        }
    }

    private static String cleanPart(String value) {
        String part = value == null ? "Setup" : value;
        part = part.replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]+", "_")
                .replaceAll("\\s+", " ").trim();
        return part.isEmpty() ? "Setup" : part;
    }

    private QueryLogic() {}
}
