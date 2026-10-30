package industries.angelic.biomic.world.modern;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Direct shared port of pinned RU's mega (2x2) and ultra (3x3) baobabs. */
final class ModernRuBaobabFeature {

    private static final int[][] DIRECTIONS = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
    private static final String[] FACINGS = { "north", "south", "east", "west" };

    private final ModernFeatureEngine features;

    ModernRuBaobabFeature(ModernFeatureEngine features) {
        this.features = features;
    }

    boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos suppliedOrigin, boolean ultra) {
        Pos origin = ultra ? suppliedOrigin : suppliedOrigin.offset(0, 0, 1);
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        for (int y = 0; y <= height; y++) {
            Pos center = origin.offset(0, y, 0);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (!replaceable(engine, world, center.offset(dx, 0, dz))) return false;
                }
            }
        }

        int[][] trunk = ultra
            ? new int[][] { { 0, 0 }, { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 }, { 1, -1 }, { -1, -1 }, { 1, 1 },
                { -1, 1 } }
            : new int[][] { { 0, 0 }, { 0, -1 }, { 1, 0 }, { 1, -1 } };
        for (int y = 0; y <= height; y++) {
            Pos level = origin.offset(0, y, 0);
            if (y == 0) {
                for (int[] offset : trunk) {
                    placeRoot(engine, config, world, random, level.offset(offset[0], 0, offset[1]));
                }
            }
            for (int[] offset : trunk) {
                placeLog(engine, config, world, random, level.offset(offset[0], 0, offset[1]), "y");
            }
            if (y == height) placeTop(engine, config, world, random, level, ultra);
        }
        return true;
    }

    private void placeTop(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos top,
        boolean ultra) {
        Pos upper = top.offset(0, 1, 0);
        if (ultra) {
            placeLog(engine, config, world, random, upper, "y");
            for (int direction = 0; direction < DIRECTIONS.length; direction++) {
                int[] offset = DIRECTIONS[direction];
                Pos branchBase = upper.offset(offset[0], 0, offset[1]);
                placeLog(engine, config, world, random, branchBase, "y");
                placeCrownBranch(engine, config, world, random, branchBase, direction, true);
            }
        } else {
            placeLog(engine, config, world, random, upper, "y");
            // Pinned source redundantly places this log before the west branch.
            placeLog(engine, config, world, random, upper, "y");
            placeCrownBranch(engine, config, world, random, upper, 3, false);
            Pos north = upper.offset(0, 0, -1);
            placeLog(engine, config, world, random, north, "y");
            placeCrownBranch(engine, config, world, random, north, 0, false);
            Pos northEast = upper.offset(1, 0, -1);
            placeLog(engine, config, world, random, northEast, "y");
            placeCrownBranch(engine, config, world, random, northEast, 2, false);
            Pos east = upper.offset(1, 0, 0);
            placeLog(engine, config, world, random, east, "y");
            placeCrownBranch(engine, config, world, random, east, 1, false);
        }
    }

    private void placeCrownBranch(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos base, int direction, boolean ultra) {
        int type = random.nextInt(8);
        int dx = DIRECTIONS[direction][0];
        int dz = DIRECTIONS[direction][1];
        String axis = direction < 2 ? "z" : "x";
        Pos position = base;
        int outward;
        int upward;
        if (ultra) {
            outward = type == 7 ? 3 : type == 2 || type == 3 ? 1 : 2;
            upward = type == 0 ? 1 : type == 1 ? 1 : type == 2 ? 2 : type == 3 ? 3 : type == 4 ? 2 : type == 5 ? 3 : 0;
            if (type == 1) {
                position = position.offset(0, 1, 0);
                placeLog(engine, config, world, random, position, "y");
            }
        } else {
            outward = type == 2 || type == 3 ? 1 : 2;
            upward = type == 0 ? 1 : type == 1 ? 1 : type == 2 || type == 3 ? 2 : type == 4 || type == 5 ? 2 : 0;
            if (type == 1) {
                position = position.offset(0, 1, 0);
                placeLog(engine, config, world, random, position, "y");
            }
        }
        for (int step = 0; step < outward; step++) {
            position = position.offset(dx, 0, dz);
            placeLog(engine, config, world, random, position, axis);
        }
        for (int step = 0; step < upward; step++) {
            position = position.offset(0, 1, 0);
            placeLog(engine, config, world, random, position, "y");
        }
        placeLeavesBlob(engine, config, world, random, position, ultra);
    }

    private void placeLeavesBlob(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, boolean ultra) {
        if (!ultra) random.nextInt(3); // pinned Mega source computes an unused value
        // Ultra computes the same unused value with fresh java.util.Random instead.
        placeBranchCross(engine, config, world, random, center);
        int[][] common = { { 1, 0, -1 }, { -1, 0, -1 }, { 1, 0, 1 }, { -1, 0, 1 }, { 0, 0, -2 }, { 1, 0, -2 },
            { -1, 0, -2 }, { 0, 0, 2 }, { 1, 0, 2 }, { -1, 0, 2 }, { 2, 0, 0 }, { 2, 0, -1 }, { 2, 0, 1 }, { -2, 0, 0 },
            { -2, 0, -1 }, { -2, 0, 1 } };
        placeLeaves(engine, config, world, random, center, common);
        if (ultra) {
            int[][] extra = { { 0, 0, -3 }, { 1, 0, -3 }, { -1, 0, -3 }, { 0, 0, 3 }, { 1, 0, 3 }, { -1, 0, 3 },
                { 3, 0, 0 }, { 3, 0, -1 }, { 3, 0, 1 }, { -3, 0, 0 }, { -3, 0, -1 }, { -3, 0, 1 }, { 2, 0, -2 },
                { -2, 0, -2 }, { 2, 0, 2 }, { -2, 0, 2 }, { 0, 1, 0 }, { 0, 1, -1 }, { 0, 1, 1 }, { 1, 1, 0 },
                { -1, 1, 0 }, { 0, 1, -2 }, { 0, 1, 2 }, { 2, 1, 0 }, { -2, 1, 0 }, { 1, 1, -1 }, { -1, 1, -1 },
                { 1, 1, 1 }, { -1, 1, 1 }, { 0, -1, 0 }, { 0, -1, -1 }, { 0, -1, 1 }, { 1, -1, 0 }, { -1, -1, 0 } };
            placeLeaves(engine, config, world, random, center, extra);
        } else {
            int[][] upper = { { 0, 1, 0 }, { 0, 1, -1 }, { 0, 1, 1 }, { 1, 1, 0 }, { -1, 1, 0 } };
            placeLeaves(engine, config, world, random, center, upper);
            int[][] corners = { { 1, 1, -1 }, { -1, 1, -1 }, { 1, 1, 1 }, { -1, 1, 1 } };
            for (int[] corner : corners) {
                if (random.nextInt(3) == 0) {
                    placeLeaf(engine, config, world, random, center.offset(corner[0], corner[1], corner[2]));
                }
            }
        }
    }

    private void placeBranchCross(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        for (int direction = 0; direction < DIRECTIONS.length; direction++) {
            // Each upstream helper rewrites the center trunk before adding its branch.
            world.setBlock(
                center.x,
                center.y,
                center.z,
                features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, center));
            int dx = DIRECTIONS[direction][0];
            int dz = DIRECTIONS[direction][1];
            Pos branch = center.offset(dx, 0, dz);
            if (replaceableByState(world, branch)) {
                String state = features
                    .stateProvider(engine, config.getAsJsonObject("branch_provider"), random, branch);
                world.setBlock(
                    branch.x,
                    branch.y,
                    branch.z,
                    ModernFeatureEngine.property(state, "facing", FACINGS[direction]));
            }
            placeLeaf(engine, config, world, random, center.offset(dx, 1, dz));
            placeLeaf(engine, config, world, random, center.offset(dx * 2, 0, dz * 2));
            if (dx == 0) {
                placeLeaf(engine, config, world, random, center.offset(1, 0, dz));
                placeLeaf(engine, config, world, random, center.offset(-1, 0, dz));
            } else {
                placeLeaf(engine, config, world, random, center.offset(dx, 0, 1));
                placeLeaf(engine, config, world, random, center.offset(dx, 0, -1));
            }
        }
    }

    private void placeLeaves(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, int[][] offsets) {
        for (int[] offset : offsets) {
            placeLeaf(engine, config, world, random, center.offset(offset[0], offset[1], offset[2]));
        }
    }

    private void placeLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (!replaceableByState(world, position)) return;
        String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
        world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "distance", "1"));
    }

    private void placeRoot(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int depth = RuIncidentalRandom
            .sample(world.worldSeed(), origin.x, origin.y, origin.z, RuIncidentalRandom.ROOT, 2) + 4;
        Pos position = origin;
        for (int step = 0; step <= depth; step++, position = position.offset(0, -1, 0)) {
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
            } else {
                break;
            }
        }
    }

    private void placeLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, String axis) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        String current = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = grassReplacement(current);
        if (replacement != null) {
            world.setBlock(position.x, position.y, position.z, replacement);
        } else if (replaceable(engine, world, position)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
            world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "axis", axis));
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
