package industries.angelic.biomic.world.terrain;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import industries.angelic.biomic.world.WorldgenContext;

/** New, explicitly selected BOP ocean children only. Native ecology on a hybrid ocean carrier. */
final class HostedBopMarineTerrain {

    private static final int SEA = 63, GRID = 16, SHORE = 128;
    private final long seed;
    private final BiFunction<Integer, Integer, String> habitats;
    private final Map<Long, Double> weights = new LinkedHashMap<Long, Double>(128, .75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Double> e) {
            return size() > 8192;
        }
    };
    /** Change masks are 8KiB; neighbouring chunks re-read them during population. */
    static final int MASK_CACHE_CAPACITY = 4096;
    private final Map<Long, BitSet> changed = new LinkedHashMap<Long, BitSet>(32, .75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, BitSet> e) {
            return size() > MASK_CACHE_CAPACITY;
        }
    };

    HostedBopMarineTerrain(WorldgenContext context) {
        this(
            context.getSettings()
                .getWorldSeed(),
            new RegionalHabitatSampler(context)::sample);
    }

    HostedBopMarineTerrain(long seed, BiFunction<Integer, Integer, String> habitats) {
        this.seed = seed;
        this.habitats = habitats;
    }

    static boolean marine(String habitat) {
        return island(habitat) || "biomesoplenty:coral_reef".equals(habitat)
            || "biomesoplenty:kelp_forest".equals(habitat);
    }

    static boolean island(String habitat) {
        return "biomesoplenty:mangrove".equals(habitat) || "biomesoplenty:tropics".equals(habitat)
            || "biomesoplenty:volcano".equals(habitat);
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    int floor(int x, int z, int old) {
        String habitat = habitats.apply(x, z);
        if (old < 4 || old >= SEA || !marine(habitat)) return old;
        int gx = Math.floorDiv(x, GRID), gz = Math.floorDiv(z, GRID);
        double fx = Math.floorMod(x, GRID) / (double) GRID, fz = Math.floorMod(z, GRID) / (double) GRID;
        double blend = lerp(
            lerp(weight(gx, gz), weight(gx + 1, gz), fx),
            lerp(weight(gx, gz + 1), weight(gx + 1, gz + 1), fx),
            fz);
        double relief = ValueNoise2D.fractal(seed ^ 0x424f504d4152494eL, x, z, 96, 3, .5);
        double target = switch (habitat) {
            case "biomesoplenty:mangrove" -> SEA + 2 * relief;
            case "biomesoplenty:tropics" -> SEA + 9 + 10 * relief;
            case "biomesoplenty:volcano" -> SEA + 60 + 22 * relief;
            case "biomesoplenty:kelp_forest" -> Math.min(old, SEA - 24 - 6 * relief);
            default -> Math.min(old, SEA - 8 - 3 * relief);
        };
        return (int) Math.round(lerp(old, target, blend));
    }

    private synchronized double weight(int gx, int gz) {
        long key = key(gx, gz);
        Double prior = weights.get(key);
        if (prior != null) return prior;
        int x = gx * GRID, z = gz * GRID;
        String habitat = habitats.apply(x, z);
        double distance = marine(habitat) ? SHORE : 0;
        for (int dz = -SHORE; dz <= SHORE; dz += GRID) for (int dx = -SHORE; dx <= SHORE; dx += GRID) {
            double d = Math.hypot(dx, dz);
            if (d < distance && !habitat.equals(habitats.apply(x + dx, z + dz))) distance = d;
        }
        double t = Math.max(0, Math.min(1, (distance - GRID) / (SHORE - GRID)));
        double value = t * t * (3 - 2 * t);
        weights.put(key, value);
        return value;
    }

    void apply(byte[] cells, byte[] origins, int cx, int cz) {
        BitSet mask = new BitSet(cells.length);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int start = BiomicTerrainSampler.index(x, 0, z);
            // Only open marine water. No emergent Origen landmark, cave lake or authored land is flattened.
            if (cells[start + SEA] != BiomicTerrainSampler.WATER || cells[start + SEA + 1] != BiomicTerrainSampler.AIR)
                continue;
            int old = SEA;
            while (old > 3 && cells[start + old] == BiomicTerrainSampler.WATER) old--;
            if (cells[start + old] == BiomicTerrainSampler.AIR) continue;
            int target = floor(cx * 16 + x, cz * 16 + z, old);
            if (target == old) continue;
            int bottom = Math.max(1, Math.min(old, target) - 2), top = Math.max(SEA, target);
            boolean safe = true;
            for (int y = bottom; y <= top; y++)
                if (cells[start + y] == BiomicTerrainSampler.LAVA || cells[start + y] == BiomicTerrainSampler.BEDROCK
                    || (origins[start + y] & TerrainDensitySampler.FLOATING_ORIGIN) != 0
                    || y > SEA && cells[start + y] != BiomicTerrainSampler.AIR) safe = false;
            if (!safe) continue;
            for (int y = bottom; y <= top; y++) {
                byte state = y > target ? BiomicTerrainSampler.WATER : BiomicTerrainSampler.STONE;
                if (cells[start + y] != state) {
                    cells[start + y] = state;
                    mask.set(start + y);
                }
                origins[start + y] = y <= target ? TerrainDensitySampler.GROUND_ORIGIN : 0;
            }
        }
        synchronized (changed) {
            changed.put(key(cx, cz), mask);
        }
    }

    byte[] authoredView(byte[] authored, int bx, int bz, Supplier<byte[]> source, Runnable restoreMask) {
        int height = OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE - OrigenDensityField.UPSTREAM_MIN_Y;
        if (authored.length != height && authored.length != height * 256)
            throw new IllegalArgumentException("Invalid BOP marine authored view");
        int width = authored.length == height ? 1 : 16;
        if (width == 16 && ((bx & 15) != 0 || (bz & 15) != 0))
            throw new IllegalArgumentException("Unaligned BOP marine view");
        boolean present = false;
        for (int x = 0; x < width && !present; x++)
            for (int z = 0; z < width; z++) if (marine(habitats.apply(bx + x, bz + z))) {
                present = true;
                break;
            }
        if (!present) return authored;
        byte[] physical = source.get();
        BitSet mask;
        synchronized (changed) {
            mask = changed.get(key(bx >> 4, bz >> 4));
        }
        // The physical caller can retain a chunk longer than our bounded mask cache. Rebuild
        // its deterministic base on a miss, never silently resurrect the old authored seabed.
        if (mask == null) {
            restoreMask.run();
            synchronized (changed) {
                mask = changed.get(key(bx >> 4, bz >> 4));
            }
            if (mask == null) throw new IllegalStateException("BOP marine base did not restore its projection mask");
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

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
