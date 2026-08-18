package com.greenbuddy.acevosetupengineer.core;

/** Same-car binary structure verified independently from every numeric setup value. */
public final class VerifiedStructureCarrier {
    public final byte[] bytes;
    public final String sha256;
    public final String vehicleSignature;
    public final String source;
    public final boolean fromCache;

    public VerifiedStructureCarrier(
            byte[] bytes,
            String sha256,
            String vehicleSignature,
            String source,
            boolean fromCache) {
        this.bytes = bytes.clone();
        this.sha256 = sha256;
        this.vehicleSignature = vehicleSignature;
        this.source = source;
        this.fromCache = fromCache;
    }
}
