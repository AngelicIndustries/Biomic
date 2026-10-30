package industries.angelic.biomic.world.modern;

import java.util.Random;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Direct port of pinned RU's GiantCypressTreeFeature. */
final class ModernRuGiantCypressFeature {

    private final ModernFeatureEngine features;

    ModernRuGiantCypressFeature(ModernFeatureEngine features) {
        this.features = features;
    }

    boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
        boolean vines = random.nextInt(3) == 0;
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        int[] cardinalHeights = { random.nextInt(5) + 8, random.nextInt(5) + 8, random.nextInt(5) + 8,
            random.nextInt(5) + 8 };
        int[] diagonalHeights = { random.nextInt(3) + 3, random.nextInt(3) + 3, random.nextInt(3) + 3,
            random.nextInt(3) + 3 };
        int[] outerHeights = { random.nextInt(1) + 1, random.nextInt(1) + 1, random.nextInt(1) + 1,
            random.nextInt(1) + 1 };
        for (int y = 0; y <= height; y++) {
            if (!replaceable(engine, world, origin.offset(0, y, 0))) return false;
        }

        int[][] cardinals = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        int[][] diagonals = { { -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 } };
        int[][] outers = { { 0, -2 }, { 0, 2 }, { 2, 0 }, { -2, 0 } };
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            if (!treeBodyReplaceable(engine, world, position)) return true;
            placeLog(engine, config, world, random, position, "y");
            if (y == 0) placeRoot(engine, config, world, random, position.offset(0, -1, 0));
            if (y == height) placeTop(engine, config, world, random, position, vines);
            placeColumns(engine, config, world, random, position, y, cardinals, cardinalHeights);
            placeColumns(engine, config, world, random, position, y, diagonals, diagonalHeights);
            placeColumns(engine, config, world, random, position, y, outers, outerHeights);
        }
        return true;
    }

    private void placeColumns(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos level, int y, int[][] offsets, int[] heights) {
        for (int i = 0; i < offsets.length; i++) {
            if (y > heights[i]) continue;
            Pos position = level.offset(offsets[i][0], 0, offsets[i][1]);
            placeLog(engine, config, world, random, position, "y");
            if (y == 0) placeRoot(engine, config, world, random, position.offset(0, -1, 0));
            if (y == heights[i]) placeMoss(world, position);
        }
    }

    private void placeTop(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos top,
        boolean vines) {
        placeLeavesBlob(engine, config, world, random, top, vines);
        placeLowerLimbs(engine, config, world, random, top.offset(0, -16, 0), vines);
        placeUpperLimbs(engine, config, world, random, top.offset(0, -8, 0), vines);
    }

    private void placeLowerLimbs(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos base, boolean vines) {
        Random incidental = RuIncidentalRandom
            .random(world.worldSeed(), base.x, base.y, base.z, RuIncidentalRandom.CYPRESS_LOWER);
        int[] rises = { incidental.nextInt(8), incidental.nextInt(8), incidental.nextInt(8), incidental.nextInt(8) };
        for (int direction = 0; direction < 4; direction++) {
            placeLimb(engine, config, world, random, base.offset(0, rises[direction], 0), direction, vines);
        }
    }

    private void placeUpperLimbs(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos base, boolean vines) {
        Random incidental = RuIncidentalRandom
            .random(world.worldSeed(), base.x, base.y, base.z, RuIncidentalRandom.CYPRESS_UPPER);
        int axis = incidental.nextInt(2);
        int[] rises = { incidental.nextInt(6), incidental.nextInt(6), incidental.nextInt(6), incidental.nextInt(6) };
        if (axis == 0) {
            placeLimb(engine, config, world, random, base.offset(0, rises[0], 0), 0, vines);
            placeLimb(engine, config, world, random, base.offset(0, rises[1], 0), 1, vines);
        } else {
            placeLimb(engine, config, world, random, base.offset(0, rises[2], 0), 2, vines);
            placeLimb(engine, config, world, random, base.offset(0, rises[3], 0), 3, vines);
        }
    }

    private void placeLimb(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos base,
        int direction, boolean vines) {
        int[][] directions = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        String axis = direction < 2 ? "z" : "x";
        int dx = directions[direction][0];
        int dz = directions[direction][1];
        Pos first = base.offset(dx, 0, dz);
        Pos second = base.offset(dx * 2, 1, dz * 2);
        Pos third = base.offset(dx * 3, 1, dz * 3);
        Pos fourth = base.offset(dx * 4, 1, dz * 4);
        placeLog(engine, config, world, random, first, axis);
        placeLog(engine, config, world, random, second, axis);
        placeLog(engine, config, world, random, third, axis);
        placeLog(engine, config, world, random, fourth, axis);
        placeLeavesBlob(engine, config, world, random, fourth, vines);
    }

    private void placeLeavesBlob(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, boolean vines) {
        int[][] north = { { 0, 0, -1 }, { 1, 0, -1 }, { -1, 0, -1 }, { 0, 0, -2 }, { 1, 0, -2 }, { -1, 0, -2 },
            { 2, 0, -2 }, { -2, 0, -2 }, { 0, 0, -3 }, { 1, 0, -3 }, { -1, 0, -3 } };
        int[][] south = { { 0, 0, 1 }, { 1, 0, 1 }, { -1, 0, 1 }, { 0, 0, 2 }, { 1, 0, 2 }, { -1, 0, 2 }, { 2, 0, 2 },
            { -2, 0, 2 }, { 0, 0, 3 }, { 1, 0, 3 }, { -1, 0, 3 } };
        int[][] east = { { 1, 0, 0 }, { 2, 0, 0 }, { 2, 0, -1 }, { 2, 0, 1 }, { 3, 0, 0 }, { 3, 0, -1 }, { 3, 0, 1 } };
        int[][] west = { { -1, 0, 0 }, { -2, 0, 0 }, { -2, 0, -1 }, { -2, 0, 1 }, { -3, 0, 0 }, { -3, 0, -1 },
            { -3, 0, 1 } };
        placeLeaf(engine, config, world, random, center);
        placeLeafOffsets(engine, config, world, random, center, north);
        if (vines) placeVineGroup(world, random, center, new int[][] { { 0, -3 }, { 1, -3 }, { -1, -3 } });
        placeLeafOffsets(engine, config, world, random, center, south);
        if (vines) placeVineGroup(world, random, center, new int[][] { { 0, 3 }, { 1, 3 }, { -1, 3 } });
        placeLeafOffsets(engine, config, world, random, center, east);
        if (vines) placeVineGroup(world, random, center, new int[][] { { 3, 0 }, { 3, -1 }, { 3, 1 } });
        placeLeafOffsets(engine, config, world, random, center, west);
        if (vines) placeVineGroup(world, random, center, new int[][] { { -3, 0 }, { -3, -1 }, { -3, 1 } });
        int[][] upper = { { 0, 1, 0 }, { 0, 1, -1 }, { 0, 1, -2 }, { 1, 1, -1 }, { -1, 1, -1 }, { 0, 1, 1 },
            { 1, 1, 1 }, { -1, 1, 1 }, { 0, 1, 2 }, { 1, 1, 0 }, { 2, 1, 0 }, { -1, 1, 0 }, { -2, 1, 0 } };
        placeLeafOffsets(engine, config, world, random, center, upper);
    }

    private void placeVineGroup(World world, ModernRandom random, Pos center, int[][] offsets) {
        for (int[] offset : offsets) {
            if (random.nextInt(2) == 0) placeVine(world, center.offset(offset[0], 0, offset[1]));
        }
    }

    private void placeVine(World world, Pos anchor) {
        int size = RuIncidentalRandom
            .sample(world.worldSeed(), anchor.x, anchor.y, anchor.z, RuIncidentalRandom.VINE, 6);
        Pos position = anchor.offset(0, -1, 0);
        for (int i = 0; i <= size; i++) {
            if (isAir(world, position)) {
                world.setBlock(
                    position.x,
                    position.y,
                    position.z,
                    i == size ? "regions_unexplored:spanish_moss[age=25]" : "regions_unexplored:spanish_moss_plant");
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

    private void placeMoss(World world, Pos position) {
        Pos above = position.offset(0, 1, 0);
        if (!isAir(world, above)) return;
        Random incidental = RuIncidentalRandom
            .random(world.worldSeed(), position.x, position.y, position.z, RuIncidentalRandom.CYPRESS_MOSS);
        if (incidental.nextInt(2) == 0) {
            world.setBlock(above.x, above.y, above.z, "minecraft:moss_carpet");
        }
        if (incidental.nextInt(7) == 0) {
            if (incidental.nextInt(2) == 0) {
                world.setBlock(above.x, above.y, above.z, "regions_unexplored:green_bioshroom[stage=0]");
            }
            world.setBlock(position.x, position.y, position.z, "minecraft:moss_block");
        }
    }

    private void placeRoot(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int depth = RuIncidentalRandom
            .sample(world.worldSeed(), origin.x, origin.y, origin.z, RuIncidentalRandom.ROOT, 2) + 4;
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

    private static boolean treeBodyReplaceable(ModernDecorationEngine engine, World world, Pos position) {
        String state = world.blockState(position.x, position.y, position.z);
        return engine.isBlockInTag("regions_unexplored:tree_grass_replaceables", state)
            || world.isReplaceable(position.x, position.y, position.z)
            || engine.isBlockInTag("regions_unexplored:branches", state)
            || engine.isBlockInTag("minecraft:leaves", state);
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
