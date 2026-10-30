package industries.angelic.biomic.world.terrain;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import industries.angelic.biomic.world.WorldgenContext;

/** Deep-water carrier for the blended Hyacinth habitat, not a change to the Origen backup. */
final class HostedOceanDepth {

    private static final int SEA = 63, GRID = 16, SHORE = 80;
    private final long seed;
    private final BiPredicate<Integer, Integer> habitat;
    private final Map<Long, Double> shoreWeights = new LinkedHashMap<Long, Double>(1024, .75F, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Double> entry) {
            return size() > 8192;
        }
    };

    HostedOceanDepth(WorldgenContext context) {
        this(
            context.getSettings()
                .getWorldSeed(),
            new RegionalHabitatSampler(context));
    }

    private HostedOceanDepth(long seed, RegionalHabitatSampler sampler) {
        this(seed, (x, z) -> "regions_unexplored:hyacinth_deeps".equals(sampler.sample(x, z)));
    }

    HostedOceanDepth(long seed, BiPredicate<Integer, Integer> habitat) {
        this.seed = seed;
        this.habitat = habitat;
    }

    int loweredFloor(int x, int z, int oldFloor) {
        if (oldFloor <= 3 || oldFloor >= SEA || !habitat.test(x, z)) return oldFloor;
        int gx = Math.floorDiv(x, GRID), gz = Math.floorDiv(z, GRID);
        double fx = Math.floorMod(x, GRID) / (double) GRID, fz = Math.floorMod(z, GRID) / (double) GRID;
        double blend = lerp(
            lerp(weight(gx, gz), weight(gx + 1, gz), fx),
            lerp(weight(gx, gz + 1), weight(gx + 1, gz + 1), fx),
            fz);
        double relief = ValueNoise2D.fractal(seed ^ 0x68796163696e7468L, x, z, 96, 3, .5);
        int deepFloor = (int) Math.round(SEA - 36 - 8 * relief);
        return (int) Math.round(lerp(oldFloor, Math.min(oldFloor, deepFloor), blend));
    }

    private synchronized double weight(int gx, int gz) {
        long key = ((long) gx << 32) ^ (gz & 0xffffffffL);
        Double known = shoreWeights.get(key);
        if (known != null) return known;
        int x = gx * GRID, z = gz * GRID;
        double distance = SHORE;
        if (!habitat.test(x, z)) distance = 0;
        else for (int dz = -SHORE; dz <= SHORE; dz += GRID) for (int dx = -SHORE; dx <= SHORE; dx += GRID) {
            double d = Math.hypot(dx, dz);
            if (d < distance && !habitat.test(x + dx, z + dz)) distance = d;
        }
        // Leave a full lattice-cell shoreline collar; smoothly descend into the basin.
        double t = Math.max(0, Math.min(1, (distance - GRID) / (SHORE - GRID)));
        double value = t * t * (3 - 2 * t);
        shoreWeights.put(key, value);
        return value;
    }

    void apply(byte[] materials, int chunkX, int chunkZ) {
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int start = BiomicTerrainSampler.index(x, 0, z);
            // Do not lower land, emergent rock towers, islands or cave pools.
            if (materials[start + SEA] != BiomicTerrainSampler.WATER
                || materials[start + SEA + 1] != BiomicTerrainSampler.AIR) continue;
            int oldFloor = SEA;
            while (oldFloor > 3 && materials[start + oldFloor] == BiomicTerrainSampler.WATER) oldFloor--;
            if (materials[start + oldFloor] == BiomicTerrainSampler.AIR
                || materials[start + oldFloor] == BiomicTerrainSampler.LAVA) continue;
            int floor = loweredFloor((chunkX << 4) + x, (chunkZ << 4) + z, oldFloor);
            if (floor >= oldFloor) continue;
            // Seal the new seabed rather than opening an unlit cave to the ocean.
            for (int y = Math.max(1, floor - 2); y <= floor; y++) materials[start + y] = BiomicTerrainSampler.STONE;
            for (int y = floor + 1; y <= SEA; y++) materials[start + y] = BiomicTerrainSampler.WATER;
        }
    }

    /**
     * Give Terra locators/ore replacement the same ocean as the physical generator. Never edit the shared
     * Origen payload: its cache is also used by the Origen-only backup. All collapsed authored siblings must
     * observe water, otherwise an ore written to the skipped sibling can resurrect the removed seabed.
     * Accepts either one authored column or a whole chunk; physical terrain is requested lazily.
     */
    byte[] authoredView(byte[] authored, int blockX, int blockZ, Supplier<byte[]> physicalTerrain) {
        int height = OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE - OrigenDensityField.UPSTREAM_MIN_Y;
        int width = authored.length == height ? 1 : 16;
        byte[] result = authored, physical = null;
        int seaIndex = OrigenDensityField.UPSTREAM_SEA_LEVEL - OrigenDensityField.UPSTREAM_MIN_Y;
        for (int x = 0; x < width; x++) for (int z = 0; z < width; z++) {
            int offset = (x * width + z) * height, wx = blockX + x, wz = blockZ + z;
            if (authored[offset + seaIndex] != BiomicTerrainSampler.WATER
                || authored[offset + seaIndex + 1] != BiomicTerrainSampler.AIR
                || !habitat.test(wx, wz)) continue;
            if (physical == null) physical = physicalTerrain.get();
            int start = BiomicTerrainSampler.index(wx & 15, 0, wz & 15);
            if (physical[start + SEA] != BiomicTerrainSampler.WATER
                || physical[start + SEA + 1] != BiomicTerrainSampler.AIR) continue;
            int floor = SEA;
            while (floor > 3 && physical[start + floor] == BiomicTerrainSampler.WATER) floor--;
            int oldAuthoredFloor = seaIndex;
            while (oldAuthoredFloor > 0 && authored[offset + oldAuthoredFloor] == BiomicTerrainSampler.WATER)
                oldAuthoredFloor--;
            int oldFloor = OrigenVerticalProfileSampler
                .toBiomicBlockY(oldAuthoredFloor + OrigenDensityField.UPSTREAM_MIN_Y);
            if (floor >= oldFloor || loweredFloor(wx, wz, oldFloor) >= oldFloor) continue;
            if (result == authored) result = authored.clone();
            for (int i = 0; i <= seaIndex; i++) {
                int y = OrigenVerticalProfileSampler.toBiomicBlockY(i + OrigenDensityField.UPSTREAM_MIN_Y);
                if (y >= Math.max(1, floor - 2) && y <= SEA) result[offset + i] = physical[start + y];
            }
        }
        return result;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
