package industries.angelic.biomic.api;

import static org.junit.Assert.*;

import org.junit.Test;

public class ChunkVerticalLayoutTest {

    @Test
    public void everyVoxelRoundTripsWithoutEightBitYAliasing() {
        for (var layout : ChunkVerticalLayout.values()) {
            boolean[] seen = new boolean[layout.blockCount()];
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 0; y < layout.height(); y++) {
                int index = layout.index(x, y, z);
                assertFalse(seen[index]);
                seen[index] = true;
                assertEquals(x, layout.localX(index));
                assertEquals(y, layout.localY(index));
                assertEquals(z, layout.localZ(index));
                if (layout == ChunkVerticalLayout.LEGACY_256) assertEquals((x << 12) | (z << 8) | y, index);
            }
            for (boolean cell : seen) assertTrue(cell);
        }
        var tall = ChunkVerticalLayout.EXTENDED_512;
        assertNotEquals(tall.index(0, 256, 0), tall.index(0, 0, 1));
    }

    @Test
    public void masksRetainSection31AndAll32Bits() {
        var legacy = ChunkVerticalLayout.LEGACY_256;
        var tall = ChunkVerticalLayout.EXTENDED_512;
        assertEquals(0xffff, legacy.sectionMask(Integer.MIN_VALUE, Integer.MAX_VALUE));
        assertEquals(-1, tall.sectionMask(Integer.MIN_VALUE, Integer.MAX_VALUE));
        assertEquals(0x80000000, tall.sectionMask(511, 511));
        assertEquals(0x18000, tall.sectionMask(255, 256));
        assertEquals(0, tall.sectionMask(512, 513));
        assertEquals(0, tall.sectionMask(-20, -1));
        assertEquals(0, tall.sectionMask(256, 255));
        assertEquals(0, legacy.sectionMask(256, 511));
        for (var layout : ChunkVerticalLayout.values())
            for (int a = -1; a <= layout.height(); a += 7) for (int b = a; b <= layout.height(); b += 11) {
                int expected = 0;
                for (int y = Math.max(0, a); y <= Math.min(layout.height() - 1, b); y++) expected |= 1 << (y >> 4);
                assertEquals(expected, layout.sectionMask(a, b));
            }
    }

    @Test
    public void unsupportedShapesAndCoordinatesFailClosed() {
        assertSame(ChunkVerticalLayout.EXTENDED_512, ChunkVerticalLayout.forHeight(512));
        for (int height : new int[] { -64, 0, 128, 384, 513, 1024 })
            assertThrows(IllegalArgumentException.class, () -> ChunkVerticalLayout.forHeight(height));
        for (var layout : ChunkVerticalLayout.values()) {
            assertThrows(IllegalArgumentException.class, () -> layout.index(0, -1, 0));
            assertThrows(IllegalArgumentException.class, () -> layout.index(0, layout.height(), 0));
            assertThrows(IllegalArgumentException.class, () -> layout.index(16, 0, 0));
            assertThrows(IllegalArgumentException.class, () -> layout.index(0, 0, -1));
            assertThrows(IllegalArgumentException.class, () -> layout.localY(-1));
            assertThrows(IllegalArgumentException.class, () -> layout.localX(layout.blockCount()));
            assertEquals(layout.height() / 16, layout.sectionCount());
        }
    }
}
