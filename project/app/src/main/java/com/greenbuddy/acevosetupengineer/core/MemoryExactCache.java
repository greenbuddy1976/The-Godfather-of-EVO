package com.greenbuddy.acevosetupengineer.core;

import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.model.VerifiedExact;

import java.util.HashMap;
import java.util.Map;

public final class MemoryExactCache implements ExactCache {
    private final Map<String, VerifiedExact> values = new HashMap<>();

    @Override
    public synchronized VerifiedExact get(SetupRequest request) {
        VerifiedExact exact = values.get(request.cacheKey());
        if (exact == null) return null;
        return new VerifiedExact(exact.candidate, exact.bytes.clone(), exact.sha256,
                exact.decodedVehicleSignature, exact.liveRound, exact.fromCache);
    }

    @Override
    public synchronized void put(SetupRequest request, VerifiedExact exact) {
        values.put(request.cacheKey(), new VerifiedExact(exact.candidate, exact.bytes.clone(), exact.sha256,
                exact.decodedVehicleSignature, exact.liveRound, exact.fromCache));
    }
}
