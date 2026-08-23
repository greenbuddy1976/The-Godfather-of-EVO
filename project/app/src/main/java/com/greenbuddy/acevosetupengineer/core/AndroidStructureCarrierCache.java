package com.greenbuddy.acevosetupengineer.core;

import android.content.Context;

import com.greenbuddy.acevosetupengineer.binary.CarSetupInspection;
import com.greenbuddy.acevosetupengineer.binary.CarSetupInspector;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.util.Hashing;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Integrity-checked, per-car structure cache; setup numbers are never model inputs. */
public final class AndroidStructureCarrierCache implements StructureCarrierCache {
    private final Path directory;

    public AndroidStructureCarrierCache(Context context) {
        directory = context.getFilesDir().toPath().resolve("verified-structure-cache");
    }

    @Override
    public synchronized VerifiedStructureCarrier get(SetupRequest request) {
        try {
            String key = fileKey(request);
            Path bytesPath = directory.resolve(key + ".carsetup");
            Path metadataPath = directory.resolve(key + ".json");
            if (!Files.isRegularFile(bytesPath) || !Files.isRegularFile(metadataPath)) return null;
            byte[] bytes = Files.readAllBytes(bytesPath);
            JSONObject metadata = new JSONObject(new String(
                    Files.readAllBytes(metadataPath), StandardCharsets.UTF_8));
            if (!request.vehicle.id.equals(metadata.getString("vehicleId"))
                    || !request.gameVersion.equals(metadata.getString("gameVersion"))) return null;
            String sha = Hashing.sha256(bytes);
            if (!sha.equals(metadata.getString("sha256"))) return null;
            CarSetupInspection inspection = CarSetupInspector.inspect(bytes);
            if (!inspection.structurallyValid
                    || !inspection.vehicleSignature.startsWith(request.vehicle.expectedSignaturePrefix)
                    || !inspection.vehicleSignature.equals(metadata.getString("vehicleSignature"))) return null;
            return new VerifiedStructureCarrier(bytes, sha, inspection.vehicleSignature,
                    metadata.optString("source", "AUTO-CACHE"), true);
        } catch (Exception invalidOrUnavailable) {
            return null;
        }
    }

    @Override
    public synchronized void put(SetupRequest request, VerifiedStructureCarrier carrier) {
        try {
            CarSetupInspection inspection = CarSetupInspector.inspect(carrier.bytes);
            String sha = Hashing.sha256(carrier.bytes);
            if (!inspection.structurallyValid
                    || !inspection.vehicleSignature.startsWith(request.vehicle.expectedSignaturePrefix)
                    || !sha.equals(carrier.sha256)) return;
            Files.createDirectories(directory);
            String key = fileKey(request);
            String nonce = UUID.randomUUID().toString();
            Path temporaryBytes = directory.resolve(key + "." + nonce + ".tmp");
            Path temporaryMetadata = directory.resolve(key + "." + nonce + ".json.tmp");
            Path bytesPath = directory.resolve(key + ".carsetup");
            Path metadataPath = directory.resolve(key + ".json");
            JSONObject metadata = new JSONObject()
                    .put("vehicleId", request.vehicle.id)
                    .put("gameVersion", request.gameVersion)
                    .put("sha256", sha)
                    .put("vehicleSignature", inspection.vehicleSignature)
                    .put("source", carrier.source);
            Files.write(temporaryBytes, carrier.bytes);
            Files.write(temporaryMetadata, metadata.toString().getBytes(StandardCharsets.UTF_8));
            replace(temporaryBytes, bytesPath);
            replace(temporaryMetadata, metadataPath);
        } catch (Exception ignored) {
            // Cache failure never changes LIVE or SELF CALC correctness.
        }
    }

    private static String fileKey(SetupRequest request) {
        return Hashing.sha256((request.gameVersion + "|" + request.vehicle.id)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static void replace(Path source, Path target) throws Exception {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
