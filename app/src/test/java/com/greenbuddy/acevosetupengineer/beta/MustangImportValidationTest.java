package com.greenbuddy.acevosetupengineer.beta;

import org.junit.Test;
import static org.junit.Assert.*;

public final class MustangImportValidationTest {
    private final MustangSetupInspector inspector = new MustangSetupInspector();

    @Test public void acceptsCompleteMustangWireStructure() {
        MustangImportInspection result = inspector.inspect(MustangTestFixture.valid());
        assertTrue(result.getMessage(), result.isValid());
        assertEquals(MustangField.values().length, result.getValues().size());
    }

    @Test public void rejectsDifferentPresetIdentity() {
        assertFalse(inspector.inspect(MustangTestFixture.withoutMustangId()).isValid());
    }

    @Test public void rejectsTruncatedProtobuf() {
        byte[] valid = MustangTestFixture.valid();
        byte[] truncated = java.util.Arrays.copyOf(valid, valid.length - 1);
        assertFalse(inspector.inspect(truncated).isValid());
    }
}
