package industries.angelic.biomic.world;

/** Stable seed domains. Existing salts must never change within a generator version. */
public enum WorldgenPhase {

    TERRAIN(0x243F6A8885A308D3L),
    SURFACE_BIOME(0x13198A2E03707344L),
    CAVES(0x3C6EF372FE94F82BL),
    STRUCTURES(0x5A827999FCEF3243L),
    POPULATION(0x6A09E667F3BCC909L);

    private final long salt;

    WorldgenPhase(long salt) {
        this.salt = salt;
    }

    long getSalt() {
        return salt;
    }
}
