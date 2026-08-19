package com.greenbuddy.acevosetupengineer.beta;

import android.content.Context;

import com.greenbuddy.acevosetupengineer.verification.BinaryDigest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/** App-private, exact-layout-separated storage. There is no global or nearest-layout fallback. */
public final class MustangBaseRepository {
    private static final int MAX_BYTES = 1024 * 1024;
    private final Context context;

    public MustangBaseRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    public void save(String exactLayoutId, byte[] binary) throws IOException {
        String fileName = fileName(exactLayoutId);
        if (binary == null || binary.length == 0 || binary.length > MAX_BYTES) {
            throw new IOException("Invalid Mustang base size");
        }
        try (FileOutputStream output = context.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            output.write(binary);
            output.getFD().sync();
        }
        context.getSharedPreferences("mustang_beta_layout_bases", Context.MODE_PRIVATE)
                .edit()
                .putString(exactLayoutId + ".sha256", BinaryDigest.sha256(binary))
                .putLong(exactLayoutId + ".savedAt", System.currentTimeMillis())
                .apply();
    }

    public boolean has(String exactLayoutId) {
        return new File(context.getFilesDir(), fileName(exactLayoutId)).isFile();
    }

    public byte[] load(String exactLayoutId) throws IOException {
        File file = new File(context.getFilesDir(), fileName(exactLayoutId));
        if (!file.isFile()) throw new IOException("No exact-layout Mustang base");
        try (FileInputStream input = new FileInputStream(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream((int) file.length())) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                if (output.size() + read > MAX_BYTES) throw new IOException("Base too large");
                output.write(buffer, 0, read);
            }
            byte[] binary = output.toByteArray();
            String expected = context.getSharedPreferences("mustang_beta_layout_bases", Context.MODE_PRIVATE)
                    .getString(exactLayoutId + ".sha256", "");
            if (!BinaryDigest.sha256(binary).equals(expected)) throw new IOException("Private base hash mismatch");
            return binary;
        }
    }

    private static String fileName(String exactLayoutId) {
        MustangTrackTraits.require(exactLayoutId);
        return "mustang_v081_base_" + exactLayoutId + ".bin";
    }
}
