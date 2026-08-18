package com.greenbuddy.acevosetupengineer.model;

public final class ExactCandidate {
    public final String provider;
    public final String providerId;
    public final String vehicleSlug;
    public final String layoutSlug;
    public final String gameVersion;
    public final String sourceUrl;
    public final String fileUrl;
    public final String fileName;

    public ExactCandidate(
            String provider,
            String providerId,
            String vehicleSlug,
            String layoutSlug,
            String gameVersion,
            String sourceUrl,
            String fileUrl,
            String fileName) {
        this.provider = provider;
        this.providerId = providerId;
        this.vehicleSlug = vehicleSlug;
        this.layoutSlug = layoutSlug;
        this.gameVersion = gameVersion;
        this.sourceUrl = sourceUrl;
        this.fileUrl = fileUrl;
        this.fileName = fileName;
    }
}
