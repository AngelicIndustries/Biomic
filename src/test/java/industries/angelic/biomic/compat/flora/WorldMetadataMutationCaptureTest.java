package industries.angelic.biomic.compat.flora;

import static org.junit.Assert.*;

import net.minecraft.block.Block;

import org.junit.Test;

/** Exercises the exact hook World's setBlockMetadataWithNotify calls, without claiming a transformed Forge runtime. */
public class WorldMetadataMutationCaptureTest {

    @Test
    public void virtualMetadataUsesCurrentVirtualBlockAndNeverFallsThrough() {
        int[] calls = { 0 };
        NativeFeatureCapture.scoped(null, new NativeFeatureCapture.Access() {

            public Block block(int x, int y, int z) {
                assertNull(NativeFeatureCapture.current(null));
                calls[0]++;
                return null;
            }

            public int metadata(int x, int y, int z) {
                throw new AssertionError("Old metadata is not a write prerequisite");
            }

            public boolean set(int x, int y, int z, Block block, int metadata) {
                assertNull(block);
                assertEquals(-17, x);
                assertEquals(120, y);
                assertEquals(31, z);
                assertEquals(7, metadata);
                assertNull(NativeFeatureCapture.current(null));
                calls[0]++;
                return true;
            }
        }, () -> {
            assertTrue(WorldCaptureHooks.setMetadataApplies(null, -17, 120, 31, 7, 2));
            assertTrue(WorldCaptureHooks.setMetadata(null, -17, 120, 31, 7, 2));
            return null;
        });
        assertEquals(2, calls[0]);
        assertNull(NativeFeatureCapture.current(null));
    }

    @Test
    public void noCapturePreservesOrdinaryMetadataExecution() {
        assertFalse(WorldCaptureHooks.setMetadataApplies(null, -17, 120, 31, 7, 2));
    }
}
