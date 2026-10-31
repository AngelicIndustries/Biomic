package industries.angelic.biomic.world.structure;

import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenDesertWells;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces;
import net.minecraft.world.gen.structure.MapGenStructure;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

import industries.angelic.biomic.mixins.early.minecraft.AccessorScatteredFeature;
import industries.angelic.biomic.world.WorldgenContext;

/**
 * Scattered structures, on the ground and on floating islands. Vanilla's desert and jungle temples and witch huts, and
 * Biomic's own igloos ({@link BiomicIgloo}) and ruined portals ({@link BiomicRuinedPortal}): on the ground one
 * candidate chunk per region, standing where that chunk's habitat is the kind's and the ground under the whole
 * footprint is flat enough, at the real surface of the terrain as provided; in the sky the best spot of an island
 * cluster for the kind its island's habitat holds. Dungeons, desert wells and the islands' amethyst geodes are small
 * and lie wholly inside one population box. All of it is drawn last in the box, like the other structures
 * (StructurePopulationBox).
 */
public final class BiomicScatteredFeatures {

    enum Kind {

        DESERT_TEMPLE(6),
        JUNGLE_TEMPLE(6),
        WITCH_HUT(4),
        IGLOO(3),
        RUINED_PORTAL(6);

        /** How far the ground under the footprint may rise and fall. */
        final int relief;

        Kind(int relief) {
            this.relief = relief;
        }
    }

    /**
     * A region's structure: its piece's corner (the start chunk holds it), orientation and random salt, its footprint,
     * and the height its piece's base stands at (the ground's top, or the first air over it where vanilla's piece sits
     * so).
     */
    record Plan(Kind kind, int originX, int originZ, int orientation, long salt, StructureBoundingBox footprint,
        int ground) {

        int chunkX() {
            return originX >> 4;
        }

        int chunkZ() {
            return originZ >> 4;
        }
    }

    private static final int REGION = 32, SEPARATION = 8, PORTAL_REGION = 40, PORTAL_SEPARATION = 12;
    private static final long TEMPLE_SALT = 0x6A09E667F3BCC909L, PORTAL_SALT = 0x3C6EF372FE94F82BL,
        SMALL_SALT = 0xA54FF53A5F1D36F1L, SKY_SALT = 0xBB67AE8584CAA73BL;
    /** Island clusters (FloatingIslandDensitySampler's regions); the search grid over one; spots confirmed exactly. */
    private static final int SKY_REGION = industries.angelic.biomic.world.terrain.FloatingIslandDensitySampler.CLUSTER_REGION,
        SKY_STEP = 16, SKY_CONFIRMED = 8;
    private static final Plan NONE = new Plan(null, 0, 0, 0, 0L, null, 0);

    private final long seed;
    private final StructureTerrain terrain;
    private final StructurePopulationBox.Terrain provided;
    private final Habitats habitats;
    private final MapGenScattered delegate = new MapGenScattered();
    private final BoxedStructureStarts boxed = new BoxedStructureStarts();
    private final Map<Long, Plan> plans = new java.util.LinkedHashMap<>(256, .75F, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<Long, Plan> eldest) {
            return size() > 4096;
        }
    };
    private Villages villages = (x, y, z, reach) -> false;

    /** Whether a village lies within {@code reach} blocks of a block at its height (from the village plans). */
    public interface Villages {

        boolean near(int x, int y, int z, int reach);
    }

    /**
     * Scattered structures keep {@link #VILLAGE_CLEARANCE} blocks from villages: a temple or portal in one makes no
     * sense.
     */
    public void setVillages(Villages villages) {
        this.villages = java.util.Objects.requireNonNull(villages);
    }

    private static final int VILLAGE_CLEARANCE = 8;

    private boolean nearVillage(StructureBoundingBox footprint, int ground) {
        int reach = Math.max(footprint.getXSize(), footprint.getZSize()) / 2 + VILLAGE_CLEARANCE;
        return villages
            .near((footprint.minX + footprint.maxX) / 2, ground, (footprint.minZ + footprint.maxZ) / 2, reach);
    }

    /**
     * Habitat keys, as the flora palettes name them ({@code origen:DESERT}, {@code regions_unexplored:fen}): a ground
     * column's, or a floating island's at a height. Biomic's biome objects carry no useful type for this.
     */
    public interface Habitats {

        String ground(int x, int z);

        String island(int x, int y, int z);
    }

    public BiomicScatteredFeatures(WorldgenContext context, StructureTerrain terrain,
        StructurePopulationBox.Terrain provided, Habitats habitats) {
        this.seed = context.getSettings()
            .getWorldSeed();
        this.terrain = terrain;
        this.provided = provided;
        this.habitats = habitats;
    }

    /** Structure components and starts, for saving and loading the starts with the world. */
    public static void register() {
        MapGenStructureIO.registerStructure(Start.class, "BiomicScattered");
        MapGenStructureIO.func_143031_a(BiomicIgloo.class, "BiomicIgloo");
        MapGenStructureIO.func_143031_a(BiomicRuinedPortal.class, "BiomicRuinedPortal");
    }

    public void prepare(IChunkProvider provider, World world, int chunkX, int chunkZ, Block[] blocks) {
        delegate.func_151539_a(provider, world, chunkX, chunkZ, blocks);
    }

    /** One population box: the scattered structures reaching it, then its dungeons, geode and well. */
    public void populate(World world, int chunkX, int chunkZ) {
        boxed.draw(delegate.starts(), world, seed ^ TEMPLE_SALT, chunkX, chunkZ, provided);
        Random random = new Random(
            WorldgenContext.mix64(seed ^ SMALL_SALT ^ chunkX * 0x9E3779B97F4A7C15L ^ chunkZ * 0xC2B2AE3D27D4EB4FL));
        int minX = (chunkX << 4) + 8, minZ = (chunkZ << 4) + 8;
        dungeons(world, random, minX, minZ);
        for (int cx = chunkX - 1; cx <= chunkX; cx++)
            for (int cz = chunkZ - 1; cz <= chunkZ; cz++) islandGeode(world, cx, cz);
        int x = minX + 2 + random.nextInt(12), z = minZ + 2 + random.nextInt(12);
        int surface = provided.surface(x, z);
        if (surface < 0) return;
        String habitat = habitat(x, surface, z);
        if (desert(habitat) && random.nextInt(1000) == 0 && provided.block(x, surface, z) == Blocks.sand)
            new WorldGenDesertWells().generate(world, random, x, surface + 1, z);
    }

    /** Vanilla's eight tries a chunk, in rock between the floor and a few blocks under the ground. */
    private void dungeons(World world, Random random, int minX, int minZ) {
        WorldGenDungeons dungeon = new WorldGenDungeons();
        for (int i = 0; i < 8; i++) {
            int x = minX + 4 + random.nextInt(8), z = minZ + 4 + random.nextInt(8);
            int top = provided.surface(x, z) - 6, bottom = terrain.minY() + 4;
            if (top <= bottom) continue;
            dungeon.generate(world, random, x, bottom + random.nextInt(top - bottom), z);
        }
    }

    /**
     * Now and then an amethyst geode (Et Futurum's) in the core of a floating island thick enough to hold one, decided
     * per population box from its own seed. EFR's geode grows from its origin towards +x, +y and +z (its points lie 4
     * to
     * 6 blocks out, its shell up to 9 beyond), so it is wider than a box: every box it reaches draws it (the box it
     * starts in and the next ones along +x and +z) and keeps its own part, as with the other structures.
     */
    private void islandGeode(World world, int chunkX, int chunkZ) {
        Random random = new Random(
            WorldgenContext.mix64(seed ^ GEODE_SALT ^ chunkX * 0x9E3779B97F4A7C15L ^ chunkZ * 0xC2B2AE3D27D4EB4FL));
        if (random.nextInt(ISLAND_GEODE_CHANCE) != 0) return;
        int minX = (chunkX << 4) + 8, minZ = (chunkZ << 4) + 8, x = minX + 8, z = minZ + 8;
        int span = terrain.skySpan(x, z);
        if (span < 0) return;
        int bottom = StructureTerrain.spanBottom(span), top = StructureTerrain.spanTop(span);
        if (top - bottom < ISLAND_GEODE_THICKNESS || provided.surface(x, z) < top - 3) return;
        WorldGenerator geode = geode();
        if (geode != null)
            geode.generate(world, random, minX + GEODE_ORIGIN, (bottom + top) / 2 - 6, minZ + GEODE_ORIGIN);
    }

    /** One island box in this many has a geode; the island must be this thick; where in its box the geode starts. */
    private static final int ISLAND_GEODE_CHANCE = 20, ISLAND_GEODE_THICKNESS = 22, GEODE_ORIGIN = 4;
    private static final long GEODE_SALT = 0x510E527FADE682D1L;
    private WorldGenerator geode;
    private boolean geodeResolved;

    private WorldGenerator geode() {
        if (!geodeResolved) {
            geodeResolved = true;
            var outer = ganymedes01.etfuturum.configuration.configs.ConfigWorld.amethystOuterBlock;
            var middle = ganymedes01.etfuturum.configuration.configs.ConfigWorld.amethystMiddleBlock;
            geode = outer == null || middle == null ? null
                : new ganymedes01.etfuturum.world.generate.feature.WorldGenGeode(outer, middle);
        }
        return geode;
    }

    public boolean protects(int x, int y, int z, int margin) {
        return protects(x, y, z, margin, REGION, SEPARATION, TEMPLE_SALT)
            || protects(x, y, z, margin, PORTAL_REGION, PORTAL_SEPARATION, PORTAL_SALT)
            || protectsSky(x, y, z, margin);
    }

    private boolean protectsSky(int x, int y, int z, int margin) {
        int rx = Math.floorDiv(x, SKY_REGION), rz = Math.floorDiv(z, SKY_REGION);
        SkyAround around = skyAround(rx, rz);
        for (int i = 0; i < 9; i++) {
            var cluster = around.clusters[i];
            if (cluster == null) continue;
            int r = cluster.radius() + REACH + margin;
            if (Math.abs(x - cluster.centerX()) > r || Math.abs(z - cluster.centerZ()) > r) continue;
            Plan plan = around.plans[i];
            if (plan == null) around.plans[i] = plan = skyPlan(rx + i / 3 - 1, rz + i % 3 - 1);
            if (plan != NONE && covers(plan, x, y, z, margin)) return true;
        }
        return false;
    }

    /**
     * The island clusters of a sky region and the eight around it, and their plans once asked for, remembered for the
     * last region: decoration asks for every column it roots in, and a population's columns share their region.
     * Clusters and plans are pure, so threads that race here fill in the same values.
     */
    private static final class SkyAround {

        final int regionX, regionZ;
        final industries.angelic.biomic.world.terrain.FloatingIslandDensitySampler.Cluster[] clusters = new industries.angelic.biomic.world.terrain.FloatingIslandDensitySampler.Cluster[9];
        final Plan[] plans = new Plan[9];

        SkyAround(int regionX, int regionZ) {
            this.regionX = regionX;
            this.regionZ = regionZ;
        }
    }

    private volatile SkyAround skyAround;

    private SkyAround skyAround(int regionX, int regionZ) {
        SkyAround known = skyAround;
        if (known != null && known.regionX == regionX && known.regionZ == regionZ) return known;
        SkyAround around = new SkyAround(regionX, regionZ);
        for (int i = 0; i < 9; i++) around.clusters[i] = cluster(regionX + i / 3 - 1, regionZ + i % 3 - 1);
        skyAround = around;
        return around;
    }

    private static boolean covers(Plan plan, int x, int y, int z, int margin) {
        StructureBoundingBox b = plan.footprint;
        return x >= b.minX - margin && x <= b.maxX + margin
            && z >= b.minZ - margin
            && z <= b.maxZ + margin
            && (y == Integer.MIN_VALUE || y >= plan.ground - 8 && y <= plan.ground + 24);
    }

    private static final industries.angelic.biomic.world.terrain.FloatingIslandDensitySampler.Cluster NO_CLUSTER = new industries.angelic.biomic.world.terrain.FloatingIslandDensitySampler.Cluster(
        0,
        0,
        -1,
        null);
    private final Map<Long, industries.angelic.biomic.world.terrain.FloatingIslandDensitySampler.Cluster> clusters = new java.util.LinkedHashMap<>(
        256,
        .75F,
        true) {

        @Override
        protected boolean removeEldestEntry(
            Map.Entry<Long, industries.angelic.biomic.world.terrain.FloatingIslandDensitySampler.Cluster> eldest) {
            return size() > 1024;
        }
    };

    /** A region's island cluster, or null; asked for every guarded column, so remembered (it samples the ground). */
    private industries.angelic.biomic.world.terrain.FloatingIslandDensitySampler.Cluster cluster(int regionX,
        int regionZ) {
        // Mixed: a raw packed key hashes to regionX ^ regionZ, and neighbouring regions pile into one bucket.
        long key = WorldgenContext.mix64((long) regionX << 32 ^ regionZ & 0xFFFF_FFFFL);
        synchronized (clusters) {
            var known = clusters.get(key);
            if (known != null) return known == NO_CLUSTER ? null : known;
        }
        var cluster = terrain.skyCluster(regionX, regionZ);
        synchronized (clusters) {
            clusters.put(key, cluster == null ? NO_CLUSTER : cluster);
        }
        return cluster;
    }

    private boolean protects(int x, int y, int z, int margin, int region, int separation, long salt) {
        int regionBlocks = region << 4, reach = REACH + margin;
        for (int rx = Math.floorDiv(x - reach, regionBlocks); rx <= Math.floorDiv(x + reach, regionBlocks); rx++)
            for (int rz = Math.floorDiv(z - reach, regionBlocks); rz <= Math.floorDiv(z + reach, regionBlocks); rz++) {
                // Only near the region's candidate chunk is its plan worth making (it reads the terrain).
                long chunk = candidate(rx, rz, region, separation, salt);
                int cx = (int) (chunk >> 32) << 4, cz = (int) chunk << 4;
                if (x < cx - reach || x > cx + 15 + reach || z < cz - reach || z > cz + 15 + reach) continue;
                Plan plan = plan(rx, rz, region, separation, salt);
                if (plan != NONE && covers(plan, x, y, z, margin)) return true;
            }
        return false;
    }

    private Plan planAt(int chunkX, int chunkZ) {
        Plan plan = planAt(chunkX, chunkZ, REGION, SEPARATION, TEMPLE_SALT);
        if (plan == null) plan = planAt(chunkX, chunkZ, PORTAL_REGION, PORTAL_SEPARATION, PORTAL_SALT);
        if (plan != null) return plan;
        int x = (chunkX << 4) + 8, z = (chunkZ << 4) + 8;
        int rx = Math.floorDiv(x, SKY_REGION), rz = Math.floorDiv(z, SKY_REGION);
        SkyAround around = skyAround(rx, rz);
        for (int i = 0; i < 9; i++) {
            var cluster = around.clusters[i];
            if (cluster == null) continue;
            int r = cluster.radius() + 16;
            if (Math.abs(x - cluster.centerX()) > r || Math.abs(z - cluster.centerZ()) > r) continue;
            Plan sky = around.plans[i];
            if (sky == null) around.plans[i] = sky = skyPlan(rx + i / 3 - 1, rz + i % 3 - 1);
            if (sky != NONE && sky.chunkX() == chunkX && sky.chunkZ() == chunkZ) return sky;
        }
        return null;
    }

    private Plan planAt(int chunkX, int chunkZ, int region, int separation, long salt) {
        int rx = Math.floorDiv(chunkX, region), rz = Math.floorDiv(chunkZ, region);
        if (candidate(rx, rz, region, separation, salt) != ((long) chunkX << 32 | chunkZ & 0xFFFF_FFFFL)) return null;
        Plan plan = plan(rx, rz, region, separation, salt);
        return plan == NONE ? null : plan;
    }

    /** A footprint's farthest reach from its start chunk's corner (the desert temple is 21 blocks across). */
    private static final int REACH = 24;

    private Random regionRandom(int regionX, int regionZ, long salt) {
        return new Random(
            WorldgenContext.mix64(seed ^ salt ^ regionX * 0x9E3779B97F4A7C15L ^ regionZ * 0xC2B2AE3D27D4EB4FL));
    }

    /** The region's candidate chunk, {@code x << 32 | z}: known without reading the terrain. */
    private long candidate(int regionX, int regionZ, int region, int separation, long salt) {
        Random random = regionRandom(regionX, regionZ, salt);
        int chunkX = regionX * region + random.nextInt(region - separation);
        int chunkZ = regionZ * region + random.nextInt(region - separation);
        return (long) chunkX << 32 | chunkZ & 0xFFFF_FFFFL;
    }

    private Plan plan(int regionX, int regionZ, int region, int separation, long salt) {
        long key = planKey(regionX, regionZ, salt);
        synchronized (plans) {
            Plan known = plans.get(key);
            if (known != null) return known;
        }
        // As with sky plans: the first thread computes it, a worker's warm-up or the server's check, the other waits.
        Pending pending = this.pending.computeIfAbsent(key, ignored -> new Pending());
        if (!pending.claimed.compareAndSet(false, true)) return pending.result.join();
        try {
            Plan plan = compute(regionX, regionZ, region, separation, salt);
            synchronized (plans) {
                plans.put(key, plan);
            }
            pending.result.complete(plan);
            return plan;
        } catch (RuntimeException | Error failure) {
            pending.result.completeExceptionally(failure);
            throw failure;
        } finally {
            this.pending.remove(key, pending);
        }
    }

    private static long planKey(int regionX, int regionZ, long salt) {
        return WorldgenContext.mix64(salt ^ (long) regionX << 32 ^ regionZ & 0xFFFF_FFFFL);
    }

    private Plan compute(int regionX, int regionZ, int region, int separation, long salt) {
        Random random = regionRandom(regionX, regionZ, salt);
        int chunkX = regionX * region + random.nextInt(region - separation);
        int chunkZ = regionZ * region + random.nextInt(region - separation);
        int x = (chunkX << 4) + 8, z = (chunkZ << 4) + 8;
        int surface = provided.surface(x, z);
        if (surface < 0) return NONE;
        Kind kind = salt == PORTAL_SALT ? Kind.RUINED_PORTAL : kind(habitat(x, surface, z));
        if (kind == null || salt == PORTAL_SALT && water(x, surface, z)) return NONE;
        int orientation = random.nextInt(4);
        return fit(kind, chunkX << 4, chunkZ << 4, orientation, random.nextLong(), surface);
    }

    /**
     * The plan for a piece with its corner at (originX, originZ), or NONE: the whole footprint on one layer's ground
     * (within a dozen blocks of {@code surface}), within the kind's relief, and never over water but for a hut.
     */
    private Plan fit(Kind kind, int originX, int originZ, int orientation, long pieceSalt, int surface) {
        StructureBoundingBox footprint = piece(kind, originX, originZ, orientation, 64, pieceSalt).getBoundingBox();
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE, sum = 0, columns = 0;
        for (int fx = footprint.minX; fx <= footprint.maxX; fx++)
            for (int fz = footprint.minZ; fz <= footprint.maxZ; fz++) {
                int top = provided.surface(fx, fz);
                if (top < 0 || Math.abs(top - surface) > 12) return NONE;
                if (kind != Kind.WITCH_HUT && water(fx, top, fz)) return NONE;
                low = Math.min(low, top);
                high = Math.max(high, top);
                sum += top;
                columns++;
            }
        if (high - low > kind.relief) return NONE;
        int top = Math.round((float) sum / columns);
        int centerX = (footprint.minX + footprint.maxX) / 2, centerZ = (footprint.minZ + footprint.maxZ) / 2;
        int ground = switch (kind) {
            case WITCH_HUT -> waterline(centerX, top, centerZ);
            case JUNGLE_TEMPLE -> top + 1;
            default -> top;
        };
        footprint.offset(0, ground - footprint.minY, 0);
        if (nearVillage(footprint, ground)) return NONE;
        return new Plan(kind, originX, originZ, orientation, pieceSalt, footprint, ground);
    }

    private static long skyKey(int regionX, int regionZ) {
        return WorldgenContext.mix64(SKY_SALT ^ (long) regionX << 32 ^ regionZ & 0xFFFF_FFFFL);
    }

    private Plan skyPlan(int regionX, int regionZ) {
        long key = skyKey(regionX, regionZ);
        synchronized (plans) {
            Plan known = plans.get(key);
            if (known != null) return known;
        }
        // The first thread to need a plan computes it, the rest take its result: a worker's warm-up and the server's
        // structure check never compute one twice, and the server never waits on a worker that has not started.
        Pending pending = this.pending.computeIfAbsent(key, ignored -> new Pending());
        if (!pending.claimed.compareAndSet(false, true)) return pending.result.join();
        try {
            Plan plan = computeSky(regionX, regionZ);
            synchronized (plans) {
                plans.put(key, plan);
            }
            pending.result.complete(plan);
            return plan;
        } catch (RuntimeException | Error failure) {
            pending.result.completeExceptionally(failure);
            throw failure;
        } finally {
            this.pending.remove(key, pending);
        }
    }

    /** A sky plan asked for: claimed by the first thread that computes it. */
    private static final class Pending {

        final java.util.concurrent.atomic.AtomicBoolean claimed = new java.util.concurrent.atomic.AtomicBoolean();
        final java.util.concurrent.CompletableFuture<Plan> result = new java.util.concurrent.CompletableFuture<>();
    }

    private final java.util.concurrent.ConcurrentHashMap<Long, Pending> pending = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Set<Long> warming = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** How far the server's structure check looks for starts around a chunk it provides (MapGenBase's range). */
    private static final int CHECK_RANGE = 8;

    /**
     * Hands the plans the server's structure check will ask for around a chunk to the workers. A sky plan searches its
     * cluster's islands, which samples the ground under them: done by the server thread, it made the first chunk of
     * an area with islands overhead take hundreds of milliseconds. Plans are pure functions of the seed and the
     * region, so a worker's plan is the one the server would have made.
     */
    public void warm(int chunkX, int chunkZ, java.util.concurrent.Executor workers) {
        int x = (chunkX << 4) + 8, z = (chunkZ << 4) + 8, reach = CHECK_RANGE << 4;
        for (int rx = Math.floorDiv(x - reach, SKY_REGION) - 1; rx <= Math.floorDiv(x + reach, SKY_REGION) + 1; rx++)
            for (int rz = Math.floorDiv(z - reach, SKY_REGION) - 1; rz
                <= Math.floorDiv(z + reach, SKY_REGION) + 1; rz++) {
                    long key = skyKey(rx, rz);
                    synchronized (plans) {
                        if (plans.containsKey(key)) continue;
                    }
                    if (pending.containsKey(key) || !warming.add(key)) continue;
                    int regionX = rx, regionZ = rz;
                    Runnable task = () -> {
                        try {
                            // Only a cluster within the check's reach of this chunk is asked for (planAt).
                            var cluster = cluster(regionX, regionZ);
                            int r = cluster == null ? -1 : cluster.radius() + 16 + reach;
                            if (cluster != null && Math.abs(x - cluster.centerX()) <= r
                                && Math.abs(z - cluster.centerZ()) <= r) skyPlan(regionX, regionZ);
                        } catch (RuntimeException ignored) {
                            // The server's own request plans again and reports it.
                        } finally {
                            warming.remove(key);
                        }
                    };
                    try {
                        workers.execute(task);
                    } catch (java.util.concurrent.RejectedExecutionException stopping) {
                        warming.remove(key);
                        return;
                    }
                }
        warmPlans(chunkX, chunkZ, REGION, SEPARATION, TEMPLE_SALT, workers);
        warmPlans(chunkX, chunkZ, PORTAL_REGION, PORTAL_SEPARATION, PORTAL_SALT, workers);
    }

    /**
     * The temple and ruined portal plans the check will ask for around a chunk: those of the regions whose candidate
     * chunk lies within its reach (planAt). A plan reads the provided terrain at its candidate chunk, which the server
     * built for it, often well ahead of the look-ahead: up to half a second for one chunk.
     */
    private void warmPlans(int chunkX, int chunkZ, int region, int separation, long salt,
        java.util.concurrent.Executor workers) {
        for (int rx = Math.floorDiv(chunkX - CHECK_RANGE, region); rx
            <= Math.floorDiv(chunkX + CHECK_RANGE, region); rx++)
            for (int rz = Math.floorDiv(chunkZ - CHECK_RANGE, region); rz
                <= Math.floorDiv(chunkZ + CHECK_RANGE, region); rz++) {
                    long candidate = candidate(rx, rz, region, separation, salt);
                    if (Math.abs((int) (candidate >> 32) - chunkX) > CHECK_RANGE
                        || Math.abs((int) candidate - chunkZ) > CHECK_RANGE) continue;
                    long key = planKey(rx, rz, salt);
                    synchronized (plans) {
                        if (plans.containsKey(key)) continue;
                    }
                    if (pending.containsKey(key) || !warming.add(key)) continue;
                    int regionX = rx, regionZ = rz;
                    Runnable task = () -> {
                        try {
                            plan(regionX, regionZ, region, separation, salt);
                        } catch (RuntimeException ignored) {
                            // The server's own request plans again and reports it.
                        } finally {
                            warming.remove(key);
                        }
                    };
                    try {
                        workers.execute(task);
                    } catch (java.util.concurrent.RejectedExecutionException stopping) {
                        warming.remove(key);
                        return;
                    }
                }
    }

    /**
     * One structure on two clusters of floating islands in three: searched over the cluster on the islands' estimated
     * tops (a 12-block grid), a spot whose island habitat holds a kind (else a ruined portal) and whose footprint the
     * estimates keep on the island, flattest first, then confirmed on the provided terrain as on the ground.
     */
    private Plan computeSky(int regionX, int regionZ) {
        var cluster = cluster(regionX, regionZ);
        if (cluster == null) return NONE;
        Random random = regionRandom(regionX, regionZ, SKY_SALT);
        if (random.nextInt(3) == 0) return NONE;
        int orientation = random.nextInt(4);
        long pieceSalt = random.nextLong();
        java.util.List<int[]> spots = new java.util.ArrayList<>(); // {portal?, relief, kind, x, z}
        // Each kind's footprint for this cluster's orientation, at the origin: a spot only shifts it.
        StructureBoundingBox[] shapes = new StructureBoundingBox[Kind.values().length];
        for (Kind kind : Kind.values())
            shapes[kind.ordinal()] = piece(kind, 0, 0, orientation, 64, pieceSalt).getBoundingBox();
        int r = cluster.radius();
        for (int dx = -r; dx <= r; dx += SKY_STEP) for (int dz = -r; dz <= r; dz += SKY_STEP) {
            if (dx * dx + dz * dz > r * r) continue;
            int x = cluster.centerX() + dx, z = cluster.centerZ() + dz;
            int top = terrain.skyTopEstimate(x, z);
            if (top < 0) continue;
            String island = habitats.island(x, top, z);
            Kind kind = island == null ? null : kind(island.toLowerCase(java.util.Locale.ROOT));
            if (kind == null) kind = Kind.RUINED_PORTAL;
            StructureBoundingBox shape = shapes[kind.ordinal()];
            int relief = estimatedRelief(
                new StructureBoundingBox(x + shape.minX, 0, z + shape.minZ, x + shape.maxX, 0, z + shape.maxZ));
            if (relief <= kind.relief)
                spots.add(new int[] { kind == Kind.RUINED_PORTAL ? 1 : 0, relief, kind.ordinal(), x, z });
        }
        spots.sort(
            java.util.Comparator.<int[]>comparingInt(spot -> spot[0])
                .thenComparingInt(spot -> spot[1]));
        for (int[] spot : spots.subList(0, Math.min(SKY_CONFIRMED, spots.size()))) {
            Plan plan = fitIsland(Kind.values()[spot[2]], spot[3], spot[4], orientation, pieceSalt);
            if (plan != NONE) return plan;
        }
        return NONE;
    }

    /**
     * As {@link #fit} on an island, from the islands' exact columns (the raster their blocks come from), so no terrain
     * of a chunk far from any player is built: the whole footprint on island columns within the kind's relief, and on
     * islands of the kind's habitat (a cluster's islands may each have their own).
     */
    private Plan fitIsland(Kind kind, int originX, int originZ, int orientation, long pieceSalt) {
        StructureBoundingBox footprint = piece(kind, originX, originZ, orientation, 64, pieceSalt).getBoundingBox();
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE, sum = 0, columns = 0;
        for (int fx = footprint.minX; fx <= footprint.maxX; fx++)
            for (int fz = footprint.minZ; fz <= footprint.maxZ; fz++) {
                int span = terrain.skyColumn(fx, fz);
                if (span < 0) return NONE;
                int top = StructureTerrain.spanTop(span);
                low = Math.min(low, top);
                high = Math.max(high, top);
                sum += top;
                columns++;
            }
        if (high - low > kind.relief) return NONE;
        if (kind != Kind.RUINED_PORTAL) {
            int[][] points = { { footprint.minX, footprint.minZ }, { footprint.maxX, footprint.minZ },
                { footprint.minX, footprint.maxZ }, { footprint.maxX, footprint.maxZ },
                { (footprint.minX + footprint.maxX) / 2, (footprint.minZ + footprint.maxZ) / 2 } };
            for (int[] point : points) {
                String island = habitats
                    .island(point[0], StructureTerrain.spanTop(terrain.skyColumn(point[0], point[1])), point[1]);
                if (island == null || kind(island.toLowerCase(java.util.Locale.ROOT)) != kind) return NONE;
            }
        }
        int top = Math.round((float) sum / columns);
        int ground = kind == Kind.JUNGLE_TEMPLE || kind == Kind.WITCH_HUT ? top + 1 : top;
        footprint.offset(0, ground - footprint.minY, 0);
        if (nearVillage(footprint, ground)) return NONE;
        return new Plan(kind, originX, originZ, orientation, pieceSalt, footprint, ground);
    }

    /** The relief of a footprint's corners and centre on the islands' estimated tops; MAX_VALUE where one is off. */
    private int estimatedRelief(StructureBoundingBox box) {
        int[][] points = { { box.minX, box.minZ }, { box.maxX, box.minZ }, { box.minX, box.maxZ },
            { box.maxX, box.maxZ }, { (box.minX + box.maxX) / 2, (box.minZ + box.maxZ) / 2 } };
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int[] point : points) {
            int top = terrain.skyTopEstimate(point[0], point[1]);
            if (top < 0) return Integer.MAX_VALUE;
            low = Math.min(low, top);
            high = Math.max(high, top);
        }
        return high - low;
    }

    /** The structure's piece, built as vanilla builds it (the same random state gives the same orientation). */
    private static StructureComponent piece(Kind kind, int x, int z, int orientation, int y, long salt) {
        Random random = new Random(salt);
        return switch (kind) {
            case DESERT_TEMPLE -> new ComponentScatteredFeaturePieces.DesertPyramid(random, x, z);
            case JUNGLE_TEMPLE -> new ComponentScatteredFeaturePieces.JunglePyramid(random, x, z);
            case WITCH_HUT -> new ComponentScatteredFeaturePieces.SwampHut(random, x, z);
            case IGLOO -> new BiomicIgloo(x, y, z, orientation);
            case RUINED_PORTAL -> new BiomicRuinedPortal(x, y, z, orientation, salt);
        };
    }

    /** The first air (or the water's surface, for a hut on stilts) over the ground. */
    private int waterline(int x, int ground, int z) {
        int y = ground + 1;
        while (y < terrain.maxY() && provided.block(x, y, z)
            .getMaterial()
            .isLiquid()) y++;
        return y;
    }

    private boolean water(int x, int surface, int z) {
        return provided.block(x, surface + 1, z)
            .getMaterial()
            .isLiquid();
    }

    /** The habitat a structure stands in: a floating island's own where the ground is its, else the column's. */
    private String habitat(int x, int surface, int z) {
        int span = terrain.skySpan(x, z);
        if (span >= 0 && surface >= (span >> 16) && surface <= (span & 0xFFFF) + 3) {
            for (int below = 0; below < 4; below++) {
                String sky = habitats.island(x, surface - below, z);
                if (sky != null) return sky.toLowerCase(java.util.Locale.ROOT);
            }
        }
        String ground = habitats.ground(x, z);
        return ground == null ? "" : ground.toLowerCase(java.util.Locale.ROOT);
    }

    private static Kind kind(String habitat) {
        if (has(habitat, "ocean", "sea", "beach", "shore", "coast", "river", "reef")) return null;
        if (has(habitat, "jungle", "rainforest", "tropical") && !has(habitat, "temperate")) return Kind.JUNGLE_TEMPLE;
        if (swamp(habitat)) return Kind.WITCH_HUT;
        if (desert(habitat)) return Kind.DESERT_TEMPLE;
        if (snowy(habitat)) return Kind.IGLOO;
        return null;
    }

    private static boolean desert(String habitat) {
        return has(habitat, "desert", "dune", "outback") && !has(habitat, "beach", "shore");
    }

    /** Habitats where snow lies (RU's tundra is cold but dry). */
    private static boolean snowy(String habitat) {
        return has(habitat, "snow", "frost", "frozen", "glacier", "arctic", "alps", "ice_", "icy");
    }

    private static boolean swamp(String habitat) {
        return has(habitat, "swamp", "marsh", "bayou", "fen", "bog", "mire", "wetland", "mangrove");
    }

    private static boolean has(String habitat, String... words) {
        for (String word : words) if (habitat.contains(word)) return true;
        return false;
    }

    /** A start holding one scattered piece, at its plan's ground. */
    public static final class Start extends StructureStart {

        public Start() {}

        Start(int chunkX, int chunkZ, StructureComponent piece) {
            super(chunkX, chunkZ);
            components.add(piece);
            updateBoundingBox();
        }
    }

    private final class MapGenScattered extends MapGenStructure {

        @Override
        public String func_143025_a() {
            return "BiomicScattered";
        }

        Map<Long, StructureStart> starts() {
            return structureMap;
        }

        @Override
        protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
            return planAt(chunkX, chunkZ) != null;
        }

        @Override
        protected StructureStart getStructureStart(int chunkX, int chunkZ) {
            Plan plan = planAt(chunkX, chunkZ);
            StructureComponent piece = piece(plan.kind, plan.originX, plan.originZ, plan.orientation, 64, plan.salt);
            piece.getBoundingBox()
                .offset(0, plan.ground - piece.getBoundingBox().minY, 0);
            // Vanilla's jungle temple and hut find their ground from the first box drawing them; theirs is the plan's.
            if (piece instanceof AccessorScatteredFeature feature) feature.biomic$setGround(plan.ground);
            if (piece instanceof BiomicRuinedPortal portal) {
                StructureBoundingBox b = plan.footprint;
                portal.snowy = snowy(habitat((b.minX + b.maxX) / 2, plan.ground, (b.minZ + b.maxZ) / 2));
            }
            return new Start(chunkX, chunkZ, piece);
        }
    }
}
