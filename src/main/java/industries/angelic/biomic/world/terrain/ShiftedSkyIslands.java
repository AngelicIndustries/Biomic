package industries.angelic.biomic.world.terrain;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import industries.angelic.biomic.api.ChunkVerticalLayout;
import industries.angelic.biomic.world.WorldgenContext;

/**
 * The sky layer's floating islands (archipelagos and donuts) over uncompressed (shifted512) Origen. Each island
 * column is one span from its deck to its keel (IslandKeel), its underside kept twelve blocks clear of nearby
 * ground (a deep keel is trimmed there, not dropped). Per chunk, the island mask is its ownership for terrain checks,
 * habitats and biomes; the cells get stone with a rock skin on their visible underside and flanks (IslandSkin).
 */
public final class ShiftedSkyIslands implements HostedIslandRuntime.IslandShape {

    private static final ChunkVerticalLayout LAYOUT = ChunkVerticalLayout.EXTENDED_512;
    private static final int GRID = 4;
    private static final int GRID_WIDTH = 16 / GRID + 1;
    private static final int GROUND_CLEARANCE = 12;
    private static final int CLEARANCE_GRID_RADIUS = 24 / GRID;
    private static final int MASK_CACHE = 1024;
    private static final Chunk EMPTY = new Chunk(new BitSet(0), null);

    private final FloatingIslandDensitySampler islands;
    private final ShiftedGroundHeights ground;
    private final SkyIslandHabitats habitats;
    private final long seed;
    private final Map<Long, Chunk> chunks = new LinkedHashMap<Long, Chunk>(256, .75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Chunk> eldest) {
            return size() > MASK_CACHE;
        }
    };

    /** One chunk's islands: its cells and each column's span ({@code bottom << 16 | top}, or -1). */
    private record Chunk(BitSet mask, int[] spans) {}

    public ShiftedSkyIslands(WorldgenContext context, IslandBiomeRules rules) {
        ground = new ShiftedGroundHeights(context);
        islands = FloatingIslandDensitySampler.shifted512(context, ground);
        habitats = new SkyIslandHabitats(
            islands,
            rules,
            context.getNativeHabitats()
                .bopHabitats());
        seed = context.getSettings()
            .getWorldSeed();
    }

    /** The mainland's habitats, which islands follow when the world's rules say so. */
    public void setGround(RegionalHabitatSampler mainland) {
        habitats.setGround(mainland);
    }

    /** Island cells of one chunk (512 layout indices); shared and immutable. */
    public BitSet mask(int chunkX, int chunkZ) {
        return chunk(chunkX, chunkZ).mask;
    }

    private Chunk chunk(int chunkX, int chunkZ) {
        long key = (long) chunkX << 32 ^ (chunkZ & 0xffffffffL);
        synchronized (chunks) {
            Chunk known = chunks.get(key);
            if (known != null) return known;
        }
        Chunk made = rasterize(chunkX, chunkZ);
        synchronized (chunks) {
            chunks.put(key, made);
        }
        return made;
    }

    /** Fills island cells that are open sky in this physical chunk with their rock; the hosted soil comes later. */
    public byte[] apply(int chunkX, int chunkZ, byte[] physical) {
        Chunk chunk = chunk(chunkX, chunkZ);
        if (chunk == EMPTY) return physical;
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int span = chunk.spans[x * 16 + z];
            if (span < 0) continue;
            int bottom = span >>> 16, top = span & 0xFFFF;
            int west = neighbour(chunk, baseX, baseZ, x - 1, z), east = neighbour(chunk, baseX, baseZ, x + 1, z);
            int north = neighbour(chunk, baseX, baseZ, x, z - 1), south = neighbour(chunk, baseX, baseZ, x, z + 1);
            boolean tip = IslandSkin.tip(bottom, west, east, north, south);
            String habitat = habitats.at(baseX + x, baseZ + z);
            IslandKind kind = IslandKind.of(habitat);
            int water = poolDepth(habitat, baseX + x, baseZ + z, top, bottom, west, east, north, south);
            for (int y = bottom; y <= top; y++) {
                int index = LAYOUT.index(x, y, z);
                if (physical[index] != BiomicTerrainSampler.AIR) continue;
                physical[index] = y > top - water ? BiomicTerrainSampler.WATER
                    : IslandSkin.material(
                        seed,
                        baseX + x,
                        y,
                        baseZ + z,
                        top,
                        bottom,
                        IslandSkin.exposed(y, west, east, north, south),
                        tip,
                        kind);
            }
        }
        return physical;
    }

    /**
     * Swamp islands hold shallow pools (review: "none of the bayou islands truly look like a bayou"): where a broad
     * noise says so, a column's top one or two cells are water, but only where none of its four neighbours stands
     * lower,
     * so the water is held at its level, and never within two blocks of an island's edge.
     */
    private int poolDepth(String habitat, int x, int z, int top, int bottom, int west, int east, int north, int south) {
        if (habitat == null || !swampy(habitat) || top - bottom < 6) return 0;
        for (int neighbour : new int[] { west, east, north, south })
            if (neighbour < 0 || (neighbour & 0xFFFF) < top) return 0;
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) if (islands.span(x + dx, z + dz) < 0) return 0;
        double pool = ValueNoise2D.fractal(seed ^ POOL_SALT, x, z, 20, 2, .5);
        return pool > POOL_DEEP ? 2 : pool > POOL_SHALLOW ? 1 : 0;
    }

    private static boolean swampy(String habitat) {
        String key = habitat.toLowerCase(java.util.Locale.ROOT);
        for (String word : new String[] { "bayou", "swamp", "marsh", "fen", "bog", "mire", "wetland", "mangrove" })
            if (key.contains(word)) return true;
        return false;
    }

    private static final double POOL_SHALLOW = 0.12D, POOL_DEEP = 0.38D;
    private static final long POOL_SALT = 0x7B1D5E3A9C24F681L;

    /** A column's span, inside this chunk or (without ground clearance) just beside it. */
    private int neighbour(Chunk chunk, int baseX, int baseZ, int x, int z) {
        return x >= 0 && x < 16 && z >= 0 && z < 16 ? chunk.spans[x * 16 + z] : islands.span(baseX + x, baseZ + z);
    }

    public boolean owns(int x, int y, int z) {
        if (y < islands.minY() || y > islands.maxY()) return false;
        BitSet mask = mask(x >> 4, z >> 4);
        return mask.get(LAYOUT.index(x & 15, y, z & 15));
    }

    /** The island in one column: {@code bottom << 16 | top}, or -1 where it has none. */
    @Override
    public int span(int x, int z) {
        Chunk chunk = chunk(x >> 4, z >> 4);
        return chunk == EMPTY ? -1 : chunk.spans[(x & 15) * 16 + (z & 15)];
    }

    /** {@link #span}, for a lone column, without making its chunk: for probes spread over many chunks. */
    public int columnSpan(int x, int z) {
        int span = islands.span(x, z);
        if (span < 0) return -1;
        int baseX = Math.floorDiv(x, GRID) * GRID, baseZ = Math.floorDiv(z, GRID) * GRID;
        int ceiling = Integer.MIN_VALUE;
        for (int gx = -CLEARANCE_GRID_RADIUS; gx <= 1 + CLEARANCE_GRID_RADIUS; gx++)
            for (int gz = -CLEARANCE_GRID_RADIUS; gz <= 1 + CLEARANCE_GRID_RADIUS; gz++)
                ceiling = Math.max(ceiling, ground.ground(baseX + gx * GRID, baseZ + gz * GRID));
        return clear(span, ceiling);
    }

    /** The islets centred in a chunk that kept their island (see FloatingIslandDensitySampler#islets). */
    @Override
    public List<int[]> islets(int chunkX, int chunkZ) {
        List<int[]> islets = islands.islets(chunkX, chunkZ);
        islets.removeIf(islet -> span(islet[0], islet[1]) < 0);
        return islets;
    }

    /** A cheap estimate of the island top in a column, or -1 (see FloatingIslandDensitySampler.estimateTop). */
    public int estimateTop(int x, int z) {
        return islands.estimateTop(x, z);
    }

    /** The admitted island cluster of one cluster region, or null. */
    public FloatingIslandDensitySampler.Cluster cluster(int regionX, int regionZ) {
        return islands.cluster(regionX, regionZ);
    }

    /**
     * The habitat of a lone column's island, or null where it has none: {@link #habitatAt} without making chunks, and
     * without the ground clearance, which trims an island's keel but keeps its deck.
     */
    public String columnHabitat(int x, int z) {
        return islands.span(x, z) < 0 ? null : habitats.at(x, z);
    }

    public FloatingIslandArchetype archetypeAt(int x, int y, int z) {
        return owns(x, y, z) ? islands.sampleArchetype(x, y, z) : null;
    }

    /**
     * One RU/Biomic/BOP habitat per island (SkyIslandHabitats): a donut is one island, while an archipelago's islands
     * (one per jittered ~144-block cell) differ or share their group's, as the world's rules say.
     */
    public String habitatAt(int x, int y, int z) {
        return archetypeAt(x, y, z) == null ? null : habitats.at(x, z);
    }

    private Chunk rasterize(int chunkX, int chunkZ) {
        int[] spans = new int[256];
        boolean any = false;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            spans[x * 16 + z] = islands.span((chunkX << 4) + x, (chunkZ << 4) + z);
            any |= spans[x * 16 + z] >= 0;
        }
        if (!any) return EMPTY;
        int[][] ceilings = groundCeilings(chunkX, chunkZ);
        BitSet mask = new BitSet(LAYOUT.blockCount());
        any = false;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int span = clear(spans[x * 16 + z], nearbyCeiling(ceilings, x, z));
            spans[x * 16 + z] = span;
            if (span < 0) continue;
            any = true;
            for (int y = span >>> 16; y <= (span & 0xFFFF); y++) mask.set(LAYOUT.index(x, y, z));
        }
        return any ? new Chunk(mask, spans) : EMPTY;
    }

    /** A span kept {@link #GROUND_CLEARANCE} above the ground around it: the keel is trimmed, a sliver dropped. */
    private static int clear(int span, int ceiling) {
        if (span < 0) return -1;
        int bottom = Math.max(span >>> 16, ceiling + GROUND_CLEARANCE + 1), top = span & 0xFFFF;
        return top - bottom < 2 ? -1 : bottom << 16 | top;
    }

    private int[][] groundCeilings(int chunkX, int chunkZ) {
        int width = GRID_WIDTH + CLEARANCE_GRID_RADIUS * 2;
        int[][] sampled = new int[width][width];
        for (int sx = 0; sx < width; sx++) for (int sz = 0; sz < width; sz++) sampled[sx][sz] = ground.ground(
            (chunkX << 4) + (sx - CLEARANCE_GRID_RADIUS) * GRID,
            (chunkZ << 4) + (sz - CLEARANCE_GRID_RADIUS) * GRID);
        int[][] ceilings = new int[GRID_WIDTH][GRID_WIDTH];
        for (int gx = 0; gx < GRID_WIDTH; gx++) for (int gz = 0; gz < GRID_WIDTH; gz++) {
            int maximum = Integer.MIN_VALUE;
            for (int ox = 0; ox <= CLEARANCE_GRID_RADIUS * 2; ox++)
                for (int oz = 0; oz <= CLEARANCE_GRID_RADIUS * 2; oz++)
                    maximum = Math.max(maximum, sampled[gx + ox][gz + oz]);
            ceilings[gx][gz] = maximum;
        }
        return ceilings;
    }

    private static int nearbyCeiling(int[][] ceilings, int x, int z) {
        int gx = x / GRID, gz = z / GRID;
        return Math.max(
            Math.max(ceilings[gx][gz], ceilings[gx + 1][gz]),
            Math.max(ceilings[gx][gz + 1], ceilings[gx + 1][gz + 1]));
    }
}
