package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import industries.angelic.biomic.api.ChunkVerticalLayout;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.alexscaves.AbyssalSourceHeightTransport;
import industries.angelic.biomic.world.alexscaves.OceanTrenchPlanner;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;

/**
 * Alex's Caves' Ocean Trench (the Abyssal Chasm), rasterized one chunk at a time in source (authored) Y and landed
 * through the world's height transport: one-to-one on physical Y + 128 over uncompressed (shifted512) Origen, or the
 * source's compressed legacy transform on 256-high worlds, where several source rows can share a physical cell (the
 * highest wins).
 *
 * <p>
 * The source piece plans a whole trench body in one pass and shares a sequential random stream across it, so a
 * chunk could only be generated after planning up to 25x25 chunks of terrain. Here every decision is a function of
 * position: the dig and sea-mount tests read only the column's ocean floor (sampled from Origen's density, so every
 * chunk sees the same floors), muck depth and wall layering use position hashes, digging wins over a neighbour's
 * wall skin, and a column's skin follows the highest neighbouring floor that walls it. A chunk therefore needs only
 * the floors of a two-column halo. Natural air or lava touching trench water is sealed on its own side, which both
 * chunks of a border compute identically. Sealing runs on the physical rows, so compression never opens a leak.
 * </p>
 */
public final class AbyssalTrenches {

    private static final int AUTHORED_HEIGHT = 384;
    private static final int SEA = OceanTrenchPlanner.SOURCE_SEA_LEVEL, MIN = OceanTrenchPlanner.SOURCE_MIN_Y;
    private static final int CENTER_Y = OceanTrenchPlanner.SOURCE_CENTER_Y;
    private static final int ROWS = SEA - MIN + 1;
    private static final int HALO = 2, SPAN = 16 + 2 * HALO;
    private static final int[][] SIDES = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
    private static final int FLOOR_CACHE = 1 << 16, TRENCH_CACHE = 64, MASK_CACHE = 1024;
    private static final BitSet EMPTY = new BitSet(0);

    private record Trench(long key, int centerX, int centerZ, int radius) {}

    private final long seed;
    private final OrigenDensityField field;
    private final AbyssalRegionSampler regions;
    private final HostedAbyssalRuntime runtime;
    private final BiFunction<Integer, Integer, byte[]> foundation;
    private final ChunkVerticalLayout layout;
    private final AbyssalSourceHeightTransport height;
    /** Physical Y of each source row MIN..SEA, and the physical span they cover. */
    private final int[] physicalRow = new int[ROWS];
    private final int lowPhysical, highPhysical;
    private final Long2IntLinkedOpenHashMap floors = new Long2IntLinkedOpenHashMap(FLOOR_CACHE + 1);
    private final Map<Long, Trench> trenches = lru(TRENCH_CACHE);
    private final Map<Long, BitSet> masks = lru(MASK_CACHE);
    private static final Trench NONE = new Trench(0, 0, 0, 0);

    /** Uncompressed Origen; {@code foundation} rebuilds a chunk's pre-trench terrain when its mask is not cached. */
    public static AbyssalTrenches shifted512(WorldgenContext context, BiFunction<Integer, Integer, byte[]> foundation) {
        return new AbyssalTrenches(context, foundation, AbyssalSourceHeightTransport.shifted512());
    }

    /** Compressed 256-high Origen. */
    public static AbyssalTrenches legacy256(WorldgenContext context, BiFunction<Integer, Integer, byte[]> foundation) {
        return new AbyssalTrenches(
            context,
            foundation,
            AbyssalSourceHeightTransport.legacy(ChunkVerticalLayout.LEGACY_256));
    }

    private AbyssalTrenches(WorldgenContext context, BiFunction<Integer, Integer, byte[]> foundation,
        AbyssalSourceHeightTransport height) {
        seed = context.getSettings()
            .getWorldSeed();
        field = OrigenDensityField.shared(context);
        this.foundation = foundation;
        this.height = height;
        layout = height.layout();
        for (int y = MIN; y <= SEA; y++) physicalRow[y - MIN] = height.physicalY(y);
        lowPhysical = physicalRow[0];
        highPhysical = physicalRow[ROWS - 1];
        floors.defaultReturnValue(Integer.MIN_VALUE);
        regions = new AbyssalRegionSampler(seed, this::floor);
        var host = new HostedAbyssalRuntime.TerrainHost() {

            @Override
            public ChunkVerticalLayout layout() {
                return layout;
            }

            @Override
            public HostedAbyssalRuntime.Identity identity(int x, int sourceY, int z) {
                return regions.identity(x, sourceY, z);
            }
        };
        runtime = height.compressed() ? HostedAbyssalRuntime.legacy256(host) : HostedAbyssalRuntime.shifted512(host);
    }

    public HostedAbyssalRuntime runtime() {
        return runtime;
    }

    /**
     * Source Y of the highest solid block below sea level, or 63 (land) when the column is solid at sea level or lies
     * outside Origen's oceans: a dry basin below the sea's level (a sinkhole) is no sea floor, and a trench dug there
     * stood as a tower of water in its abyssmarine shell (review, 2026-10-24: "this big abyssmarine thing is
     * horrible").
     */
    public int floor(int x, int z) {
        long key = (long) x << 32 ^ (z & 0xFFFF_FFFFL);
        synchronized (floors) {
            int known = floors.getAndMoveToLast(key);
            if (known != Integer.MIN_VALUE) return known;
        }
        int floor = scanFloor(x, z);
        synchronized (floors) {
            floors.putAndMoveToLast(key, floor);
            if (floors.size() > FLOOR_CACHE) floors.removeFirstInt();
        }
        return floor;
    }

    private int scanFloor(int x, int z) {
        if (field.sampleSurfaceProfile(x, z)
            .getKind() != industries.angelic.biomic.biome.origen.OrigenBiomeProfile.Kind.OCEAN || solid(x, SEA - 1, z))
            return SEA;
        int y = SEA - 5;
        while (y > MIN && !solid(x, y, z)) y -= 4;
        if (y <= MIN) return MIN;
        while (y < SEA - 1 && solid(x, y + 1, z)) y++;
        return y;
    }

    private boolean solid(int x, int sourceY, int z) {
        return field.sampleUpstreamInterpolated(x, sourceY, z) > 0.0D;
    }

    /** Trench cells (512 layout indices) of one chunk; shared and immutable. Rebuilds the chunk when not cached. */
    public BitSet mask(int chunkX, int chunkZ) {
        long key = chunkKey(chunkX, chunkZ);
        synchronized (masks) {
            BitSet known = masks.get(key);
            if (known != null) return known;
        }
        if (near(chunkX, chunkZ).isEmpty()) return remember(key, EMPTY);
        return apply(chunkX, chunkZ, foundation.apply(chunkX, chunkZ), null);
    }

    public boolean owns(int x, int y, int z) {
        if (y < lowPhysical || y > highPhysical) return false;
        BitSet mask = lastMask(x >> 4, z >> 4);
        return mask != EMPTY && mask.get(layout.index(x & 15, y, z & 15));
    }

    private final ThreadLocal<LastMask> last = ThreadLocal.withInitial(LastMask::new);

    private static final class LastMask {

        int chunkX, chunkZ;
        BitSet mask;
    }

    private BitSet lastMask(int chunkX, int chunkZ) {
        LastMask known = last.get();
        if (known.mask == null || known.chunkX != chunkX || known.chunkZ != chunkZ) {
            known.chunkX = chunkX;
            known.chunkZ = chunkZ;
            known.mask = mask(chunkX, chunkZ);
        }
        return known.mask;
    }

    /** Saved-biome identity: one trench cell claims its 4x4x4 quart, as in the 256 blend. */
    public boolean ownsQuart(int x, int y, int z) {
        if (y < lowPhysical - 3 || y > highPhysical + 3) return false;
        BitSet mask = lastMask(x >> 4, z >> 4);
        if (mask == EMPTY) return false;
        int minX = x & 12, minY = y & ~3, minZ = z & 12;
        for (int qx = minX; qx < minX + 4; qx++) for (int qz = minZ; qz < minZ + 4; qz++)
            for (int qy = Math.max(minY, 0); qy < Math.min(minY + 4, layout.height()); qy++)
                if (mask.get(layout.index(qx, qy, qz))) return true;
        return false;
    }

    /** Marks every quart holding a trench cell as the Abyssal Chasm. */
    public BiomeVolume project(BiomeVolume original, int chunkX, int chunkZ) {
        BitSet mask = mask(chunkX, chunkZ);
        if (mask.isEmpty()) return original;
        List<String> palette = new ArrayList<>(original.getPalette());
        int id = palette.indexOf(HostedAbyssalRuntime.BIOME_KEY);
        if (id < 0) {
            id = palette.size();
            palette.add(HostedAbyssalRuntime.BIOME_KEY);
        }
        short[] cells = original.copyPaletteIndices();
        for (int i = mask.nextSetBit(0); i >= 0; i = mask.nextSetBit(i + 1)) {
            int column = i / layout.height(), y = i % layout.height();
            int x = column >> 4, z = column & 15;
            cells[((y >> 2) * 4 + (z >> 2)) * 4 + (x >> 2)] = (short) id;
        }
        return BiomeVolume.create(original.getGeneratorVersion(), original.getBlockHeight(), palette, cells);
    }

    /**
     * Physical Y just above the pre-trench sea floor: where the source's floor features start, descending through
     * the trench water to the chasm floor or a sea mount.
     */
    public int populationFloor(int x, int z) {
        return height.physicalY(Math.min(floor(x, z), SEA)) + 1;
    }

    /**
     * Writes the trench into one chunk's pre-cave physical terrain (and its shifted512 authored companion, when
     * given) and returns the trench cells.
     */
    public BitSet apply(int chunkX, int chunkZ, byte[] physical, byte[] authored) {
        long key = chunkKey(chunkX, chunkZ);
        List<Trench> near = near(chunkX, chunkZ);
        if (near.isEmpty()) return remember(key, EMPTY);
        int x0 = (chunkX << 4) - HALO, z0 = (chunkZ << 4) - HALO;
        int[] floor = new int[SPAN * SPAN];
        for (int i = 0; i < SPAN; i++) for (int k = 0; k < SPAN; k++) floor[i * SPAN + k] = floor(x0 + i, z0 + k);
        // Final materials of the own columns over the trench's source rows; the halo ring keeps an abstract copy
        // (its host is not available here) that only needs to tell trench water apart.
        byte[] cells = new byte[SPAN * SPAN * ROWS];
        boolean[] trenchWater = new boolean[SPAN * SPAN * ROWS];
        BitSet written = new BitSet();
        for (int i = 0; i < SPAN; i++) for (int k = 0; k < SPAN; k++) {
            int c = i * SPAN + k;
            boolean own = i >= HALO && i < HALO + 16 && k >= HALO && k < HALO + 16;
            for (int y = MIN; y <= SEA; y++)
                cells[c * ROWS + y - MIN] = own ? physical[layout.index(i - HALO, physicalRow[y - MIN], k - HALO)]
                    : y > floor[c] ? BiomicTerrainSampler.WATER : BiomicTerrainSampler.STONE;
        }
        for (Trench trench : near) carve(trench, x0, z0, floor, cells, trenchWater, written);
        // Land the source rows on physical rows, the highest source row of a shared cell winning.
        int rows = highPhysical - lowPhysical + 1;
        byte[] landed = new byte[SPAN * SPAN * rows];
        boolean[] landedWater = new boolean[SPAN * SPAN * rows];
        BitSet landedWritten = new BitSet();
        for (int c = 0; c < SPAN * SPAN; c++) for (int y = MIN; y <= SEA; y++) {
            int from = c * ROWS + y - MIN, to = c * rows + physicalRow[y - MIN] - lowPhysical;
            landed[to] = cells[from];
            landedWater[to] = trenchWater[from];
            landedWritten.set(to, written.get(from));
        }
        // Seal natural air (and lava) that touches trench water: abyssmarine like the source membrane.
        for (int i = HALO; i < HALO + 16; i++) for (int k = HALO; k < HALO + 16; k++) {
            int c = i * SPAN + k;
            for (int p = 1; p < rows - 1; p++) {
                byte cell = landed[c * rows + p];
                if (cell != BiomicTerrainSampler.AIR && cell != BiomicTerrainSampler.LAVA) continue;
                if (!touchesTrenchWater(landedWater, rows, i, p, k)) continue;
                landed[c * rows + p] = cell == BiomicTerrainSampler.LAVA ? BiomicTerrainSampler.MAGMA
                    : BiomicTerrainSampler.ABYSSMARINE;
                landedWritten.set(c * rows + p);
            }
        }
        BitSet mask = new BitSet();
        for (int w = landedWritten.nextSetBit(0); w >= 0; w = landedWritten.nextSetBit(w + 1)) {
            int c = w / rows, y = w % rows + lowPhysical, i = c / SPAN - HALO, k = c % SPAN - HALO;
            if (i < 0 || i >= 16 || k < 0 || k >= 16) continue;
            int index = layout.index(i, y, k);
            physical[index] = landed[w];
            if (authored != null) authored[(i * 16 + k) * AUTHORED_HEIGHT + y - 64] = landed[w];
            mask.set(index);
        }
        return remember(key, mask.isEmpty() ? EMPTY : mask);
    }

    /** One trench over the halo: dig, muck floor, then wall skins, column by column in the source order of effects. */
    private void carve(Trench trench, int x0, int z0, int[] floor, byte[] cells, boolean[] trenchWater,
        BitSet written) {
        OceanTrenchPlanner.Column[] columns = new OceanTrenchPlanner.Column[SPAN * SPAN];
        int[] lowest = new int[SPAN * SPAN];
        boolean[] centerWater = new boolean[SPAN * SPAN];
        boolean any = false;
        for (int i = 0; i < SPAN; i++) for (int k = 0; k < SPAN; k++) {
            int c = i * SPAN + k;
            var column = new OceanTrenchPlanner.Column(
                trench.centerX(),
                trench.centerZ(),
                trench.radius(),
                x0 + i,
                z0 + k,
                floor[c]);
            columns[c] = column;
            // The source's deepest WATER cell; sea-mount (tuff) digs neither take a muck floor nor raise walls.
            lowest[c] = Integer.MAX_VALUE;
            for (int y = column.digHigh(); y >= column.digLow(); y--)
                if (column.digs(y) && !column.seaMount(y)) lowest[c] = y;
            any |= lowest[c] != Integer.MAX_VALUE || column.digHigh() >= column.digLow();
            centerWater[c] = floor[c] < CENTER_Y || CENTER_Y >= column.digLow() && CENTER_Y <= column.digHigh()
                && column.digs(CENTER_Y)
                && !column.seaMount(CENTER_Y);
        }
        if (!any) return;
        for (int i = 0; i < SPAN; i++) for (int k = 0; k < SPAN; k++) {
            int c = i * SPAN + k, x = x0 + i, z = z0 + k;
            var column = columns[c];
            for (int y = column.digHigh(); y >= column.digLow(); y--) {
                if (!column.digs(y)) continue;
                int at = c * ROWS + y - MIN;
                if (column.seaMount(y)) {
                    if (cells[at] != BiomicTerrainSampler.BEDROCK)
                        set(cells, trenchWater, written, at, BiomicTerrainSampler.TUFF);
                } else set(cells, trenchWater, written, at, BiomicTerrainSampler.WATER);
            }
            if (lowest[c] != Integer.MAX_VALUE) {
                int floorY = lowest[c] - 1;
                if (!column.seaMount(floorY) && floorY < SEA - 32) {
                    int layers = 1 + (int) (hash(trench.key(), x, floorY, z) & 1);
                    for (int layer = 0; layer <= layers && floorY - layer >= MIN; layer++) {
                        int at = c * ROWS + floorY - layer - MIN;
                        if (cells[at] == BiomicTerrainSampler.BEDROCK) break;
                        set(
                            cells,
                            trenchWater,
                            written,
                            at,
                            cells[at] == BiomicTerrainSampler.LAVA ? BiomicTerrainSampler.DEEPSLATE
                                : BiomicTerrainSampler.ABYSSAL_MUCK);
                    }
                }
            }
            if (centerWater[c]) continue;
            // Wall skin: the column itself when its deepest water does not continue below, and any side column
            // whose deepest water this column does not continue at that height.
            int skin = Integer.MIN_VALUE;
            if (lowest[c] != Integer.MAX_VALUE && !column.digs(lowest[c] - 1)) skin = floor[c];
            for (int[] side : SIDES) {
                int si = i + side[0], sk = k + side[1];
                if (si < 0 || si >= SPAN || sk < 0 || sk >= SPAN) continue;
                int s = si * SPAN + sk;
                if (lowest[s] == Integer.MAX_VALUE || skin >= floor[s]) continue;
                var probe = new OceanTrenchPlanner.Column(
                    trench.centerX(),
                    trench.centerZ(),
                    trench.radius(),
                    x,
                    z,
                    floor[s]);
                if (!probe.digs(lowest[s])) skin = floor[s];
            }
            if (skin == Integer.MIN_VALUE) continue;
            boolean mountBelow = false;
            for (int y = MIN + 2; y < skin - 2; y++) {
                if (column.seaMount(y)) {
                    mountBelow = true;
                    continue;
                }
                int at = c * ROWS + y - MIN;
                byte before = cells[at];
                if (mountBelow && before == BiomicTerrainSampler.WATER) continue;
                byte wall = wall(before, skin, y, (int) (hash(trench.key(), x, y, z) >>> 8 & 1));
                if (wall != BiomicTerrainSampler.AIR) set(cells, trenchWater, written, at, wall);
            }
        }
    }

    private static void set(byte[] cells, boolean[] trenchWater, BitSet written, int at, byte material) {
        cells[at] = material;
        trenchWater[at] = material == BiomicTerrainSampler.WATER;
        written.set(at);
    }

    /** Source wall layering; AIR means "leave the cell" (bedrock). */
    private static byte wall(byte before, int floor, int sourceY, int layerOffset) {
        if (before == BiomicTerrainSampler.BEDROCK) return BiomicTerrainSampler.AIR;
        if (before == BiomicTerrainSampler.LAVA) return BiomicTerrainSampler.MAGMA;
        int distance = floor - sourceY;
        if (distance <= 5 + layerOffset)
            return sourceY < 0 ? BiomicTerrainSampler.DEEPSLATE : BiomicTerrainSampler.STONE;
        return distance <= 12 + layerOffset ? BiomicTerrainSampler.DEEPSLATE : BiomicTerrainSampler.ABYSSMARINE;
    }

    private static boolean touchesTrenchWater(boolean[] water, int rows, int i, int p, int k) {
        int c = i * SPAN + k;
        if (water[c * rows + p - 1] || water[c * rows + p + 1]) return true;
        for (int[] side : SIDES) if (water[((i + side[0]) * SPAN + k + side[1]) * rows + p]) return true;
        return false;
    }

    /** Admitted trenches whose body (plus the halo) can reach this chunk, in a fixed order. */
    private List<Trench> near(int chunkX, int chunkZ) {
        List<Trench> out = new ArrayList<>(2);
        int minX = (chunkX << 4) - HALO, maxX = (chunkX << 4) + 15 + HALO;
        int minZ = (chunkZ << 4) - HALO, maxZ = (chunkZ << 4) + 15 + HALO;
        for (HostedAbyssalRuntime.Start start : regions.candidates(chunkX, chunkZ)) {
            Trench trench = trench(start);
            if (trench == NONE) continue;
            int reach = trench.radius() + 1;
            if (trench.centerX() + reach < minX || trench.centerX() - reach > maxX
                || trench.centerZ() + reach < minZ
                || trench.centerZ() - reach > maxZ) continue;
            out.add(trench);
        }
        out.sort((a, b) -> Long.compare(a.key(), b.key()));
        return out;
    }

    private Trench trench(HostedAbyssalRuntime.Start start) {
        long key = chunkKey(start.chunkX(), start.chunkZ());
        synchronized (trenches) {
            Trench known = trenches.get(key);
            if (known != null) return known;
        }
        Trench made;
        try {
            made = new Trench(
                key,
                start.x(),
                start.z(),
                runtime.extent(start)
                    .radius());
        } catch (RuntimeException rejected) {
            made = NONE;
        }
        synchronized (trenches) {
            trenches.put(key, made);
        }
        return made;
    }

    private BitSet remember(long key, BitSet mask) {
        synchronized (masks) {
            masks.put(key, mask);
        }
        return mask;
    }

    private long hash(long trench, int x, int y, int z) {
        return WorldgenContext.mix64(
            seed ^ trench * 0x9E3779B97F4A7C15L
                ^ x * 0xC2B2AE3D27D4EB4FL
                ^ (long) y * 0x165667B19E3779F9L
                ^ z * 0x27D4EB2F165667C5L);
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (long) chunkX << 32 ^ (chunkZ & 0xFFFF_FFFFL);
    }

    private static <T> Map<Long, T> lru(int capacity) {
        return new LinkedHashMap<Long, T>(capacity, .75f, true) {

            @Override
            protected boolean removeEldestEntry(Map.Entry<Long, T> eldest) {
                return size() > capacity;
            }
        };
    }
}
