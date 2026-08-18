package com.greenbuddy.acevosetupengineer.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;
import android.widget.TextView;

import com.greenbuddy.acevosetupengineer.R;
import com.greenbuddy.acevosetupengineer.model.CatalogItem;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public final class VehicleThumbnailLoader implements AutoCloseable {
    private static final int MAX_DOWNLOAD_BYTES = 1_000_000;
    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int READ_TIMEOUT_MS = 12_000;

    private final File cacheDirectory;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicLong requestCounter = new AtomicLong();
    private final LruCache<String, Bitmap> memoryCache = new LruCache<>(8 * 1024 * 1024) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getAllocationByteCount();
        }
    };

    public VehicleThumbnailLoader(Context context) {
        cacheDirectory = new File(context.getCacheDir(), "verified-vehicle-thumbnails");
        if (!cacheDirectory.exists()) cacheDirectory.mkdirs();
    }

    public void load(CatalogItem vehicle, ImageView target, TextView visibleName) {
        long request = requestCounter.incrementAndGet();
        target.setTag(request);
        visibleName.setText(vehicle.name);
        target.setContentDescription("Fahrzeugbild: " + vehicle.name);
        target.setImageResource(R.drawable.ic_car_placeholder);

        if (!vehicle.hasVerifiedThumbnail()) return;
        Bitmap memory = memoryCache.get(vehicle.id);
        if (memory != null) {
            target.setImageBitmap(memory);
            return;
        }

        executor.execute(() -> {
            Bitmap loaded = readDisk(vehicle.id);
            if (loaded == null) {
                try {
                    byte[] bytes = downloadVerified(vehicle.thumbnailUrl);
                    loaded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (loaded != null) writeDisk(vehicle.id, bytes);
                } catch (IOException ignored) {
                    loaded = null;
                }
            }
            Bitmap result = loaded;
            if (result != null) memoryCache.put(vehicle.id, result);
            main.post(() -> {
                Object tag = target.getTag();
                if (!(tag instanceof Long) || ((Long) tag) != request) return;
                if (result != null) {
                    target.setImageBitmap(result);
                    target.setContentDescription(vehicle.thumbnailAltText.trim().isEmpty()
                            ? "Fahrzeugbild: " + vehicle.name : vehicle.thumbnailAltText);
                } else {
                    target.setImageResource(R.drawable.ic_car_placeholder);
                    target.setContentDescription("Kein Fahrzeugbild geladen: " + vehicle.name);
                }
            });
        });
    }

    private Bitmap readDisk(String vehicleId) {
        File file = cacheFile(vehicleId);
        if (!file.isFile() || file.length() <= 0 || file.length() > MAX_DOWNLOAD_BYTES) return null;
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    private void writeDisk(String vehicleId, byte[] bytes) {
        if (!cacheDirectory.isDirectory()) return;
        File target = cacheFile(vehicleId);
        File temporary = new File(cacheDirectory, vehicleId + ".part");
        try (FileOutputStream output = new FileOutputStream(temporary)) {
            output.write(bytes);
            output.getFD().sync();
            if (!temporary.renameTo(target)) temporary.delete();
        } catch (IOException ignored) {
            temporary.delete();
        }
    }

    private File cacheFile(String vehicleId) {
        if (!vehicleId.matches("[a-z0-9-]+")) throw new IllegalArgumentException("Ungültige Fahrzeug-ID");
        return new File(cacheDirectory, vehicleId + ".jpg");
    }

    private byte[] downloadVerified(String value) throws IOException {
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException ex) {
            throw new IOException("Ungültige Bildadresse", ex);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"assettocorsa.gg".equalsIgnoreCase(uri.getHost())
                || !uri.getPath().startsWith("/wp-content/uploads/")) {
            throw new IOException("Nicht freigegebene Bildquelle");
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(value).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept", "image/jpeg,image/png,image/webp");
        try {
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Bildserver HTTP " + connection.getResponseCode());
            }
            String contentType = connection.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                throw new IOException("Antwort ist kein Bild");
            }
            int declaredLength = connection.getContentLength();
            if (declaredLength > MAX_DOWNLOAD_BYTES) throw new IOException("Bild ist zu groß");
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(8_192, declaredLength))) {
                byte[] buffer = new byte[8_192];
                int total = 0;
                int count;
                while ((count = input.read(buffer)) != -1) {
                    total += count;
                    if (total > MAX_DOWNLOAD_BYTES) throw new IOException("Bild ist zu groß");
                    output.write(buffer, 0, count);
                }
                return output.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
