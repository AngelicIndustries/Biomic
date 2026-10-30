package industries.angelic.biomic.compat.biome;

import java.util.Random;

import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

/** Native 1.7 surface-noise construction, without a World, provider, chunks or Forge generation hooks. */
public final class BopSurfaceNoise {

    private final NoiseGeneratorPerlin noise;

    public BopSurfaceNoise(long seed) {
        Random random = new Random(seed);
        // Preserve ChunkProviderGenerate's constructor RNG consumption before its surface field.
        new NoiseGeneratorOctaves(random, 16);
        new NoiseGeneratorOctaves(random, 16);
        new NoiseGeneratorOctaves(random, 8);
        noise = new NoiseGeneratorPerlin(random, 4);
    }

    public double sample(int x, int z) {
        // Use absolute coordinates. The old provider's transposed chunk-array lookup introduced
        // local seams; do not reproduce those by swapping coordinates inside every 16-block square.
        // The array API includes octave offsets/amplitudes absent from func_151601_a.
        return noise.func_151599_a(null, x, z, 1, 1, .0625, .0625, 1)[0];
    }
}
