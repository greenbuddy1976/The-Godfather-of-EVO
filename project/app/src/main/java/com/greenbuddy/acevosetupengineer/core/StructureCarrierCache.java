package com.greenbuddy.acevosetupengineer.core;

import com.greenbuddy.acevosetupengineer.model.SetupRequest;

public interface StructureCarrierCache {
    VerifiedStructureCarrier get(SetupRequest request);
    void put(SetupRequest request, VerifiedStructureCarrier carrier);
}
