package industries.angelic.biomic.world.cave;

import java.util.Random;

import industries.angelic.biomic.internal.ultimatecaves.FastNoise;
import industries.angelic.biomic.internal.ultimatecaves.WorleyUtil;

/**
 * UCM's warped F1/F3 field without World, chunk loading or global hooks.
 * Its headroom smoothing is evaluated on a global lattice with neighbor samples,
 * fixing the source's missing smoothing contribution at each chunk's positive edge.
 * Surface/fluids/feature protection belong to the cave-composition stage.
 */
public final class UltimateWorleyField {

    private final WorleyUtil cellular = new WorleyUtil();
    private final FastNoise warp;
    private final Settings settings;
    private final boolean continuous;

    public record Settings(float cutoff, float warpAmplitude, float verticalCompression, float horizontalCompression,
        int minY, int maxY) {

        public Settings {
            if (!Float.isFinite(cutoff) || cutoff < -1
                || cutoff > 1
                || !Float.isFinite(warpAmplitude)
                || warpAmplitude < 0
                || warpAmplitude > 128
                || !Float.isFinite(verticalCompression)
                || verticalCompression <= 0
                || verticalCompression > 16
                || !Float.isFinite(horizontalCompression)
                || horizontalCompression <= 0
                || horizontalCompression > 16
                || minY < 1
                || maxY > 256
                || minY >= maxY) throw new IllegalArgumentException("Unsafe UltimateCaves field settings");
        }

        public static Settings sourceDefaults() {
            return new Settings(-.14f, 8f, 2f, 1f, 1, 128);
        }
    }

    /** Legacy source extrapolation is retained for v1 saves; v2 interpolates the Y lattice normally. */
    public UltimateWorleyField(long seed, Settings settings, boolean continuous) {
        if (settings == null) throw new IllegalArgumentException("Missing cave field settings");
        this.settings = settings;
        this.continuous = continuous;
        int noiseSeed = new Random(seed).nextInt();
        cellular.SetSeed(noiseSeed);
        cellular.SetFrequency(.016f);
        warp = new FastNoise(noiseSeed);
        warp.SetFrequency(.05f);
    }

    /** Unsmoothed source sample; x/z are global 4-block lattice indices, y a 2-block index. */
    public float rawGrid(int gx, int gy, int gz) {
        int x = gx * 4, z = gz * 4;
        float y = gy * 2;
        if (gy < 0 || gy > 128 || y > settings.maxY || y < settings.minY) return -1.1f;
        float amplitude = (float) (settings.warpAmplitude * ((128 - gy) / (128 * .85)));
        float dx = warp.GetNoise(x, z) * amplitude;
        float dy = warp.GetNoise(x, z + 67.0f) * amplitude;
        float dz = warp.GetNoise(x, z + 149.0f) * amplitude;
        return cellular.SingleCellular3Edge(
            x * settings.horizontalCompression + dx,
            y * settings.verticalCompression + dy,
            z * settings.horizontalCompression + dz);
    }

    private float smooth(float value, float below, float twoBelow, float south, float east) {
        if (below > settings.cutoff && below > value) value = below * .8f + value * .2f;
        if (twoBelow > settings.cutoff && twoBelow > value) value = twoBelow * .35f + value * .65f;
        if (south > settings.cutoff) value = south * .2f + value * .8f;
        if (east > settings.cutoff) value = east * .2f + value * .8f;
        return value;
    }

    /** One bounded lattice per chunk, rather than repeating cellular samples for every voxel. */
    public float[] chunk(int chunkX, int chunkZ) {
        int height = Math.min(129, settings.maxY / 2 + 2);
        // Neighbor smoothing repeatedly requested the same expensive warped cellular nodes.
        // Memoize the exact raw samples (including the two lower Y rows) before smoothing;
        // do not reassociate the source's floating-point operations.
        float[][][] raw = new float[6][height + 2][6];
        for (int x = 0; x < 6; x++) for (int z = 0; z < 6; z++)
            for (int y = -2; y < height; y++) raw[x][y + 2][z] = rawGrid(chunkX * 4 + x, y, chunkZ * 4 + z);
        float[][][] samples = new float[5][height][5];
        for (int x = 0; x < 5; x++)
            for (int z = 0; z < 5; z++) for (int y = 0; y < height; y++) samples[x][y][z] = smooth(
                raw[x][y + 2][z],
                raw[x][y + 1][z],
                raw[x][y][z],
                raw[x][y + 2][z + 1],
                raw[x + 1][y + 2][z]);
        Grid lookup = (x, y, z) -> samples[x - chunkX * 4][y][z - chunkZ * 4];
        float[] result = new float[65536];
        java.util.Arrays.fill(result, -1f);
        for (int x = 0; x < 16; x++)
            for (int z = 0; z < 16; z++) for (int y = settings.minY; y <= Math.min(255, settings.maxY); y++)
                result[(x * 16 + z) * 256 + y] = density(chunkX * 16 + x, y, chunkZ * 16 + z, lookup);
        return result;
    }

    @FunctionalInterface
    private interface Grid {

        float at(int x, int y, int z);
    }

    private float density(int x, int y, int z, Grid samples) {
        if (y < settings.minY || y > settings.maxY || y >= 256) return -1;
        int gx = Math.floorDiv(x, 4), gz = Math.floorDiv(z, 4), gy = Math.floorDiv(y, 2);
        float north0 = samples.at(gx, gy, gz), south0 = samples.at(gx, gy, gz + 1);
        float north1 = samples.at(gx + 1, gy, gz), south1 = samples.at(gx + 1, gy, gz + 1);
        float fy = continuous ? (y & 1) * .5f : ((y & 1) == 0 ? -.5f : 0f);
        if (fy != 0) {
            north0 += (samples.at(gx, gy + 1, gz) - north0) * fy;
            south0 += (samples.at(gx, gy + 1, gz + 1) - south0) * fy;
            north1 += (samples.at(gx + 1, gy + 1, gz) - north1) * fy;
            south1 += (samples.at(gx + 1, gy + 1, gz + 1) - south1) * fy;
        }
        float stepNorth = (north1 - north0) * .25f, stepSouth = (south1 - south0) * .25f;
        for (int i = 0; i < Math.floorMod(x, 4); i++) {
            north0 += stepNorth;
            south0 += stepSouth;
        }
        float stepZ = (south0 - north0) * .25f;
        for (int i = 0; i < Math.floorMod(z, 4); i++) north0 += stepZ;
        return north0 - settings.cutoff;
    }
}
