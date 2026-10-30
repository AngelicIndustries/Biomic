package industries.angelic.biomic.compat.flora;

import static org.junit.Assert.*;

import net.minecraft.block.Block;

import org.junit.Test;

/** Detached coordinate/ownership proof, not a native optional-provider population witness. */
public class NativeFloraHeightFrameTest {

    private static class Access implements NativeFeatureCapture.Access {

        final int minimum, maximum;
        int observed, written, meta;

        Access(int minimum, int maximum) {
            this.minimum = minimum;
            this.maximum = maximum;
        }

        public int minimumY() {
            return minimum;
        }

        public int maximumYExclusive() {
            return maximum;
        }

        public Block block(int x, int y, int z) {
            assertTrue(y >= minimum && y < maximum);
            observed = y;
            return null;
        }

        public int metadata(int x, int y, int z) {
            observed = y;
            return 7;
        }

        public boolean set(int x, int y, int z, Block block, int value) {
            assertTrue(y >= minimum && y < maximum);
            written = y;
            meta = value;
            return true;
        }

        public int heightValue(int x, int z) {
            return 410;
        }
    }

    @Test
    public void all512UsesExactRigidSourceFrameAndRestoresCapture() {
        NativeFeatureCapture.install();
        Access physical = new Access(64, 512);
        assertTrue(NativeFloraHeightFrame.run(null, physical, 400, y -> {
            assertEquals(128, (int) y);
            var frame = NativeFeatureCapture.current(null);
            frame.block(-17, y - 1, 31);
            assertEquals(399, physical.observed);
            assertEquals(7, frame.metadata(-17, y, 31));
            assertEquals(400, physical.observed);
            assertEquals(138, frame.heightValue(-17, 31));
            assertTrue(frame.set(-17, y + 7, 31, null, 3));
            return true;
        }));
        assertEquals(407, physical.written);
        assertEquals(3, physical.meta);
        assertNull(NativeFeatureCapture.current(null));
    }

    @Test
    public void legacy256IsLiteralAndActualUpperLowerBoundsRejectWithoutRescue() {
        NativeFeatureCapture.install();
        Access legacy = new Access(0, 256);
        assertTrue(NativeFloraHeightFrame.run(null, legacy, 200, y -> {
            assertEquals(200, (int) y);
            return true;
        }));
        Access tall = new Access(64, 512);
        int[] calls = { 0 };
        assertFalse(NativeFloraHeightFrame.run(null, tall, 64, y -> {
            calls[0]++;
            return true;
        }));
        assertFalse(NativeFloraHeightFrame.run(null, tall, 512, y -> {
            calls[0]++;
            return true;
        }));
        assertEquals(0, calls[0]);
        var low = NativeFloraHeightFrame.translated(tall, 65 - 128);
        assertTrue(NativeFeatureCapture.containsPhysicalY(low, 127));
        assertFalse(NativeFeatureCapture.containsPhysicalY(low, 126));
        var high = NativeFloraHeightFrame.translated(tall, 508 - 128);
        assertTrue(NativeFeatureCapture.containsPhysicalY(high, 131));
        assertFalse(NativeFeatureCapture.containsPhysicalY(high, 132));
    }
}
