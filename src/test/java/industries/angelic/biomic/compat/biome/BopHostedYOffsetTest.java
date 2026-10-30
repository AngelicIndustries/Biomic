package industries.angelic.biomic.compat.biome;

import static org.junit.Assert.*;

import net.minecraft.block.Block;

import org.junit.Test;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

public class BopHostedYOffsetTest {

    @Test
    public void translatesCompleteLegacyTreeViewToUpperPhysicalSurface() {
        int[] observed = new int[5];
        var physical = new BopNativePond.HostedAccess() {

            public Block block(int x, int y, int z) {
                observed[0] = y;
                return null;
            }

            public int metadata(int x, int y, int z) {
                observed[1] = y;
                return 7;
            }

            public boolean set(int x, int y, int z, Block block, int metadata) {
                observed[2] = y;
                return true;
            }

            public int heightValue(int x, int z) {
                return 267;
            }

            public int minimumY() {
                return 64;
            }

            public int maximumYExclusive() {
                return 512;
            }

            public boolean immutableCarrier(int x, int y, int z, Block block, int metadata) {
                observed[3] = y;
                return true;
            }

            public String immutableCarrierRejection(int x, int y, int z, Block block, int metadata) {
                observed[4] = y;
                return null;
            }
        };
        var source = new BopHostedYOffset(physical, 147);
        source.block(0, 120, 0);
        source.metadata(0, 121, 0);
        source.set(0, 122, 0, null, 0);
        source.immutableCarrier(0, 123, 0, null, 0);
        source.immutableCarrierRejection(0, 124, 0, null, 0);
        assertArrayEquals(new int[] { 267, 268, 269, 270, 271 }, observed);
        assertEquals(120, source.heightValue(0, 0));
        assertEquals(0, source.minimumY());
        assertEquals(256, source.maximumYExclusive());
        assertTrue(NativeFeatureCapture.containsPhysicalY(source, 0));
        assertFalse(NativeFeatureCapture.containsFeatureY(source, 0));
        assertTrue(NativeFeatureCapture.containsFeatureY(source, 120));
        assertTrue(NativeFeatureCapture.containsPhysicalY(source, 255));
        assertFalse(NativeFeatureCapture.containsPhysicalY(source, 256));
    }

    @Test
    public void negativeOffsetIntersectsWithoutClampingQueries() {
        int[] observed = { Integer.MIN_VALUE };
        var physical = access(64, 512, observed);
        var source = new BopHostedYOffset(physical, -20);
        assertEquals(84, source.minimumY());
        assertEquals(256, source.maximumYExclusive());
        assertFalse(NativeFeatureCapture.containsPhysicalY(source, 83));
        assertTrue(NativeFeatureCapture.containsFeatureY(source, 85));
        source.block(0, 90, 0);
        assertEquals(70, observed[0]);
    }

    @Test
    public void legacyZeroOffsetKeepsExactEnvelope() {
        int[] observed = { Integer.MIN_VALUE };
        var source = new BopHostedYOffset(access(0, 256, observed), 0);
        assertEquals(0, source.minimumY());
        assertEquals(256, source.maximumYExclusive());
        source.block(0, 255, 0);
        assertEquals(255, observed[0]);
    }

    @Test
    public void rejectsEmptyIntersectionAndOffsetOverflow() {
        assertThrows(IllegalArgumentException.class, () -> new BopHostedYOffset(access(64, 512, new int[1]), 600));
        assertThrows(
            ArithmeticException.class,
            () -> new BopHostedYOffset(access(64, 512, new int[1]), Integer.MIN_VALUE));
    }

    private static BopNativePond.HostedAccess access(int minimum, int maximum, int[] observed) {
        return new BopNativePond.HostedAccess() {

            public Block block(int x, int y, int z) {
                observed[0] = y;
                return null;
            }

            public int metadata(int x, int y, int z) {
                return 0;
            }

            public boolean set(int x, int y, int z, Block block, int metadata) {
                observed[0] = y;
                return true;
            }

            public int heightValue(int x, int z) {
                return 100;
            }

            public int minimumY() {
                return minimum;
            }

            public int maximumYExclusive() {
                return maximum;
            }

            public boolean immutableCarrier(int x, int y, int z, Block block, int metadata) {
                return true;
            }
        };
    }
}
