package industries.angelic.biomic.world.modern;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Direct port of pinned RU's six live/dead stripped-pine configurations. */
final class ModernRuStrippedPineFeature {

    private static final int[][] X_SMALL = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 } };
    private static final int[][] SMALL = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 },
        { 1, 0, -1 }, { -1, 0, -1 }, { 1, 0, 1 }, { -1, 0, 1 } };
    private static final int[][] MEDIUM = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 },
        { 1, 0, -1 }, { -1, 0, -1 }, { 1, 0, 1 }, { -1, 0, 1 }, { 0, 0, -2 }, { 0, 0, 2 }, { 2, 0, 0 }, { -2, 0, 0 } };
    private static final int[][] BIG = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 },
        { 1, 0, -1 }, { -1, 0, -1 }, { 1, 0, 1 }, { -1, 0, 1 }, { 0, 0, -2 }, { 0, 0, 2 }, { 2, 0, 0 }, { -2, 0, 0 },
        { 1, 0, -2 }, { -1, 0, -2 }, { 1, 0, 2 }, { -1, 0, 2 }, { 2, 0, -1 }, { 2, 0, 1 }, { -2, 0, -1 },
        { -2, 0, 1 } };
    private static final int[][] X_BIG = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 },
        { 1, 0, -1 }, { -1, 0, -1 }, { 1, 0, 1 }, { -1, 0, 1 }, { 0, 0, -2 }, { 0, 0, 2 }, { 2, 0, 0 }, { -2, 0, 0 },
        { 1, 0, -2 }, { -1, 0, -2 }, { 1, 0, 2 }, { -1, 0, 2 }, { 2, 0, -1 }, { 2, 0, 1 }, { -2, 0, -1 }, { -2, 0, 1 },
        { 2, 0, -2 }, { -2, 0, -2 }, { 2, 0, 2 }, { -2, 0, 2 }, { 1, 0, -3 }, { -1, 0, -3 }, { 1, 0, 3 }, { -1, 0, 3 },
        { 3, 0, -1 }, { 3, 0, 1 }, { -3, 0, -1 }, { -3, 0, 1 }, { 0, 0, -3 }, { 0, 0, 3 }, { 3, 0, 0 }, { -3, 0, 0 } };
    private static final int[][] BLOB_CHECK = BIG;

    private final ModernFeatureEngine features;

    ModernRuStrippedPineFeature(ModernFeatureEngine features) {
        this.features = features;
    }

    boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            if (y == height / 2) {
                for (int[] offset : BLOB_CHECK) {
                    if (!replaceable(engine, world, position.offset(offset[0], offset[1], offset[2]))) return false;
                }
            }
            if (!replaceable(engine, world, position)) return false;
        }

        int strippedDivisor = random.nextInt(2) + 4;
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            if (y < height / strippedDivisor) {
                placeLog(engine, config, world, random, position, "y", true, false);
            } else if (y == height / strippedDivisor) {
                placeLog(engine, config, world, random, position, "y", false, true);
            } else {
                placeLog(engine, config, world, random, position, "y", false, false);
                placeBranchDecorator(engine, config, world, random, position);
            }
            if (y == 0) placeRoot(engine, config, world, random, position);
            if (y == height) {
                if (random.nextInt(11) == 0) {
                    placeTop3(engine, config, world, random, position);
                } else if (random.nextInt(11) <= 5) {
                    placeTop2(engine, config, world, random, position);
                } else {
                    placeTop1(engine, config, world, random, position);
                }
            }
        }
        return true;
    }

    private void placeTop1(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos top) {
        Pos position = top.offset(0, -random.nextInt(2), 0);
        blobTop(engine, config, world, random, position);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, X_SMALL);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, SMALL);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, MEDIUM);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, X_SMALL);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, BIG);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, MEDIUM);
        if (random.nextInt(3) == 0) blob(engine, config, world, random, position, X_BIG);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, X_SMALL);
        optionalCorners(engine, config, world, random, position);
    }

    private void placeTop2(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos top) {
        Pos position = top.offset(0, -random.nextInt(2), 0);
        blobTop(engine, config, world, random, position);
        blob(engine, config, world, random, position, X_SMALL);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, SMALL);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, MEDIUM);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, X_SMALL);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, MEDIUM);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, BIG);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, SMALL);
        if (random.nextInt(3) == 0) placeLeaf(engine, config, world, random, position.offset(0, 0, -2));
        if (random.nextInt(3) == 0) placeLeaf(engine, config, world, random, position.offset(0, 0, -2));
        if (random.nextInt(3) == 0) placeLeaf(engine, config, world, random, position.offset(0, 0, 2));
        if (random.nextInt(3) == 0) placeLeaf(engine, config, world, random, position.offset(0, 0, 2));
        if (random.nextInt(3) == 0) blob(engine, config, world, random, position, X_BIG);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, X_SMALL);
    }

    private void placeTop3(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos top) {
        Pos position = top.offset(0, -random.nextInt(2), 0);
        blobTop(engine, config, world, random, position);
        blob(engine, config, world, random, position, X_SMALL);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, X_SMALL);
        if (random.nextInt(3) == 0) {
            position = position.offset(0, -1, 0);
            blob(engine, config, world, random, position, X_SMALL);
        }
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, X_SMALL);
        optionalCorners(engine, config, world, random, position);
        position = position.offset(0, -1, 0);
        blob(engine, config, world, random, position, X_SMALL);
        position = position.offset(0, -2, 0);
        if (random.nextInt(2) == 0) position = position.offset(0, -1, 0);
        randomLogDecoration(engine, config, world, random, position);
    }

    private void blobTop(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        int[][] offsets = { { 0, 1, 0 }, { 0, 2, 0 }, { 0, 3, 0 }, { 0, 1, -1 }, { 0, 1, 1 }, { 1, 1, 0 }, { -1, 1, 0 },
            { -1, 1, 0 } };
        blob(engine, config, world, random, position, offsets);
    }

    private void optionalCorners(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        int[][] corners = { { 1, 0, -1 }, { -1, 0, -1 }, { 1, 0, 1 }, { -1, 0, 1 } };
        for (int[] corner : corners) {
            if (random.nextInt(3) == 0) {
                placeLeaf(engine, config, world, random, position.offset(corner[0], corner[1], corner[2]));
            }
        }
    }

    private void randomLogDecoration(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        int chance = random.nextInt(10);
        if (chance < 4) {
            int[][] directions = { { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 } };
            int[] direction = directions[chance];
            placeLog(
                engine,
                config,
                world,
                random,
                position.offset(direction[0], direction[1], direction[2]),
                chance < 2 ? "z" : "x",
                false,
                false);
        }
    }

    private void placeBranchDecorator(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, Pos trunk) {
        if (random.nextInt(10) != 0) return;
        int direction = RuIncidentalRandom
            .sample(world.worldSeed(), trunk.x, trunk.y, trunk.z, RuIncidentalRandom.BRANCH, 4);
        int[][] directions = { { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 } };
        String[] facings = { "north", "south", "east", "west" };
        int dx = directions[direction][0];
        int dz = directions[direction][2];
        Pos branch = trunk.offset(dx, 0, dz);
        if (replaceableByState(world, branch)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("branch_provider"), random, trunk);
            world.setBlock(
                branch.x,
                branch.y,
                branch.z,
                ModernFeatureEngine.property(state, "facing", facings[direction]));
        }
        placeLeaf(engine, config, world, random, trunk.offset(dx, 1, dz));
        placeLeaf(engine, config, world, random, trunk.offset(dx * 2, 0, dz * 2));
        if (dx == 0) {
            placeLeaf(engine, config, world, random, trunk.offset(1, 0, dz));
            placeLeaf(engine, config, world, random, trunk.offset(-1, 0, dz));
        } else {
            placeLeaf(engine, config, world, random, trunk.offset(dx, 0, 1));
            placeLeaf(engine, config, world, random, trunk.offset(dx, 0, -1));
        }
    }

    private void placeRoot(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int depth = RuIncidentalRandom
            .sample(world.worldSeed(), origin.x, origin.y, origin.z, RuIncidentalRandom.ROOT, 2) + 4;
        Pos position = origin;
        for (int i = 0; i <= depth; i++, position = position.offset(0, -1, 0)) {
            String current = world.blockState(position.x, position.y, position.z);
            if (world.isReplaceable(position.x, position.y, position.z)
                && engine.isBlockInTag("minecraft:dirt", world.blockState(position.x, position.y + 1, position.z))) {
                world.setBlock(position.x, position.y, position.z, "minecraft:hanging_roots[waterlogged=false]");
                break;
            }
            if (engine.isBlockInTag("minecraft:dirt", current)
                || engine.isBlockInTag("minecraft:replaceable_by_trees", current)
                || "minecraft:air".equals(ModernDecorationEngine.blockName(current))) {
                placeLog(engine, config, world, random, position, "y", true, false);
            } else {
                break;
            }
        }
    }

    private void blob(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos center,
        int[][] offsets) {
        for (int[] offset : offsets) {
            placeLeaf(engine, config, world, random, center.offset(offset[0], offset[1], offset[2]));
        }
    }

    private void placeLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()
            || !replaceableByState(world, position)) return;
        String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
        world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "distance", "1"));
    }

    private void placeLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, String axis, boolean stripped, boolean transition) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        String current = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = grassReplacement(current);
        if (replacement != null) {
            world.setBlock(position.x, position.y, position.z, replacement);
        } else if (replaceable(engine, world, position)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
            state = ModernFeatureEngine.property(state, "axis", axis);
            if ("regions_unexplored:pine_log".equals(ModernDecorationEngine.blockName(state))) {
                state = ModernFeatureEngine.property(state, "is_stripped", Boolean.toString(stripped));
                state = ModernFeatureEngine.property(state, "transition_block", Boolean.toString(transition));
            }
            world.setBlock(position.x, position.y, position.z, state);
        } else {
            return;
        }
        Pos below = position.offset(0, -1, 0);
        String belowReplacement = grassReplacement(
            ModernDecorationEngine.blockName(world.blockState(below.x, below.y, below.z)));
        if (belowReplacement != null) {
            world.setBlock(below.x, below.y, below.z, belowReplacement);
        }
    }

    private static boolean replaceable(ModernDecorationEngine engine, World world, Pos position) {
        return position.y >= world.minBuildHeight() && position.y < world.maxBuildHeight()
            && engine.isBlockInTag(
                "regions_unexplored:replaceable_blocks",
                world.blockState(position.x, position.y, position.z));
    }

    private static boolean replaceableByState(World world, Pos position) {
        return position.y >= world.minBuildHeight() && position.y < world.maxBuildHeight()
            && world.isReplaceable(position.x, position.y, position.z);
    }

    private static String grassReplacement(String block) {
        if ("regions_unexplored:peat_grass_block".equals(block)) return "regions_unexplored:peat_dirt";
        if ("regions_unexplored:silt_grass_block".equals(block)) return "regions_unexplored:silt_dirt";
        if ("regions_unexplored:alpha_grass_block".equals(block) || "minecraft:grass_block".equals(block))
            return "minecraft:dirt";
        return null;
    }
}
