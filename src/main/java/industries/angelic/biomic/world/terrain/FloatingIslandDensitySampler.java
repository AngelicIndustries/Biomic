package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;
import industries.angelic.biomic.biome.origen.OrigenTerrainFamily;
import industries.angelic.biomic.world.WorldgenContext;
import industries.angelic.biomic.world.WorldgenPhase;

/**
 * Sparse floating-landmass density field composed from Biomic's accepted sky donut and the attributed Aeropelago
 * port.
 *
 * <p>
 * Floating terrain is a separate 3D world layer, not a heightmap pretending to float. Each cluster chooses a
 * reproducible shape grammar and hosts a 3D identity independently from the surface directly below it. The sky donut
 * and five dormant prototypes are original Biomic work. Aeropelago is an attributed GPL-3.0 port kept in a separate
 * sampler so its exact source graph and Biomic's coexistence envelope remain auditable.
 * </p>
 */
public final class FloatingIslandDensitySampler {

    public static final int MIN_Y = 84;
    public static final int MAX_Y = 246;

    private static final int REGION_SIZE = 704;

    private static final int GROUND_CLEARANCE = 16;
    private static final int SURFACE_SCAN_STEP = 24;
    private static final int SURFACE_SCAN_MARGIN = 8;
    static final int OVERLAND_CANOPY_CLEARANCE = 32;
    static final int OVERLAND_SCAN_RADIUS = 208;
    static final int OVERLAND_SCAN_STEP = 4;
    static final int REMOTE_OCEAN_MARGIN = 112;
    static final int REMOTE_OCEAN_SCAN_STEP = 16;
    private static final int AEROPELAGO_OCEAN_SCAN_STEP = 32;
    private static final int AEROPELAGO_INTRUSION_SCAN_STEP = 16;
    private static final int AEROPELAGO_INTRUSION_SAMPLE_OFFSET = AEROPELAGO_INTRUSION_SCAN_STEP / 2;
    private static final double AEROPELAGO_MINIMUM_BUFFER_COVERAGE = 0.9D;
    private static final int PLACEMENT_CACHE_SIZE = 512;
    private static final int COLUMN_CACHE_SIZE = 128;
    private static final long CLUSTER_SALT = 0xA54FF53A5F1D36F1L;
    private static final long WARP_X_SALT = 0x510E527FADE682D1L;
    private static final long WARP_Z_SALT = 0x9B05688C2B3E6C1FL;
    private static final long TOP_SALT = 0x1F83D9ABFB41BD6BL;
    private static final long RING_PLAN_SALT = 0x243F6A8885A308D3L;
    private static final long AEROPELAGO_BOUNDARY_SALT = 0xD1310BA698DFB5ACL;
    private static final int AEROPELAGO_CORE_RADIUS = 224;
    private static final int AEROPELAGO_FEATHER_WIDTH = 96;
    private static final int AEROPELAGO_BOUNDARY_JITTER = 24;

    private final long seed;
    private final int seaLevel;
    /** Frame: island band and the Y offset applied to every authored island height (0 at 256, 128 shifted). */
    private final int minY, maxY, yOffset;
    /** Shifted frames fit overland rings to the stepped scan plus a margin instead of every column. */
    private final boolean exactOverlandScan;
    /**
     * The 512 sky layer: groups first. Aeropelago archipelagos (3 in 4 clusters) or a single big sky donut fit over
     * any terrain with clearance, in one of three regions, instead of the blend's rare remote-ocean admission.
     */
    private final boolean skyLayer;
    private final Ground ground;
    private final OrigenVerticalProfileSampler groundProfiles;
    private final boolean overlandRings;
    private final AeropelagoDensitySampler aeropelagoDensity;
    private final AeropelagoBiomeSampler aeropelagoBiomes;
    private final ThreadLocal<SeedCache> seedCache = ThreadLocal.withInitial(SeedCache::new);
    private final ThreadLocal<ColumnCache> columnCache = new ThreadLocal<ColumnCache>() {

        @Override
        protected ColumnCache initialValue() {
            return new ColumnCache();
        }
    };
    private final Map<Long, ClusterPlacement> placementCache = new LinkedHashMap<Long, ClusterPlacement>(
        64,
        0.75F,
        true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, ClusterPlacement> eldest) {
            return size() > PLACEMENT_CACHE_SIZE;
        }
    };

    public FloatingIslandDensitySampler(WorldgenContext context) {
        this(
            context,
            legacyGround(context),
            0,
            MIN_Y,
            MAX_Y,
            context.getSeaLevel(),
            context.getConfig()
                .isFloatingIslandsEnabled()
                && context.getSettings()
                    .getFloatingPlacementRevision() == 1,
            true,
            false);
    }

    /** Surface heights the placement scans read: coarse for ring/ocean admission, exact for the overland fit. */
    public interface Ground {

        int surfaceHeight(int blockX, int blockZ);

        int exactSurfaceHeight(int blockX, int blockZ);
    }

    /**
     * The same islands over uncompressed (shifted512) Origen: every authored island height moves up by 128 with
     * the sea (63 -> 191), so islands keep their block-scale shapes and their height above the sea. Clusters over
     * land rise to clear it, up to the authored envelope's top.
     */
    public static FloatingIslandDensitySampler shifted512(WorldgenContext context, ShiftedGroundHeights heights) {
        // Every Origen column height costs a density scan, so admission samples sparsely (see groundStep/groundMargin);
        // the per-chunk rasterizer re-checks exact clearance at 4-block resolution.
        Ground ground = new Ground() {

            @Override
            public int surfaceHeight(int x, int z) {
                return heights.ground(x, z);
            }

            @Override
            public int exactSurfaceHeight(int x, int z) {
                return heights.ground(x, z);
            }
        };
        // The band ends at the authored envelope's top (447): island surfaces stay inside the frame that palettes,
        // native height schedules and climate rules are defined over.
        return new FloatingIslandDensitySampler(context, ground, 128, MIN_Y + 128, 447, 191, true, false, true);
    }

    /** Shifted frames scan ground at most every 32 blocks; the margin bounds the relief between samples. */
    private int groundStep(int step) {
        return exactOverlandScan ? step : Math.max(step, SPARSE_GROUND_STEP);
    }

    private int groundMargin() {
        return exactOverlandScan ? 0 : SPARSE_GROUND_MARGIN;
    }

    private static final int SPARSE_GROUND_STEP = 32, SPARSE_GROUND_MARGIN = 24;
    /** Sky-layer fits: every ground column is an Origen density scan, so ~60 per archipelago. */
    private static final int SKY_GROUND_STEP = 64, SKY_CLEARANCE = 40;
    /** Sky-layer archipelagos use a smaller disk than the blend's remote-ocean ones so groups fit over land. */
    private static final int SKY_AEROPELAGO_CORE = 192, SKY_AEROPELAGO_FEATHER = 80;

    private int aeropelagoCore() {
        return skyLayer ? SKY_AEROPELAGO_CORE : AEROPELAGO_CORE_RADIUS;
    }

    /**
     * Sky archipelagos sample the authored field at about a third of the blend's scale and slightly thinned, so one
     * disk frames three to seven separate islands of the pack's archipelago rather than a single merged landmass.
     */
    private static final double SKY_AEROPELAGO_SCALE = 0.36D;

    /**
     * Where an archipelago's plan crosses the coast, and the plan's typical change per block there (re-measure both if
     * the scale or the threshold changes): the sky layer's 0.36 scale, and the blend's projection.
     */
    private static final double SKY_PLAN_THRESHOLD = 0.635D, SKY_PLAN_GRADIENT = 0.00306D;
    private static final double BLEND_PLAN_THRESHOLD = 0.585D,
        BLEND_PLAN_GRADIENT = SKY_PLAN_GRADIENT * SKY_AEROPELAGO_SCALE / AeropelagoDensitySampler.PHYSICAL_SCALE;
    /** How much the archipelago disk's feather lowers the plan at its edge. */
    private static final double PLAN_FEATHER = 0.35D;
    /** Keels: the deepest in the sky layer and in the blend's band, and the least any cluster gets. */
    private static final int SKY_MAX_KEEL = 64, BLEND_MAX_KEEL = 40, MIN_KEEL = 24;
    /** How far a deck can sit below its cluster's centre. */
    private static final int DECK_DIP = 4;
    /** How far outside a coast a column still reports its island's profile (biomes around the rim). */
    private static final double COAST_REACH = 8.0D;
    private static final long DECK_SALT = 0xBB67AE8584CAA73BL, SPIRE_SALT = 0x6A09E667F3BCC909L,
        ISLET_SALT = 0x3C6EF372FE94F82AL;
    /** Islets: a jittered grid, the share of its cells that hold one. */
    private static final double ISLET_CELL = 48.0D, ISLET_CHANCE = 0.4D;

    private int aeropelagoFeather() {
        return skyLayer ? SKY_AEROPELAGO_FEATHER : AEROPELAGO_FEATHER_WIDTH;
    }

    /** The deepest keel a cluster can hang: down to its clearance over the ground below. */
    private int maxKeel(double centerY, int ground, int clearance) {
        return (int) Math
            .max(MIN_KEEL, Math.min(skyLayer ? SKY_MAX_KEEL : BLEND_MAX_KEEL, centerY - DECK_DIP - ground - clearance));
    }

    /** Size of the regions that each hold at most one island cluster. */
    public static final int CLUSTER_REGION = REGION_SIZE;

    /** One admitted island cluster: its centre, how far from it its islands can reach, and their shape. */
    public record Cluster(int centerX, int centerZ, int radius, FloatingIslandArchetype archetype) {}

    /**
     * The admitted cluster of one region, or null. The same selection, archetype and placement as
     * {@link #sampleColumn}, so a cluster reported here is the one whose islands the field holds.
     */
    public Cluster cluster(int regionX, int regionZ) {
        long clusterHash = regionHash(regionX, regionZ);
        if (Math.floorMod(clusterHash, skyLayer ? 3L : 4L) != 0L) return null;
        double centerX = (regionX + 0.5D) * REGION_SIZE + signedUnit(clusterHash) * 176.0D;
        double centerZ = (regionZ + 0.5D) * REGION_SIZE + signedUnit(mix(clusterHash, 1L)) * 176.0D;
        FloatingIslandArchetype archetype = archetype(clusterHash);
        if (archetype == null) return null;
        if (!resolvePlacement(clusterHash, archetype, centerX, centerZ, initialCenterY(archetype, clusterHash)).allowed)
            return null;
        // Donuts are sampled through a domain warp of up to 28 blocks.
        int radius = (skyLayer ? skyFootprintRadius(archetype) : footprintRadius(archetype)) + 32;
        return new Cluster((int) Math.round(centerX), (int) Math.round(centerZ), radius, archetype);
    }

    /**
     * Roughly where the island field tops out in a column, read straight from the field every four blocks and refined:
     * no lattice, minimum thickness or ground clearance (the rasterizers' rules). -1 where the column has none. For
     * searching a cluster cheaply; exact island columns come from the rasterized layer.
     */
    public int estimateTop(int blockX, int blockZ) {
        long span = columnCache.get()
            .resolve(blockX, blockZ).span;
        return span == IslandKeel.NONE ? -1 : IslandKeel.top(span);
    }

    /**
     * The island in a column, {@code bottom << 16 | top}, or -1: one span from its deck to its keel (IslandKeel),
     * before the rasterizers' ground clearance.
     */
    public int span(int blockX, int blockZ) {
        long span = columnCache.get()
            .resolve(blockX, blockZ).span;
        if (span == IslandKeel.NONE) return -1;
        int top = Math.min(maxY, IslandKeel.top(span)), bottom = Math.max(minY, IslandKeel.bottom(span));
        return top < bottom ? -1 : bottom << 16 | top;
    }

    private int skyFootprintRadius(FloatingIslandArchetype archetype) {
        return archetype == FloatingIslandArchetype.AEROPELAGO
            ? SKY_AEROPELAGO_CORE + SKY_AEROPELAGO_FEATHER + AEROPELAGO_BOUNDARY_JITTER
            : footprintRadius(archetype);
    }

    private int groundPercentile(double centerX, double centerZ, int radius, int step, double quantile) {
        java.util.List<Integer> heights = new java.util.ArrayList<>();
        int startX = floorToStep((int) Math.floor(centerX) - radius, step), endX = (int) Math.ceil(centerX) + radius;
        int startZ = floorToStep((int) Math.floor(centerZ) - radius, step), endZ = (int) Math.ceil(centerZ) + radius;
        for (int z = startZ; z <= endZ; z += step) for (int x = startX; x <= endX; x += step)
            if (square(x - centerX) + square(z - centerZ) <= square(radius)) heights.add(ground.surfaceHeight(x, z));
        if (heights.isEmpty()) return ground.surfaceHeight((int) Math.round(centerX), (int) Math.round(centerZ));
        java.util.Collections.sort(heights);
        return heights.get(Math.min(heights.size() - 1, (int) Math.floor(quantile * heights.size())));
    }

    private FloatingIslandDensitySampler(WorldgenContext context, Ground ground, int yOffset, int minY, int maxY,
        int seaLevel, boolean overlandRings, boolean exactOverlandScan, boolean skyLayer) {
        if (context == null) {
            throw new IllegalArgumentException("Worldgen context cannot be null");
        }
        seed = context.seedForPhase(WorldgenPhase.TERRAIN) ^ CLUSTER_SALT;
        this.seaLevel = seaLevel;
        this.ground = ground;
        this.yOffset = yOffset;
        this.minY = minY;
        this.maxY = maxY;
        this.exactOverlandScan = exactOverlandScan;
        this.overlandRings = overlandRings;
        this.skyLayer = skyLayer;
        aeropelagoDensity = new AeropelagoDensitySampler(
            context.getSettings()
                .getWorldSeed());
        aeropelagoBiomes = new AeropelagoBiomeSampler(
            context.getSettings()
                .getWorldSeed());
        groundProfiles = new OrigenVerticalProfileSampler(context);
    }

    private static Ground legacyGround(WorldgenContext context) {
        MacroTerrainSampler macroTerrain = new MacroTerrainSampler(context);
        OrigenTerrainGraph groundGraph = new OrigenTerrainGraph(context);
        TerrainColumnSampler columns = new TerrainColumnSampler(macroTerrain, groundGraph);
        OrigenDensityField density = OrigenDensityField.shared(context);
        return new Ground() {

            @Override
            public int surfaceHeight(int x, int z) {
                return columns.sample(x, z)
                    .getSurfaceHeight();
            }

            @Override
            public int exactSurfaceHeight(int x, int z) {
                return density.sampleScaledSurfaceHeight(x, z);
            }
        };
    }

    public int minY() {
        return minY;
    }

    public int maxY() {
        return maxY;
    }

    /** Positive values are floating terrain; negative values are open sky. */
    public double sampleDensity(int blockX, int blockY, int blockZ) {
        return sample(blockX, blockY, blockZ).density;
    }

    /** Returns the hosted Origen profile in and immediately around floating terrain, otherwise {@code null}. */
    public OrigenBiomeProfile sampleProfile(int blockX, int blockY, int blockZ) {
        IslandSample sample = sample(blockX, blockY, blockZ);
        return sample.density >= -4.0D ? sample.profile : null;
    }

    /** Returns the local shape grammar in and immediately around floating terrain, otherwise {@code null}. */
    public FloatingIslandArchetype sampleArchetype(int blockX, int blockY, int blockZ) {
        IslandSample sample = sample(blockX, blockY, blockZ);
        return sample.density >= -4.0D ? sample.archetype : null;
    }

    /** The island (or group) a column's habitat belongs to: its key, and the square its islands lie in. */
    public record HabitatSite(FloatingIslandArchetype archetype, long key, int anchorX, int anchorZ, int reach) {}

    /**
     * The habitat site of a column's island, or null off the islands. An archipelago island (its connected land, see
     * {@link #island(ClusterColumn, int, int)}) has its own in {@code variety} percent of islands, else it shares its
     * group's, the cluster's; an islet, or land the lattice misses, has the jittered habitat cell's
     * ({@link #islandCell}). A donut is one island.
     */
    public HabitatSite habitatSite(int blockX, int blockZ, int variety) {
        ClusterColumn cluster = columnCache.get()
            .resolve(blockX, blockZ).cluster;
        if (cluster == null) return null;
        if (cluster.archetype == FloatingIslandArchetype.AEROPELAGO && variety > 0) {
            Island island = island(cluster, blockX, blockZ);
            if (island != null) {
                if (variety >= 100 || Math.floorMod(WorldgenContext.mix64(island.key ^ VARIETY_SALT), 100L) < variety)
                    return new HabitatSite(cluster.archetype, island.key, island.anchorX, island.anchorZ, island.reach);
                int reach = (skyLayer ? skyFootprintRadius(cluster.archetype) : footprintRadius(cluster.archetype))
                    + 32;
                return new HabitatSite(
                    cluster.archetype,
                    cluster.hash,
                    (int) Math.floor(cluster.centerX),
                    (int) Math.floor(cluster.centerZ),
                    reach);
            }
            double[] point = new double[2];
            long cell = islandCell(cluster.hash, blockX, blockZ, point);
            if (variety >= 100 || Math.floorMod(WorldgenContext.mix64(cell ^ VARIETY_SALT), 100L) < variety)
                return new HabitatSite(
                    cluster.archetype,
                    cell,
                    (int) Math.floor(point[0]),
                    (int) Math.floor(point[1]),
                    HABITAT_CELL);
        }
        int reach = (skyLayer ? skyFootprintRadius(cluster.archetype) : footprintRadius(cluster.archetype)) + 32;
        return new HabitatSite(
            cluster.archetype,
            cluster.hash,
            (int) Math.floor(cluster.centerX),
            (int) Math.floor(cluster.centerZ),
            reach);
    }

    /**
     * Island columns of a habitat site, on a lattice of at most 13 by 13 over its reach: where the ground below its
     * islands is read (SkyIslandHabitats).
     */
    public List<int[]> habitatPoints(HabitatSite site, int variety) {
        int step = Math.max(8, site.reach() / 6);
        List<int[]> points = new ArrayList<>();
        for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
            int x = site.anchorX() + dx * step, z = site.anchorZ() + dz * step;
            if (columnCache.get()
                .resolve(x, z).span == IslandKeel.NONE) continue;
            HabitatSite at = habitatSite(x, z, variety);
            if (at != null && at.key() == site.key()) points.add(new int[] { x, z });
        }
        return points;
    }

    private static final long VARIETY_SALT = 0x5EB1D2C3A4F59687L;

    /**
     * One archipelago island: its key (from its lowest lattice node, so the same whichever column asks first), the
     * centre of its nodes and how far they reach from it.
     */
    private record Island(long key, int anchorX, int anchorZ, int reach) {}

    /** The island lattice's spacing; a flood fill stops past this many nodes (and the land keeps its cells). */
    private static final int ISLAND_NODE = 4, ISLAND_NODE_LIMIT = 20_000;
    private static final Island NO_ISLAND = new Island(0L, 0, 0, 0);
    private final Map<Long, Island> islandNodes = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * The archipelago island holding a column, or null: the connected land (eight-connected nodes inside the coast) of
     * its cluster's plan on a {@link #ISLAND_NODE}-block lattice, from the land node nearest the column. Habitats are
     * chosen per island, so no island shows two (a jittered cell used to split those lying across its border).
     */
    private Island island(ClusterColumn cluster, int blockX, int blockZ) {
        int nodeX = Math.floorDiv(blockX, ISLAND_NODE), nodeZ = Math.floorDiv(blockZ, ISLAND_NODE);
        int startX = 0, startZ = 0;
        double nearest = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            int x = (nodeX + dx) * ISLAND_NODE + ISLAND_NODE / 2, z = (nodeZ + dz) * ISLAND_NODE + ISLAND_NODE / 2;
            double distance = square(x - blockX) + square(z - blockZ);
            if (distance < nearest && land(cluster, nodeX + dx, nodeZ + dz)) {
                nearest = distance;
                startX = nodeX + dx;
                startZ = nodeZ + dz;
            }
        }
        if (nearest == Double.MAX_VALUE) return null;
        Island known = islandNodes.get(nodeKey(cluster.hash, startX, startZ));
        if (known != null) return known == NO_ISLAND ? null : known;
        java.util.ArrayDeque<long[]> open = new java.util.ArrayDeque<>();
        java.util.Map<Long, Boolean> seen = new java.util.HashMap<>();
        List<long[]> nodes = new ArrayList<>();
        open.add(new long[] { startX, startZ });
        seen.put(nodeKey(cluster.hash, startX, startZ), Boolean.TRUE);
        long lowX = startX, lowZ = startZ, sumX = 0, sumZ = 0;
        while (!open.isEmpty() && nodes.size() <= ISLAND_NODE_LIMIT) {
            long[] node = open.poll();
            nodes.add(node);
            sumX += node[0];
            sumZ += node[1];
            if (node[0] < lowX || node[0] == lowX && node[1] < lowZ) {
                lowX = node[0];
                lowZ = node[1];
            }
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                int x = (int) node[0] + dx, z = (int) node[1] + dz;
                Long key = nodeKey(cluster.hash, x, z);
                if (seen.containsKey(key)) continue;
                boolean land = land(cluster, x, z);
                seen.put(key, land);
                if (land) open.add(new long[] { x, z });
            }
        }
        Island island = NO_ISLAND;
        if (nodes.size() <= ISLAND_NODE_LIMIT) {
            int anchorX = (int) Math.round((double) sumX / nodes.size() * ISLAND_NODE + ISLAND_NODE / 2.0D);
            int anchorZ = (int) Math.round((double) sumZ / nodes.size() * ISLAND_NODE + ISLAND_NODE / 2.0D);
            int reach = 0;
            for (long[] node : nodes) reach = Math.max(
                reach,
                (int) Math.ceil(
                    Math.max(
                        Math.abs(node[0] * ISLAND_NODE + ISLAND_NODE / 2.0D - anchorX),
                        Math.abs(node[1] * ISLAND_NODE + ISLAND_NODE / 2.0D - anchorZ))));
            island = new Island(
                WorldgenContext.mix64(cluster.hash ^ lowX * 0x9E3779B97F4A7C15L ^ lowZ * 0xC2B2AE3D27D4EB4FL),
                anchorX,
                anchorZ,
                reach + ISLAND_NODE * 2);
        }
        if (islandNodes.size() > 1 << 20) islandNodes.clear();
        for (long[] node : nodes) islandNodes.put(nodeKey(cluster.hash, (int) node[0], (int) node[1]), island);
        return island == NO_ISLAND ? null : island;
    }

    /** Whether a lattice node lies inside its cluster's coast (the archipelago's own outline, not warped). */
    private boolean land(ClusterColumn cluster, int nodeX, int nodeZ) {
        int x = nodeX * ISLAND_NODE + ISLAND_NODE / 2, z = nodeZ * ISLAND_NODE + ISLAND_NODE / 2;
        double deltaX = x - cluster.centerX, deltaZ = z - cluster.centerZ;
        return inside(
            cluster,
            deltaX * cluster.cos + deltaZ * cluster.sin,
            -deltaX * cluster.sin + deltaZ * cluster.cos,
            x,
            z) > 0.0D;
    }

    private static long nodeKey(long cluster, int nodeX, int nodeZ) {
        return WorldgenContext.mix64(cluster ^ ((long) nodeX << 32 ^ nodeZ & 0xFFFF_FFFFL));
    }

    /**
     * Inside an island's span the density is the distance to its top or bottom (at least 1); around it, above or below
     * its span or off its coast, it is negative: the blocks to the island.
     */
    private IslandSample sample(int blockX, int blockY, int blockZ) {
        if (blockY < minY || blockY > maxY) return IslandSample.NONE;
        ColumnContext column = columnCache.get()
            .resolve(blockX, blockZ);
        if (column.cluster == null) return IslandSample.NONE;
        double density;
        if (column.span != IslandKeel.NONE) {
            int top = IslandKeel.top(column.span), bottom = IslandKeel.bottom(column.span);
            density = Math.min(top - blockY, blockY - bottom);
            if (density >= 0.0D) density += 1.0D;
        } else {
            density = Math.min(column.inside, Math.min(column.deck + 2.0D - blockY, blockY - column.deck + 8.0D));
        }
        OrigenBiomeProfile profile = column.cluster.archetype == FloatingIslandArchetype.RING_BASIN
            ? OrigenBiomeProfile.LUSH_LOOPS
            : aeropelagoBiomes.sample(column.cluster.hash, blockX, blockZ);
        return new IslandSample(density, profile, column.cluster.archetype);
    }

    /** The island in one X/Z column, worked out once for every height asked about there. */
    private ColumnContext sampleColumn(int blockX, int blockZ) {
        double warpX = warp(WARP_X_SALT, blockX, blockZ), warpZ = warp(WARP_Z_SALT, blockX, blockZ);
        double sampleX = blockX + warpX;
        double sampleZ = blockZ + warpZ;
        int regionX = floor(sampleX / REGION_SIZE);
        int regionZ = floor(sampleZ / REGION_SIZE);
        double topNoise = ValueNoise2D.fractal(seed ^ TOP_SALT, blockX, blockZ, 104.0D, 3, 0.52D);

        ClusterColumn[] clusters = new ClusterColumn[9];
        int clusterCount = 0;
        for (int regionOffsetZ = -1; regionOffsetZ <= 1; regionOffsetZ++) {
            for (int regionOffsetX = -1; regionOffsetX <= 1; regionOffsetX++) {
                int candidateRegionX = regionX + regionOffsetX;
                int candidateRegionZ = regionZ + regionOffsetZ;
                long clusterHash = regionHash(candidateRegionX, candidateRegionZ);
                if (Math.floorMod(clusterHash, skyLayer ? 3L : 4L) != 0L) continue;
                FloatingIslandArchetype archetype = archetype(clusterHash);
                if (archetype == null) continue;
                double centerX = (candidateRegionX + 0.5D) * REGION_SIZE + signedUnit(clusterHash) * 176.0D;
                double centerZ = (candidateRegionZ + 0.5D) * REGION_SIZE + signedUnit(mix(clusterHash, 1L)) * 176.0D;
                ClusterPlacement placement = resolvePlacement(
                    clusterHash,
                    archetype,
                    centerX,
                    centerZ,
                    initialCenterY(archetype, clusterHash));
                if (!placement.allowed) continue;
                double angle = unit(mix(clusterHash, 3L)) * Math.PI * 2.0D;
                // Donuts are warped; archipelagos keep the pack's own outlines.
                boolean warped = archetype == FloatingIslandArchetype.RING_BASIN;
                double deltaX = (warped ? sampleX : blockX) - centerX;
                double deltaZ = (warped ? sampleZ : blockZ) - centerZ;
                double cos = Math.cos(angle), sin = Math.sin(angle);
                clusters[clusterCount++] = new ClusterColumn(
                    archetype,
                    clusterHash,
                    deltaX * cos + deltaZ * sin,
                    -deltaX * sin + deltaZ * cos,
                    placement.centerY,
                    placement.maxKeel,
                    centerX,
                    centerZ,
                    cos,
                    sin);
            }
        }
        return island(blockX, blockZ, topNoise, clusters, clusterCount);
    }

    private ClusterPlacement resolvePlacement(long clusterHash, FloatingIslandArchetype archetype, double centerX,
        double centerZ, double initialCenterY) {
        synchronized (placementCache) {
            ClusterPlacement cached = placementCache.get(clusterHash);
            if (cached != null) {
                return cached;
            }
        }
        // Remote-ocean and overland admission are far more expensive than the local density field. One computation
        // per cluster is in flight; threads needing the same cluster wait for it, but nothing holds the cache lock
        // during a scan (the server thread must never queue behind a worker's unrelated cluster).
        java.util.concurrent.CompletableFuture<ClusterPlacement> mine = new java.util.concurrent.CompletableFuture<>();
        java.util.concurrent.CompletableFuture<ClusterPlacement> running = inFlightPlacements
            .putIfAbsent(clusterHash, mine);
        if (running != null) return running.join();
        try {
            ClusterPlacement placement;
            synchronized (placementCache) {
                placement = placementCache.get(clusterHash);
            }
            if (placement == null) {
                placement = calculatePlacement(clusterHash, archetype, centerX, centerZ, initialCenterY);
                synchronized (placementCache) {
                    placementCache.put(clusterHash, placement);
                }
            }
            mine.complete(placement);
            return placement;
        } catch (RuntimeException | Error failure) {
            mine.completeExceptionally(failure);
            throw failure;
        } finally {
            inFlightPlacements.remove(clusterHash, mine);
        }
    }

    private final java.util.concurrent.ConcurrentHashMap<Long, java.util.concurrent.CompletableFuture<ClusterPlacement>> inFlightPlacements = new java.util.concurrent.ConcurrentHashMap<>();

    private ClusterPlacement calculatePlacement(long clusterHash, FloatingIslandArchetype archetype, double centerX,
        double centerZ, double initialCenterY) {
        if (skyLayer) {
            // Over any terrain: lift the whole cluster until its lowest point clears the high ground under its
            // footprint (85th percentile: isolated peaks are left to the rasterizer's exact local clearance, which
            // trims an island column rather than let it touch ground); reject it when that leaves the sky band.
            // High enough for the full keel where the sky band allows, else for a shorter one.
            int ground = groundPercentile(centerX, centerZ, skyFootprintRadius(archetype), SKY_GROUND_STEP, 0.85D);
            double highest = maxY - maximumTopOffset(archetype);
            double centerY = Math
                .max(initialCenterY, Math.min(highest, ground + SKY_CLEARANCE + DECK_DIP + SKY_MAX_KEEL));
            return centerY >= ground + SKY_CLEARANCE - minimumBottomOffset() && centerY <= highest
                ? new ClusterPlacement(true, centerY, maxKeel(centerY, ground, SKY_CLEARANCE))
                : ClusterPlacement.REJECTED;
        }
        if (isOverlandRingCandidate(archetype, centerX, centerZ)) {
            // A new save-pinned branch: preserve the remote-ocean layouts exactly, never clip the ring to a mountain.
            // Cheap rejection first, then every integer column including the canopy/edge halo. A lattice-only scan
            // is insufficient: Origen adds a per-column elevation term after interpolating its density nodes.
            if (ground.surfaceHeight((int) Math.round(centerX), (int) Math.round(centerZ)) <= seaLevel)
                return ClusterPlacement.REJECTED;
            GroundScan coarse = scanGround(centerX, centerZ, OVERLAND_SCAN_RADIUS, 32);
            if (!Double.isFinite(overlandCenterY(coarse.maximumSurface + groundMargin(), initialCenterY, maxY)))
                return ClusterPlacement.REJECTED;
            GroundScan full = exactOverlandScan ? scanGround(centerX, centerZ, OVERLAND_SCAN_RADIUS, OVERLAND_SCAN_STEP)
                : coarse;
            if (!Double.isFinite(overlandCenterY(full.maximumSurface + groundMargin(), initialCenterY, maxY)))
                return ClusterPlacement.REJECTED;
            // Shifted frames: per-column density heights already include Origen's relief; the stepped scan plus a
            // margin bounds the gaps between samples without scanning ~140k columns on a worker.
            GroundScan exact = exactOverlandScan ? scanOverlandGround(centerX, centerZ, initialCenterY) : full;
            double center = overlandCenterY(exact.maximumSurface + groundMargin(), initialCenterY, maxY);
            return Double.isFinite(center) ? new ClusterPlacement(
                true,
                center,
                maxKeel(center, exact.maximumSurface + groundMargin(), OVERLAND_CANOPY_CLEARANCE + SURFACE_SCAN_MARGIN))
                : ClusterPlacement.REJECTED;
        }
        int radius = footprintRadius(archetype);
        if (isProductionRemoteOceanArchetype(archetype)) {
            int admissionRadius = radius + REMOTE_OCEAN_MARGIN;
            if (archetype == FloatingIslandArchetype.RING_BASIN) {
                if (!isRemoteOpenOcean(centerX, centerZ, admissionRadius)
                    || scanGround(centerX, centerZ, admissionRadius, REMOTE_OCEAN_SCAN_STEP).maximumSurface
                        >= seaLevel) {
                    return ClusterPlacement.REJECTED;
                }
            } else if (!isBroadRemoteOcean(clusterHash, centerX, centerZ, admissionRadius)) {
                return ClusterPlacement.REJECTED;
            }
        }
        // Aeropelago's broad profile gate is deliberately cheap enough to execute on a terrain worker. Its fixed sky
        // projection already clears sea level; the fresh-world integration fixture proves the complete admitted disk
        // against generated blocks instead of recomputing hundreds of full Origen chunks on the hot path.
        GroundScan ground = archetype == FloatingIslandArchetype.AEROPELAGO ? new GroundScan(seaLevel - 1)
            : scanGround(centerX, centerZ, radius, SURFACE_SCAN_STEP);
        if (isProductionRemoteOceanArchetype(archetype) && ground.maximumSurface >= seaLevel)
            return ClusterPlacement.REJECTED;
        double requiredCenter = ground.maximumSurface + groundMargin()
            + GROUND_CLEARANCE
            + SURFACE_SCAN_MARGIN
            - minimumBottomOffset();
        double centerY = Math.max(initialCenterY, requiredCenter);
        double maximumCenter = maxY - maximumTopOffset(archetype);
        return centerY <= maximumCenter
            ? new ClusterPlacement(
                true,
                centerY,
                maxKeel(centerY, ground.maximumSurface + groundMargin(), GROUND_CLEARANCE + SURFACE_SCAN_MARGIN))
            : ClusterPlacement.REJECTED;
    }

    private boolean isOverlandRingCandidate(FloatingIslandArchetype archetype, double centerX, double centerZ) {
        return overlandRings && archetype == FloatingIslandArchetype.RING_BASIN
            && groundProfiles.sampleSurface((int) Math.round(centerX), (int) Math.round(centerZ))
                .getKind() == OrigenBiomeProfile.Kind.LAND;
    }

    static double overlandCenterY(int maximumGround, double initialCenterY, int maxY) {
        if (maximumGround < 1 || !Double.isFinite(initialCenterY)) return Double.NaN;
        double center = Math.max(
            initialCenterY,
            maximumGround + OVERLAND_CANOPY_CLEARANCE + SURFACE_SCAN_MARGIN - minimumBottomOffset());
        return center <= maxY - maximumTopOffset(FloatingIslandArchetype.RING_BASIN) ? center : Double.NaN;
    }

    private GroundScan scanOverlandGround(double centerX, double centerZ, double initialCenterY) {
        int radius = OVERLAND_SCAN_RADIUS, maximum = Integer.MIN_VALUE;
        int minChunkX = Math.floorDiv((int) Math.floor(centerX) - radius, 16);
        int maxChunkX = Math.floorDiv((int) Math.ceil(centerX) + radius, 16);
        int minChunkZ = Math.floorDiv((int) Math.floor(centerZ) - radius, 16);
        int maxChunkZ = Math.floorDiv((int) Math.ceil(centerZ) + radius, 16);
        // Chunk-major traversal reuses the immutable source lattice, with no chunk loads and no per-column wrappers.
        for (int cz = minChunkZ; cz <= maxChunkZ; cz++) for (int cx = minChunkX; cx <= maxChunkX; cx++)
            for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) {
                int x = (cx << 4) + lx, z = (cz << 4) + lz;
                if (square(x - centerX) + square(z - centerZ) > square(radius)) continue;
                maximum = Math.max(maximum, ground.exactSurfaceHeight(x, z));
                if (!Double.isFinite(overlandCenterY(maximum, initialCenterY, maxY))) return new GroundScan(maximum);
            }
        return new GroundScan(maximum);
    }

    private GroundScan scanGround(double centerX, double centerZ, int radius, int requestedStep) {
        int step = groundStep(requestedStep);
        int maximumSurface = Integer.MIN_VALUE;
        int startX = floorToStep((int) Math.floor(centerX) - radius, step);
        int endX = (int) Math.ceil(centerX) + radius;
        int startZ = floorToStep((int) Math.floor(centerZ) - radius, step);
        int endZ = (int) Math.ceil(centerZ) + radius;
        double radiusSquared = square(radius);
        for (int blockZ = startZ; blockZ <= endZ; blockZ += step) {
            for (int blockX = startX; blockX <= endX; blockX += step) {
                if (square(blockX - centerX) + square(blockZ - centerZ) > radiusSquared) {
                    continue;
                }
                int surface = ground.surfaceHeight(blockX, blockZ);
                maximumSurface = Math.max(maximumSurface, surface);
            }
        }
        return new GroundScan(maximumSurface);
    }

    private boolean isRemoteOpenOcean(double centerX, double centerZ, int radius) {
        int startX = floorToStep((int) Math.floor(centerX) - radius, REMOTE_OCEAN_SCAN_STEP);
        int endX = (int) Math.ceil(centerX) + radius;
        int startZ = floorToStep((int) Math.floor(centerZ) - radius, REMOTE_OCEAN_SCAN_STEP);
        int endZ = (int) Math.ceil(centerZ) + radius;
        double radiusSquared = square(radius);
        for (int blockZ = startZ; blockZ <= endZ; blockZ += REMOTE_OCEAN_SCAN_STEP) {
            for (int blockX = startX; blockX <= endX; blockX += REMOTE_OCEAN_SCAN_STEP) {
                if (square(blockX - centerX) + square(blockZ - centerZ) > radiusSquared) {
                    continue;
                }
                if (!isOpenOceanProfile(groundProfiles.sampleSurface(blockX, blockZ))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isBroadRemoteOcean(long clusterHash, double centerX, double centerZ, int radius) {
        if (!isAbyssalOpenOceanProfile(
            groundProfiles.sampleSurface((int) Math.round(centerX), (int) Math.round(centerZ)))) {
            return false;
        }
        int oceanSamples = 0;
        int totalSamples = 0;
        int startX = floorToStep((int) Math.floor(centerX) - radius, AEROPELAGO_OCEAN_SCAN_STEP);
        int endX = (int) Math.ceil(centerX) + radius;
        int startZ = floorToStep((int) Math.floor(centerZ) - radius, AEROPELAGO_OCEAN_SCAN_STEP);
        int endZ = (int) Math.ceil(centerZ) + radius;
        double radiusSquared = square(radius);
        for (int blockZ = startZ; blockZ <= endZ; blockZ += AEROPELAGO_OCEAN_SCAN_STEP) {
            for (int blockX = startX; blockX <= endX; blockX += AEROPELAGO_OCEAN_SCAN_STEP) {
                double distanceSquared = square(blockX - centerX) + square(blockZ - centerZ);
                if (distanceSquared > radiusSquared) continue;
                totalSamples++;
                OrigenBiomeProfile profile = groundProfiles.sampleSurface(blockX, blockZ);
                if (profile.getKind() != OrigenBiomeProfile.Kind.OCEAN) return false;
                boolean abyssalOcean = isAbyssalOpenOceanProfile(profile);
                if (abyssalOcean) {
                    oceanSamples++;
                }
            }
        }
        return totalSamples > 0 && oceanSamples / (double) totalSamples >= AEROPELAGO_MINIMUM_BUFFER_COVERAGE
            && hasNoAeropelagoGroundIntrusions(clusterHash, centerX, centerZ);
    }

    private boolean hasNoAeropelagoGroundIntrusions(long clusterHash, double centerX, double centerZ) {
        int radius = footprintRadius(FloatingIslandArchetype.AEROPELAGO);
        int startX = floorToStep((int) Math.floor(centerX) - radius, AEROPELAGO_INTRUSION_SCAN_STEP);
        int endX = (int) Math.ceil(centerX) + radius;
        int startZ = floorToStep((int) Math.floor(centerZ) - radius, AEROPELAGO_INTRUSION_SCAN_STEP);
        int endZ = (int) Math.ceil(centerZ) + radius;
        double radiusSquared = square(radius);
        for (int blockZ = startZ; blockZ <= endZ; blockZ += AEROPELAGO_INTRUSION_SCAN_STEP) {
            for (int blockX = startX; blockX <= endX; blockX += AEROPELAGO_INTRUSION_SCAN_STEP) {
                if (square(blockX - centerX) + square(blockZ - centerZ) > radiusSquared) continue;
                if (isAbyssalOpenOceanProfile(groundProfiles.sampleSurface(blockX, blockZ))) continue;
                if (!aeropelagoOccupiesSampleCell(clusterHash, centerX, centerZ, blockX, blockZ)) continue;
                if (ground.surfaceHeight(blockX, blockZ) >= seaLevel) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean aeropelagoOccupiesSampleCell(long clusterHash, double centerX, double centerZ, int blockX,
        int blockZ) {
        double angle = unit(mix(clusterHash, 3L)) * Math.PI * 2.0D;
        ClusterColumn cluster = new ClusterColumn(
            FloatingIslandArchetype.AEROPELAGO,
            clusterHash,
            0.0D,
            0.0D,
            0.0D,
            0,
            centerX,
            centerZ,
            Math.cos(angle),
            Math.sin(angle));
        int[] offsets = { -AEROPELAGO_INTRUSION_SAMPLE_OFFSET, 0, AEROPELAGO_INTRUSION_SAMPLE_OFFSET };
        for (int offsetZ : offsets) for (int offsetX : offsets) {
            int sampleX = blockX + offsetX, sampleZ = blockZ + offsetZ;
            double deltaX = sampleX - centerX, deltaZ = sampleZ - centerZ;
            double along = deltaX * cluster.cos + deltaZ * cluster.sin,
                across = -deltaX * cluster.sin + deltaZ * cluster.cos;
            if (inside(cluster, along, across, sampleX, sampleZ) > 0.0D) return true;
        }
        return false;
    }

    private static boolean isOpenOceanProfile(OrigenBiomeProfile profile) {
        return profile.getKind() == OrigenBiomeProfile.Kind.OCEAN;
    }

    private static boolean isAbyssalOpenOceanProfile(OrigenBiomeProfile profile) {
        return profile.getKind() == OrigenBiomeProfile.Kind.OCEAN
            && profile.getTerrainFamily() == OrigenTerrainFamily.ABYSSAL_OCEAN;
    }

    private static int floorToStep(int coordinate, int step) {
        return Math.floorDiv(coordinate, step) * step;
    }

    private static int footprintRadius(FloatingIslandArchetype archetype) {
        return archetype == FloatingIslandArchetype.RING_BASIN ? 176
            : AEROPELAGO_CORE_RADIUS + AEROPELAGO_FEATHER_WIDTH + AEROPELAGO_BOUNDARY_JITTER;
    }

    /** How far below the cluster centre the islands' deck can dip, plus the shallowest keel a cluster gets. */
    private static int minimumBottomOffset() {
        return -(DECK_DIP + MIN_KEEL);
    }

    /** How far above the cluster centre a deck with its hills can rise. */
    private static int maximumTopOffset(FloatingIslandArchetype archetype) {
        return archetype == FloatingIslandArchetype.RING_BASIN ? 20 : 26;
    }

    /**
     * A cluster region's island family, or null: archipelagos and donuts in the sky layer; in the blend, the v71
     * grammar's choice (its tapered masses became Aeropelago archipelagos, its other grammars are gone).
     */
    private FloatingIslandArchetype archetype(long clusterHash) {
        if (skyLayer) return Math.floorMod(mix(clusterHash, 7L), 4L) == 0L ? FloatingIslandArchetype.RING_BASIN
            : FloatingIslandArchetype.AEROPELAGO;
        int original = Math.floorMod(mix(clusterHash, 4L), 6);
        return original == 0 ? FloatingIslandArchetype.AEROPELAGO
            : original == 2 ? FloatingIslandArchetype.RING_BASIN : null;
    }

    private double initialCenterY(FloatingIslandArchetype archetype, long clusterHash) {
        return yOffset
            + (archetype == FloatingIslandArchetype.RING_BASIN ? 132.0D + unit(mix(clusterHash, 2L)) * 36.0D : 165.0D);
    }

    /**
     * The island in a column: the cluster whose plan reaches farthest inside its coast there, and its deck-and-keel
     * span (IslandKeel). Everything is a function of the column and the cluster.
     */
    private ColumnContext island(int blockX, int blockZ, double topNoise, ClusterColumn[] clusters, int count) {
        ClusterColumn best = null;
        double inside = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < count; index++) {
            double candidate = inside(clusters[index], clusters[index].along, clusters[index].across, blockX, blockZ);
            if (candidate > inside) {
                inside = candidate;
                best = clusters[index];
            }
        }
        if (best == null) return new ColumnContext(null, null, IslandKeel.NONE, inside, 0.0D, false);
        if (inside < -COAST_REACH) {
            double[] islet = islet(best, blockX, blockZ);
            long span = islet == null ? IslandKeel.NONE
                : IslandKeel.column(
                    seed ^ best.hash ^ ISLET_SALT,
                    blockX,
                    blockZ,
                    islet[0],
                    islet[1],
                    IslandKeel.ISLET,
                    islet[2],
                    0.0D,
                    false);
            return span == IslandKeel.NONE ? new ColumnContext(null, best, IslandKeel.NONE, inside, 0.0D, false)
                : new ColumnContext(best, best, span, islet[0], islet[1], true);
        }
        boolean ring = best.archetype == FloatingIslandArchetype.RING_BASIN;
        IslandKeel.Shape shape = ring ? IslandKeel.DONUT : IslandKeel.ARCHIPELAGO;
        long keelSeed = seed ^ best.hash;
        double deck = deck(best, topNoise, blockX, blockZ);
        ClusterColumn cluster = best;
        double spire = IslandKeel.spire(
            keelSeed,
            blockX,
            blockZ,
            shape,
            (cellX, cellZ, x, z) -> seedInside(cluster, SPIRE_SALT, cellX, cellZ, x, z));
        boolean columnar = ((ring ? best.hash : islandCell(best.hash, blockX, blockZ)) >>> 21 & 1L) != 0L;
        long span = IslandKeel.column(keelSeed, blockX, blockZ, inside, deck, shape, best.maxDepth, spire, columnar);
        return new ColumnContext(best, best, span, inside, deck, false);
    }

    /** Blocks inside a cluster's coast at a point (negative outside), from its plan. */
    private double inside(ClusterColumn cluster, double along, double across, int blockX, int blockZ) {
        if (cluster.archetype == FloatingIslandArchetype.RING_BASIN) {
            double aspect = 0.88D + unit(mix(cluster.hash, 51L)) * 0.26D;
            double radial = Math.sqrt(square(along / aspect) + square(across));
            double angle = Math.atan2(across, along);
            double phase = signedUnit(mix(cluster.hash, 55L)) * Math.PI;
            double planNoise = ValueNoise2D
                .fractal(seed ^ RING_PLAN_SALT ^ cluster.hash, blockX, blockZ, 92.0D, 2, 0.55D);
            double ringRadius = 79.0D + unit(mix(cluster.hash, 52L)) * 17.0D
                + Math.sin(angle * 2.0D + phase) * 6.5D
                + Math.sin(angle * 5.0D - phase * 0.7D) * 3.0D
                + planNoise * 5.0D;
            double ringWidth = (27.0D + unit(mix(cluster.hash, 53L)) * 7.0D)
                * (0.93D + Math.sin(angle * 3.0D + phase * 1.3D) * 0.07D);
            return ringWidth - Math.abs(radial - ringRadius);
        }
        double radial = Math.sqrt(square(along) + square(across));
        double outerRadius = aeropelagoOuterRadius(cluster.hash, blockX, blockZ);
        if (radial >= outerRadius) return Double.NEGATIVE_INFINITY;
        double plan = skyLayer
            ? aeropelagoDensity.massPlan(
                along / SKY_AEROPELAGO_SCALE + signedUnit(mix(cluster.hash, 101L)) * 65_536.0D,
                across / SKY_AEROPELAGO_SCALE + signedUnit(mix(cluster.hash, 102L)) * 65_536.0D)
            : aeropelagoDensity
                .massPlan(aeropelagoSourceX(cluster.hash, along), aeropelagoSourceZ(cluster.hash, across));
        if (radial > aeropelagoCore()) plan -= PLAN_FEATHER * smoothstep(aeropelagoCore(), outerRadius, radial);
        return skyLayer ? (plan - SKY_PLAN_THRESHOLD) / SKY_PLAN_GRADIENT
            : (plan - BLEND_PLAN_THRESHOLD) / BLEND_PLAN_GRADIENT;
    }

    private double aeropelagoOuterRadius(long hash, int blockX, int blockZ) {
        double boundaryNoise = ValueNoise2D
            .fractal(seed ^ AEROPELAGO_BOUNDARY_SALT ^ hash, blockX, blockZ, 176.0D, 2, 0.5D);
        return aeropelagoCore() + aeropelagoFeather() + boundaryNoise * AEROPELAGO_BOUNDARY_JITTER;
    }

    private static double aeropelagoSourceX(long hash, double along) {
        return AeropelagoDensitySampler.toAuthoredHorizontal(along) + signedUnit(mix(hash, 101L)) * 65_536.0D;
    }

    private static double aeropelagoSourceZ(long hash, double across) {
        return AeropelagoDensitySampler.toAuthoredHorizontal(across) + signedUnit(mix(hash, 102L)) * 65_536.0D;
    }

    /** The rim height: archipelago islands at different heights, a donut rising and falling around its ring. */
    private double deck(ClusterColumn cluster, double topNoise, int blockX, int blockZ) {
        if (cluster.archetype == FloatingIslandArchetype.RING_BASIN) {
            double angle = Math.atan2(cluster.across, cluster.along);
            double phase = signedUnit(mix(cluster.hash, 55L)) * Math.PI;
            return cluster.centerY + 6.0D + topNoise * 3.5D + Math.sin(angle * 3.0D + phase) * 2.5D;
        }
        return cluster.centerY + 6.0D
            + 10.0D * ValueNoise2D.fractal(seed ^ DECK_SALT ^ cluster.hash, blockX, blockZ, 150.0D, 2, 0.5D);
    }

    /**
     * An islet whose disc reaches a column: {blocks inside its coast, its deck, its deepest keel}, or null. On a
     * jittered {@link #ISLET_CELL} grid, some cells hold one, three to six blocks across in radius, in the open sky
     * near the cluster's islands (between 14 and 60 blocks off their coasts), up to twelve blocks above or below
     * their decks.
     */
    private double[] islet(ClusterColumn cluster, int blockX, int blockZ) {
        int cellX = floor(blockX / ISLET_CELL), cellZ = floor(blockZ / ISLET_CELL);
        double[] best = null;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            long hash = WorldgenContext.mix64(
                seed ^ ISLET_SALT
                    ^ cluster.hash
                    ^ (cellX + dx) * 0x9E3779B97F4A7C15L
                    ^ (cellZ + dz) * 0xC2B2AE3D27D4EB4FL);
            if (unit(hash) >= ISLET_CHANCE) continue;
            double px = (cellX + dx + 0.2D + 0.6D * unit(mix(hash, 1L))) * ISLET_CELL;
            double pz = (cellZ + dz + 0.2D + 0.6D * unit(mix(hash, 2L))) * ISLET_CELL;
            double radius = 3.0D + 3.0D * unit(mix(hash, 3L));
            double distance = Math.sqrt(square(blockX - px) + square(blockZ - pz));
            if (distance > radius + 2.0D) continue;
            double around = seedInside(cluster, ISLET_SALT, cellX + dx, cellZ + dz, px, pz);
            if (around > -14.0D || around < -60.0D) continue;
            double inside = radius - distance;
            if (best != null && inside <= best[0]) continue;
            double deck = (cluster.archetype == FloatingIslandArchetype.RING_BASIN ? cluster.centerY + 6.0D
                : deck(cluster, 0.0D, (int) px, (int) pz)) + 24.0D * (unit(mix(hash, 4L)) - 0.5D);
            best = new double[] { inside, deck, Math.min(cluster.maxDepth, 1.8D * radius + 4.0D) };
        }
        return best;
    }

    /**
     * The islets centred in a chunk, {x, z, trees}: one or two trees each, the habitat's own (HostedIslandRuntime
     * plants them; an islet is too small for its flora's chance to land one).
     */
    public List<int[]> islets(int chunkX, int chunkZ) {
        List<int[]> found = new ArrayList<>(2);
        int minX = chunkX << 4, minZ = chunkZ << 4;
        for (int cellX = floor(minX / ISLET_CELL); cellX <= floor((minX + 15) / ISLET_CELL); cellX++)
            for (int cellZ = floor(minZ / ISLET_CELL); cellZ <= floor((minZ + 15) / ISLET_CELL); cellZ++) {
                int cx = (int) Math.floor((cellX + 0.5D) * ISLET_CELL),
                    cz = (int) Math.floor((cellZ + 0.5D) * ISLET_CELL);
                ClusterColumn cluster = columnCache.get()
                    .resolve(cx, cz).nearest;
                if (cluster == null) continue;
                long hash = WorldgenContext.mix64(
                    seed ^ ISLET_SALT ^ cluster.hash ^ cellX * 0x9E3779B97F4A7C15L ^ cellZ * 0xC2B2AE3D27D4EB4FL);
                if (unit(hash) >= ISLET_CHANCE) continue;
                int px = (int) Math.floor((cellX + 0.2D + 0.6D * unit(mix(hash, 1L))) * ISLET_CELL);
                int pz = (int) Math.floor((cellZ + 0.2D + 0.6D * unit(mix(hash, 2L))) * ISLET_CELL);
                if (px < minX || px > minX + 15 || pz < minZ || pz > minZ + 15) continue;
                ColumnContext column = columnCache.get()
                    .resolve(px, pz);
                if (!column.islet || column.span == IslandKeel.NONE) continue;
                found.add(new int[] { px, pz, 1 + (int) (unit(mix(hash, 5L)) * 2.0D) });
            }
        return found;
    }

    /** The domain warp a donut's plan is sampled through, at a column. */
    private double warp(long salt, int blockX, int blockZ) {
        return ValueNoise2D.fractal(seed ^ salt, blockX, blockZ, 236.0D, 2, 0.5D) * 28.0D;
    }

    /**
     * {@link #inside} at a seeded point, remembered per thread by its cluster, kind (salt) and grid cell: a pure
     * function of those, whichever column asks first.
     */
    private double seedInside(ClusterColumn cluster, long salt, int cellX, int cellZ, double x, double z) {
        SeedCache cache = seedCache.get();
        long key = WorldgenContext
            .mix64(cluster.hash ^ salt ^ cellX * 0x9E3779B97F4A7C15L ^ cellZ * 0xC2B2AE3D27D4EB4FL);
        int slot = (int) key & SeedCache.SIZE - 1;
        if (cache.used[slot] && cache.keys[slot] == key) return cache.values[slot];
        int blockX = (int) Math.round(x), blockZ = (int) Math.round(z);
        double deltaX = x - cluster.centerX, deltaZ = z - cluster.centerZ;
        if (cluster.archetype == FloatingIslandArchetype.RING_BASIN) {
            // Donuts are warped, as a column at the seed is (sampleColumn).
            deltaX += warp(WARP_X_SALT, blockX, blockZ);
            deltaZ += warp(WARP_Z_SALT, blockX, blockZ);
        }
        double along = deltaX * cluster.cos + deltaZ * cluster.sin,
            across = -deltaX * cluster.sin + deltaZ * cluster.cos;
        double inside = inside(cluster, along, across, blockX, blockZ);
        cache.keys[slot] = key;
        cache.values[slot] = inside;
        cache.used[slot] = true;
        return inside;
    }

    /**
     * The habitat cell of an archipelago island: one per jittered ~144-block cell, so the islands of one group differ
     * but an ordinary island stays in a single cell. Its habitat and its keel style follow it.
     */
    static long islandCell(long cluster, int x, int z) {
        return islandCell(cluster, x, z, null);
    }

    /** {@link #islandCell}, also giving the cell's point ({x, z}) when {@code point} is not null. */
    private static long islandCell(long cluster, int x, int z, double[] point) {
        int cellX = Math.floorDiv(x, HABITAT_CELL), cellZ = Math.floorDiv(z, HABITAT_CELL);
        long best = 0;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            long hash = WorldgenContext
                .mix64(cluster ^ (cellX + dx) * 0x9E3779B97F4A7C15L ^ (cellZ + dz) * 0xC2B2AE3D27D4EB4FL);
            double px = (cellX + dx + ((hash >>> 16) & 0xFFFF) / 65536.0D) * HABITAT_CELL;
            double pz = (cellZ + dz + ((hash >>> 32) & 0xFFFF) / 65536.0D) * HABITAT_CELL;
            double distance = (x - px) * (x - px) + (z - pz) * (z - pz);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = hash;
                if (point != null) {
                    point[0] = px;
                    point[1] = pz;
                }
            }
        }
        return best;
    }

    private static final int HABITAT_CELL = 144;

    private static boolean isProductionRemoteOceanArchetype(FloatingIslandArchetype archetype) {
        return archetype == FloatingIslandArchetype.RING_BASIN || archetype == FloatingIslandArchetype.AEROPELAGO;
    }

    private long regionHash(int regionX, int regionZ) {
        return WorldgenContext.mix64(seed ^ (long) regionX * 341873128712L ^ (long) regionZ * 132897987541L);
    }

    private static long coordinateKey(int blockX, int blockZ) {
        return (long) blockX << 32 ^ blockZ & 0xFFFF_FFFFL;
    }

    private static long mix(long hash, long offset) {
        return WorldgenContext.mix64(hash + offset * 0x9E3779B97F4A7C15L);
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static double unit(long hash) {
        return (hash >>> 11) * 0x1.0p-53;
    }

    private static double signedUnit(long hash) {
        return unit(hash) * 2.0D - 1.0D;
    }

    private static double smoothstep(double lower, double upper, double value) {
        double progress = clamp01((value - lower) / (upper - lower));
        return progress * progress * (3.0D - 2.0D * progress);
    }

    private static double square(double value) {
        return value * value;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static final class IslandSample {

        private static final IslandSample NONE = new IslandSample(Double.NEGATIVE_INFINITY, null, null);

        private final double density;
        private final OrigenBiomeProfile profile;
        private final FloatingIslandArchetype archetype;

        private IslandSample(double density, OrigenBiomeProfile profile, FloatingIslandArchetype archetype) {
            this.density = density;
            this.profile = profile;
            this.archetype = archetype;
        }
    }

    private final class ColumnCache {

        private final long[] keys = new long[COLUMN_CACHE_SIZE];
        private final boolean[] occupied = new boolean[COLUMN_CACHE_SIZE];
        private final ColumnContext[] columns = new ColumnContext[COLUMN_CACHE_SIZE];

        private ColumnContext resolve(int blockX, int blockZ) {
            long key = coordinateKey(blockX, blockZ);
            int slot = (int) WorldgenContext.mix64(key) & (COLUMN_CACHE_SIZE - 1);
            if (occupied[slot] && keys[slot] == key) {
                return columns[slot];
            }
            ColumnContext sampled = sampleColumn(blockX, blockZ);
            keys[slot] = key;
            occupied[slot] = true;
            columns[slot] = sampled;
            return sampled;
        }
    }

    /**
     * A column's island: its cluster (null where no island is near), the nearest cluster even then, its span, how far
     * inside the coast it lies, its deck, and whether the span is an islet's.
     */
    private record ColumnContext(ClusterColumn cluster, ClusterColumn nearest, long span, double inside, double deck,
        boolean islet) {}

    /** One cluster as seen from a column: its frame (true centre and rotation), height and keel budget. */
    private record ClusterColumn(FloatingIslandArchetype archetype, long hash, double along, double across,
        double centerY, int maxDepth, double centerX, double centerZ, double cos, double sin) {}

    /** Spire seeds' depth inside their coast, per thread (IslandKeel.Plan). */
    private static final class SeedCache {

        static final int SIZE = 256;
        final long[] keys = new long[SIZE];
        final double[] values = new double[SIZE];
        final boolean[] used = new boolean[SIZE];
    }

    private static final class ClusterPlacement {

        private static final ClusterPlacement REJECTED = new ClusterPlacement(false, 0.0D, 0);

        private final boolean allowed;
        private final double centerY;
        private final int maxKeel;

        private ClusterPlacement(boolean allowed, double centerY, int maxKeel) {
            this.allowed = allowed;
            this.centerY = centerY;
            this.maxKeel = maxKeel;
        }
    }

    private static final class GroundScan {

        private final int maximumSurface;

        private GroundScan(int maximumSurface) {
            this.maximumSurface = maximumSurface;
        }
    }

}
