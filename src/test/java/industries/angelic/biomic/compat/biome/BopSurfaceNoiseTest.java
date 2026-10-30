package industries.angelic.biomic.compat.biome;

import static org.junit.Assert.*;

import java.util.Random;

import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

import org.junit.Test;

public class BopSurfaceNoiseTest {

    @Test
    public void continuousSamplesMatchNativeArrayWithoutChunkTranspose() {
        for (long seed : new long[] { 42, -901, Long.MAX_VALUE }) {
            var sampled = new BopSurfaceNoise(seed);
            var random = new Random(seed);
            new NoiseGeneratorOctaves(random, 16);
            new NoiseGeneratorOctaves(random, 16);
            new NoiseGeneratorOctaves(random, 8);
            var nativeNoise = new NoiseGeneratorPerlin(random, 4);
            for (int[] origin : new int[][] { { -17, -33 }, { 0, 0 }, { 30000001, -30000007 } }) {
                double[] expected = nativeNoise.func_151599_a(null, origin[0], origin[1], 33, 33, .0625, .0625, 1);
                for (int z = 0; z < 33; z++) for (int x = 0; x < 33; x++)
                    assertEquals(expected[x + z * 33], sampled.sample(origin[0] + x, origin[1] + z), 0);
                for (int x = 32; x >= 0; x--) for (int z = 32; z >= 0; z--)
                    assertEquals(expected[x + z * 33], sampled.sample(origin[0] + x, origin[1] + z), 0);
            }
        }
    }

    @Test
    public void patchThresholdAndFallbackDoNotLeakPreviousColumnState() {
        for (int pass = 0; pass < 4; pass++)
            for (double value : new double[] { 5, -5, 1.75, Math.nextUp(1.75), -.95, -.96, 0 }) {
                assertEquals(
                    value > 1.75 ? "minecraft:dirt[legacy_meta=1]" : "minecraft:grass_block[snowy=false]",
                    BopNativeBiomes.soil(BopNativeBiomes.DENSE_FOREST, 0, value));
                assertEquals("minecraft:dirt", BopNativeBiomes.soil(BopNativeBiomes.DENSE_FOREST, 1, value));
                assertEquals(
                    value > 1.75 ? "minecraft:stone" : "minecraft:grass_block[snowy=false]",
                    BopNativeBiomes.soil(BopNativeBiomes.SHIELD, 0, value));
                assertEquals(
                    value > 1.75 ? "minecraft:stone" : "minecraft:dirt",
                    BopNativeBiomes.soil(BopNativeBiomes.SHIELD, 1, value));
                assertEquals(
                    "minecraft:grass_block[snowy=false]",
                    BopNativeBiomes.soil(BopNativeBiomes.GROVE, 0, value));
            }
        assertThrows(IllegalArgumentException.class, () -> BopNativeBiomes.soil(BopNativeBiomes.SHIELD, 0, Double.NaN));
        assertThrows(
            IllegalArgumentException.class,
            () -> BopNativeBiomes.soil(BopNativeBiomes.SHIELD, 0, Double.POSITIVE_INFINITY));
    }
}
