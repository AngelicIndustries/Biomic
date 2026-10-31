package industries.angelic.biomic.world.terrain;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

/**
 * Each chunk's carved ground height per column, kept long after the chunk's 128 KiB material array has left its
 * 128-chunk cache. Sky islands and hosted habitats ask for ground heights around chunks the look-ahead built well
 * before; rebuilding a whole 512-high chunk to read them was much of the server thread's work in a Distant Horizons
 * pre-generation. The heights are a pure function of the chunk, so this changes no world.
 */
final class GroundHeights {

    /** 512 bytes a chunk, so 4 MiB at most. */
    static final int MAX_CHUNKS = 8192;
    static final int UNKNOWN = Integer.MIN_VALUE;

    /** A column's ground height in a complete physical chunk. */
    @FunctionalInterface
    interface Scan {

        int height(byte[] materials, int localX, int localZ);
    }

    private final Scan scan;
    private final Long2ObjectLinkedOpenHashMap<short[]> chunks = new Long2ObjectLinkedOpenHashMap<>();

    GroundHeights(Scan scan) {
        this.scan = scan;
    }

    /** The column's height if its chunk was built, else UNKNOWN. */
    int get(int chunkX, int chunkZ, int localX, int localZ) {
        short[] heights;
        synchronized (chunks) {
            heights = chunks.getAndMoveToLast(key(chunkX, chunkZ));
        }
        return heights == null ? UNKNOWN : heights[localZ << 4 | localX];
    }

    /** Records the heights of a chunk that was just built. */
    void remember(int chunkX, int chunkZ, byte[] materials) {
        short[] heights = new short[256];
        for (int localX = 0; localX < 16; localX++) for (int localZ = 0; localZ < 16; localZ++)
            heights[localZ << 4 | localX] = (short) scan.height(materials, localX, localZ);
        synchronized (chunks) {
            chunks.putAndMoveToLast(key(chunkX, chunkZ), heights);
            if (chunks.size() > MAX_CHUNKS) chunks.removeFirst();
        }
    }

    private static long key(int chunkX, int chunkZ) {
        return (long) chunkX << 32 | chunkZ & 0xFFFFFFFFL;
    }
}
