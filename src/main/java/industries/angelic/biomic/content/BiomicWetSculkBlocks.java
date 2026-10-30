package industries.angelic.biomic.content;

import net.minecraft.block.Block;

import industries.angelic.biomic.content.block.BlockSculkVein;
import industries.angelic.biomic.content.block.BlockWetSculkVein;

/** Persisted retained-source-water counterparts; dry registry IDs and metadata are never repurposed. */
public final class BiomicWetSculkBlocks {

    public static final BlockWetSculkVein VEIN = new BlockWetSculkVein("wet_sculk_vein", false, false);
    public static final BlockWetSculkVein VEIN_UP = new BlockWetSculkVein("wet_sculk_vein_up", true, false);
    public static final BlockWetSculkVein VEIN_DOWN = new BlockWetSculkVein("wet_sculk_vein_down", false, true);
    public static final BlockWetSculkVein VEIN_VERTICAL = new BlockWetSculkVein("wet_sculk_vein_vertical", true, true);

    private BiomicWetSculkBlocks() {}

    public static BlockWetSculkVein wetVein(boolean up, boolean down) {
        return up ? (down ? VEIN_VERTICAL : VEIN_UP) : (down ? VEIN_DOWN : VEIN);
    }

    public static boolean wet(Block block) {
        return block == VEIN || block == VEIN_UP || block == VEIN_DOWN || block == VEIN_VERTICAL;
    }

    public static boolean dry(Block block) {
        return block == BiomicSculkBlocks.SCULK_VEIN || block == BiomicSculkBlocks.SCULK_VEIN_UP
            || block == BiomicSculkBlocks.SCULK_VEIN_DOWN
            || block == BiomicSculkBlocks.SCULK_VEIN_VERTICAL;
    }

    public static BlockSculkVein dryCounterpart(BlockWetSculkVein block) {
        if (!wet(block)) throw new IllegalArgumentException("Unknown wet sculk identity");
        return BiomicSculkBlocks.vein(block.hasUp(), block.hasDown());
    }
}
