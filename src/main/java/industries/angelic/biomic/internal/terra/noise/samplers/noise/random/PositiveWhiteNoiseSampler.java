/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package industries.angelic.biomic.internal.terra.noise.samplers.noise.random;

/**
 * NoiseSampler implementation to produce random, uniformly distributed (white) noise.
 */
public class PositiveWhiteNoiseSampler extends WhiteNoiseSampler {

    // Bits that when applied to the exponent/sign section of a double, produce a positive number with a power of 1.

    @Override
    public double getNoiseRaw(long seed, double x, double y) {
        return (getNoiseUnmapped(seed, x, y) - 1);
    }

    @Override
    public double getNoiseRaw(long seed, double x, double y, double z) {
        return (getNoiseUnmapped(seed, x, y, z) - 1);
    }
}
