package com.greenbuddy.acevosetupengineer.engineering;

/**
 * A per-vehicle parameter definition. Values may only be loaded from a separately
 * verified engineering manifest; LIVE setup numbers are never accepted here.
 */
public final class ParameterDefinition {
    public final ParameterKey key;
    public final double minimum;
    public final double maximum;
    public final double step;
    public final double engineeringAnchor;
    public final double maximumFineTuneSteps;
    public final double styleResponse;
    public final double speedResponse;
    public final double bumpResponse;
    public final double tractionResponse;
    public final double brakingResponse;
    public final String unit;
    public final String verificationSource;
    public final boolean verified;
    public final boolean engineeringAnchorVerified;
    public final boolean responseModelVerified;
    public final boolean binaryWriteVerified;

    public ParameterDefinition(
            ParameterKey key,
            double minimum,
            double maximum,
            double step,
            double engineeringAnchor,
            double maximumFineTuneSteps,
            double styleResponse,
            double speedResponse,
            double bumpResponse,
            double tractionResponse,
            double brakingResponse,
            String unit,
            String verificationSource,
            boolean verified,
            boolean engineeringAnchorVerified,
            boolean responseModelVerified,
            boolean binaryWriteVerified) {
        this.key = key;
        this.minimum = minimum;
        this.maximum = maximum;
        this.step = step;
        this.engineeringAnchor = engineeringAnchor;
        this.maximumFineTuneSteps = maximumFineTuneSteps;
        this.styleResponse = styleResponse;
        this.speedResponse = speedResponse;
        this.bumpResponse = bumpResponse;
        this.tractionResponse = tractionResponse;
        this.brakingResponse = brakingResponse;
        this.unit = unit;
        this.verificationSource = verificationSource;
        this.verified = verified;
        this.engineeringAnchorVerified = engineeringAnchorVerified;
        this.responseModelVerified = responseModelVerified;
        this.binaryWriteVerified = binaryWriteVerified;
        validateDefinition();
    }

    public static ParameterDefinition verifiedRangeOnly(
            ParameterKey key,
            double minimum,
            double maximum,
            double step,
            String unit,
            String verificationSource,
            boolean binaryWriteVerified) {
        return new ParameterDefinition(key, minimum, maximum, step, Double.NaN,
                4.0, 0, 0, 0, 0, 0, unit, verificationSource,
                true, false, false, binaryWriteVerified);
    }

    public double clampAndRound(double value) {
        double clamped = Math.max(minimum, Math.min(maximum, value));
        double steps = Math.rint((clamped - minimum) / step);
        double rounded = minimum + steps * step;
        return Math.max(minimum, Math.min(maximum, rounded));
    }

    public boolean contains(double value) {
        return Double.isFinite(value) && value >= minimum - 1e-9 && value <= maximum + 1e-9;
    }

    public boolean fineTuneWritable() {
        return verified && binaryWriteVerified;
    }

    public boolean selfCalcReady() {
        return verified && binaryWriteVerified && engineeringAnchorVerified
                && responseModelVerified && contains(engineeringAnchor);
    }

    private void validateDefinition() {
        if (key == null) throw new IllegalArgumentException("Parameter fehlt");
        if (!Double.isFinite(minimum) || !Double.isFinite(maximum) || minimum >= maximum) {
            throw new IllegalArgumentException("Ungültiger Parameterbereich für " + key);
        }
        if (!Double.isFinite(step) || step <= 0 || step > maximum - minimum) {
            throw new IllegalArgumentException("Ungültige Schrittweite für " + key);
        }
        if (engineeringAnchorVerified && !contains(engineeringAnchor)) {
            throw new IllegalArgumentException("Engineering-Anker außerhalb des Bereichs für " + key);
        }
        if (!Double.isFinite(maximumFineTuneSteps) || maximumFineTuneSteps <= 0) {
            throw new IllegalArgumentException("Ungültige Fine-Tuning-Grenze für " + key);
        }
        if (verified && (verificationSource == null || verificationSource.trim().isEmpty())) {
            throw new IllegalArgumentException("Verifiziertes Profil benötigt eine Quellenreferenz");
        }
        if (responseModelVerified && !engineeringAnchorVerified) {
            throw new IllegalArgumentException("Reaktionsmodell benötigt einen verifizierten Engineering-Anker");
        }
    }
}
