package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.*;

import org.junit.Test;

public class IslandKeelTest {

    private static final long SEED = 0x5EEDL;

    private static long column(int x, int z, double inside, boolean columnar) {
        double spire = IslandKeel.spire(SEED, x, z, IslandKeel.ARCHIPELAGO, (cx, cz, px, pz) -> 40.0D);
        return IslandKeel.column(SEED, x, z, inside, 200.0D, IslandKeel.ARCHIPELAGO, 64.0D, spire, columnar);
    }

    @Test
    public void outsideTheCoastIsNoIsland() {
        for (int x = 0; x < 64; x++) assertEquals(IslandKeel.NONE, column(x, 7, -10.0D, false));
    }

    @Test
    public void columnsArePureFunctionsOfPosition() {
        for (int x = -40; x < 40; x += 3) for (int z = -40; z < 40; z += 5) {
            assertEquals(column(x, z, 12.0D, true), column(x, z, 12.0D, true));
            assertEquals(column(x, z, 12.0D, false), column(x, z, 12.0D, false));
        }
    }

    @Test
    public void keelHangsAtLeastTwoBelowTheDeckAndNoDeeperThanItsBudget() {
        for (int x = -50; x < 50; x++)
            for (int z = -50; z < 50; z += 7) for (boolean columnar : new boolean[] { true, false }) {
                long column = column(x, z, 3.0D + Math.abs(x) * 0.6D, columnar);
                if (column == IslandKeel.NONE) continue;
                int top = IslandKeel.top(column), bottom = IslandKeel.bottom(column);
                assertTrue(top - bottom >= 2);
                assertTrue("x=" + x + " z=" + z, 200 - bottom <= 64 + 3 + 0.36D * 64 + 2);
            }
    }

    @Test
    public void deeperInsideTheCoast() {
        double shallow = 0, deep = 0;
        for (int x = 0; x < 200; x++) {
            shallow += 200 - IslandKeel
                .bottom(IslandKeel.column(SEED, x, 3, 4.0D, 200.0D, IslandKeel.ARCHIPELAGO, 64.0D, 0.0D, false));
            deep += 200 - IslandKeel
                .bottom(IslandKeel.column(SEED, x, 3, 30.0D, 200.0D, IslandKeel.ARCHIPELAGO, 64.0D, 0.0D, false));
        }
        assertTrue(deep > shallow * 3);
    }

    @Test
    public void theDeckIsSmoothAcrossNeighbours() {
        // Including across chunk borders: columns only see their own position.
        for (int x = -33; x < 33; x++) for (int z = -33; z < 33; z += 4) {
            long here = column(x, z, 20.0D + 0.2D * x, false), next = column(x + 1, z, 20.0D + 0.2D * (x + 1), false);
            assertTrue(Math.abs(IslandKeel.top(here) - IslandKeel.top(next)) <= 2);
        }
    }

    @Test
    public void spiresHangOnlyFromSeedsWellInsideTheCoast() {
        for (int x = 0; x < 120; x += 3) for (int z = 0; z < 120; z += 3)
            assertEquals(0.0D, IslandKeel.spire(SEED, x, z, IslandKeel.ARCHIPELAGO, (cx, cz, px, pz) -> 5.0D), 0.0D);
        double deepest = 0;
        for (int x = 0; x < 120; x += 3) for (int z = 0; z < 120; z += 3) deepest = Math
            .max(deepest, IslandKeel.spire(SEED, x, z, IslandKeel.ARCHIPELAGO, (cx, cz, px, pz) -> 30.0D));
        assertTrue(deepest > 20.0D);
        assertEquals(0.0D, IslandKeel.spire(SEED, 5, 5, IslandKeel.ISLET, (cx, cz, px, pz) -> 30.0D), 0.0D);
    }
}
