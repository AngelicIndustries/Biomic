package industries.angelic.biomic.compat.flora;

import java.util.function.IntFunction;

import net.minecraft.block.Block;
import net.minecraft.world.World;

/** Rigid local source frame for audited 256-high generators, never a clipped native feature. */
public final class NativeFloraHeightFrame {

    private NativeFloraHeightFrame() {}

    public static boolean run(World world, NativeFeatureCapture.Access physical, int anchorY,
        IntFunction<Boolean> generator) {
        if (!NativeFeatureCapture.containsFeatureY(physical, anchorY)) return false;
        if (physical.maximumYExclusive() <= 256)
            return NativeFeatureCapture.run(world, physical, () -> generator.apply(anchorY));
        // Trees contain literal source bounds (Pam/Growthcraft: 256). Keep the source
        // anchor at 128 and translate ALL observations/writes, preserving their RNG.
        int offset = anchorY - 128;
        NativeFeatureCapture.Access source = translated(physical, offset);
        return NativeFeatureCapture.run(world, source, () -> generator.apply(128));
    }

    static NativeFeatureCapture.Access translated(NativeFeatureCapture.Access physical, int offset) {
        return new NativeFeatureCapture.Access() {

            private int nativeY(int y) {
                return Math.addExact(y, offset);
            }

            public Block block(int x, int y, int z) {
                return physical.block(x, nativeY(y), z);
            }

            public int metadata(int x, int y, int z) {
                return physical.metadata(x, nativeY(y), z);
            }

            public boolean set(int x, int y, int z, Block block, int meta) {
                return physical.set(x, nativeY(y), z, block, meta);
            }

            public int heightValue(int x, int z) {
                return physical.heightValue(x, z) - offset;
            }

            public int topSolidOrLiquid(int x, int z) {
                return physical.topSolidOrLiquid(x, z) - offset;
            }

            public int minimumY() {
                return Math.max(0, physical.minimumY() - offset);
            }

            public int maximumYExclusive() {
                return Math.min(256, physical.maximumYExclusive() - offset);
            }
        };
    }
}
