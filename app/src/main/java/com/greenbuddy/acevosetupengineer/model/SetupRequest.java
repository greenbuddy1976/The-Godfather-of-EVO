package com.greenbuddy.acevosetupengineer.model;

import java.util.Objects;

public final class SetupRequest {
    private final CarIdentity car;
    private final TrackLayout layout;
    private final SetupStyle style;
    private final FineTuningProblem fineTuningProblem;
    private final FineTuningStrength fineTuningStrength;
    private final String gameVersion;

    public SetupRequest(CarIdentity car, TrackLayout layout, SetupStyle style,
                        FineTuningProblem problem, FineTuningStrength strength,
                        String gameVersion) {
        this.car = Objects.requireNonNull(car, "car");
        this.layout = Objects.requireNonNull(layout, "layout");
        this.style = Objects.requireNonNull(style, "style");
        this.fineTuningProblem = Objects.requireNonNull(problem, "problem");
        this.fineTuningStrength = Objects.requireNonNull(strength, "strength");
        this.gameVersion = Objects.requireNonNull(gameVersion, "gameVersion");
        if (!gameVersion.equals(car.getGameVersion()) || !gameVersion.equals(layout.getGameVersion())) {
            throw new IllegalArgumentException("Versions must match exactly");
        }
    }

    public CarIdentity getCar() { return car; }
    public TrackLayout getLayout() { return layout; }
    public SetupStyle getStyle() { return style; }
    public FineTuningProblem getFineTuningProblem() { return fineTuningProblem; }
    public FineTuningStrength getFineTuningStrength() { return fineTuningStrength; }
    public String getGameVersion() { return gameVersion; }

    public String exactKey() {
        return car.getId() + "|" + layout.getId() + "|" + gameVersion;
    }

    public String requestKey() {
        return exactKey() + "|" + style.name() + "|" + fineTuningProblem.name()
                + "|" + fineTuningStrength.getLevel();
    }
}
