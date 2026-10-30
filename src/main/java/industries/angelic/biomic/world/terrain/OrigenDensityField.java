package industries.angelic.biomic.world.terrain;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLongArray;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.world.WorldgenContext;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;

/**
 * Terra 6.4.2's Origen density interpolation and biome blending, with only the vertical coordinate
 * transformed to fit the complete authored -64..319 range into a 1.7.10 world.
 */
final class OrigenDensityField {

    static final int UPSTREAM_MIN_Y = -64;
    static final int UPSTREAM_SEA_LEVEL = 62;
    static final int UPSTREAM_MAX_Y_EXCLUSIVE = 320;
    static final int BIOMIC_SEA_LEVEL = 63;
    static final int BIOMIC_MAX_Y_EXCLUSIVE = 256;

    private static final int TERRA_GRID = 4;
    private static final int CHUNK_GRID_WIDTH = BiomicTerrainSampler.CHUNK_WIDTH / TERRA_GRID + 1;
    private static final int UPSTREAM_GRID_HEIGHT = (UPSTREAM_MAX_Y_EXCLUSIVE - UPSTREAM_MIN_Y) / TERRA_GRID + 1;
    private static final int PROFILE_PLANE_PADDING = 3;
    private static final int PROFILE_PLANE_WIDTH = CHUNK_GRID_WIDTH + PROFILE_PLANE_PADDING * 2;
    private static final int PROFILE_PLANE_SIZE = PROFILE_PLANE_WIDTH * PROFILE_PLANE_WIDTH;
    private static final int CARVER_HORIZONTAL_GRID = 4;
    private static final int CARVER_VERTICAL_GRID = 2;
    private static final int CARVER_GRID_WIDTH = BiomicTerrainSampler.CHUNK_WIDTH / CARVER_HORIZONTAL_GRID + 1;
    private static final int CARVER_GRID_HEIGHT = (UPSTREAM_MAX_Y_EXCLUSIVE - UPSTREAM_MIN_Y) / CARVER_VERTICAL_GRID
        + 1;
    private static final int ELEVATION_SMOOTH_RADIUS = 4;
    private static final int ELEVATION_PLANE_WIDTH = BiomicTerrainSampler.CHUNK_WIDTH + ELEVATION_SMOOTH_RADIUS * 2;
    private static final int LATTICE_CACHE_SIZE = 131_072;
    private static final int ELEVATION_CACHE_SIZE = 65_536;
    private static final int PROFILE_CACHE_SIZE = 262_144;
    private static final int BOUNDARY_HEIGHT_CACHE_SIZE = 65_536;
    private static final int CHUNK_DENSITY_CACHE_SIZE = 512;
    private static final int CHUNK_CARVER_CACHE_SIZE = 512;
    private static final int CACHE_LOCK_STRIPES = 64;
    private static final int SNOWY_BOUNDARY_BLEND_MIN_Y = 48;
    private static final int SNOWY_BOUNDARY_BLEND_FULL_Y = 64;
    private static final int SHARED_FIELD_COUNT = 4;
    private static final OrigenBiomeProfile[] PROFILE_VALUES = OrigenBiomeProfile.values();
    private static final Map<FieldKey, OrigenDensityField> SHARED = new LinkedHashMap<FieldKey, OrigenDensityField>(
        SHARED_FIELD_COUNT,
        0.75F,
        true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<FieldKey, OrigenDensityField> eldest) {
            return size() > SHARED_FIELD_COUNT;
        }
    };

    private final long worldSeed;
    private final OrigenBiomePipeline pipeline;
    private final OrigenTerrainRuntime runtime;
    /** Per profile with arches, one admission per arch family (ArchLobeAdmission). */
    private final java.util.EnumMap<OrigenBiomeProfile, ArchLobeAdmission[]> archAdmissions = new java.util.EnumMap<>(
        OrigenBiomeProfile.class);
    private final SnowySpiresAdmission snowySpiresAdmission;
    private final LongDoubleCache latticeCache = new LongDoubleCache(LATTICE_CACHE_SIZE);
    private final LongDoubleCache elevationCache = new LongDoubleCache(ELEVATION_CACHE_SIZE);
    private final LongDoubleCache boundaryHeightCache = new LongDoubleCache(BOUNDARY_HEIGHT_CACHE_SIZE);
    private final LongProfileCache profileCache = new LongProfileCache(PROFILE_CACHE_SIZE);
    private final ThreadLocal<int[]> profileCounts = ThreadLocal.withInitial(() -> new int[PROFILE_VALUES.length]);
    private final ThreadLocal<boolean[]> missingLatticePlanes = ThreadLocal
        .withInitial(() -> new boolean[UPSTREAM_GRID_HEIGHT]);
    private final ThreadLocal<byte[]> extrudedProfileColumnScratch = ThreadLocal
        .withInitial(() -> new byte[UPSTREAM_GRID_HEIGHT]);
    private final ThreadLocal<byte[]> extrudedProfileVolumeScratch = ThreadLocal
        .withInitial(() -> new byte[UPSTREAM_GRID_HEIGHT * PROFILE_PLANE_SIZE]);
    private final ThreadLocal<OrigenBiomeProfile[]> elevationProfilePlaneScratch = ThreadLocal
        .withInitial(() -> new OrigenBiomeProfile[ELEVATION_PLANE_WIDTH * ELEVATION_PLANE_WIDTH]);
    private final ThreadLocal<double[]> upstreamDensityColumnScratch = ThreadLocal
        .withInitial(() -> new double[UPSTREAM_MAX_Y_EXCLUSIVE - UPSTREAM_MIN_Y]);
    private final Long2ObjectLinkedOpenHashMap<ScaledChunkDensity> chunkDensityCache = new Long2ObjectLinkedOpenHashMap<>(
        CHUNK_DENSITY_CACHE_SIZE);
    private final ThreadLocal<ChunkDensityLookup> lastChunkDensity = ThreadLocal.withInitial(ChunkDensityLookup::new);
    private final Long2ObjectLinkedOpenHashMap<UpstreamChunkCarver> chunkCarverCache = new Long2ObjectLinkedOpenHashMap<>(
        CHUNK_CARVER_CACHE_SIZE);
    private final ThreadLocal<ChunkCarverLookup> lastChunkCarver = ThreadLocal.withInitial(ChunkCarverLookup::new);

    OrigenDensityField(WorldgenContext context) {
        this(
            context.getSettings()
                .getWorldSeed(),
            OrigenTerrainRuntime.instance());
    }

    private OrigenDensityField(long worldSeed, OrigenTerrainRuntime runtime) {
        this.worldSeed = worldSeed;
        this.pipeline = new OrigenBiomePipeline(worldSeed);
        this.runtime = runtime;
        ArchLobeAdmission[] loops = {
            new ArchLobeAdmission(ArchLobeAdmission.LUSH_LOOPS, worldSeed, pipeline, runtime) };
        archAdmissions.put(OrigenBiomeProfile.LUSH_LOOPS, loops);
        archAdmissions.put(OrigenBiomeProfile.LOOPS_RIVER, loops);
        archAdmissions.put(
            OrigenBiomeProfile.STONEGATE_SEAS,
            new ArchLobeAdmission[] {
                new ArchLobeAdmission(ArchLobeAdmission.STONEGATE_SEAS_A, worldSeed, pipeline, runtime),
                new ArchLobeAdmission(ArchLobeAdmission.STONEGATE_SEAS_B, worldSeed, pipeline, runtime) });
        archAdmissions.put(
            OrigenBiomeProfile.ARCTIC_ARCHES,
            new ArchLobeAdmission[] {
                new ArchLobeAdmission(ArchLobeAdmission.ARCTIC_ARCHES_A, worldSeed, pipeline, runtime),
                new ArchLobeAdmission(ArchLobeAdmission.ARCTIC_ARCHES_B, worldSeed, pipeline, runtime) });
        this.snowySpiresAdmission = new SnowySpiresAdmission(worldSeed, pipeline, runtime);
    }

    record FieldKey(long seed) {}

    static FieldKey key(WorldgenContext context) {
        return new FieldKey(
            context.getSettings()
                .getWorldSeed());
    }

    /** The blend and Origen backup must never share a field with different sinkhole routing. */
    static OrigenDensityField shared(WorldgenContext context) {
        FieldKey key = key(context);
        synchronized (SHARED) {
            OrigenDensityField field = SHARED.get(key);
            if (field == null) {
                field = new OrigenDensityField(context);
                SHARED.put(key, field);
            }
            return field;
        }
    }

    OrigenProvinceSample sampleProvince(int blockX, int blockZ) {
        return pipeline.sample(blockX, blockZ);
    }

    /** The climate band of the biome cell at a column: 0 cold, 1 medium, 2 warm. */
    int climateBand(int blockX, int blockZ) {
        return pipeline.band(blockX, blockZ);
    }

    OrigenBiomeProfile sampleSurfaceProfile(int blockX, int blockZ) {
        return pipeline.sampleProfile(blockX, blockZ);
    }

    OrigenBiomeProfile extrudeProfile(OrigenBiomeProfile surfaceProfile, int blockX, int upstreamY, int blockZ) {
        return runtime.extrude(surfaceProfile, worldSeed, blockX, upstreamY, blockZ);
    }

    void fillExtrudedProfiles(OrigenBiomeProfile surfaceProfile, int blockX, int blockZ, byte[] destination,
        int offset) {
        runtime.fillExtrudedProfiles(surfaceProfile, worldSeed, blockX, blockZ, destination, offset);
    }

    /** Returns the exact extruded Origen profile at an upstream Terra coordinate. */
    OrigenBiomeProfile sampleProfile(int blockX, int upstreamY, int blockZ) {
        long key = coordinateKey(blockX, upstreamY, blockZ);
        OrigenBiomeProfile cached = profileCache.get(key);
        if (cached != null) {
            return cached;
        }
        OrigenBiomeProfile sampled = runtime
            .extrude(pipeline.sampleProfile(blockX, blockZ), worldSeed, blockX, upstreamY, blockZ);
        return profileCache.putIfAbsent(key, sampled);
    }

    /** Samples the scaled world at an integer 1.7.10 block coordinate. */
    double sampleScaledDensity(int blockX, int biomicY, int blockZ) {
        return sampleUpstreamInterpolated(blockX, toUpstreamY(biomicY), blockZ);
    }

    /**
     * Materializes Terra's authored density lattice for one chunk. The returned view performs the one and only
     * trilinear interpolation while mapping physical block Y back into authored space.
     */
    ScaledChunkDensity createScaledChunkDensity(int chunkX, int chunkZ) {
        long key = horizontalKey(chunkX, chunkZ);
        ChunkDensityLookup local = lastChunkDensity.get();
        if (local.present && local.key == key) return local.density;
        synchronized (chunkDensityCache) {
            ScaledChunkDensity cached = chunkDensityCache.getAndMoveToLast(key);
            if (cached != null) {
                local.set(key, cached);
                return cached;
            }
        }
        ScaledChunkDensity created = computeScaledChunkDensity(chunkX, chunkZ);
        synchronized (chunkDensityCache) {
            ScaledChunkDensity raced = chunkDensityCache.getAndMoveToLast(key);
            ScaledChunkDensity selected = raced == null ? created : raced;
            if (raced == null) {
                chunkDensityCache.putAndMoveToLast(key, created);
                if (chunkDensityCache.size() > CHUNK_DENSITY_CACHE_SIZE) chunkDensityCache.removeFirst();
            }
            local.set(key, selected);
            return selected;
        }
    }

    private ScaledChunkDensity computeScaledChunkDensity(int chunkX, int chunkZ) {
        double[] lattice = new double[CHUNK_GRID_WIDTH * CHUNK_GRID_WIDTH * UPSTREAM_GRID_HEIGHT];
        int worldStartX = chunkX << 4;
        int worldStartZ = chunkZ << 4;
        boolean[] missingPlanes = missingLatticePlanes.get();
        Arrays.fill(missingPlanes, false);
        int missingPlaneCount = 0;
        for (int gridY = 0; gridY < UPSTREAM_GRID_HEIGHT; gridY++) {
            int upstreamY = UPSTREAM_MIN_Y + gridY * TERRA_GRID;
            boolean hasMissingSample = false;
            for (int gridX = 0; gridX < CHUNK_GRID_WIDTH; gridX++) {
                int blockX = worldStartX + gridX * TERRA_GRID;
                for (int gridZ = 0; gridZ < CHUNK_GRID_WIDTH; gridZ++) {
                    int blockZ = worldStartZ + gridZ * TERRA_GRID;
                    double cached = latticeCache.get(coordinateKey(blockX, upstreamY, blockZ));
                    lattice[latticeIndex(gridX, gridZ, gridY)] = cached;
                    hasMissingSample |= Double.isNaN(cached);
                }
            }
            missingPlanes[gridY] = hasMissingSample;
            if (hasMissingSample) missingPlaneCount++;
        }

        if (missingPlaneCount > 0) {
            byte[] profileColumn = extrudedProfileColumnScratch.get();
            byte[] profileVolume = extrudedProfileVolumeScratch.get();
            for (int planeX = 0; planeX < PROFILE_PLANE_WIDTH; planeX++) {
                int blockX = worldStartX + (planeX - PROFILE_PLANE_PADDING) * TERRA_GRID;
                for (int planeZ = 0; planeZ < PROFILE_PLANE_WIDTH; planeZ++) {
                    int blockZ = worldStartZ + (planeZ - PROFILE_PLANE_PADDING) * TERRA_GRID;
                    int planeIndex = planeX * PROFILE_PLANE_WIDTH + planeZ;
                    OrigenBiomeProfile surfaceProfile = pipeline.sampleProfile(blockX, blockZ);
                    runtime.fillExtrudedProfilesStrided(
                        surfaceProfile,
                        worldSeed,
                        blockX,
                        blockZ,
                        UPSTREAM_MIN_Y,
                        UPSTREAM_MAX_Y_EXCLUSIVE + TERRA_GRID,
                        TERRA_GRID,
                        profileColumn,
                        0);
                    for (int gridY = 0; gridY < UPSTREAM_GRID_HEIGHT; gridY++) {
                        profileVolume[gridY * PROFILE_PLANE_SIZE + planeIndex] = profileColumn[gridY];
                    }
                }
            }

            int[] counts = profileCounts.get();
            for (int gridY = 0; gridY < UPSTREAM_GRID_HEIGHT; gridY++) {
                if (!missingPlanes[gridY]) continue;
                int upstreamY = UPSTREAM_MIN_Y + gridY * TERRA_GRID;
                int profilePlaneOffset = gridY * PROFILE_PLANE_SIZE;
                for (int gridX = 0; gridX < CHUNK_GRID_WIDTH; gridX++) {
                    int blockX = worldStartX + gridX * TERRA_GRID;
                    for (int gridZ = 0; gridZ < CHUNK_GRID_WIDTH; gridZ++) {
                        int latticeIndex = latticeIndex(gridX, gridZ, gridY);
                        if (!Double.isNaN(lattice[latticeIndex])) continue;
                        Arrays.fill(counts, 0);
                        int distinctProfiles = 0;
                        for (int planeX = gridX; planeX <= gridX + PROFILE_PLANE_PADDING * 2; planeX++) {
                            int planeOffset = profilePlaneOffset + planeX * PROFILE_PLANE_WIDTH + gridZ;
                            for (int planeZ = 0; planeZ <= PROFILE_PLANE_PADDING * 2; planeZ++) {
                                int ordinal = profileVolume[planeOffset + planeZ] & 0xFF;
                                if (counts[ordinal]++ == 0) distinctProfiles++;
                            }
                        }
                        int blockZ = worldStartZ + gridZ * TERRA_GRID;
                        double sampled = computeBlendedLattice(blockX, upstreamY, blockZ, counts, distinctProfiles);
                        lattice[latticeIndex] = latticeCache
                            .putIfAbsent(coordinateKey(blockX, upstreamY, blockZ), sampled);
                    }
                }
            }
        }

        double[] elevation = computeChunkElevations(worldStartX, worldStartZ);
        return new ScaledChunkDensity(lattice, elevation);
    }

    private double[] computeChunkElevations(int worldStartX, int worldStartZ) {
        double[] elevations = new double[BiomicTerrainSampler.CHUNK_WIDTH * BiomicTerrainSampler.CHUNK_WIDTH];
        boolean missing = false;
        for (int localX = 0; localX < BiomicTerrainSampler.CHUNK_WIDTH; localX++) {
            for (int localZ = 0; localZ < BiomicTerrainSampler.CHUNK_WIDTH; localZ++) {
                double cached = elevationCache.get(horizontalKey(worldStartX + localX, worldStartZ + localZ));
                elevations[localX * BiomicTerrainSampler.CHUNK_WIDTH + localZ] = cached;
                missing |= Double.isNaN(cached);
            }
        }
        if (!missing) return elevations;

        OrigenBiomeProfile[] profilePlane = elevationProfilePlaneScratch.get();
        for (int planeX = 0; planeX < ELEVATION_PLANE_WIDTH; planeX++) {
            int blockX = worldStartX + planeX - ELEVATION_SMOOTH_RADIUS;
            for (int planeZ = 0; planeZ < ELEVATION_PLANE_WIDTH; planeZ++) {
                int blockZ = worldStartZ + planeZ - ELEVATION_SMOOTH_RADIUS;
                profilePlane[planeX * ELEVATION_PLANE_WIDTH + planeZ] = pipeline.sampleProfile(blockX, blockZ);
            }
        }

        int[] counts = profileCounts.get();
        for (int localX = 0; localX < BiomicTerrainSampler.CHUNK_WIDTH; localX++) {
            int blockX = worldStartX + localX;
            for (int localZ = 0; localZ < BiomicTerrainSampler.CHUNK_WIDTH; localZ++) {
                int elevationIndex = localX * BiomicTerrainSampler.CHUNK_WIDTH + localZ;
                if (!Double.isNaN(elevations[elevationIndex])) continue;
                OrigenBiomeProfile center = profilePlane[(localX + ELEVATION_SMOOTH_RADIUS) * ELEVATION_PLANE_WIDTH
                    + localZ
                    + ELEVATION_SMOOTH_RADIUS];
                Arrays.fill(counts, 0);
                boolean same = true;
                for (int offsetX = 0; offsetX <= ELEVATION_SMOOTH_RADIUS * 2; offsetX++) {
                    int planeOffset = (localX + offsetX) * ELEVATION_PLANE_WIDTH + localZ;
                    for (int offsetZ = 0; offsetZ <= ELEVATION_SMOOTH_RADIUS * 2; offsetZ++) {
                        OrigenBiomeProfile profile = profilePlane[planeOffset + offsetZ];
                        same &= profile == center;
                        counts[profile.ordinal()]++;
                    }
                }
                int blockZ = worldStartZ + localZ;
                double sampled = computeElevation(center, blockX, blockZ, counts, same);
                elevations[elevationIndex] = elevationCache.putIfAbsent(horizontalKey(blockX, blockZ), sampled);
            }
        }
        return elevations;
    }

    private static final class ChunkDensityLookup {

        private boolean present;
        private long key;
        private ScaledChunkDensity density;

        private void set(long key, ScaledChunkDensity density) {
            present = true;
            this.key = key;
            this.density = density;
        }
    }

    /** Highest positive density in the scaled Origen field, including disconnected authored mass. */
    int sampleScaledSurfaceHeight(int blockX, int blockZ) {
        int chunkX = Math.floorDiv(blockX, BiomicTerrainSampler.CHUNK_WIDTH);
        int chunkZ = Math.floorDiv(blockZ, BiomicTerrainSampler.CHUNK_WIDTH);
        int localX = Math.floorMod(blockX, BiomicTerrainSampler.CHUNK_WIDTH);
        int localZ = Math.floorMod(blockZ, BiomicTerrainSampler.CHUNK_WIDTH);
        ScaledChunkDensity density = createScaledChunkDensity(chunkX, chunkZ);
        double upperDensity = density.sampleUpstream(localX, UPSTREAM_MAX_Y_EXCLUSIVE, localZ);
        for (int upstreamY = UPSTREAM_MAX_Y_EXCLUSIVE - TERRA_GRID; upstreamY
            >= UPSTREAM_MIN_Y; upstreamY -= TERRA_GRID) {
            double lowerDensity = density.sampleUpstream(localX, upstreamY, localZ);
            if (upperDensity > 0.0D || lowerDensity > 0.0D) {
                int maximumBiomicY = Math
                    .min(BIOMIC_MAX_Y_EXCLUSIVE - 1, (int) Math.ceil(toBiomicY(upstreamY + TERRA_GRID)));
                int minimumBiomicY = Math.max(1, (int) Math.floor(toBiomicY(upstreamY)) - 1);
                for (int biomicY = maximumBiomicY; biomicY >= minimumBiomicY; biomicY--) {
                    if (density.sample(localX, biomicY, localZ) > 0.0D) {
                        return biomicY;
                    }
                }
            }
            upperDensity = lowerDensity;
        }
        return 1;
    }

    /**
     * Samples the result of Terra's native 4x4x4 interpolation at an upstream vertical coordinate.
     * Horizontal coordinates remain unscaled.
     */
    double sampleUpstreamInterpolated(int blockX, double upstreamY, int blockZ) {
        return sampleUpstreamInterpolated((double) blockX, upstreamY, (double) blockZ);
    }

    /** Terra's chunk interpolator accepts fractional coordinates when calculating palette slant. */
    double sampleUpstreamInterpolated(double blockX, double upstreamY, double blockZ) {
        int roundedX = (int) Math.round(blockX);
        int roundedZ = (int) Math.round(blockZ);
        int lowerX = (int) Math.floor(blockX / TERRA_GRID) * TERRA_GRID;
        int lowerZ = (int) Math.floor(blockZ / TERRA_GRID) * TERRA_GRID;
        int lowerY = (int) Math.floor(upstreamY / TERRA_GRID) * TERRA_GRID;
        int upperX = lowerX + TERRA_GRID;
        int upperZ = lowerZ + TERRA_GRID;
        int upperY = lowerY + TERRA_GRID;
        double progressX = (blockX - lowerX) / (double) TERRA_GRID;
        double progressY = (upstreamY - lowerY) / TERRA_GRID;
        double progressZ = (blockZ - lowerZ) / (double) TERRA_GRID;

        double lowerNorthWest = sampleBlendedLattice(lowerX, lowerY, lowerZ);
        double lowerNorthEast = progressX == 0.0D ? lowerNorthWest : sampleBlendedLattice(upperX, lowerY, lowerZ);
        double lowerSouthWest = progressZ == 0.0D ? lowerNorthWest : sampleBlendedLattice(lowerX, lowerY, upperZ);
        double lowerSouthEast;
        if (progressX == 0.0D) {
            lowerSouthEast = lowerSouthWest;
        } else if (progressZ == 0.0D) {
            lowerSouthEast = lowerNorthEast;
        } else {
            lowerSouthEast = sampleBlendedLattice(upperX, lowerY, upperZ);
        }

        double upperNorthWest = progressY == 0.0D ? lowerNorthWest : sampleBlendedLattice(lowerX, upperY, lowerZ);
        double upperNorthEast;
        double upperSouthWest;
        double upperSouthEast;
        if (progressY == 0.0D) {
            upperNorthEast = lowerNorthEast;
            upperSouthWest = lowerSouthWest;
            upperSouthEast = lowerSouthEast;
        } else {
            upperNorthEast = progressX == 0.0D ? upperNorthWest : sampleBlendedLattice(upperX, upperY, lowerZ);
            upperSouthWest = progressZ == 0.0D ? upperNorthWest : sampleBlendedLattice(lowerX, upperY, upperZ);
            if (progressX == 0.0D) {
                upperSouthEast = upperSouthWest;
            } else if (progressZ == 0.0D) {
                upperSouthEast = upperNorthEast;
            } else {
                upperSouthEast = sampleBlendedLattice(upperX, upperY, upperZ);
            }
        }

        double lowerNorth = lerp(lowerNorthWest, lowerNorthEast, progressX);
        double lowerSouth = lerp(lowerSouthWest, lowerSouthEast, progressX);
        double upperNorth = lerp(upperNorthWest, upperNorthEast, progressX);
        double upperSouth = lerp(upperSouthWest, upperSouthEast, progressX);
        double base = lerp(lerp(lowerNorth, lowerSouth, progressZ), lerp(upperNorth, upperSouth, progressZ), progressY);
        return base + sampleElevation(roundedX, roundedZ);
    }

    /** Executes Terra 6.4.2's stateful, chunk-local lazy carver interpolator. */
    double sampleUpstreamCarverInterpolated(int blockX, double upstreamY, int blockZ) {
        int chunkX = Math.floorDiv(blockX, BiomicTerrainSampler.CHUNK_WIDTH);
        int chunkZ = Math.floorDiv(blockZ, BiomicTerrainSampler.CHUNK_WIDTH);
        return chunkCarver(chunkX, chunkZ).sample(
            Math.floorMod(blockX, BiomicTerrainSampler.CHUNK_WIDTH),
            upstreamY,
            Math.floorMod(blockZ, BiomicTerrainSampler.CHUNK_WIDTH));
    }

    /** Raw final-terrain predicate used by bounded, deterministic connected-component polish passes. */
    boolean isUnfilteredSolidAtUpstream(int blockX, int upstreamY, int blockZ) {
        return sampleUpstreamInterpolated(blockX, upstreamY, blockZ) > 0.0D
            && sampleUpstreamCarverInterpolated(blockX, upstreamY, blockZ) <= 0.0D;
    }

    UpstreamChunkCarver chunkCarver(int chunkX, int chunkZ) {
        long key = horizontalKey(chunkX, chunkZ);
        ChunkCarverLookup local = lastChunkCarver.get();
        if (local.present && local.key == key) return local.carver;
        synchronized (chunkCarverCache) {
            UpstreamChunkCarver cached = chunkCarverCache.getAndMoveToLast(key);
            if (cached != null) {
                local.set(key, cached);
                return cached;
            }
        }
        UpstreamChunkCarver created = new UpstreamChunkCarver(chunkX, chunkZ, createScaledChunkDensity(chunkX, chunkZ));
        synchronized (chunkCarverCache) {
            UpstreamChunkCarver raced = chunkCarverCache.getAndMoveToLast(key);
            UpstreamChunkCarver selected = raced == null ? created : raced;
            if (raced == null) {
                chunkCarverCache.putAndMoveToLast(key, created);
                if (chunkCarverCache.size() > CHUNK_CARVER_CACHE_SIZE) chunkCarverCache.removeFirst();
            }
            local.set(key, selected);
            return selected;
        }
    }

    private static final class ChunkCarverLookup {

        private boolean present;
        private long key;
        private UpstreamChunkCarver carver;

        private void set(long key, UpstreamChunkCarver carver) {
            present = true;
            this.key = key;
            this.carver = carver;
        }
    }

    double sampleBlendedLattice(int blockX, int upstreamY, int blockZ) {
        long key = coordinateKey(blockX, upstreamY, blockZ);
        double cached = latticeCache.get(key);
        if (!Double.isNaN(cached)) {
            return cached;
        }
        OrigenBiomeProfile center = sampleProfile(blockX, upstreamY, blockZ);
        OrigenTerrainRuntime.ProfileGraph centerGraph = runtime.profile(center);
        int blend = centerGraph.getBlendDistance();
        int step = centerGraph.getBlendStep();
        int[] counts = profileCounts.get();
        Arrays.fill(counts, 0);
        int distinctProfiles = 0;
        for (int offsetX = -blend; offsetX <= blend; offsetX++) {
            for (int offsetZ = -blend; offsetZ <= blend; offsetZ++) {
                OrigenBiomeProfile profile = sampleProfile(blockX + offsetX * step, upstreamY, blockZ + offsetZ * step);
                int ordinal = profile.ordinal();
                if (counts[ordinal]++ == 0) distinctProfiles++;
            }
        }
        double sampled = computeBlendedLattice(blockX, upstreamY, blockZ, counts, distinctProfiles);
        return latticeCache.putIfAbsent(key, sampled);
    }

    private double computeBlendedLattice(int blockX, int upstreamY, int blockZ, int[] counts, int distinctProfiles) {
        double weightedDensity = 0.0D;
        double weightedRawDensity = 0.0D;
        double totalWeight = 0.0D;
        boolean mixedSnowySpiresBoundary = upstreamY >= SNOWY_BOUNDARY_BLEND_MIN_Y
            && counts[OrigenBiomeProfile.SNOWY_SPIRES.ordinal()] != 0
            && distinctProfiles > 1;
        for (int ordinal = 0; ordinal < counts.length; ordinal++) {
            int count = counts[ordinal];
            if (count == 0) continue;
            OrigenBiomeProfile profile = PROFILE_VALUES[ordinal];
            double weight = runtime.profile(profile)
                .getBlendWeight() * count;
            if (mixedSnowySpiresBoundary) {
                // EQ_PILLARS bounds ordinary ground density to [-2, 3], while the neighboring coastal erosion
                // graph can return values in the hundreds. Raw density averaging therefore jumps between two
                // incompatible scales when the last coastal sample leaves Terra's stencil. Blend their highest
                // base zero-crossings instead: this keeps the authored heights and carvers, but turns a rejected
                // boundary spire into a continuous ground envelope instead of a one-block terracotta shelf.
                weightedDensity += sampleBoundaryBaseHeight(profile, blockX, blockZ) * weight;
                if (upstreamY < SNOWY_BOUNDARY_BLEND_FULL_Y) {
                    weightedRawDensity += sampleBoundarySafeBase(profile, blockX, upstreamY, blockZ) * weight;
                }
            } else {
                weightedDensity += sampleBoundarySafeBase(profile, blockX, upstreamY, blockZ) * weight;
            }
            totalWeight += weight;
        }
        double sampled = weightedDensity / totalWeight;
        if (mixedSnowySpiresBoundary) {
            sampled -= upstreamY;
            if (upstreamY < SNOWY_BOUNDARY_BLEND_FULL_Y) {
                double progress = (upstreamY - SNOWY_BOUNDARY_BLEND_MIN_Y)
                    / (double) (SNOWY_BOUNDARY_BLEND_FULL_Y - SNOWY_BOUNDARY_BLEND_MIN_Y);
                sampled = lerp(weightedRawDensity / totalWeight, sampled, progress);
            }
        }
        return sampled;
    }

    private double sampleBoundaryBaseHeight(OrigenBiomeProfile profile, int blockX, int blockZ) {
        long key = coordinateKey(blockX, profile.ordinal(), blockZ);
        double cached = boundaryHeightCache.get(key);
        if (!Double.isNaN(cached)) return cached;

        double upperDensity = sampleBoundarySafeBase(profile, blockX, UPSTREAM_MAX_Y_EXCLUSIVE, blockZ);
        if (upperDensity > 0.0D) {
            return boundaryHeightCache.putIfAbsent(key, UPSTREAM_MAX_Y_EXCLUSIVE);
        }
        for (int lowerY = UPSTREAM_MAX_Y_EXCLUSIVE - TERRA_GRID; lowerY >= UPSTREAM_MIN_Y; lowerY -= TERRA_GRID) {
            double lowerDensity = sampleBoundarySafeBase(profile, blockX, lowerY, blockZ);
            if (lowerDensity > 0.0D) {
                double progress = lowerDensity / (lowerDensity - upperDensity);
                double height = lowerY + progress * TERRA_GRID;
                return boundaryHeightCache.putIfAbsent(key, height);
            }
            upperDensity = lowerDensity;
        }
        return boundaryHeightCache.putIfAbsent(key, UPSTREAM_MIN_Y);
    }

    /**
     * A profile's own ground height at a column, unblended: the zero crossing of its base graph nearest the guess,
     * found by stepping as far as the density says (exact in one step for Origen's usual -y + height graphs) and
     * interpolated between the two rows that bracket it.
     */
    double sampleProfileGround(OrigenBiomeProfile profile, int blockX, int blockZ, int guess) {
        int a = clamp(guess, UPSTREAM_MIN_Y, UPSTREAM_MAX_Y_EXCLUSIVE - 1), b = a;
        double densityA = sampleBoundarySafeBase(profile, blockX, a, blockZ), densityB = densityA;
        boolean bracketed = false;
        for (int iteration = 0; iteration < 16 && !bracketed; iteration++) {
            int step = (int) Math.max(-32, Math.min(32, Math.round(densityA)));
            if (step == 0) step = densityA > 0.0D ? 1 : -1;
            b = clamp(a + step, UPSTREAM_MIN_Y, UPSTREAM_MAX_Y_EXCLUSIVE - 1);
            if (b == a) return a;
            densityB = sampleBoundarySafeBase(profile, blockX, b, blockZ);
            bracketed = (densityA > 0.0D) != (densityB > 0.0D);
            if (!bracketed) {
                a = b;
                densityA = densityB;
            }
        }
        if (!bracketed) return a;
        while (Math.abs(b - a) > 1) {
            int middle = (a + b) >> 1;
            double densityM = sampleBoundarySafeBase(profile, blockX, middle, blockZ);
            if ((densityM > 0.0D) == (densityA > 0.0D)) {
                a = middle;
                densityA = densityM;
            } else {
                b = middle;
                densityB = densityM;
            }
        }
        return a + densityA / (densityA - densityB) * (b - a);
    }

    double sampleElevation(int blockX, int blockZ) {
        long key = horizontalKey(blockX, blockZ);
        double cached = elevationCache.get(key);
        if (!Double.isNaN(cached)) {
            return cached;
        }
        OrigenBiomeProfile center = pipeline.sampleProfile(blockX, blockZ);
        int[] counts = profileCounts.get();
        Arrays.fill(counts, 0);
        boolean same = true;
        for (int offsetX = -ELEVATION_SMOOTH_RADIUS; offsetX <= ELEVATION_SMOOTH_RADIUS; offsetX++) {
            for (int offsetZ = -ELEVATION_SMOOTH_RADIUS; offsetZ <= ELEVATION_SMOOTH_RADIUS; offsetZ++) {
                OrigenBiomeProfile profile = pipeline.sampleProfile(blockX + offsetX, blockZ + offsetZ);
                same &= profile == center;
                counts[profile.ordinal()]++;
            }
        }
        double sampled = computeElevation(center, blockX, blockZ, counts, same);
        return elevationCache.putIfAbsent(key, sampled);
    }

    private double computeElevation(OrigenBiomeProfile center, int blockX, int blockZ, int[] counts, boolean same) {
        if (same) return runtime.sampleElevation(center, worldSeed, blockX, blockZ);
        double weightedElevation = 0.0D;
        double totalWeight = 0.0D;
        for (int ordinal = 0; ordinal < counts.length; ordinal++) {
            int count = counts[ordinal];
            if (count == 0) continue;
            OrigenBiomeProfile profile = PROFILE_VALUES[ordinal];
            double weight = runtime.profile(profile)
                .getElevationWeight() * count;
            weightedElevation += runtime.sampleElevation(profile, worldSeed, blockX, blockZ) * weight;
            totalWeight += weight;
        }
        return weightedElevation / totalWeight;
    }

    private double sampleBoundarySafeBase(OrigenBiomeProfile profile, int blockX, int upstreamY, int blockZ) {
        ArchLobeAdmission[] arches = archAdmissions.get(profile);
        if (arches != null) {
            int dropped = 0;
            for (ArchLobeAdmission arch : arches) {
                if (!arch.isAdmitted(blockX, blockZ)) dropped |= arch.shape()
                    .bit();
            }
            if (dropped != 0)
                return runtime.sampleBaseWithoutArches(profile, dropped, worldSeed, blockX, upstreamY, blockZ);
        }
        if (profile == OrigenBiomeProfile.SNOWY_SPIRES && !snowySpiresAdmission.isAdmitted(blockX, blockZ)) {
            return runtime.sampleBaseWithoutSnowySpiresPillars(profile, worldSeed, blockX, upstreamY, blockZ);
        }
        return runtime.sampleBase(profile, worldSeed, blockX, upstreamY, blockZ);
    }

    static double toUpstreamY(double biomicY) {
        return OrigenVerticalTransform.toAuthoredY(biomicY);
    }

    static double toBiomicY(double upstreamY) {
        return OrigenVerticalTransform.toPhysicalY(upstreamY);
    }

    private static long coordinateKey(int blockX, int blockY, int blockZ) {
        return ((long) blockX & 0x3FF_FFFFL) << 38 | ((long) blockZ & 0x3FF_FFFFL) << 12 | (blockY & 0xFFFL);
    }

    private static long horizontalKey(int blockX, int blockZ) {
        return (long) blockX << 32 ^ blockZ & 0xFFFF_FFFFL;
    }

    private static int latticeIndex(int gridX, int gridZ, int gridY) {
        return (gridX * CHUNK_GRID_WIDTH + gridZ) * UPSTREAM_GRID_HEIGHT + gridY;
    }

    private static double lerp(double first, double second, double progress) {
        return first + (second - first) * progress;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    /**
     * Faithful copy of Terra's {@code LazilyEvaluatedInterpolator}, including its observable cache-fill order.
     * A cache node is sampled at the coordinates of the first solid voxel which asks for it, not necessarily at
     * the nominal 4x2x4 lattice coordinate. Sparse upper terrain can consequently move a carver crossing by a few
     * blocks; eagerly sampling a regular lattice loses that behavior.
     */
    final class UpstreamChunkCarver {

        private final int chunkOriginX;
        private final int chunkOriginZ;
        private final double[] samples = new double[CARVER_GRID_WIDTH * CARVER_GRID_WIDTH * CARVER_GRID_HEIGHT];
        /**
         * True while the constructor primes the solid voxels' nodes in canonical order (Terra samples a lazy node at
         * the position of the voxel that first needs it). Afterwards a node still unset is sampled at its own corner:
         * the carver is cached and shared, and taking the first later caller's position made a node's value depend on
         * which chunk or feature asked first (order-dependent cave edges and features).
         */
        private boolean priming;

        private UpstreamChunkCarver(int chunkX, int chunkZ, ScaledChunkDensity density) {
            chunkOriginX = chunkX << 4;
            chunkOriginZ = chunkZ << 4;
            java.util.Arrays.fill(samples, Double.NaN);
            priming = true;
            double[] densityColumn = upstreamDensityColumnScratch.get();
            for (int localX = 0; localX < BiomicTerrainSampler.CHUNK_WIDTH; localX++) {
                for (int localZ = 0; localZ < BiomicTerrainSampler.CHUNK_WIDTH; localZ++) {
                    density.fillUpstreamColumn(localX, localZ, densityColumn);
                    for (int authoredY = UPSTREAM_MAX_Y_EXCLUSIVE - 1; authoredY >= UPSTREAM_MIN_Y; authoredY--) {
                        if (densityColumn[authoredY - UPSTREAM_MIN_Y] > 0.0D) {
                            primeInteger(localX, authoredY, localZ);
                        }
                    }
                }
            }
            priming = false;
        }

        // The field/cache is shared by source workers and the owner. Canonical solid-voxel
        // priming remains constructor-local; serialize any remaining lazy samples and readers.
        synchronized double sample(int localX, double authoredY, int localZ) {
            int integerY = (int) authoredY;
            if (authoredY == integerY) return sampleInteger(localX, integerY, localZ);

            int lowerY = (int) Math.floor(authoredY / CARVER_VERTICAL_GRID) * CARVER_VERTICAL_GRID;
            int yIndex = clamp((lowerY - UPSTREAM_MIN_Y) / CARVER_VERTICAL_GRID, 0, CARVER_GRID_HEIGHT - 2);
            double progressY = (authoredY - lowerY) / CARVER_VERTICAL_GRID;
            return sampleCell(localX, localZ, yIndex, lowerY + 1, progressY);
        }

        synchronized void fillIntegerColumn(int localX, int localZ, double[] densityColumn, double[] destination) {
            int xIndex = localX / CARVER_HORIZONTAL_GRID;
            int zIndex = localZ / CARVER_HORIZONTAL_GRID;
            double progressX = localX % CARVER_HORIZONTAL_GRID / (double) CARVER_HORIZONTAL_GRID;
            double progressZ = localZ % CARVER_HORIZONTAL_GRID / (double) CARVER_HORIZONTAL_GRID;
            boolean horizontalCorner = localX % CARVER_HORIZONTAL_GRID == 0 && localZ % CARVER_HORIZONTAL_GRID == 0;
            for (int yIndex = 0; yIndex < CARVER_GRID_HEIGHT - 1; yIndex++) {
                int offset = yIndex * CARVER_VERTICAL_GRID;
                boolean lowerSolid = densityColumn[offset] > 0.0D;
                boolean upperSolid = densityColumn[offset + 1] > 0.0D;
                if (!lowerSolid && !upperSolid) continue;

                if (lowerSolid && horizontalCorner) {
                    destination[offset] = samples[nodeIndex(xIndex, yIndex, zIndex)];
                }
                if (upperSolid || lowerSolid && !horizontalCorner) {
                    double lower = sampleHorizontal(xIndex, yIndex, zIndex, progressX, progressZ);
                    if (lowerSolid && !horizontalCorner) destination[offset] = lower;
                    if (upperSolid) {
                        double upper = sampleHorizontal(xIndex, yIndex + 1, zIndex, progressX, progressZ);
                        destination[offset + 1] = lerp(lower, upper, 0.5D);
                    }
                }
            }
        }

        private double sampleHorizontal(int xIndex, int yIndex, int zIndex, double progressX, double progressZ) {
            double west = lerp(
                samples[nodeIndex(xIndex, yIndex, zIndex)],
                samples[nodeIndex(xIndex, yIndex, zIndex + 1)],
                progressZ);
            double east = lerp(
                samples[nodeIndex(xIndex + 1, yIndex, zIndex)],
                samples[nodeIndex(xIndex + 1, yIndex, zIndex + 1)],
                progressZ);
            return lerp(west, east, progressX);
        }

        private int nodeIndex(int xIndex, int yIndex, int zIndex) {
            return xIndex + zIndex * CARVER_GRID_WIDTH + yIndex * CARVER_GRID_WIDTH * CARVER_GRID_WIDTH;
        }

        private double sampleInteger(int localX, int authoredY, int localZ) {
            int yIndex = clamp((authoredY - UPSTREAM_MIN_Y) / CARVER_VERTICAL_GRID, 0, CARVER_GRID_HEIGHT - 2);
            double progressY = Math.floorMod(authoredY, CARVER_VERTICAL_GRID) / (double) CARVER_VERTICAL_GRID;
            return sampleCell(localX, localZ, yIndex, authoredY, progressY);
        }

        private void primeInteger(int localX, int authoredY, int localZ) {
            int yIndex = clamp((authoredY - UPSTREAM_MIN_Y) / CARVER_VERTICAL_GRID, 0, CARVER_GRID_HEIGHT - 2);
            double progressY = Math.floorMod(authoredY, CARVER_VERTICAL_GRID) / (double) CARVER_VERTICAL_GRID;
            int xIndex = localX / CARVER_HORIZONTAL_GRID;
            int zIndex = localZ / CARVER_HORIZONTAL_GRID;
            sampleNode(xIndex, yIndex, zIndex, localX, authoredY, localZ);
            if (localX % CARVER_HORIZONTAL_GRID == 0 && progressY == 0.0D && localZ % CARVER_HORIZONTAL_GRID == 0) {
                return;
            }
            sampleNode(xIndex, yIndex, zIndex + 1, localX, authoredY, localZ + CARVER_HORIZONTAL_GRID);
            sampleNode(xIndex + 1, yIndex, zIndex, localX + CARVER_HORIZONTAL_GRID, authoredY, localZ);
            sampleNode(
                xIndex + 1,
                yIndex,
                zIndex + 1,
                localX + CARVER_HORIZONTAL_GRID,
                authoredY,
                localZ + CARVER_HORIZONTAL_GRID);
            if (progressY == 0.0D) return;

            int upperY = authoredY + CARVER_VERTICAL_GRID;
            sampleNode(xIndex, yIndex + 1, zIndex, localX, upperY, localZ);
            sampleNode(xIndex, yIndex + 1, zIndex + 1, localX, upperY, localZ + CARVER_HORIZONTAL_GRID);
            sampleNode(xIndex + 1, yIndex + 1, zIndex, localX + CARVER_HORIZONTAL_GRID, upperY, localZ);
            sampleNode(
                xIndex + 1,
                yIndex + 1,
                zIndex + 1,
                localX + CARVER_HORIZONTAL_GRID,
                upperY,
                localZ + CARVER_HORIZONTAL_GRID);
        }

        private double sampleCell(int localX, int localZ, int yIndex, int sampledY, double progressY) {
            int xIndex = localX / CARVER_HORIZONTAL_GRID;
            int zIndex = localZ / CARVER_HORIZONTAL_GRID;
            double lowerNorthWest = sampleNode(xIndex, yIndex, zIndex, localX, sampledY, localZ);
            if (localX % CARVER_HORIZONTAL_GRID == 0 && progressY == 0.0D && localZ % CARVER_HORIZONTAL_GRID == 0) {
                return lowerNorthWest;
            }

            double lowerSouthWest = sampleNode(
                xIndex,
                yIndex,
                zIndex + 1,
                localX,
                sampledY,
                localZ + CARVER_HORIZONTAL_GRID);
            double lowerNorthEast = sampleNode(
                xIndex + 1,
                yIndex,
                zIndex,
                localX + CARVER_HORIZONTAL_GRID,
                sampledY,
                localZ);
            double lowerSouthEast = sampleNode(
                xIndex + 1,
                yIndex,
                zIndex + 1,
                localX + CARVER_HORIZONTAL_GRID,
                sampledY,
                localZ + CARVER_HORIZONTAL_GRID);
            double progressX = localX % CARVER_HORIZONTAL_GRID / (double) CARVER_HORIZONTAL_GRID;
            double progressZ = localZ % CARVER_HORIZONTAL_GRID / (double) CARVER_HORIZONTAL_GRID;
            double lowerWest = lerp(lowerNorthWest, lowerSouthWest, progressZ);
            double lowerEast = lerp(lowerNorthEast, lowerSouthEast, progressZ);
            double lower = lerp(lowerWest, lowerEast, progressX);
            if (progressY == 0.0D) return lower;

            int upperY = sampledY + CARVER_VERTICAL_GRID;
            double upperNorthWest = sampleNode(xIndex, yIndex + 1, zIndex, localX, upperY, localZ);
            double upperSouthWest = sampleNode(
                xIndex,
                yIndex + 1,
                zIndex + 1,
                localX,
                upperY,
                localZ + CARVER_HORIZONTAL_GRID);
            double upperNorthEast = sampleNode(
                xIndex + 1,
                yIndex + 1,
                zIndex,
                localX + CARVER_HORIZONTAL_GRID,
                upperY,
                localZ);
            double upperSouthEast = sampleNode(
                xIndex + 1,
                yIndex + 1,
                zIndex + 1,
                localX + CARVER_HORIZONTAL_GRID,
                upperY,
                localZ + CARVER_HORIZONTAL_GRID);
            double upperWest = lerp(upperNorthWest, upperSouthWest, progressZ);
            double upperEast = lerp(upperNorthEast, upperSouthEast, progressZ);
            return lerp(lower, lerp(upperWest, upperEast, progressX), progressY);
        }

        private double sampleNode(int xIndex, int yIndex, int zIndex, int localX, int authoredY, int localZ) {
            int index = nodeIndex(xIndex, yIndex, zIndex);
            double cached = samples[index];
            if (!Double.isNaN(cached)) return cached;
            if (!priming) {
                localX = xIndex * CARVER_HORIZONTAL_GRID;
                localZ = zIndex * CARVER_HORIZONTAL_GRID;
                authoredY = UPSTREAM_MIN_Y + yIndex * CARVER_VERTICAL_GRID;
            }

            int sampledY = Math.min(UPSTREAM_MAX_Y_EXCLUSIVE - 1, authoredY);
            int blockX = chunkOriginX + localX;
            int blockZ = chunkOriginZ + localZ;
            OrigenBiomeProfile profile = sampleProfile(blockX, sampledY, blockZ);
            double sampled = runtime.sampleCarver(profile, worldSeed, blockX, sampledY, blockZ);
            samples[index] = sampled;
            return sampled;
        }
    }

    /** Chunk-local, allocation-free sampler over the exact authored Terra lattice. */
    static final class ScaledChunkDensity {

        private final double[] lattice;
        private final double[] elevation;

        private ScaledChunkDensity(double[] lattice, double[] elevation) {
            this.lattice = lattice;
            this.elevation = elevation;
        }

        double sample(int localX, int physicalY, int localZ) {
            int gridX = localX / TERRA_GRID;
            int gridZ = localZ / TERRA_GRID;
            double progressX = (localX % TERRA_GRID) / (double) TERRA_GRID;
            double progressZ = (localZ % TERRA_GRID) / (double) TERRA_GRID;

            double upstreamY = toUpstreamY(physicalY);
            int lowerUpstreamY = (int) Math.floor(upstreamY / TERRA_GRID) * TERRA_GRID;
            int gridY = (lowerUpstreamY - UPSTREAM_MIN_Y) / TERRA_GRID;
            double progressY = (upstreamY - lowerUpstreamY) / TERRA_GRID;

            double lowerNorth = lerp(
                lattice[latticeIndex(gridX, gridZ, gridY)],
                lattice[latticeIndex(gridX + 1, gridZ, gridY)],
                progressX);
            double lowerSouth = lerp(
                lattice[latticeIndex(gridX, gridZ + 1, gridY)],
                lattice[latticeIndex(gridX + 1, gridZ + 1, gridY)],
                progressX);
            double upperNorth = lerp(
                lattice[latticeIndex(gridX, gridZ, gridY + 1)],
                lattice[latticeIndex(gridX + 1, gridZ, gridY + 1)],
                progressX);
            double upperSouth = lerp(
                lattice[latticeIndex(gridX, gridZ + 1, gridY + 1)],
                lattice[latticeIndex(gridX + 1, gridZ + 1, gridY + 1)],
                progressX);
            return lerp(lerp(lowerNorth, lowerSouth, progressZ), lerp(upperNorth, upperSouth, progressZ), progressY)
                + elevation[localX * BiomicTerrainSampler.CHUNK_WIDTH + localZ];
        }

        /** Fills physical Y with identical arithmetic, reusing each horizontally interpolated lattice plane. */
        void fillPhysicalColumn(int localX, int localZ, double[] destination) {
            int gridX = localX / TERRA_GRID;
            int gridZ = localZ / TERRA_GRID;
            double progressX = localX % TERRA_GRID / (double) TERRA_GRID;
            double progressZ = localZ % TERRA_GRID / (double) TERRA_GRID;
            double columnElevation = elevation[localX * BiomicTerrainSampler.CHUNK_WIDTH + localZ];
            int previousGridY = -1;
            double lower = 0.0D;
            double upper = 0.0D;
            for (int physicalY = 0; physicalY < BiomicTerrainSampler.WORLD_HEIGHT; physicalY++) {
                double upstreamY = toUpstreamY(physicalY);
                int lowerUpstreamY = (int) Math.floor(upstreamY / TERRA_GRID) * TERRA_GRID;
                int gridY = (lowerUpstreamY - UPSTREAM_MIN_Y) / TERRA_GRID;
                double progressY = (upstreamY - lowerUpstreamY) / TERRA_GRID;
                if (gridY != previousGridY) {
                    lower = gridY == previousGridY + 1 && previousGridY >= 0 ? upper
                        : sampleHorizontalPlane(gridX, gridZ, gridY, progressX, progressZ);
                    upper = sampleHorizontalPlane(gridX, gridZ, gridY + 1, progressX, progressZ);
                    previousGridY = gridY;
                }
                destination[physicalY] = lerp(lower, upper, progressY) + columnElevation;
            }
        }

        double sampleUpstream(int localX, int authoredY, int localZ) {
            int gridX = localX / TERRA_GRID;
            int gridZ = localZ / TERRA_GRID;
            int lowerY = Math.floorDiv(authoredY, TERRA_GRID) * TERRA_GRID;
            int gridY = (lowerY - UPSTREAM_MIN_Y) / TERRA_GRID;
            double progressX = localX % TERRA_GRID / (double) TERRA_GRID;
            double progressZ = localZ % TERRA_GRID / (double) TERRA_GRID;
            if (authoredY == UPSTREAM_MAX_Y_EXCLUSIVE) {
                double north = lerp(
                    lattice[latticeIndex(gridX, gridZ, gridY)],
                    lattice[latticeIndex(gridX + 1, gridZ, gridY)],
                    progressX);
                double south = lerp(
                    lattice[latticeIndex(gridX, gridZ + 1, gridY)],
                    lattice[latticeIndex(gridX + 1, gridZ + 1, gridY)],
                    progressX);
                return lerp(north, south, progressZ) + elevation[localX * BiomicTerrainSampler.CHUNK_WIDTH + localZ];
            }
            double progressY = Math.floorMod(authoredY, TERRA_GRID) / (double) TERRA_GRID;

            double lowerNorth = lerp(
                lattice[latticeIndex(gridX, gridZ, gridY)],
                lattice[latticeIndex(gridX + 1, gridZ, gridY)],
                progressX);
            double lowerSouth = lerp(
                lattice[latticeIndex(gridX, gridZ + 1, gridY)],
                lattice[latticeIndex(gridX + 1, gridZ + 1, gridY)],
                progressX);
            double upperNorth = lerp(
                lattice[latticeIndex(gridX, gridZ, gridY + 1)],
                lattice[latticeIndex(gridX + 1, gridZ, gridY + 1)],
                progressX);
            double upperSouth = lerp(
                lattice[latticeIndex(gridX, gridZ + 1, gridY + 1)],
                lattice[latticeIndex(gridX + 1, gridZ + 1, gridY + 1)],
                progressX);
            return lerp(lerp(lowerNorth, lowerSouth, progressZ), lerp(upperNorth, upperSouth, progressZ), progressY)
                + elevation[localX * BiomicTerrainSampler.CHUNK_WIDTH + localZ];
        }

        /** Fills every integer authored Y using the exact operations performed by {@link #sampleUpstream}. */
        void fillUpstreamColumn(int localX, int localZ, double[] destination) {
            int gridX = localX / TERRA_GRID;
            int gridZ = localZ / TERRA_GRID;
            double progressX = localX % TERRA_GRID / (double) TERRA_GRID;
            double progressZ = localZ % TERRA_GRID / (double) TERRA_GRID;
            double columnElevation = elevation[localX * BiomicTerrainSampler.CHUNK_WIDTH + localZ];
            double lower = sampleHorizontalPlane(gridX, gridZ, 0, progressX, progressZ);
            for (int gridY = 0; gridY < UPSTREAM_GRID_HEIGHT - 1; gridY++) {
                double upper = sampleHorizontalPlane(gridX, gridZ, gridY + 1, progressX, progressZ);
                int offset = gridY * TERRA_GRID;
                for (int step = 0; step < TERRA_GRID; step++) {
                    destination[offset + step] = lerp(lower, upper, step / (double) TERRA_GRID) + columnElevation;
                }
                lower = upper;
            }
        }

        private double sampleHorizontalPlane(int gridX, int gridZ, int gridY, double progressX, double progressZ) {
            double north = lerp(
                lattice[latticeIndex(gridX, gridZ, gridY)],
                lattice[latticeIndex(gridX + 1, gridZ, gridY)],
                progressX);
            double south = lerp(
                lattice[latticeIndex(gridX, gridZ + 1, gridY)],
                lattice[latticeIndex(gridX + 1, gridZ + 1, gridY)],
                progressX);
            return lerp(north, south, progressZ);
        }

        /**
         * The field at a whole block inside the chunk, equal to {@link #sampleUpstreamInterpolated}; NaN above or
         * below the lattice.
         */
        double sampleBlock(int localX, int upstreamY, int localZ) {
            int offsetY = upstreamY - UPSTREAM_MIN_Y;
            int gridY = Math.floorDiv(offsetY, TERRA_GRID);
            if (gridY < 0 || gridY >= UPSTREAM_GRID_HEIGHT - 1) return Double.NaN;
            double progressX = (localX % TERRA_GRID) / (double) TERRA_GRID;
            double progressY = Math.floorMod(offsetY, TERRA_GRID) / (double) TERRA_GRID;
            double progressZ = (localZ % TERRA_GRID) / (double) TERRA_GRID;
            int gridX = localX / TERRA_GRID, gridZ = localZ / TERRA_GRID;
            return lerp(
                sampleHorizontalPlane(gridX, gridZ, gridY, progressX, progressZ),
                sampleHorizontalPlane(gridX, gridZ, gridY + 1, progressX, progressZ),
                progressY) + elevationAt(localX, localZ);
        }

        double elevationAt(int localX, int localZ) {
            return elevation[localX * BiomicTerrainSampler.CHUNK_WIDTH + localZ];
        }
    }

    private static int slot(long key, int mask) {
        return (int) WorldgenContext.mix64(key) & mask;
    }

    private static final class LongDoubleCache {

        private final long[] keys;
        private final double[] values;
        private final boolean[] occupied;
        private final Object[] locks;
        private final int mask;

        private LongDoubleCache(int size) {
            if (Integer.bitCount(size) != 1) {
                throw new IllegalArgumentException("Exact terrain cache size must be a power of two");
            }
            keys = new long[size];
            values = new double[size];
            occupied = new boolean[size];
            locks = cacheLocks(size);
            mask = size - 1;
        }

        private double get(long key) {
            int index = slot(key, mask);
            synchronized (locks[index & locks.length - 1]) {
                return occupied[index] && keys[index] == key ? values[index] : Double.NaN;
            }
        }

        private double putIfAbsent(long key, double value) {
            int index = slot(key, mask);
            synchronized (locks[index & locks.length - 1]) {
                if (occupied[index] && keys[index] == key) {
                    return values[index];
                }
                keys[index] = key;
                values[index] = value;
                occupied[index] = true;
                return value;
            }
        }
    }

    /**
     * Direct-mapped and lock-free: the key's hash is a bijection, so its slot and its remaining high bits identify
     * the key exactly, and one long per entry holds them with the profile, read and written atomically. Profile
     * lookups are the hottest path of lattice sampling; a lock per lookup cost more than the lookup.
     */
    private static final class LongProfileCache {

        private static final int PROFILE_BITS = 7;

        private final AtomicLongArray entries;
        private final int mask, indexBits;

        private LongProfileCache(int size) {
            if (Integer.bitCount(size) != 1 || size < 1 << PROFILE_BITS) {
                throw new IllegalArgumentException("Exact profile cache size must be a power of two of at least 128");
            }
            if (PROFILE_VALUES.length >= 1 << PROFILE_BITS) throw new IllegalStateException("Too many Origen profiles");
            entries = new AtomicLongArray(size);
            mask = size - 1;
            indexBits = Integer.numberOfTrailingZeros(size);
        }

        private OrigenBiomeProfile get(long key) {
            long hash = WorldgenContext.mix64(key);
            long entry = entries.get((int) hash & mask);
            return entry != 0 && entry >>> PROFILE_BITS == hash >>> indexBits
                ? PROFILE_VALUES[(int) (entry & (1 << PROFILE_BITS) - 1) - 1]
                : null;
        }

        private OrigenBiomeProfile putIfAbsent(long key, OrigenBiomeProfile profile) {
            long hash = WorldgenContext.mix64(key);
            int index = (int) hash & mask;
            long tag = hash >>> indexBits, entry = entries.get(index);
            if (entry != 0 && entry >>> PROFILE_BITS == tag)
                return PROFILE_VALUES[(int) (entry & (1 << PROFILE_BITS) - 1) - 1];
            entries.set(index, tag << PROFILE_BITS | profile.ordinal() + 1);
            return profile;
        }
    }

    private static Object[] cacheLocks(int cacheSize) {
        Object[] locks = new Object[Math.min(CACHE_LOCK_STRIPES, cacheSize)];
        for (int index = 0; index < locks.length; index++) locks[index] = new Object();
        return locks;
    }
}
