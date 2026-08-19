package com.greenbuddy.acevosetupengineer.model;

import java.util.Objects;

public final class GenerationOutcome {
    public enum State {
        LIVE_EXACT_FOUND,
        NO_EXACT_LIVE_HIT,
        LIVE_SOURCE_TECHNICAL_ERROR,
        ENGINEERING_MODEL_RECALCULATED,
        BLOCKED_NOT_VERIFIED
    }

    private final State state;
    private final String liveStateText;
    private final String message;
    private final GeneratedSetup setup;

    private GenerationOutcome(State state, String liveStateText, String message, GeneratedSetup setup) {
        this.state = Objects.requireNonNull(state, "state");
        this.liveStateText = Objects.requireNonNull(liveStateText, "liveStateText");
        this.message = Objects.requireNonNull(message, "message");
        this.setup = setup;
    }

    public static GenerationOutcome success(State state, String liveStateText,
                                             String message, GeneratedSetup setup) {
        return new GenerationOutcome(state, liveStateText, message,
                Objects.requireNonNull(setup, "setup"));
    }

    public static GenerationOutcome blocked(String liveStateText, String message) {
        return new GenerationOutcome(State.BLOCKED_NOT_VERIFIED, liveStateText, message, null);
    }

    public State getState() { return state; }
    public String getLiveStateText() { return liveStateText; }
    public String getMessage() { return message; }
    public GeneratedSetup getSetup() { return setup; }
    public boolean isExportable() { return setup != null && setup.isExportable(); }
}
