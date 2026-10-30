package industries.angelic.biomic.compat.biome;

import static org.junit.Assert.*;

import java.util.Random;

import net.minecraft.block.Block;

import org.junit.Test;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

public class BopSacredCaptureBudgetTest {

    private NativeFeatureCapture.Access sink() {
        return new NativeFeatureCapture.Access() {

            public Block block(int x, int y, int z) {
                return null;
            }

            public int metadata(int x, int y, int z) {
                return 0;
            }

            public int heightValue(int x, int z) {
                return 0;
            }

            public boolean set(int x, int y, int z, Block block, int meta) {
                return true;
            }
        };
    }

    @Test
    public void ordinaryBudgetRemainsUnchanged() {
        var capture = new BopSurfaceCapture(sink(), new Random(0));
        for (int i = 0; i < 32768; i++) assertTrue(capture.set(0, 0, 0, null, 0));
        try {
            capture.set(0, 0, 0, null, 0);
            fail("Ordinary budget silently expanded");
        } catch (IllegalStateException expected) {
            assertTrue(
                expected.getMessage()
                    .contains("write budget"));
        }
    }

    @Test
    public void sacredRepeatedWritesAreStillBounded() {
        var capture = BopSurfaceCapture.sacredOak(sink(), new Random(0));
        for (int i = 0; i < 131072; i++) capture.set(0, 0, 0, null, 0);
        try {
            capture.set(0, 0, 0, null, 0);
            fail("Sacred budget is unbounded");
        } catch (IllegalStateException expected) {
            assertTrue(
                expected.getMessage()
                    .contains("write budget"));
        }
    }
}
