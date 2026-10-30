/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package industries.angelic.biomic.internal.terra.noise.normalizer;

import industries.angelic.biomic.internal.terra.noise.NoiseSampler;

public abstract class Normalizer implements NoiseSampler {

    private final NoiseSampler sampler;

    public Normalizer(NoiseSampler sampler) {
        this.sampler = sampler;
    }

    public abstract double normalize(double in);

    @Override
    public double noise(long seed, double x, double y) {
        return normalize(sampler.noise(seed, x, y));
    }

    @Override
    public double noise(long seed, double x, double y, double z) {
        return normalize(sampler.noise(seed, x, y, z));
    }
}
