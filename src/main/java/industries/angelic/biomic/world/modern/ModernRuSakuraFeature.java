package industries.angelic.biomic.world.modern;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Direct port of pinned RU's SakuraTreeFeature used by all four magnolia trees. */
final class ModernRuSakuraFeature {

    private static final int Y = 0;
    private static final int X = 1;
    private static final int Z = 2;

    /** dx, dy, dz, axis. The first five entries form the left crown's limb. */
    private static final int[][][] LIMBS = {
        { { 0, 0, -1, Y }, { 0, 1, -1, Y }, { 0, 2, -1, Y }, { 0, 2, -2, Y }, { 0, 3, -2, Y }, { 0, 1, 1, Z },
            { 0, 1, 2, Z }, { 0, 2, 3, Z } },
        { { 0, 0, 1, Y }, { 0, 1, 1, Y }, { 0, 2, 1, Y }, { 0, 2, 2, Y }, { 0, 3, 2, Y }, { 0, 1, -1, Z },
            { 0, 1, -2, Z }, { 0, 2, -3, Z } },
        { { 1, 0, 0, Y }, { 1, 1, 0, Y }, { 1, 2, 0, Y }, { 2, 2, 0, Y }, { 2, 3, 0, Y }, { -1, 1, 0, X },
            { -2, 1, 0, X }, { -3, 2, 0, X } },
        { { -1, 0, 0, Y }, { -1, 1, 0, Y }, { -1, 2, 0, Y }, { -2, 2, 0, Y }, { -2, 3, 0, Y }, { 1, 1, 0, X },
            { 2, 1, 0, X }, { 3, 2, 0, X } },
        { { 1, 0, -1, Y }, { 1, 1, -1, Y }, { 1, 2, -1, Y }, { 2, 2, -2, Y }, { 2, 3, -2, Y }, { -1, 1, 1, Z },
            { -1, 1, 2, Z }, { -2, 2, 3, Z } },
        { { -1, 0, -1, Y }, { -1, 1, -1, Y }, { -1, 2, -1, Y }, { -2, 2, -2, Y }, { -2, 3, -2, Y }, { 1, 1, 1, Z },
            { 1, 1, 2, Z }, { 2, 2, 3, Z } },
        { { 1, 0, 1, Y }, { 1, 1, 1, Y }, { 1, 2, 1, Y }, { 2, 2, 2, Y }, { 2, 3, 2, Y }, { -1, 1, -1, Z },
            { -1, 1, -2, Z }, { -2, 2, -3, Z } },
        { { -1, 0, 1, Y }, { -1, 1, 1, Y }, { -1, 2, 1, Y }, { -2, 2, 2, Y }, { -2, 3, 2, Y }, { 1, 1, -1, Z },
            { 1, 1, -2, Z }, { 2, 2, -3, Z } },
        { { 1, 0, -1, Y }, { 1, 1, -1, Y }, { 1, 2, -1, Y }, { 2, 2, -2, Y }, { 2, 3, -2, Y }, { -1, 1, 1, X },
            { -2, 1, 1, X }, { -3, 2, 2, X } },
        { { -1, 0, -1, Y }, { -1, 1, -1, Y }, { -1, 2, -1, Y }, { -2, 2, -2, Y }, { -2, 3, -2, Y }, { 1, 1, 1, X },
            { 2, 1, 1, X }, { 3, 2, 2, X } },
        { { 1, 0, 1, Y }, { 1, 1, 1, Y }, { 1, 2, 1, Y }, { 2, 2, 2, Y }, { 2, 3, 2, Y }, { -1, 1, -1, X },
            { -2, 1, -1, X }, { -3, 2, -2, X } },
        { { -1, 0, 1, Y }, { -1, 1, 1, Y }, { -1, 2, 1, Y }, { -2, 2, 2, Y }, { -2, 3, 2, Y }, { 1, 1, -1, X },
            { 2, 1, -1, X }, { 3, 2, -2, X } } };

    private static final int[][] BEES = { { 0, 1, 3, 1 }, { 0, 1, -3, 0 }, { -3, 1, 0, 3 }, { 3, 1, 0, 2 },
        { -2, 1, 3, 1 }, { 2, 1, 3, 1 }, { -2, 1, -3, 0 }, { 2, 1, -3, 0 }, { -3, 1, 2, 3 }, { 3, 1, 2, 2 },
        { -3, 1, -2, 3 }, { 3, 1, -2, 2 } };

    private static final int[][][] BASES = { { { 1, 0, -1, Y }, { 0, 0, 1, Z } }, { { -1, 0, -1, Y }, { 0, 0, 1, Z } },
        { { 1, 0, 1, Y }, { 0, 0, -1, Z } }, { { -1, 0, 1, Y }, { 0, 0, -1, Z } }, { { 1, 0, -1, Y }, { -1, 0, 0, X } },
        { { 1, 0, 1, Y }, { -1, 0, 0, X } }, { { -1, 0, -1, Y }, { 1, 0, 0, X } },
        { { -1, 0, 1, Y }, { 1, 0, 0, X } } };

    private static final int[][] MIDDLE_LEAVES = { { 0, 0, 0 }, { 0, 0, -1 }, { 1, 0, -1 }, { 2, 0, -1 }, { 3, 0, -1 },
        { -1, 0, -1 }, { -2, 0, -1 }, { -3, 0, -1 }, { 0, 0, -2 }, { 1, 0, -2 }, { 2, 0, -2 }, { 3, 0, -2 },
        { -1, 0, -2 }, { -2, 0, -2 }, { -3, 0, -2 }, { 0, 0, -3 }, { 1, 0, -3 }, { 2, 0, -3 }, { -1, 0, -3 },
        { -2, 0, -3 }, { 0, 0, 1 }, { 1, 0, 1 }, { 2, 0, 1 }, { 3, 0, 1 }, { -1, 0, 1 }, { -2, 0, 1 }, { -3, 0, 1 },
        { 0, 0, 2 }, { 1, 0, 2 }, { 2, 0, 2 }, { 3, 0, 2 }, { -1, 0, 2 }, { -2, 0, 2 }, { -3, 0, 2 }, { 0, 0, 3 },
        { 1, 0, 3 }, { 2, 0, 3 }, { -1, 0, 3 }, { -2, 0, 3 }, { 1, 0, 0 }, { 2, 0, 0 }, { 3, 0, 0 }, { -1, 0, 0 },
        { -2, 0, 0 }, { -3, 0, 0 } };

    private static final int[][] TOP_LEFT_LEAVES = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, -2 }, { -1, 0, -1 },
        { -1, 0, -2 }, { 0, 0, 1 }, { 0, 0, 2 }, { 1, 0, 1 }, { 1, 0, 2 }, { 1, 0, 0 }, { 2, 0, 0 }, { 1, 0, -1 },
        { 2, 0, -1 }, { -1, 0, 0 }, { -2, 0, 0 }, { -1, 0, 1 }, { -2, 0, 1 } };

    private static final int[][] TOP_RIGHT_LEAVES = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, -2 }, { 1, 0, -1 },
        { 1, 0, -2 }, { 0, 0, 1 }, { 0, 0, 2 }, { -1, 0, 1 }, { -1, 0, 2 }, { 1, 0, 0 }, { 2, 0, 0 }, { 1, 0, 1 },
        { 2, 0, 1 }, { -1, 0, 0 }, { -2, 0, 0 }, { -1, 0, -1 }, { -2, 0, -1 } };

    private final ModernFeatureEngine features;

    ModernRuSakuraFeature(ModernFeatureEngine features) {
        this.features = features;
    }

    boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
        int limbDirection = random.nextInt(12);
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        for (int y = 0; y <= height; y++) {
            Pos check = origin.offset(0, y, 0);
            if (!replaceable(engine, world, check)) return false;
            if (y == height && !limbsReplaceable(engine, world, check, limbDirection)) return false;
        }

        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            if (y == 0) placeBase(engine, config, world, random, position);
            if (y == height) placeLimbs(engine, config, world, random, position, limbDirection);
            placeLog(engine, config, world, random, position, Y);
        }
        return true;
    }

    private boolean limbsReplaceable(ModernDecorationEngine engine, World world, Pos top, int direction) {
        for (int[] block : LIMBS[direction]) {
            if (!replaceable(engine, world, top.offset(block[0], block[1], block[2]))) return false;
        }
        return true;
    }

    private void placeBase(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int[][] base = BASES[RuIncidentalRandom
            .sample(world.worldSeed(), origin.x, origin.y, origin.z, RuIncidentalRandom.BASE, BASES.length)];
        for (int[] block : base) {
            Pos position = origin.offset(block[0], block[1], block[2]);
            placeLog(engine, config, world, random, position, block[3]);
            placeRoot(engine, config, world, random, position);
        }
    }

    private void placeLimbs(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos top,
        int direction) {
        int[][] limbs = LIMBS[direction];
        for (int[] block : limbs) {
            placeLog(engine, config, world, random, top.offset(block[0], block[1], block[2]), block[3]);
        }
        leafBlob(engine, config, world, random, top.offset(limbs[4][0], limbs[4][1], limbs[4][2]), false);
        int[] bee = BEES[direction];
        placeBeehive(world, top.offset(bee[0], bee[1], bee[2]), bee[3]);
        leafBlob(engine, config, world, random, top.offset(limbs[7][0], limbs[7][1], limbs[7][2]), true);
    }

    private void placeRoot(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos base) {
        Pos position = base.offset(0, -1, 0);
        for (int depth = 0; depth <= 2; depth++, position = position.offset(0, -1, 0)) {
            String current = world.blockState(position.x, position.y, position.z);
            String above = world.blockState(position.x, position.y + 1, position.z);
            if (world.isReplaceable(position.x, position.y, position.z)
                && engine.isBlockInTag("minecraft:dirt", above)) {
                world.setBlock(position.x, position.y, position.z, "minecraft:hanging_roots[waterlogged=false]");
                break;
            }
            if (engine.isBlockInTag("minecraft:dirt", current)
                || engine.isBlockInTag("minecraft:replaceable_by_trees", current)
                || "minecraft:air".equals(ModernDecorationEngine.blockName(current))) {
                placeLog(engine, config, world, random, position, Y);
            } else {
                break;
            }
        }
    }

    private void leafBlob(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, boolean right) {
        placeLeaves(engine, config, world, random, center, right ? TOP_RIGHT_LEAVES : TOP_LEFT_LEAVES);
        placeLeaves(engine, config, world, random, center.offset(0, 1, 0), MIDDLE_LEAVES);
        placeLeaves(engine, config, world, random, center.offset(0, 2, 0), right ? TOP_RIGHT_LEAVES : TOP_LEFT_LEAVES);
    }

    private void placeLeaves(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, int[][] offsets) {
        for (int[] offset : offsets) {
            Pos position = center.offset(offset[0], offset[1], offset[2]);
            if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()
                || !world.isReplaceable(position.x, position.y, position.z)) continue;
            String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
            world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "distance", "1"));
        }
    }

    private void placeLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, int axis) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        String current = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = grassReplacement(current);
        if (replacement != null) {
            world.setBlock(position.x, position.y, position.z, replacement);
        } else if (replaceable(engine, world, position)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
            world.setBlock(
                position.x,
                position.y,
                position.z,
                ModernFeatureEngine.property(state, "axis", axis == X ? "x" : axis == Z ? "z" : "y"));
        } else {
            return;
        }

        Pos below = position.offset(0, -1, 0);
        String belowBlock = ModernDecorationEngine.blockName(world.blockState(below.x, below.y, below.z));
        String belowReplacement = grassReplacement(belowBlock);
        if (belowReplacement != null) {
            world.setBlock(below.x, below.y, below.z, belowReplacement);
        }
    }

    private static void placeBeehive(World world, Pos position, int facing) {
        if (!"minecraft:air"
            .equals(ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z)))
            || RuIncidentalRandom
                .sample(world.worldSeed(), position.x, position.y, position.z, RuIncidentalRandom.BEEHIVE, 20) != 0)
            return;
        String[] directions = { "north", "south", "east", "west" };
        world.setBlock(
            position.x,
            position.y,
            position.z,
            "minecraft:bee_nest[facing=" + directions[facing] + ",honey_level=0]");
    }

    private static boolean replaceable(ModernDecorationEngine engine, World world, Pos position) {
        return position.y >= world.minBuildHeight() && position.y < world.maxBuildHeight()
            && engine.isBlockInTag(
                "regions_unexplored:replaceable_blocks",
                world.blockState(position.x, position.y, position.z));
    }

    private static String grassReplacement(String block) {
        if ("regions_unexplored:peat_grass_block".equals(block)) return "regions_unexplored:peat_dirt";
        if ("regions_unexplored:silt_grass_block".equals(block)) return "regions_unexplored:silt_dirt";
        if ("regions_unexplored:alpha_grass_block".equals(block) || "minecraft:grass_block".equals(block))
            return "minecraft:dirt";
        return null;
    }
}
