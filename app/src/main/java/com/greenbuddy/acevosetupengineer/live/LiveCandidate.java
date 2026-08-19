package com.greenbuddy.acevosetupengineer.live;

import java.util.Objects;

public final class LiveCandidate {
    private final String carId;
    private final String layoutId;
    private final String gameVersion;
    private final String setupId;
    private final String source;
    private final String downloadAddress;
    private final String declaredSha256;
    private final long checkedAtEpochMillis;
    private final byte[] binary;

    public LiveCandidate(String carId, String layoutId, String gameVersion,
                         String setupId, String source, String downloadAddress,
                         String declaredSha256, long checkedAtEpochMillis, byte[] binary) {
        this.carId = Objects.requireNonNull(carId, "carId");
        this.layoutId = Objects.requireNonNull(layoutId, "layoutId");
        this.gameVersion = Objects.requireNonNull(gameVersion, "gameVersion");
        this.setupId = Objects.requireNonNull(setupId, "setupId");
        this.source = Objects.requireNonNull(source, "source");
        this.downloadAddress = Objects.requireNonNull(downloadAddress, "downloadAddress");
        this.declaredSha256 = Objects.requireNonNull(declaredSha256, "declaredSha256");
        this.checkedAtEpochMillis = checkedAtEpochMillis;
        this.binary = Objects.requireNonNull(binary, "binary").clone();
    }

    public String getCarId() { return carId; }
    public String getLayoutId() { return layoutId; }
    public String getGameVersion() { return gameVersion; }
    public String getSetupId() { return setupId; }
    public String getSource() { return source; }
    public String getDownloadAddress() { return downloadAddress; }
    public String getDeclaredSha256() { return declaredSha256; }
    public long getCheckedAtEpochMillis() { return checkedAtEpochMillis; }
    public byte[] getBinary() { return binary.clone(); }
}
