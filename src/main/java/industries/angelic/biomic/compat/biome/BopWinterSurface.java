package industries.angelic.biomic.compat.biome;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;

/** Native1.7 winter predicates, with an explicit habitat and no implicit World/chunk lookup. */
public final class BopWinterSurface {

    private BopWinterSurface() {}

    public static boolean coldDark(float temperature, int blockLight) {
        return temperature <= .15F && blockLight < 10;
    }

    public static boolean freeze(float temperature, int blockLight, Block block, int meta) {
        java.util.Objects.requireNonNull(block, "Uninitialized native block registry");
        return coldDark(temperature, blockLight) && (block == Blocks.water || block == Blocks.flowing_water)
            && meta == 0;
    }

    public static boolean snow(float temperature, int blockLight, Block target, Block below, int belowMeta,
        boolean leaves) {
        java.util.Objects.requireNonNull(target, "Uninitialized native block registry");
        java.util.Objects.requireNonNull(below, "Uninitialized native block registry");
        if (!coldDark(temperature, blockLight) || target.getMaterial() != Material.air
            || below == Blocks.ice
            || below == Blocks.packed_ice) return false;
        return leaves || below == Blocks.snow_layer && (belowMeta & 7) == 7
            || below.isOpaqueCube() && below.getMaterial()
                .blocksMovement();
    }

    @FunctionalInterface
    public interface Emission {

        int at(int x, int y, int z);
    }

    /**
     * Only emitters at Manhattan distance<=5 can produce level10 (max emission15).
     * Ignoring occlusion is conservative: may suppress snow near a shielded lamp,
     * never invent darkness beside lava. Values below10 are only a threshold
     * result, not a complete light field. No lighting engine or chunk acquisition.
     */
    public static final class HeatBound {

        private final Emission source;
        private final Map<Pos, Integer> cached = new HashMap<>();

        public HeatBound(Emission source) {
            this.source = source;
        }

        public int at(int x, int y, int z) {
            int upper = 0;
            for (int dx = -5; dx <= 5; dx++) for (int dy = -5 + Math.abs(dx); dy <= 5 - Math.abs(dx); dy++) {
                int radius = 5 - Math.abs(dx) - Math.abs(dy);
                for (int dz = -radius; dz <= radius; dz++) {
                    Pos p = new Pos(x + dx, y + dy, z + dz);
                    int emission = cached.computeIfAbsent(p, k -> {
                        if (cached.size() >= 131072)
                            throw new IllegalStateException("Native winter light envelope exceeded");
                        int value = source.at(k.x, k.y, k.z);
                        if (value < 0 || value > 15) throw new IllegalStateException("Invalid native block emission");
                        return value;
                    });
                    upper = Math.max(upper, emission - Math.abs(dx) - Math.abs(dy) - Math.abs(dz));
                    if (upper >= 10) return upper;
                }
            }
            return upper;
        }
    }
}
