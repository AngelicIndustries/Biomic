package industries.angelic.biomic.compat.biome;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import net.minecraft.block.Block;

import org.junit.Test;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

public class BopSurfaceCaptureHeightTest {

    @Test
    public void legacyAccessRetainsDefaultPhysicalEnvelope() {
        NativeFeatureCapture.Access access = inert();
        var capture = new BopSurfaceCapture(access, new Random(1));
        assertEquals(0, capture.minimumY());
        assertEquals(256, capture.maximumYExclusive());
    }

    @Test
    public void explicitPhysicalEnvelopePropagatesThroughCaptureAndYOffset() {
        NativeFeatureCapture.Access access = new NativeFeatureCapture.Access() {

            public Block block(int x, int y, int z) {
                return null;
            }

            public int metadata(int x, int y, int z) {
                return 0;
            }

            public boolean set(int x, int y, int z, Block block, int metadata) {
                return true;
            }

            public int minimumY() {
                return 64;
            }

            public int maximumYExclusive() {
                return 448;
            }
        };
        var capture = new BopSurfaceCapture(access, new Random(1));
        assertEquals(64, capture.minimumY());
        assertEquals(448, capture.maximumYExclusive());
        assertTrue(NativeFeatureCapture.containsPhysicalY(capture, 64));
        assertFalse(NativeFeatureCapture.containsFeatureY(capture, 64));
        assertTrue(NativeFeatureCapture.containsFeatureY(capture, 267));
        assertTrue(NativeFeatureCapture.containsFeatureY(capture, 447));
        assertFalse(NativeFeatureCapture.containsPhysicalY(capture, 448));
        var translated = new BopSurfaceYOffset(capture, 200);
        assertEquals(-136, translated.minimumY());
        assertEquals(248, translated.maximumYExclusive());
    }

    private static NativeFeatureCapture.Access inert() {
        return new NativeFeatureCapture.Access() {

            public Block block(int x, int y, int z) {
                return null;
            }

            public int metadata(int x, int y, int z) {
                return 0;
            }

            public boolean set(int x, int y, int z, Block block, int metadata) {
                return true;
            }
        };
    }
}
