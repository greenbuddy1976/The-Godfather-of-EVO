package com.greenbuddy.acevosetupengineer.engine;

import com.greenbuddy.acevosetupengineer.verification.VerifiedBinaryInspector;

public final class ProviderLoader {
    private ProviderLoader() { }

    public static VerifiedWriterProvider load(String className) {
        if (className == null || className.trim().isEmpty()) return null;
        try {
            Object instance = Class.forName(className).getDeclaredConstructor().newInstance();
            if (!(instance instanceof VerifiedWriterProvider)) return null;
            return (VerifiedWriterProvider) instance;
        } catch (ReflectiveOperationException | LinkageError error) {
            return null;
        }
    }

    public static VerifiedBinaryInspector loadInspector(String className) {
        if (className == null || className.trim().isEmpty()) return null;
        try {
            Object instance = Class.forName(className).getDeclaredConstructor().newInstance();
            if (!(instance instanceof VerifiedBinaryInspector)) return null;
            return (VerifiedBinaryInspector) instance;
        } catch (ReflectiveOperationException | LinkageError error) {
            return null;
        }
    }
}
