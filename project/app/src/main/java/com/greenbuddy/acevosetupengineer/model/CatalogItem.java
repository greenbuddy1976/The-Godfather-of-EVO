package com.greenbuddy.acevosetupengineer.model;

public final class CatalogItem {
    public enum Kind { VEHICLE, LAYOUT }

    public final Kind kind;
    public final String id;
    public final String name;
    public final String providerSlug;
    public final String expectedSignaturePrefix;
    public final String rangeKey;
    public final String thumbnailUrl;
    public final String thumbnailSourceUrl;
    public final String thumbnailAltText;
    public final boolean thumbnailOfficialVerified;
    public final boolean officialVerified;
    public final boolean selectable;

    public CatalogItem(
            Kind kind,
            String id,
            String name,
            String providerSlug,
            String expectedSignaturePrefix,
            boolean officialVerified,
            boolean selectable) {
        this(kind, id, name, providerSlug, expectedSignaturePrefix, "", "", "", "", false,
                officialVerified, selectable);
    }

    public CatalogItem(
            Kind kind,
            String id,
            String name,
            String providerSlug,
            String expectedSignaturePrefix,
            String rangeKey,
            boolean officialVerified,
            boolean selectable) {
        this(kind, id, name, providerSlug, expectedSignaturePrefix, rangeKey, "", "", "", false,
                officialVerified, selectable);
    }

    public CatalogItem(
            Kind kind,
            String id,
            String name,
            String providerSlug,
            String expectedSignaturePrefix,
            String rangeKey,
            String thumbnailUrl,
            String thumbnailSourceUrl,
            String thumbnailAltText,
            boolean thumbnailOfficialVerified,
            boolean officialVerified,
            boolean selectable) {
        this.kind = kind;
        this.id = id;
        this.name = name;
        this.providerSlug = providerSlug;
        this.expectedSignaturePrefix = expectedSignaturePrefix;
        this.rangeKey = rangeKey;
        this.thumbnailUrl = thumbnailUrl;
        this.thumbnailSourceUrl = thumbnailSourceUrl;
        this.thumbnailAltText = thumbnailAltText;
        this.thumbnailOfficialVerified = thumbnailOfficialVerified;
        this.officialVerified = officialVerified;
        this.selectable = selectable;
    }

    public boolean hasVerifiedBinaryIdentity() {
        return expectedSignaturePrefix != null && !expectedSignaturePrefix.trim().isEmpty();
    }

    public boolean hasVerifiedRangeIdentity() {
        return rangeKey != null && !rangeKey.trim().isEmpty();
    }

    public boolean hasVerifiedThumbnail() {
        return thumbnailOfficialVerified
                && thumbnailUrl != null && thumbnailUrl.startsWith("https://assettocorsa.gg/")
                && thumbnailSourceUrl != null && thumbnailSourceUrl.startsWith("https://assettocorsa.gg/");
    }

    @Override
    public String toString() {
        return name;
    }
}
