package com.greenbuddy.acevosetupengineer.engineering;

public final class ParameterAdjustment {
    public final ParameterKey parameter;
    public final double normalizedDelta;
    public final String reason;

    public ParameterAdjustment(ParameterKey parameter, double normalizedDelta, String reason) {
        this.parameter = parameter;
        this.normalizedDelta = normalizedDelta;
        this.reason = reason;
    }
}
