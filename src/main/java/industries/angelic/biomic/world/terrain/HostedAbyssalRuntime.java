package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import industries.angelic.biomic.api.ChunkVerticalLayout;
import industries.angelic.biomic.content.alexscaves.AbyssalContentBlocks;
import industries.angelic.biomic.content.alexscaves.AbyssalStateCodec;
import industries.angelic.biomic.world.alexscaves.AbyssalFeaturePlanner;
import industries.angelic.biomic.world.alexscaves.AbyssalSourceHeightTransport;
import industries.angelic.biomic.world.alexscaves.OceanTrenchPlanner;

/**
 * Detached host facade for the pinned Ocean Trench and its intrinsic aquatic ecology.
 *
 * <p>
 * This class deliberately owns no block palette, chunk cache, World access or feature RNG synthesis. The
 * provider supplies immutable source cells and a save-pinned source attempt schedule. That keeps this seam usable
 * by both 256- and 512-high physical layouts without silently turning the source trench into a generic flooded
 * cavern or assigning shared material ids before the saved format owns them.
 * </p>
 */
public final class HostedAbyssalRuntime {

    public static final String BIOME_KEY = "alexscaves:abyssal_chasm";
    public static final String DECORATION_SIGNATURE = "alexscaves:abyssal-decoration-source-v1";
    public static final int STRUCTURE_SPACING_CHUNKS = 2;
    private static final int SOURCE_STEP = 16;

    public enum Identity {
        ABYSSAL,
        COMPATIBLE_OCEAN,
        FOREIGN_HOSTED,
        INCOMPATIBLE
    }

    public enum Feature {

        ABYSSAL_BOULDER("alexscaves:abyssal_boulder"),
        BLACK_VENT("alexscaves:black_vent"),
        TUBE_WORM("alexscaves:tube_worm"),
        MUSSEL("alexscaves:mussel"),
        PING_PONG_SPONGE("alexscaves:ping_pong_sponge"),
        DUSK_ANEMONE("alexscaves:dusk_anemone"),
        TWILIGHT_ANEMONE("alexscaves:twilight_anemone"),
        MIDNIGHT_ANEMONE("alexscaves:midnight_anemone");

        private final String id;

        Feature(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public record Start(int chunkX, int chunkZ) {

        public Start {
            if (Math.floorMod(chunkX, STRUCTURE_SPACING_CHUNKS) != 0
                || Math.floorMod(chunkZ, STRUCTURE_SPACING_CHUNKS) != 0)
                throw new IllegalArgumentException("Ocean Trench start is not on its spacing-2 source grid");
        }

        public int x() {
            return Math.multiplyExact(chunkX, 16);
        }

        public int z() {
            return Math.multiplyExact(chunkZ, 16);
        }
    }

    public record Extent(int radius) {}

    public record FeatureAttempt(Feature feature, long generatorSeed, AbyssalFeaturePlanner.Position origin) {

        public FeatureAttempt {
            Objects.requireNonNull(feature, "feature");
            Objects.requireNonNull(origin, "origin");
        }
    }

    public record PopulationResult(int scheduled, int accepted, int rejected, int writes) {}

    record UnderwaterMagmaConfig(int minimumAttempts, int attemptSpread, int floorSearchRange, int radius,
        float probability) {}

    static UnderwaterMagmaConfig underwaterMagmaConfig() {
        return new UnderwaterMagmaConfig(44, 9, 5, 1, .5F);
    }

    /** All methods are pure lookups over already-computed provider data; implementations must not load chunks. */
    public interface TerrainHost {

        ChunkVerticalLayout layout();

        Identity identity(int x, int sourceY, int z);
    }

    /** Exact source scheduler output, including global feature seeds and modifier order, owned by saved settings. */
    public interface FeatureSchedule {

        String signature();

        List<FeatureAttempt> attempts(int chunkX, int chunkZ);
    }

    /** Each accepted proposal is committed as an independent whole-feature transaction. */
    public interface PopulationHost extends AbyssalFeaturePlanner.PredictingHost {

        boolean owns(int x, int y, int z);

        boolean commit(AbyssalFeaturePlanner.Proposal proposal);
    }

    @FunctionalInterface
    public interface Ownership {

        boolean owns(int x, int y, int z);
    }

    private final TerrainHost terrain;
    private final AbyssalSourceHeightTransport height;
    private final java.util.Map<String, Integer> populationDiagnostics = new java.util.LinkedHashMap<>();

    /** Natural-only Abyssal ecology over compressed 256-high Origen. */
    public static HostedAbyssalRuntime legacy256(TerrainHost terrain) {
        return new HostedAbyssalRuntime(
            terrain,
            AbyssalSourceHeightTransport.legacy(
                Objects.requireNonNull(terrain, "terrain")
                    .layout()));
    }

    /** Natural-only Abyssal ecology over uncompressed (shifted512) Origen. */
    public static HostedAbyssalRuntime shifted512(TerrainHost terrain) {
        return new HostedAbyssalRuntime(terrain, AbyssalSourceHeightTransport.shifted512());
    }

    private HostedAbyssalRuntime(TerrainHost terrain, AbyssalSourceHeightTransport height) {
        this.terrain = Objects.requireNonNull(terrain, "terrain");
        this.height = Objects.requireNonNull(height, "height transport");
        if (height.layout() != terrain.layout())
            throw new IllegalArgumentException("Abyssal host and height transport disagree");
    }

    /** Source continuation samples the cave-biome identity every 16 blocks, including the origin sample. */
    public Extent extent(Start start) {
        // Structure validity is tested at the generation stub (chunk middle); the piece center and all continuation
        // walks intentionally remain at the source chunk's minimum block coordinate.
        requireAbyssal(start.x() + 8, OceanTrenchPlanner.SOURCE_CENTER_Y, start.z() + 8);
        int down = continueFor(
            start.x(),
            OceanTrenchPlanner.SOURCE_CENTER_Y,
            start.z(),
            0,
            -1,
            0,
            OceanTrenchPlanner.SOURCE_HEIGHT_RADIUS) + 16;
        int ground = OceanTrenchPlanner.SOURCE_CENTER_Y - (down - 2);
        int east = continueFor(start.x(), ground, start.z(), 1, 0, 0, OceanTrenchPlanner.SOURCE_RADIUS) + 32;
        int west = continueFor(start.x(), ground, start.z(), -1, 0, 0, OceanTrenchPlanner.SOURCE_RADIUS) + 32;
        int north = continueFor(start.x(), ground, start.z(), 0, 0, -1, OceanTrenchPlanner.SOURCE_RADIUS) + 32;
        int south = continueFor(start.x(), ground, start.z(), 0, 0, 1, OceanTrenchPlanner.SOURCE_RADIUS) + 32;
        return new Extent((east + west + north + south) / 4);
    }

    /** Applies only intrinsic blocks; mobs, loot, ruins and vanilla shared features are intentionally absent. */
    public PopulationResult populate(int chunkX, int chunkZ, FeatureSchedule schedule, PopulationHost host) {
        Objects.requireNonNull(schedule, "schedule");
        Objects.requireNonNull(host, "host");
        if (!DECORATION_SIGNATURE.equals(schedule.signature()))
            throw new IllegalArgumentException("Unsupported Abyssal decoration schedule: " + schedule.signature());
        List<FeatureAttempt> attempts = Objects
            .requireNonNull(schedule.attempts(chunkX, chunkZ), "Abyssal source attempts");
        AbyssalFeaturePlanner.PredictingHost bounded = new AbyssalFeaturePlanner.PredictingHost() {

            @Override
            public boolean covers(AbyssalFeaturePlanner.Bounds bounds) {
                return (height.compressed() || bounds.minY() >= height.physicalY(-64))
                    && populationBoundsAllowed(
                        bounds,
                        height.layout()
                            .height())
                    && host.covers(bounds);
            }

            @Override
            public AbyssalFeaturePlanner.HostCell predict(int x, int y, int z) {
                return host.owns(x, y, z) ? host.predict(x, y, z) : AbyssalFeaturePlanner.HostCell.OUTSIDE;
            }
        };
        int prior = -1, accepted = 0, rejected = 0, writes = 0;
        for (FeatureAttempt attempt : attempts) {
            int order = attempt.feature()
                .ordinal();
            if (order < prior) throw new IllegalArgumentException(
                "Abyssal source feature order regressed at " + attempt.feature()
                    .id());
            prior = order;
            recordPopulation(attempt.feature(), "scheduled");
            var origin = attempt.origin();
            if (!host.owns(origin.x(), origin.y(), origin.z())) {
                recordPopulation(attempt.feature(), "origin_outside");
                rejected++;
                continue;
            }
            AbyssalFeaturePlanner.Proposal proposal = plan(attempt, bounded);
            boolean complete = proposal.accepted();
            if (complete) for (var placement : proposal.placements())
                if (!populationPlacementAllowed(placement.position(), height) || !host.owns(
                    placement.position()
                        .x(),
                    placement.position()
                        .y(),
                    placement.position()
                        .z())) {
                            complete = false;
                            break;
                        }
            if (complete && host.commit(proposal)) {
                recordPopulation(attempt.feature(), "accepted");
                accepted++;
                writes += proposal.placements()
                    .size();
            } else {
                recordPopulation(
                    attempt.feature(),
                    proposal.accepted() ? "commit_rejected"
                        : "planner_" + proposal.failureReason()
                            .name()
                            .toLowerCase(java.util.Locale.ROOT));
                rejected++;
            }
        }
        return new PopulationResult(attempts.size(), accepted, rejected, writes);
    }

    /** Feature outcomes so far, for the benchmark report. */
    public synchronized String populationReport() {
        return "Abyssal population: " + populationDiagnostics;
    }

    private synchronized void recordPopulation(Feature feature, String outcome) {
        populationDiagnostics.merge(feature.id() + "." + outcome, 1, Integer::sum);
    }

    static FeatureSchedule biomicSchedule(long worldSeed, java.util.function.IntBinaryOperator physicalFloor,
        AbyssalSourceHeightTransport height) {
        return new FeatureSchedule() {

            @Override
            public String signature() {
                return DECORATION_SIGNATURE;
            }

            @Override
            public List<FeatureAttempt> attempts(int chunkX, int chunkZ) {
                Random random = new Random(
                    industries.angelic.biomic.world.WorldgenContext.mix64(
                        worldSeed ^ 0x4142595353414C46L
                            ^ (long) chunkX * 341873128712L
                            ^ (long) chunkZ * 132897987541L));
                List<FeatureAttempt> out = new java.util.ArrayList<>();
                int ox = chunkX << 4, oz = chunkZ << 4;
                add(out, Feature.ABYSSAL_BOULDER, 17 + random.nextInt(14), random, ox, oz, height);
                addFloor(out, Feature.BLACK_VENT, 3 + random.nextInt(10), random, ox, oz, physicalFloor);
                addFloor(out, Feature.TUBE_WORM, 5 + random.nextInt(12), random, ox, oz, physicalFloor);
                int mussels = 140 + random.nextInt(61);
                for (int i = 0; i < mussels; i++) {
                    int y = height.physicalY(-64 + random.nextInt(129));
                    int x = ox + random.nextInt(16), z = oz + random.nextInt(16);
                    out.add(
                        new FeatureAttempt(
                            Feature.MUSSEL,
                            random.nextLong(),
                            new AbyssalFeaturePlanner.Position(x, y, z)));
                }
                int sponge = Math.max(0, (int) Math.ceil((BIOME_INFO_NOISE.value(ox / 20D, oz / 20D)) * 7));
                for (int i = 0; i < sponge; i++) if (random.nextFloat() < .5F)
                    addFloor(out, Feature.PING_PONG_SPONGE, 1, random, ox, oz, physicalFloor);
                for (Feature flora : new Feature[] { Feature.DUSK_ANEMONE, Feature.TWILIGHT_ANEMONE,
                    Feature.MIDNIGHT_ANEMONE })
                    if (random.nextFloat() < .125F)
                        addFloor(out, flora, 3 + random.nextInt(4), random, ox, oz, physicalFloor);
                return java.util.Collections.unmodifiableList(out);
            }
        };
    }

    public PopulationResult populate(World world, int chunkX, int chunkZ, OrigenFeatureRuntime physicalRuntime,
        long worldSeed, Ownership ownership, java.util.function.IntBinaryOperator sourceFloor) {
        final PopulationResult[] result = { new PopulationResult(0, 0, 0, 0) };
        physicalRuntime.withPhysicalPopulation(world, chunkX, chunkZ, physical -> {
            PopulationHost host = new PopulationHost() {

                // Feature planners conservatively include Y=0 in their read envelope. That does not authorize a
                // write through the retained bottom seal; individual placements are checked again before commit.
                public boolean covers(AbyssalFeaturePlanner.Bounds b) {
                    return populationBoundsAllowed(
                        b,
                        height.layout()
                            .height());
                }

                public boolean owns(int x, int y, int z) {
                    return ownership.owns(x, y, z);
                }

                public AbyssalFeaturePlanner.HostCell predict(int x, int y, int z) {
                    if (!owns(x, y, z)) return AbyssalFeaturePlanner.HostCell.OUTSIDE;
                    Block block = physical.block(x, y, z);
                    if (block == Blocks.water) return AbyssalFeaturePlanner.HostCell.WATER;
                    if (block == AbyssalContentBlocks.MUCK) return AbyssalFeaturePlanner.HostCell.MUCK;
                    if (block == net.minecraft.init.Blocks.air) return AbyssalFeaturePlanner.HostCell.REPLACEABLE;
                    if (block == Blocks.bedrock || block == Blocks.lava)
                        return AbyssalFeaturePlanner.HostCell.UNMOVEABLE;
                    if (block == ganymedes01.etfuturum.ModBlocks.TUFF.get()) return AbyssalFeaturePlanner.HostCell.TUFF;
                    if (block == AbyssalContentBlocks.ABYSSMARINE || block == Blocks.stone
                        || block == ganymedes01.etfuturum.ModBlocks.DEEPSLATE.get())
                        return AbyssalFeaturePlanner.HostCell.SOLID_SUPPORT;
                    return AbyssalFeaturePlanner.HostCell.PROTECTED;
                }

                public boolean commit(AbyssalFeaturePlanner.Proposal proposal) {
                    for (var placement : proposal.placements()) {
                        var p = placement.position();
                        if (!populationPlacementAllowed(p, height) || !owns(p.x(), p.y(), p.z())
                            || AbyssalStateCodec.resolve(placement.state()) == null) return false;
                    }
                    for (var placement : proposal.placements()) {
                        var p = placement.position();
                        physical.setBlock(p.x(), p.y(), p.z(), placement.state());
                    }
                    return true;
                }
            };
            FeatureSchedule schedule = biomicSchedule(worldSeed, sourceFloor, height);
            // Natural ecology only: boulders, vents, underwater magma, then the late floor and wall flora.
            List<FeatureAttempt> all = schedule.attempts(chunkX, chunkZ);
            int localEnd = 0;
            while (localEnd < all.size() && all.get(localEnd)
                .feature()
                .ordinal() <= Feature.ABYSSAL_BOULDER.ordinal()) localEnd++;
            int undergroundEnd = localEnd;
            while (undergroundEnd < all.size() && all.get(undergroundEnd)
                .feature()
                .ordinal() <= Feature.BLACK_VENT.ordinal()) undergroundEnd++;
            PopulationResult local = populate(chunkX, chunkZ, fixedSchedule(all.subList(0, localEnd)), host);
            PopulationResult vents = populate(
                chunkX,
                chunkZ,
                fixedSchedule(all.subList(localEnd, undergroundEnd)),
                host);
            PopulationResult magma = populateUnderwaterMagma(
                chunkX,
                chunkZ,
                worldSeed,
                ownership,
                sourceFloor,
                physical,
                combine(local, vents));
            PopulationResult late = populate(
                chunkX,
                chunkZ,
                fixedSchedule(all.subList(undergroundEnd, all.size())),
                host);
            result[0] = new PopulationResult(
                magma.scheduled() + late.scheduled(),
                magma.accepted() + late.accepted(),
                magma.rejected() + late.rejected(),
                magma.writes() + late.writes());
        });
        return result[0];
    }

    private static FeatureSchedule fixedSchedule(List<FeatureAttempt> attempts) {
        List<FeatureAttempt> fixed = List.copyOf(attempts);
        return new FeatureSchedule() {

            public String signature() {
                return DECORATION_SIGNATURE;
            }

            public List<FeatureAttempt> attempts(int ignoredX, int ignoredZ) {
                return fixed;
            }
        };
    }

    /** Pinned vanilla underwater_magma: uniform 44..52 attempts, floor search 5, radius 1, probability .5. */
    private PopulationResult populateUnderwaterMagma(int chunkX, int chunkZ, long worldSeed, Ownership ownership,
        java.util.function.IntBinaryOperator sourceFloor, OrigenFeatureRuntime.PhysicalPopulation physical,
        PopulationResult prior) {
        Random random = new Random(
            industries.angelic.biomic.world.WorldgenContext.mix64(
                worldSeed ^ 0x554E4445524D4147L ^ (long) chunkX * 341873128712L ^ (long) chunkZ * 132897987541L));
        UnderwaterMagmaConfig config = underwaterMagmaConfig();
        int attempts = config.minimumAttempts() + random.nextInt(config.attemptSpread()), accepted = 0, rejected = 0,
            writes = 0;
        for (int attempt = 0; attempt < attempts; attempt++) {
            int x = (chunkX << 4) + random.nextInt(16), z = (chunkZ << 4) + random.nextInt(16);
            int originY = height.physicalY(-64 + random.nextInt(321));
            int oceanFloor = sourceFloor.applyAsInt(x, z);
            if (!underwaterMagmaOriginAllowed(originY, oceanFloor)) {
                rejected++;
                continue;
            }
            int floor = findUnderwaterFloor(x, originY, z, physical);
            if (floor < 1) {
                rejected++;
                continue;
            }
            java.util.LinkedHashMap<AbyssalFeaturePlanner.Position, String> proposal = new java.util.LinkedHashMap<>();
            // Original BlockPos.betweenClosed stream advances X first, then Y, then Z.
            for (var p : underwaterMagmaPositions(x, floor, z, config.radius()))
                if (random.nextFloat() < config.probability() && validMagmaCell(p.x(), p.y(), p.z(), physical))
                    proposal.put(p, "minecraft:magma_block");
            boolean admitted = !proposal.isEmpty();
            for (var p : proposal.keySet())
                if (!populationPlacementAllowed(new AbyssalFeaturePlanner.Position(p.x(), p.y(), p.z()), height)
                    || !ownership.owns(p.x(), p.y(), p.z())) {
                        admitted = false;
                        break;
                    }
            if (!admitted) {
                rejected++;
                continue;
            }
            for (var entry : proposal.entrySet()) {
                var p = entry.getKey();
                physical.setBlock(p.x(), p.y(), p.z(), entry.getValue());
            }
            accepted++;
            writes += proposal.size();
        }
        return new PopulationResult(
            prior.scheduled() + attempts,
            prior.accepted() + accepted,
            prior.rejected() + rejected,
            prior.writes() + writes);
    }

    private int findUnderwaterFloor(int x, int originY, int z, OrigenFeatureRuntime.PhysicalPopulation physical) {
        return findUnderwaterFloorCells(y -> {
            Block block = physical.block(x, y, z);
            // Modern WATER also contains flowing states; waterlogged non-WATER blocks are NOT
            // Column.scan's inside predicate in the pinned UnderwaterMagmaFeature.
            return block == Blocks.water || block == Blocks.flowing_water;
        }, originY, underwaterMagmaConfig().floorSearchRange(), height.physicalY(-64), height.physicalY(319) + 1);
    }

    /**
     * Source Column.scan DOWN endpoints, with explicit native-domain guards. Reads at most range
     * distinct cells, origin through origin-(range-1); never consults upper water or lower geology.
     */
    static int findUnderwaterFloorCells(java.util.function.IntPredicate water, int originY, int range, int minimumY,
        int maximumYExclusive) {
        Objects.requireNonNull(water);
        if (range < 1 || range > 64 || minimumY < 0 || maximumYExclusive <= minimumY || maximumYExclusive > 512)
            throw new IllegalArgumentException("Invalid bounded underwater floor search");
        if (originY < minimumY || originY >= maximumYExclusive || !water.test(originY)) return -1;
        int cursor = originY;
        for (int step = 1; step < range; step++) {
            if (cursor == minimumY) return -1;
            cursor--;
            if (!water.test(cursor)) return cursor;
        }
        return -1;
    }

    /** OCEAN_FLOOR_WG is first air above ORIGINAL solid, not the final excavated trench floor. */
    static boolean underwaterMagmaOriginAllowed(int originY, int originalOceanFloor) {
        return (long) originY <= (long) originalOceanFloor - 2;
    }

    static List<AbyssalFeaturePlanner.Position> underwaterMagmaPositions(int x, int floor, int z, int radius) {
        if (radius < 0 || radius > 8) throw new IllegalArgumentException("Excessive underwater magma proposal radius");
        Math.subtractExact(x, radius);
        Math.addExact(x, radius);
        Math.subtractExact(floor, radius);
        Math.addExact(floor, radius);
        Math.subtractExact(z, radius);
        Math.addExact(z, radius);
        var result = new ArrayList<AbyssalFeaturePlanner.Position>();
        for (int dz = -radius; dz <= radius; dz++)
            for (int dy = -radius; dy <= radius; dy++) for (int dx = -radius; dx <= radius; dx++)
                result.add(new AbyssalFeaturePlanner.Position(x + dx, floor + dy, z + dz));
        return List.copyOf(result);
    }

    private static boolean validMagmaCell(int x, int y, int z, OrigenFeatureRuntime.PhysicalPopulation physical) {
        if (isAirOrWater(physical.block(x, y, z)) || isAirOrWater(physical.block(x, y - 1, z))) return false;
        return !isAirOrWater(physical.block(x - 1, y, z)) && !isAirOrWater(physical.block(x + 1, y, z))
            && !isAirOrWater(physical.block(x, y, z - 1))
            && !isAirOrWater(physical.block(x, y, z + 1));
    }

    private static boolean isAirOrWater(Block block) {
        return block == Blocks.air || block == Blocks.water || block == Blocks.flowing_water;
    }

    private static PopulationResult combine(PopulationResult first, PopulationResult second) {
        return new PopulationResult(
            first.scheduled() + second.scheduled(),
            first.accepted() + second.accepted(),
            first.rejected() + second.rejected(),
            first.writes() + second.writes());
    }

    static boolean populationBoundsAllowed(AbyssalFeaturePlanner.Bounds bounds, int authoredHeight) {
        return bounds.minY() >= 0 && bounds.maxY() < authoredHeight;
    }

    static boolean populationPlacementAllowed(AbyssalFeaturePlanner.Position position,
        AbyssalSourceHeightTransport height) {
        return position.y() > height.physicalY(-64) && height.containsEcologyY(position.y());
    }

    private static final industries.angelic.biomic.world.modern.noise.ModernPerlinSimplexNoise BIOME_INFO_NOISE = new industries.angelic.biomic.world.modern.noise.ModernPerlinSimplexNoise(
        2345L,
        0);

    private static void add(List<FeatureAttempt> out, Feature feature, int count, Random random, int ox, int oz,
        AbyssalSourceHeightTransport height) {
        for (int i = 0; i < count; i++) {
            random.nextFloat();
            int x = ox + random.nextInt(16), z = oz + random.nextInt(16);
            int y = height.physicalY(-64 + random.nextInt(321));
            out.add(new FeatureAttempt(feature, random.nextLong(), new AbyssalFeaturePlanner.Position(x, y, z)));
        }
    }

    private static void addFloor(List<FeatureAttempt> out, Feature feature, int count, Random random, int ox, int oz,
        java.util.function.IntBinaryOperator sourceFloor) {
        for (int i = 0; i < count; i++) {
            int x = ox + random.nextInt(16), z = oz + random.nextInt(16), y = sourceFloor.applyAsInt(x, z);
            out.add(new FeatureAttempt(feature, random.nextLong(), new AbyssalFeaturePlanner.Position(x, y, z)));
        }
    }

    private AbyssalFeaturePlanner.Proposal plan(FeatureAttempt attempt, AbyssalFeaturePlanner.PredictingHost host) {
        return switch (attempt.feature()) {
            case ABYSSAL_BOULDER -> AbyssalFeaturePlanner
                .boulder(attempt.generatorSeed(), attempt.origin(), host, height);
            case BLACK_VENT -> AbyssalFeaturePlanner.blackVent(attempt.generatorSeed(), attempt.origin(), host, height);
            case TUBE_WORM -> AbyssalFeaturePlanner.tubeWorm(attempt.generatorSeed(), attempt.origin(), host, height);
            case MUSSEL -> AbyssalFeaturePlanner.mussel(attempt.generatorSeed(), attempt.origin(), host);
            case PING_PONG_SPONGE -> AbyssalFeaturePlanner
                .sponge(attempt.generatorSeed(), attempt.origin(), host, height);
            case DUSK_ANEMONE -> AbyssalFeaturePlanner
                .flora(AbyssalFeaturePlanner.Flora.DUSK, attempt.origin(), host, height);
            case TWILIGHT_ANEMONE -> AbyssalFeaturePlanner
                .flora(AbyssalFeaturePlanner.Flora.TWILIGHT, attempt.origin(), host, height);
            case MIDNIGHT_ANEMONE -> AbyssalFeaturePlanner
                .flora(AbyssalFeaturePlanner.Flora.MIDNIGHT, attempt.origin(), host, height);
        };
    }

    private int continueFor(int x, int y, int z, int dx, int dy, int dz, int cutoff) {
        int distance = 0;
        while (distance < cutoff) {
            long sx = (long) x + (long) dx * distance, sy = (long) y + (long) dy * distance,
                sz = (long) z + (long) dz * distance;
            if (sx < Integer.MIN_VALUE || sx > Integer.MAX_VALUE
                || sz < Integer.MIN_VALUE
                || sz > Integer.MAX_VALUE
                || terrain.identity((int) sx, (int) sy, (int) sz) != Identity.ABYSSAL) break;
            distance += SOURCE_STEP;
        }
        return Math.min(distance, cutoff);
    }

    private void requireAbyssal(int x, int y, int z) {
        if (terrain.identity(x, y, z) != Identity.ABYSSAL) throw new Rejected("source start is not " + BIOME_KEY);
    }

    private static final class Rejected extends RuntimeException {

        Rejected(String message) {
            super(message);
        }
    }
}
