package industries.angelic.biomic.compat.biome;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Random;

import org.junit.Test;

public class BopFinalPondSchedulesTest {

    @Test
    public void finalWaterPondsRetainNativeDoubleNestedYDraw() {
        for (var profile : List.of(
            BopNativePond.Profile.WASTELAND_WATER,
            BopNativePond.Profile.MANGROVE_WATER,
            BopNativePond.Profile.OASIS_WATER)) for (int seed = 0; seed < 128; seed++) {
                var expected = new Random(seed);
                int y = expected.nextInt(expected.nextInt(240) + 8);
                assertEquals(y, BopNativePond.sourceY(new Random(seed), profile));
                assertTrue(y >= 0 && y <= 246);
            }
    }

    @Test
    public void poisonAndVolcanoPondsRetainNativeTripleNestedYDraw() {
        for (var profile : List.of(BopNativePond.Profile.WASTELAND_POISON, BopNativePond.Profile.VOLCANO_LAVA))
            for (int seed = 0; seed < 128; seed++) {
                var expected = new Random(seed);
                int y = expected.nextInt(expected.nextInt(expected.nextInt(112) + 8) + 8);
                assertEquals(y, BopNativePond.sourceY(new Random(seed), profile));
                assertTrue(y >= 0 && y <= 124);
            }
    }
}
