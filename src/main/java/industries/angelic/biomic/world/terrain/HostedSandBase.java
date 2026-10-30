package industries.angelic.biomic.world.terrain;

import static industries.angelic.biomic.world.terrain.BiomicTerrainSampler.*;

import java.util.function.IntPredicate;

/** Replace an existing unsupported sand base, never fill the space under a landform. */
final class HostedSandBase {

    private HostedSandBase() {}

    static boolean ownsHabitat(String key) {
        return industries.angelic.biomic.compat.biome.BopNativeBiomes.LUSH_DESERT.equals(key)
            || industries.angelic.biomic.compat.biome.BopNativeBiomes.canyon(key);
    }

    static int apply(byte[] materials, int height, IntPredicate ownsCell) {
        if (height < 2 || height > 512 || materials.length != 256 * height || ownsCell == null)
            throw new IllegalArgumentException("Expected one supported-height material chunk and explicit ownership");
        int changed = 0;
        for (int column = 0; column < 256; column++) for (int y = 1; y < height; y++) {
            int i = column * height + y;
            byte state = materials[i], below = materials[i - 1];
            if ((state == SAND || state == RED_SAND) && (below == AIR || below == WATER || below == LAVA)
                && ownsCell.test(i)) {
                materials[i] = state == SAND ? SANDSTONE : RED_SANDSTONE;
                changed++;
            }
        }
        return changed;
    }
}
