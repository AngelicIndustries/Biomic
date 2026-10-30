package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import industries.angelic.biomic.content.BiomicBlocks;
import industries.angelic.biomic.content.BiomicDripleafBlocks;
import industries.angelic.biomic.content.BiomicWetSculkBlocks;

/** Fluid authority for exact persisted wet companions, not a general rendering-marker permission. */
public final class RetainedPlantSourceWater {

    private RetainedPlantSourceWater() {}

    public static boolean contains(Block block, int metadata) {
        if (metadata < 0 || metadata > 15) return false;
        if (block instanceof BlockCoralWallFan fan)
            return BlockCoralWallFan.validMetadata(metadata) && fan.isWaterlogged()
                && block == industries.angelic.biomic.content.BiomicCoralWallFans.get(fan.species(), true);
        if (block instanceof BlockBigDripleaf) return metadata < 4
            && (block == BiomicDripleafBlocks.WET_BIG_DRIPLEAF || block == BiomicDripleafBlocks.WET_BIG_DRIPLEAF_STEM);
        if (block instanceof BlockSmallDripleaf)
            return metadata < 8 && block == BiomicDripleafBlocks.WET_SMALL_DRIPLEAF;
        if (block instanceof BlockWetSculkVein vein)
            return BiomicWetSculkBlocks.wet(block) && vein.faceMask(metadata) != 0;
        if (block instanceof BlockSkylandsWetMangroveRoots)
            return metadata == 0 && block == BiomicBlocks.SKYLANDS_WET_MANGROVE_ROOTS;
        return block instanceof BlockSkylandsWetGlowLichen && metadata == 0
            && block == BiomicBlocks.SKYLANDS_WET_GLOW_LICHEN;
    }

    public static boolean contains(IBlockAccess world, int x, int y, int z) {
        return contains(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
    }

    /** Plant facing/face-mask bits are never water decay. */
    public static int fluidMetadata(IBlockAccess world, int x, int y, int z) {
        int metadata = world.getBlockMetadata(x, y, z);
        return contains(world.getBlock(x, y, z), metadata) ? 0 : metadata;
    }

    public static void schedule(World world, int x, int y, int z) {
        if (!world.isRemote && world.blockExists(x, y, z) && contains(world, x, y, z))
            world.scheduleBlockUpdate(x, y, z, world.getBlock(x, y, z), Blocks.flowing_water.tickRate(world));
    }

    public static void tick(World world, int x, int y, int z, Random random) {
        if (world.isRemote || !world.blockExists(x, y, z) || !contains(world, x, y, z)) return;
        // Vanilla slope search reads up to five cells away. A boundary tick must never generate neighbors.
        if (!world.checkChunksExist(x - 5, Math.max(0, y - 1), z - 5, x + 5, y + 1, z + 5)) {
            schedule(world, x, y, z);
            return;
        }
        Blocks.flowing_water.updateTick(world, x, y, z, random);
    }
}
