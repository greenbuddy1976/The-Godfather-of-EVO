package com.greenbuddy.acevosetupengineer.engineering;

import com.greenbuddy.acevosetupengineer.model.SetupMode;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public final class ModeFineTunePlannerTest {
    private final ModeFineTunePlanner planner = new ModeFineTunePlanner();

    @Test
    public void attackMovesVerifiedStepsTowardRotation() {
        FineTunePlan plan = planner.plan(SetupMode.FAST_ATTACK,
                FineTunePlan.Origin.EXACT_DERIVATIVE);
        assertDelta(plan, ParameterKey.BRAKE_BIAS, false);
        assertDelta(plan, ParameterKey.REAR_WING, false);
        assertDelta(plan, ParameterKey.TRACTION_CONTROL, false);
    }

    @Test
    public void safeModeMovesVerifiedStepsTowardStability() {
        FineTunePlan plan = planner.plan(SetupMode.FAST_SAFE,
                FineTunePlan.Origin.EXACT_DERIVATIVE);
        assertDelta(plan, ParameterKey.BRAKE_BIAS, true);
        assertDelta(plan, ParameterKey.REAR_WING, true);
        assertDelta(plan, ParameterKey.TRACTION_CONTROL, true);
        assertDelta(plan, ParameterKey.ABS, true);
    }

    @Test
    public void allFiveModesHaveAUsableExactAdjustmentPlan() {
        for (SetupMode mode : SetupMode.values()) {
            FineTunePlan plan = planner.plan(mode, FineTunePlan.Origin.EXACT_DERIVATIVE);
            assertTrue(plan.adjustments.size() >= 6);
        }
    }

    private static void assertDelta(FineTunePlan plan, ParameterKey key, boolean positive) {
        double delta = plan.adjustments.stream().filter(value -> value.parameter == key)
                .findFirst().orElseThrow().normalizedDelta;
        assertTrue(positive ? delta > 0 : delta < 0);
    }
}
