package industries.angelic.biomic.world.modern.ruport;

import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import industries.angelic.biomic.world.modern.ModernDecorationEngine;
import industries.angelic.biomic.world.modern.ModernFeatureEngine;
import industries.angelic.biomic.world.modern.ModernSurfaceSystem;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Tiny state/position facade for the unmodified RU willow algorithm bodies. No game engine emulation. */
public final class BrimWillowCompat {

    private BrimWillowCompat() {}

    public static boolean place(boolean tall, ModernDecorationEngine engine, ModernDecorationEngine.World world,
        ModernRandom random, ModernDecorationEngine.Pos origin) {
        Context context = new Context(
            new BlockPos(origin.x, origin.y, origin.z),
            new WorldGenLevel(engine, world),
            random);
        return tall ? new TallBrimWillowFeature().place(context) : new BrimWillowFeature().place(context);
    }

    public record Context(BlockPos origin, WorldGenLevel level, ModernRandom random) {}

    public static Random incidental(LevelAccessor level, BlockPos pos) {
        return RuIncidentalRandom.random(level.world.worldSeed(), pos.x, pos.y, pos.z, RuIncidentalRandom.VINE);
    }

    public static class BlockPos {

        int x, y, z;

        public BlockPos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public BlockPos above() {
            return new BlockPos(x, y + 1, z);
        }

        public BlockPos below() {
            return new BlockPos(x, y - 1, z);
        }

        public BlockPos north() {
            return new BlockPos(x, y, z - 1);
        }

        public BlockPos south() {
            return new BlockPos(x, y, z + 1);
        }

        public BlockPos east() {
            return new BlockPos(x + 1, y, z);
        }

        public BlockPos west() {
            return new BlockPos(x - 1, y, z);
        }

        public MutableBlockPos mutable() {
            return new MutableBlockPos(x, y, z);
        }

        public static final class MutableBlockPos extends BlockPos {

            MutableBlockPos(int x, int y, int z) {
                super(x, y, z);
            }

            public void move(Direction direction) {
                if (direction != Direction.UP) throw new IllegalArgumentException();
                y++;
            }
        }
    }

    public enum Direction {

        UP;

        public enum Axis {
            X,
            Y,
            Z
        }
    }

    public static final class Block {

        private static final Map<String, Block> INTERN = new ConcurrentHashMap<>();
        final String id;

        private Block(String id) {
            this.id = id;
        }

        static Block of(String id) {
            return INTERN.computeIfAbsent(id, Block::new);
        }

        public BlockState defaultBlockState() {
            return new BlockState(id, null);
        }
    }

    public static final class BlockState {

        final String state;
        final ModernDecorationEngine engine;

        BlockState(String state, ModernDecorationEngine engine) {
            this.state = state;
            this.engine = engine;
        }

        public Block getBlock() {
            return Block.of(ModernDecorationEngine.blockName(state));
        }

        public boolean is(Block block) {
            return getBlock() == block;
        }

        public boolean is(String tag) {
            return engine.isBlockInTag(tag, state);
        }

        public BlockState setValue(String property, Object value) {
            return new BlockState(
                ModernFeatureEngine.property(
                    state,
                    property,
                    value.toString()
                        .toLowerCase(Locale.ROOT)),
                engine);
        }
    }

    public static class LevelSimulatedReader {

        final ModernDecorationEngine engine;
        final ModernDecorationEngine.World world;

        LevelSimulatedReader(ModernDecorationEngine engine, ModernDecorationEngine.World world) {
            this.engine = engine;
            this.world = world;
        }

        public BlockState getBlockState(BlockPos pos) {
            return new BlockState(world.blockState(pos.x, pos.y, pos.z), engine);
        }

        public boolean isStateAtPosition(BlockPos pos, Predicate<BlockState> predicate) {
            return predicate.test(getBlockState(pos));
        }
    }

    public static class LevelAccessor extends LevelSimulatedReader {

        LevelAccessor(ModernDecorationEngine engine, ModernDecorationEngine.World world) {
            super(engine, world);
        }

        public boolean isOutsideBuildHeight(BlockPos pos) {
            return pos.y < world.minBuildHeight() || pos.y >= world.maxBuildHeight();
        }

        public boolean isEmptyBlock(BlockPos pos) {
            return ModernSurfaceSystem.isAirState(getBlockState(pos).state);
        }

        public boolean setBlock(BlockPos pos, BlockState state, int flags) {
            return world.setBlock(pos.x, pos.y, pos.z, state.state);
        }
    }

    public static final class WorldGenLevel extends LevelAccessor {

        WorldGenLevel(ModernDecorationEngine engine, ModernDecorationEngine.World world) {
            super(engine, world);
        }
    }

    public static final class Blocks {

        public static final Block AIR = Block.of("minecraft:air"), CAVE_AIR = Block.of("minecraft:cave_air"),
            VOID_AIR = Block.of("minecraft:void_air"), SCULK = Block.of("minecraft:sculk");
    }

    public static final class RUBlocks {

        public static final Wood BRIMWOOD_WOOD_SET = new Wood();
        public static final Natural BRIMWOOD_NATURAL_SET = new Natural();
        public static final Block BRIMSPROUT_NYLIUM = Block.of("regions_unexplored:brimsprout_nylium");
    }

    public static final class Wood {

        public Block getLogMagma() {
            return Block.of("regions_unexplored:brimwood_log_magma");
        }

        public Block getLog() {
            return Block.of("regions_unexplored:brimwood_log");
        }
    }

    public static final class Natural {

        public Block getLeaves() {
            return Block.of("regions_unexplored:brimwood_leaves");
        }
    }

    public static final class LeavesBlock {

        public static final String PERSISTENT = "persistent";
    }

    public static final class RotatedPillarBlock {

        public static final String AXIS = "axis";
    }

    public static final class MagmaLogBlock {

        public static final String TRANSITION_BLOCK = "transition_block";
    }

    public static final class BlockTags {

        public static final String NYLIUM = "minecraft:nylium", DIRT = "minecraft:dirt";
    }

    public static final class RUBlockTags {

        public static final String REPLACEABLE_BLOCKS = "regions_unexplored:replaceable_blocks";
    }
}
