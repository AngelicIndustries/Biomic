package industries.angelic.biomic.world;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

import industries.angelic.biomic.world.modern.ModernBlockSemantics;
import industries.angelic.biomic.world.modern.ModernCavePlantSupport;
import industries.angelic.biomic.world.modern.ModernDecorationEngine;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationGraph;
import industries.angelic.biomic.world.modern.ModernDensityGraph;
import industries.angelic.biomic.world.modern.ModernFeatureEngine;
import industries.angelic.biomic.world.modern.ModernGroundCoverSupport;
import industries.angelic.biomic.world.modern.ModernStateCompatibility;
import industries.angelic.biomic.world.modern.ModernSurfaceSystem;
import industries.angelic.biomic.world.modern.noise.ModernLegacyRandom;
import industries.angelic.biomic.world.modern.noise.ModernWorldgenRandom;

/**
 * Regions Unexplored's four Nether biomes, dressed in the Tartarus Nether by their own placed features (the pinned
 * graph's Nether supplement) through Biomic's modern decoration engine. Like Gardens of the Dead, the decoration runs
 * with the terrain on a worker: each chunk's reads the composed terrain and its own writes, and writes reach at most a
 * chunk into its neighbours (NetherTerrainSource's Area), so the Nether is the same in any generation order. Only RU's
 * own features run; Tartarus gives every neighbour its ores, lava and glowstone.
 */
final class RuNetherDressing {

    /** The terrain one chunk's decoration reads and writes. */
    interface Region {

        Block blockAt(int x, int y, int z);

        int metadata(int x, int y, int z);

        void set(int x, int y, int z, String state);

        /** The RU Nether biome (its modern key) of a column, or null. */
        String ruBiome(int x, int z);
    }

    /** The decoration step a Nether biome's features sit in (underground decoration). */
    private static final int STEP = 7;
    private static final String BLACKSTONE_BASIN = "regions_unexplored:blackstone_basin";
    /** RU's ceiling depth under its surface rule's UNDER_CEILING. */
    private static final int CEILING_DEPTH = 3;
    private static final ModernDecorationGraph GRAPH = new ModernDecorationGraph();
    /** The engines cache noises and compiled functions lazily: one per worker thread. */
    private static final ThreadLocal<Map<Long, Seeded>> SEEDED = ThreadLocal.withInitial(HashMap::new);

    private record Seeded(ModernDecorationEngine engine, ModernDensityGraph density) {}

    private RuNetherDressing() {}

    /** The modern key of the RU Nether biome a neighbour stands for ("biomic:infernal_holt" to RU's). */
    static String modernKey(String key) {
        return "regions_unexplored:" + key.substring(key.indexOf(':') + 1);
    }

    /** Decorates one chunk's anchors; writes may reach into its neighbours. */
    static void decorate(Region region, long seed, int chunkX, int chunkZ) {
        int startX = chunkX << 4, startZ = chunkZ << 4;
        Set<String> present = new TreeSet<>();
        for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) {
            String biome = region.ruBiome(startX + x, startZ + z);
            if (biome != null) present.add(biome);
        }
        if (present.isEmpty()) return;
        Seeded seeded = SEEDED.get()
            .computeIfAbsent(seed, ignored -> {
                ModernDensityGraph density = new ModernDensityGraph(seed);
                ModernFeatureEngine features = new ModernFeatureEngine();
                return new Seeded(
                    new ModernDecorationEngine(
                        GRAPH,
                        density,
                        (engine, type, config, world, random, pos) -> features
                            .place(engine, type, config, world, random, pos)),
                    density);
            });
        AreaWorld view = new AreaWorld(region, seed);
        if (present.contains(BLACKSTONE_BASIN)) blackstoneCeilings(view, seeded.density(), startX, startZ);
        ModernWorldgenRandom random = new ModernWorldgenRandom();
        long decorationSeed = random.setDecorationSeed(seed, startX, startZ);
        Pos origin = new Pos(startX, 0, startZ);
        List<String> order = GRAPH.featureOrder(STEP);
        for (String feature : GRAPH.orderedFeatures(present, STEP)) {
            random.setFeatureSeed(decorationSeed, order.indexOf(feature), STEP);
            seeded.engine()
                .place(new com.google.gson.JsonPrimitive(feature), feature, view, random, origin);
        }
    }

    /**
     * RU's surface rule lines Blackstone Basin's ceilings with blackstone where the Nether state selector is positive;
     * Tartarus' terrain gives a neighbour its floors only, so the dressing lays them before the features (the basin's
     * hanging earlights grow from blackstone).
     */
    private static void blackstoneCeilings(AreaWorld view, ModernDensityGraph density, int startX, int startZ) {
        for (int x = startX; x < startX + 16; x++) for (int z = startZ; z < startZ + 16; z++) {
            if (!BLACKSTONE_BASIN.equals(view.region.ruBiome(x, z))) continue;
            if (density.sampleNoise("minecraft:nether_state_selector", x, 0, z) < 0) continue;
            for (int y = 1; y < NetherTerrainSource.HEIGHT - 1; y++) {
                if (view.region.blockAt(x, y, z) != Blocks.air) continue;
                for (int depth = 1; depth <= CEILING_DEPTH && y + depth < NetherTerrainSource.HEIGHT; depth++) {
                    if (view.region.blockAt(x, y + depth, z) != Blocks.netherrack) break;
                    view.setBlock(x, y + depth, z, "minecraft:blackstone");
                }
            }
        }
    }

    /**
     * The area as the engine sees it: 1.7.10 blocks and metadata, the form a written state takes in the Nether's
     * palette (one entry per block and metadata, however many modern properties a feature names).
     */
    private static final class AreaWorld implements ModernDecorationEngine.World {

        final Region region;
        private final long seed;

        AreaWorld(Region region, long seed) {
            this.region = region;
            this.seed = seed;
        }

        private static boolean inside(int y) {
            return y >= 0 && y < NetherTerrainSource.HEIGHT;
        }

        private Block block(int x, int y, int z) {
            return inside(y) ? region.blockAt(x, y, z) : Blocks.bedrock;
        }

        @Override
        public long worldSeed() {
            return seed;
        }

        @Override
        public int minBuildHeight() {
            return 0;
        }

        @Override
        public int maxBuildHeight() {
            return NetherTerrainSource.HEIGHT;
        }

        @Override
        public int seaLevel() {
            return 32;
        }

        @Override
        public int height(String heightmap, int x, int z) {
            for (int y = NetherTerrainSource.HEIGHT - 1; y >= 0; y--)
                if (region.blockAt(x, y, z) != Blocks.air) return y + 1;
            return 0;
        }

        @Override
        public String blockState(int x, int y, int z) {
            if (!inside(y)) return "minecraft:bedrock";
            return ModernStateCompatibility.modernState(region.blockAt(x, y, z), region.metadata(x, y, z));
        }

        @Override
        public String fluidState(int x, int y, int z) {
            return ModernBlockSemantics.fluidState(blockState(x, y, z));
        }

        @Override
        public String biome(int x, int y, int z) {
            String biome = region.ruBiome(x, z);
            return biome != null ? biome : "minecraft:nether_wastes";
        }

        @Override
        public String noiseBiome(int x, int y, int z) {
            return biome(x, y, z);
        }

        @Override
        public boolean shouldFreeze(int x, int y, int z, boolean mustBeAtEdge) {
            return false;
        }

        @Override
        public boolean shouldSnow(int x, int y, int z) {
            return false;
        }

        @Override
        public boolean isSolid(int x, int y, int z) {
            return block(x, y, z).getMaterial()
                .isSolid();
        }

        @Override
        public boolean hasSturdyFace(int x, int y, int z, String direction) {
            return ModernBlockSemantics.legacyFullFace(block(x, y, z));
        }

        @Override
        public boolean canSurvive(String state, int x, int y, int z) {
            if (!inside(y) || y <= 0) return false;
            String below = blockState(x, y - 1, z);
            Boolean pinned = ModernGroundCoverSupport.testPinned(state, below);
            if (pinned != null) return pinned;
            if (ModernCavePlantSupport.handles(state))
                return ModernCavePlantSupport.test(state, below, block(x, y - 1, z).isOpaqueCube());
            return block(x, y - 1, z).isOpaqueCube();
        }

        @Override
        public boolean isReplaceable(int x, int y, int z) {
            if (!inside(y)) return false;
            Block block = region.blockAt(x, y, z);
            return block == Blocks.air || ModernSurfaceSystem.isAirState(blockState(x, y, z))
                || block.getMaterial()
                    .isReplaceable();
        }

        @Override
        public boolean randomChance(int x, int y, int z, float chance) {
            return new ModernLegacyRandom(seed).forkPositional()
                .at(x, y, z)
                .nextFloat() < chance;
        }

        @Override
        public boolean intersectsStructure(int x, int y, int z, int range, String structure) {
            return false;
        }

        @Override
        public List<Pos> carvingMask(String step, int chunkX, int chunkZ) {
            return Collections.emptyList();
        }

        @Override
        public boolean setBlock(int x, int y, int z, String state) {
            if (!inside(y)) return false;
            var resolved = ModernStateCompatibility.resolve(state);
            Block block = resolved.getBlock();
            region.set(
                x,
                y,
                z,
                block == Blocks.air ? "minecraft:air" : NetherStates.nativeState(block, resolved.getMetadata()));
            return true;
        }
    }
}
