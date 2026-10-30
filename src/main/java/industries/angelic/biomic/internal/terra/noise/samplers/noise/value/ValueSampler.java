/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package industries.angelic.biomic.internal.terra.noise.samplers.noise.value;

import industries.angelic.biomic.internal.terra.noise.MathUtil;

/** Exact Terra 6.4.2 value-noise sampler used by Origen palettes. */
public final class ValueSampler extends ValueStyleNoise {

    @Override
    public double getNoiseRaw(long seedValue, double x, double y) {
        int seed = (int) seedValue;
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        double xs = MathUtil.interpHermite(x - x0);
        double ys = MathUtil.interpHermite(y - y0);
        x0 *= PRIME_X;
        y0 *= PRIME_Y;
        int x1 = x0 + PRIME_X;
        int y1 = y0 + PRIME_Y;
        /*
         * Keep the argument order from Terra's shipped 6.4.2 addon bytecode. Its value sampler was compiled
         * against the factor-first MathUtil overload while still passing (first, second, factor). Origen's
         * terracotta bands were authored and rendered with that quirk, so correcting it here phase-shifts every
         * stratum even though the resulting interpolation looks unconventional.
         */
        double lower = MathUtil.lerp(valCoord(seed, x0, y0), valCoord(seed, x1, y0), xs);
        double upper = MathUtil.lerp(valCoord(seed, x0, y1), valCoord(seed, x1, y1), xs);
        return MathUtil.lerp(lower, upper, ys);
    }

    @Override
    public double getNoiseRaw(long seedValue, double x, double y, double z) {
        int seed = (int) seedValue;
        int x0 = (int) Math.floor(x);
        int y0 = (int) Math.floor(y);
        int z0 = (int) Math.floor(z);
        double xs = MathUtil.interpHermite(x - x0);
        double ys = MathUtil.interpHermite(y - y0);
        double zs = MathUtil.interpHermite(z - z0);
        x0 *= PRIME_X;
        y0 *= PRIME_Y;
        z0 *= PRIME_Z;
        int x1 = x0 + PRIME_X;
        int y1 = y0 + PRIME_Y;
        int z1 = z0 + PRIME_Z;
        double x00 = MathUtil.lerp(valCoord(seed, x0, y0, z0), valCoord(seed, x1, y0, z0), xs);
        double x10 = MathUtil.lerp(valCoord(seed, x0, y1, z0), valCoord(seed, x1, y1, z0), xs);
        double x01 = MathUtil.lerp(valCoord(seed, x0, y0, z1), valCoord(seed, x1, y0, z1), xs);
        double x11 = MathUtil.lerp(valCoord(seed, x0, y1, z1), valCoord(seed, x1, y1, z1), xs);
        return MathUtil.lerp(MathUtil.lerp(x00, x10, ys), MathUtil.lerp(x01, x11, ys), zs);
    }
}
