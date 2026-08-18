package com.greenbuddy.acevosetupengineer.binary;

public final class CarSetupInspection {
    public final boolean structurallyValid;
    public final String vehicleSignature;
    public final int topLevelFieldCount;
    public final int floatCount;
    public final String failureReason;

    public CarSetupInspection(
            boolean structurallyValid,
            String vehicleSignature,
            int topLevelFieldCount,
            int floatCount,
            String failureReason) {
        this.structurallyValid = structurallyValid;
        this.vehicleSignature = vehicleSignature;
        this.topLevelFieldCount = topLevelFieldCount;
        this.floatCount = floatCount;
        this.failureReason = failureReason;
    }
}
