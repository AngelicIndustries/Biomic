package industries.angelic.biomic.world.modern;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Direct port of the custom big-blackwood feature in pinned RU. */
final class ModernRuBlackwoodFeature {

    private final ModernFeatureEngine features;

    ModernRuBlackwoodFeature(ModernFeatureEngine features) {
        this.features = features;
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
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            placeLog(engine, config, world, random, position, "y");
            if (y > 1 && random.nextInt(10) == 0) {
                placeBranch(
                    engine,
                    config,
                    world,
                    random,
                    position,
                    RuIncidentalRandom
                        .sample(world.worldSeed(), position.x, position.y, position.z, RuIncidentalRandom.BRANCH, 4));
            }
            if (y == 0) placeRoot(engine, config, world, random, position);
            if (y == height) placeTop(engine, config, world, random, position);
        }
        return true;
    }

    private void placeTop(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos top) {
        placeTopBlob(engine, config, world, random, top);
        placeShortBranches(engine, config, world, random, top.offset(0, -6, 0));
        placeLongBranches(engine, config, world, random, top.offset(0, -9, 0), false);
        placeLongBranches(engine, config, world, random, top.offset(0, -12, 0), true);
        if (random.nextInt(3) == 0) {
            placeLongBranches(engine, config, world, random, top.offset(0, -15, 0), true);
        }
    }

    private void placeTopBlob(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos top) {
        if (RuIncidentalRandom.sample(world.worldSeed(), top.x, top.y, top.z, RuIncidentalRandom.TOP, 2) == 0) {
            int[][] offsets = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 },
                { 0, 2, 0 }, { 0, -2, -1 }, { 0, -2, 1 }, { 1, -2, 0 }, { -1, -2, 0 } };
            placeLeafOffsets(engine, config, world, random, top, offsets);
        } else {
            int[][] offsets = { { 0, 1, 0 }, { 0, 1, -1 }, { 0, 1, 1 }, { 1, 1, 0 }, { -1, 1, 0 }, { 0, 2, 0 },
                { 0, 3, 0 }, { 0, -1, -1 }, { 0, -1, 1 }, { 1, -1, 0 }, { -1, -1, 0 }, { 0, -2, -1 }, { 0, -2, 1 },
                { 1, -2, 0 }, { -1, -2, 0 } };
            placeLeafOffsets(engine, config, world, random, top, offsets);
        }
        Pos lower = top.offset(0, -3, 0);
        placeLeaf(engine, config, world, random, lower);
        int[][] directions = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        String[] axes = { "z", "z", "x", "x" };
        for (int i = 0; i < directions.length; i++) {
            placeLog(engine, config, world, random, lower.offset(directions[i][0], 0, directions[i][1]), axes[i]);
        }
        int[][] lowerLeaves = { { 1, 0, -1 }, { -1, 0, -1 }, { 1, 0, 1 }, { -1, 0, 1 }, { 0, 0, -2 }, { 1, 0, -2 },
            { -1, 0, -2 }, { 0, 0, 2 }, { 1, 0, 2 }, { -1, 0, 2 }, { 2, 0, 0 }, { 2, 0, -1 }, { 2, 0, 1 }, { -2, 0, 0 },
            { -2, 0, -1 }, { -2, 0, 1 } };
        placeLeafOffsets(engine, config, world, random, lower, lowerLeaves);
    }

    private void placeShortBranches(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        random.nextInt(2); // pinned source computes an unused layout type
        for (int direction = 0; direction < 4; direction++) {
            int[][] dirs = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
            String axis = direction < 2 ? "z" : "x";
            int dx = dirs[direction][0];
            int dz = dirs[direction][1];
            Pos end = center.offset(dx, 0, dz);
            placeLog(engine, config, world, random, end, axis);
            placeDirectionalBlob(engine, config, world, random, end, dx, dz);
        }
    }

    private void placeLongBranches(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, boolean extraLong) {
        random.nextInt(2); // pinned source computes an unused layout type
        int[][] preLeaves = { { 0, 0, -1 }, { 1, 0, -1 }, { -1, 0, -1 }, { 0, 0, 1 }, { 1, 0, 1 }, { -1, 0, 1 },
            { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, -1 }, { 1, 1, -1 }, { -1, 1, -1 }, { 0, 1, 1 }, { 1, 1, 1 },
            { -1, 1, 1 }, { 1, 1, 0 }, { -1, 1, 0 }, { 0, 2, -1 }, { 1, 2, -1 }, { -1, 2, -1 }, { 0, 2, 1 },
            { 1, 2, 1 }, { -1, 2, 1 }, { 1, 2, 0 }, { -1, 2, 0 } };
        placeLeafOffsets(engine, config, world, random, center, preLeaves);
        int[][] dirs = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        for (int direction = 0; direction < 4; direction++) {
            int dx = dirs[direction][0];
            int dz = dirs[direction][1];
            String axis = direction < 2 ? "z" : "x";
            if (extraLong) {
                placeLog(engine, config, world, random, center.offset(dx, -1, dz), axis);
            }
            int length = extraLong ? 3 : 2;
            for (int distance = 1; distance <= length; distance++) {
                placeLog(engine, config, world, random, center.offset(dx * distance, 0, dz * distance), axis);
            }
            Pos end = center.offset(dx * length, 0, dz * length);
            placeDirectionalBlob(engine, config, world, random, end, dx, dz);
        }
    }

    private void placeDirectionalBlob(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, Pos center, int dx, int dz) {
        int px = -dz;
        int pz = dx;
        int[][] coordinates = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -2 }, { 0, -1 }, { 0, 0 }, { 0, 1 }, { 0, 2 },
            { 1, -2 }, { 1, -1 }, { 1, 0 }, { 1, 1 }, { 1, 2 }, { 2, -1 }, { 2, 0 }, { 2, 1 } };
        for (int[] coordinate : coordinates) {
            placeLeaf(
                engine,
                config,
                world,
                random,
                center.offset(dx * coordinate[0] + px * coordinate[1], 0, dz * coordinate[0] + pz * coordinate[1]));
        }
        placeLeaf(engine, config, world, random, center.offset(0, 1, 0));
        placeLeaf(engine, config, world, random, center.offset(0, 2, 0));
        placeLeaf(engine, config, world, random, center.offset(px, 1, pz));
        placeLeaf(engine, config, world, random, center.offset(-px, 1, -pz));
        for (int perpendicular = -1; perpendicular <= 1; perpendicular++) {
            placeLeaf(
                engine,
                config,
                world,
                random,
                center.offset(dx + px * perpendicular, 1, dz + pz * perpendicular));
        }
    }

    private void placeBranch(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, int direction) {
        int[][] offsets = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        String[] facings = { "north", "south", "east", "west" };
        String[] axes = { "z", "z", "x", "x" };
        int dx = offsets[direction][0];
        int dz = offsets[direction][1];
        Pos branch = center.offset(dx, 0, dz);
        if (inside(world, branch) && world.isReplaceable(branch.x, branch.y, branch.z)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("branch_provider"), random, center);
            // Source uses trySetValue: RU branch blocks have facing, not RotatedPillarBlock's axis.
            if (!ModernDecorationEngine.blockName(state)
                .endsWith("_branch")) {
                state = ModernFeatureEngine.property(state, "axis", axes[direction]);
            }
            state = ModernFeatureEngine.property(state, "facing", facings[direction]);
            world.setBlock(branch.x, branch.y, branch.z, state);
        }
        placeLeaf(engine, config, world, random, branch.offset(0, 1, 0));
        placeLeaf(engine, config, world, random, center.offset(dx * 2, 0, dz * 2));
        if (dx == 0) {
            placeLeaf(engine, config, world, random, center.offset(1, 0, dz));
            placeLeaf(engine, config, world, random, center.offset(-1, 0, dz));
        } else {
            placeLeaf(engine, config, world, random, center.offset(dx, 0, 1));
            placeLeaf(engine, config, world, random, center.offset(dx, 0, -1));
        }
    }

    private void placeLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, String axis) {
        if (!inside(world, position)) return;
        String current = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = substrateReplacement(current);
        if (replacement != null) {
            world.setBlock(position.x, position.y, position.z, replacement);
        } else if (replaceable(engine, world, position)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
            world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "axis", axis));
        } else {
            return;
        }
        Pos below = position.offset(0, -1, 0);
        String belowReplacement = substrateReplacement(
            ModernDecorationEngine.blockName(world.blockState(below.x, below.y, below.z)));
        if (belowReplacement != null) {
            world.setBlock(below.x, below.y, below.z, belowReplacement);
        }
    }

    private void placeRoot(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int depth = RuIncidentalRandom
            .sample(world.worldSeed(), origin.x, origin.y, origin.z, RuIncidentalRandom.ROOT, 2) + 2;
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
                placeLog(engine, config, world, random, position, "y");
            } else break;
        }
    }

    private void placeLeafOffsets(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, int[][] offsets) {
        for (int[] offset : offsets) {
            placeLeaf(engine, config, world, random, center.offset(offset[0], offset[1], offset[2]));
        }
    }

    private void placeLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (!inside(world, position) || !world.isReplaceable(position.x, position.y, position.z)) {
            return;
        }
        String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
        world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "distance", "1"));
    }

    private static String substrateReplacement(String block) {
        if ("regions_unexplored:peat_grass_block".equals(block)) return "regions_unexplored:peat_dirt";
        if ("regions_unexplored:silt_grass_block".equals(block)) return "regions_unexplored:silt_dirt";
        if ("regions_unexplored:alpha_grass_block".equals(block) || "minecraft:grass_block".equals(block))
            return "minecraft:dirt";
        return null;
    }

    private static boolean replaceable(ModernDecorationEngine engine, World world, Pos position) {
        return inside(world, position) && engine.isBlockInTag(
            "regions_unexplored:replaceable_blocks",
            world.blockState(position.x, position.y, position.z));
    }

    private static boolean inside(World world, Pos position) {
        return position.y >= world.minBuildHeight() && position.y < world.maxBuildHeight();
    }
}
