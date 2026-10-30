package industries.angelic.biomic.world.modern;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Direct port of pinned RU's LushPineTreeFeature, including its vine cleanup order. */
final class ModernRuLushPineFeature {

    private final ModernFeatureEngine features;

    ModernRuLushPineFeature(ModernFeatureEngine features) {
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
        placeLongBranches(engine, config, world, random, top.offset(0, -9, 0));
        placeLongBranches(engine, config, world, random, top.offset(0, -12, 0));
        Pos lower = top.offset(0, -15, 0);
        if (random.nextInt(3) == 0) {
            placeLongBranches(engine, config, world, random, lower);
            placeShortBranches(engine, config, world, random, lower.offset(0, -2, 0));
        } else {
            Pos shortLevel = lower.offset(0, 1, 0);
            placeShortBranches(engine, config, world, random, shortLevel);
            int[][] cardinals = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
            for (int[] offset : cardinals) {
                placeLog(engine, config, world, random, shortLevel.offset(offset[0], -1, offset[1]), "y");
            }
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
        int[][] dirs = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        for (int direction = 0; direction < 4; direction++) {
            int dx = dirs[direction][0];
            int dz = dirs[direction][1];
            Pos end = center.offset(dx, 0, dz);
            placeLog(engine, config, world, random, end, direction < 2 ? "z" : "x");
            placeDirectionalBlob(engine, config, world, random, end, dx, dz);
        }
    }

    private void placeLongBranches(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
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
            placeLog(engine, config, world, random, center.offset(dx, -1, dz), axis);
            placeLog(engine, config, world, random, center.offset(dx, 0, dz), axis);
            placeLog(engine, config, world, random, center.offset(dx * 2, 0, dz * 2), axis);
            placeDirectionalBlob(engine, config, world, random, center.offset(dx * 2, 0, dz * 2), dx, dz);
        }
    }

    private void placeDirectionalBlob(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, Pos center, int dx, int dz) {
        // Source NORTH/SOUTH both visit east before west; EAST/WEST both visit north before
        // south. Rotating a symmetric mask reverses two visit orders and reseeds every vine
        // draw, then subsequent trees in the same placed-feature stream.
        int px = dx == 0 ? 1 : 0;
        int pz = dz == 0 ? -1 : 0;
        int[][] coordinates = { { -1, 0 }, { -1, 1 }, { -1, -1 }, { 0, 0 }, { 0, 1 }, { 0, -1 }, { 0, 2 }, { 0, -2 },
            { 1, 0 }, { 1, 1 }, { 1, -1 }, { 1, 2 }, { 1, -2 }, { 2, 0 }, { 2, 1 }, { 2, -1 } };
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
        for (int perpendicular = 0; perpendicular <= 1; perpendicular++) {
            int value = perpendicular == 0 ? 0 : 1;
            placeLeaf(engine, config, world, random, center.offset(dx + px * value, 1, dz + pz * value));
            if (value != 0) {
                placeLeaf(engine, config, world, random, center.offset(dx - px, 1, dz - pz));
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
        // The branch decorator writes foliage directly. Unlike the crown helpers it does
        // not run LushPineTreeFeature.addVineDecorator for these four leaves.
        placePlainLeaf(engine, config, world, random, branch.offset(0, 1, 0));
        placePlainLeaf(engine, config, world, random, center.offset(dx * 2, 0, dz * 2));
        if (dx == 0) {
            placePlainLeaf(engine, config, world, random, center.offset(1, 0, dz));
            placePlainLeaf(engine, config, world, random, center.offset(-1, 0, dz));
        } else {
            placePlainLeaf(engine, config, world, random, center.offset(dx, 0, 1));
            placePlainLeaf(engine, config, world, random, center.offset(dx, 0, -1));
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
        addVines(world, random, position);
        Pos below = position.offset(0, -1, 0);
        while ("minecraft:vine".equals(ModernDecorationEngine.blockName(world.blockState(below.x, below.y, below.z)))) {
            world.setBlock(below.x, below.y, below.z, "minecraft:air");
            below = below.offset(0, -1, 0);
        }
    }

    private void placePlainLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (!inside(world, position) || !world.isReplaceable(position.x, position.y, position.z)) {
            return;
        }
        String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
        world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "distance", "1"));
    }

    private void addVines(World world, ModernRandom random, Pos leaf) {
        int[][] offsets = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        String[] facings = { "east", "west", "south", "north" };
        for (int i = 0; i < offsets.length; i++) {
            if (random.nextFloat() >= 0.25F) continue;
            Pos start = leaf.offset(offsets[i][0], 0, offsets[i][1]);
            if (!isAir(world, start)) continue;
            String vine = ModernFeatureEngine.property("minecraft:vine", facings[i], "true");
            world.setBlock(start.x, start.y, start.z, vine);
            int remaining = random.nextInt(4) + 2;
            Pos below = start.offset(0, -1, 0);
            while (isAir(world, below) && remaining-- > 0) {
                world.setBlock(below.x, below.y, below.z, vine);
                below = below.offset(0, -1, 0);
            }
        }
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
