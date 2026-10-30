package industries.angelic.biomic.world.cave;

import static org.junit.Assert.*;

import java.util.Random;

import net.minecraft.world.gen.NoiseGeneratorOctaves;

import org.junit.Test;

import industries.angelic.biomic.internal.ultimatecaves.NoiseCaveGenerator;

public class UltimateGregFieldTest {

    @Test
    public void seedPreludeMatchesActualVanillaOctaveConstructors() {
        for (long seed : new long[] { 42, -7, Long.MAX_VALUE }) {
            Random actual = new Random(seed);
            for (int count : new int[] { 16, 16, 8, 16 }) new NoiseGeneratorOctaves(actual, count);
            Random portable = UltimateGregField.sourceRandom(seed);
            for (int i = 0; i < 32; i++) assertEquals(actual.nextLong(), portable.nextLong());
        }
    }

    @Test
    public void densityPreservesSourceAxisOrderAndBothHostDensityBranches() {
        int samples = 0;
        boolean air = false, solid = false;
        for (long seed : new long[] { 42, -913 }) {
            UltimateGregField field = new UltimateGregField(seed);
            Random actual = new Random(seed);
            for (int count : new int[] { 16, 16, 8, 16 }) new NoiseGeneratorOctaves(actual, count);
            NoiseCaveGenerator reference = new NoiseCaveGenerator(actual);
            for (int x : new int[] { -177, 0, 531 }) for (int z : new int[] { -73, 0, 211 })
                for (int y = 4; y <= 120; y += 4) for (double host : new double[] { 128, 256 }) {
                    double expected = reference.sample(host, y, z, x), value = field.density(host, x, y, z);
                    assertTrue(Double.isFinite(value));
                    assertEquals(expected, value, 0);
                    air |= value < 0;
                    solid |= value > 0;
                    samples++;
                }
        }
        assertEquals(1080, samples);
        assertTrue(air);
        assertTrue(solid);
        assertThrows(IllegalArgumentException.class, () -> new UltimateGregField(42).density(Double.NaN, 0, 40, 0));
    }
}
