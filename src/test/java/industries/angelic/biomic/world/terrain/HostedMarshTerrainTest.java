package industries.angelic.biomic.world.terrain;

import static industries.angelic.biomic.world.terrain.BiomicTerrainSampler.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class HostedMarshTerrainTest {

    @Test
    public void coreIsLowReliefWithDryAndWetSitesAndSoftBoundary() {
        var marsh = new HostedMarshTerrain(42, (x, z) -> x >= 0);
        int dry = 0, wet = 0;
        for (int x = 80; x < 208; x++) for (int z = -128; z < 0; z++) {
            int y = marsh.floor(x, z, 78);
            assertTrue(y >= 61 && y <= 65);
            if (y >= 63) dry++;
            else wet++;
            assertTrue(Math.abs(y - marsh.floor(x + 1, z, 78)) <= 1);
        }
        assertTrue(dry > 0 && wet > 0);
        assertEquals(78, marsh.floor(-1, 0, 78));
        assertEquals(78, marsh.floor(0, 0, 78));
        assertEquals(130, marsh.floor(100, 0, 130));
        for (int x = 0; x < 80; x++) assertTrue(Math.abs(marsh.floor(x, 0, 78) - marsh.floor(x + 1, 0, 78)) <= 1);
    }

    @Test
    public void rewritesAreProjectedWithoutTouchingFloatingIslandsOrSharedAuthoredData() {
        var marsh = new HostedMarshTerrain(42, (x, z) -> true);
        byte[] cells = new byte[65536], origins = new byte[65536];
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 1; y <= 78; y++) {
            cells[index(x, y, z)] = y == 78 ? GRASS : STONE;
            origins[index(x, y, z)] = 1;
        }
        cells[index(0, 160, 0)] = STONE;
        origins[index(0, 160, 0)] = 2;
        byte[] before = cells.clone();
        marsh.apply(cells, origins, 0, 0);
        assertEquals(STONE, cells[index(0, 160, 0)]);
        assertEquals(GRASS, cells[index(0, 78, 0)]);
        byte[] authored = new byte[384 * 256];
        java.util.Arrays.fill(authored, STONE);
        byte[] original = authored.clone(), view = marsh.authoredView(
            authored,
            0,
            0,
            () -> cells,
            () -> { throw new AssertionError("Cached mask should remain available"); });
        int changes = 0;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y < 256; y++) {
            int i = index(x, y, z);
            if (cells[i] == before[i]) continue;
            changes++;
            for (int ay : OrigenVerticalProfileSampler.authoredBlockYsAt(y))
                assertEquals(cells[i], view[OrigenSurfaceRuntime.authoredMaterialIndex(x, ay, z)]);
        }
        assertTrue(changes > 100);
        assertArrayEquals(original, authored);
    }

    @Test
    public void evictedMaskRestoresTheExactAuthoredProjection() {
        var marsh = new HostedMarshTerrain(42, (x, z) -> true);
        byte[] base = new byte[65536], origins = new byte[65536];
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 1; y <= 78; y++) {
            base[index(x, y, z)] = y == 78 ? GRASS : STONE;
            origins[index(x, y, z)] = 1;
        }
        byte[] physical = base.clone();
        marsh.apply(physical, origins.clone(), 0, 0);
        byte[] authored = new byte[384 * 256];
        java.util.Arrays.fill(authored, STONE);
        byte[] expected = marsh.authoredView(
            authored,
            0,
            0,
            () -> physical,
            () -> { throw new AssertionError("Cached mask should remain available"); });
        for (int cx = 1; cx <= HostedMarshTerrain.MASK_CACHE_CAPACITY + 1; cx++)
            marsh.apply(base.clone(), origins.clone(), cx, 0);
        int[] restores = { 0 };
        byte[] actual = marsh.authoredView(authored, 0, 0, () -> physical, () -> {
            restores[0]++;
            marsh.apply(base.clone(), origins.clone(), 0, 0);
        });
        assertEquals(1, restores[0]);
        assertArrayEquals(expected, actual);
        assertNotSame(authored, actual);
    }

    @Test
    public void unselectedHabitatAndProtectedLayersAreNotFlattened() {
        byte[] cells = new byte[65536], origins = new byte[65536];
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 1; y <= 78; y++) {
            cells[index(x, y, z)] = y == 78 ? GRASS : STONE;
            origins[index(x, y, z)] = TerrainDensitySampler.GROUND_ORIGIN;
        }
        var untouched = cells.clone();
        new HostedMarshTerrain(42, (x, z) -> false).apply(cells, origins, 0, 0);
        assertArrayEquals(untouched, cells);
        cells[index(0, 75, 0)] = LAVA;
        cells[index(1, 75, 0)] = BEDROCK;
        origins[index(2, 75, 0)] = TerrainDensitySampler.FLOATING_ORIGIN;
        untouched = cells.clone();
        new HostedMarshTerrain(42, (x, z) -> true).apply(cells, origins, 0, 0);
        for (int x = 0; x < 3; x++)
            for (int y = 0; y < 256; y++) assertEquals(untouched[index(x, y, 0)], cells[index(x, y, 0)]);
        assertNotEquals(untouched[index(3, 78, 0)], cells[index(3, 78, 0)]);
    }

    @Test
    public void shiftedMarshUsesSourceHeightAndPreservesBareDeepBelow() {
        var marsh = HostedMarshTerrain.shifted512(42, (x, z) -> true);
        var layout = industries.angelic.biomic.api.ChunkVerticalLayout.EXTENDED_512;
        var projection = AuthoredVerticalProjections.shifted512();
        byte[] cells = new byte[layout.blockCount()], origins = new byte[layout.blockCount()];
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 64; y++) cells[layout.index(x, y, z)] = (byte) (y == 0 ? BEDROCK : STONE);
            for (int source = -64; source <= 78; source++) {
                int y = projection.toPhysicalBlockY(source)
                    .orElseThrow(), i = layout.index(x, y, z);
                cells[i] = source == 78 ? GRASS : STONE;
                origins[i] = TerrainDensitySampler.GROUND_ORIGIN;
            }
        }
        byte[] before = cells.clone();
        marsh.apply(cells, origins, 0, 0);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            for (int y = 0; y < 64; y++) assertEquals(before[layout.index(x, y, z)], cells[layout.index(x, y, z)]);
            int target = marsh.floor(x, z, 78), targetY = projection.toPhysicalBlockY(target)
                .orElseThrow();
            // The shifted frame's sea is Origen's (62), so the marsh meets Origen's sea without a step.
            assertEquals(
                target >= OrigenDensityField.UPSTREAM_SEA_LEVEL ? GRASS : DIRT,
                cells[layout.index(x, targetY, z)]);
            assertEquals(TerrainDensitySampler.GROUND_ORIGIN, origins[layout.index(x, targetY, z)]);
        }
        assertFalse(java.util.Arrays.equals(before, cells));
    }

    @Test
    public void shiftedAuthoredViewProjectsOnlyTheRecordedSourceCells() {
        var marsh = HostedMarshTerrain.shifted512(42, (x, z) -> true);
        var layout = industries.angelic.biomic.api.ChunkVerticalLayout.EXTENDED_512;
        var projection = AuthoredVerticalProjections.shifted512();
        byte[] cells = new byte[layout.blockCount()], origins = new byte[layout.blockCount()];
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int source = -64; source <= 78; source++) {
            int y = projection.toPhysicalBlockY(source)
                .orElseThrow(), i = layout.index(x, y, z);
            cells[i] = source == 78 ? GRASS : STONE;
            origins[i] = TerrainDensitySampler.GROUND_ORIGIN;
        }
        marsh.apply(cells, origins, 0, 0);
        byte[] authored = new byte[384 * 256];
        java.util.Arrays.fill(authored, STONE);
        byte[] original = authored.clone();
        byte[] view = marsh.authoredView(authored, 0, 0, () -> cells, () -> fail("Unexpected mask restore"));
        int changed = 0;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int source = -64; source < 320; source++) {
            int physical = projection.toPhysicalBlockY(source)
                .orElseThrow();
            int authoredIndex = OrigenSurfaceRuntime.authoredMaterialIndex(x, source, z);
            if (view[authoredIndex] != original[authoredIndex]) {
                changed++;
                assertEquals(cells[layout.index(x, physical, z)], view[authoredIndex]);
            }
        }
        assertTrue(changed > 100);
        assertArrayEquals(original, authored);
    }
}
