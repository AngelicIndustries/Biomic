package industries.angelic.biomic.world;

import net.minecraft.world.World;

import industries.angelic.biomic.api.ChunkVerticalLayout;
import industries.angelic.biomic.world.terrain.AuthoredVerticalProjections;

/**
 * Hohen Angst gives the overworld 512-block chunk storage. Biomic then generates Origen at its uncompressed height
 * (shifted512) instead of squeezing it into 256 blocks. New worlds pin the choice in their saved height revision.
 */
public final class TallWorlds {

    /** How far shifted512 lifts Origen: its sea sits at 190 instead of 62. */
    static final int SHIFT = AuthoredVerticalProjections.shifted512()
        .toPhysicalBlockY(0)
        .getAsInt();

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

    /**
     * The height vanilla's climate reads for a block at Y. Vanilla cools the air above Y=64 (BiomeGenBase
     * getFloatTemperature), which in a tall world puts the lowlands, lifted by SHIFT, 128 blocks up a mountain: rain
     * fell
     * as snow there and settled in whatever chunks were around a player, though Biomic generates them snowless.
     */
    public static int climateY(World world, int y) {
        return climateY(liveTall(world), y);
    }

    static int climateY(boolean tall, int y) {
        return tall ? y - SHIFT : y;
    }
}
