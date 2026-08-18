package com.greenbuddy.godfatherlive;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.Locale;

final class QueryLogic {
    static final String CURRENT_VERSION_PREFIX = "0.8";

    enum Style {
        SCHUMACHER("Michael Schumacher – Hotlap/Qualifying bevorzugen",
                new String[]{"qualifying", "hotlap", "quali", "attack", "fast"}),
        ROEHRL("Walter Röhrl – direkt und kontrolliert bevorzugen",
                new String[]{"race", "control", "balanced", "direct"}),
        DUESEL("Dieter Düsel – stabile Rennbasis bevorzugen",
                new String[]{"stable", "baseline", "race", "safe"}),
        HERTHA("Oma Hertha – sicher/Langstrecke bevorzugen",
                new String[]{"safe", "long", "endurance", "wet", "stable"});

        final String label;
        final String[] terms;
        Style(String label, String[] terms) { this.label = label; this.terms = terms; }
        @Override public String toString() { return label; }
    }

    enum FineTune {
        NONE("Kein Zusatzwunsch – Quelldatei unverändert", new String[]{}),
        REAR("Ruhiges Heck bevorzugen", new String[]{"stable", "rear", "safe"}),
        BRAKES("Stabiles Bremsen bevorzugen", new String[]{"brake", "stable"}),
        BUMPS("Curbs/Bodenwellen bevorzugen", new String[]{"curb", "bump", "baseline"}),
        LONG_RUN("Langer Stint bevorzugen", new String[]{"long", "endurance", "race"});

        final String label;
        final String[] terms;
        FineTune(String label, String[] terms) { this.label = label; this.terms = terms; }
        @Override public String toString() { return label; }
    }

    static String key(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replace("spa francorchamps", "spafrancorchamps")
                .replace("nürburgring", "nurburgring");
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

    static Comparator<SourceSetup> preferenceComparator(Style style, FineTune fineTune) {
        return Comparator.<SourceSetup>comparingInt(s -> score(s, style, fineTune)).reversed()
                .thenComparing(s -> s.source.name())
                .thenComparing(s -> s.fileName, String.CASE_INSENSITIVE_ORDER);
    }

    static int score(SourceSetup setup, Style style, FineTune fineTune) {
        String text = setup.searchableText();
        int score = 0;
        for (String term : style.terms) if (text.contains(term)) score += 3;
        for (String term : fineTune.terms) if (text.contains(term)) score += 2;
        if (currentVersion(setup.gameVersion)) score += 10;
        if (setup.source == SourceSetup.Source.SETUPSMARKET) score += 1;
        return score;
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

    static void requireRealCarsetup(byte[] bytes) {
        if (bytes == null || bytes.length < 32) throw new IllegalArgumentException("Datei ist zu klein");
        if (bytes.length > 2_000_000) throw new IllegalArgumentException("Datei ist ungewöhnlich groß");
        if (bytes[0] == 'P' && bytes[1] == 'K') throw new IllegalArgumentException("ZIP statt .carsetup empfangen");
        String prefix = new String(bytes, 0, Math.min(bytes.length, 96), StandardCharsets.UTF_8)
                .trim().toLowerCase(Locale.ROOT);
        if (prefix.startsWith("<html") || prefix.startsWith("<!doctype")
                || prefix.startsWith("{") || prefix.startsWith("[")) {
            throw new IllegalArgumentException("Text/Fehlerseite statt .carsetup empfangen");
        }
    }

    private static String cleanPart(String value) {
        String part = value == null ? "Setup" : value;
        part = part.replaceAll("[\\p{Cntrl}\\\\/:*?\"<>|]+", "_").trim();
        return part.isEmpty() ? "Setup" : part;
    }

    private QueryLogic() {}
}
