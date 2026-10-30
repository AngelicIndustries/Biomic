package industries.angelic.biomic.world.modern;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Direct port of pinned RU's CypressTreeFeature. */
final class ModernRuCypressFeature {

    private final ModernFeatureEngine features;

    ModernRuCypressFeature(ModernFeatureEngine features) {
        this.features = features;
    }

    boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
        boolean vines = random.nextInt(3) == 0;
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
            if (y == 0) placeBase(engine, config, world, random, position);
            if (y == height) placeTop(engine, config, world, random, position, vines);
        }
        return true;
    }

    private void placeBase(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int[][] directions = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        for (int[] direction : directions) {
            Pos side = origin.offset(direction[0], 0, direction[1]);
            placeRoot(engine, config, world, random, side.offset(0, -1, 0));
            placeLog(engine, config, world, random, side, "y");
            if (random.nextInt(2) == 0) {
                placeLog(engine, config, world, random, side.offset(0, 1, 0), "y");
                if (random.nextInt(2) == 0) {
                    placeLog(engine, config, world, random, side.offset(0, 2, 0), "y");
                }
            }
        }
        placeRoot(engine, config, world, random, origin);
    }

    private void placeTop(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos top,
        boolean vines) {
        placeLeavesBlob(engine, config, world, random, top, vines);
        random.nextInt(2); // pinned source computes an unused branch-layout type
        int[] positions = { random.nextInt(2) + 3, random.nextInt(2) + 6, random.nextInt(2) + 9,
            random.nextInt(2) + 12 };
        for (int i = 0; i < positions.length; i++) {
            int swap = random.nextInt(positions.length);
            int value = positions[swap];
            positions[swap] = positions[i];
            positions[i] = value;
        }
        int[][] directions = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        String[] axes = { "z", "z", "x", "x" };
        for (int i = 0; i < directions.length; i++) {
            int dx = directions[i][0];
            int dz = directions[i][1];
            Pos base = top.offset(0, -positions[i], 0);
            Pos first = base.offset(dx, 0, dz);
            Pos second = base.offset(dx * 2, 0, dz * 2);
            placeLog(engine, config, world, random, first, axes[i]);
            placeLog(engine, config, world, random, second, axes[i]);
            placeLeavesBlob(engine, config, world, random, second, vines);
        }
    }

    private void placeLeavesBlob(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, boolean vines) {
        int[][] upper = { { 0, 1, 0 }, { 0, 1, -1 }, { 0, 1, 1 }, { 1, 1, 0 }, { -1, 1, 0 } };
        placeLeafOffsets(engine, config, world, random, center, upper);
        int[][] firstHalf = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, -2 }, { 0, 0, 1 }, { 0, 0, 2 }, { 1, 0, 0 },
            { 2, 0, 0 }, { -1, 0, 0 }, { -2, 0, 0 }, { 1, 0, -1 }, { 2, 0, -1 }, { 1, 0, -2 } };
        placeLeafOffsets(engine, config, world, random, center, firstHalf);
        if (vines) {
            int[][] north = { { 0, -2 }, { 1, -2 }, { -1, -2 } };
            int[][] south = { { 0, 2 }, { 1, 2 }, { -1, 2 } };
            int[][] east = { { 2, 0 }, { 2, -1 }, { 2, 1 } };
            int[][] west = { { -2, 0 }, { -2, -1 }, { -2, 1 } };
            placeVines(world, random, center, north);
            placeVines(world, random, center, south);
            placeVines(world, random, center, east);
            placeVines(world, random, center, west);
        }
        int[][] secondHalf = { { -1, 0, -1 }, { -2, 0, -1 }, { -1, 0, -2 }, { 1, 0, 1 }, { 2, 0, 1 }, { 1, 0, 2 },
            { -1, 0, 1 }, { -2, 0, 1 }, { -1, 0, 2 } };
        placeLeafOffsets(engine, config, world, random, center, secondHalf);
    }

    private void placeVines(World world, ModernRandom random, Pos center, int[][] offsets) {
        for (int[] offset : offsets) {
            if (random.nextInt(2) == 0) {
                placeVine(world, center.offset(offset[0], 0, offset[1]));
            }
        }
    }

    private void placeVine(World world, Pos anchor) {
        int size = RuIncidentalRandom
            .sample(world.worldSeed(), anchor.x, anchor.y, anchor.z, RuIncidentalRandom.VINE, 6);
        Pos position = anchor.offset(0, -1, 0);
        for (int i = 0; i <= size; i++) {
            if (isAir(world, position)) {
                String state = i == size ? "regions_unexplored:spanish_moss[age=25]"
                    : "regions_unexplored:spanish_moss_plant";
                world.setBlock(position.x, position.y, position.z, state);
                position = position.offset(0, -1, 0);
            } else {
                if (i != 0) {
                    Pos terminal = position.offset(0, 1, 0);
                    world.setBlock(terminal.x, terminal.y, terminal.z, "regions_unexplored:spanish_moss[age=25]");
                }
                break;
            }
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
            state = ModernFeatureEngine.tryProperty(state, "axis", axes[direction]);
            state = ModernFeatureEngine.tryProperty(state, "facing", facings[direction]);
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

    private void placeRoot(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int depth = RuIncidentalRandom
            .sample(world.worldSeed(), origin.x, origin.y, origin.z, RuIncidentalRandom.ROOT, 2) + 2;
        Pos position = origin;
        for (int step = 0; step <= depth; step++, position = position.offset(0, -1, 0)) {
            String state = world.blockState(position.x, position.y, position.z);
            String above = world.blockState(position.x, position.y + 1, position.z);
            if (world.isReplaceable(position.x, position.y, position.z)
                && engine.isBlockInTag("minecraft:dirt", above)) {
                world.setBlock(position.x, position.y, position.z, "minecraft:hanging_roots[waterlogged=false]");
                break;
            }
            if (engine.isBlockInTag("minecraft:dirt", state)
                || engine.isBlockInTag("minecraft:replaceable_by_trees", state)
                || isAir(world, position)) {
                placeLog(engine, config, world, random, position, "y");
            } else {
                break;
            }
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

    private static boolean isAir(World world, Pos position) {
        return "minecraft:air"
            .equals(ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z)));
    }

    private static boolean inside(World world, Pos position) {
        return position.y >= world.minBuildHeight() && position.y < world.maxBuildHeight();
    }
}
