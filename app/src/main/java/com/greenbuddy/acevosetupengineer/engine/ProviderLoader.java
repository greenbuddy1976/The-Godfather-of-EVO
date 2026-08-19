package com.greenbuddy.acevosetupengineer.engine;

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
}
