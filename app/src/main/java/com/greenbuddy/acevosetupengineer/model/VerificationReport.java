package com.greenbuddy.acevosetupengineer.model;

import java.util.Objects;

public final class VerificationReport {
    private final boolean vehicleIdentity;
    private final boolean layoutIdentity;
    private final boolean gameVersion;
    private final boolean binaryStructure;
    private final boolean vehicleSignature;
    private final boolean fieldsRoundTrip;
    private final boolean ranges;
    private final boolean steps;
    private final boolean tractionControlRule;
    private final boolean unknownFieldsPreserved;
    private final boolean gameLoadAcceptanceEvidence;
    private final String sha256;
    private final String details;

    public VerificationReport(boolean vehicleIdentity, boolean layoutIdentity,
            boolean gameVersion, boolean binaryStructure, boolean vehicleSignature,
            boolean fieldsRoundTrip, boolean ranges, boolean steps,
            boolean tractionControlRule, boolean unknownFieldsPreserved,
            boolean gameLoadAcceptanceEvidence,
            String sha256, String details) {
        this.vehicleIdentity = vehicleIdentity;
        this.layoutIdentity = layoutIdentity;
        this.gameVersion = gameVersion;
        this.binaryStructure = binaryStructure;
        this.vehicleSignature = vehicleSignature;
        this.fieldsRoundTrip = fieldsRoundTrip;
        this.ranges = ranges;
        this.steps = steps;
        this.tractionControlRule = tractionControlRule;
        this.unknownFieldsPreserved = unknownFieldsPreserved;
        this.gameLoadAcceptanceEvidence = gameLoadAcceptanceEvidence;
        this.sha256 = Objects.requireNonNull(sha256, "sha256");
        this.details = Objects.requireNonNull(details, "details");
    }

    public boolean isFullyVerified() {
        return vehicleIdentity && layoutIdentity && gameVersion && binaryStructure
                && vehicleSignature && fieldsRoundTrip && ranges && steps
                && tractionControlRule && unknownFieldsPreserved
                && gameLoadAcceptanceEvidence
                && sha256.matches("[0-9a-f]{64}");
    }

    public boolean isBetaStructurallyChecked() {
        return vehicleIdentity && binaryStructure && vehicleSignature && fieldsRoundTrip
                && ranges && steps && tractionControlRule && unknownFieldsPreserved
                && !gameLoadAcceptanceEvidence && sha256.matches("[0-9a-f]{64}");
    }

    public boolean isVehicleIdentityValid() { return vehicleIdentity; }
    public boolean isLayoutIdentityValid() { return layoutIdentity; }
    public boolean isGameVersionValid() { return gameVersion; }
    public boolean isBinaryStructureValid() { return binaryStructure; }
    public boolean isVehicleSignatureValid() { return vehicleSignature; }
    public boolean areFieldsRoundTripValid() { return fieldsRoundTrip; }
    public boolean areRangesValid() { return ranges; }
    public boolean areStepsValid() { return steps; }
    public boolean isTractionControlRuleValid() { return tractionControlRule; }
    public boolean areUnknownFieldsPreserved() { return unknownFieldsPreserved; }
    public boolean hasGameLoadAcceptanceEvidence() { return gameLoadAcceptanceEvidence; }
    public String getSha256() { return sha256; }
    public String getDetails() { return details; }
}
