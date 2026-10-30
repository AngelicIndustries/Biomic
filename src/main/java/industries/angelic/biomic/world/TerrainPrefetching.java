package industries.angelic.biomic.world;

/** A Biomic generator whose terrain workers can start a chunk ahead of demand (DeferredChunkGeneration). */
public interface TerrainPrefetching {

    /** Starts the chunk's terrain on the workers; false once their look-ahead is full. */
    boolean prefetchTerrain(int chunkX, int chunkZ);

    /** Whether the workers have finished the chunk's terrain, so generating it now is cheap. */
    boolean terrainReady(int chunkX, int chunkZ);
}
