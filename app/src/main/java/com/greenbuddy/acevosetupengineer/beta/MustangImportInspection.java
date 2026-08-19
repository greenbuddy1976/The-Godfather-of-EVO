package com.greenbuddy.acevosetupengineer.beta;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class MustangImportInspection {
    private final boolean valid;
    private final String message;
    private final Map<MustangField, Float> values;

    MustangImportInspection(boolean valid, String message, Map<MustangField, Float> values) {
        this.valid = valid;
        this.message = message;
        this.values = Collections.unmodifiableMap(new EnumMap<>(values));
    }

    public boolean isValid() { return valid; }
    public String getMessage() { return message; }
    public Map<MustangField, Float> getValues() { return values; }
}
