package industries.angelic.biomic.world;

import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.world.terrain.ShiftedSkyIslands;

/** What the biome browser's search reads from an overworld provider, off the server thread. */
public interface BiomeLocating {

    WorldgenContext worldgenContext();

    /** A chunk's 3D biome volume as its sampler draws it, before the terrain shapes it. */
    BiomeVolume rawBiomeVolume(int chunkX, int chunkZ);

    /** The sky islands, or null where there are none. */
    ShiftedSkyIslands skyIslands();

    /** A column's highest solid Y before carving, or -1 where this world cannot say cheaply. */
    int groundHeight(int x, int z);
}
