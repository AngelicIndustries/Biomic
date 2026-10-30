package industries.angelic.biomic.world.terrain;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

import industries.angelic.biomic.world.cave.CaveComposition;
import industries.angelic.biomic.world.cave.CaveComposition.Material;
import industries.angelic.biomic.world.cave.UltimateWorleyField;
import industries.angelic.biomic.world.cave.YungCaveLiquidPlan;
import industries.angelic.biomic.world.cave.YungWaterRegionSelector;

/**
 * New-save cave adapter. Its halo observes immutable pre-expansion terrain,
 * never generated chunks, decoration, or another chunk's excavation result.
 * No world access, chunk loading, global carver hook or native ore pass.
 */
final class HostedCaveExpansion {

    static final String STRESS_CACHE_CAPACITY_PROPERTY = "biomic.worldgenStressCaveCacheCapacity";
    // Shared by this provider's workers. The production512 ceiling is exactly
    // 205,520,896 bytes (196MiB): 512 * (two 65,536-byte Base arrays + one 65,536-float field + one
    // 65,536-bit mask). Object/map overhead is additional and deliberately excluded from this array budget.
    // Hohenangst's physical height is512, but these immutable cave inputs retain the authored source height256;
    // the cache arrays therefore remain 65,536 cells rather than doubling with physical world height.
    private static final int DEFAULT_CACHE_CAPACITY = 512;

    record Base(byte[] materials, byte[] origins) {}

    record CacheStats(long hits, long misses, long evictions, int size) {}

    private static final CaveComposition.Limits LIMITS = new CaveComposition.Limits(8, 120, 10, 4, 4);
    // Shifted512 window rows 0..255 are authored -64..191 at physical scale: 8..180 spans the same authored
    // band (about -56..116) as the compressed 8..120, with the field reaching just above it.
    private static final CaveComposition.Limits SHIFTED_LIMITS = new CaveComposition.Limits(8, 180, 10, 4, 4);
    private final CaveComposition.Limits limits;
    private final UltimateWorleyField field;
    /** Regional providers: YUNG/Ultimate-chamber regions blended into this same Worley field. */
    private final industries.angelic.biomic.world.cave.RegionalCaveContentDensity regional;
    /** Regional providers: YUNG's water/lava regions fill opened cells at and below its liquid altitude. */
    private final YungWaterRegionSelector liquids;
    private static final YungCaveLiquidPlan.Settings LIQUID = YungCaveLiquidPlan.Settings.sourceDefaults();
    private final boolean roomy;
    private final BiFunction<Integer, Integer, Base> source;
    private final int cacheCapacity;
    private final BoundedCache<Base> bases;
    private final BoundedCache<BitSet> masks;
    private final BoundedCache<float[]> fields;

    HostedCaveExpansion(long seed, industries.angelic.biomic.world.cave.CaveExpansionSettings settings,
        BiFunction<Integer, Integer, Base> source) {
        this(seed, settings, source, configuredCacheCapacity());
    }

    HostedCaveExpansion(long seed, industries.angelic.biomic.world.cave.CaveExpansionSettings settings,
        BiFunction<Integer, Integer, Base> source, int cacheCapacity) {
        this.source = source;
        this.cacheCapacity = validatedCacheCapacity(cacheCapacity);
        bases = new BoundedCache<>(this.cacheCapacity);
        // Masks are 8KiB (bases 128KiB, fields 256KiB) and are read again by neighbouring populations.
        masks = new BoundedCache<>(this.cacheCapacity * 4);
        fields = new BoundedCache<>(this.cacheCapacity);
        roomy = settings.roomy();
        limits = settings.shifted512() ? SHIFTED_LIMITS : LIMITS;
        field = new UltimateWorleyField(
            seed,
            roomy ? new UltimateWorleyField.Settings(-.25f, 8f, 1.5f, 1f, 1, settings.shifted512() ? 190 : 128)
                : UltimateWorleyField.Settings.sourceDefaults(),
            roomy);
        regional = settings.regional() ? new industries.angelic.biomic.world.cave.RegionalCaveContentDensity(
            seed,
            settings.shifted512() ? industries.angelic.biomic.world.cave.RegionalCaveContentDensity.shifted512Defaults()
                : industries.angelic.biomic.world.cave.RegionalCaveContentDensity.defaults())
            : null;
        liquids = regional == null ? null
            : new YungWaterRegionSelector(seed, YungWaterRegionSelector.Settings.sourceDefaults());
        if (regional != null && !regional.settings()
            .base()
            .clearance()
            .equals(limits)) throw new IllegalStateException("Regional cave envelope differs from the provider's");
    }

    private static int configuredCacheCapacity() {
        return configuredCacheCapacity(System.getProperty(STRESS_CACHE_CAPACITY_PROPERTY));
    }

    static int configuredCacheCapacity(String configured) {
        if (configured == null) return DEFAULT_CACHE_CAPACITY;
        throw new IllegalArgumentException(
            STRESS_CACHE_CAPACITY_PROPERTY + " is restricted to the worldgen stress fixture");
    }

    static int validatedCacheCapacity(int capacity) {
        if (capacity != 32 && capacity != 64 && capacity != 128 && capacity != 256 && capacity != 512)
            throw new IllegalArgumentException("Cave cache capacity must be 32, 64, 128, 256 or 512");
        return capacity;
    }

    private static long key(int cx, int cz) {
        return ((long) cx << 32) ^ (cz & 0xffffffffL);
    }

    /** Both planners only carve rows inside the envelope. */
    boolean mayCarveRow(int y) {
        return y >= limits.minY() && y <= limits.maxY();
    }

    private volatile java.util.concurrent.Executor haloWorkers;

    /** The terrain workers, for making a cold mask's halo in parallel (see {@link #prefetchHalo}). */
    void setHaloWorkers(java.util.concurrent.Executor workers) {
        haloWorkers = workers;
    }

    /**
     * Outside the worker pool (the server thread, building a chunk the workers had not finished) the 3x3 halo's
     * missing bases and density fields are made in parallel: each is handed to the workers and the caller takes any
     * no worker has started, so it never waits behind queued work. A cold chunk's halo was eight neighbours' terrain
     * made one after another on the server thread (130-285 ms of a first chunk on two cores). Bases and fields are
     * pure, so who makes one changes nothing.
     */
    private void prefetchHalo(int cx, int cz) {
        java.util.concurrent.Executor workers = haloWorkers;
        if (workers == null || Thread.currentThread()
            .getName()
            .startsWith("Biomic")) return;
        java.util.List<java.util.concurrent.FutureTask<Object>> tasks = new java.util.ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            int x = cx + dx, z = cz + dz;
            if (bases.peek(key(x, z)) == null) tasks.add(new java.util.concurrent.FutureTask<>(() -> base(x, z)));
            if (fields.peek(key(x, z)) == null) tasks.add(new java.util.concurrent.FutureTask<>(() -> field(x, z)));
        }
        if (tasks.size() < 2) return;
        for (var task : tasks) {
            try {
                workers.execute(task);
            } catch (java.util.concurrent.RejectedExecutionException stopping) {
                break;
            }
        }
        for (var task : tasks) task.run();
        for (var task : tasks) {
            try {
                task.get();
            } catch (InterruptedException interrupted) {
                Thread.currentThread()
                    .interrupt();
                return;
            } catch (java.util.concurrent.ExecutionException failure) {
                throw new IllegalStateException("Cave halo preparation failed", failure.getCause());
            }
        }
    }

    /** A base already computed for some halo; never computes. Bases are immutable once cached. */
    Base peekBase(int cx, int cz) {
        return bases.peek(key(cx, cz));
    }

    private Base base(int cx, int cz) {
        long key = key(cx, cz);
        Base known = bases.get(key);
        if (known != null) return known;
        Base result = source.apply(cx, cz);
        bases.put(key, result);
        return result;
    }

    private float[] field(int cx, int cz) {
        long key = key(cx, cz);
        float[] known = fields.get(key);
        if (known != null) return known;
        float[] result = field.chunk(cx, cz);
        fields.put(key, result);
        return result;
    }

    byte[] apply(int cx, int cz, Base terrain) {
        bases.put(key(cx, cz), terrain);
        byte[] result = terrain.materials.clone();
        forEachOpened(cx, cz, mask(cx, cz), (index, material) -> result[index] = material);
        return result;
    }

    @FunctionalInterface
    interface Opened {

        void at(int index, byte material);
    }

    /** What each cell of a final mask becomes: air, or YUNG's water or lava at and below the liquid altitude. */
    void forEachOpened(int cx, int cz, BitSet mask, Opened opened) {
        YungWaterRegionSelector.Selection kinds = null;
        for (int index = mask.nextSetBit(0); index >= 0; index = mask.nextSetBit(index + 1)) {
            byte material = BiomicTerrainSampler.AIR;
            if (liquids != null && (index & 255) <= LIQUID.liquidAltitude()) {
                if (kinds == null) kinds = liquids.chunk(cx, cz);
                material = kinds.at(index >> 12, (index >> 8) & 15) == YungWaterRegionSelector.Kind.WATER
                    ? BiomicTerrainSampler.WATER
                    : BiomicTerrainSampler.LAVA;
            }
            opened.at(index, material);
        }
    }

    /**
     * Drops the wet cells YUNG's plan keeps as rock: those not sealed by rock or the same fluid below and on all
     * four sides. A neighbouring chunk's rock counts as excavated, so cross-chunk seals never wait for its plan
     * (opposite fluids then keep rock on both sides).
     */
    private BitSet sealLiquids(int cx, int cz, BitSet excavated, CaveComposition.Host host) {
        boolean wet = false;
        for (int column = 0; column < 256 && !wet; column++) {
            int first = excavated.nextSetBit(column * 256);
            wet = first >= 0 && first <= column * 256 + LIQUID.liquidAltitude();
        }
        if (!wet) return excavated;
        BitSet retained = YungCaveLiquidPlan.retainedRock(
            cx,
            cz,
            excavated,
            host,
            (x, y, z) -> (x >> 4) == cx && (z >> 4) == cz ? excavated.get(BiomicTerrainSampler.index(x & 15, y, z & 15))
                : host.at(x, y, z) == Material.ROCK,
            liquids,
            LIQUID);
        BitSet result = (BitSet) excavated.clone();
        result.andNot(retained);
        return result;
    }

    /** The chunk's own exact base replaces any halo copy before planning. */
    BitSet mask(int cx, int cz, Base terrain) {
        bases.put(key(cx, cz), terrain);
        return mask(cx, cz);
    }

    /** The planner rows a column's rock pocket spans (CavePockets, packed the same way), or -1. */
    interface PocketRows {

        int range(int x, int z);
    }

    private PocketRows pocketRows;

    /**
     * Rock pockets the caves leave solid: the planner treats their cells as protected and keeps its guard from them.
     */
    void setPockets(PocketRows rows) {
        pocketRows = rows;
    }

    BitSet mask(int cx, int cz) {
        long key = key(cx, cz);
        BitSet known = masks.get(key);
        if (known != null) return known;
        prefetchHalo(cx, cz);
        int[] pockets = pocketRows == null ? null : new int[48 * 48];
        if (pockets != null) java.util.Arrays.fill(pockets, Integer.MIN_VALUE);
        int haloX = (cx - 1) << 4, haloZ = (cz - 1) << 4;
        // The fixed four-block guard plus two-block clearance halo fits a 3x3 chunk window.
        // Indexed local references avoid boxed keys/hash lookups for millions of voxel probes,
        // and keep arrays alive independently of shared LRU eviction or concurrent calls.
        Base[] halo = new Base[9];
        CaveComposition.Host host = (x, y, z) -> {
            if (y < 1 || y >= 255) return Material.PROTECTED;
            if (pockets != null) {
                int lx = x - haloX, lz = z - haloZ;
                if (lx >= 0 && lx < 48 && lz >= 0 && lz < 48) {
                    int index = lx * 48 + lz, range = pockets[index];
                    if (range == Integer.MIN_VALUE) pockets[index] = range = pocketRows.range(x, z);
                    if (range != -1 && y >= CavePockets.low(range) && y <= CavePockets.high(range))
                        return Material.PROTECTED;
                }
            }
            int slot = haloSlot(cx, cz, x, z);
            Base terrain = halo[slot];
            if (terrain == null) halo[slot] = terrain = base(x >> 4, z >> 4);
            int index = BiomicTerrainSampler.index(x & 15, y, z & 15);
            return classify(terrain.materials[index], terrain.origins[index]);
        };
        BitSet result;
        {
            float[][] localFields = new float[9][];
            CaveComposition.Density density = regional != null ? regional.createPlanningDensity(this::field)
                : (x, y, z) -> {
                    int slot = haloSlot(cx, cz, x, z);
                    float[] values = localFields[slot];
                    if (values == null) localFields[slot] = values = field(x >> 4, z >> 4);
                    return values[BiomicTerrainSampler.index(x & 15, y, z & 15)];
                };
            result = roomy ? CaveComposition.planWithClearance(cx, cz, host, density, limits)
                : CaveComposition.plan(cx, cz, host, density, limits);
        }
        if (liquids != null && result != null) result = sealLiquids(cx, cz, result, host);
        if (result == null || result.length() > BiomicTerrainSampler.BLOCK_COUNT)
            throw new IllegalStateException("Cave provider returned an invalid authored mask");
        masks.put(key, result);
        return result;
    }

    /** Snapshot only an already-computed mask; never starts halo or density work. */
    BitSet completedMaskSnapshot(int cx, int cz) {
        BitSet known = masks.get(key(cx, cz));
        return known == null ? null : (BitSet) known.clone();
    }

    /** Re-seed the existing bounded cache from the exact worker payload without sharing mutable ownership. */
    void rememberCompletedMask(int cx, int cz, BitSet completed) {
        if (completed == null) return;
        if (completed.length() > BiomicTerrainSampler.BLOCK_COUNT)
            throw new IllegalArgumentException("Cave projection mask exceeds one authored source chunk");
        masks.put(key(cx, cz), (BitSet) completed.clone());
    }

    String performanceReport() {
        CacheStats base = bases.stats(), mask = masks.stats(), density = fields.stats();
        return String.format(
            java.util.Locale.ROOT,
            "Cave caches (capacity %d): base %d hits/%d misses/%d evictions/%d resident; "
                + "mask %d/%d/%d/%d; field %d/%d/%d/%d",
            cacheCapacity,
            base.hits(),
            base.misses(),
            base.evictions(),
            base.size(),
            mask.hits(),
            mask.misses(),
            mask.evictions(),
            mask.size(),
            density.hits(),
            density.misses(),
            density.evictions(),
            density.size());
    }

    private static final class BoundedCache<T> {

        private final int capacity;
        private final Map<Long, T> entries;
        private long hits, misses, evictions;

        BoundedCache(int capacity) {
            this.capacity = capacity;
            entries = new LinkedHashMap<>(capacity, .75f, true);
        }

        synchronized T get(long key) {
            T value = entries.get(key);
            if (value == null) misses++;
            else hits++;
            return value;
        }

        synchronized T peek(long key) {
            return entries.get(key);
        }

        synchronized void put(long key, T value) {
            entries.put(key, value);
            if (entries.size() > capacity) {
                var eldest = entries.entrySet()
                    .iterator();
                eldest.next();
                eldest.remove();
                evictions++;
            }
        }

        synchronized CacheStats stats() {
            return new CacheStats(hits, misses, evictions, entries.size());
        }
    }

    private static int haloSlot(int cx, int cz, int x, int z) {
        int dx = (x >> 4) - cx + 1, dz = (z >> 4) - cz + 1;
        if (dx < 0 || dx > 2 || dz < 0 || dz > 2)
            throw new IllegalStateException("Cave planner exceeded its immutable halo");
        return dx * 3 + dz;
    }

    static Material classify(byte material, byte origins) {
        if (material == BiomicTerrainSampler.WATER || material == BiomicTerrainSampler.LAVA) return Material.FLUID;
        if ((origins & TerrainDensitySampler.GROUND_ORIGIN) == 0) return Material.AIR;
        if ((origins & TerrainDensitySampler.FLOATING_ORIGIN) != 0) return Material.PROTECTED;
        if (material == BiomicTerrainSampler.AIR) return Material.CAVITY;
        return switch (material) {
            case BiomicTerrainSampler.STONE, BiomicTerrainSampler.DEEPSLATE, BiomicTerrainSampler.TUFF, BiomicTerrainSampler.GRANITE, BiomicTerrainSampler.DIORITE, BiomicTerrainSampler.ANDESITE, BiomicTerrainSampler.BASALT, BiomicTerrainSampler.SMOOTH_BASALT, BiomicTerrainSampler.BLACKSTONE, BiomicTerrainSampler.SANDSTONE, BiomicTerrainSampler.RED_SANDSTONE -> Material.ROCK;
            // Soil, gravity blocks, ice, authored structures and decorative strata remain protected.
            default -> Material.PROTECTED;
        };
    }

    byte[] authoredView(byte[] authored, int blockX, int blockZ, BitSet completedMask) {
        int height = OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE - OrigenDensityField.UPSTREAM_MIN_Y;
        if (authored.length != height && authored.length != 256 * height)
            throw new IllegalArgumentException("Expected one authored column or chunk");
        int width = authored.length == height ? 1 : 16;
        if (width == 16 && ((blockX & 15) != 0 || (blockZ & 15) != 0))
            throw new IllegalArgumentException("Authored chunk view must be chunk-aligned");
        if (completedMask != null && completedMask.length() > BiomicTerrainSampler.BLOCK_COUNT)
            throw new IllegalArgumentException("Cave projection mask exceeds one authored source chunk");
        BitSet carved = completedMask == null ? mask(blockX >> 4, blockZ >> 4) : completedMask;
        if (carved.isEmpty()) return authored;
        byte[] opened = new byte[BiomicTerrainSampler.BLOCK_COUNT];
        forEachOpened(blockX >> 4, blockZ >> 4, carved, (index, material) -> opened[index] = material);
        byte[] result = authored;
        for (int x = 0; x < width; x++) for (int z = 0; z < width; z++) {
            int offset = (x * width + z) * height;
            for (int ay = 0; ay < height; ay++) {
                int y = OrigenVerticalProfileSampler.toBiomicBlockY(ay + OrigenDensityField.UPSTREAM_MIN_Y);
                int index = BiomicTerrainSampler.index((blockX + x) & 15, y, (blockZ + z) & 15);
                if (carved.get(index) && authored[offset + ay] != opened[index]) {
                    if (result == authored) result = authored.clone();
                    result[offset + ay] = opened[index];
                }
            }
        }
        return result;
    }
}
