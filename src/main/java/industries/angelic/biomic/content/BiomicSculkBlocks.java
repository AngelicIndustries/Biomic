package industries.angelic.biomic.content;

import industries.angelic.biomic.content.block.BlockSculkFlora;
import industries.angelic.biomic.content.block.BlockSculkFlora.Kind;
import industries.angelic.biomic.content.block.BlockSculkVein;

/** Dedicated worldgen forms for the Deep Dark blocks which EFR does not register. */
public final class BiomicSculkBlocks {

    public static final BlockSculkFlora SCULK_SENSOR = new BlockSculkFlora("sculk_sensor", Kind.SENSOR);
    public static final BlockSculkFlora SCULK_SHRIEKER = new BlockSculkFlora("sculk_shrieker", Kind.SHRIEKER);

    /*
     * A modern sculk vein has six independent face properties, while legacy metadata only has four bits. The four
     * internal block IDs retain up/down and metadata retains N/E/S/W. This avoids a tile entity for every vein while
     * preserving the exact state authored by Origen's TerraScript, including deferred cross-chunk placements.
     */
    public static final BlockSculkVein SCULK_VEIN = new BlockSculkVein("sculk_vein", false, false);
    public static final BlockSculkVein SCULK_VEIN_UP = new BlockSculkVein("sculk_vein_up", true, false);
    public static final BlockSculkVein SCULK_VEIN_DOWN = new BlockSculkVein("sculk_vein_down", false, true);
    public static final BlockSculkVein SCULK_VEIN_VERTICAL = new BlockSculkVein("sculk_vein_vertical", true, true);

    private BiomicSculkBlocks() {}

    public static BlockSculkVein vein(boolean up, boolean down) {
        if (up) return down ? SCULK_VEIN_VERTICAL : SCULK_VEIN_UP;
        return down ? SCULK_VEIN_DOWN : SCULK_VEIN;
    }
}
