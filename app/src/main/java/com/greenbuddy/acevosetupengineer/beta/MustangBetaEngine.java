package com.greenbuddy.acevosetupengineer.beta;

import com.greenbuddy.acevosetupengineer.model.FineTuningProblem;
import com.greenbuddy.acevosetupengineer.model.GeneratedSetup;
import com.greenbuddy.acevosetupengineer.model.GenerationOutcome;
import com.greenbuddy.acevosetupengineer.model.ParameterChange;
import com.greenbuddy.acevosetupengineer.model.ResultLabel;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.model.SetupStyle;
import com.greenbuddy.acevosetupengineer.model.SetupValue;
import com.greenbuddy.acevosetupengineer.model.VerificationReport;
import com.greenbuddy.acevosetupengineer.verification.BinaryDigest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Same-car byte-patch BETA. It never claims game-load acceptance. */
public final class MustangBetaEngine {
    public static final String WARNING =
            "BETA – SAME-CAR BASIS / SPIELAKZEPTANZ NOCH NICHT BESTÄTIGT";
    private static final Map<MustangField, MustangRange> RANGES = MustangRange.all();
    private final MustangSetupInspector inspector = new MustangSetupInspector();
    private final MustangBytePatcher patcher = new MustangBytePatcher();

    public GenerationOutcome generate(byte[] layoutSpecificBase, SetupRequest request) {
        requireMustangRequest(request);
        TrackTrait trait = MustangTrackTraits.require(request.getLayout().getId());
        MustangImportInspection baseInspection = inspector.inspect(layoutSpecificBase);
        if (!baseInspection.isValid()) {
            return GenerationOutcome.blocked("BETA-BASIS ABGELEHNT",
                    "NICHT SICHER – " + baseInspection.getMessage());
        }

        EnumMap<MustangField, Float> base = new EnumMap<>(baseInspection.getValues());
        EnumMap<MustangField, Float> target = new EnumMap<>(base);
        applyStyle(target, request.getStyle());
        applyTrack(target, trait);
        applyFineTuning(target, request.getFineTuningProblem(),
                request.getFineTuningStrength().getLevel());
        if (request.getStyle() == SetupStyle.FAST_ATTACK) set(target, MustangField.TC, 1);

        EnumMap<MustangField, Float> changed = changedValues(base, target);
        if (changed.isEmpty()) {
            return GenerationOutcome.blocked("BETA-BASIS UNVERÄNDERT",
                    "NICHT SICHER – Alle konservativen Änderungen lagen bereits an einer Grenze.");
        }
        byte[] output;
        try {
            output = patcher.patch(layoutSpecificBase, changed);
        } catch (RuntimeException error) {
            return GenerationOutcome.blocked("BETA-PATCH FEHLGESCHLAGEN",
                    "NICHT SICHER – Es wurde keine Datei ausgegeben.");
        }

        MustangImportInspection decoded = inspector.inspect(output);
        boolean roundTrip = decoded.isValid() && sameTarget(target, decoded.getValues());
        boolean tcRule = request.getStyle() != SetupStyle.FAST_ATTACK
                || close(decoded.getValues().get(MustangField.TC), 1f);
        String sha = BinaryDigest.sha256(output);
        VerificationReport report = new VerificationReport(
                true,
                true,
                false,
                decoded.isValid(),
                decoded.isValid(),
                roundTrip,
                decoded.isValid(),
                decoded.isValid(),
                tcRule,
                true,
                false,
                sha,
                WARNING + "\nTrack-Trait: " + trait
                        + "\nExaktes Layout ist vom Benutzer zugeordnet; die Binärdatei enthält keinen geprüften Track-Marker."
                        + "\nKlick-Deltas beziehen sich auf die importierte, vom Benutzer als spielgültig erklärte Basis; globale Schrittanker bleiben im BETA unbestätigt."
        );
        GeneratedSetup setup = new GeneratedSetup(request, output, setupValues(decoded.getValues()),
                changes(base, decoded.getValues()), report, ResultLabel.BETA_SAME_CAR);
        if (!setup.isBetaExportable()) {
            return GenerationOutcome.blocked("BETA-ROUNDTRIP FEHLGESCHLAGEN",
                    "NICHT SICHER – Die unabhängige Strukturprüfung war nicht vollständig.");
        }
        return GenerationOutcome.success(GenerationOutcome.State.BETA_SAME_CAR_CREATED,
                "BETA SAME-CAR BASIS", WARNING, setup);
    }

    private static void requireMustangRequest(SetupRequest request) {
        if (!"ford_mustang_gt3".equals(request.getCar().getId())) {
            throw new IllegalArgumentException("Mustang-only BETA");
        }
        if (!"0.8.1".equals(request.getGameVersion())) {
            throw new IllegalArgumentException("Unsupported BETA version");
        }
    }

    private static void applyStyle(EnumMap<MustangField, Float> values, SetupStyle style) {
        switch (style) {
            case FAST_CONTROL:
                delta(values, MustangField.REAR_ARB, -1000);
                delta(values, MustangField.BRAKE_BIAS, .4f);
                delta(values, MustangField.DIFF_COAST, .05f);
                rearToe(values, .01f);
                delta(values, MustangField.REAR_WING, 1);
                break;
            case FAST_ATTACK:
                delta(values, MustangField.FRONT_ARB, 2000);
                delta(values, MustangField.REAR_ARB, 1000);
                delta(values, MustangField.BRAKE_BIAS, -.4f);
                delta(values, MustangField.DIFF_COAST, -.05f);
                delta(values, MustangField.REAR_WING, -1);
                set(values, MustangField.TC, 1);
                break;
            case FAST_STABLE:
                delta(values, MustangField.REAR_ARB, -2000);
                delta(values, MustangField.BRAKE_BIAS, .8f);
                delta(values, MustangField.DIFF_COAST, .1f);
                rearToe(values, .02f);
                delta(values, MustangField.REAR_WING, 1);
                break;
            case FAST_SAFE:
                delta(values, MustangField.FRONT_ARB, -1000);
                delta(values, MustangField.REAR_ARB, -3000);
                delta(values, MustangField.BRAKE_BIAS, 1.2f);
                delta(values, MustangField.DIFF_COAST, .15f);
                rearToe(values, .03f);
                delta(values, MustangField.REAR_WING, 2);
                delta(values, MustangField.TC, 1);
                break;
            case FAST_LONG_RUN:
                delta(values, MustangField.FRONT_ARB, -1000);
                delta(values, MustangField.REAR_ARB, -1000);
                delta(values, MustangField.DIFF_POWER, -.05f);
                delta(values, MustangField.DIFF_COAST, .05f);
                rearToe(values, .01f);
                break;
            default:
                throw new IllegalArgumentException("Unknown style");
        }
    }

    private static void applyTrack(EnumMap<MustangField, Float> values, TrackTrait trait) {
        switch (trait) {
            case LOW_DOWNFORCE:
                delta(values, MustangField.REAR_WING, -1);
                break;
            case HIGH_SPEED:
                delta(values, MustangField.REAR_WING, 1);
                rearToe(values, .01f);
                break;
            case BUMPY_KERB:
                delta(values, MustangField.FRONT_ARB, -1000);
                delta(values, MustangField.REAR_ARB, -1000);
                delta(values, MustangField.FRONT_RIDE_HEIGHT, 1);
                delta(values, MustangField.REAR_RIDE_HEIGHT, 1);
                break;
            case TECHNICAL:
                delta(values, MustangField.REAR_ARB, 1000);
                delta(values, MustangField.TOE_FL, -.01f);
                delta(values, MustangField.TOE_FR, -.01f);
                delta(values, MustangField.REAR_WING, 1);
                break;
            case BALANCED:
                delta(values, MustangField.BRAKE_BIAS, .2f);
                break;
            default:
                throw new IllegalArgumentException("Unknown trait");
        }
    }

    private static void applyFineTuning(EnumMap<MustangField, Float> values,
            FineTuningProblem problem, int strength) {
        switch (problem) {
            case NONE:
                return;
            case BRAKING_REAR_INSTABILITY:
                delta(values, MustangField.BRAKE_BIAS, .4f * strength);
                delta(values, MustangField.DIFF_COAST, .05f * strength);
                rearToe(values, .01f * strength);
                break;
            case TURN_IN_UNDERSTEER:
                delta(values, MustangField.FRONT_ARB, -1000f * strength);
                delta(values, MustangField.REAR_ARB, 1000f * strength);
                delta(values, MustangField.BRAKE_BIAS, -.2f * strength);
                break;
            case EXIT_REAR_NERVOUS:
                delta(values, MustangField.DIFF_POWER, .05f * strength);
                rearToe(values, .01f * strength);
                delta(values, MustangField.TC, strength);
                break;
            case KERBS_OR_CRESTS_UNSETTLED:
                delta(values, MustangField.FRONT_ARB, -1000f * strength);
                delta(values, MustangField.REAR_ARB, -1000f * strength);
                delta(values, MustangField.FRONT_RIDE_HEIGHT, strength);
                delta(values, MustangField.REAR_RIDE_HEIGHT, strength);
                break;
            case SLOW_TURN_IN:
                delta(values, MustangField.REAR_ARB, 1000f * strength);
                delta(values, MustangField.TOE_FL, -.01f * strength);
                delta(values, MustangField.TOE_FR, -.01f * strength);
                break;
            case HIGH_TYRE_WEAR:
                towardZero(values, MustangField.CAMBER_FL, .1f * strength);
                towardZero(values, MustangField.CAMBER_FR, .1f * strength);
                towardZero(values, MustangField.CAMBER_RL, .1f * strength);
                towardZero(values, MustangField.CAMBER_RR, .1f * strength);
                towardZero(values, MustangField.TOE_FL, .01f * strength);
                towardZero(values, MustangField.TOE_FR, .01f * strength);
                towardZero(values, MustangField.TOE_RL, .01f * strength);
                towardZero(values, MustangField.TOE_RR, .01f * strength);
                break;
            case HIGH_SPEED_REAR_NERVOUS:
                delta(values, MustangField.REAR_WING, strength);
                rearToe(values, .01f * strength);
                break;
            case MORE_TOP_SPEED:
                delta(values, MustangField.REAR_WING, -strength);
                break;
            default:
                throw new IllegalArgumentException("Unknown fine tuning");
        }
    }

    private static void delta(EnumMap<MustangField, Float> values,
                              MustangField field, float amount) {
        MustangRange range = RANGES.get(field);
        if (range == null) throw new IllegalArgumentException("No verified range: " + field);
        values.put(field, range.clamp(values.get(field) + amount));
    }

    private static void set(EnumMap<MustangField, Float> values,
                            MustangField field, float value) {
        MustangRange range = RANGES.get(field);
        if (range == null) throw new IllegalArgumentException("No verified range: " + field);
        values.put(field, range.snapClamp(value));
    }

    private static void towardZero(EnumMap<MustangField, Float> values,
                                   MustangField field, float amount) {
        float current = values.get(field);
        float adjusted = current > 0 ? Math.max(0, current - amount) : Math.min(0, current + amount);
        set(values, field, adjusted);
    }

    private static void rearToe(EnumMap<MustangField, Float> values, float amount) {
        delta(values, MustangField.TOE_RL, amount);
        delta(values, MustangField.TOE_RR, amount);
    }

    private static EnumMap<MustangField, Float> changedValues(
            Map<MustangField, Float> base, Map<MustangField, Float> target) {
        EnumMap<MustangField, Float> changed = new EnumMap<>(MustangField.class);
        for (MustangField field : MustangField.values()) {
            if (!close(base.get(field), target.get(field))) changed.put(field, target.get(field));
        }
        return changed;
    }

    private static boolean sameTarget(Map<MustangField, Float> expected,
                                      Map<MustangField, Float> decoded) {
        if (decoded.size() != MustangField.values().length) return false;
        for (MustangField field : MustangField.values()) {
            if (!close(expected.get(field), decoded.get(field))) return false;
        }
        return true;
    }

    private static boolean close(Float left, Float right) {
        return left != null && right != null && Math.abs(left - right) <= .002f;
    }

    private static List<SetupValue> setupValues(Map<MustangField, Float> values) {
        List<SetupValue> result = new ArrayList<>();
        for (MustangField field : MustangField.values()) {
            result.add(new SetupValue(field.section(), field.position(), field.key(),
                    field.displayName(), format(values.get(field)),
                    MustangBytePatcher.isWritable(field)));
        }
        return result;
    }

    private static List<ParameterChange> changes(Map<MustangField, Float> base,
                                                  Map<MustangField, Float> output) {
        List<ParameterChange> result = new ArrayList<>();
        for (MustangField field : MustangField.values()) {
            if (!close(base.get(field), output.get(field))) {
                result.add(new ParameterChange(field.displayName(), format(base.get(field)),
                        format(output.get(field))));
            }
        }
        return result;
    }

    private static String format(float value) {
        return new BigDecimal(Float.toString(value)).stripTrailingZeros().toPlainString();
    }
}
