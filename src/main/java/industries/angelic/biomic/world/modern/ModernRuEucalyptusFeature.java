package industries.angelic.biomic.world.modern;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Direct ports of pinned RU's custom eucalyptus features. */
final class ModernRuEucalyptusFeature {

    private final ModernFeatureEngine features;

    ModernRuEucalyptusFeature(ModernFeatureEngine features) {
        this.features = features;
    }

    boolean placeLarge(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
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
            placeLargeLog(engine, config, world, random, position, "y");
            if (y > 3 && random.nextInt(10) == 0) {
                placeSmallBranch(
                    engine,
                    config,
                    world,
                    random,
                    position,
                    RuIncidentalRandom
                        .sample(world.worldSeed(), position.x, position.y, position.z, RuIncidentalRandom.BRANCH, 4));
            }
            if (y == 0) {
                int[][] roots = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
                for (int[] direction : roots) {
                    if (random.nextInt(2) == 0) {
                        Pos side = origin.offset(direction[0], 0, direction[1]);
                        placeLargeRoot(engine, config, world, random, side.offset(0, -1, 0));
                        placeLargeLog(engine, config, world, random, side, "y");
                        if (random.nextInt(2) == 0) {
                            placeLargeLog(engine, config, world, random, side.offset(0, 1, 0), "y");
                        }
                    }
                }
                placeLargeRoot(engine, config, world, random, origin);
            }
            if (y == height) placeLargeCrown(engine, config, world, random, position);
        }
        return true;
    }

    private void placeLargeCrown(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos top) {
        placeLeaf(engine, config, world, random, top.offset(0, 3, 0));
        Pos layer = top.offset(0, 2, 0);
        placeCross(engine, config, world, random, layer);
        int[][] diagonals = { { 1, -1 }, { -1, -1 }, { 1, 1 }, { -1, 1 } };
        for (int[] offset : diagonals) {
            if (random.nextInt(2) == 0) {
                placeLeaf(engine, config, world, random, layer.offset(offset[0], 0, offset[1]));
            }
        }
        placeMediumBlob(engine, config, world, random, top.offset(0, 1, 0));
        placeLargeBlob(engine, config, world, random, top);
        placeMediumBlob(engine, config, world, random, top.offset(0, -1, 0));
        placeSmallBlob(engine, config, world, random, top.offset(0, -2, 0));
        int[][] cardinals = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        for (int depth = 3; depth <= 4; depth++) {
            for (int[] offset : cardinals) {
                placeLeaf(engine, config, world, random, top.offset(offset[0], -depth, offset[1]));
            }
        }
        for (int[] offset : cardinals) {
            if (random.nextInt(2) == 0) {
                placeLeaf(engine, config, world, random, top.offset(offset[0], -5, offset[1]));
            }
        }
    }

    private void placeLargeBlob(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        int[][] offsets = { { 0, 0 }, { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 }, { 0, -2 }, { 0, 2 }, { 2, 0 },
            { -2, 0 }, { 1, -1 }, { 1, -2 }, { 2, -1 }, { 2, -2 }, { -1, -1 }, { -1, -2 }, { -2, -1 }, { -2, -2 },
            { 1, 1 }, { 1, 2 }, { 2, 1 }, { 2, 2 }, { -1, 1 }, { -1, 2 }, { -2, 1 }, { -2, 2 }, { 0, -3 }, { 1, -3 },
            { 2, -3 }, { -1, -3 }, { -2, -3 }, { 0, 3 }, { 1, 3 }, { 2, 3 }, { -1, 3 }, { -2, 3 }, { 3, 0 }, { 3, -1 },
            { 3, -2 }, { 3, 1 }, { 3, 2 }, { -3, 0 }, { -3, -1 }, { -3, -2 }, { -3, 1 }, { -3, 2 } };
        placeLeafOffsets(engine, config, world, random, center, offsets);
    }

    private void placeMediumBlob(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        int[][] offsets = { { 0, 0 }, { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 }, { 0, -2 }, { 0, 2 }, { 2, 0 },
            { -2, 0 }, { 1, -1 }, { 1, -2 }, { 2, -1 }, { -1, -1 }, { -1, -2 }, { -2, -1 }, { 1, 1 }, { 1, 2 },
            { 2, 1 }, { -1, 1 }, { -1, 2 }, { -2, 1 } };
        placeLeafOffsets(engine, config, world, random, center, offsets);
    }

    private void placeSmallBlob(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        int[][] offsets = { { 0, 0 }, { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 }, { 0, -2 }, { 0, 2 }, { 2, 0 },
            { -2, 0 }, { 1, -1 }, { -1, -1 }, { 1, 1 }, { -1, 1 } };
        placeLeafOffsets(engine, config, world, random, center, offsets);
    }

    private void placeLeafOffsets(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, int[][] offsets) {
        for (int[] offset : offsets) {
            placeLeaf(engine, config, world, random, center.offset(offset[0], 0, offset[1]));
        }
    }

    private void placeLargeRoot(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int depth = RuIncidentalRandom
            .sample(world.worldSeed(), origin.x, origin.y, origin.z, RuIncidentalRandom.ROOT, 2) + 2;
        Pos position = origin;
        for (int step = 0; step <= depth; step++, position = position.offset(0, -1, 0)) {
            String state = world.blockState(position.x, position.y, position.z);
            String above = world.blockState(position.x, position.y + 1, position.z);
            if (world.isReplaceable(position.x, position.y, position.z)
                && engine.isBlockInTag("minecraft:dirt", above)) {
                world.setBlock(position.x, position.y, position.z, "minecraft:hanging_roots");
                break;
            }
            if (engine.isBlockInTag("minecraft:dirt", state)
                || engine.isBlockInTag("minecraft:replaceable_by_trees", state)
                || "minecraft:air".equals(ModernDecorationEngine.blockName(state))) {
                placeLargeLog(engine, config, world, random, position, "y");
            } else {
                break;
            }
        }
    }

    private void placeLargeLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, String axis) {
        if (!inside(world, position)) return;
        String current = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = substrateReplacement(current, false);
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
            ModernDecorationEngine.blockName(world.blockState(below.x, below.y, below.z)),
            false);
        if (belowReplacement != null) {
            world.setBlock(below.x, below.y, below.z, belowReplacement);
        }
    }

    boolean placeSmall(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
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
            placeLog(engine, config, world, random, position);
            if (y > 3 && random.nextInt(8) == 0) {
                placeSmallBranch(
                    engine,
                    config,
                    world,
                    random,
                    position,
                    RuIncidentalRandom
                        .sample(world.worldSeed(), position.x, position.y, position.z, RuIncidentalRandom.BRANCH, 4));
            }
            if (y == height) placeSmallCrown(engine, config, world, random, position);
        }
        return true;
    }

    private void placeSmallBranch(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, int direction) {
        int[][] offsets = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        String[] facings = { "north", "south", "east", "west" };
        String[] axes = { "z", "z", "x", "x" };
        int dx = offsets[direction][0];
        int dz = offsets[direction][1];
        Pos branch = center.offset(dx, 0, dz);
        if (inside(world, branch) && world.isReplaceable(branch.x, branch.y, branch.z)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("branch_provider"), random, center);
            // RU BranchBlock has facing, not axis: source trySetValue ignores absent properties.
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

    private void placeSmallCrown(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos top) {
        Pos layer = top.offset(0, -3, 0);
        placeCross(engine, config, world, random, layer);

        layer = layer.offset(0, 1, 0);
        placeCross(engine, config, world, random, layer);
        int[][] diagonals = { { 1, -1 }, { -1, -1 }, { 1, 1 }, { -1, 1 } };
        for (int[] offset : diagonals) {
            if (random.nextInt(3) == 0) {
                placeLeaf(engine, config, world, random, layer.offset(offset[0], 0, offset[1]));
            }
        }

        layer = layer.offset(0, 1, 0);
        placeSquare(engine, config, world, random, layer);

        layer = layer.offset(0, 1, 0);
        placeSquare(engine, config, world, random, layer);
        int[][] northSouthOuter = { { 0, -2 }, { 1, -2 }, { -1, -2 }, { 0, 2 }, { 1, 2 }, { -1, 2 } };
        for (int[] offset : northSouthOuter) {
            placeLeaf(engine, config, world, random, layer.offset(offset[0], 0, offset[1]));
        }
        addHangingVine(world, random, layer.offset(0, 0, -3), "south");
        addHangingVine(world, random, layer.offset(1, 0, -3), "south");
        addHangingVine(world, random, layer.offset(-1, 0, -3), "south");
        addHangingVine(world, random, layer.offset(0, 0, 3), "north");
        addHangingVine(world, random, layer.offset(1, 0, 3), "north");
        addHangingVine(world, random, layer.offset(-1, 0, 3), "north");
        int[][] eastWestOuter = { { 2, 0 }, { 2, -1 }, { 2, 1 }, { -2, 0 }, { -2, -1 }, { -2, 1 } };
        for (int[] offset : eastWestOuter) {
            placeLeaf(engine, config, world, random, layer.offset(offset[0], 0, offset[1]));
        }
        addHangingVine(world, random, layer.offset(3, 0, 0), "west");
        addHangingVine(world, random, layer.offset(3, 0, -1), "west");
        addHangingVine(world, random, layer.offset(3, 0, 1), "west");
        addHangingVine(world, random, layer.offset(-3, 0, 0), "east");
        addHangingVine(world, random, layer.offset(-3, 0, -1), "east");
        addHangingVine(world, random, layer.offset(-3, 0, 1), "east");

        layer = layer.offset(0, 1, 0);
        placeSquare(engine, config, world, random, layer);

        layer = layer.offset(0, 1, 0);
        placeCross(engine, config, world, random, layer);
        for (int[] offset : diagonals) {
            if (random.nextInt(3) == 0) {
                placeLeaf(engine, config, world, random, layer.offset(offset[0], 0, offset[1]));
            }
        }

        layer = layer.offset(0, 1, 0);
        placeCross(engine, config, world, random, layer);
        layer = layer.offset(0, 1, 0);
        placeCross(engine, config, world, random, layer);
        layer = layer.offset(0, 1, 0);
        placeLeaf(engine, config, world, random, layer);
        placeLeaf(engine, config, world, random, layer.offset(0, 1, 0));
    }

    private void placeCross(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        placeLeaf(engine, config, world, random, center);
        placeLeaf(engine, config, world, random, center.offset(0, 0, -1));
        placeLeaf(engine, config, world, random, center.offset(0, 0, 1));
        placeLeaf(engine, config, world, random, center.offset(1, 0, 0));
        placeLeaf(engine, config, world, random, center.offset(-1, 0, 0));
    }

    private void placeSquare(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        placeCross(engine, config, world, random, center);
        placeLeaf(engine, config, world, random, center.offset(1, 0, -1));
        placeLeaf(engine, config, world, random, center.offset(-1, 0, -1));
        placeLeaf(engine, config, world, random, center.offset(1, 0, 1));
        placeLeaf(engine, config, world, random, center.offset(-1, 0, 1));
    }

    private void addHangingVine(World world, ModernRandom random, Pos origin, String facing) {
        if (random.nextInt(3) == 0) return;
        String vine = ModernFeatureEngine.property("minecraft:vine", facing, "true");
        world.setBlock(origin.x, origin.y, origin.z, vine);
        int remaining = random.nextInt(3) + 3;
        Pos position = origin.offset(0, -1, 0);
        while ("minecraft:air"
            .equals(ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z)))
            && remaining-- > 0) {
            world.setBlock(position.x, position.y, position.z, vine);
            position = position.offset(0, -1, 0);
        }
    }

    private void placeLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (!inside(world, position)) return;
        String current = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = substrateReplacement(current, false);
        if (replacement != null) {
            world.setBlock(position.x, position.y, position.z, replacement);
        } else if (replaceable(engine, world, position)) {
            world.setBlock(
                position.x,
                position.y,
                position.z,
                features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position));
        }
        Pos below = position.offset(0, -1, 0);
        String belowBlock = ModernDecorationEngine.blockName(world.blockState(below.x, below.y, below.z));
        String belowReplacement = substrateReplacement(belowBlock, true);
        if (belowReplacement != null) {
            world.setBlock(below.x, below.y, below.z, belowReplacement);
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

    private static String substrateReplacement(String block, boolean below) {
        if ("regions_unexplored:peat_grass_block".equals(block)) {
            return "regions_unexplored:peat_dirt";
        }
        if ("regions_unexplored:silt_grass_block".equals(block)) {
            return below ? "regions_unexplored:silt_podzol" : "regions_unexplored:silt_dirt";
        }
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
