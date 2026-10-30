package industries.angelic.biomic.world.terrain;

/** Pure saved-policy selection; its callers separately validate world type, storage and peer lifecycle. */
public record AuthoredHeightPolicy(String revision, int requiredPhysicalStorage, int biomeVolumeSchema) {

    public static final String SAVE_KEY = "authoredHeightRevision";
    public static final String LEGACY_COMPRESSED_REVISION = "compressed-256-v1";
    public static final String SHIFTED_512_REVISION = "shifted-512-v1";
    private static final AuthoredHeightPolicy LEGACY = new AuthoredHeightPolicy(LEGACY_COMPRESSED_REVISION, 256, 1);
    private static final AuthoredHeightPolicy SHIFTED = new AuthoredHeightPolicy(SHIFTED_512_REVISION, 512, 2);

    /** Missing legacy saves remain compressed even when the installed storage and protocol can support 512. */
    public static AuthoredHeightPolicy select(String savedRevision, int physicalStorageHeight,
        boolean schema2CapabilityReady) {
        if (physicalStorageHeight != 256 && physicalStorageHeight != 512)
            throw new IllegalArgumentException("Unsupported physical storage height: " + physicalStorageHeight);
        if (savedRevision == null || LEGACY_COMPRESSED_REVISION.equals(savedRevision)) {
            if (physicalStorageHeight < LEGACY.requiredPhysicalStorage)
                throw new IllegalArgumentException("Legacy storage is truncated");
            return LEGACY;
        }
        if (SHIFTED_512_REVISION.equals(savedRevision)) {
            if (physicalStorageHeight != 512)
                throw new IllegalStateException("Shifted authored height requires physical storage 512");
            if (!schema2CapabilityReady)
                throw new IllegalStateException("Shifted authored height requires biome-volume schema 2 capability");
            return SHIFTED;
        }
        throw new IllegalArgumentException("Unsupported saved authored-height revision: " + savedRevision);
    }

    public boolean shifted512() {
        return SHIFTED_512_REVISION.equals(revision);
    }
}
