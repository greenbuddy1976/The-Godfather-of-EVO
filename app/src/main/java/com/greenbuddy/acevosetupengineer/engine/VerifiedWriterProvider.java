package com.greenbuddy.acevosetupengineer.engine;

import com.greenbuddy.acevosetupengineer.live.LiveCandidate;
import com.greenbuddy.acevosetupengineer.live.LiveSetupSource;
import com.greenbuddy.acevosetupengineer.model.GeneratedSetup;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import java.util.List;

/** Boundary implemented only by the separately verified, version-bound release module. */
public interface VerifiedWriterProvider {
    String providerId();
    String supportedGameVersion();
    boolean supports(SetupRequest request);
    GeneratedSetup verifyLiveExact(SetupRequest request, LiveCandidate candidate) throws Exception;
    GeneratedSetup generateEngineeringModel(SetupRequest request) throws Exception;
    List<LiveSetupSource> liveSources();
}
