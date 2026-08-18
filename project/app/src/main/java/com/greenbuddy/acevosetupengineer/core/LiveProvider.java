package com.greenbuddy.acevosetupengineer.core;

import com.greenbuddy.acevosetupengineer.model.ExactCandidate;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import java.io.IOException;
import java.util.List;

public interface LiveProvider {
    String name();
    List<ExactCandidate> searchExact(SetupRequest request) throws IOException;
    byte[] download(ExactCandidate candidate) throws IOException;
}
