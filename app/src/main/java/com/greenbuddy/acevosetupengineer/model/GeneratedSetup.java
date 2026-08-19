package com.greenbuddy.acevosetupengineer.model;

import com.greenbuddy.acevosetupengineer.verification.BinaryDigest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class GeneratedSetup {
    private final SetupRequest request;
    private final byte[] binary;
    private final List<SetupValue> values;
    private final List<ParameterChange> changes;
    private final VerificationReport verification;
    private final ResultLabel origin;

    public GeneratedSetup(SetupRequest request, byte[] binary, List<SetupValue> values,
                          List<ParameterChange> changes, VerificationReport verification,
                          ResultLabel origin) {
        this.request = Objects.requireNonNull(request, "request");
        this.binary = Objects.requireNonNull(binary, "binary").clone();
        if (this.binary.length == 0) throw new IllegalArgumentException("binary is empty");
        List<SetupValue> sorted = new ArrayList<>(Objects.requireNonNull(values, "values"));
        Collections.sort(sorted);
        this.values = Collections.unmodifiableList(sorted);
        this.changes = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(changes, "changes")));
        this.verification = Objects.requireNonNull(verification, "verification");
        if (!BinaryDigest.sha256(this.binary).equals(verification.getSha256())) {
            throw new IllegalArgumentException("verification SHA-256 does not match binary");
        }
        this.origin = Objects.requireNonNull(origin, "origin");
    }

    public SetupRequest getRequest() { return request; }
    public byte[] getBinary() { return binary.clone(); }
    public List<SetupValue> getValues() { return values; }
    public List<ParameterChange> getChanges() { return changes; }
    public VerificationReport getVerification() { return verification; }
    public ResultLabel getOrigin() { return origin; }
    public boolean isExportable() { return verification.isFullyVerified(); }
}
