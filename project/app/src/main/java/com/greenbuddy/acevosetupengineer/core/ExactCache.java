package com.greenbuddy.acevosetupengineer.core;

import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.model.VerifiedExact;

public interface ExactCache {
    VerifiedExact get(SetupRequest request);
    void put(SetupRequest request, VerifiedExact exact);
}
