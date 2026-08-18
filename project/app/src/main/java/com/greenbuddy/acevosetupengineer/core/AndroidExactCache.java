package com.greenbuddy.acevosetupengineer.core;

import android.content.Context;

import com.greenbuddy.acevosetupengineer.binary.CarSetupInspection;
import com.greenbuddy.acevosetupengineer.binary.CarSetupInspector;
import com.greenbuddy.acevosetupengineer.model.ExactCandidate;
import com.greenbuddy.acevosetupengineer.model.SetupRequest;
import com.greenbuddy.acevosetupengineer.model.VerifiedExact;
import com.greenbuddy.acevosetupengineer.util.Hashing;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/** Private, integrity-checked cache used only when every LIVE provider is unavailable. */
public final class AndroidExactCache implements ExactCache {
    private final Path directory;

    public AndroidExactCache(Context context) {
        directory = context.getFilesDir().toPath().resolve("verified-exact-cache");
    }

    @Override
    public synchronized VerifiedExact get(SetupRequest request) {
        try {
            String key = fileKey(request);
            Path bytesPath = directory.resolve(key + ".carsetup");
            Path metadataPath = directory.resolve(key + ".json");
            if (!Files.isRegularFile(bytesPath) || !Files.isRegularFile(metadataPath)) return null;

            byte[] bytes = Files.readAllBytes(bytesPath);
            JSONObject metadata = new JSONObject(new String(
                    Files.readAllBytes(metadataPath), StandardCharsets.UTF_8));
            if (!request.cacheKey().equals(metadata.getString("requestKey"))) return null;
            if (!request.gameVersion.equals(metadata.getString("gameVersion"))) return null;
            String sha256 = Hashing.sha256(bytes);
            if (!sha256.equals(metadata.getString("sha256"))) return null;

            CarSetupInspection inspection = CarSetupInspector.inspect(bytes);
            if (!inspection.structurallyValid
                    || !inspection.vehicleSignature.startsWith(request.vehicle.expectedSignaturePrefix)
                    || !inspection.vehicleSignature.equals(metadata.getString("vehicleSignature"))) {
                return null;
            }

            ExactCandidate candidate = new ExactCandidate(
                    metadata.getString("provider"), metadata.getString("providerId"),
                    metadata.getString("vehicleSlug"), metadata.getString("layoutSlug"),
                    metadata.getString("gameVersion"), metadata.getString("sourceUrl"),
                    metadata.getString("fileUrl"), metadata.getString("fileName"));
            if (!request.vehicle.providerSlug.equals(candidate.vehicleSlug)
                    || !request.layout.providerSlug.equals(candidate.layoutSlug)) return null;
            return new VerifiedExact(candidate, bytes.clone(), sha256,
                    inspection.vehicleSignature, metadata.getInt("liveRound"), true);
        } catch (Exception invalidOrUnavailable) {
            return null;
        }
    }

    @Override
    public synchronized void put(SetupRequest request, VerifiedExact exact) {
        try {
            CarSetupInspection inspection = CarSetupInspector.inspect(exact.bytes);
            String actualHash = Hashing.sha256(exact.bytes);
            if (!inspection.structurallyValid
                    || !inspection.vehicleSignature.startsWith(request.vehicle.expectedSignaturePrefix)
                    || !actualHash.equals(exact.sha256)) return;

            Files.createDirectories(directory);
            String key = fileKey(request);
            String nonce = UUID.randomUUID().toString();
            Path temporaryBytes = directory.resolve(key + "." + nonce + ".tmp");
            Path temporaryMetadata = directory.resolve(key + "." + nonce + ".json.tmp");
            Path bytesPath = directory.resolve(key + ".carsetup");
            Path metadataPath = directory.resolve(key + ".json");

            JSONObject metadata = new JSONObject()
                    .put("requestKey", request.cacheKey())
                    .put("sha256", actualHash)
                    .put("vehicleSignature", inspection.vehicleSignature)
                    .put("liveRound", exact.liveRound)
                    .put("provider", exact.candidate.provider)
                    .put("providerId", exact.candidate.providerId)
                    .put("vehicleSlug", exact.candidate.vehicleSlug)
                    .put("layoutSlug", exact.candidate.layoutSlug)
                    .put("gameVersion", exact.candidate.gameVersion)
                    .put("sourceUrl", exact.candidate.sourceUrl)
                    .put("fileUrl", exact.candidate.fileUrl)
                    .put("fileName", exact.candidate.fileName);

            Files.write(temporaryBytes, exact.bytes);
            Files.write(temporaryMetadata, metadata.toString().getBytes(StandardCharsets.UTF_8));
            replace(temporaryBytes, bytesPath);
            replace(temporaryMetadata, metadataPath);
        } catch (Exception ignored) {
            // Cache failure never changes LIVE or SELF CALC correctness.
        }
    }

    private static String fileKey(SetupRequest request) {
        return Hashing.sha256(request.cacheKey().getBytes(StandardCharsets.UTF_8));
    }

    private static void replace(Path source, Path target) throws Exception {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException unsupported) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
