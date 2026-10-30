package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.world.WorldgenContext;

/** Small deterministic value-noise primitive used by Biomic's authored field samplers. */
final class ValueNoise2D {

    private static final long GRID_X_SALT = 0x632BE59BD9B4E019L;
    private static final long GRID_Z_SALT = 0x9E3779B97F4A7C15L;
    private static final long OCTAVE_SALT = 0xD1B54A32D192ED03L;

    private ValueNoise2D() {}

    static double fractal(long seed, double blockX, double blockZ, double largestScale, int octaves,
        double persistence) {
        double value = 0.0D;
        double amplitude = 1.0D;
        double amplitudeSum = 0.0D;
        double scale = largestScale;
        for (int octave = 0; octave < octaves; octave++) {
            value += sample(WorldgenContext.mix64(seed + OCTAVE_SALT * octave), blockX, blockZ, scale) * amplitude;
            amplitudeSum += amplitude;
            amplitude *= persistence;
            scale *= 0.5D;
        }
        return value / amplitudeSum;
    }

    private static double sample(long seed, double blockX, double blockZ, double scale) {
        double scaledX = blockX / scale;
        double scaledZ = blockZ / scale;
        long gridX = floor(scaledX);
        long gridZ = floor(scaledZ);
        double localX = fade(scaledX - gridX);
        double localZ = fade(scaledZ - gridZ);

        double north = lerp(gridValue(seed, gridX, gridZ), gridValue(seed, gridX + 1L, gridZ), localX);
        double south = lerp(gridValue(seed, gridX, gridZ + 1L), gridValue(seed, gridX + 1L, gridZ + 1L), localX);
        return lerp(north, south, localZ);
    }

    private static double gridValue(long seed, long gridX, long gridZ) {
        long mixed = WorldgenContext.mix64(seed ^ gridX * GRID_X_SALT ^ gridZ * GRID_Z_SALT);
        return ((mixed >>> 11) * 0x1.0p-53) * 2.0D - 1.0D;
    }

    private static long floor(double value) {
        long truncated = (long) value;
        return value < truncated ? truncated - 1L : truncated;
    }

    private static double fade(double value) {
        return value * value * value * (value * (value * 6.0D - 15.0D) + 10.0D);
    }

    private static double lerp(double first, double second, double progress) {
        return first + (second - first) * progress;
    }
}
