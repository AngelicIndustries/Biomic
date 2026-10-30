package industries.angelic.biomic.world.modern;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Direct port of pinned RU's normal and golden large larch trees. */
final class ModernRuLarchFeature {

    private static final int[][] DIRECTIONS = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
    private static final String[] FACINGS = { "north", "south", "east", "west" };

    private final ModernFeatureEngine features;

    ModernRuLarchFeature(ModernFeatureEngine features) {
        this.features = features;
    }

    boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        for (int y = 0; y <= height; y++) {
            Pos candidate = origin.offset(0, y, 0);
            boolean accepted = replaceable(engine, world, candidate);
            world.recordPlacementCandidate(
                candidate,
                -5,
                "larch_scan=" + world.blockState(candidate.x, candidate.y, candidate.z) + ";accepted=" + accepted);
            if (!accepted) return false;
        }
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            placeLog(engine, config, world, random, position, "y");
            if (y > 2) placeBranchDecorator(engine, config, world, random, position);
            if (y == 0) placeRoot(engine, config, world, random, position);
            if (y == height) placeTop(engine, config, world, random, position);
        }
        return true;
    }

    private void placeTop(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos top) {
        Pos position = top.offset(0, -6, 0);
        placeTopLeaves(engine, config, world, random, top);
        placeBranchesShort(engine, config, world, random, position);
        position = position.offset(0, -3, 0);
        placeBranchesLong(engine, config, world, random, position);
        position = position.offset(0, -3, 0);
        placeBranchesLong(engine, config, world, random, position);
        position = position.offset(0, -3, 0);
        if (random.nextInt(3) == 0) {
            placeBranchesLong(engine, config, world, random, position);
            position = position.offset(0, -2, 0);
            placeBranchesShort(engine, config, world, random, position);
        } else {
            position = position.offset(0, 1, 0);
            placeBranchesShort(engine, config, world, random, position);
        }
    }

    private void placeBranchesShort(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        random.nextInt(2); // pinned source computes an unused type
        for (int direction = 0; direction < DIRECTIONS.length; direction++) {
            int dx = DIRECTIONS[direction][0];
            int dz = DIRECTIONS[direction][1];
            Pos endpoint = center.offset(dx, 0, dz);
            placeLog(engine, config, world, random, endpoint, direction < 2 ? "z" : "x");
            placeEndBlob(engine, config, world, random, endpoint, dx, dz);
        }
    }

    private void placeBranchesLong(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        random.nextInt(2); // pinned source computes an unused type
        for (int y = 0; y <= 2; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        placeLeaf(engine, config, world, random, center.offset(dx, y, dz));
                    }
                }
            }
        }
        for (int direction = 0; direction < DIRECTIONS.length; direction++) {
            int dx = DIRECTIONS[direction][0];
            int dz = DIRECTIONS[direction][1];
            String axis = direction < 2 ? "z" : "x";
            placeLog(engine, config, world, random, center.offset(dx, -1, dz), axis);
            placeLog(engine, config, world, random, center.offset(dx, 0, dz), axis);
            Pos endpoint = center.offset(dx * 2, 0, dz * 2);
            placeLog(engine, config, world, random, endpoint, axis);
            placeEndBlob(engine, config, world, random, endpoint, dx, dz);
        }
    }

    private void placeTopLeaves(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos top) {
        int variant = RuIncidentalRandom.sample(world.worldSeed(), top.x, top.y, top.z, RuIncidentalRandom.TOP, 2);
        if (variant == 0) {
            placeCross(engine, config, world, random, top, true);
            placeLeaf(engine, config, world, random, top.offset(0, 1, 0));
            placeLeaf(engine, config, world, random, top.offset(0, 2, 0));
            placeCross(engine, config, world, random, top.offset(0, -2, 0), false);
        } else {
            placeCross(engine, config, world, random, top.offset(0, 1, 0), true);
            placeLeaf(engine, config, world, random, top.offset(0, 2, 0));
            placeLeaf(engine, config, world, random, top.offset(0, 3, 0));
            placeCross(engine, config, world, random, top.offset(0, -1, 0), false);
            placeCross(engine, config, world, random, top.offset(0, -2, 0), false);
        }

        Pos lower = top.offset(0, -3, 0);
        placeLeaf(engine, config, world, random, lower);
        for (int direction = 0; direction < DIRECTIONS.length; direction++) {
            int dx = DIRECTIONS[direction][0];
            int dz = DIRECTIONS[direction][1];
            placeLog(engine, config, world, random, lower.offset(dx, 0, dz), direction < 2 ? "z" : "x");
        }
        int[][] leaves = { { 1, -1 }, { -1, -1 }, { 1, 1 }, { -1, 1 }, { 0, -2 }, { 1, -2 }, { -1, -2 }, { 0, 2 },
            { 1, 2 }, { -1, 2 }, { 2, 0 }, { 2, -1 }, { 2, 1 }, { -2, 0 }, { -2, -1 }, { -2, 1 } };
        for (int[] offset : leaves) {
            placeLeaf(engine, config, world, random, lower.offset(offset[0], 0, offset[1]));
        }
    }

    private void placeCross(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, boolean includeCenter) {
        if (includeCenter) placeLeaf(engine, config, world, random, center);
        for (int[] direction : DIRECTIONS) {
            placeLeaf(engine, config, world, random, center.offset(direction[0], 0, direction[1]));
        }
    }

    private void placeEndBlob(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, int dx, int dz) {
        int px = -dz;
        int pz = dx;
        for (int side = -1; side <= 1; side++) {
            placeLeaf(engine, config, world, random, center.offset(-dx + px * side, 0, -dz + pz * side));
        }
        for (int side = -2; side <= 2; side++) {
            placeLeaf(engine, config, world, random, center.offset(px * side, 0, pz * side));
            placeLeaf(engine, config, world, random, center.offset(dx + px * side, 0, dz + pz * side));
        }
        for (int side = -1; side <= 1; side++) {
            placeLeaf(engine, config, world, random, center.offset(dx * 2 + px * side, 0, dz * 2 + pz * side));
        }
        placeLeaf(engine, config, world, random, center.offset(0, 1, 0));
        placeLeaf(engine, config, world, random, center.offset(0, 2, 0));
        placeLeaf(engine, config, world, random, center.offset(px, 1, pz));
        placeLeaf(engine, config, world, random, center.offset(-px, 1, -pz));
        for (int side = -1; side <= 1; side++) {
            placeLeaf(engine, config, world, random, center.offset(dx + px * side, 1, dz + pz * side));
        }
    }

    private void placeBranchDecorator(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, Pos trunk) {
        if (random.nextInt(10) != 0) return;
        int direction = RuIncidentalRandom
            .sample(world.worldSeed(), trunk.x, trunk.y, trunk.z, RuIncidentalRandom.BRANCH, 4);
        int dx = DIRECTIONS[direction][0];
        int dz = DIRECTIONS[direction][1];
        Pos branch = trunk.offset(dx, 0, dz);
        if (replaceableByState(world, branch)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("branch_provider"), random, trunk);
            world.setBlock(
                branch.x,
                branch.y,
                branch.z,
                ModernFeatureEngine.property(state, "facing", FACINGS[direction]));
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
                placeLog(engine, config, world, random, position, "y");
            } else {
                break;
            }
        }
    }

    private void placeLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        if (!replaceableByState(world, position)) return;
        String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
        world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "distance", "1"));
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
        String replacementBelow = grassReplacement(
            ModernDecorationEngine.blockName(world.blockState(below.x, below.y, below.z)));
        if (replacementBelow != null) {
            world.setBlock(below.x, below.y, below.z, replacementBelow);
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
