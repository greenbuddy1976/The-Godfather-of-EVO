package com.greenbuddy.acevosetupengineer.core;

import android.content.Context;

import com.greenbuddy.acevosetupengineer.binary.CarSetupInspection;
import com.greenbuddy.acevosetupengineer.binary.CarSetupInspector;
import com.greenbuddy.acevosetupengineer.model.CatalogItem;
import com.greenbuddy.acevosetupengineer.util.Hashing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class PreloadedSetupRepository {
    public static final String GOLDEN = "Mustang_GT3_Nurburgring_Touristenfahrten_FAST_CONTROL.carsetup";

    private final Context context;
    private final List<String> names = new ArrayList<>();

    public PreloadedSetupRepository(Context context) throws IOException {
        this.context = context.getApplicationContext();
        String[] files = this.context.getAssets().list("preloaded-setups");
        if (files != null) names.addAll(Arrays.asList(files));
        names.sort(Comparator.comparing((String n) -> !GOLDEN.equals(n))
                .thenComparing(String::compareToIgnoreCase));
    }

    public int count() {
        return names.size();
    }

    public VerifiedStructureCarrier findCarrier(CatalogItem vehicle) {
        if (vehicle == null || !vehicle.hasVerifiedBinaryIdentity()) return null;
        for (String name : names) {
            try {
                byte[] bytes = read(name);
                CarSetupInspection inspection = CarSetupInspector.inspect(bytes);
                if (!inspection.structurallyValid) continue;
                if (!inspection.vehicleSignature.startsWith(vehicle.expectedSignaturePrefix)) continue;
                String source = GOLDEN.equals(name)
                        ? "GOLDEN SETUP / " + name
                        : "Vorgeladener Bestand / " + name;
                return new VerifiedStructureCarrier(bytes, Hashing.sha256(bytes),
                        inspection.vehicleSignature, source, false);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private byte[] read(String name) throws IOException {
        try (InputStream input = context.getAssets().open("preloaded-setups/" + name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }
}
