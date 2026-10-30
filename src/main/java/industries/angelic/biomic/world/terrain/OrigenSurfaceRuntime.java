package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.internal.terra.noise.NoiseSampler;
import industries.angelic.biomic.world.WorldgenContext;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

/** Executes Origen's Terra 6.4.2 palette, ocean, and DotProduct slant configuration. */
final class OrigenSurfaceRuntime {

    /**
     * Biomic: half a Terra interpolation cell (Terra 0.55). The density is trilinear on a 4-block lattice, so its
     * gradient over ±0.55 is constant across each cell and jumps at every cell face: slant palettes came out in
     * straight-edged patches along the lattice, which runs along every chunk line (a calcite strip starting exactly at
     * a chunk's west edge). A difference over a whole cell is the gradient averaged over it, continuous everywhere.
     */
    private static final int SLANT_SAMPLE_DISTANCE = 2;
    private static final int SHARED_RUNTIME_COUNT = 4;
    private static final int AUTHORED_HEIGHT = OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE
        - OrigenDensityField.UPSTREAM_MIN_Y;
    private static final int NEGATIVE_DENSITY_STATE = 0;
    private static final int SOLID_DENSITY_STATE = 1;
    private static final int CARVED_DENSITY_STATE = 2;
    private static final int AUTHORED_CHUNK_CACHE_SIZE = 1_024;
    private static final OrigenBiomeProfile[] PROFILE_VALUES = OrigenBiomeProfile.values();
    private static final Map<OrigenDensityField.FieldKey, OrigenSurfaceRuntime> SHARED = new LinkedHashMap<OrigenDensityField.FieldKey, OrigenSurfaceRuntime>(
        SHARED_RUNTIME_COUNT,
        0.75F,
        true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<OrigenDensityField.FieldKey, OrigenSurfaceRuntime> eldest) {
            return size() > SHARED_RUNTIME_COUNT;
        }
    };

    private final SurfaceProfile[] profiles = new SurfaceProfile[PROFILE_VALUES.length];
    private final Map<String, Palette> palettes = new HashMap<String, Palette>();
    private final Map<OrigenBiomeProfile, List<String>> treeFeatures = new java.util.EnumMap<>(
        OrigenBiomeProfile.class);
    /** Immutable authored columns retained from pure terrain generation for the later Terra feature pass. */
    private final Long2ObjectLinkedOpenHashMap<byte[]> authoredChunkCache = new Long2ObjectLinkedOpenHashMap<>(
        AUTHORED_CHUNK_CACHE_SIZE);
    /** Fully overwritten on every pass; one buffer per terrain worker avoids a 98 KiB allocation per chunk. */
    private final ThreadLocal<short[]> authoredProfileScratch = ThreadLocal.withInitial(
        () -> new short[BiomicTerrainSampler.CHUNK_WIDTH * BiomicTerrainSampler.CHUNK_WIDTH * AUTHORED_HEIGHT]);
    private final ThreadLocal<byte[]> extrudedProfileScratch = ThreadLocal.withInitial(() -> new byte[AUTHORED_HEIGHT]);
    private final ThreadLocal<double[]> authoredDensityColumnScratch = ThreadLocal
        .withInitial(() -> new double[AUTHORED_HEIGHT]);
    private final ThreadLocal<double[]> authoredCarverColumnScratch = ThreadLocal
        .withInitial(() -> new double[AUTHORED_HEIGHT]);
    private final OrigenDensityField densityField;
    private final OrigenSmallOrphanFilter smallOrphanFilter;
    private final long worldSeed;
    /** Rock pockets Origen's carver leaves solid (mineshafts stand in them). */
    private final CavePockets pockets;
    private final int authoredCacheLimit;

    OrigenSurfaceRuntime(WorldgenContext context) {
        this(context, AUTHORED_CHUNK_CACHE_SIZE);
    }

    OrigenSurfaceRuntime(WorldgenContext context, int cacheLimit) {
        if (context == null) {
            throw new IllegalArgumentException("Worldgen context cannot be null");
        }
        worldSeed = context.getSettings()
            .getWorldSeed();
        pockets = new CavePockets(worldSeed);
        if (PROFILE_VALUES.length * 3 > 0x10000) {
            throw new IllegalStateException("Origen density/profile encoding requires at most 21845 profiles");
        }
        if (cacheLimit < 1 || cacheLimit > AUTHORED_CHUNK_CACHE_SIZE)
            throw new IllegalArgumentException("Invalid authored cache limit");
        authoredCacheLimit = cacheLimit;

        densityField = OrigenDensityField.shared(context);
        smallOrphanFilter = new OrigenSmallOrphanFilter(densityField);
        load();
    }

    static OrigenSurfaceRuntime shared(WorldgenContext context) {
        OrigenDensityField.FieldKey seed = OrigenDensityField.key(context);
        synchronized (SHARED) {
            OrigenSurfaceRuntime runtime = SHARED.get(seed);
            if (runtime == null) {
                runtime = new OrigenSurfaceRuntime(context);
                SHARED.put(seed, runtime);
            }
            return runtime;
        }
    }

    /** The top block of a profile's ground palette at one authored-height point (its flat, unslanted surface). */
    byte groundTop(OrigenBiomeProfile profile, int x, int authoredY, int z) {
        return profiles[profile.ordinal()].material(0, x, authoredY, z, worldSeed, Double.NaN);
    }

    /** The profile's tree features (names such as SPRUCE_TREES), as Origen authors them. */
    List<String> treeFeatures(OrigenBiomeProfile profile) {
        return treeFeatures.get(profile);
    }

    void apply(byte[] materials, byte[] solidOrigins, int chunkX, int chunkZ) {
        apply(materials, solidOrigins, chunkX, chunkZ, false);
    }

    /** Projects strict Origen terrain without first materializing an equivalent physical stone/water raster. */
    void applyStrict(byte[] materials, int chunkX, int chunkZ) {
        apply(materials, null, chunkX, chunkZ, true);
    }

    private void apply(byte[] materials, byte[] solidOrigins, int chunkX, int chunkZ, boolean strict) {
        int worldStartX = chunkX << 4;
        int worldStartZ = chunkZ << 4;
        OrigenDensityField.ScaledChunkDensity chunkDensity = densityField.createScaledChunkDensity(chunkX, chunkZ);
        OrigenDensityField.UpstreamChunkCarver chunkCarver = densityField.chunkCarver(chunkX, chunkZ);
        short[] authoredProfiles = authoredProfileScratch.get();
        byte[] authoredChunk = fillAuthoredChunk(chunkX, chunkZ, chunkDensity, chunkCarver, authoredProfiles);
        smallOrphanFilter.cull(authoredChunk, authoredProfiles, materials, solidOrigins, chunkX, chunkZ);
        double[] walls = shapeSinkholeWalls(authoredChunk, authoredProfiles, chunkX, chunkZ);
        raiseSeaRims(authoredChunk, authoredProfiles, walls, chunkX, chunkZ);
        sealSeaCaves(authoredChunk, authoredProfiles, chunkX, chunkZ);
        for (int localX = 0; localX < BiomicTerrainSampler.CHUNK_WIDTH; localX++) {
            int worldX = worldStartX + localX;
            for (int localZ = 0; localZ < BiomicTerrainSampler.CHUNK_WIDTH; localZ++) {
                int worldZ = worldStartZ + localZ;
                int authoredOffset = authoredColumnOffset(localX, localZ);
                int paletteLevel = 0;
                OrigenBiomeProfile columnSurface = densityField.sampleSurfaceProfile(worldX, worldZ);
                SurfaceProfile columnSkin = profile(skinProfile(columnSurface, worldX, worldZ));
                for (int y = BiomicTerrainSampler.WORLD_HEIGHT - 1; y >= 0; y--) {
                    int index = BiomicTerrainSampler.index(localX, y, localZ);
                    int upstreamY = (int) Math.round(OrigenDensityField.toUpstreamY(y));
                    int encodedProfile = authoredProfiles[authoredOffset + upstreamY
                        - OrigenDensityField.UPSTREAM_MIN_Y] & 0xFFFF;
                    boolean origenSolid = strict ? isSolidAtPhysicalY(encodedProfile, chunkDensity, localX, y, localZ)
                        : (solidOrigins[index] & TerrainDensitySampler.GROUND_ORIGIN) != 0;
                    boolean customFloating = !strict
                        && (solidOrigins[index] & TerrainDensitySampler.FLOATING_ORIGIN) != 0
                        && !origenSolid;
                    if (y <= OrigenDensityField.BIOMIC_SEA_LEVEL && !customFloating) {
                        // The underground transform samples every other authored layer. Palette depth still advances
                        // through the skipped solid layer in Terra, so replay the complete authored column instead
                        // of halving deep surface bands such as Abyssal Alleys' dripstone backing.
                        int projectedAuthoredY = preserveCollapsedDrySurface(
                            authoredChunk,
                            authoredProfiles,
                            authoredOffset,
                            y,
                            upstreamY);
                        materials[index] = authoredChunk[authoredOffset + projectedAuthoredY
                            - OrigenDensityField.UPSTREAM_MIN_Y];
                        continue;
                    }
                    OrigenBiomeProfile biome = decodeProfile(encodedProfile);
                    SurfaceProfile surface = profile(biome);
                    SurfaceProfile palettes = biome == columnSurface ? columnSkin : surface;
                    if (origenSolid) {
                        if (strict && isCarvedAtPhysicalY(encodedProfile, chunkCarver, localX, y, localZ)) {
                            materials[index] = BiomicTerrainSampler.AIR;
                            paletteLevel = surface.updatePaletteWhenCarving ? 0 : paletteLevel + 1;
                            continue;
                        }
                        if (!strict && !isUncarvedBase(materials[index])) {
                            paletteLevel = surface.updatePaletteWhenCarving ? 0 : paletteLevel + 1;
                            continue;
                        }
                        double slant = palettes.hasApplicableSlant(paletteLevel)
                            ? calculateSlant(chunkDensity, worldX, upstreamY, worldZ, localX, localZ)
                            : Double.NaN;
                        materials[index] = palettes.material(paletteLevel, worldX, upstreamY, worldZ, worldSeed, slant);
                        paletteLevel++;
                    } else if (customFloating) {
                        // Third-party floating layers are deliberately outside the Origen baseline.
                        paletteLevel = 0;
                    } else {
                        if (surface.hasOcean() && upstreamY <= surface.seaLevel
                            && materials[index] == BiomicTerrainSampler.WATER) {
                            materials[index] = surface.ocean
                                .get(surface.seaLevel - upstreamY, worldX, upstreamY, worldZ, worldSeed);
                        }
                        paletteLevel = 0;
                    }
                }
                sealSqueezedSeaCaves(materials, localX, localZ);
                if (!strict) settleSubmergedGrass(materials, localX, localZ);
            }
        }
        if (!strict) {
            // Features read this as the chunk's authored terrain, as they read createAuthoredChunk's for a chunk the
            // terrain never computed: the projection culled crumbs only up to its visual ceiling, Origen's own copy
            // culls them all, and which of the two a feature saw would depend on which chunks were computed.
            authoredChunk = authoredChunk.clone();
            smallOrphanFilter.cullAuthored(authoredChunk, authoredProfiles.clone(), chunkX, chunkZ);
        }
        rememberAuthoredChunk(chunkX, chunkZ, authoredChunk);
    }

    /**
     * Where Origen's density is positive, per authored cell in createAuthoredChunk's layout: rock, or a cave Origen's
     * own carver opened. Air outside it is open air: the sky, the sea, the hollow under an arch or overhang.
     */
    java.util.BitSet groundMask(int chunkX, int chunkZ) {
        OrigenDensityField.ScaledChunkDensity density = densityField.createScaledChunkDensity(chunkX, chunkZ);
        double[] column = authoredDensityColumnScratch.get();
        java.util.BitSet ground = new java.util.BitSet(
            BiomicTerrainSampler.CHUNK_WIDTH * BiomicTerrainSampler.CHUNK_WIDTH * AUTHORED_HEIGHT);
        // What a rebuilt sinkhole wall cut away is open air, as the authored chunk has it.
        double[] walls = sinkholeWallHeights(chunkX, chunkZ);
        for (int localX = 0; localX < BiomicTerrainSampler.CHUNK_WIDTH; localX++) {
            for (int localZ = 0; localZ < BiomicTerrainSampler.CHUNK_WIDTH; localZ++) {
                density.fillUpstreamColumn(localX, localZ, column);
                int offset = authoredColumnOffset(localX, localZ);
                int ceiling = AUTHORED_HEIGHT;
                if (walls != null) {
                    int row = wallRow(walls, chunkX, chunkZ, localX, localZ, column);
                    if (row >= 0) ceiling = row + 1;
                }
                for (int row = 0; row < ceiling; row++)
                    if (column[row] > 0.0D && !sliver(column, row)) ground.set(offset + row);
            }
        }
        return ground;
    }

    /**
     * Immutable, complete authored pre-feature terrain. Shares the existing palette/carver/orphan pipeline,
     * but never allocates or indexes a compressed physical raster. Callers must not mutate the cached result.
     */
    byte[] createAuthoredChunk(int chunkX, int chunkZ) {
        byte[] cached = cachedAuthoredChunk(chunkX, chunkZ);
        if (cached != null) return cached;
        long started = System.nanoTime();
        try {
            return createUncachedAuthoredChunk(chunkX, chunkZ);
        } finally {
            industries.angelic.biomic.world.SlowSteps.note("authored chunk", System.nanoTime() - started);
        }
    }

    private byte[] createUncachedAuthoredChunk(int chunkX, int chunkZ) {
        short[] profiles = authoredProfileScratch.get();
        byte[] materials = fillAuthoredChunk(
            chunkX,
            chunkZ,
            densityField.createScaledChunkDensity(chunkX, chunkZ),
            densityField.chunkCarver(chunkX, chunkZ),
            profiles);
        smallOrphanFilter.cullAuthored(materials, profiles, chunkX, chunkZ);
        double[] walls = shapeSinkholeWalls(materials, profiles, chunkX, chunkZ);
        raiseSeaRims(materials, profiles, walls, chunkX, chunkZ);
        sealSeaCaves(materials, profiles, chunkX, chunkZ);
        rememberAuthoredChunk(chunkX, chunkZ, materials);
        return materials;
    }

    private byte[] fillAuthoredChunk(int chunkX, int chunkZ, OrigenDensityField.ScaledChunkDensity density,
        OrigenDensityField.UpstreamChunkCarver carver, short[] profiles) {
        byte[] materials = new byte[BiomicTerrainSampler.CHUNK_WIDTH * BiomicTerrainSampler.CHUNK_WIDTH
            * AUTHORED_HEIGHT];
        double[] densityColumn = authoredDensityColumnScratch.get();
        double[] carverColumn = authoredCarverColumnScratch.get();
        for (int localX = 0; localX < BiomicTerrainSampler.CHUNK_WIDTH; localX++) {
            for (int localZ = 0; localZ < BiomicTerrainSampler.CHUNK_WIDTH; localZ++) {
                fillAuthoredMaterialColumn(
                    (chunkX << 4) + localX,
                    (chunkZ << 4) + localZ,
                    materials,
                    profiles,
                    authoredColumnOffset(localX, localZ),
                    density,
                    carver,
                    densityColumn,
                    carverColumn,
                    localX,
                    localZ);
            }
        }
        return materials;
    }

    /**
     * Underground the compressed projection samples every other authored row, so it can land the authored air row
     * below a sealed one directly under the sea's water. Close that cell too.
     */
    private static void sealSqueezedSeaCaves(byte[] materials, int localX, int localZ) {
        for (int y = 1; y < OrigenDensityField.BIOMIC_SEA_LEVEL; y++) {
            int index = BiomicTerrainSampler.index(localX, y, localZ);
            if (materials[index] == BiomicTerrainSampler.AIR
                && materials[BiomicTerrainSampler.index(localX, y + 1, localZ)] == BiomicTerrainSampler.WATER) {
                materials[index] = BiomicTerrainSampler.STONE;
            }
        }
    }

    /**
     * Grass is a dry exposed top material. At a profile/coast transition Origen can select a land palette for the
     * floor while the neighboring ocean palette still supplies water above it. Retain the terrain and water exactly;
     * only settle that directly submerged cap to its ordinary dirt backing. Strict Origen replay stays byte-exact.
     */
    private static void settleSubmergedGrass(byte[] materials, int localX, int localZ) {
        for (int y = 0; y + 1 < BiomicTerrainSampler.WORLD_HEIGHT; y++) {
            int index = BiomicTerrainSampler.index(localX, y, localZ);
            if (materials[index] == BiomicTerrainSampler.GRASS
                && materials[BiomicTerrainSampler.index(localX, y + 1, localZ)] == BiomicTerrainSampler.WATER) {
                materials[index] = BiomicTerrainSampler.DIRT;
            }
        }
    }

    /**
     * Keeps an odd authored surface layer when the underground two-to-one squeeze folds it together with dry air.
     * Fluids retain priority, and a carved cell with solid terrain immediately above is left open, so this cannot
     * fill underwater floors or close a one-block cave merely to preserve its lower wall.
     */
    private static int preserveCollapsedDrySurface(byte[] authoredMaterials, short[] authoredProfiles, int offset,
        int physicalY, int selectedAuthoredY) {
        int selectedIndex = offset + selectedAuthoredY - OrigenDensityField.UPSTREAM_MIN_Y;
        if (authoredMaterials[selectedIndex] != BiomicTerrainSampler.AIR
            || densityState(authoredProfiles[selectedIndex]) != NEGATIVE_DENSITY_STATE) return selectedAuthoredY;

        int lowerAuthoredY = selectedAuthoredY - 1;
        int upperAuthoredY = selectedAuthoredY + 1;
        if (lowerAuthoredY < OrigenDensityField.UPSTREAM_MIN_Y
            || upperAuthoredY >= OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE
            || OrigenVerticalProfileSampler.toBiomicBlockY(lowerAuthoredY) != physicalY) return selectedAuthoredY;

        int lowerIndex = offset + lowerAuthoredY - OrigenDensityField.UPSTREAM_MIN_Y;
        int upperIndex = offset + upperAuthoredY - OrigenDensityField.UPSTREAM_MIN_Y;
        return densityState(authoredProfiles[lowerIndex]) == SOLID_DENSITY_STATE
            && densityState(authoredProfiles[upperIndex]) == NEGATIVE_DENSITY_STATE ? lowerAuthoredY
                : selectedAuthoredY;
    }

    private static int densityState(short encodedProfile) {
        return (encodedProfile & 0xFFFF) / PROFILE_VALUES.length;
    }

    /** Complete pre-feature Terra column, preserving palette depth across vertically skipped authored layers. */
    byte[] createAuthoredMaterialColumn(int worldX, int worldZ) {
        byte[] materials = new byte[AUTHORED_HEIGHT];
        fillAuthoredMaterialColumn(worldX, worldZ, materials, null, 0, null, null, null, null, 0, 0);
        return materials;
    }

    private void fillAuthoredMaterialColumn(int worldX, int worldZ, byte[] materials, short[] authoredProfiles,
        int offset, OrigenDensityField.ScaledChunkDensity chunkDensity,
        OrigenDensityField.UpstreamChunkCarver chunkCarver, double[] densityColumn, double[] carverColumn, int localX,
        int localZ) {
        int paletteLevel = 0;
        if (chunkDensity != null) {
            chunkDensity.fillUpstreamColumn(localX, localZ, densityColumn);
            chunkCarver.fillIntegerColumn(localX, localZ, densityColumn, carverColumn);
        }
        OrigenBiomeProfile surfaceProfile = densityField.sampleSurfaceProfile(worldX, worldZ);
        SurfaceProfile skin = profile(skinProfile(surfaceProfile, worldX, worldZ));
        int pocket = pockets.range(worldX, worldZ);
        int pocketLow = pocket < 0 ? Integer.MAX_VALUE : CavePockets.low(pocket),
            pocketHigh = pocket < 0 ? 0 : CavePockets.high(pocket);
        boolean[] pocketFill = pocket < 0 ? null
            : pocketFill(worldX, worldZ, pocketLow, pocketHigh, densityColumn, carverColumn);
        if (authoredProfiles != null) {
            byte[] extruded = extrudedProfileScratch.get();
            densityField.fillExtrudedProfiles(surfaceProfile, worldX, worldZ, extruded, 0);
            for (int i = 0; i < AUTHORED_HEIGHT; i++) authoredProfiles[offset + i] = (short) (extruded[i] & 0xFF);
        }
        for (int authoredY = OrigenDensityField.UPSTREAM_MAX_Y_EXCLUSIVE - 1; authoredY
            >= OrigenDensityField.UPSTREAM_MIN_Y; authoredY--) {
            int index = offset + authoredY - OrigenDensityField.UPSTREAM_MIN_Y;
            OrigenBiomeProfile biome = authoredProfiles == null
                ? densityField.extrudeProfile(surfaceProfile, worldX, authoredY, worldZ)
                : PROFILE_VALUES[authoredProfiles[index] & 0xFFFF];
            SurfaceProfile surface = profile(biome);
            SurfaceProfile palettes = biome == surfaceProfile ? skin : surface;
            int densityState = NEGATIVE_DENSITY_STATE;
            double density = densityColumn == null ? densityField.sampleUpstreamInterpolated(worldX, authoredY, worldZ)
                : densityColumn[authoredY - OrigenDensityField.UPSTREAM_MIN_Y];
            if (density > 0.0D
                && (densityColumn != null ? sliver(densityColumn, authoredY - OrigenDensityField.UPSTREAM_MIN_Y)
                    : densityField.sampleUpstreamInterpolated(worldX, authoredY + 1, worldZ) <= 0.0D
                        && densityField.sampleUpstreamInterpolated(worldX, authoredY - 1, worldZ) <= 0.0D))
                density = 0.0D;
            boolean pocketRock = pocketFill != null && authoredY >= pocketLow
                && authoredY <= pocketHigh
                && pocketFill[authoredY - pocketLow + 1];
            if (density <= 0.0D && pocketRock) density = POCKET_DENSITY;
            if (density > 0.0D) {
                double carving = carverColumn == null
                    ? densityField.sampleUpstreamCarverInterpolated(worldX, authoredY, worldZ)
                    : carverColumn[authoredY - OrigenDensityField.UPSTREAM_MIN_Y];
                if (carving <= 0.0D || pocketRock) {
                    densityState = SOLID_DENSITY_STATE;
                    double slant = palettes.hasApplicableSlant(paletteLevel)
                        ? calculateSlant(chunkDensity, worldX, authoredY, worldZ, localX, localZ)
                        : Double.NaN;
                    materials[index] = palettes.material(paletteLevel, worldX, authoredY, worldZ, worldSeed, slant);
                    paletteLevel++;
                } else if (surface.updatePaletteWhenCarving) {
                    densityState = CARVED_DENSITY_STATE;
                    paletteLevel = 0;
                } else {
                    densityState = CARVED_DENSITY_STATE;
                    paletteLevel++;
                }
            } else {
                if (surface.hasOcean() && authoredY <= surface.seaLevel) {
                    materials[index] = surface.ocean
                        .get(surface.seaLevel - authoredY, worldX, authoredY, worldZ, worldSeed);
                }
                paletteLevel = 0;
            }
            if (authoredProfiles != null) {
                authoredProfiles[index] = (short) (biome.ordinal() + densityState * PROFILE_VALUES.length);
            }
        }
    }

    /** The density a pocket gives the caverns it fills. */
    private static final double POCKET_DENSITY = 1.0D;
    /** Rows from a pocket band's low - 1 to its high + 1 (CavePockets bands are at most 73 rows). */
    private final ThreadLocal<boolean[]> pocketScratch = ThreadLocal.withInitial(() -> new boolean[128]);

    /**
     * Which rows of a column's pocket band the pocket fills, indexed from the band's low - 1: the openings (Origen's
     * caverns, density zero or below, and its carver's caves) whose whole run lies inside the band, capped and floored
     * by rock, so caves the pocket's rock encloses. A run that leaves the band, up into a sinkhole, canyon or the sky,
     * or down to a basin's floor under an overhang, stays open: filled, it hung a ball of rock in the air (review,
     * 2026-10-24: "can you explain this large ball?").
     */
    private boolean[] pocketFill(int worldX, int worldZ, int low, int high, double[] densityColumn,
        double[] carverColumn) {
        boolean[] fill = pocketScratch.get();
        int rows = high - low + 3;
        for (int i = 0; i < rows; i++) fill[i] = open(worldX, low - 1 + i, worldZ, densityColumn, carverColumn);
        for (int start = 0; start < rows;) {
            if (!fill[start]) {
                start++;
                continue;
            }
            int end = start;
            while (end + 1 < rows && fill[end + 1]) end++;
            boolean enclosed = start > 0 && end < rows - 1;
            if (!enclosed) for (int i = start; i <= end; i++) fill[i] = false;
            start = end + 1;
        }
        return fill;
    }

    /** Whether a cell is open in Origen's terrain: no density there (or a lone sliver), or carved by its carver. */
    private boolean open(int worldX, int authoredY, int worldZ, double[] densityColumn, double[] carverColumn) {
        int row = authoredY - OrigenDensityField.UPSTREAM_MIN_Y;
        boolean solid = densityColumn != null ? densityColumn[row] > 0.0D && !sliver(densityColumn, row)
            : densityField.sampleUpstreamInterpolated(worldX, authoredY, worldZ) > 0.0D
                && (densityField.sampleUpstreamInterpolated(worldX, authoredY + 1, worldZ) > 0.0D
                    || densityField.sampleUpstreamInterpolated(worldX, authoredY - 1, worldZ) > 0.0D);
        if (!solid) return true;
        return (carverColumn != null ? carverColumn[row]
            : densityField.sampleUpstreamCarverInterpolated(worldX, authoredY, worldZ)) > 0.0D;
    }

    /**
     * A positive cell with no positive density above or below it: a single lattice node of Terra's 4-block
     * interpolation that is barely positive while its neighbours are not leaves a sheet one block thick, and a row of
     * them a long floating line (review: "an unnatural long line" at -6292 252 19019, density +0.21 between -0.97 and
     * -0.63). It is air, as its neighbours are.
     */
    private static boolean sliver(double[] column, int row) {
        return (row == 0 || column[row - 1] <= 0.0D) && (row + 1 >= column.length || column[row + 1] <= 0.0D);
    }

    /** The profile whose palettes skin a column ({@link SkinJitter}); the column keeps its own everywhere else. */
    private OrigenBiomeProfile skinProfile(OrigenBiomeProfile own, int worldX, int worldZ) {
        if (!SkinJitter.origenLand(own)) return own;
        int x = SkinJitter.x(worldSeed, worldX, worldZ), z = SkinJitter.z(worldSeed, worldX, worldZ);
        if (x == worldX && z == worldZ) return own;
        OrigenBiomeProfile other = densityField.sampleSurfaceProfile(x, z);
        return SkinJitter.origenLand(other) ? other : own;
    }

    private static OrigenBiomeProfile decodeProfile(int encodedProfile) {
        return PROFILE_VALUES[encodedProfile % PROFILE_VALUES.length];
    }

    private static boolean isSolidAtPhysicalY(int encodedProfile, OrigenDensityField.ScaledChunkDensity chunkDensity,
        int localX, int physicalY, int localZ) {
        if (physicalY <= OrigenVerticalTransform.PHYSICAL_VISUAL_CEILING) {
            return encodedProfile / PROFILE_VALUES.length != NEGATIVE_DENSITY_STATE;
        }
        return chunkDensity.sample(localX, physicalY, localZ) > 0.0D;
    }

    private static boolean isCarvedAtPhysicalY(int encodedProfile, OrigenDensityField.UpstreamChunkCarver chunkCarver,
        int localX, int physicalY, int localZ) {
        if (physicalY <= OrigenVerticalTransform.PHYSICAL_VISUAL_CEILING) {
            return encodedProfile / PROFILE_VALUES.length == CARVED_DENSITY_STATE;
        }
        return chunkCarver.sample(localX, OrigenDensityField.toUpstreamY(physicalY), localZ) > 0.0D;
    }

    byte[] cachedAuthoredChunk(int chunkX, int chunkZ) {
        synchronized (authoredChunkCache) {
            return authoredChunkCache.getAndMoveToLast(horizontalKey(chunkX, chunkZ));
        }
    }

    /**
     * Restores an immutable authored chunk at demand time. Pure terrain can be calculated hundreds of chunks ahead
     * of population, so computation-order LRU alone may evict precisely the old result the server is about to use.
     */
    void rememberAuthoredChunk(int chunkX, int chunkZ, byte[] authoredChunk) {
        if (authoredChunk == null) return;
        if (authoredChunk.length
            != BiomicTerrainSampler.CHUNK_WIDTH * BiomicTerrainSampler.CHUNK_WIDTH * AUTHORED_HEIGHT) {
            throw new IllegalArgumentException("Authored Origen terrain must contain exactly one chunk");
        }
        synchronized (authoredChunkCache) {
            authoredChunkCache.putAndMoveToLast(horizontalKey(chunkX, chunkZ), authoredChunk);
            if (authoredChunkCache.size() > authoredCacheLimit) authoredChunkCache.removeFirst();
        }
    }

    static int authoredMaterialIndex(int localX, int authoredY, int localZ) {
        return authoredColumnOffset(localX, localZ) + authoredY - OrigenDensityField.UPSTREAM_MIN_Y;
    }

    /**
     * Terra fills the ocean per 3D biome and keeps carved cells dry, so where an Origen cave (a Vine Vault, say)
     * meets the sea floor the water rests on the cave's open roof and its floor flora shows through. Dry cells at or
     * below sea level with ocean water above or beside them, open or carved, stay stone instead. Cells across the
     * chunk border are tested against Origen's density, so both chunks of a border agree. A grass or moss cave floor
     * left directly under the sea settles to dirt, like the 256 path's submerged grass.
     */
    private void sealSeaCaves(byte[] materials, short[] profiles, int chunkX, int chunkZ) {
        int states = PROFILE_VALUES.length;
        for (int localX = 0; localX < BiomicTerrainSampler.CHUNK_WIDTH; localX++) {
            for (int localZ = 0; localZ < BiomicTerrainSampler.CHUNK_WIDTH; localZ++) {
                int offset = authoredColumnOffset(localX, localZ);
                for (int authoredY = OrigenDensityField.UPSTREAM_MIN_Y + 1; authoredY < SEA_CAVE_CEILING; authoredY++) {
                    int index = offset + authoredY - OrigenDensityField.UPSTREAM_MIN_Y;
                    if ((materials[index] == BiomicTerrainSampler.GRASS
                        || materials[index] == BiomicTerrainSampler.MOSS)
                        && materials[index + 1] == BiomicTerrainSampler.WATER) {
                        materials[index] = BiomicTerrainSampler.DIRT;
                        continue;
                    }
                    if (materials[index] != BiomicTerrainSampler.AIR
                        || (profiles[index] & 0xFFFF) / states == SOLID_DENSITY_STATE) continue;
                    if (materials[index + 1] == BiomicTerrainSampler.WATER
                        || seaWater(materials, chunkX, chunkZ, localX - 1, authoredY, localZ)
                        || seaWater(materials, chunkX, chunkZ, localX + 1, authoredY, localZ)
                        || seaWater(materials, chunkX, chunkZ, localX, authoredY, localZ - 1)
                        || seaWater(materials, chunkX, chunkZ, localX, authoredY, localZ + 1))
                        materials[index] = BiomicTerrainSampler.STONE;
                }
            }
        }
    }

    /**
     * A sinkhole's walls came from Terra's blend: the floor's density is clamped to ±5 and averaged, lattice node by
     * lattice node over a 7 by 7 stencil, with the rim's unclamped -y + 67, so a wall's height is a hyperbola of the
     * rim's share of the stencil, 67 - 5 (1 - w) / w: flat while the rim dominates, then 100 blocks down within one
     * 4-block lattice cell. Trilinear interpolation cuts that plunge into planar facets cell by cell, and a facet that
     * steep shows as ragged one-block pillars of every height ("a bombed hospital", review 2026-10-24). Here the wall
     * is rebuilt from heights instead: a smooth distance into the basin (a chamfer transform of Terra's 4-block blend
     * grid, quadratic B-spline between nodes) wandering on low-frequency noise, a fall from the neighbouring habitats'
     * own ground at a steady slope down to the floor, and terraces ({@link #terraced}) that turn the fall into rock
     * ledges and risers, the construct of Origen's own canyon walls. Every input is a pure function of the seed and the
     * position, so a column comes out the same in any chunk. The band begins WALL_OUTER blocks outside the floor's
     * edge with a plateau at the neighbouring habitats' height, which the wall itself leaves to the land there but the
     * sea's rim holds its crest to ({@link #raiseSeaRims}), and the fall begins WALL_PLATEAU blocks later. Returns,
     * over
     * the chunk and a margin of WALL_MARGIN, four layers (see WALL, BAND, HIGH, LOW): the wall's height (NaN at its
     * foot,
     * where it leaves the floor alone), how far into the band a cell lies, the neighbouring habitats' height and the
     * floor's (all NaN outside the band); or null when no sinkhole edge is near.
     */
    private double[] sinkholeWallHeights(int chunkX, int chunkZ) {
        // Terra's blend grid around the chunk, out to WALL_REACH: which nodes are sinkhole floor.
        int reach = WALL_REACH / 4, width = 5 + 2 * reach, gx0 = chunkX * 4 - reach, gz0 = chunkZ * 4 - reach;
        byte[] grid = new byte[width * width];
        boolean inside = false, outside = false;
        for (int i = 0; i < width; i++) for (int j = 0; j < width; j++) {
            int ordinal = gridProfile(gx0 + i, gz0 + j);
            grid[i * width + j] = (byte) ordinal;
            if (sinkholeFloor(PROFILE_VALUES[ordinal])) inside = true;
            else outside = true;
        }
        if (!inside || !outside) return null;
        // Each node's distance to the nearest node across the edge (the edge runs midway between unlike nodes). A node
        // within reach - 1 grid steps of it finds it inside this window, so every chunk agrees on the distances that
        // shape a wall; the walls end at the floor well before that.
        double[] toOutside = new double[width * width], toInside = new double[width * width];
        for (int c = 0; c < width * width; c++) {
            boolean floor = sinkholeFloor(PROFILE_VALUES[grid[c]]);
            toOutside[c] = floor ? FAR : 0.0D;
            toInside[c] = floor ? 0.0D : FAR;
        }
        chamfer(toOutside, width);
        chamfer(toInside, width);
        // The 7 by 7 nodes the spline reads (blocks -4..20 of the chunk): the signed distance into the basin in blocks,
        // the floor's height, and the tent-weighted mean of the other habitats' own ground heights (the sea counting as
        // its shoreline). All three sinkhole floors share Origen's SINKHOLE_BASE graph.
        int nodes = 7, base = reach - 1, radius = WALL_RADIUS;
        double[] signed = new double[nodes * nodes], floor = new double[nodes * nodes],
            outer = new double[nodes * nodes];
        double[] byProfile = new double[PROFILE_VALUES.length];
        for (int ni = 0; ni < nodes; ni++) for (int nj = 0; nj < nodes; nj++) {
            int c = (base + ni) * width + base + nj, node = ni * nodes + nj;
            signed[node] = sinkholeFloor(PROFILE_VALUES[grid[c]]) ? toOutside[c] * 4.0D - 2.0D
                : 2.0D - toInside[c] * 4.0D;
            java.util.Arrays.fill(byProfile, 0.0D);
            for (int di = -radius; di <= radius; di++) for (int dj = -radius; dj <= radius; dj++) {
                int ordinal = grid[(base + ni + di) * width + base + nj + dj];
                if (!sinkholeFloor(PROFILE_VALUES[ordinal]))
                    byProfile[ordinal] += (radius + 1 - Math.abs(di)) * (radius + 1 - Math.abs(dj));
            }
            int x = (gx0 + base + ni) << 2, z = (gz0 + base + nj) << 2;
            floor[node] = densityField.sampleProfileGround(OrigenBiomeProfile.SINKHOLE_JUNGLE, x, z, -40);
            double outerWeight = 0.0D, outerHeight = 0.0D;
            for (int ordinal = 0; ordinal < byProfile.length; ordinal++) {
                if (byProfile[ordinal] == 0.0D) continue;
                OrigenBiomeProfile other = PROFILE_VALUES[ordinal];
                double ground = densityField
                    .sampleProfileGround(other, x, z, OrigenDensityField.UPSTREAM_SEA_LEVEL + 5);
                if (profile(other).hasOcean()) ground = Math.max(ground, OrigenDensityField.UPSTREAM_SEA_LEVEL + 1.0D);
                outerHeight += ground * byProfile[ordinal];
                outerWeight += byProfile[ordinal];
            }
            outer[node] = outerWeight > 0.0D ? outerHeight / outerWeight : Double.NaN;
        }
        // The fall: steeper where it has further to go, so it always meets the floor within the tent's reach.
        int span = 16 + 2 * WALL_MARGIN, cells = span * span;
        double[] raw = new double[cells], walls = new double[4 * cells];
        java.util.Arrays.fill(walls, Double.NaN);
        for (int a = 0; a < span; a++) for (int b = 0; b < span; b++) {
            int cell = a * span + b, x = (chunkX << 4) + a - WALL_MARGIN, z = (chunkZ << 4) + b - WALL_MARGIN;
            double tx = (a - WALL_MARGIN) / 4.0D + 1.0D, tz = (b - WALL_MARGIN) / 4.0D + 1.0D;
            double t = spline(signed, nodes, tx, tz) + WALL_WANDER * wallWander.getSample(worldSeed, x, z) - WALL_OUTER;
            double high = spline(outer, nodes, tx, tz), bottom = spline(floor, nodes, tx, tz);
            raw[cell] = Double.NaN;
            if (t <= 0.0D || Double.isNaN(high)) continue;
            double slope = Math.max(WALL_SLOPE_MIN + WALL_SLOPE * unit(wallSlope, x, z), (high - bottom) / WALL_FALL);
            raw[cell] = high - slope * Math.max(0.0D, t - WALL_PLATEAU);
            walls[BAND * cells + cell] = t;
            walls[HIGH * cells + cell] = high;
            walls[LOW * cells + cell] = bottom;
        }
        boolean changes = false;
        for (int a = 0; a < span; a++) for (int b = 0; b < span; b++) {
            int cell = a * span + b;
            if (Double.isNaN(raw[cell])) continue;
            double fall = walls[BAND * cells + cell] - WALL_PLATEAU;
            double weight = terraceWeight(slope(raw, span, a, b)) * smooth01(fall / 3.0D);
            double wall = terraced((chunkX << 4) + a - WALL_MARGIN, (chunkZ << 4) + b - WALL_MARGIN, raw[cell], weight);
            // At the foot the basin's own floor (and whatever its carver opened) stays as it is.
            if (wall <= walls[LOW * cells + cell] + WALL_FOOT) continue;
            walls[WALL * cells + cell] = wall;
            changes = true;
        }
        return changes ? walls : null;
    }

    /**
     * The authored row a rebuilt wall puts a column's ground at, or -1 where it leaves the column alone (no wall there,
     * the plateau, or the sea's own habitat). The fall eases in from the column's own ground (Origen's density, as
     * {@link #groundMask} reads it) over its first WALL_BLEND blocks, so the wall's top meets the land outside it.
     */
    private int wallRow(double[] walls, int chunkX, int chunkZ, int localX, int localZ, double[] densityColumn) {
        int span = 16 + 2 * WALL_MARGIN, cells = span * span,
            cell = (localX + WALL_MARGIN) * span + localZ + WALL_MARGIN;
        double wall = walls[WALL * cells + cell];
        if (Double.isNaN(wall) || walls[BAND * cells + cell] < WALL_PLATEAU
            || profile(densityField.sampleSurfaceProfile((chunkX << 4) + localX, (chunkZ << 4) + localZ)).hasOcean())
            return -1;
        int top = AUTHORED_HEIGHT - 1;
        while (top > 0 && !(densityColumn[top] > 0.0D && !sliver(densityColumn, top))) top--;
        double own = top + 0.5D + OrigenDensityField.UPSTREAM_MIN_Y;
        double ground = own + (wall - own) * smooth01((walls[BAND * cells + cell] - WALL_PLATEAU) / WALL_BLEND);
        return Math.max(1, (int) Math.floor(ground) - OrigenDensityField.UPSTREAM_MIN_Y);
    }

    /**
     * The height a rebuilt wall's face reaches down to at a cell, for the solid skin of the columns beside it: the wall
     * in the fall, the floor at its foot, and nothing lower (infinity) on the plateau or outside the band.
     */
    private static double faceBottom(double[] walls, int cells, int cell) {
        double t = walls[BAND * cells + cell];
        if (Double.isNaN(t) || t < WALL_PLATEAU) return Double.POSITIVE_INFINITY;
        double wall = walls[WALL * cells + cell];
        return Double.isNaN(wall) ? walls[LOW * cells + cell] : wall;
    }

    /**
     * The height the sinkhole wall model gives a cell: the neighbouring habitats' height on the plateau, the wall in
     * the
     * fall; NaN outside the band and at the foot.
     */
    private static double wallModel(double[] walls, int cells, int cell) {
        if (walls == null) return Double.NaN;
        double t = walls[BAND * cells + cell];
        if (Double.isNaN(t)) return Double.NaN;
        return t < WALL_PLATEAU ? walls[HIGH * cells + cell] : walls[WALL * cells + cell];
    }

    /**
     * Rebuilds the sinkhole walls in an authored chunk: cuts or fills each dry column to its new ground, then makes the
     * wall solid; returns the wall grid ({@link #sinkholeWallHeights}) for the sea's rim. Under Terra's blend and
     * Origen's carver a wall column could stand over a cave, or over the basin's own sky trapped under a slab, a crust
     * over a hollow that the basin saw as a ledge or an overhang (review, 2026-10-24). Every hollow in a wall column
     * from
     * its new ground down to WALL_SKIN below the lowest face beside it is rock now, so every face the wall shows is
     * solid, and so is the trapped sky below that, down to the floor; caves further in stay, sealed from the basin. The
     * skin's inner side follows the terraces a step down rather than a flat cut.
     */
    private double[] shapeSinkholeWalls(byte[] materials, short[] profiles, int chunkX, int chunkZ) {
        double[] walls = sinkholeWallHeights(chunkX, chunkZ);
        if (walls == null) return null;
        OrigenDensityField.ScaledChunkDensity density = densityField.createScaledChunkDensity(chunkX, chunkZ);
        double[] column = authoredDensityColumnScratch.get();
        int span = 16 + 2 * WALL_MARGIN, cells = span * span, states = PROFILE_VALUES.length;
        for (int i = 0; i < 256; i++) {
            int lx = i >> 4, lz = i & 15, a = lx + WALL_MARGIN, b = lz + WALL_MARGIN, cell = a * span + b;
            if (Double.isNaN(walls[WALL * cells + cell])) continue;
            int offset = authoredColumnOffset(lx, lz), top = AUTHORED_HEIGHT - 1;
            while (top > 0 && materials[offset + top] == BiomicTerrainSampler.AIR) top--;
            if (materials[offset + top] == BiomicTerrainSampler.WATER) continue;
            density.fillUpstreamColumn(lx, lz, column);
            int row = wallRow(walls, chunkX, chunkZ, lx, lz, column);
            if (row < 0) continue;
            int x = (chunkX << 4) + lx, z = (chunkZ << 4) + lz;
            double slant = columnSlant(walls, span, a, b);
            if (row != top) paintColumn(materials, profiles, offset, x, z, row, top, slant, false);
            // The skin: from the new ground down to WALL_SKIN below the lowest face beside this column; below it, the
            // basin's own sky that Terra's blend trapped under the wall (open air of the floor's habitat), down to the
            // floor. Caves (the carver's, or a cave habitat's under the rim) beyond the skin stay as they are.
            double face = Math.min(
                row + OrigenDensityField.UPSTREAM_MIN_Y,
                Math.min(
                    Math.min(faceBottom(walls, cells, cell - span), faceBottom(walls, cells, cell + span)),
                    Math.min(faceBottom(walls, cells, cell - 1), faceBottom(walls, cells, cell + 1))));
            int bottom = Math.max(1, (int) Math.floor(face) - OrigenDensityField.UPSTREAM_MIN_Y - WALL_SKIN);
            int floor = Math
                .max(1, (int) Math.floor(walls[LOW * cells + cell]) - OrigenDensityField.UPSTREAM_MIN_Y - WALL_SKIN);
            SurfaceProfile skin = null;
            for (int r = row; r >= Math.min(bottom, floor); r--) {
                byte old = materials[offset + r];
                if (old != BiomicTerrainSampler.AIR && old != BiomicTerrainSampler.WATER) continue;
                if (r < bottom) {
                    int encoded = profiles[offset + r] & 0xFFFF;
                    if (old != BiomicTerrainSampler.AIR || encoded / states != NEGATIVE_DENSITY_STATE
                        || !sinkholeFloor(PROFILE_VALUES[encoded % states])) continue;
                }
                if (skin == null) skin = profile(skinProfile(densityField.sampleSurfaceProfile(x, z), x, z));
                int depth = row - r;
                materials[offset + r] = skin.material(
                    depth,
                    x,
                    r + OrigenDensityField.UPSTREAM_MIN_Y,
                    z,
                    worldSeed,
                    skin.hasApplicableSlant(depth) ? slant : Double.NaN);
                profiles[offset + r] = (short) (profiles[offset + r] % states + SOLID_DENSITY_STATE * states);
            }
        }
        return walls;
    }

    /**
     * Terraces, the construct of Origen's own canyon and mountain walls (Carving Creaks, Terrace Mountains): within
     * each band between two levels Origen's power curve, here one-sided (the ledge at the band's foot, the riser at
     * its top, so a terrace only ever cuts into a slope and never stands a tower on a crest). The levels lie about
     * TERRACE_STEP apart, each riser a little off the grid, the grid's offset drifting slowly across the land so ledges
     * wander and tilt like bedding. On a steep slope this turns the fall into near-flat ledges and near-vertical risers
     * a level high, between a staircase (a step on every column) and a sheer wall of ragged pillars; weight fades it
     * out on gentle ground, which stays smooth. The rim's inner face and the sinkhole walls share the levels, so where
     * they meet their ledges line up.
     */
    private double terraced(int x, int z, double height, double weight) {
        if (weight <= 0.0D) return height;
        double step = TERRACE_STEP * (1.0D + TERRACE_STEP_VARIATION * terraceStep.getSample(worldSeed, x, z));
        double offset = OrigenDensityField.UPSTREAM_SEA_LEVEL
            + TERRACE_OFFSET * terraceOffset.getSample(worldSeed, x, z);
        double v = (height - offset) / step;
        int k = (int) Math.floor(v);
        double lower = k + riserJitter(k), upper = k + 1 + riserJitter(k + 1);
        if (v < lower) {
            upper = lower;
            lower = k - 1 + riserJitter(k - 1);
        } else if (v >= upper) {
            lower = upper;
            upper = k + 2 + riserJitter(k + 2);
        }
        double u = Math.max(0.0D, Math.min(1.0D, (v - lower) / (upper - lower)));
        double shaped = lower + (upper - lower) * Math.pow(u, TERRACE_SHARPNESS);
        return height + (offset + shaped * step - height) * Math.min(1.0D, weight);
    }

    /** A level's riser offset in steps: within ±TERRACE_JITTER / 2, so risers keep their order. */
    private double riserJitter(int level) {
        long hash = WorldgenContext.mix64(worldSeed ^ TERRACE_SALT * (level + 1000L));
        return TERRACE_JITTER * (((hash >>> 11) * 0x1.0p-53) - 0.5D);
    }

    /** How much a raw slope (blocks per block) is terraced: none on gentle ground, fully on cliffs. */
    private static double terraceWeight(double slope) {
        return smooth01((slope - TERRACE_SLOPE_MIN) / (TERRACE_SLOPE_FULL - TERRACE_SLOPE_MIN));
    }

    /** The raw slope at a cell of a height grid (central differences, one-sided at the grid's edge or a gap). */
    private static double slope(double[] heights, int span, int a, int b) {
        double h = heights[a * span + b];
        double gx = difference(heights, span, a, b, 1, 0, h), gz = difference(heights, span, a, b, 0, 1, h);
        return Math.sqrt(gx * gx + gz * gz);
    }

    private static double difference(double[] heights, int span, int a, int b, int da, int db, double h) {
        double plus = a + da < span && b + db < span ? heights[(a + da) * span + b + db] : Double.NaN;
        double minus = a - da >= 0 && b - db >= 0 ? heights[(a - da) * span + b - db] : Double.NaN;
        if (!Double.isNaN(plus) && !Double.isNaN(minus)) return (plus - minus) / 2.0D;
        if (!Double.isNaN(plus)) return plus - h;
        if (!Double.isNaN(minus)) return h - minus;
        return 0.0D;
    }

    /** Terra's DotProduct slant of a height grid at a cell: the up component of its normal. */
    private static double columnSlant(double[] heights, int span, int a, int b) {
        double s = slope(heights, span, a, b);
        return 1.0D / Math.sqrt(1.0D + s * s);
    }

    /**
     * Uniform quadratic B-spline over an n by n node grid at fractional node coordinates (C1, so contours run smooth).
     */
    private static double spline(double[] values, int n, double tx, double tz) {
        int ix = (int) Math.floor(tx + 0.5D), iz = (int) Math.floor(tz + 0.5D);
        double fx = tx - ix, fz = tz - iz, result = 0.0D;
        for (int di = -1; di <= 1; di++) {
            double wx = bspline(fx - di);
            int i = Math.max(0, Math.min(n - 1, ix + di));
            for (int dj = -1; dj <= 1; dj++) {
                int j = Math.max(0, Math.min(n - 1, iz + dj));
                result += wx * bspline(fz - dj) * values[i * n + j];
            }
        }
        return result;
    }

    private static double bspline(double u) {
        u = Math.abs(u);
        return u <= 0.5D ? 0.75D - u * u : u < 1.5D ? 0.5D * (1.5D - u) * (1.5D - u) : 0.0D;
    }

    private static boolean sinkholeFloor(OrigenBiomeProfile profile) {
        return profile == OrigenBiomeProfile.SINKHOLE_JUNGLE || profile == OrigenBiomeProfile.SINKHOLE_FOREST
            || profile == OrigenBiomeProfile.SINKHOLE_FROZEN;
    }

    /**
     * The surface habitat at a node of Terra's 4-block blend grid, memoised in a lock-free direct-mapped table (an
     * entry
     * is one long, key and value together, so a racing reader never sees half of one). Only the cost depends on it.
     */
    private int gridProfile(int gridX, int gridZ) {
        long key = VALID_GRID_ENTRY | ((long) gridX & 0xFFFFFFL) << 32 | ((long) gridZ & 0xFFFFFFL) << 8;
        int slot = (int) (WorldgenContext.mix64(key) & (GRID_CACHE_SIZE - 1));
        long entry = gridProfiles.get(slot);
        if ((entry & ~0xFFL) == key) return (int) (entry & 0xFF);
        int ordinal = densityField.sampleSurfaceProfile(gridX << 2, gridZ << 2)
            .ordinal();
        gridProfiles.set(slot, key | ordinal);
        return ordinal;
    }

    /**
     * Gives a column a new ground at row: everything above it is open air, the cells up to it ground, and so is any
     * hollow within FILL_DEPTH under a raised ground (an overhang's or a cave's), so nothing is left hanging. The new
     * surface and the old one's top layers under it are painted by palette depth from the new surface (sand on a
     * beach), with the slant for the palettes that turn to rock on steep ground. Changed cells are marked open or solid
     * for the projection, as Origen's density would have them.
     */
    private void paintColumn(byte[] materials, short[] profiles, int offset, int x, int z, int row, int oldTop,
        double slant, boolean sand) {
        int states = PROFILE_VALUES.length;
        for (int r = row + 1; r <= oldTop; r++) {
            if (materials[offset + r] == BiomicTerrainSampler.AIR) continue;
            materials[offset + r] = BiomicTerrainSampler.AIR;
            profiles[offset + r] = (short) (profiles[offset + r] % states);
        }
        if (row >= oldTop) for (int r = Math.max(1, Math.min(oldTop + 1, row - FILL_DEPTH)); r <= row; r++) {
            byte old = materials[offset + r];
            if (old != BiomicTerrainSampler.AIR && old != BiomicTerrainSampler.WATER) continue;
            materials[offset + r] = BiomicTerrainSampler.STONE;
            profiles[offset + r] = (short) (profiles[offset + r] % states + SOLID_DENSITY_STATE * states);
        }
        SurfaceProfile skin = profile(skinProfile(densityField.sampleSurfaceProfile(x, z), x, z));
        for (int r = row, depth = 0; r > 0 && r > Math.min(row, oldTop) - 3; r--, depth++) {
            byte old = materials[offset + r];
            if (old == BiomicTerrainSampler.AIR || old == BiomicTerrainSampler.WATER) break;
            materials[offset + r] = sand && depth < 3
                ? (depth < 2 ? BiomicTerrainSampler.SAND : BiomicTerrainSampler.SANDSTONE)
                : skin.material(
                    depth,
                    x,
                    r + OrigenDensityField.UPSTREAM_MIN_Y,
                    z,
                    worldSeed,
                    skin.hasApplicableSlant(depth) ? slant : Double.NaN);
        }
    }

    /**
     * The habitats without a sea of their own (sinkholes, sunken valleys, the tombs) lie below the sea's level. Where
     * one
     * reaches the sea, {@link #sealSeaCaves} walls the water off with a one-block wall from the basin's floor to the
     * sea
     * (review, 2026-10-23: "those dams ... are very flat and look super unnatural"), and a long earthen ramp from the
     * shore into the basin, the first answer, filled whole sinkholes with a regular grass staircase (review,
     * 2026-10-24). Instead the basin keeps its own ground and gains a rim where the sea is near, shaped like the
     * basin's own walls: a shallow sandy shelf where the coast sits back, a gentle beach, a crest of uneven height with
     * the odd knoll, then an inner face that falls with its line wandering in and out, terraced like the sinkhole walls
     * (on the same levels, so where the two meet their ledges line up), until it meets the basin's own wall or floor.
     * The rim is skinned with the basin's own palette, slope by slope (rock on the faces, soil on the crest and the
     * ledges), so it reads as part of the habitat. Only columns open to the sky below the sea's level change (never
     * caves under land), and every input is a pure function of the seed and the position (the sea is looked for in
     * Origen's fields, never in the chunk's materials), so a column comes out the same whichever chunk computes it,
     * and the seal finds nothing left to wall.
     */
    private void raiseSeaRims(byte[] materials, short[] profiles, double[] walls, int chunkX, int chunkZ) {
        int sea = OrigenDensityField.UPSTREAM_SEA_LEVEL, seaRow = sea - OrigenDensityField.UPSTREAM_MIN_Y;
        int[] top = new int[256];
        boolean any = false;
        for (int i = 0; i < 256; i++) {
            int offset = authoredColumnOffset(i >> 4, i & 15), row = AUTHORED_HEIGHT - 1;
            while (row > 0 && materials[offset + row] == BiomicTerrainSampler.AIR) row--;
            top[i] = row < seaRow && materials[offset + row] != BiomicTerrainSampler.WATER ? row : -1;
            any |= top[i] >= 0;
        }
        if (!any) return;
        int x0 = chunkX << 4, z0 = chunkZ << 4;
        // A coarse look first: most basins lie far from any sea. The lattice below is Terra's blend grid, so a habitat
        // with an ocean on it is exactly what the lattice could find (memoised, and no density).
        boolean near = false;
        for (int x = -RIM_REACH; x <= 16 + RIM_REACH && !near; x += RIM_LATTICE)
            for (int z = -RIM_REACH; z <= 16 + RIM_REACH && !near; z += RIM_LATTICE)
                near = profile(PROFILE_VALUES[gridProfile((x0 + x) >> 2, (z0 + z) >> 2)]).hasOcean();
        if (!near) return;
        // The sea on the lattice around the chunk and every node's distance to it (a two-pass chamfer transform).
        int n = (16 + 2 * RIM_REACH) / RIM_LATTICE + 1;
        double[] distance = new double[n * n];
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++)
            distance[i * n + j] = rimSea(x0 - RIM_REACH + i * RIM_LATTICE, z0 - RIM_REACH + j * RIM_LATTICE) ? 0.0D
                : FAR;
        chamfer(distance, n);
        // Ground heights over the chunk and a margin, interpolated between nodes (the margin gives every column the
        // slope that picks rock or soil, and the slope that says how much to terrace).
        int span = 16 + 2 * RIM_MARGIN, pad = RIM_MARGIN + RIM_LATTICE, cells = 16 + 2 * pad;
        byte[] exactSea = new byte[cells * cells];
        double[] raw = new double[span * span], shore = new double[span * span];
        for (int a = 0; a < span; a++) for (int b = 0; b < span; b++) {
            int lx = a - RIM_MARGIN, lz = b - RIM_MARGIN;
            double fx = (lx + RIM_REACH) / (double) RIM_LATTICE, fz = (lz + RIM_REACH) / (double) RIM_LATTICE;
            int ix = (int) fx, iz = (int) fz;
            double tx = fx - ix, tz = fz - iz;
            double d = ((distance[ix * n + iz] * (1 - tz) + distance[ix * n + iz + 1] * tz) * (1 - tx)
                + (distance[(ix + 1) * n + iz] * (1 - tz) + distance[(ix + 1) * n + iz + 1] * tz) * tx) * RIM_LATTICE;
            // By the coast the sea is found cell by cell, so the shore follows the water exactly.
            if (d < RIM_LATTICE * 2) for (int dx = -RIM_LATTICE; dx <= RIM_LATTICE; dx++)
                for (int dz = -RIM_LATTICE; dz <= RIM_LATTICE; dz++) {
                    double exact = Math.sqrt(dx * dx + dz * dz);
                    if (exact >= d) continue;
                    int cell = (lx + dx + pad) * cells + lz + dz + pad;
                    if (exactSea[cell] == 0) exactSea[cell] = (byte) (rimSea(x0 + lx + dx, z0 + lz + dz) ? 2 : 1);
                    if (exactSea[cell] == 2) d = exact;
                }
            int x = x0 + lx, z = z0 + lz;
            // How far inland of the shoreline: the coast sits back by up to RIM_SHELF_WIDTH, flooding a shallow shelf.
            double t = d - RIM_SHELF_WIDTH * Math.max(0.0D, 2.0D * unit(rimShelf, x, z) - 1.0D);
            shore[a * span + b] = t;
            raw[a * span + b] = rimHeight(x, z, t);
            // Over a sinkhole wall's band the rim holds its crest where the wall stands higher than its fall, so the
            // crest runs on to where the wall drops into the basin instead of leaving a trench between the two.
            double model = t < 0.0D ? Double.NaN : wallModel(walls, span * span, a * span + b);
            if (model > raw[a * span + b]) raw[a * span + b] = Math.min(model, rimCrest(x, z, t));
        }
        double[] ground = new double[span * span];
        for (int a = 0; a < span; a++) for (int b = 0; b < span; b++) {
            // Only the inner face is terraced: the beach and the crest stay smooth.
            int x = x0 + a - RIM_MARGIN, z = z0 + b - RIM_MARGIN;
            double weight = terraceWeight(slope(raw, span, a, b)) * smooth01(rimFall(x, z, shore[a * span + b]) / 2.0D);
            ground[a * span + b] = terraced(x, z, raw[a * span + b], weight);
        }
        for (int i = 0; i < 256; i++) {
            if (top[i] < 0) continue;
            int lx = i >> 4, lz = i & 15, a = lx + RIM_MARGIN, b = lz + RIM_MARGIN,
                offset = authoredColumnOffset(lx, lz);
            double t = shore[a * span + b];
            int surfaceRow = (int) Math.floor(ground[a * span + b]) - OrigenDensityField.UPSTREAM_MIN_Y;
            boolean shelf = t < 0.0D && surfaceRow < seaRow;
            if (!shelf && surfaceRow <= top[i]) continue;
            int x = x0 + lx, z = z0 + lz;
            boolean sand = shelf || surfaceRow <= seaRow + 1 && t < RIM_BEACH * (1.6D * unit(rimBeach, x, z) - 0.4D);
            int groundRow = Math.max(surfaceRow, top[i]);
            if (shelf) for (int row = groundRow + 1; row <= seaRow; row++)
                materials[offset + row] = BiomicTerrainSampler.WATER;
            paintColumn(materials, profiles, offset, x, z, groundRow, top[i], columnSlant(ground, span, a, b), sand);
        }
    }

    /** How far a rim's inner face has fallen t blocks inland of its shoreline: none yet on the beach and the crest. */
    private double rimFall(int x, int z, double t) {
        if (t < 0.0D) return 0.0D;
        double wander = smooth01((t - 1.5D) / 3.0D) * RIM_WANDER * rimFace.getSample(worldSeed, x, z);
        return Math.max(0.0D, t + wander - RIM_WIDTH_MIN - RIM_WIDTH * unit(rimWidth, x, z));
    }

    /**
     * Authored height of a rim's ground t blocks inland of its shoreline (negative: under the shelf's water). The beach
     * rises over RIM_RAMP blocks to a crest RIM_CREST high (knolls up to RIM_KNOLL more, rising over RIM_KNOLL_RAMP
     * more), RIM_WIDTH wide; the inner face then falls RIM_SLOPE per block, its line wandering by RIM_WANDER.
     */
    private double rimHeight(int x, int z, double t) {
        if (t < 0.0D) return OrigenDensityField.UPSTREAM_SEA_LEVEL + 1.0D + RIM_SHELF_SLOPE * t;
        double slope = RIM_SLOPE_MIN + RIM_SLOPE * unit(rimSlope, x, z);
        return rimCrest(x, z, t) - slope * rimFall(x, z, t);
    }

    /** A rim's height t blocks inland of its shoreline before its inner face falls: the beach and the crest. */
    private double rimCrest(int x, int z, double t) {
        double ramp = RIM_RAMP_MIN + RIM_RAMP * unit(rimRamp, x, z);
        return OrigenDensityField.UPSTREAM_SEA_LEVEL + 1.0D
            + RIM_CREST * unit(rimCrest, x, z) * smooth01(t / ramp)
            + RIM_KNOLL * Math.max(0.0D, rimKnoll.getSample(worldSeed, x, z) - 0.4D)
                / 0.6D
                * smooth01((t - 1.0D) / (ramp + RIM_KNOLL_RAMP));
    }

    /**
     * The sea at the sea's level row, from Origen's own fields alone (never a chunk's materials, so every chunk that
     * looks at a cell sees the same); the habitat first, as it is cheap and rules out most cells without density.
     * Origen extrudes no habitat as high as the sea's level, so the surface habitat is the one there.
     */
    private boolean rimSea(int worldX, int worldZ) {
        int sea = OrigenDensityField.UPSTREAM_SEA_LEVEL;
        SurfaceProfile surface = profile(
            ((worldX | worldZ) & 3) == 0 ? PROFILE_VALUES[gridProfile(worldX >> 2, worldZ >> 2)]
                : densityField.sampleSurfaceProfile(worldX, worldZ));
        return surface.hasOcean() && sea <= surface.seaLevel
            && densityField.sampleUpstreamInterpolated(worldX, sea, worldZ) <= 0.0D;
    }

    /** Low-frequency noise from [-1, 1] to [0, 1]. */
    private double unit(Terra64OpenSimplex2Sampler noise, int x, int z) {
        return 0.5D * (1.0D + noise.getSample(worldSeed, x, z));
    }

    private static double smooth01(double t) {
        t = Math.max(0.0D, Math.min(1.0D, t));
        return t * t * (3 - 2 * t);
    }

    /** Two-pass chamfer distance transform over an n by n lattice, in lattice steps. */
    private static void chamfer(double[] distance, int n) {
        double diagonal = Math.sqrt(2.0D);
        for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
            double d = distance[i * n + j];
            if (i > 0) d = Math.min(d, distance[(i - 1) * n + j] + 1);
            if (j > 0) d = Math.min(d, distance[i * n + j - 1] + 1);
            if (i > 0 && j > 0) d = Math.min(d, distance[(i - 1) * n + j - 1] + diagonal);
            if (i > 0 && j + 1 < n) d = Math.min(d, distance[(i - 1) * n + j + 1] + diagonal);
            distance[i * n + j] = d;
        }
        for (int i = n - 1; i >= 0; i--) for (int j = n - 1; j >= 0; j--) {
            double d = distance[i * n + j];
            if (i + 1 < n) d = Math.min(d, distance[(i + 1) * n + j] + 1);
            if (j + 1 < n) d = Math.min(d, distance[i * n + j + 1] + 1);
            if (i + 1 < n && j + 1 < n) d = Math.min(d, distance[(i + 1) * n + j + 1] + diagonal);
            if (i + 1 < n && j > 0) d = Math.min(d, distance[(i + 1) * n + j - 1] + diagonal);
            distance[i * n + j] = d;
        }
    }

    /** A chamfer transform's "no such node in the window", in lattice steps: far beyond any wall or rim. */
    private static final double FAR = 1.0e6D;

    /**
     * Terraces: the levels' spacing, how much it drifts, how far the levels' offset drifts, how flat the ledges are
     * (the power curve's exponent), how far each riser sits off its level (in steps), and the slopes (blocks per block)
     * where terracing begins and is complete.
     */
    private static final double TERRACE_STEP = 8.0D, TERRACE_STEP_VARIATION = 0.03D, TERRACE_OFFSET = 3.0D,
        TERRACE_SHARPNESS = 15.0D, TERRACE_JITTER = 0.6D, TERRACE_SLOPE_MIN = 0.7D, TERRACE_SLOPE_FULL = 1.8D;
    private static final long TERRACE_SALT = 0x51D3A7C9E1L;
    private final Terra64OpenSimplex2Sampler terraceStep = new Terra64OpenSimplex2Sampler(1 / 90.0D, 0x7E77A1L),
        terraceOffset = new Terra64OpenSimplex2Sampler(1 / 70.0D, 0x7E77A2L);

    /**
     * Sinkhole walls: how far around the chunk the blend grid is read (a wall must meet the floor within it), the
     * tent's radius in grid steps for the neighbouring habitats' height, and the margin of columns whose heights give
     * the slopes. The band starts WALL_OUTER blocks inside the edge (outside it: negative) with a plateau WALL_PLATEAU
     * wide, wanders by WALL_WANDER, falls WALL_SLOPE_MIN to WALL_SLOPE_MIN + WALL_SLOPE per block (but always reaches
     * the floor within WALL_FALL), leaves the floor alone within WALL_FOOT of it, eases in from the land outside over
     * WALL_BLEND blocks, and is rock to WALL_SKIN below the lowest face beside a column.
     */
    private static final int WALL_REACH = 48, WALL_RADIUS = 10, WALL_MARGIN = 2, WALL_SKIN = 4;
    private static final double WALL_OUTER = -12.0D, WALL_PLATEAU = 9.0D, WALL_WANDER = 3.0D, WALL_SLOPE_MIN = 3.0D,
        WALL_SLOPE = 1.5D, WALL_FALL = 30.0D, WALL_FOOT = 1.0D, WALL_BLEND = 6.0D;
    /**
     * The layers of {@link #sinkholeWallHeights}: the wall, the depth into the band, the neighbours' height, the
     * floor's.
     */
    private static final int WALL = 0, BAND = 1, HIGH = 2, LOW = 3;
    private final Terra64OpenSimplex2Sampler wallSlope = new Terra64OpenSimplex2Sampler(1 / 40.0D, 0x3A11A1L),
        wallWander = new Terra64OpenSimplex2Sampler(1 / 35.0D, 0x3A11A2L);

    /** How deep under a raised ground hollows are closed. */
    private static final int FILL_DEPTH = 12;

    private static final int GRID_CACHE_SIZE = 1 << 16;
    private static final long VALID_GRID_ENTRY = 1L << 62;
    private final java.util.concurrent.atomic.AtomicLongArray gridProfiles = new java.util.concurrent.atomic.AtomicLongArray(
        GRID_CACHE_SIZE);

    /**
     * How far around the chunk the sea is looked for and on which lattice (Terra's blend grid); the margin of columns
     * around the chunk whose heights give the slopes. The reach must cover the whole fall: from the highest knoll
     * (authored 75) the gentlest face is below the basin's floor (authored -42) within 52 blocks of the sea, and a
     * node's chamfer distance agrees between windows up to the reach less a lattice step.
     */
    private static final int RIM_REACH = 60, RIM_LATTICE = 4, RIM_MARGIN = WALL_MARGIN;
    /** Shelf: widest sit-back of the coast and its slope under water. Beach: widest sand at the waterline. */
    private static final double RIM_SHELF_WIDTH = 5.0D, RIM_SHELF_SLOPE = 0.5D, RIM_BEACH = 2.5D;
    /** Crest: height above the shoreline, knolls and how much further they take to rise, the beach's climb, width. */
    private static final double RIM_CREST = 2.5D, RIM_KNOLL = 9.0D, RIM_KNOLL_RAMP = 8.0D, RIM_RAMP_MIN = 4.0D,
        RIM_RAMP = 6.0D, RIM_WIDTH_MIN = 2.0D, RIM_WIDTH = 3.0D;
    /** Inner face: slope per block and how far its line wanders (broadly: no per-block jitter). */
    private static final double RIM_SLOPE_MIN = 3.0D, RIM_SLOPE = 1.5D, RIM_WANDER = 3.0D;
    private final Terra64OpenSimplex2Sampler rimFace = new Terra64OpenSimplex2Sampler(1 / 30.0D, 0x5157A1L),
        rimCrest = new Terra64OpenSimplex2Sampler(1 / 40.0D, 0x5157A3L),
        rimWidth = new Terra64OpenSimplex2Sampler(1 / 30.0D, 0x5157A4L),
        rimSlope = new Terra64OpenSimplex2Sampler(1 / 24.0D, 0x5157A5L),
        rimKnoll = new Terra64OpenSimplex2Sampler(1 / 50.0D, 0x5157A6L),
        rimRamp = new Terra64OpenSimplex2Sampler(1 / 36.0D, 0x5157A7L),
        rimShelf = new Terra64OpenSimplex2Sampler(1 / 45.0D, 0x5157A8L),
        rimBeach = new Terra64OpenSimplex2Sampler(1 / 20.0D, 0x5157A9L);

    /** Highest authored Y an ocean can reach: Origen's sea level plus one. */
    private static final int SEA_CAVE_CEILING = 64;

    private boolean seaWater(byte[] materials, int chunkX, int chunkZ, int localX, int authoredY, int localZ) {
        if (localX >= 0 && localX < BiomicTerrainSampler.CHUNK_WIDTH
            && localZ >= 0
            && localZ < BiomicTerrainSampler.CHUNK_WIDTH) {
            return materials[authoredColumnOffset(localX, localZ) + authoredY - OrigenDensityField.UPSTREAM_MIN_Y]
                == BiomicTerrainSampler.WATER;
        }
        int worldX = (chunkX << 4) + localX, worldZ = (chunkZ << 4) + localZ;
        // The habitat first: it is cheap, and most cells it rules out need no density.
        SurfaceProfile surface = profile(
            densityField.extrudeProfile(densityField.sampleSurfaceProfile(worldX, worldZ), worldX, authoredY, worldZ));
        if (!surface.hasOcean() || authoredY > surface.seaLevel) return false;
        return densityField.sampleUpstreamInterpolated(worldX, authoredY, worldZ) <= 0.0D;
    }

    private static int authoredColumnOffset(int localX, int localZ) {
        return (localX * BiomicTerrainSampler.CHUNK_WIDTH + localZ) * AUTHORED_HEIGHT;
    }

    private static long horizontalKey(int x, int z) {
        return (long) x << 32 ^ z & 0xFFFF_FFFFL;
    }

    private static boolean isUncarvedBase(byte material) {
        return material == BiomicTerrainSampler.STONE || material == BiomicTerrainSampler.BEDROCK;
    }

    private SurfaceProfile profile(OrigenBiomeProfile biome) {
        SurfaceProfile profile = profiles[biome.ordinal()];
        if (profile == null) {
            throw new IllegalArgumentException("No imported Origen surface profile for " + biome);
        }
        return profile;
    }

    /**
     * Terra's DotProduct slant (the up component of the density gradient's direction), with each difference taken
     * over {@link #SLANT_SAMPLE_DISTANCE} on the continuous field, so neighbouring chunks agree at their shared edge.
     */
    private double calculateSlant(OrigenDensityField.ScaledChunkDensity chunkDensity, int worldX, int upstreamY,
        int worldZ, int localX, int localZ) {
        int distance = SLANT_SAMPLE_DISTANCE;
        double normalX = sampleField(chunkDensity, worldX - distance, upstreamY, worldZ, localX - distance, localZ)
            - sampleField(chunkDensity, worldX + distance, upstreamY, worldZ, localX + distance, localZ);
        double normalY = sampleField(chunkDensity, worldX, upstreamY - distance, worldZ, localX, localZ)
            - sampleField(chunkDensity, worldX, upstreamY + distance, worldZ, localX, localZ);
        double normalZ = sampleField(chunkDensity, worldX, upstreamY, worldZ - distance, localX, localZ - distance)
            - sampleField(chunkDensity, worldX, upstreamY, worldZ + distance, localX, localZ + distance);
        double lengthSquared = normalX * normalX + normalY * normalY + normalZ * normalZ;
        return lengthSquared == 0.0D ? 0.0D : normalY * terraInverseSqrt(lengthSquared);
    }

    /** The interpolated field at a block: from the chunk's own lattice inside it, from the shared field outside. */
    private double sampleField(OrigenDensityField.ScaledChunkDensity chunkDensity, int worldX, int upstreamY,
        int worldZ, int localX, int localZ) {
        if (chunkDensity != null && localX >= 0
            && localX < BiomicTerrainSampler.CHUNK_WIDTH
            && localZ >= 0
            && localZ < BiomicTerrainSampler.CHUNK_WIDTH) {
            double inside = chunkDensity.sampleBlock(localX, upstreamY, localZ);
            if (!Double.isNaN(inside)) return inside;
        }
        return densityField.sampleUpstreamInterpolated(worldX, upstreamY, worldZ);
    }

    /** Terra 6.4.2 normalizes slant vectors with its single-iteration fast inverse square root. */
    private static double terraInverseSqrt(double value) {
        double half = 0.5D * value;
        long bits = Double.doubleToLongBits(value);
        bits = 0x5FE6EC85E7DE30DAL - (bits >> 1);
        double inverse = Double.longBitsToDouble(bits);
        return inverse * (1.5D - half * inverse * inverse);
    }

    private void load() {
        try {
            JsonObject manifest = OrigenManifest.read();
            OrigenTerrainRuntime.Compiler compiler = new OrigenTerrainRuntime.Compiler(
                object(manifest, "functions"),
                object(manifest, "samplers"));
            loadPalettes(object(manifest, "palettes"), compiler);
            loadProfiles(object(manifest, "profiles"), compiler);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Could not compile Origen surface palettes", exception);
        }
    }

    private void loadPalettes(JsonObject manifest, OrigenTerrainRuntime.Compiler compiler) {
        for (Map.Entry<String, JsonElement> entry : manifest.entrySet()) {
            JsonObject definition = entry.getValue()
                .getAsJsonObject();
            NoiseSampler defaultSampler = compiler.compileSampler(object(definition, "sampler"));
            List<PaletteLayer> layers = new ArrayList<PaletteLayer>();
            int layerIndex = 0;
            for (JsonElement rawLayer : array(definition, "layers")) {
                JsonObject layer = rawLayer.getAsJsonObject();
                NoiseSampler sampler = layer.has("sampler") ? compiler.compileSampler(layer.getAsJsonObject("sampler"))
                    : defaultSampler;
                byte[] probability = probabilityArray(array(layer, "materials"), compiler, entry.getKey(), layerIndex);
                int repetitions = (int) compiler.numeric(required(layer, "layers"));
                if (repetitions < 1) {
                    throw new IllegalStateException("Palette " + entry.getKey() + " has a non-positive layer count");
                }
                for (int index = 0; index < repetitions; index++) {
                    layers.add(new PaletteLayer(probability, sampler));
                }
                layerIndex++;
            }
            palettes.put(entry.getKey(), new Palette(layers));
        }
    }

    private static byte[] probabilityArray(JsonArray materials, OrigenTerrainRuntime.Compiler compiler, String palette,
        int layer) {
        int size = 0;
        for (JsonElement rawMaterial : materials) {
            int weight = (int) compiler.numeric(required(rawMaterial.getAsJsonObject(), "weight"));
            if (weight < 1) {
                throw new IllegalStateException(
                    "Palette " + palette + " layer " + layer + " has a non-positive material weight");
            }
            size += weight;
        }
        byte[] probability = new byte[size];
        int output = 0;
        for (JsonElement rawMaterial : materials) {
            JsonObject material = rawMaterial.getAsJsonObject();
            int weight = (int) compiler.numeric(required(material, "weight"));
            byte id = material(required(material, "material").getAsString());
            for (int index = 0; index < weight; index++) {
                probability[output++] = id;
            }
        }
        return probability;
    }

    private void loadProfiles(JsonObject manifest, OrigenTerrainRuntime.Compiler compiler) {
        for (OrigenBiomeProfile biome : OrigenBiomeProfile.values()) {
            JsonElement features = object(manifest, biome.name()).get("features");
            List<String> trees = new ArrayList<String>();
            if (features != null && features.isJsonObject()
                && features.getAsJsonObject()
                    .has("trees"))
                for (JsonElement tree : features.getAsJsonObject()
                    .getAsJsonArray("trees")) trees.add(tree.getAsString());
            treeFeatures.put(biome, Collections.unmodifiableList(trees));
            JsonObject entry = object(object(manifest, biome.name()), "surface");
            PaletteHolder base = holder(array(entry, "palette"), compiler);
            List<SlantLayer> slant = new ArrayList<SlantLayer>();
            for (JsonElement rawSlant : array(entry, "slant")) {
                JsonObject slantEntry = rawSlant.getAsJsonObject();
                slant.add(
                    new SlantLayer(
                        compiler.numeric(required(slantEntry, "threshold")),
                        holder(array(slantEntry, "palette"), compiler)));
            }
            Collections.sort(slant, Comparator.comparingDouble(SlantLayer::threshold));
            Palette ocean = null;
            Integer seaLevel = null;
            JsonElement rawOcean = entry.get("ocean_palette");
            JsonElement rawSeaLevel = entry.get("ocean_level");
            if (rawOcean != null && !rawOcean.isJsonNull() && rawSeaLevel != null && !rawSeaLevel.isJsonNull()) {
                ocean = palette(rawOcean.getAsString());
                seaLevel = Integer.valueOf((int) compiler.numeric(rawSeaLevel));
            }
            profiles[biome.ordinal()] = new SurfaceProfile(
                base,
                slant,
                (int) compiler.numeric(required(entry, "slant_depth")),
                ocean,
                seaLevel,
                required(entry, "update_palette_when_carving").getAsBoolean());
        }
    }

    private PaletteHolder holder(JsonArray entries, OrigenTerrainRuntime.Compiler compiler) {
        List<PaletteCutoff> cutoffs = new ArrayList<PaletteCutoff>();
        for (JsonElement rawEntry : entries) {
            JsonObject entry = rawEntry.getAsJsonObject();
            cutoffs.add(
                new PaletteCutoff(
                    (int) compiler.numeric(required(entry, "max_y")),
                    palette(required(entry, "palette").getAsString())));
        }
        Collections.sort(cutoffs, Comparator.comparingInt(PaletteCutoff::maximumY));
        return new PaletteHolder(cutoffs);
    }

    private Palette palette(String reference) {
        if (reference.startsWith("BLOCK:")) {
            byte[] singleton = { material(reference.substring("BLOCK:".length())) };
            return new Palette(Collections.singletonList(new PaletteLayer(singleton, NoiseSampler.zero())));
        }
        Palette palette = palettes.get(reference);
        if (palette == null) {
            throw new IllegalStateException("Unknown Origen palette " + reference);
        }
        return palette;
    }

    private static byte material(String state) {
        switch (state) {
            case "minecraft:air":
                return BiomicTerrainSampler.AIR;
            case "minecraft:andesite":
                return BiomicTerrainSampler.ANDESITE;
            case "minecraft:basalt":
                return BiomicTerrainSampler.BASALT;
            case "minecraft:bedrock":
                return BiomicTerrainSampler.BEDROCK;
            case "minecraft:blackstone":
                return BiomicTerrainSampler.BLACKSTONE;
            case "minecraft:blue_ice":
                return BiomicTerrainSampler.BLUE_ICE;
            case "minecraft:packed_ice":
                return BiomicTerrainSampler.PACKED_ICE;
            case "minecraft:blue_terracotta":
                return BiomicTerrainSampler.BLUE_TERRACOTTA;
            case "minecraft:brown_concrete":
                return BiomicTerrainSampler.BROWN_CONCRETE;
            case "minecraft:brown_terracotta":
                return BiomicTerrainSampler.BROWN_TERRACOTTA;
            case "minecraft:calcite":
                return BiomicTerrainSampler.CALCITE;
            case "minecraft:clay":
                return BiomicTerrainSampler.CLAY;
            case "minecraft:coarse_dirt":
                return BiomicTerrainSampler.COARSE_DIRT;
            case "minecraft:cobblestone":
                return BiomicTerrainSampler.COBBLESTONE;
            case "minecraft:cyan_terracotta":
                return BiomicTerrainSampler.CYAN_TERRACOTTA;
            case "minecraft:dead_brain_coral_block":
                return BiomicTerrainSampler.DEAD_BRAIN_CORAL;
            case "minecraft:dead_bubble_coral_block":
                return BiomicTerrainSampler.DEAD_BUBBLE_CORAL;
            case "minecraft:dead_horn_coral_block":
                return BiomicTerrainSampler.DEAD_HORN_CORAL;
            case "minecraft:deepslate":
                return BiomicTerrainSampler.DEEPSLATE;
            case "minecraft:diorite":
                return BiomicTerrainSampler.DIORITE;
            case "minecraft:dirt":
                return BiomicTerrainSampler.DIRT;
            case "minecraft:dripstone_block":
                return BiomicTerrainSampler.DRIPSTONE_BLOCK;
            case "minecraft:farmland[moisture=7]":
                return BiomicTerrainSampler.FARMLAND_MOIST;
            case "minecraft:granite":
                return BiomicTerrainSampler.GRANITE;
            case "minecraft:grass_block":
                return BiomicTerrainSampler.GRASS;
            case "minecraft:gravel":
                return BiomicTerrainSampler.GRAVEL;
            case "minecraft:ice":
                return BiomicTerrainSampler.ICE;
            case "minecraft:lava":
                return BiomicTerrainSampler.LAVA;
            case "minecraft:light_blue_terracotta":
                return BiomicTerrainSampler.LIGHT_BLUE_TERRACOTTA;
            case "minecraft:light_gray_terracotta":
                return BiomicTerrainSampler.LIGHT_GRAY_TERRACOTTA;
            case "minecraft:lime_terracotta":
                return BiomicTerrainSampler.LIME_TERRACOTTA;
            case "minecraft:magma_block":
                return BiomicTerrainSampler.MAGMA;
            case "minecraft:moss_block":
                return BiomicTerrainSampler.MOSS;
            case "minecraft:mossy_cobblestone":
                return BiomicTerrainSampler.MOSSY_COBBLESTONE;
            case "minecraft:mud":
                return BiomicTerrainSampler.MUD;
            case "minecraft:mycelium":
                return BiomicTerrainSampler.MYCELIUM;
            case "minecraft:obsidian":
                return BiomicTerrainSampler.OBSIDIAN;
            case "minecraft:orange_terracotta":
                return BiomicTerrainSampler.ORANGE_TERRACOTTA;
            case "minecraft:packed_mud":
                return BiomicTerrainSampler.PACKED_MUD;
            case "minecraft:pink_terracotta":
                return BiomicTerrainSampler.PINK_TERRACOTTA;
            case "minecraft:podzol":
                return BiomicTerrainSampler.PODZOL;
            case "minecraft:powder_snow":
                return BiomicTerrainSampler.POWDER_SNOW;
            case "minecraft:red_concrete":
                return BiomicTerrainSampler.RED_CONCRETE;
            case "minecraft:red_sand":
                return BiomicTerrainSampler.RED_SAND;
            case "minecraft:red_sandstone":
                return BiomicTerrainSampler.RED_SANDSTONE;
            case "minecraft:red_terracotta":
                return BiomicTerrainSampler.RED_TERRACOTTA;
            case "minecraft:rooted_dirt":
                return BiomicTerrainSampler.ROOTED_DIRT;
            case "minecraft:sand":
                return BiomicTerrainSampler.SAND;
            case "minecraft:sandstone":
                return BiomicTerrainSampler.SANDSTONE;
            case "minecraft:smooth_basalt":
                return BiomicTerrainSampler.SMOOTH_BASALT;
            case "minecraft:smooth_sandstone":
                return BiomicTerrainSampler.SMOOTH_SANDSTONE;
            case "minecraft:snow_block":
                return BiomicTerrainSampler.SNOW;
            case "minecraft:stone":
                return BiomicTerrainSampler.STONE;
            case "minecraft:terracotta":
                return BiomicTerrainSampler.TERRACOTTA;
            case "minecraft:tuff":
                return BiomicTerrainSampler.TUFF;
            case "minecraft:water":
                return BiomicTerrainSampler.WATER;
            case "minecraft:white_concrete_powder":
                return BiomicTerrainSampler.WHITE_CONCRETE_POWDER;
            case "minecraft:white_terracotta":
                return BiomicTerrainSampler.WHITE_TERRACOTTA;
            case "minecraft:yellow_concrete":
                return BiomicTerrainSampler.YELLOW_CONCRETE;
            case "minecraft:yellow_terracotta":
                return BiomicTerrainSampler.YELLOW_TERRACOTTA;
            default:
                throw new IllegalStateException("Unsupported Origen palette material " + state);
        }
    }

    private static JsonElement required(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            throw new IllegalStateException("Missing generated Origen surface property " + key);
        }
        return value;
    }

    private static JsonObject object(JsonObject object, String key) {
        JsonElement value = required(object, key);
        if (!value.isJsonObject()) {
            throw new IllegalStateException("Generated Origen surface property " + key + " is not an object");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray array(JsonObject object, String key) {
        JsonElement value = required(object, key);
        if (!value.isJsonArray()) {
            throw new IllegalStateException("Generated Origen surface property " + key + " is not an array");
        }
        return value.getAsJsonArray();
    }

    private static final class SurfaceProfile {

        private final PaletteHolder base;
        private final SlantLayer[] slant;
        private final int slantDepth;
        private final Palette ocean;
        private final Integer seaLevel;
        private final boolean updatePaletteWhenCarving;

        private SurfaceProfile(PaletteHolder base, List<SlantLayer> slant, int slantDepth, Palette ocean,
            Integer seaLevel, boolean updatePaletteWhenCarving) {
            this.base = base;
            this.slant = slant.toArray(new SlantLayer[slant.size()]);
            this.slantDepth = slantDepth;
            this.ocean = ocean;
            this.seaLevel = seaLevel;
            this.updatePaletteWhenCarving = updatePaletteWhenCarving;
        }

        private boolean hasOcean() {
            return ocean != null && seaLevel != null;
        }

        private boolean hasApplicableSlant(int depth) {
            return slant.length != 0 && depth <= slantDepth;
        }

        private byte material(int depth, int x, int y, int z, long seed, double sampledSlant) {
            Palette selected = base.get(y);
            if (!Double.isNaN(sampledSlant) && sampledSlant < slant[slant.length - 1].threshold) {
                for (int index = 0; index < slant.length; index++) {
                    SlantLayer layer = slant[index];
                    if (layer.threshold >= sampledSlant) {
                        selected = layer.palette.get(y);
                        break;
                    }
                }
            }
            return selected.get(depth, x, y, z, seed);
        }
    }

    private static final class PaletteHolder {

        private final PaletteCutoff[] cutoffs;

        private PaletteHolder(List<PaletteCutoff> cutoffs) {
            if (cutoffs.isEmpty()) {
                throw new IllegalStateException("An Origen palette holder cannot be empty");
            }
            this.cutoffs = cutoffs.toArray(new PaletteCutoff[cutoffs.size()]);
        }

        private Palette get(int y) {
            for (int index = 0; index < cutoffs.length; index++) {
                PaletteCutoff cutoff = cutoffs[index];
                if (cutoff.maximumY >= y) {
                    return cutoff.palette;
                }
            }
            return cutoffs[cutoffs.length - 1].palette;
        }
    }

    private static final class Palette {

        private final PaletteLayer[] layers;

        private Palette(List<PaletteLayer> layers) {
            if (layers.isEmpty()) {
                throw new IllegalStateException("An Origen palette cannot be empty");
            }
            this.layers = layers.toArray(new PaletteLayer[layers.size()]);
        }

        private byte get(int depth, double x, double y, double z, long seed) {
            PaletteLayer layer = layers[Math.min(Math.max(depth, 0), layers.length - 1)];
            if (layer.materials.length == 1) return layer.materials[0];
            double noise = layer.sampler.noise(seed, x, y, z);
            int index = Math.max(
                0,
                Math.min(
                    layer.materials.length - 1,
                    (int) Math.floor(((noise + 1.0D) / 2.0D) * layer.materials.length)));
            return layer.materials[index];
        }
    }

    private static final class PaletteLayer {

        private final byte[] materials;
        private final NoiseSampler sampler;

        private PaletteLayer(byte[] materials, NoiseSampler sampler) {
            this.materials = materials.clone();
            this.sampler = sampler;
        }
    }

    private static final class PaletteCutoff {

        private final int maximumY;
        private final Palette palette;

        private PaletteCutoff(int maximumY, Palette palette) {
            this.maximumY = maximumY;
            this.palette = palette;
        }

        private int maximumY() {
            return maximumY;
        }
    }

    private static final class SlantLayer {

        private final double threshold;
        private final PaletteHolder palette;

        private SlantLayer(double threshold, PaletteHolder palette) {
            this.threshold = threshold;
            this.palette = palette;
        }

        private double threshold() {
            return threshold;
        }
    }
}
