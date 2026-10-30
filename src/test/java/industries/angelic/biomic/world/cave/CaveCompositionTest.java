package industries.angelic.biomic.world.cave;

import static industries.angelic.biomic.world.cave.CaveComposition.Material.*;
import static org.junit.Assert.*;

import java.util.BitSet;

import org.junit.Test;

public class CaveCompositionTest {

    private static final CaveComposition.Limits LIMITS = new CaveComposition.Limits(4, 120, 8, 3, 3);

    private static int index(int x, int y, int z) {
        return (x * 16 + z) * 256 + y;
    }

    @Test
    public void roofFluidAndProtectedLandformsSurviveEvenWithAnEverywhereCaveField() {
        CaveComposition.Host host = (x, y, z) -> {
            if (y < 1 || y > 100) return AIR;
            if (y == 1) return PROTECTED;
            if (x == 7 && z == 7 && y == 50) return FLUID;
            if (x >= 12 && y >= 40 && y < 60) return AIR; // exposed cliff face
            if (x == 3 && z == 3 && y == 30) return PROTECTED;
            return ROCK;
        };
        BitSet result = CaveComposition.plan(0, 0, host, (x, y, z) -> 1, LIMITS);
        assertTrue(result.cardinality() > 0);
        assertFalse(result.get(index(5, 93, 5))); // eight-block roof
        assertFalse(result.get(index(7, 49, 7))); // water immediately above
        assertFalse(result.get(index(7, 43, 7))); // water inside, not at envelope endpoint
        assertFalse(result.get(index(11, 48, 5))); // cliff side
        assertFalse(result.get(index(3, 30, 3))); // protected material
        assertTrue(result.get(index(5, 80, 5))); // genuine thick mountain interior
    }

    @Test
    public void haloCrossesChunkBordersAndPlansDoNotDependOnTraversal() {
        CaveComposition.Host host = (x, y, z) -> x == 16 ? FLUID : y > 0 && y < 100 ? ROCK : AIR;
        var first = CaveComposition.plan(0, -1, host, (x, y, z) -> 1, LIMITS);
        var neighbor = CaveComposition.plan(1, -1, host, (x, y, z) -> 1, LIMITS);
        assertFalse(first.get(index(15, 50, 8)));
        assertFalse(first.get(index(13, 50, 8)));
        assertTrue(first.get(index(12, 50, 8)));
        assertFalse(neighbor.get(index(0, 50, 8)));
        assertFalse(neighbor.get(index(3, 50, 8)));
        assertTrue(neighbor.get(index(4, 50, 8)));
        assertEquals(first, CaveComposition.plan(0, -1, host, (x, y, z) -> 1, LIMITS));
    }

    @Test
    public void blendsDoNotStackTwoIndependentExcavations() {
        assertEquals(-.5, CaveComposition.blend(1, -1, .75), 0);
        assertEquals(1, CaveComposition.blend(1, -1, 0), 0);
        assertEquals(-1, CaveComposition.blend(1, -1, 1), 0);
        assertThrows(IllegalArgumentException.class, () -> CaveComposition.blend(1, Double.NaN, .5));
        assertThrows(IllegalArgumentException.class, () -> CaveComposition.blend(1, 1, 2));
    }

    @Test
    public void existingCaveOpeningsCanConnectButExteriorArchesCannot() {
        CaveComposition.Host underground = (x, y, z) -> y < 1 || y > 100 ? AIR : y == 50 ? CAVITY : ROCK;
        CaveComposition.Host arch = (x, y, z) -> y < 1 || y > 100 || y == 50 ? AIR : ROCK;
        BitSet cave = CaveComposition.plan(0, 0, underground, (x, y, z) -> 1, LIMITS);
        BitSet outside = CaveComposition.plan(0, 0, arch, (x, y, z) -> 1, LIMITS);
        assertTrue(cave.get(index(8, 49, 8)));
        assertFalse(cave.get(index(8, 50, 8))); // already air: no material write
        assertFalse(outside.get(index(8, 49, 8)));
    }
}
