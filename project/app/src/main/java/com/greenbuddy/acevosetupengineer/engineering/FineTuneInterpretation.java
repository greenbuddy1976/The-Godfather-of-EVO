package com.greenbuddy.acevosetupengineer.engineering;

import java.util.Collections;
import java.util.List;

public final class FineTuneInterpretation {
    public final String originalText;
    public final List<HandlingIssue> issues;
    public final boolean understood;

    public FineTuneInterpretation(String originalText, List<HandlingIssue> issues) {
        this.originalText = originalText;
        this.issues = Collections.unmodifiableList(issues);
        this.understood = !issues.isEmpty();
    }
}
