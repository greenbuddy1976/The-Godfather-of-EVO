package com.greenbuddy.acevosetupengineer.engineering;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class EngineeringProfile {
    public final String vehicleId;
    public final String gameVersion;
    public final String binarySignaturePrefix;
    public final String profileFingerprint;
    public final boolean structureCoverageVerified;
    public final Map<ParameterKey, ParameterDefinition> parameters;

    public EngineeringProfile(
            String vehicleId,
            String gameVersion,
            String binarySignaturePrefix,
            String profileFingerprint,
            boolean structureCoverageVerified,
            Map<ParameterKey, ParameterDefinition> parameters) {
        this.vehicleId = vehicleId;
        this.gameVersion = gameVersion;
        this.binarySignaturePrefix = binarySignaturePrefix;
        this.profileFingerprint = profileFingerprint;
        this.structureCoverageVerified = structureCoverageVerified;
        EnumMap<ParameterKey, ParameterDefinition> copy = new EnumMap<>(ParameterKey.class);
        copy.putAll(parameters);
        this.parameters = Collections.unmodifiableMap(copy);
    }

    public ParameterDefinition requireVerified(ParameterKey key) {
        ParameterDefinition definition = parameters.get(key);
        if (definition == null || !definition.verified) {
            throw new SetupValidationException("Kein verifizierter Fahrzeugparameter: " + key.displayName);
        }
        return definition;
    }

    public void requireUsableFor(String requestedVehicleId, String requestedVersion) {
        requireFineTuneUsableFor(requestedVehicleId, requestedVersion);
        if (!structureCoverageVerified) {
            throw new SetupValidationException("Binäre Parameterabdeckung ist nicht vollständig verifiziert");
        }
        if (parameters.isEmpty() || parameters.values().stream().anyMatch(value -> !value.selfCalcReady())) {
            throw new SetupValidationException(
                    "SELF CALC benötigt verifizierte Anker, Reaktionsmodelle und Schreibwege für alle Profilparameter");
        }
    }

    /** Range-derived SELF CALC does not claim a hidden baseline or imported anchor. */
    public void requireRangeModelUsableFor(String requestedVehicleId, String requestedVersion) {
        requireFineTuneUsableFor(requestedVehicleId, requestedVersion);
        if (parameters.isEmpty()) {
            throw new SetupValidationException("Fahrzeugprofil enthält keine verifizierten Wertebereiche");
        }
        for (ParameterDefinition definition : parameters.values()) {
            if (!definition.verified || !definition.binaryWriteVerified) {
                throw new SetupValidationException("SELF CALC Schreibweg nicht verifiziert: "
                        + definition.key.displayName);
            }
        }
    }

    public void requireFineTuneUsableFor(String requestedVehicleId, String requestedVersion) {
        if (!vehicleId.equals(requestedVehicleId)) {
            throw new SetupValidationException("Engineering-Profil gehört zu einem anderen Fahrzeug");
        }
        if (!gameVersion.equals(requestedVersion)) {
            throw new SetupValidationException("Engineering-Profil gehört zu Spielversion " + gameVersion);
        }
        if (profileFingerprint == null || profileFingerprint.trim().isEmpty()) {
            throw new SetupValidationException("Engineering-Profil besitzt keinen verifizierten Fingerprint");
        }
        for (ParameterDefinition definition : parameters.values()) {
            if (!definition.verified) {
                throw new SetupValidationException("Nicht verifizierter Parameter im Fahrzeugprofil: "
                        + definition.key.displayName);
            }
        }
    }
}
