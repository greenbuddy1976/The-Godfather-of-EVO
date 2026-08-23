package com.greenbuddy.acevosetupengineer.engineering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class EngineeringSetup {
    public enum Label { ENGINEERING_MODEL, EXACT_DERIVATIVE }

    public final Label label;
    public final Map<ParameterKey, Double> values;
    public final List<String> audit;

    public EngineeringSetup(Label label, Map<ParameterKey, Double> values, List<String> audit) {
        EnumMap<ParameterKey, Double> valueCopy = new EnumMap<>(ParameterKey.class);
        valueCopy.putAll(values);
        this.label = label;
        this.values = Collections.unmodifiableMap(valueCopy);
        this.audit = Collections.unmodifiableList(new ArrayList<>(audit));
    }
}
