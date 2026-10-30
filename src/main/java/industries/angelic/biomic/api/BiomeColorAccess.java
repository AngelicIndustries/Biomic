package industries.angelic.biomic.api;

import net.minecraft.world.biome.BiomeGenBase;

/** Immutable render-snapshot biome access. Never requests terrain or reads the live world from a mesh worker. */
public interface BiomeColorAccess {

    BiomeGenBase biomic$colorBiome(int x, int y, int z);
}
