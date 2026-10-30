package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;

/**
 * Biomic-owned broad habitat mosaic hosted by the licensed Aeropelago density field.
 *
 * <p>
 * A {@code null} result deliberately preserves an open Aeropelago meadow. Non-null results are real Origen profile
 * identities; callers use the same value for vertical-biome storage, surface palettes, decoration and downstream
 * spawning. The scales are wider than a normal chunk so one floating landmass reads as a few coherent habitats
 * rather than biome confetti.
 * </p>
 */
final class AeropelagoBiomeSampler {

    private static final long MEADOW_SALT = 0x6A09E667F3BCC909L;
    private static final long TEMPERATURE_SALT = 0xBB67AE8584CAA73BL;
    private static final long HUMIDITY_SALT = 0x3C6EF372FE94F82BL;
    private static final double MEADOW_THRESHOLD = 0.16D;

    private final long seed;

    AeropelagoBiomeSampler(long seed) {
        this.seed = seed;
    }

    OrigenBiomeProfile sample(long clusterHash, double blockX, double blockZ) {
        long localSeed = seed ^ clusterHash;
        double meadow = ValueNoise2D.fractal(localSeed ^ MEADOW_SALT, blockX, blockZ, 224.0D, 2, 0.52D);
        if (Math.abs(meadow) < MEADOW_THRESHOLD) return null;

        double temperature = ValueNoise2D.fractal(localSeed ^ TEMPERATURE_SALT, blockX, blockZ, 320.0D, 2, 0.5D);
        double humidity = ValueNoise2D.fractal(localSeed ^ HUMIDITY_SALT, blockX, blockZ, 272.0D, 2, 0.5D);
        if (temperature < -0.28D) {
            return OrigenBiomeProfile.TALL_TIMBERLAND;
        }
        if (humidity > 0.20D) return OrigenBiomeProfile.LUSH_LOOPS;
        if (temperature > 0.22D) return OrigenBiomeProfile.SAKURA_STREAMS;
        return OrigenBiomeProfile.FOLIAGE_FORTRESS_INNER;
    }
}
