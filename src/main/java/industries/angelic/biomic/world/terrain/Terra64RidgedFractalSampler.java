package industries.angelic.biomic.world.terrain;

import com.dfsek.seismic.algorithms.sampler.noise.fractal.RidgedFractalSampler;
import com.dfsek.seismic.type.sampler.Sampler;

/** Preserves the argument ordering used by Terra 6.4.2's fractal amplitude interpolation. */
final class Terra64RidgedFractalSampler extends RidgedFractalSampler {

    Terra64RidgedFractalSampler(long salt, Sampler input, double gain, double lacunarity, double weightedStrength,
        int octaves) {
        super(salt, input, gain, lacunarity, weightedStrength, octaves);
    }

    @Override
    public double getNoiseRaw(long seed, double x, double y) {
        double sum = 0.0D;
        double amplitude = fractalBounding;
        for (int octave = 0; octave < octaves; octave++) {
            double noise = Math.abs(input.getSample(seed++, x, y));
            sum += (1.0D - 2.0D * noise) * amplitude;
            amplitude *= terra64Lerp(1.0D, 1.0D - noise, weightedStrength);
            x *= lacunarity;
            y *= lacunarity;
            amplitude *= gain;
        }
        return sum;
    }

    @Override
    public double getNoiseRaw(long seed, double x, double y, double z) {
        double sum = 0.0D;
        double amplitude = fractalBounding;
        for (int octave = 0; octave < octaves; octave++) {
            double noise = Math.abs(input.getSample(seed++, x, y, z));
            sum += (1.0D - 2.0D * noise) * amplitude;
            amplitude *= terra64Lerp(1.0D, 1.0D - noise, weightedStrength);
            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
            amplitude *= gain;
        }
        return sum;
    }

    private static double terra64Lerp(double progress, double first, double second) {
        return first + progress * (second - first);
    }
}
