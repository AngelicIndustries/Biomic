package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class HostedBopMarineTerrainTest {

    private static byte[] sea(int floor) {
        byte[] data = new byte[65536];
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y <= 63; y++)
            data[BiomicTerrainSampler.index(x, y, z)] = y <= floor ? BiomicTerrainSampler.STONE
                : BiomicTerrainSampler.WATER;
        return data;
    }

    @Test
    public void unselectedAndInlandStayUnchanged() {
        byte[] cells = sea(35), before = cells.clone();
        new HostedBopMarineTerrain(42, (x, z) -> null).apply(cells, new byte[65536], 0, 0);
        assertArrayEquals(before, cells);
        var kelp = new HostedBopMarineTerrain(42, (x, z) -> "biomesoplenty:kelp_forest");
        assertEquals(72, kelp.floor(0, 0, 72));
        assertFalse(HostedBopMarineTerrain.marine("biomesoplenty:bayou"));
    }

    @Test
    public void bedsHaveDepthAndIslandsActuallyEmerge() {
        var kelp = new HostedBopMarineTerrain(42, (x, z) -> "biomesoplenty:kelp_forest");
        assertTrue(kelp.floor(0, 0, 61) <= 45);
        for (String profile : Arrays.asList("biomesoplenty:tropics", "biomesoplenty:volcano")) {
            byte[] cells = sea(30);
            var terrain = new HostedBopMarineTerrain(42, (x, z) -> profile);
            terrain.apply(cells, new byte[65536], 0, 0);
            int top = terrain.floor(0, 0, 30);
            assertTrue(top > 63);
            assertEquals(BiomicTerrainSampler.STONE, cells[BiomicTerrainSampler.index(0, top, 0)]);
            assertEquals(BiomicTerrainSampler.AIR, cells[BiomicTerrainSampler.index(0, top + 1, 0)]);
        }
    }

    @Test
    public void shoreCollarDoesNotMakeVerticalWalls() {
        var terrain = new HostedBopMarineTerrain(42, (x, z) -> x < 0 ? null : "biomesoplenty:volcano");
        assertEquals(30, terrain.floor(0, 0, 30));
        int previous = 30;
        for (int x = 1; x < 160; x++) {
            int floor = terrain.floor(x, 0, 30);
            assertTrue("Abrupt carrier edge", Math.abs(floor - previous) <= 3);
            previous = floor;
        }
        assertTrue(previous > 80);
    }

    @Test
    public void doesNotOverwriteSkyOriginOrExistingRockTower() {
        var terrain = new HostedBopMarineTerrain(42, (x, z) -> "biomesoplenty:volcano");
        byte[] cells = sea(30), origins = new byte[65536];
        int floating = BiomicTerrainSampler.index(0, 80, 0);
        cells[floating] = BiomicTerrainSampler.STONE;
        origins[floating] = TerrainDensitySampler.FLOATING_ORIGIN;
        cells[BiomicTerrainSampler.index(1, 63, 0)] = BiomicTerrainSampler.STONE;
        byte[] before = cells.clone();
        terrain.apply(cells, origins, 0, 0);
        for (int y = 0; y < 256; y++) {
            assertEquals(before[BiomicTerrainSampler.index(0, y, 0)], cells[BiomicTerrainSampler.index(0, y, 0)]);
            assertEquals(before[BiomicTerrainSampler.index(1, y, 0)], cells[BiomicTerrainSampler.index(1, y, 0)]);
        }
    }

    @Test
    public void authoredViewMirrorsEveryChangedPhysicalSibling() {
        var terrain = new HostedBopMarineTerrain(42, (x, z) -> "biomesoplenty:tropics");
        byte[] cells = sea(30);
        terrain.apply(cells, new byte[65536], 0, 0);
        int height = OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE - OrigenDensityField.UPSTREAM_MIN_Y;
        byte[] authored = new byte[height];
        Arrays.fill(authored, BiomicTerrainSampler.AIR);
        byte[] result = terrain.authoredView(authored, 0, 0, () -> cells, () -> fail("Unexpected cache miss"));
        for (int ay = 0; ay < height; ay++) {
            int y = OrigenVerticalProfileSampler.toBiomicBlockY(ay + OrigenDensityField.UPSTREAM_MIN_Y);
            if (y > 30 && y <= terrain.floor(0, 0, 30)) assertEquals(BiomicTerrainSampler.STONE, result[ay]);
            assertEquals(BiomicTerrainSampler.AIR, authored[ay]);
        }
    }

    @Test
    public void maskEvictionCannotRestoreTheOldSeabed() {
        var terrain = new HostedBopMarineTerrain(42, (x, z) -> "biomesoplenty:tropics");
        byte[] cells = sea(30);
        terrain.apply(cells, new byte[65536], 0, 0);
        int height = OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE - OrigenDensityField.UPSTREAM_MIN_Y;
        byte[] authored = new byte[height];
        byte[] before = terrain.authoredView(authored, 0, 0, () -> cells, () -> fail("Unexpected cache miss"));
        for (int cx = 1; cx <= HostedBopMarineTerrain.MASK_CACHE_CAPACITY + 1; cx++)
            terrain.apply(sea(30), new byte[65536], cx, 0);
        int[] restored = { 0 };
        byte[] after = terrain.authoredView(authored, 0, 0, () -> cells, () -> {
            restored[0]++;
            terrain.apply(sea(30), new byte[65536], 0, 0);
        });
        assertEquals(1, restored[0]);
        assertArrayEquals(before, after);
    }

    @Test
    public void aquaticsCannotClaimDryLand() {
        assertEquals(2, HostedVegetationPolicy.minimumWaterDepth("biomesoplenty:coral_reef"));
        assertEquals(2, HostedVegetationPolicy.minimumWaterDepth("biomesoplenty:kelp_forest"));
        assertEquals(0, HostedVegetationPolicy.minimumWaterDepth("biomesoplenty:mangrove"));
        assertEquals(63, HostedVegetationPolicy.waterDepthLimit("biomesoplenty:kelp_forest"));
    }
}
