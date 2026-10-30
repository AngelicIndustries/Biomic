/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the Terra API module.
 */

package industries.angelic.biomic.internal.terra.noise;

/** Exact Terra 6.4.2 math operations required by the imported noise samplers. */
public final class MathUtil {

    private MathUtil() {}

    /** Terra 6.4.2 uses the interpolation factor as the first argument. */
    public static double lerp(double t, double v0, double v1) {
        return v0 + t * (v1 - v0);
    }

    public static double interpHermite(double t) {
        return t * t * (3.0D - 2.0D * t);
    }

    public static double interpQuintic(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    public static long murmur64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }
}
