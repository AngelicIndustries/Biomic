package industries.angelic.biomic.testterra.raster.config.noise;

import industries.angelic.biomic.internal.terra.noise.NoiseSampler;

/** Test properties only. Original interpolation code reads the exact authored settings through this carrier. */
public record BiomeNoiseProperties(NoiseSampler base, NoiseSampler elevation, NoiseSampler carving, int blendDistance,
    int blendStep, double blendWeight, double elevationWeight, Holder noiseHolder) {

    public static final class Holder {

        public double getNoise(NoiseSampler sampler, int x, int y, int z, long seed) {
            return sampler.noise(seed, x, y, z);
        }
    }
}
