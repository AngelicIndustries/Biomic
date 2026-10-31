package industries.angelic.biomic.world.terrain;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import industries.angelic.biomic.api.ChunkVerticalLayout;
import industries.angelic.biomic.compat.biome.SaltMarshCompat;
import industries.angelic.biomic.world.WorldgenContext;

/** Save-revision-2 marsh carrier. Low relief, shallow water; never a mountain/island flattener. */
final class HostedMarshTerrain {

    private static final int SEA = 63, GRID = 16, SHORE = 64;
    private final long seed;
    /**
     * The sea's top water block in this layout's source frame: 63 in the compressed 256 frame, Origen's 62 in the
     * authored frame the 512 layout shifts (one higher left a step against Origen's sea, walled by its dams).
     */
    private final int sea;
    private final BiPredicate<Integer, Integer> habitat;
    private final ChunkVerticalLayout layout;
    private final AuthoredVerticalProjection projection;
    private final Map<Long, Double> weights = new LinkedHashMap<Long, Double>(128, .75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Double> e) {
            return size() > 4096;
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

    HostedMarshTerrain(WorldgenContext context) {
        this(
            context.getSettings()
                .getWorldSeed(),
            new RegionalHabitatSampler(context));
    }

    private HostedMarshTerrain(long seed, RegionalHabitatSampler habitats) {
        this(seed, (x, z) -> {
            String key = habitats.sample(x, z);
            return SaltMarshCompat.BIOME.equals(key)
                || industries.angelic.biomic.compat.biome.BopNativeBiomes.lowlandWetland(key);
        });
    }

    HostedMarshTerrain(long seed, BiPredicate<Integer, Integer> habitat) {
        this(seed, habitat, ChunkVerticalLayout.LEGACY_256, AuthoredVerticalProjections.origenCompressed256());
    }

    static HostedMarshTerrain shifted512(WorldgenContext context) {
        RegionalHabitatSampler habitats = new RegionalHabitatSampler(context);
        return shifted512(
            context.getSettings()
                .getWorldSeed(),
            (x, z) -> {
                String key = habitats.sample(x, z);
                return SaltMarshCompat.BIOME.equals(key)
                    || industries.angelic.biomic.compat.biome.BopNativeBiomes.lowlandWetland(key);
            });
    }

    static HostedMarshTerrain shifted512(long seed, BiPredicate<Integer, Integer> habitat) {
        return new HostedMarshTerrain(
            seed,
            habitat,
            ChunkVerticalLayout.EXTENDED_512,
            AuthoredVerticalProjections.shifted512());
    }

    private HostedMarshTerrain(long seed, BiPredicate<Integer, Integer> habitat, ChunkVerticalLayout layout,
        AuthoredVerticalProjection projection) {
        this.seed = seed;
        this.habitat = java.util.Objects.requireNonNull(habitat);
        this.layout = layout;
        this.projection = projection;
        this.sea = layout == ChunkVerticalLayout.EXTENDED_512 ? OrigenDensityField.UPSTREAM_SEA_LEVEL : SEA;
        boolean legacy = layout == ChunkVerticalLayout.LEGACY_256
            && projection == AuthoredVerticalProjections.origenCompressed256();
        boolean shifted = layout == ChunkVerticalLayout.EXTENDED_512
            && projection == AuthoredVerticalProjections.shifted512();
        if (!legacy && !shifted)
            throw new IllegalArgumentException("Marsh terrain requires canonical legacy or shifted layout");
    }

    private static long key(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    int floor(int x, int z, int old) {
        if (old < 50 || old > 96 || !habitat.test(x, z)) return old;
        int gx = Math.floorDiv(x, GRID), gz = Math.floorDiv(z, GRID);
        double fx = Math.floorMod(x, GRID) / (double) GRID, fz = Math.floorMod(z, GRID) / (double) GRID;
        double w = lerp(
            lerp(weight(gx, gz), weight(gx + 1, gz), fx),
            lerp(weight(gx, gz + 1), weight(gx + 1, gz + 1), fx),
            fz);
        // Broad dry hummocks and one-/two-block pools rather than Origen's tall wetland relief.
        double target = sea + 2.0 * ValueNoise2D.fractal(seed ^ 0x73616c74666c6174L, x, z, 42, 3, .5);
        return (int) Math.round(lerp(old, target, w));
    }

    private synchronized double weight(int gx, int gz) {
        long key = key(gx, gz);
        Double prior = weights.get(key);
        if (prior != null) return prior;
        double distance = SHORE;
        int x = gx * GRID, z = gz * GRID;
        for (int dz = -SHORE; dz <= SHORE; dz += GRID) for (int dx = -SHORE; dx <= SHORE; dx += GRID) {
            double d = Math.hypot(dx, dz);
            if (d < distance && !habitat.test(x + dx, z + dz)) distance = d;
        }
        double t = Math.max(0, Math.min(1, (distance - GRID) / (SHORE - GRID)));
        double value = t * t * (3 - 2 * t);
        weights.put(key, value);
        return value;
    }

    void apply(byte[] cells, byte[] origins, int cx, int cz) {
        if (cells == null || origins == null
            || cells.length != layout.blockCount()
            || origins.length != layout.blockCount())
            throw new IllegalArgumentException("Marsh terrain needs complete physical material and origin arrays");
        if (layout == ChunkVerticalLayout.EXTENDED_512) {
            applyShifted(cells, origins, cx, cz);
            return;
        }
        BitSet mask = new BitSet(layout.blockCount());
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int start = BiomicTerrainSampler.index(x, 0, z), old = 255;
            while (old > 0
                && (cells[start + old] == BiomicTerrainSampler.AIR || cells[start + old] == BiomicTerrainSampler.WATER))
                old--;
            // A floating island or high authored landform above the marsh vetoes this column.
            if (old < 50 || old > 96 || (origins[start + old] & TerrainDensitySampler.FLOATING_ORIGIN) != 0) continue;
            int target = floor(cx * 16 + x, cz * 16 + z, old);
            // An unchanged bed under the sea line still takes the marsh's water: left dry, it held air beside its
            // neighbours' water, which Origen's water containment then walled in with stone.
            if (target == old && old >= SEA) continue;
            boolean safe = true;
            for (int y = Math.min(old, target) - 3; y <= Math.max(old, SEA); y++)
                if (cells[start + y] == BiomicTerrainSampler.LAVA || cells[start + y] == BiomicTerrainSampler.BEDROCK
                    || (origins[start + y] & TerrainDensitySampler.FLOATING_ORIGIN) != 0) safe = false;
            if (!safe) continue;
            for (int y = Math.min(old, target) - 2; y <= Math.max(old, SEA); y++) {
                byte state = y > target ? (y <= SEA ? BiomicTerrainSampler.WATER : BiomicTerrainSampler.AIR)
                    : (y == target && y >= SEA ? BiomicTerrainSampler.GRASS : BiomicTerrainSampler.DIRT);
                if (cells[start + y] != state) {
                    cells[start + y] = state;
                    mask.set(start + y);
                }
                if (y <= target) origins[start + y] = TerrainDensitySampler.GROUND_ORIGIN;
            }
        }
        synchronized (changed) {
            changed.put(key(cx, cz), mask);
        }
    }

    private void applyShifted(byte[] cells, byte[] origins, int cx, int cz) {
        BitSet mask = new BitSet(layout.blockCount());
        int minimum = projection.physicalMinY(), maximum = projection.physicalMaxYExclusive();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int oldPhysical = maximum - 1;
            while (oldPhysical >= minimum && (cells[layout.index(x, oldPhysical, z)] == BiomicTerrainSampler.AIR
                || cells[layout.index(x, oldPhysical, z)] == BiomicTerrainSampler.WATER)) oldPhysical--;
            if (oldPhysical < minimum) continue;
            int oldSource = projection.toSourceBlockY(oldPhysical)
                .orElseThrow();
            int oldIndex = layout.index(x, oldPhysical, z);
            if (oldSource < 50 || oldSource > 96 || (origins[oldIndex] & TerrainDensitySampler.FLOATING_ORIGIN) != 0)
                continue;
            int targetSource = floor(cx * 16 + x, cz * 16 + z, oldSource);
            // As in the legacy layout: an unchanged bed under the sea line still takes the marsh's water.
            if (targetSource == oldSource && oldSource >= sea) continue;
            boolean safe = true;
            for (int sourceY = Math.min(oldSource, targetSource) - 3; sourceY <= Math.max(oldSource, sea); sourceY++) {
                int y = projection.toPhysicalBlockY(sourceY)
                    .orElseThrow();
                int i = layout.index(x, y, z);
                if (cells[i] == BiomicTerrainSampler.LAVA || cells[i] == BiomicTerrainSampler.BEDROCK
                    || (origins[i] & TerrainDensitySampler.FLOATING_ORIGIN) != 0) safe = false;
            }
            if (!safe) continue;
            for (int sourceY = Math.min(oldSource, targetSource) - 2; sourceY <= Math.max(oldSource, sea); sourceY++) {
                int y = projection.toPhysicalBlockY(sourceY)
                    .orElseThrow(), i = layout.index(x, y, z);
                byte state = sourceY > targetSource
                    ? (sourceY <= sea ? BiomicTerrainSampler.WATER : BiomicTerrainSampler.AIR)
                    : (sourceY == targetSource && sourceY >= sea ? BiomicTerrainSampler.GRASS
                        : BiomicTerrainSampler.DIRT);
                if (cells[i] != state) {
                    cells[i] = state;
                    mask.set(i);
                }
                if (sourceY <= targetSource) origins[i] = TerrainDensitySampler.GROUND_ORIGIN;
            }
        }
        synchronized (changed) {
            changed.put(key(cx, cz), mask);
        }
    }

    byte[] authoredView(byte[] authored, int bx, int bz, Supplier<byte[]> source, Runnable restoreMask) {
        int height = OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE - OrigenDensityField.UPSTREAM_MIN_Y;
        if (authored.length != height && authored.length != height * 256)
            throw new IllegalArgumentException("Invalid authored marsh view");
        int width = authored.length == height ? 1 : 16;
        if (width == 16 && ((bx & 15) != 0 || (bz & 15) != 0))
            throw new IllegalArgumentException("Unaligned marsh view");
        boolean present = false;
        for (int x = 0; x < width && !present; x++) for (int z = 0; z < width; z++) if (habitat.test(bx + x, bz + z)) {
            present = true;
            break;
        }
        if (!present) return authored;
        byte[] physical = source.get();
        BitSet mask;
        synchronized (changed) {
            mask = changed.get(key(bx >> 4, bz >> 4));
        }
        // Physical terrain can survive longer than this192-entry projection cache. Never expose the old tall
        // wetland source to native decorations merely because its reconciliation mask was evicted.
        if (mask == null) {
            restoreMask.run();
            synchronized (changed) {
                mask = changed.get(key(bx >> 4, bz >> 4));
            }
            if (mask == null) throw new IllegalStateException("Marsh terrain did not restore its projection mask");
        }
        if (mask.isEmpty()) return authored;
        byte[] result = authored.clone();
        for (int x = 0; x < width; x++) for (int z = 0; z < width; z++) for (int ay = 0; ay < height; ay++) {
            int sourceY = ay + OrigenDensityField.UPSTREAM_MIN_Y;
            int y = layout == ChunkVerticalLayout.LEGACY_256 ? OrigenVerticalProfileSampler.toBiomicBlockY(sourceY)
                : projection.toPhysicalBlockY(sourceY)
                    .orElseThrow();
            int i = layout.index((bx + x) & 15, y, (bz + z) & 15);
            if (mask.get(i)) result[(x * width + z) * height + ay] = physical[i];
        }
        return result;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
