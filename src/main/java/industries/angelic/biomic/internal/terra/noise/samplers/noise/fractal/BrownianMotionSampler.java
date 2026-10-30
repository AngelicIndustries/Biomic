/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package industries.angelic.biomic.internal.terra.noise.samplers.noise.fractal;

import industries.angelic.biomic.internal.terra.noise.MathUtil;
import industries.angelic.biomic.internal.terra.noise.NoiseSampler;

public class BrownianMotionSampler extends FractalNoiseFunction {

    public BrownianMotionSampler(NoiseSampler input) {
        super(input);
    }

    @Override
    public double getNoiseRaw(long seed, double x, double y) {
        double sum = 0;
        double amp = fractalBounding;

        for (int i = 0; i < octaves; i++) {
            double noise = input.noise(seed++, x, y);
            sum += noise * amp;
            amp *= MathUtil.lerp(1.0, Math.min(noise + 1, 2) * 0.5, weightedStrength);

            x *= lacunarity;
            y *= lacunarity;
            amp *= gain;
        }

        return sum;
    }

    @Override
    public double getNoiseRaw(long seed, double x, double y, double z) {
        double sum = 0;
        double amp = fractalBounding;

        for (int i = 0; i < octaves; i++) {
            double noise = input.noise(seed++, x, y, z);
            sum += noise * amp;
            amp *= MathUtil.lerp(1.0, (noise + 1) * 0.5, weightedStrength);

            x *= lacunarity;
            y *= lacunarity;
            z *= lacunarity;
            amp *= gain;
        }

        return sum;
    }
}
