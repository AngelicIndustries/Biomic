package industries.angelic.biomic.world;

/**
 * Whether a chunk's population runs now, the provider's and other mods' generators alike (marked around
 * ChunkProviderServer.populate by MixinChunkProviderServerGenerating). Server thread only.
 */
public final class GenerationScope {

    private static int depth;

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
}
