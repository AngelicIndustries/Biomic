package industries.angelic.biomic.world;

import net.minecraft.world.World;

import industries.angelic.biomic.api.ChunkVerticalLayout;

/**
 * Hohen Angst gives the overworld 512-block chunk storage. Biomic then generates Origen at its uncompressed height
 * (shifted512) instead of squeezing it into 256 blocks. New worlds pin the choice in their saved height revision.
 */
public final class TallWorlds {

    private TallWorlds() {}

    /** A Biomic overworld running on 512-block storage. */
    public static boolean liveTall(World world) {
        return world != null && world.provider != null
            && world.provider.dimensionId == 0
            && world.getWorldInfo() != null
            && world.getWorldInfo()
                .getTerrainType() != null
            && WorldTypeBiomicBlend.ID.equals(
                world.getWorldInfo()
                    .getTerrainType()
                    .getWorldTypeName())
            && LiveChunkLayout.of(world)
                .height() == ChunkVerticalLayout.EXTENDED_512.height();
    }
}
