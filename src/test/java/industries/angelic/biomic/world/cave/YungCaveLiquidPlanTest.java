package industries.angelic.biomic.world.cave;

import static org.junit.Assert.*;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import industries.angelic.biomic.world.cave.YungWaterRegionSelector.Kind;
import industries.angelic.biomic.world.cave.YungWaterRegionSelector.Settings;

/** Whole-clearance mask fixtures, not a natural fluid or scheduled-flow runtime proof. */
public class YungCaveLiquidPlanTest {

    private record P(int x, int y, int z) {}

    private static int index(int x, int y, int z) {
        return (x * 16 + z) * 256 + y;
    }

    private static BitSet mask() {
        var m = new BitSet(65536);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 9; y <= 11; y++) m.set(index(x, y, z));
        return m;
    }

    private static YungWaterRegionSelector selector(Kind kind) {
        return new YungWaterRegionSelector(
            42,
            new Settings(40, .004f),
            (x, z) -> kind == Kind.WATER ? -1 : kind == Kind.LAVA ? 1 : YungWaterRegionSelector.threshold(40));
    }

    private static YungCaveLiquidPlan.Excavations excavations(Set<P> positions) {
        return (x, y, z) -> positions.contains(new P(x, y, z));
    }

    private static Set<P> positions(int cx, int cz, BitSet m) {
        var result = new HashSet<P>();
        for (int i = m.nextSetBit(0); i >= 0; i = m.nextSetBit(i + 1))
            result.add(new P(cx * 16 + i / 4096, i % 256, cz * 16 + (i / 256) % 16));
        return result;
    }
}
