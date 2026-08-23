package com.greenbuddy.acevosetupengineer.core;

import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import java.util.HashMap;
import java.util.Map;

public final class MemoryStructureCarrierCache implements StructureCarrierCache {
    private final Map<String, VerifiedStructureCarrier> values = new HashMap<>();

    @Override
    public synchronized VerifiedStructureCarrier get(SetupRequest request) {
        VerifiedStructureCarrier value = values.get(key(request));
        if (value == null) return null;
        return new VerifiedStructureCarrier(value.bytes, value.sha256, value.vehicleSignature,
                value.source, true);
    }

    @Override
    public synchronized void put(SetupRequest request, VerifiedStructureCarrier carrier) {
        values.put(key(request), new VerifiedStructureCarrier(carrier.bytes, carrier.sha256,
                carrier.vehicleSignature, carrier.source, false));
    }

    private static String key(SetupRequest request) {
        return request.gameVersion + "|" + request.vehicle.id;
    }
}
