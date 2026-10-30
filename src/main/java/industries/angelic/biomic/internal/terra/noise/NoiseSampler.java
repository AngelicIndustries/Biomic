/*
 * Copyright (c) 2020-2023 Polyhedral Development
 * The Terra API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the Terra API module.
 */

package industries.angelic.biomic.internal.terra.noise;

/**
 * Minimal, source-compatible subset of Terra 6.4.2's noise sampler contract.
 *
 * <p>
 * Biomic keeps this interface private so the Origen runtime does not expose or require Terra's
 * API. Vector convenience overloads were intentionally left out because the imported sampler
 * graph does not use them.
 * </p>
 */
public interface NoiseSampler {

    static NoiseSampler zero() {
        return new NoiseSampler() {

            @Override
            public double noise(long seed, double x, double y) {
                return 0;
            }

            @Override
            public double noise(long seed, double x, double y, double z) {
                return 0;
            }
        };
    }

    double noise(long seed, double x, double y);

    double noise(long seed, double x, double y, double z);

    /** Exact column sampling for regularly spaced authored Y coordinates. */
    default void fillNoiseVerticalStrided(long seed, double x, int minimumY, int maximumYExclusive, int yStep,
        double yScale, double z, double[] samples, int offset) {
        int sample = offset;
        for (int y = minimumY; y < maximumYExclusive; y += yStep) {
            samples[sample++] = noise(seed, x, y * yScale, z);
        }
    }

    /**
     * Samples selected rows of a vertical column at explicit Y coordinates. Unselected destination cells are left
     * untouched, preserving conditional-expression short circuiting. Explicit coordinates let callers retain their
     * original floating-point evaluation order instead of folding scale factors together.
     */
    default void fillNoiseVerticalMasked(long seed, double x, double z, double[] yCoordinates, int yOffset,
        boolean[] selected, int selectedOffset, int count, double[] samples, int offset) {
        for (int row = 0; row < count; row++) {
            if (selected[selectedOffset + row]) samples[offset + row] = noise(seed, x, yCoordinates[yOffset + row], z);
        }
    }
}
