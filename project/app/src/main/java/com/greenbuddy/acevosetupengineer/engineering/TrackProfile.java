package com.greenbuddy.acevosetupengineer.engineering;

public final class TrackProfile {
    public final String layoutId;
    public final String gameVersion;
    public final double speedDemand;
    public final double bumpDemand;
    public final double tractionDemand;
    public final double brakingDemand;
    public final String verificationSource;
    public final String profileFingerprint;
    public final double lengthMeters;
    public final boolean verified;

    public TrackProfile(
            String layoutId,
            String gameVersion,
            double speedDemand,
            double bumpDemand,
            double tractionDemand,
            double brakingDemand,
            double lengthMeters,
            String verificationSource,
            String profileFingerprint,
            boolean verified) {
        this.layoutId = layoutId;
        this.gameVersion = gameVersion;
        this.speedDemand = bounded(speedDemand, "speedDemand");
        this.bumpDemand = bounded(bumpDemand, "bumpDemand");
        this.tractionDemand = bounded(tractionDemand, "tractionDemand");
        this.brakingDemand = bounded(brakingDemand, "brakingDemand");
        if (!Double.isFinite(lengthMeters) || lengthMeters <= 0) {
            throw new IllegalArgumentException("lengthMeters muss positiv sein");
        }
        this.lengthMeters = lengthMeters;
        this.verificationSource = verificationSource;
        this.profileFingerprint = profileFingerprint;
        this.verified = verified;
    }

    public void requireUsableFor(String requestedLayoutId, String requestedVersion) {
        if (!verified || verificationSource == null || verificationSource.trim().isEmpty()
                || profileFingerprint == null || profileFingerprint.trim().isEmpty()) {
            throw new SetupValidationException("Streckenprofil ist nicht verifiziert");
        }
        if (!layoutId.equals(requestedLayoutId) || !gameVersion.equals(requestedVersion)) {
            throw new SetupValidationException("Streckenprofil passt nicht exakt zu Layout oder Spielversion");
        }
    }

    private static double bounded(double value, String name) {
        if (!Double.isFinite(value) || value < -1 || value > 1) {
            throw new IllegalArgumentException(name + " muss zwischen -1 und 1 liegen");
        }
        return value;
    }
}
