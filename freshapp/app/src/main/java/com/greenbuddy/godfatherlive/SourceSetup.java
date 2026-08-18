package com.greenbuddy.godfatherlive;

import java.util.Locale;

final class SourceSetup {
    enum Source { SETUPSMARKET, RACEPLACE }

    final Source source;
    final String sourceId;
    final String car;
    final String track;
    final String gameVersion;
    final String fileName;
    final String title;
    final String description;
    final String downloadUrl;
    final String archiveEntry;

    SourceSetup(Source source, String sourceId, String car, String track,
                String gameVersion, String fileName, String title, String description,
                String downloadUrl, String archiveEntry) {
        this.source = source;
        this.sourceId = clean(sourceId);
        this.car = clean(car);
        this.track = clean(track);
        this.gameVersion = clean(gameVersion);
        this.fileName = QueryLogic.safeCarsetupName(fileName, car, track);
        this.title = clean(title);
        this.description = clean(description);
        this.downloadUrl = clean(downloadUrl);
        this.archiveEntry = clean(archiveEntry);
    }

    String stableKey() {
        return source.name() + "|" + sourceId + "|" + QueryLogic.key(car) + "|"
                + QueryLogic.key(track) + "|" + QueryLogic.key(gameVersion) + "|"
                + QueryLogic.key(archiveEntry);
    }

    String searchableText() {
        return (title + " " + description + " " + fileName).toLowerCase(Locale.ROOT);
    }

    String displayLabel() {
        String provider = source == Source.SETUPSMARKET ? "SetupsMarket" : "RacePlace";
        String version = gameVersion.isEmpty() ? "Version nicht angegeben" : "v" + gameVersion;
        return provider + " · " + version + " · " + fileName;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
