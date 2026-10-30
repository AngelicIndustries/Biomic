package industries.angelic.biomic.world.terrain;

import static industries.angelic.biomic.world.terrain.BiomicTerrainSampler.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class HostedSandBaseTest {

    @Test
    public void hardenedCanyonRoofDoesNotHideUnsupportedSandAndOtherHabitatsDoNotOptIn() {
        var nativeBiomes = industries.angelic.biomic.compat.biome.BopNativeBiomes.supportedKeys();
        for (String key : nativeBiomes) assertEquals(
            java.util.Set.of("biomesoplenty:canyon", "biomesoplenty:canyon_ravine", "biomesoplenty:lush_desert")
                .contains(key),
            HostedSandBase.ownsHabitat(key));
        assertFalse(HostedSandBase.ownsHabitat(null));
        assertFalse(HostedSandBase.ownsHabitat("biomic:surface"));
        byte[] cells = new byte[256 * 256];
        cells[62] = SAND;
        for (int y = 63; y <= 65; y++) cells[y] = DIRT;
        byte[] original = cells.clone();
        assertEquals(1, HostedSandBase.apply(cells, 256, i -> HostedSandBase.ownsHabitat("biomesoplenty:canyon")));
        original[62] = SANDSTONE;
        assertArrayEquals(original, cells);
    }

    @Test
    public void onlyUnsupportedAuthorizedBaseChangesAtBothHeights() {
        for (int height : new int[] { 256, 512 }) for (byte below : new byte[] { AIR, WATER, LAVA }) {
            byte[] cells = new byte[256 * height];
            int base = height + height - 4;
            cells[base - 1] = below;
            cells[base] = RED_SAND;
            cells[base + 1] = RED_SAND;
            cells[base + 2] = SAND;
            byte[] original = cells.clone();
            assertEquals(0, HostedSandBase.apply(cells, height, i -> false));
            assertArrayEquals(original, cells);
            assertEquals(1, HostedSandBase.apply(cells, height, i -> i == base));
            assertEquals(RED_SANDSTONE, cells[base]);
            assertEquals(below, cells[base - 1]);
            assertEquals(RED_SAND, cells[base + 1]);
            assertEquals(SAND, cells[base + 2]);
            assertEquals(0, HostedSandBase.apply(cells, height, i -> true));
        }
    }

    @Test
    public void retainsSupportedSandAndUnrelatedMaterials() {
        byte[] cells = new byte[256 * 256];
        cells[20] = STONE;
        cells[21] = SAND;
        cells[30] = GRAVEL;
        cells[40] = TERRACOTTA;
        byte[] original = cells.clone();
        assertEquals(0, HostedSandBase.apply(cells, 256, i -> true));
        assertArrayEquals(original, cells);
        cells[50] = SAND;
        assertEquals(1, HostedSandBase.apply(cells, 256, i -> true));
        assertEquals(SANDSTONE, cells[50]);
    }

    @Test
    public void rejectsInvalidShapesBeforeMutation() {
        assertThrows(IllegalArgumentException.class, () -> HostedSandBase.apply(new byte[256], 1, i -> true));
        assertThrows(IllegalArgumentException.class, () -> HostedSandBase.apply(new byte[256], 256, i -> true));
        assertThrows(IllegalArgumentException.class, () -> HostedSandBase.apply(new byte[256 * 256], 256, null));
    }
}
