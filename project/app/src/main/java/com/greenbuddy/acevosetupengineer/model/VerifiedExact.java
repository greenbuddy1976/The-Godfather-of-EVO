package com.greenbuddy.acevosetupengineer.model;

public final class VerifiedExact {
    public final ExactCandidate candidate;
    public final byte[] bytes;
    public final String sha256;
    public final String decodedVehicleSignature;
    public final int liveRound;
    public final boolean fromCache;

    public VerifiedExact(
            ExactCandidate candidate,
            byte[] bytes,
            String sha256,
            String decodedVehicleSignature,
            int liveRound,
            boolean fromCache) {
        this.candidate = candidate;
        this.bytes = bytes;
        this.sha256 = sha256;
        this.decodedVehicleSignature = decodedVehicleSignature;
        this.liveRound = liveRound;
        this.fromCache = fromCache;
    }
}
