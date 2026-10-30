package industries.angelic.biomic.compat.biome;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.Direction;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Native horizontal attachment masks, without loading chunks or mutating a vine to test it. */
public final class BopTreeAttachments {

    private BopTreeAttachments() {}

    public static String vineState(int metadata) {
        if (metadata < 1 || metadata > 15)
            throw new IllegalArgumentException("Native tree vine needs horizontal faces");
        return "minecraft:vine[south=" + ((metadata & 1) != 0)
            + ",west="
            + ((metadata & 2) != 0)
            + ",north="
            + ((metadata & 4) != 0)
            + ",east="
            + ((metadata & 8) != 0)
            + "]";
    }

    public static String cocoaState(int metadata) {
        if (metadata < 0 || metadata > 11) throw new IllegalArgumentException("Invalid native cocoa age/facing");
        return "minecraft:cocoa[age=" + (metadata >> 2)
            + ",facing="
            + new String[] { "south", "east", "north", "west" }[metadata & 3]
            + "]";
    }

    private static boolean vineSupport(Block block) {
        return block.renderAsNormalBlock() && block.getMaterial()
            .blocksMovement();
    }

    /** Explicit native vine identity; never borrow support from another vine species. */
    public static int supportedVineFaces(NativeFeatureCapture.Access view, int x, int y, int z, int metadata,
        Block vine) {
        if (!(vine instanceof net.minecraft.block.BlockVine))
            throw new IllegalArgumentException("Expected native vine block");
        if (metadata < 1 || metadata > 15 || !NativeFeatureCapture.containsFeatureY(view, y)) return 0;
        int supported = 0;
        for (int side = 0; side < 4; side++) if ((metadata & (1 << side)) != 0) {
            boolean attached = false;
            for (int up = 0; up <= 64 && y + up < view.maximumYExclusive(); up++) {
                if (vineSupport(view.block(x + Direction.offsetX[side], y + up, z + Direction.offsetZ[side]))) {
                    attached = true;
                    break;
                }
                if (y + up + 1 >= view.maximumYExclusive() || view.block(x, y + up + 1, z) != vine
                    || (view.metadata(x, y + up + 1, z) & (1 << side)) == 0) break;
            }
            if (attached) supported |= 1 << side;
        }
        return supported;
    }

    public static boolean cocoaSupported(NativeFeatureCapture.Access view, int x, int y, int z, int metadata) {
        if (metadata < 0 || metadata > 11 || !NativeFeatureCapture.containsFeatureY(view, y)) return false;
        int face = metadata & 3, px = x + Direction.offsetX[face], pz = z + Direction.offsetZ[face];
        return view.block(px, y, pz) == Blocks.log && (view.metadata(px, y, pz) & 3) == 3;
    }
}
