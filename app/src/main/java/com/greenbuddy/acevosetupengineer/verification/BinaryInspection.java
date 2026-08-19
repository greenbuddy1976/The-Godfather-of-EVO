package com.greenbuddy.acevosetupengineer.verification;

import com.greenbuddy.acevosetupengineer.model.SetupValue;
import com.greenbuddy.acevosetupengineer.model.VerificationReport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class BinaryInspection {
    private final List<SetupValue> decodedValues;
    private final VerificationReport verification;

    public BinaryInspection(List<SetupValue> decodedValues, VerificationReport verification) {
        List<SetupValue> sorted = new ArrayList<>(Objects.requireNonNull(decodedValues,
                "decodedValues"));
        Collections.sort(sorted);
        this.decodedValues = Collections.unmodifiableList(sorted);
        this.verification = Objects.requireNonNull(verification, "verification");
    }

    public List<SetupValue> getDecodedValues() { return decodedValues; }
    public VerificationReport getVerification() { return verification; }

    public boolean verifies(byte[] binary) {
        return !decodedValues.isEmpty() && verification.isFullyVerified()
                && BinaryDigest.sha256(binary).equals(verification.getSha256());
    }
}
