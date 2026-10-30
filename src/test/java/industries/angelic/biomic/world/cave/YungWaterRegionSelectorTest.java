package industries.angelic.biomic.world.cave;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import industries.angelic.biomic.world.cave.YungWaterRegionSelector.Kind;
import industries.angelic.biomic.world.cave.YungWaterRegionSelector.Settings;

public class YungWaterRegionSelectorTest {

    @Test
    public void exactJavaXorSeedAndXOuterZInnerRngSequenceIncludeNullBuffer() {
        long seed = 0x123456789ABCDEFL;
        int cx = -17, cz = 9;
        float threshold = YungWaterRegionSelector.threshold(40);
        List<String> calls = new ArrayList<>();
        var selector = new YungWaterRegionSelector(seed, Settings.sourceDefaults(), (x, z) -> {
            calls.add((int) x + "," + (int) z);
            return threshold + .045f;
        });
        var result = selector.chunk(cx, cz);
        Random source = new Random(seed ^ cx ^ cz);
        int buffers = 0, lava = 0;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            float offset = source.nextFloat() * .01f + .04f;
            Kind expected = threshold + .045f < threshold + offset ? Kind.NONE : Kind.LAVA;
            assertEquals(expected, result.at(x, z));
            assertEquals((cx * 16 + x) + "," + (cz * 16 + z), calls.get(x * 16 + z));
            if (expected == Kind.NONE) buffers++;
            else lava++;
        }
        assertTrue(buffers > 0);
        assertTrue(lava > 0);
        assertEquals(256, calls.size());
        assertThrows(IllegalArgumentException.class, () -> result.at(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> result.at(0, 16));
    }
}
