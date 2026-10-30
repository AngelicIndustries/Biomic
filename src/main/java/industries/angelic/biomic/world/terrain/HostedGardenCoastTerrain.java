package industries.angelic.biomic.world.terrain;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.compat.biome.BopNativeBiomes;
import industries.angelic.biomic.world.WorldgenContext;

/** Smooth coastal carrier for Garden provinces; never participates in strict Origen generation. */
final class HostedGardenCoastTerrain {

    private static final int SEA = 63, GRID = 16, GARDEN_FEATHER = 48, OCEAN_SHORE = 64, OCEAN_CORE = 16;
    private final long seed;
    private final BiPredicate<Integer, Integer> garden;
    private final java.util.function.BiFunction<Integer, Integer, OrigenBiomeProfile> profile;
    private final Map<Long, Double> strengths = new LinkedHashMap<Long, Double>(128, .75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Double> e) {
            return size() > 4096;
        }
    };
    private final Map<Long, BitSet> changed = new LinkedHashMap<Long, BitSet>(32, .75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, BitSet> e) {
            return size() > 4096;
        }
    };

    HostedGardenCoastTerrain(WorldgenContext context) {
        RegionalHabitatSampler habitats = new RegionalHabitatSampler(context);
        OrigenVerticalProfileSampler profiles = new OrigenVerticalProfileSampler(context);
        seed = context.getSettings()
            .getWorldSeed();
        garden = (x, z) -> BopNativeBiomes.GARDEN.equals(habitats.sample(x, z));
        profile = profiles::sampleSurface;
    }

    int floor(int x, int z, int old) {
        if (old <= SEA + 4) return old;
        int gx = Math.floorDiv(x, GRID), gz = Math.floorDiv(z, GRID);
        double fx = Math.floorMod(x, GRID) / (double) GRID, fz = Math.floorMod(z, GRID) / (double) GRID;
        double strength = lerp(
            lerp(strength(gx, gz), strength(gx + 1, gz), fx),
            lerp(strength(gx, gz + 1), strength(gx + 1, gz + 1), fx),
            fz);
        if (strength <= 0) return old;
        double noise = ValueNoise2D.fractal(seed ^ 0x67617264656e636fL, x, z, 48, 2, .5);
        int shore = SEA + (int) Math.round(noise * 2);
        return (int) Math.round(lerp(old, Math.min(old, shore), strength));
    }

    private synchronized double strength(int gx, int gz) {
        long key = key(gx, gz);
        Double known = strengths.get(key);
        if (known != null) return known;
        int x = gx * GRID, z = gz * GRID;
        double gardenDistance = GARDEN_FEATHER, oceanDistance = OCEAN_SHORE;
        for (int dz = -OCEAN_SHORE; dz <= OCEAN_SHORE; dz += GRID)
            for (int dx = -OCEAN_SHORE; dx <= OCEAN_SHORE; dx += GRID) {
                double distance = Math.hypot(dx, dz);
                int sx = x + dx, sz = z + dz;
                if (distance <= GARDEN_FEATHER && distance < gardenDistance && garden.test(sx, sz))
                    gardenDistance = distance;
                if (distance < oceanDistance && profile.apply(sx, sz)
                    .getKind() == OrigenBiomeProfile.Kind.OCEAN) oceanDistance = distance;
            }
        double gardenStrength = 1 - smooth(gardenDistance / GARDEN_FEATHER);
        double coastStrength = 1 - smooth((oceanDistance - OCEAN_CORE) / (double) (OCEAN_SHORE - OCEAN_CORE));
        // Bias the overlap toward full reconciliation; the outer smoothstep still reaches zero continuously,
        // while source height discontinuities cannot survive at half strength as a second coastal cliff.
        double value = smooth(Math.sqrt(Math.sqrt(Math.max(0, gardenStrength * coastStrength))));
        strengths.put(key, value);
        return value;
    }

    void apply(byte[] cells, byte[] origins, int cx, int cz) {
        BitSet mask = new BitSet(BiomicTerrainSampler.BLOCK_COUNT);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int start = BiomicTerrainSampler.index(x, 0, z), old = 255;
            while (old > 0
                && (cells[start + old] == BiomicTerrainSampler.AIR || cells[start + old] == BiomicTerrainSampler.WATER))
                old--;
            if (old <= SEA + 4 || (origins[start + old] & TerrainDensitySampler.FLOATING_ORIGIN) != 0) continue;
            int target = floor((cx << 4) + x, (cz << 4) + z, old);
            if (target >= old) continue;
            // Do not manufacture a new shore cap inside a cave or beneath a detached source arch. Descend to the
            // first retained three-block ground substrate; the rewrite may expose existing ground, never add a new
            // unsupported platform at the nominal coast height.
            while (target >= 3 && !supported(cells, origins, start, target)) target--;
            if (target < 3) continue;
            boolean safe = true;
            for (int y = Math.max(1, target - 3); y <= old; y++)
                if (cells[start + y] == BiomicTerrainSampler.LAVA || cells[start + y] == BiomicTerrainSampler.BEDROCK
                    || (origins[start + y] & TerrainDensitySampler.FLOATING_ORIGIN) != 0) {
                        safe = false;
                        break;
                    }
            if (!safe) continue;
            for (int y = Math.max(1, target - 2); y <= old; y++) {
                byte state = y > target ? (y <= SEA ? BiomicTerrainSampler.WATER : BiomicTerrainSampler.AIR)
                    : (y == target && y >= SEA ? BiomicTerrainSampler.GRASS : BiomicTerrainSampler.DIRT);
                if (y < target) state = BiomicTerrainSampler.DIRT;
                int i = start + y;
                if (cells[i] != state) {
                    cells[i] = state;
                    mask.set(i);
                }
                if (y <= target) origins[i] = TerrainDensitySampler.GROUND_ORIGIN;
                else origins[i] &= ~TerrainDensitySampler.GROUND_ORIGIN;
            }
        }
        synchronized (changed) {
            changed.put(key(cx, cz), mask);
        }
    }

    private static boolean supported(byte[] cells, byte[] origins, int start, int y) {
        for (int depth = 0; depth < 3; depth++) {
            int i = start + y - depth;
            byte state = cells[i];
            if (state == BiomicTerrainSampler.AIR || state == BiomicTerrainSampler.WATER
                || state == BiomicTerrainSampler.LAVA
                || (origins[i] & TerrainDensitySampler.GROUND_ORIGIN) == 0
                || (origins[i] & TerrainDensitySampler.FLOATING_ORIGIN) != 0) return false;
        }
        return true;
    }

    byte[] authoredView(byte[] authored, int bx, int bz, Supplier<byte[]> source, Runnable restoreMask) {
        int height = OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE - OrigenDensityField.UPSTREAM_MIN_Y;
        if (authored.length != height && authored.length != height * 256)
            throw new IllegalArgumentException("Invalid authored Garden view");
        int width = authored.length == height ? 1 : 16;
        if (width == 16 && ((bx & 15) != 0 || (bz & 15) != 0))
            throw new IllegalArgumentException("Unaligned Garden view");
        byte[] physical = source.get();
        BitSet mask;
        synchronized (changed) {
            mask = changed.get(key(bx >> 4, bz >> 4));
        }
        // Physical terrain can outlive this bounded mask cache. Rebuild the deterministic base on a miss instead of
        // silently exposing pre-reconciliation authored cells to structures and vegetation.
        if (mask == null) {
            restoreMask.run();
            synchronized (changed) {
                mask = changed.get(key(bx >> 4, bz >> 4));
            }
            if (mask == null) throw new IllegalStateException("Garden coast did not restore its projection mask");
        }
        if (mask.isEmpty()) return authored;
        byte[] result = authored.clone();
        for (int x = 0; x < width; x++) for (int z = 0; z < width; z++) for (int ay = 0; ay < height; ay++) {
            int y = OrigenVerticalProfileSampler.toBiomicBlockY(ay + OrigenDensityField.UPSTREAM_MIN_Y);
            int i = BiomicTerrainSampler.index((bx + x) & 15, y, (bz + z) & 15);
            if (mask.get(i)) result[(x * width + z) * height + ay] = physical[i];
        }
        return result;
    }

    private static double smooth(double value) {
        double t = Math.max(0, Math.min(1, value));
        return t * t * (3 - 2 * t);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static long key(int x, int z) {
        return (long) x << 32 ^ (z & 0xffffffffL);
    }
}
