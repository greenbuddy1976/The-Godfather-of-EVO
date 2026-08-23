package com.greenbuddy.acevosetupengineer.engineering;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class FineTuneInterpreterTest {
    private final FineTuneInterpreter interpreter = new FineTuneInterpreter();
    private final FineTunePlanner planner = new FineTunePlanner();

    @Test
    public void rearNervousCreatesStabilisingPlanWithoutNumbers() {
        FineTuneInterpretation interpretation = interpreter.interpret(
                "Das Heck wird nervös beim Anbremsen und kommt leicht.");
        assertTrue(interpretation.issues.contains(HandlingIssue.REAR_NERVOUS));
        assertTrue(interpretation.issues.contains(HandlingIssue.BRAKING_INSTABILITY));

        FineTunePlan plan = planner.plan(interpretation, FineTunePlan.Origin.EXACT_DERIVATIVE);
        assertEquals(FineTunePlan.Origin.EXACT_DERIVATIVE, plan.origin);
        assertDelta(plan, ParameterKey.TOE_REAR, true);
        assertDelta(plan, ParameterKey.REAR_WING, true);
        assertDelta(plan, ParameterKey.ANTI_ROLL_BAR_REAR, false);
    }

    @Test
    public void rearSluggishCreatesRotationPlan() {
        FineTunePlan plan = planner.plan(interpreter.interpret("Das Heck ist zu träge."),
                FineTunePlan.Origin.ENGINEERING_MODEL);
        assertDelta(plan, ParameterKey.ANTI_ROLL_BAR_REAR, true);
        assertDelta(plan, ParameterKey.TOE_REAR, false);
        assertDelta(plan, ParameterKey.REAR_WING, false);
    }

    @Test
    public void vagueTextIsNotInvented() {
        FineTuneInterpretation interpretation = interpreter.interpret("Mach das Auto besser.");
        assertTrue(interpretation.issues.isEmpty());
    }

    @Test
    public void directRearWingRequestNeedsDirection() {
        FineTuneInterpretation vague = interpreter.interpret("Heckflügel einstellen");
        assertTrue(vague.issues.isEmpty());

        FineTunePlan more = planner.plan(interpreter.interpret("Bitte mehr Heckflügel."),
                FineTunePlan.Origin.EXACT_DERIVATIVE);
        assertDelta(more, ParameterKey.REAR_WING, true);
    }

    @Test
    public void slowSteeringMovesFrontToeTowardToeOut() {
        FineTunePlan plan = planner.plan(interpreter.interpret("Die Lenkung ist zu langsam."),
                FineTunePlan.Origin.ENGINEERING_MODEL);
        assertDelta(plan, ParameterKey.TOE_FRONT, false);
    }

    private static void assertDelta(FineTunePlan plan, ParameterKey key, boolean positive) {
        ParameterAdjustment found = plan.adjustments.stream()
                .filter(value -> value.parameter == key)
                .findFirst()
                .orElseThrow();
        assertTrue(positive ? found.normalizedDelta > 0 : found.normalizedDelta < 0);
    }
}
