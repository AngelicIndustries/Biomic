package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;

import org.junit.Test;

/**
 * A lowland river where it once cut a 16-block wall through tundra hills (seed 8685361214559040823, near -183 50):
 * its banks must rise without walls, and its bed keep its depth.
 */
public class LowlandRiverValleyTest {

    private static final long SEED = 8685361214559040823L;

    @Test
    public void banksRiseWithoutWallsAndTheBedKeepsItsDepth() throws Exception {
        Constructor<OrigenDensityField> constructor = OrigenDensityField.class
            .getDeclaredConstructor(long.class, OrigenTerrainRuntime.class);
        constructor.setAccessible(true);
        OrigenDensityField field = constructor.newInstance(SEED, OrigenTerrainRuntime.instance());
        for (int x : new int[] { -200, -183, -170 }) {
            int previous = surface(field, x, 30), deepest = previous;
            for (int z = 31; z <= 90; z++) {
                int height = surface(field, x, z);
                assertTrue(
                    "step of " + Math.abs(height - previous) + " at " + x + " " + z,
                    Math.abs(height - previous) <= 3);
                previous = height;
                deepest = Math.min(deepest, height);
            }
            assertTrue("river bed at x " + x + " only reaches " + deepest, deepest <= 50);
        }
    }

    /** The highest solid block of a column, in Origen's (upstream) heights; the water stands at 62. */
    private static int surface(OrigenDensityField field, int x, int z) {
        for (int y = 120; y >= 0; y--) if (field.sampleUpstreamInterpolated(x, y, z) > 0.0D) return y;
        return -1;
    }
}
