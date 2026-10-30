/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package industries.angelic.biomic.internal.terra.noise.samplers.noise.value;

import industries.angelic.biomic.internal.terra.noise.samplers.noise.NoiseFunction;

abstract class ValueStyleNoise extends NoiseFunction {

    static double valCoord(int seed, int xPrimed, int yPrimed) {
        int hash = hash(seed, xPrimed, yPrimed);
        hash *= hash;
        hash ^= hash << 19;
        return hash * (1.0D / 2147483648.0D);
    }

    static double valCoord(int seed, int xPrimed, int yPrimed, int zPrimed) {
        int hash = hash(seed, xPrimed, yPrimed, zPrimed);
        hash *= hash;
        hash ^= hash << 19;
        return hash * (1.0D / 2147483648.0D);
    }
}
