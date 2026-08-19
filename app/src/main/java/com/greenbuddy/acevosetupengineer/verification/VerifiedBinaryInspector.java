package com.greenbuddy.acevosetupengineer.verification;

import com.greenbuddy.acevosetupengineer.model.SetupRequest;

/** Decoder/test oracle that is separate from the engineering writer path. */
public interface VerifiedBinaryInspector {
    String inspectorId();
    String supportedGameVersion();
    BinaryInspection inspect(SetupRequest request, byte[] binary) throws Exception;
}
