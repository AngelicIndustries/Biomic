package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.world.WorldgenContext;

/**
 * Rasterizes Terra's authored density lattice once and combines it with Biomic's separately sampled floating-island
 * field.
 */
final class TerrainDensitySampler {

    static final byte GROUND_ORIGIN = 1;
    static final byte FLOATING_ORIGIN = 2;

    private static final int HORIZONTAL_GRID = 4;
    private static final int GRID_WIDTH = BiomicTerrainSampler.CHUNK_WIDTH / HORIZONTAL_GRID + 1;
    private static final int FLOATING_GROUND_CLEARANCE = 12;
    private static final int CLEARANCE_RADIUS = 24;
    private static final int CLEARANCE_GRID_RADIUS = CLEARANCE_RADIUS / HORIZONTAL_GRID;
    private static final int CLEARANCE_GRID_WIDTH = GRID_WIDTH + CLEARANCE_GRID_RADIUS * 2;
    private final WorldgenContext context;
    private final TerrainColumnSampler columnSampler;
    private final OrigenTerrainGraph terrainGraph;

    TerrainDensitySampler(WorldgenContext context, TerrainColumnSampler columnSampler,
        OrigenTerrainGraph terrainGraph) {
        this.context = context;
        this.columnSampler = columnSampler;
        this.terrainGraph = terrainGraph;
    }

    /** The kind of the island holding a column, for its rock (IslandSkin). */
    interface IslandKinds {

        IslandKind at(int x, int z);
    }

    private volatile IslandKinds islandKinds = (x, z) -> IslandKind.GREEN;

    void setIslandKinds(IslandKinds kinds) {
        islandKinds = kinds;
    }

    void fillBaseTerrain(byte[] materials, byte[] solidOrigins, int chunkX, int chunkZ) {
        validateChunkArrays(materials, solidOrigins);
        OrigenDensityField.ScaledChunkDensity groundDensity = terrainGraph.createGroundDensityChunk(chunkX, chunkZ);
        int[] spans = floatingSpans(chunkX, chunkZ);
        double[] groundColumn = new double[BiomicTerrainSampler.WORLD_HEIGHT];
        int baseX = chunkX << 4, baseZ = chunkZ << 4;
        long seed = context.getSettings()
            .getWorldSeed();
        for (int localX = 0; localX < BiomicTerrainSampler.CHUNK_WIDTH; localX++) {
            for (int localZ = 0; localZ < BiomicTerrainSampler.CHUNK_WIDTH; localZ++) {
                groundDensity.fillPhysicalColumn(localX, localZ, groundColumn);
                int span = spans[localX * BiomicTerrainSampler.CHUNK_WIDTH + localZ];
                int bottom = span < 0 ? Integer.MAX_VALUE : span >>> 16,
                    top = span < 0 ? Integer.MIN_VALUE : span & 0xFFFF;
                int west = floatingNeighbour(spans, baseX, baseZ, localX - 1, localZ);
                int east = floatingNeighbour(spans, baseX, baseZ, localX + 1, localZ);
                int north = floatingNeighbour(spans, baseX, baseZ, localX, localZ - 1);
                int south = floatingNeighbour(spans, baseX, baseZ, localX, localZ + 1);
                boolean tip = span >= 0 && IslandSkin.tip(bottom, west, east, north, south);
                IslandKind kind = span >= 0 ? islandKinds.at(baseX + localX, baseZ + localZ) : null;
                for (int y = 0; y < BiomicTerrainSampler.WORLD_HEIGHT; y++) {
                    int index = BiomicTerrainSampler.index(localX, y, localZ);
                    if (y == 0) {
                        materials[index] = BiomicTerrainSampler.BEDROCK;
                        solidOrigins[index] = GROUND_ORIGIN;
                        continue;
                    }
                    boolean ground = groundColumn[y] > 0.0D;
                    boolean floating = y >= bottom && y <= top;
                    if (ground || floating) {
                        materials[index] = ground ? BiomicTerrainSampler.STONE
                            : IslandSkin.material(
                                seed,
                                baseX + localX,
                                y,
                                baseZ + localZ,
                                top,
                                bottom,
                                IslandSkin.exposed(y, west, east, north, south),
                                tip,
                                kind);
                        solidOrigins[index] = (byte) ((ground ? GROUND_ORIGIN : 0) | (floating ? FLOATING_ORIGIN : 0));
                    } else if (y <= context.getSeaLevel()) {
                        materials[index] = BiomicTerrainSampler.WATER;
                    }
                }
            }
        }
    }

    private static void validateChunkArrays(byte[] materials, byte[] solidOrigins) {
        if (materials == null || solidOrigins == null
            || materials.length != BiomicTerrainSampler.BLOCK_COUNT
            || solidOrigins.length != BiomicTerrainSampler.BLOCK_COUNT) {
            throw new IllegalArgumentException("Terrain material and origin arrays must cover one complete chunk");
        }
    }

    double sampleDensity(int blockX, int blockY, int blockZ) {
        return terrainGraph.sampleDensity(blockX, blockY, blockZ);
    }

    int sampleSurfaceHeight(int blockX, int blockZ, boolean natural) {
        TerrainColumn column = columnSampler.sample(blockX, blockZ);
        return natural ? column.getNaturalSurfaceHeight() : column.getSurfaceHeight();
    }

    /** A column's floating span, inside this chunk or (without ground clearance) just beside it. */
    private int floatingNeighbour(int[] spans, int baseX, int baseZ, int localX, int localZ) {
        return localX >= 0 && localX < BiomicTerrainSampler.CHUNK_WIDTH
            && localZ >= 0
            && localZ < BiomicTerrainSampler.CHUNK_WIDTH ? spans[localX * BiomicTerrainSampler.CHUNK_WIDTH + localZ]
                : terrainGraph.floatingSpan(baseX + localX, baseZ + localZ);
    }

    private int[][] createNearbyGroundCeilings(int chunkX, int chunkZ) {
        int[][] ceilings = new int[GRID_WIDTH][GRID_WIDTH];
        int worldStartX = chunkX << 4;
        int worldStartZ = chunkZ << 4;
        int[][] sampled = new int[CLEARANCE_GRID_WIDTH][CLEARANCE_GRID_WIDTH];
        for (int sampleX = 0; sampleX < CLEARANCE_GRID_WIDTH; sampleX++) {
            int blockX = worldStartX + (sampleX - CLEARANCE_GRID_RADIUS) * HORIZONTAL_GRID;
            for (int sampleZ = 0; sampleZ < CLEARANCE_GRID_WIDTH; sampleZ++) {
                int blockZ = worldStartZ + (sampleZ - CLEARANCE_GRID_RADIUS) * HORIZONTAL_GRID;
                sampled[sampleX][sampleZ] = columnSampler.sample(blockX, blockZ)
                    .getSurfaceHeight();
            }
        }
        for (int gridX = 0; gridX < GRID_WIDTH; gridX++) {
            for (int gridZ = 0; gridZ < GRID_WIDTH; gridZ++) {
                int maximum = Integer.MIN_VALUE;
                for (int offsetX = -CLEARANCE_GRID_RADIUS; offsetX <= CLEARANCE_GRID_RADIUS; offsetX++) {
                    for (int offsetZ = -CLEARANCE_GRID_RADIUS; offsetZ <= CLEARANCE_GRID_RADIUS; offsetZ++) {
                        maximum = Math.max(
                            maximum,
                            sampled[gridX + CLEARANCE_GRID_RADIUS + offsetX][gridZ + CLEARANCE_GRID_RADIUS + offsetZ]);
                    }
                }
                ceilings[gridX][gridZ] = maximum;
            }
        }
        return ceilings;
    }

    private static int nearbyGroundCeiling(int[][] ceilings, int localX, int localZ) {
        int gridX = localX / HORIZONTAL_GRID;
        int gridZ = localZ / HORIZONTAL_GRID;
        return Math.max(
            Math.max(ceilings[gridX][gridZ], ceilings[gridX + 1][gridZ]),
            Math.max(ceilings[gridX][gridZ + 1], ceilings[gridX + 1][gridZ + 1]));
    }

    private static final int FLOATING_SPAN_CACHE = 1024;
    private final java.util.LinkedHashMap<Long, int[]> floatingSpans = new java.util.LinkedHashMap<>(64, .75F, true);

    /**
     * The floating layer of one chunk exactly as {@link #fillBaseTerrain} lays it, per column (x * 16 + z): one span
     * from deck to keel (IslandKeel), kept clear of the ground below, packed as {@code bottom << 16 | top}, or -1.
     */
    int[] floatingSpans(int chunkX, int chunkZ) {
        long key = (long) chunkX << 32 ^ chunkZ & 0xFFFF_FFFFL;
        synchronized (floatingSpans) {
            int[] known = floatingSpans.get(key);
            if (known != null) return known;
        }
        int[][] ceilings = null;
        int[] spans = new int[BiomicTerrainSampler.CHUNK_WIDTH * BiomicTerrainSampler.CHUNK_WIDTH];
        for (int localX = 0; localX < BiomicTerrainSampler.CHUNK_WIDTH; localX++) {
            for (int localZ = 0; localZ < BiomicTerrainSampler.CHUNK_WIDTH; localZ++) {
                int span = terrainGraph.floatingSpan((chunkX << 4) + localX, (chunkZ << 4) + localZ);
                if (span >= 0) {
                    if (ceilings == null) ceilings = createNearbyGroundCeilings(chunkX, chunkZ);
                    // A keel is trimmed to keep clear of the ground below; a sliver is dropped.
                    int bottom = Math.max(
                        span >>> 16,
                        nearbyGroundCeiling(ceilings, localX, localZ) + FLOATING_GROUND_CLEARANCE + 1);
                    int top = span & 0xFFFF;
                    span = top - bottom < 2 ? -1 : bottom << 16 | top;
                }
                spans[localX * BiomicTerrainSampler.CHUNK_WIDTH + localZ] = span;
            }
        }
        synchronized (floatingSpans) {
            floatingSpans.put(key, spans);
            if (floatingSpans.size() > FLOATING_SPAN_CACHE) {
                var eldest = floatingSpans.keySet()
                    .iterator();
                eldest.next();
                eldest.remove();
            }
        }
        return spans;
    }
}
