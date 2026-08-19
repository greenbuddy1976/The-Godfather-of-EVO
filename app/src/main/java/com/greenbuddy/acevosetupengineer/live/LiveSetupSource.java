package com.greenbuddy.acevosetupengineer.live;

import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import java.util.List;

/** Implementations must return only source results, never similar-car/layout fallbacks. */
public interface LiveSetupSource {
    String name();
    List<LiveCandidate> exactLookup(SetupRequest request) throws Exception;
    List<LiveCandidate> fetchConfirmedIndex() throws Exception;
}
