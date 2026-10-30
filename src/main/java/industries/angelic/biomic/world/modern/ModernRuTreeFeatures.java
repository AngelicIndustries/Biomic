package industries.angelic.biomic.world.modern;

import java.util.Random;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Direct ports of RU's non-vanilla tree Feature implementations. */
final class ModernRuTreeFeatures {

    private final ModernFeatureEngine features;

    ModernRuTreeFeatures(ModernFeatureEngine features) {
        this.features = features;
    }

    boolean place(ModernDecorationEngine engine, String type, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        if ("regions_unexplored:ashen_tree".equals(type)) {
            return ashen(engine, config, world, random, origin);
        }
        if ("regions_unexplored:small_joshua_tree".equals(type)) {
            return smallJoshua(engine, config, world, random, origin);
        }
        if ("regions_unexplored:medium_joshua_tree".equals(type)) {
            return mediumJoshua(world, random, origin);
        }
        if ("regions_unexplored:large_joshua_tree".equals(type)) {
            return largeJoshua(world, random, origin);
        }
        if ("regions_unexplored:saguaro_cactus".equals(type)) {
            return saguaro(engine, config, world, random, origin);
        }
        if ("regions_unexplored:palm_tree".equals(type)) {
            return palm(engine, config, world, random, origin);
        }
        if ("regions_unexplored:small_socotra_tree".equals(type)) {
            return smallSocotra(engine, world, random, origin);
        }
        if ("regions_unexplored:large_socotra_tree".equals(type)) {
            return largeSocotra(engine, config, world, random, origin);
        }
        if ("regions_unexplored:bamboo_tree".equals(type)) {
            return bamboo(engine, config, world, random, origin);
        }
        if ("regions_unexplored:cobalt_tree".equals(type)) {
            return cobalt(engine, world, random, origin);
        }
        if ("regions_unexplored:small_oak_tree".equals(type)) {
            return smallOak(engine, config, world, random, origin);
        }
        if ("regions_unexplored:dead_tree".equals(type)) {
            return deadTree(engine, config, world, random, origin);
        }
        if ("regions_unexplored:spire".equals(type)) {
            return iceSpire(engine, config, world, random, origin);
        }
        throw new IllegalArgumentException("Unsupported pinned RU tree feature " + type);
    }

    private boolean iceSpire(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        int northHeight = height > 10 ? height / 2 + random.nextInt(3) : height / 2;
        int southHeight = height > 10 ? height / 2 + random.nextInt(3) : height / 2;
        int eastHeight = height > 10 ? height / 2 + random.nextInt(3) : height / 2;
        int westHeight = height > 10 ? height / 2 + random.nextInt(3) : height / 2;
        int northEastHeight = random.nextInt(5);
        int northWestHeight = random.nextInt(5);
        int southEastHeight = random.nextInt(5);
        int southWestHeight = random.nextInt(5);
        for (int y = 0; y <= height; y++) {
            if (!saguaroReplaceable(engine, world, origin.offset(0, y, 0))) return false;
        }
        if (random.nextInt(2) == 0) {
            generateSmallSpire(engine, config, world, random, origin);
            return true;
        }

        int[][] columns = { { 1, 0, northEastHeight }, { -1, 0, northWestHeight }, { 1, 1, southEastHeight },
            { -1, 1, southWestHeight }, { 0, -1, northHeight }, { 0, 1, southHeight }, { 1, 0, eastHeight },
            { -1, 0, westHeight } };
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            placeIceSpireLog(engine, config, world, random, position);
            for (int index = 0; index < columns.length; index++) {
                int[] column = columns[index];
                int dx;
                int dz;
                if (index < 4) {
                    dx = column[0];
                    dz = column[1] == 0 ? -1 : 1;
                } else {
                    dx = column[0];
                    dz = column[1];
                }
                if (y <= column[2]) {
                    Pos side = position.offset(dx, 0, dz);
                    if (y == column[2]) iceSpireLeaves(engine, config, world, random, side);
                    placeIceSpireLog(engine, config, world, random, side);
                }
            }
            if (y == 0) {
                int[][] roots = { { 0, 0 }, { 0, -1 }, { 1, -1 }, { -1, -1 }, { 0, 1 }, { 1, 1 }, { -1, 1 }, { 1, 0 },
                    { -1, 0 } };
                for (int[] root : roots) {
                    iceSpireRoot(engine, config, world, random, position.offset(root[0], -1, root[1]));
                }
                int[][] optional = { { 0, -2 }, { 0, 2 }, { 2, 0 }, { -2, 0 } };
                for (int[] side : optional) {
                    if (random.nextInt(2) == 0) {
                        Pos outer = position.offset(side[0], 0, side[1]);
                        iceSpireLeaves(engine, config, world, random, outer);
                        iceSpireRoot(engine, config, world, random, outer);
                    }
                }
            }
            if (y == height) iceSpireLeaves(engine, config, world, random, position);
        }
        return true;
    }

    private void generateSmallSpire(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int size = random.nextInt(3) + 5;
        int[][] cross = { { 0, 0 }, { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        for (int y = 0; y <= size; y++) {
            Pos position = origin.offset(0, y, 0);
            if (y == 0) {
                for (int[] offset : cross) {
                    iceSpireRoot(engine, config, world, random, origin.offset(offset[0], 0, offset[1]));
                }
            }
            placeIceSpireLog(engine, config, world, random, position);
            if (y == size) iceSpireLeaves(engine, config, world, random, position);
        }
        for (int index = 1; index < cross.length; index++) {
            int[] offset = cross[index];
            Pos side = origin.offset(offset[0], 0, offset[1]);
            placeIceSpireLog(engine, config, world, random, side);
            placeIceSpireLog(engine, config, world, random, side.offset(0, 1, 0));
            iceSpireLeaves(engine, config, world, random, side.offset(0, 1, 0));
        }
    }

    private void iceSpireLeaves(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos base) {
        placeIceSpireLeaf(engine, config, world, random, base.offset(0, 1, 0));
        placeIceSpireLeaf(engine, config, world, random, base.offset(0, 2, 0));
        if (random.nextBoolean()) {
            placeIceSpireLeaf(engine, config, world, random, base.offset(0, 3, 0));
        }
    }

    private void placeIceSpireLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()
            || !world.isReplaceable(position.x, position.y, position.z)) return;
        world.setBlock(
            position.x,
            position.y,
            position.z,
            features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position));
    }

    private void placeIceSpireLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        String current = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = saguaroGrassReplacement(current);
        if (replacement != null) {
            world.setBlock(position.x, position.y, position.z, replacement);
        } else if (saguaroReplaceable(engine, world, position)) {
            world.setBlock(
                position.x,
                position.y,
                position.z,
                features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position));
        } else {
            return;
        }
        String below = ModernDecorationEngine.blockName(world.blockState(position.x, position.y - 1, position.z));
        String belowReplacement = saguaroGrassReplacement(below);
        if (belowReplacement != null) {
            world.setBlock(position.x, position.y - 1, position.z, belowReplacement);
        }
    }

    private void iceSpireRoot(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int depth = RuIncidentalRandom
            .sample(world.worldSeed(), origin.x, origin.y, origin.z, RuIncidentalRandom.ROOT, 2) + 4;
        Pos position = origin;
        for (int step = 0; step <= depth; step++, position = position.offset(0, -1, 0)) {
            if (world.isReplaceable(position.x, position.y, position.z)
                && engine.isBlockInTag("minecraft:dirt", world.blockState(position.x, position.y + 1, position.z)))
                break;
            placeIceSpireLog(engine, config, world, random, position);
        }
    }

    private boolean deadTree(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        for (int y = 0; y <= height; y++) {
            if (!saguaroReplaceable(engine, world, origin.offset(0, y, 0))) return false;
        }
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            placeDeadLog(engine, config, world, random, position, "y");
            if (y > 2) {
                deadLeafDecorator(engine, config, world, random, position);
                if (random.nextInt(3) == 0) {
                    int direction = RuIncidentalRandom
                        .sample(world.worldSeed(), position.x, position.y, position.z, RuIncidentalRandom.BRANCH, 4);
                    int[][] offsets = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
                    String[] facings = { "north", "south", "east", "west" };
                    placeDeadBranch(
                        engine,
                        config,
                        world,
                        random,
                        position,
                        offsets[direction][0],
                        offsets[direction][1],
                        facings[direction]);
                }
            }
            if (y == 0) deadRoot(engine, config, world, random, position);
            if (y == height) deadTop(engine, config, world, random, position);
        }
        return true;
    }

    private void deadTop(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos top) {
        Pos branchLevel = top.offset(0, -(random.nextInt(2) + 3), 0);
        int direction = random.nextInt(5);
        placeDeadLeaf(engine, config, world, random, top.offset(0, 0, -1));
        placeDeadLeaf(engine, config, world, random, top.offset(0, 0, 1));
        placeDeadLeaf(engine, config, world, random, top.offset(1, 0, 0));
        placeDeadLeaf(engine, config, world, random, top.offset(-1, 0, 0));
        placeDeadLeaf(engine, config, world, random, top.offset(0, 1, 0));
        if (direction < 4) {
            int[][] offsets = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
            String[] axes = { "z", "z", "x", "x" };
            placeDeadLog(
                engine,
                config,
                world,
                random,
                branchLevel.offset(offsets[direction][0], 0, offsets[direction][1]),
                axes[direction]);
        }
    }

    private void deadLeafDecorator(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        Random incidental = RuIncidentalRandom
            .random(world.worldSeed(), center.x, center.y, center.z, RuIncidentalRandom.LEAF_BLOB);
        int[][] offsets = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        for (int[] offset : offsets) {
            if (incidental.nextInt(2) == 0) {
                placeDeadLeaf(engine, config, world, random, center.offset(offset[0], 0, offset[1]));
            }
        }
    }

    private void placeDeadBranch(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, int dx, int dz, String facing) {
        Pos branch = center.offset(dx, 0, dz);
        if (branch.y < world.minBuildHeight() || branch.y >= world.maxBuildHeight()
            || !world.isReplaceable(branch.x, branch.y, branch.z)) return;
        String state = features.stateProvider(engine, config.getAsJsonObject("branch_provider"), random, center);
        world.setBlock(
            branch.x,
            branch.y,
            branch.z,
            ModernFeatureEngine
                .tryProperty(ModernFeatureEngine.tryProperty(state, "axis", dx == 0 ? "z" : "x"), "facing", facing));
    }

    private void placeDeadLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, String axis) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        String block = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = saguaroGrassReplacement(block);
        if (replacement != null) {
            world.setBlock(position.x, position.y, position.z, replacement);
        } else if (saguaroReplaceable(engine, world, position)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
            world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "axis", axis));
        } else {
            return;
        }
        String below = ModernDecorationEngine.blockName(world.blockState(position.x, position.y - 1, position.z));
        String belowReplacement = saguaroGrassReplacement(below);
        if (belowReplacement != null) {
            world.setBlock(position.x, position.y - 1, position.z, belowReplacement);
        }
    }

    private void placeDeadLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()
            || !world.isReplaceable(position.x, position.y, position.z)) return;
        String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
        world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "distance", "1"));
    }

    private void deadRoot(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
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
                placeDeadLog(engine, config, world, random, position, "y");
            } else {
                break;
            }
        }
    }

    private boolean smallOak(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        for (int y = 0; y <= height; y++) {
            if (!saguaroReplaceable(engine, world, origin.offset(0, y, 0))) return false;
        }
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            placeSmallOakLog(engine, config, world, random, position, "y");
            if (y == height) smallOakTop(engine, config, world, random, position);
        }
        return true;
    }

    private void smallOakTop(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos top) {
        int[] drops = { random.nextInt(2) + 1, random.nextInt(2) + 1, random.nextInt(2) + 1, random.nextInt(2) + 1 };
        int[][] directions = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        String[] facings = { "north", "south", "east", "west" };
        for (int index = 0; index < directions.length; index++) {
            smallOakBranch(
                engine,
                config,
                world,
                random,
                top.offset(0, -drops[index], 0),
                directions[index][0],
                directions[index][1],
                facings[index]);
        }
        int[][] blob = { { 0, 3, 0 }, { 0, 2, 0 }, { 0, 1, 0 }, { 0, 1, -1 }, { 0, 1, 1 }, { 1, 1, 0 }, { -1, 1, 0 },
            { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, -1, 0 }, { 0, -1, -1 },
            { 0, -1, 1 }, { 1, -1, 0 }, { -1, -1, 0 }, { 1, -1, -1 }, { -1, -1, -1 }, { 1, -1, 1 }, { -1, -1, 1 } };
        for (int[] offset : blob) {
            placeSmallOakLeaf(engine, config, world, random, top.offset(offset[0], offset[1], offset[2]));
        }
    }

    private void smallOakBranch(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos base, int dx, int dz, String facing) {
        Pos branch = base.offset(dx, 0, dz);
        if (branch.y >= world.minBuildHeight() && branch.y < world.maxBuildHeight()
            && world.isReplaceable(branch.x, branch.y, branch.z)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("branch_provider"), random, base);
            state = ModernFeatureEngine.tryProperty(state, "axis", dx == 0 ? "z" : "x");
            state = ModernFeatureEngine.tryProperty(state, "facing", facing);
            ModernTreeShapeUpdates.smallOakWrite(engine, world, branch, state);
        }
        int[][] leaves = { { dx, 1, dz }, { dx, -1, dz }, { dx * 2, 0, dz * 2 } };
        for (int[] offset : leaves) {
            placeSmallOakLeaf(engine, config, world, random, base.offset(offset[0], offset[1], offset[2]));
        }
        if (dx == 0) {
            placeSmallOakLeaf(engine, config, world, random, base.offset(1, 0, dz));
            placeSmallOakLeaf(engine, config, world, random, base.offset(-1, 0, dz));
        } else {
            // Source order is south then north for east/west branches.
            placeSmallOakLeaf(engine, config, world, random, base.offset(dx, 0, 1));
            placeSmallOakLeaf(engine, config, world, random, base.offset(dx, 0, -1));
        }
    }

    private void placeSmallOakLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, String axis) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()
            || !saguaroReplaceable(engine, world, position)) return;
        String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
        ModernTreeShapeUpdates
            .smallOakWrite(engine, world, position, ModernFeatureEngine.property(state, "axis", axis));
    }

    private void placeSmallOakLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()
            || !world.isReplaceable(position.x, position.y, position.z)) return;
        String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
        ModernTreeShapeUpdates
            .smallOakWrite(engine, world, position, ModernFeatureEngine.property(state, "distance", "1"));
    }

    private boolean cobalt(ModernDecorationEngine engine, World world, ModernRandom random, Pos origin) {
        if (!saguaroReplaceable(engine, world, origin)) return false;
        int size = random.nextInt(4);
        int directionIndex = random.nextInt(4);
        int[][] directions = { { 1, 0 }, { 0, 1 }, { -1, 0 }, { 0, -1 } };
        int dx = directions[directionIndex][0];
        int dz = directions[directionIndex][1];
        Pos position = origin;
        for (int y = 0; y <= size; y++) {
            placeCobaltLog(engine, world, position, "y");
            if (y < size) position = position.offset(0, 1, 0);
        }
        cobaltTop(world, position);
        if (size == 2 && random.nextInt(4) != 0) return true;
        if (size >= 2) {
            position = position.offset(dx, 0, dz);
            placeCobaltLog(engine, world, position, dx == 0 ? "z" : "x");
            position = position.offset(dx, 1, dz);
            placeCobaltLog(engine, world, position, "y");
            position = position.offset(0, 1, 0);
            placeCobaltLog(engine, world, position, "y");
            cobaltTop(world, position);
        }
        return true;
    }

    private static void cobaltTop(World world, Pos center) {
        int[][] offsets = { { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 }, { 1, 0, -1 }, { -1, 0, -1 },
            { 1, 0, 1 }, { -1, 0, 1 }, { 0, 1, 0 }, { 0, 1, -1 }, { 0, 1, 1 }, { 1, 1, 0 }, { -1, 1, 0 } };
        for (int[] offset : offsets) {
            Pos leaf = center.offset(offset[0], offset[1], offset[2]);
            if (leaf.y >= world.minBuildHeight() && leaf.y < world.maxBuildHeight()
                && world.isReplaceable(leaf.x, leaf.y, leaf.z)) {
                world.setBlock(
                    leaf.x,
                    leaf.y,
                    leaf.z,
                    "regions_unexplored:cobalt_webbing[distance=1,persistent=false,waterlogged=false]");
            }
        }
    }

    private static void placeCobaltLog(ModernDecorationEngine engine, World world, Pos position, String axis) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        String block = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        if ("regions_unexplored:glistering_nylium".equals(block) || "regions_unexplored:mycotoxic_moss".equals(block)
            || "regions_unexplored:brimsprout_nylium".equals(block)) {
            world.setBlock(position.x, position.y, position.z, "minecraft:netherrack");
        }
        block = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        if ("regions_unexplored:cobalt_nylium".equals(block)) {
            world.setBlock(position.x, position.y, position.z, "minecraft:blackstone");
        } else if (engine.isBlockInTag(
            "regions_unexplored:replaceable_blocks",
            world.blockState(position.x, position.y, position.z))) {
                world.setBlock(position.x, position.y, position.z, "regions_unexplored:cobalt_log[axis=" + axis + "]");
            } else {
                return;
            }

        String below = ModernDecorationEngine.blockName(world.blockState(position.x, position.y - 1, position.z));
        if ("regions_unexplored:glistering_nylium".equals(below) || "regions_unexplored:mycotoxic_moss".equals(below)
            || "regions_unexplored:brimsprout_nylium".equals(below)) {
            world.setBlock(position.x, position.y - 1, position.z, "minecraft:netherrack");
        }
        below = ModernDecorationEngine.blockName(world.blockState(position.x, position.y - 1, position.z));
        if ("regions_unexplored:cobalt_nylium".equals(below)) {
            world.setBlock(position.x, position.y - 1, position.z, "minecraft:blackstone");
        } else if (engine.isBlockInTag(
            "regions_unexplored:replaceable_blocks",
            world.blockState(position.x, position.y - 1, position.z))) {
                world.setBlock(
                    position.x,
                    position.y - 1,
                    position.z,
                    "regions_unexplored:cobalt_log[axis=" + axis + "]");
            }
    }

    private boolean bamboo(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        for (int y = 0; y <= height; y++) {
            if (!saguaroReplaceable(engine, world, origin.offset(0, y, 0))) return false;
        }
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            placeBambooLog(engine, config, world, random, position);
            if (y == height) bambooLeaves(engine, config, world, random, position);
        }
        return true;
    }

    private void bambooLeaves(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos top) {
        Pos layer = top.offset(0, -3, 0);
        bambooLeafPattern(engine, config, world, random, layer, 0);
        layer = layer.offset(0, 1, 0);
        bambooLeafPattern(engine, config, world, random, layer, 1);
        layer = layer.offset(0, 1, 0);
        bambooLeafPattern(engine, config, world, random, layer, 2);
        layer = layer.offset(0, 1, 0);
        bambooLeafPattern(engine, config, world, random, layer, 3);
        layer = layer.offset(0, 1, 0);
        bambooLeafPattern(engine, config, world, random, layer, 2);
        layer = layer.offset(0, 1, 0);
        bambooLeafPattern(engine, config, world, random, layer, 1);
        layer = layer.offset(0, 1, 0);
        bambooLeafPattern(engine, config, world, random, layer, 0);
        layer = layer.offset(0, 1, 0);
        bambooLeafPattern(engine, config, world, random, layer, 0);
        layer = layer.offset(0, 1, 0);
        placeBambooLeaf(engine, config, world, random, layer);
        placeBambooLeaf(engine, config, world, random, layer.offset(0, 1, 0));
    }

    /** Pattern 0=cross, 1=cross plus random corners, 2=3x3, 3=the 21-block wide crown. */
    private void bambooLeafPattern(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, int pattern) {
        int[][] cross = { { 0, 0 }, { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        for (int[] offset : cross) {
            placeBambooLeaf(engine, config, world, random, center.offset(offset[0], 0, offset[1]));
        }
        if (pattern == 1 || pattern == 2 || pattern == 3) {
            int[][] corners = { { 1, -1 }, { -1, -1 }, { 1, 1 }, { -1, 1 } };
            for (int[] offset : corners) {
                if (pattern != 1 || random.nextInt(3) == 0) {
                    placeBambooLeaf(engine, config, world, random, center.offset(offset[0], 0, offset[1]));
                }
            }
        }
        if (pattern == 3) {
            int[][] rim = { { 0, -2 }, { 1, -2 }, { -1, -2 }, { 0, 2 }, { 1, 2 }, { -1, 2 }, { 2, 0 }, { 2, -1 },
                { 2, 1 }, { -2, 0 }, { -2, -1 }, { -2, 1 } };
            for (int[] offset : rim) {
                placeBambooLeaf(engine, config, world, random, center.offset(offset[0], 0, offset[1]));
            }
        }
    }

    private void placeBambooLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        String block = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = bambooGrassReplacement(block);
        if (replacement != null) {
            world.setBlock(position.x, position.y, position.z, replacement);
        } else if (saguaroReplaceable(engine, world, position)) {
            world.setBlock(
                position.x,
                position.y,
                position.z,
                features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position));
        }
        String below = ModernDecorationEngine.blockName(world.blockState(position.x, position.y - 1, position.z));
        String belowReplacement = bambooGrassReplacement(below);
        if (belowReplacement != null) {
            world.setBlock(position.x, position.y - 1, position.z, belowReplacement);
        }
    }

    private void placeBambooLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()
            || !world.isReplaceable(position.x, position.y, position.z)) return;
        String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
        world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "distance", "1"));
    }

    private static String bambooGrassReplacement(String block) {
        if ("regions_unexplored:peat_grass_block".equals(block)) {
            return "regions_unexplored:peat_podzol";
        }
        if ("regions_unexplored:silt_grass_block".equals(block)) {
            return "regions_unexplored:silt_podzol";
        }
        if ("regions_unexplored:alpha_grass_block".equals(block) || "minecraft:grass_block".equals(block))
            return "minecraft:podzol";
        return null;
    }

    private boolean largeSocotra(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        for (int y = 0; y <= height; y++) {
            if (!saguaroReplaceable(engine, world, origin.offset(0, y, 0))) return false;
        }
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            placeLargeSocotraLog(engine, config, world, random, position, "y");
            if (y == 0) palmRoot(engine, config, world, random, position);
            if (y == height) largeSocotraTop(engine, config, world, random, position);
        }
        return true;
    }

    private void largeSocotraTop(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos top) {
        largeSocotraBlob(engine, config, world, random, top);
        Pos level = top.offset(0, -1, 0);
        int[][] cardinals = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        String[] axes = { "z", "z", "x", "x" };
        int[][] firstSides = { { 1, -3 }, { 1, 3 }, { 3, -1 }, { -3, -1 } };
        int[][] secondSides = { { -1, -3 }, { -1, 3 }, { 3, 1 }, { -3, 1 } };
        for (int index = 0; index < cardinals.length; index++) {
            int dx = cardinals[index][0];
            int dz = cardinals[index][1];
            placeLargeSocotraLog(engine, config, world, random, level.offset(dx, 0, dz), axes[index]);
            Pos tip = level.offset(dx * 2, 0, dz * 2);
            placeLargeSocotraLog(engine, config, world, random, tip, axes[index]);
            largeSocotraBlob(engine, config, world, random, tip);
        }

        level = level.offset(0, -1, 0);
        int[][] diagonal = { { 2, -2 }, { -2, -2 }, { 2, 2 }, { -2, 2 } };
        String[] diagonalAxes = { "z", "x", "x", "z" };
        for (int index = 0; index < diagonal.length; index++) {
            Pos tip = level.offset(diagonal[index][0], 0, diagonal[index][1]);
            placeLargeSocotraLog(engine, config, world, random, tip, diagonalAxes[index]);
            largeSocotraBlob(engine, config, world, random, tip);
        }

        level = level.offset(0, -1, 0);
        placeLargeSocotraLog(engine, config, world, random, level.offset(1, 0, -1), "z");
        placeLargeSocotraLog(engine, config, world, random, level.offset(-1, 0, -1), "x");
        placeLargeSocotraLog(engine, config, world, random, level.offset(1, 0, 1), "x");
        placeLargeSocotraLog(engine, config, world, random, level.offset(-1, 0, 1), "z");
        for (int index = 0; index < cardinals.length; index++) {
            int dx = cardinals[index][0];
            int dz = cardinals[index][1];
            placeLargeSocotraLog(engine, config, world, random, level.offset(dx * 3, 0, dz * 3), axes[index]);
            Pos far = level.offset(dx * 4, 0, dz * 4);
            placeLargeSocotraLog(engine, config, world, random, far, axes[index]);
            largeSocotraBlob(engine, config, world, random, far);

            Pos clockwise = level.offset(firstSides[index][0], 0, firstSides[index][1]);
            Pos counterclockwise = level.offset(secondSides[index][0], 0, secondSides[index][1]);
            String sideAxis = (dx == 0) ? "x" : "z";
            placeLargeSocotraLog(engine, config, world, random, clockwise, sideAxis);
            largeSocotraBlob(engine, config, world, random, clockwise);
            placeLargeSocotraLog(engine, config, world, random, counterclockwise, sideAxis);
            largeSocotraBlob(engine, config, world, random, counterclockwise);
        }

        level = level.offset(0, -1, 0);
        for (int index = 0; index < cardinals.length; index++) {
            int dx = cardinals[index][0];
            int dz = cardinals[index][1];
            placeLargeSocotraLog(engine, config, world, random, level.offset(dx * 2, 0, dz * 2), axes[index]);
            placeLargeSocotraLog(engine, config, world, random, level.offset(dx * 3, 0, dz * 3), axes[index]);
        }
        level = level.offset(0, -1, 0);
        for (int index = 0; index < cardinals.length; index++) {
            placeLargeSocotraLog(
                engine,
                config,
                world,
                random,
                level.offset(cardinals[index][0], 0, cardinals[index][1]),
                axes[index]);
        }
    }

    private void largeSocotraBlob(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        int[][] mandatory = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 }, { 1, 0, -1 },
            { -1, 0, -1 }, { 1, 0, 1 }, { -1, 0, 1 }, { 0, 1, 0 }, { 0, 1, -1 }, { 0, 1, 1 }, { 1, 1, 0 },
            { -1, 1, 0 } };
        for (int[] offset : mandatory) {
            placeLargeSocotraLeaf(engine, config, world, random, center.offset(offset[0], offset[1], offset[2]));
        }
        int[][] optional = { { 1, 1, -1 }, { -1, 1, -1 }, { 1, 1, 1 }, { -1, 1, 1 } };
        for (int[] offset : optional) {
            if (random.nextInt(5) == 0) {
                placeLargeSocotraLeaf(engine, config, world, random, center.offset(offset[0], offset[1], offset[2]));
            }
        }
    }

    private void placeLargeSocotraLog(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, Pos position, String axis) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        String block = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = saguaroGrassReplacement(block);
        if (replacement != null) {
            world.setBlock(position.x, position.y, position.z, replacement);
        } else if (saguaroReplaceable(engine, world, position)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
            world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "axis", axis));
        } else {
            return;
        }
        String below = ModernDecorationEngine.blockName(world.blockState(position.x, position.y - 1, position.z));
        String belowReplacement = saguaroGrassReplacement(below);
        if (belowReplacement != null) {
            world.setBlock(position.x, position.y - 1, position.z, belowReplacement);
        }
    }

    private void placeLargeSocotraLeaf(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, Pos position) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()
            || !world.isReplaceable(position.x, position.y, position.z)) return;
        String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
        world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "distance", "1"));
    }

    private boolean palm(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        int bendChance = random.nextInt(3);
        int dx = 0;
        int dz = 0;
        if (random.nextInt(4) == 0) dz = -1;
        else if (random.nextInt(4) == 0) dz = 1;
        else if (random.nextInt(4) == 0) dx = 1;
        else dx = -1;

        Pos checked = origin;
        for (int y = 0; y <= height; y++) {
            if (!saguaroReplaceable(engine, world, checked)) return false;
            checked = checked.offset(0, 1, 0);
            if (y % 3 == 0 && bendChance != 0) checked = checked.offset(dx, 0, dz);
        }

        Pos position = origin;
        for (int y = 0; y <= height; y++) {
            placePalmLog(engine, config, world, random, position);
            if (y == 0) {
                palmRoot(engine, config, world, random, position);
                if (bendChance != 0) {
                    // Pinned RU still draws this value even though the only statement in the
                    // conditional was commented out. Omitting it shifts the shared decoration
                    // RNG, changing every subsequent palm/tree origin in the chunk.
                    random.nextInt(2);
                }
            }
            if (y == height) palmTop(engine, config, world, random, position);
            position = position.offset(0, 1, 0);
            if (y != 0 && y % 3 == 0 && bendChance != 0) {
                position = position.offset(dx, 0, dz);
            }
        }
        return true;
    }

    private void palmTop(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        int[][] branchOffsets = { { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 } };
        String[] facings = { "north", "south", "east", "west" };
        for (int index = 0; index < branchOffsets.length; index++) {
            Pos branch = center.offset(branchOffsets[index][0], 0, branchOffsets[index][2]);
            if (world.isReplaceable(branch.x, branch.y, branch.z)) {
                String state = features
                    .stateProvider(engine, config.getAsJsonObject("branch_provider"), random, center);
                world.setBlock(
                    branch.x,
                    branch.y,
                    branch.z,
                    ModernFeatureEngine.property(state, "facing", facings[index]));
            }
        }
        int[][] leaves = { { -1, 0, -1 }, { 1, 0, -1 }, { -1, 0, 1 }, { 1, 0, 1 }, { 0, 1, 0 }, { 0, 1, -1 },
            { 0, 1, 1 }, { 1, 1, 0 }, { -1, 1, 0 }, { 1, 1, -1 }, { -1, 1, -1 }, { 1, 1, 1 }, { -1, 1, 1 },
            { 0, 2, -1 }, { 0, 2, 1 }, { 1, 2, 0 }, { -1, 2, 0 }, { 0, 2, -2 }, { 0, 2, 2 }, { 2, 2, 0 }, { -2, 2, 0 },
            { 0, 3, -2 }, { 0, 3, 2 }, { 2, 3, 0 }, { -2, 3, 0 }, { 0, 2, -3 }, { 0, 2, 3 }, { 3, 2, 0 }, { -3, 2, 0 },
            { 0, 1, -3 }, { 0, 1, 3 }, { 3, 1, 0 }, { -3, 1, 0 } };
        for (int[] offset : leaves) {
            placePalmLeaf(engine, config, world, random, center.offset(offset[0], offset[1], offset[2]));
        }
    }

    private void placePalmLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        String block = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = saguaroGrassReplacement(block);
        if (replacement != null) {
            world.setBlock(position.x, position.y, position.z, replacement);
        } else if (saguaroReplaceable(engine, world, position)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
            world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "axis", "y"));
        } else {
            return;
        }
        String below = ModernDecorationEngine.blockName(world.blockState(position.x, position.y - 1, position.z));
        String belowReplacement = saguaroGrassReplacement(below);
        if (belowReplacement != null) {
            world.setBlock(position.x, position.y - 1, position.z, belowReplacement);
        }
    }

    private void placePalmLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()
            || !world.isReplaceable(position.x, position.y, position.z)) return;
        String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
        world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "distance", "1"));
    }

    private void palmRoot(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int depth = new Random().nextInt(2) + 4;
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
                placePalmLog(engine, config, world, random, position);
            } else {
                break;
            }
        }
    }

    private boolean smallSocotra(ModernDecorationEngine engine, World world, ModernRandom random, Pos origin) {
        if (!saguaroReplaceable(engine, world, origin)) return false;
        Pos position = origin;
        if (random.nextInt(3) == 0) {
            placeSocotraLog(engine, world, position, "y");
            position = position.offset(0, 1, 0);
        }
        placeSocotraLog(engine, world, position, "y");
        position = position.offset(0, 1, 0);
        placeSocotraLog(engine, world, position, "y");
        position = position.offset(0, 1, 0);
        placeSocotraLog(engine, world, position, "y");
        int[][] directions = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        String[] axes = { "z", "z", "x", "x" };
        for (int index = 0; index < directions.length; index++) {
            Pos branch = position.offset(directions[index][0], 0, directions[index][1]);
            placeSocotraLog(engine, world, branch, axes[index]);
            socotraTop(world, random, branch);
        }
        position = position.offset(0, 1, 0);
        placeSocotraLog(engine, world, position, "y");
        socotraTop(world, random, position);
        return true;
    }

    private static void socotraTop(World world, ModernRandom random, Pos center) {
        int[][] mandatory = { { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 }, { 1, 0, -1 }, { -1, 0, -1 },
            { 1, 0, 1 }, { -1, 0, 1 }, { 0, 1, 0 }, { 0, 1, -1 }, { 0, 1, 1 }, { 1, 1, 0 }, { -1, 1, 0 } };
        for (int[] offset : mandatory) {
            placeSocotraLeaf(world, center.offset(offset[0], offset[1], offset[2]));
        }
        int[][] optional = { { 1, 1, -1 }, { -1, 1, -1 }, { 1, 1, 1 }, { -1, 1, 1 } };
        for (int[] offset : optional) {
            if (random.nextInt(4) == 0) {
                placeSocotraLeaf(world, center.offset(offset[0], offset[1], offset[2]));
            }
        }
    }

    private static void placeSocotraLog(ModernDecorationEngine engine, World world, Pos position, String axis) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        String block = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = saguaroGrassReplacement(block);
        if (replacement != null) {
            world.setBlock(position.x, position.y, position.z, replacement);
        } else if (saguaroReplaceable(engine, world, position)) {
            world.setBlock(position.x, position.y, position.z, "regions_unexplored:socotra_log[axis=" + axis + "]");
        } else {
            return;
        }
        String below = ModernDecorationEngine.blockName(world.blockState(position.x, position.y - 1, position.z));
        String belowReplacement = saguaroGrassReplacement(below);
        if (belowReplacement != null) {
            world.setBlock(position.x, position.y - 1, position.z, belowReplacement);
        }
    }

    private static void placeSocotraLeaf(World world, Pos position) {
        if (position.y >= world.minBuildHeight() && position.y < world.maxBuildHeight()
            && world.isReplaceable(position.x, position.y, position.z)) {
            world.setBlock(
                position.x,
                position.y,
                position.z,
                "regions_unexplored:socotra_leaves[distance=1,persistent=false,waterlogged=false]");
        }
    }

    private boolean saguaro(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        Pos neighborCheck = origin.offset(0, 3, 0);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if ((dx != 0 || dz != 0) && "regions_unexplored:saguaro_cactus".equals(
                    ModernDecorationEngine
                        .blockName(world.blockState(neighborCheck.x + dx, neighborCheck.y, neighborCheck.z + dz)))) {
                    return false;
                }
            }
        }
        for (int y = 0; y <= height; y++) {
            if (!saguaroReplaceable(engine, world, origin.offset(0, y, 0))) return false;
        }
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            placeSaguaro(engine, config, world, random, position, "up_down");
            if (y == 0) {
                // placeRoot cannot descend through the normal sand substrate, but the call is
                // retained because it is observable on unusual RU dirt/replacement platforms.
                saguaroRoot(engine, config, world, random, position);
                saguaroLimbs(engine, config, world, random, position);
            }
            if (y == height) saguaroFlower(engine, config, world, random, position.offset(0, 1, 0));
        }
        return true;
    }

    private void saguaroLimbs(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int[] heights = { random.nextInt(2) + 3, random.nextInt(2) + 3, random.nextInt(2) + 3, random.nextInt(2) + 3 };
        int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, -1 }, { 0, 1 } };
        String[] horizontal = { "east_west", "east_west", "north_south", "north_south" };
        String[] elbows = { "west_up", "east_up", "south_up", "north_up" };
        for (int index = 0; index < directions.length; index++) {
            int dx = directions[index][0];
            int dz = directions[index][1];
            Pos base = origin.offset(dx, heights[index], dz);
            placeSaguaro(engine, config, world, random, base, horizontal[index]);
            Pos upright = base.offset(dx, 0, dz);
            placeSaguaro(engine, config, world, random, upright, elbows[index]);
            placeSaguaro(engine, config, world, random, upright.offset(0, 1, 0), "up_down");
            placeSaguaro(engine, config, world, random, upright.offset(0, 2, 0), "up_down");
            saguaroFlower(engine, config, world, random, upright.offset(0, 3, 0));
        }
    }

    private void saguaroRoot(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int depth = new Random().nextInt(2) + 4;
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
                placeSaguaro(engine, config, world, random, position, "up_down");
            } else {
                break;
            }
        }
    }

    private boolean placeSaguaro(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, String shape) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return true;
        String block = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = saguaroGrassReplacement(block);
        if (replacement != null) {
            world.setBlock(position.x, position.y, position.z, replacement);
        } else if (saguaroReplaceable(engine, world, position)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
            world.setBlock(
                position.x,
                position.y,
                position.z,
                ModernFeatureEngine.property(state, "cactus_shape", shape));
        } else {
            return true;
        }
        String belowBlock = ModernDecorationEngine.blockName(world.blockState(position.x, position.y - 1, position.z));
        String belowReplacement = saguaroGrassReplacement(belowBlock);
        if (belowReplacement != null) {
            world.setBlock(position.x, position.y - 1, position.z, belowReplacement);
        }
        return true;
    }

    private void saguaroFlower(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        if (world.isReplaceable(position.x, position.y, position.z) && random.nextInt(4) == 0) {
            world.setBlock(
                position.x,
                position.y,
                position.z,
                features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position));
        }
    }

    private static String saguaroGrassReplacement(String block) {
        if ("regions_unexplored:peat_grass_block".equals(block)) {
            return "regions_unexplored:peat_dirt";
        }
        if ("regions_unexplored:silt_grass_block".equals(block)) {
            return "regions_unexplored:silt_dirt";
        }
        if ("regions_unexplored:alpha_grass_block".equals(block) || "minecraft:grass_block".equals(block))
            return "minecraft:dirt";
        return null;
    }

    private static boolean saguaroReplaceable(ModernDecorationEngine engine, World world, Pos position) {
        return position.y >= world.minBuildHeight() && position.y < world.maxBuildHeight()
            && engine.isBlockInTag(
                "regions_unexplored:replaceable_blocks",
                world.blockState(position.x, position.y, position.z));
    }

    private boolean smallJoshua(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        for (int y = 0; y <= height + 2; y++) {
            if (!treeReplaceable(engine, world, origin.offset(0, y, 0))) return false;
        }
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            placeAshenLog(engine, config, world, random, position, "y");
            if (y == height) {
                smallJoshuaBranches(engine, config, world, random, position);
                placeJoshuaLeaves(engine, config, world, random, position.offset(0, 1, 0));
            }
        }
        return true;
    }

    private void smallJoshuaBranches(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        String[] directions = { "north", "south", "east", "west" };
        int[][] offsets = { { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 } };
        for (int index = 0; index < directions.length; index++) {
            Pos branch = position.offset(offsets[index][0], 0, offsets[index][2]);
            if (!treeReplaceable(engine, world, branch)) continue;
            String state = features.stateProvider(engine, config.getAsJsonObject("branch_provider"), random, position);
            world.setBlock(
                branch.x,
                branch.y,
                branch.z,
                ModernFeatureEngine.property(state, "facing", directions[index]));
        }
    }

    private void placeJoshuaLeaves(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos lower) {
        Pos upper = lower.offset(0, 1, 0);
        if (!world.isReplaceable(lower.x, lower.y, lower.z) || !world.isReplaceable(upper.x, upper.y, upper.z)
            || upper.y >= world.maxBuildHeight()) return;
        String lowerState = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, lower);
        lowerState = ModernFeatureEngine.property(lowerState, "natural", "true");
        lowerState = ModernFeatureEngine.property(lowerState, "half", "lower");
        String upperState = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, upper);
        upperState = ModernFeatureEngine.property(upperState, "natural", "true");
        upperState = ModernFeatureEngine.property(upperState, "half", "upper");
        world.setBlock(lower.x, lower.y, lower.z, lowerState);
        world.setBlock(upper.x, upper.y, upper.z, upperState);
    }

    private boolean mediumJoshua(World world, ModernRandom random, Pos origin) {
        int height = random.nextInt(3) + 6;
        for (int y = 0; y <= height; y++) {
            if (origin.y + y >= world.maxBuildHeight() || !world.isReplaceable(origin.x, origin.y + y, origin.z))
                return false;
            if (y == height && origin.y + y + 6 >= world.maxBuildHeight()) return false;
        }
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            if (!world.isReplaceable(position.x, position.y, position.z)) return true;
            world.setBlock(position.x, position.y, position.z, "regions_unexplored:joshua_log[axis=y]");
            if (y == height) {
                hardcodedJoshuaCrown(world, position);
                mediumJoshuaBranches(world, position.offset(0, -3, 0));
            }
        }
        return true;
    }

    private boolean largeJoshua(World world, ModernRandom random, Pos origin) {
        int height = random.nextInt(5) + 8;
        for (int y = 0; y <= height; y++) {
            if (origin.y + y >= world.maxBuildHeight() || !world.isReplaceable(origin.x, origin.y + y, origin.z))
                return false;
            if (y == height && origin.y + y + 6 >= world.maxBuildHeight()) return false;
        }
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            if (!world.isReplaceable(position.x, position.y, position.z)) return true;
            world.setBlock(position.x, position.y, position.z, "regions_unexplored:joshua_log[axis=y]");
            if (y == height) {
                hardcodedJoshuaCrown(world, position);
                largeJoshuaCardinalBranches(world, position.offset(0, -5, 0));
                largeJoshuaDiagonalBranches(world, position.offset(0, -6, 0));
            }
        }
        return true;
    }

    private void largeJoshuaCardinalBranches(World world, Pos base) {
        Random random = industries.angelic.biomic.world.replay.RuIncidentalRandom.random(
            world.worldSeed(),
            base.x,
            base.y,
            base.z,
            industries.angelic.biomic.world.replay.RuIncidentalRandom.JOSHUA_CARDINAL);
        int[] vertical = { random.nextInt(2), random.nextInt(2), random.nextInt(2), random.nextInt(2) };
        int[][] directions = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        String[] axes = { "z", "z", "x", "x" };
        for (int index = 0; index < directions.length; index++) {
            int dx = directions[index][0];
            int dz = directions[index][1];
            Pos shifted = base.offset(0, vertical[index], 0);
            placeIfReplaceable(
                world,
                shifted.offset(dx, 0, dz),
                "regions_unexplored:joshua_log[axis=" + axes[index] + "]");
            placeIfReplaceable(world, shifted.offset(dx * 2, 1, dz * 2), "regions_unexplored:joshua_log[axis=y]");
            placeIfReplaceable(world, shifted.offset(dx * 3, 2, dz * 3), "regions_unexplored:joshua_log[axis=y]");
            Pos tip = shifted.offset(dx * 3, 3, dz * 3);
            if (placeIfReplaceable(world, tip, "regions_unexplored:joshua_log[axis=y]")) {
                hardcodedJoshuaCrown(world, tip);
            }
        }
    }

    private void largeJoshuaDiagonalBranches(World world, Pos base) {
        Random random = industries.angelic.biomic.world.replay.RuIncidentalRandom.random(
            world.worldSeed(),
            base.x,
            base.y,
            base.z,
            industries.angelic.biomic.world.replay.RuIncidentalRandom.JOSHUA_DIAGONAL);
        int northY = random.nextInt(3);
        int southY = random.nextInt(3);
        random.nextInt(3); // upstream computes but never uses east
        random.nextInt(3); // upstream computes but never uses west
        int[][] directions = { { -1, -1 }, { 1, -1 }, { -1, 1 }, { 1, 1 } };
        for (int[] direction : directions) {
            int vertical = direction[1] < 0 ? northY : southY;
            Pos shifted = base.offset(0, vertical, 0);
            placeIfReplaceable(
                world,
                shifted.offset(direction[0], 0, direction[1]),
                "regions_unexplored:joshua_log[axis=y]");
            placeIfReplaceable(
                world,
                shifted.offset(direction[0] * 2, 1, direction[1] * 2),
                "regions_unexplored:joshua_log[axis=y]");
            Pos tip = shifted.offset(direction[0] * 2, 2, direction[1] * 2);
            if (placeIfReplaceable(world, tip, "regions_unexplored:joshua_log[axis=y]")) {
                hardcodedJoshuaCrown(world, tip);
            }
        }
    }

    private void mediumJoshuaBranches(World world, Pos base) {
        int[][] directions = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        String[] axes = { "z", "z", "x", "x" };
        for (int index = 0; index < directions.length; index++) {
            int dx = directions[index][0];
            int dz = directions[index][1];
            placeIfReplaceable(
                world,
                base.offset(dx, 0, dz),
                "regions_unexplored:joshua_log[axis=" + axes[index] + "]");
            placeIfReplaceable(world, base.offset(dx * 2, 1, dz * 2), "regions_unexplored:joshua_log[axis=y]");
            Pos tip = base.offset(dx * 2, 2, dz * 2);
            if (placeIfReplaceable(world, tip, "regions_unexplored:joshua_log[axis=y]")) {
                hardcodedJoshuaCrown(world, tip);
            }
        }
    }

    private static void hardcodedJoshuaCrown(World world, Pos center) {
        String[] directions = { "north", "south", "east", "west" };
        int[][] offsets = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        for (int index = 0; index < directions.length; index++) {
            Pos beard = center.offset(offsets[index][0], 0, offsets[index][1]);
            placeIfReplaceable(world, beard, "regions_unexplored:joshua_beard[facing=" + directions[index] + "]");
        }
        placeIfReplaceable(
            world,
            center.offset(0, 1, 0),
            "regions_unexplored:joshua_leaves[half=lower,natural=true,waterlogged=false]");
        placeIfReplaceable(
            world,
            center.offset(0, 2, 0),
            "regions_unexplored:joshua_leaves[half=upper,natural=true,waterlogged=false]");
    }

    private static boolean placeIfReplaceable(World world, Pos position, String state) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()
            || !world.isReplaceable(position.x, position.y, position.z)) return false;
        world.setBlock(position.x, position.y, position.z, state);
        return true;
    }

    /** Exact AshenTreeFeature port; upstream deliberately uses fresh java.util.Random instances for leaves. */
    private boolean ashen(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        int minimumLeaves = random.nextInt(2) + 3;
        for (int y = 0; y <= height; y++) {
            if (!treeReplaceable(engine, world, origin.offset(0, y, 0))) return false;
        }
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            placeAshenLog(engine, config, world, random, position, "y");
            if (y >= minimumLeaves) {
                maybeAshenLeaf(engine, config, world, random, position.offset(0, 0, -1));
                maybeAshenLeaf(engine, config, world, random, position.offset(0, 0, 1));
                maybeAshenLeaf(engine, config, world, random, position.offset(1, 0, 0));
                maybeAshenLeaf(engine, config, world, random, position.offset(-1, 0, 0));
            }
            if (y == height) {
                ashenTop(engine, config, world, random, position);
                ashenBranches(engine, config, world, random, position, height, minimumLeaves);
            }
        }
        return true;
    }

    private void ashenBranches(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos top, int height, int minimumLeaves) {
        int bound = height - minimumLeaves;
        int north = random.nextInt(bound);
        int south = random.nextInt(bound);
        int east = random.nextInt(bound);
        int west = random.nextInt(bound);
        Pos n = top.offset(0, -north, -1);
        Pos s = top.offset(0, -south, 1);
        Pos e = top.offset(1, -east, 0);
        Pos w = top.offset(-1, -west, 0);
        placeAshenLog(engine, config, world, random, n, "z");
        ashenBranchLeaves(engine, config, world, random, n, 0, -1);
        placeAshenLog(engine, config, world, random, s, "z");
        ashenBranchLeaves(engine, config, world, random, s, 0, 1);
        placeAshenLog(engine, config, world, random, e, "x");
        ashenBranchLeaves(engine, config, world, random, e, 1, 0);
        placeAshenLog(engine, config, world, random, w, "x");
        ashenBranchLeaves(engine, config, world, random, w, -1, 0);
    }

    private void ashenBranchLeaves(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos branch, int outwardX, int outwardZ) {
        placeAshenLeaf(engine, config, world, random, branch.offset(outwardX, 0, outwardZ));
        if (outwardX == 0) {
            placeAshenLeaf(engine, config, world, random, branch.offset(1, 0, 0));
            placeAshenLeaf(engine, config, world, random, branch.offset(-1, 0, 0));
        } else {
            placeAshenLeaf(engine, config, world, random, branch.offset(0, 0, -1));
            placeAshenLeaf(engine, config, world, random, branch.offset(0, 0, 1));
        }
    }

    private void ashenTop(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos top) {
        placeAshenLeaf(engine, config, world, random, top);
        placeAshenLeaf(engine, config, world, random, top.offset(0, 1, 0));
        placeAshenLeaf(engine, config, world, random, top.offset(0, 2, 0));
        placeAshenLeaf(engine, config, world, random, top.offset(0, 0, -1));
        placeAshenLeaf(engine, config, world, random, top.offset(0, 0, 1));
        placeAshenLeaf(engine, config, world, random, top.offset(1, 0, 0));
        placeAshenLeaf(engine, config, world, random, top.offset(-1, 0, 0));
    }

    private void maybeAshenLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (industries.angelic.biomic.world.replay.RuIncidentalRandom.sample(
            world.worldSeed(),
            position.x,
            position.y,
            position.z,
            industries.angelic.biomic.world.replay.RuIncidentalRandom.LEAF_BLOB,
            3) == 0) {
            placeAshenLeaf(engine, config, world, random, position);
        }
    }

    private void placeAshenLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()
            || !world.isReplaceable(position.x, position.y, position.z)) return;
        String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
        world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "distance", "1"));
    }

    private void placeAshenLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, String axis) {
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        String current = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        if ("regions_unexplored:peat_grass_block".equals(current)) {
            world.setBlock(position.x, position.y, position.z, "regions_unexplored:peat_dirt");
        } else if ("regions_unexplored:silt_grass_block".equals(current)) {
            world.setBlock(position.x, position.y, position.z, "regions_unexplored:silt_dirt");
        } else if ("regions_unexplored:alpha_grass_block".equals(current) || "minecraft:grass_block".equals(current)) {
            world.setBlock(position.x, position.y, position.z, "minecraft:dirt");
        } else if (treeReplaceable(engine, world, position)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
            world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "axis", axis));
        }
        convertGrassBelow(world, position);
    }

    private static void convertGrassBelow(World world, Pos position) {
        Pos below = position.offset(0, -1, 0);
        String block = ModernDecorationEngine.blockName(world.blockState(below.x, below.y, below.z));
        if ("regions_unexplored:peat_grass_block".equals(block)) {
            world.setBlock(below.x, below.y, below.z, "regions_unexplored:peat_dirt");
        } else if ("regions_unexplored:silt_grass_block".equals(block)) {
            world.setBlock(below.x, below.y, below.z, "regions_unexplored:silt_dirt");
        } else if ("regions_unexplored:alpha_grass_block".equals(block) || "minecraft:grass_block".equals(block)) {
            world.setBlock(below.x, below.y, below.z, "minecraft:dirt");
        }
    }

    private static boolean treeReplaceable(ModernDecorationEngine engine, World world, Pos position) {
        return position.y >= world.minBuildHeight() && position.y < world.maxBuildHeight()
            && engine.isBlockInTag(
                "#regions_unexplored:replaceable_blocks",
                world.blockState(position.x, position.y, position.z));
    }
}
