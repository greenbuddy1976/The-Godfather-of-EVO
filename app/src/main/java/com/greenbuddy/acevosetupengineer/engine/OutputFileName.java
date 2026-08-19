package com.greenbuddy.acevosetupengineer.engine;

import com.greenbuddy.acevosetupengineer.model.GeneratedSetup;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;

import java.text.Normalizer;
import java.util.Locale;

public final class OutputFileName {
    private OutputFileName() { }

    public static String forSetup(GeneratedSetup setup) {
        return forRequest(setup.getRequest());
    }

    public static String forRequest(SetupRequest request) {
        String raw = request.getCar().getId() + "_"
                + request.getLayout().getId() + "_"
                + request.getStyle().name().toLowerCase(Locale.ROOT) + "_"
                + request.getGameVersion();
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFKD)
                .replaceAll("[^A-Za-z0-9._-]", "_")
                .replaceAll("_+", "_");
        return normalized + ".carsetup";
    }
}
