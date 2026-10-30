/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package industries.angelic.biomic.internal.terra.noise.samplers.noise.random;

import industries.angelic.biomic.internal.terra.noise.samplers.noise.NoiseFunction;

/** Exact Terra 6.4.2 normally-distributed sampler used by Origen palettes. */
public final class GaussianNoiseSampler extends NoiseFunction {

    private final WhiteNoiseSampler whiteNoiseSampler = new WhiteNoiseSampler();

    @Override
    public double getNoiseRaw(long seed, double x, double y) {
        double first;
        double second;
        double square;
        do {
            first = whiteNoiseSampler.noise(seed++, x, y);
            second = whiteNoiseSampler.noise(seed++, x, y);
            square = first * first + second * second;
        } while (square >= 1.0D || square == 0.0D);
        return first * StrictMath.sqrt(-2.0D * StrictMath.log(square) / square);
    }

    @Override
    public double getNoiseRaw(long seed, double x, double y, double z) {
        double first;
        double second;
        double square;
        do {
            first = whiteNoiseSampler.noise(seed++, x, y, z);
            second = whiteNoiseSampler.noise(seed++, x, y, z);
            square = first * first + second * second;
        } while (square >= 1.0D || square == 0.0D);
        return first * StrictMath.sqrt(-2.0D * StrictMath.log(square) / square);
    }
}
