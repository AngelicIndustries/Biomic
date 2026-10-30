package industries.angelic.biomic.world.modern;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Direct port of pinned RU's AspenTreeFeature shared by birch, silver birch and flowering oak. */
final class ModernRuAspenFeature {

    private final ModernFeatureEngine features;
    private final ModernTreeFeature treeFeatures;

    ModernRuAspenFeature(ModernFeatureEngine features, ModernTreeFeature treeFeatures) {
        this.features = features;
        this.treeFeatures = treeFeatures;
    }

    boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        for (int y = 0; y <= height; y++) {
            if (!replaceable(engine, world, origin.offset(0, y, 0))) return false;
        }
        Set<Pos> logs = new HashSet<Pos>();
        Set<Pos> leaves = new HashSet<Pos>();
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            placeLog(engine, config, world, random, logs, position, "y");
            if (y == 0) placeRoot(engine, config, world, random, logs, position);
            if (y == height - 2) placeStem(engine, config, world, random, logs, leaves, position);
            if (y == height) placeBranches(engine, config, world, random, logs, leaves, position);
        }
        treeFeatures.placeCustomDecorators(engine, config, world, random, logs, leaves);
        return true;
    }

    private void placeStem(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Set<Pos> logs, Set<Pos> leaves, Pos center) {
        random.nextInt(3); // source computes move = result - 3 but never reads it
        int direction = random.nextInt(4);
        int[][] offsets = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        String[] facings = { "north", "south", "east", "west" };
        String[] axes = { "z", "z", "x", "x" };
        int dx = offsets[direction][0];
        int dz = offsets[direction][1];
        Pos branch = center.offset(dx, 0, dz);
        if (!replaceable(engine, world, branch)) return;
        if (random.nextInt(3) != 0) {
            placeLog(engine, config, world, random, logs, branch, axes[direction]);
        } else {
            placeBranch(engine, config, world, random, branch, center, facings[direction]);
            if (random.nextInt(2) == 0) {
                placeLeaf(engine, config, world, random, leaves, branch.offset(0, 1, 0));
                placeLeaf(engine, config, world, random, leaves, branch.offset(dx, 0, dz));
                if (dx == 0) {
                    placeLeaf(engine, config, world, random, leaves, branch.offset(1, 0, 0));
                    placeLeaf(engine, config, world, random, leaves, branch.offset(-1, 0, 0));
                } else {
                    placeLeaf(engine, config, world, random, leaves, branch.offset(0, 0, -1));
                    placeLeaf(engine, config, world, random, leaves, branch.offset(0, 0, 1));
                }
            }
        }
    }

    private void placeBranches(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Set<Pos> logs, Set<Pos> leaves, Pos top) {
        int type = random.nextInt(3);
        int direction = random.nextInt(4);
        int[][] offsets = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        String axis = direction < 2 ? "z" : "x";
        int dx = offsets[direction][0];
        int dz = offsets[direction][1];
        if (type == 0) {
            int shortSide = random.nextInt(2);
            Pos opposite = top.offset(-dx, 0, -dz);
            placeLog(engine, config, world, random, logs, opposite, axis);
            leafBlob(engine, config, world, random, leaves, opposite);
            Pos longSide = top.offset(dx, 1, dz);
            placeLog(engine, config, world, random, logs, longSide, "y");
            longSide = longSide.offset(0, 1, 0);
            placeLog(engine, config, world, random, logs, longSide, "y");
            if (shortSide == 0) {
                longSide = longSide.offset(0, 1, 0);
                placeLog(engine, config, world, random, logs, longSide, "y");
            }
            leafBlob(engine, config, world, random, leaves, longSide);
        } else if (type == 1) {
            Pos longSide = top.offset(dx, 1, dz);
            placeLog(engine, config, world, random, logs, longSide, "y");
            longSide = longSide.offset(0, 1, 0);
            placeLog(engine, config, world, random, logs, longSide, "y");
            leafBlob(engine, config, world, random, leaves, longSide);
        } else {
            leafBlob(engine, config, world, random, leaves, top);
        }
    }

    private void leafBlob(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Set<Pos> leaves, Pos center) {
        int[][] offsets = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, -2 }, { 0, 0, 1 }, { 0, 0, 2 }, { 1, 0, 0 }, { 2, 0, 0 },
            { -1, 0, 0 }, { -2, 0, 0 }, { 1, 0, -1 }, { -1, 0, -1 }, { 1, 0, 1 }, { -1, 0, 1 }, { 0, 1, 0 },
            { 0, 1, -1 }, { 0, 1, 1 }, { 1, 1, 0 }, { -1, 1, 0 }, { 1, 1, -1 }, { -1, 1, -1 }, { 1, 1, 1 },
            { -1, 1, 1 }, { 0, 2, 0 }, { 0, 2, -1 }, { 0, 2, 1 }, { 1, 2, 0 }, { -1, 2, 0 }, { 0, 3, 0 } };
        for (int[] offset : offsets) {
            placeLeaf(engine, config, world, random, leaves, center.offset(offset[0], offset[1], offset[2]));
        }
        Random incidental = RuIncidentalRandom
            .random(world.worldSeed(), center.x, center.y, center.z, RuIncidentalRandom.LEAF_BLOB);
        int[][] optional = { { 0, 3, -1 }, { 0, 3, 1 }, { 1, 3, 0 }, { -1, 3, 0 } };
        for (int[] offset : optional) {
            if (incidental.nextInt(2) == 0) {
                placeLeaf(engine, config, world, random, leaves, center.offset(offset[0], offset[1], offset[2]));
            }
        }
    }

    private void placeRoot(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Set<Pos> logs, Pos origin) {
        int depth = RuIncidentalRandom
            .sample(world.worldSeed(), origin.x, origin.y, origin.z, RuIncidentalRandom.ROOT, 2) + 2;
        Pos pos = origin;
        for (int i = 0; i <= depth; i++, pos = pos.offset(0, -1, 0)) {
            String current = world.blockState(pos.x, pos.y, pos.z);
            if (world.isReplaceable(pos.x, pos.y, pos.z)
                && engine.isBlockInTag("minecraft:dirt", world.blockState(pos.x, pos.y + 1, pos.z))) {
                world.setBlock(pos.x, pos.y, pos.z, "minecraft:hanging_roots[waterlogged=false]");
                break;
            }
            if (engine.isBlockInTag("minecraft:dirt", current)
                || engine.isBlockInTag("minecraft:replaceable_by_trees", current)
                || "minecraft:air".equals(ModernDecorationEngine.blockName(current))) {
                placeLog(engine, config, world, random, logs, pos, "y");
            } else break;
        }
    }

    private void placeLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Set<Pos> logs, Pos position, String axis) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        String block = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = grassReplacement(block);
        if (replacement != null) {
            world.setBlock(position.x, position.y, position.z, replacement);
        } else if (replaceable(engine, world, position)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
            state = ModernFeatureEngine.property(state, "axis", axis);
            if ("regions_unexplored:silver_birch_log".equals(ModernDecorationEngine.blockName(state))) {
                state = ModernFeatureEngine.property(
                    state,
                    "is_base",
                    Boolean.toString(
                        engine
                            .isBlockInTag("minecraft:dirt", world.blockState(position.x, position.y - 1, position.z))));
            }
            world.setBlock(position.x, position.y, position.z, state);
            logs.add(position);
        } else {
            return;
        }
        String below = ModernDecorationEngine.blockName(world.blockState(position.x, position.y - 1, position.z));
        String belowReplacement = grassReplacement(below);
        if (belowReplacement != null) {
            world.setBlock(position.x, position.y - 1, position.z, belowReplacement);
        }
    }

    private void placeBranch(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, Pos providerPosition, String facing) {
        if (!world.isReplaceable(position.x, position.y, position.z)) return;
        String state = features
            .stateProvider(engine, config.getAsJsonObject("branch_provider"), random, providerPosition);
        world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "facing", facing));
    }

    private void placeLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Set<Pos> leaves, Pos position) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()
            || !world.isReplaceable(position.x, position.y, position.z)) return;
        String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
        world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "distance", "1"));
        leaves.add(position);
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
