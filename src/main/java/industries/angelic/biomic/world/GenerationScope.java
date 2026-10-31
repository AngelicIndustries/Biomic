package industries.angelic.biomic.world;

/**
 * Whether a chunk's population runs now, the provider's and other mods' generators alike (marked around
 * ChunkProviderServer.populate by MixinChunkProviderServerGenerating). Server thread only.
 */
public final class GenerationScope {

    private static int depth;
    private static boolean foreign;

    private GenerationScope() {}

    public static void enter() {
        depth++;
    }

    public static void leave() {
        depth--;
    }

    public static boolean active() {
        return depth > 0;
    }

    /**
     * Runs one pass of a population: Forge's world-generator pass (other mods' generators) or the provider's own. A
     * chunk the foreign pass makes generate populates inside it, with this off again for its provider's pass (marked by
     * MixinChunkProviderServerForeignGeneration).
     */
    public static void pass(boolean foreignPass, Runnable pass) {
        boolean previous = foreign;
        foreign = foreignPass;
        try {
            pass.run();
        } finally {
            foreign = previous;
        }
    }

    /** Whether other mods' generators are writing now. */
    public static boolean foreign() {
        return foreign;
    }
}
