package industries.angelic.biomic.world.modern;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Direct port of the pinned RU {@code KapokTreeFeature}. */
final class ModernRuKapokFeature {

    private static final int[][] CARDINALS = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
    private static final String[] AXES = { "z", "z", "x", "x" };
    private static final String[] FACINGS = { "north", "south", "east", "west" };

    private final ModernFeatureEngine features;

    ModernRuKapokFeature(ModernFeatureEngine features) {
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
            Pos trunk = origin.offset(0, y, 0);
            placeLog(engine, config, world, random, trunk, "y", false);
            if (y > 5 && random.nextInt(10) == 0) {
                placeBranch(
                    engine,
                    config,
                    world,
                    random,
                    trunk,
                    RuIncidentalRandom
                        .sample(world.worldSeed(), trunk.x, trunk.y, trunk.z, RuIncidentalRandom.BRANCH, 4));
            }
            if (y == 0) placeButtressRoots(engine, config, world, random, trunk);
            if (y == height) placeTop(engine, config, world, random, trunk);
        }
        return true;
    }

    private void placeButtressRoots(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        placeRoot(engine, config, world, random, origin);
        // Preserve the source's height-major call order. Each call mutates the blocks inspected
        // by the next one, so grouping an entire cardinal root before moving to the next cardinal
        // produces a materially different buttress even though the position set looks equivalent.
        for (int y = 0; y <= 4; y++) {
            for (int[] direction : CARDINALS) {
                placeRoot(engine, config, world, random, origin.offset(direction[0], y, direction[1]));
            }
        }
        for (int[] direction : CARDINALS) {
            if (random.nextInt(3) == 0) {
                placeRoot(engine, config, world, random, origin.offset(direction[0], 5, direction[1]));
            }
        }
        for (int y = 0; y <= 2; y++) {
            for (int[] direction : CARDINALS) {
                placeRoot(engine, config, world, random, origin.offset(direction[0] * 2, y, direction[1] * 2));
            }
        }
        // Preserve the pinned copy/paste bug: east is checked and rooted twice; west is absent.
        int[][] outer = { { 0, -3 }, { 0, 3 }, { 3, 0 }, { 3, 0 } };
        for (int[] offset : outer) {
            Pos root = origin.offset(offset[0], 0, offset[1]);
            if (engine.isBlockInTag("minecraft:dirt", world.blockState(root.x, root.y - 2, root.z))) {
                placeRoot(engine, config, world, random, root);
            }
        }
    }

    private void placeTop(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos top) {
        Pos base = top.offset(0, -5, 0);
        int[] rises = { random.nextInt(4), random.nextInt(4), random.nextInt(4), random.nextInt(4) };
        for (int i = 0; i < CARDINALS.length; i++) {
            placeLimb(engine, config, world, random, base.offset(0, rises[i], 0), i);
        }
        placeCrown(engine, config, world, random, top);
    }

    private void placeLimb(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos start, int direction) {
        int type = random.nextInt(8);
        int dx = CARDINALS[direction][0];
        int dz = CARDINALS[direction][1];
        String axis = AXES[direction];
        Pos p = start;
        switch (type) {
            case 0:
                p = p.offset(dx, 0, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(dx, 0, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(dx, 1, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(dx, 0, dz);
                vineLog(engine, config, world, random, p, axis);
                break;
            case 1:
                p = p.offset(dx, 0, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(dx, 1, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(dx, 0, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(dx, 1, dz);
                vineLog(engine, config, world, random, p, axis);
                break;
            case 2:
                p = p.offset(dx, 0, dz);
                plainLog(engine, config, world, random, p, axis);
                vineLog(engine, config, world, random, p.offset(0, -1, 0), "y");
                p = p.offset(dx, 0, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(dx, 0, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(dx, 1, dz);
                vineLog(engine, config, world, random, p, "y");
                p = p.offset(0, 1, 0);
                plainLog(engine, config, world, random, p, "y");
                break;
            case 3:
                p = p.offset(dx, 0, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(dx, 0, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(dx, 1, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(dx, 1, dz);
                vineLog(engine, config, world, random, p, "y");
                p = p.offset(0, 1, 0);
                plainLog(engine, config, world, random, p, "y");
                break;
            case 4:
                p = p.offset(dx, 0, dz);
                plainLog(engine, config, world, random, p, axis);
                vineLog(engine, config, world, random, p.offset(0, -2, 0), axis);
                p = p.offset(dx, 0, dz);
                plainLog(engine, config, world, random, p, axis);
                vineLog(engine, config, world, random, p.offset(0, -1, 0), "y");
                p = p.offset(dx, 0, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(dx, 1, dz);
                vineLog(engine, config, world, random, p, "y");
                p = p.offset(0, 1, 0);
                plainLog(engine, config, world, random, p, "y");
                break;
            case 5:
                p = p.offset(dx, 0, dz);
                plainLog(engine, config, world, random, p, axis);
                vineLog(engine, config, world, random, p.offset(0, -3, 0), axis);
                p = p.offset(dx, 0, dz);
                plainLog(engine, config, world, random, p, axis);
                plainLog(engine, config, world, random, p.offset(0, -1, 0), "y");
                vineLog(engine, config, world, random, p.offset(0, -2, 0), "y");
                p = p.offset(dx, 0, dz);
                vineLog(engine, config, world, random, p, axis);
                plainLog(engine, config, world, random, p.offset(0, 1, 0), "y");
                p = p.offset(dx, 1, dz);
                vineLog(engine, config, world, random, p, "y");
                p = p.offset(0, 1, 0);
                plainLog(engine, config, world, random, p, "y");
                break;
            case 6:
                p = p.offset(dx, 0, dz);
                plainLog(engine, config, world, random, p, axis);
                vineLog(engine, config, world, random, p.offset(0, -2, 0), axis);
                p = p.offset(dx, 0, dz);
                plainLog(engine, config, world, random, p, "y");
                vineLog(engine, config, world, random, p.offset(0, -1, 0), "y");
                p = p.offset(dx, 1, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(dx, 1, dz);
                vineLog(engine, config, world, random, p, "y");
                break;
            default:
                p = p.offset(dx, 0, dz);
                plainLog(engine, config, world, random, p, axis);
                vineLog(engine, config, world, random, p.offset(0, -3, 0), axis);
                p = p.offset(dx, 0, dz);
                plainLog(engine, config, world, random, p, "y");
                plainLog(engine, config, world, random, p.offset(0, -1, 0), "y");
                vineLog(engine, config, world, random, p.offset(0, -2, 0), "y");
                p = p.offset(dx, 1, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(dx, 0, dz);
                vineLog(engine, config, world, random, p, axis);
                p = p.offset(0, 1, 0);
                vineLog(engine, config, world, random, p, "y");
                break;
        }
        placeCrown(engine, config, world, random, p);
    }

    private void placeCrown(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        int[][] base = { { 0, -1, 0 }, { 0, -1, -1 }, { 0, -1, 1 }, { 1, -1, 0 }, { -1, -1, 0 }, { 0, 0, 0 },
            { 0, 0, -1 }, { 1, 0, -1 }, { -1, 0, -1 }, { 0, 0, 1 }, { 1, 0, 1 }, { -1, 0, 1 }, { 1, 0, 0 },
            { -1, 0, 0 }, { 0, 0, -2 }, { 1, 0, -2 }, { -1, 0, -2 }, { 0, 0, 2 }, { 1, 0, 2 }, { -1, 0, 2 },
            { 2, 0, 0 }, { 2, 0, -1 }, { 2, 0, 1 }, { -2, 0, 0 }, { -2, 0, -1 }, { -2, 0, 1 } };
        leafOffsets(engine, config, world, random, center, base, false);
        int[][] fringes = { { 0, 0, -3 }, { 1, 0, -3 }, { -1, 0, -3 }, { 0, 0, 3 }, { 1, 0, 3 }, { -1, 0, 3 },
            { 3, 0, 0 }, { 3, 0, -1 }, { 3, 0, 1 }, { -3, 0, 0 }, { -3, 0, -1 }, { -3, 0, 1 }, { 2, 0, -2 },
            { -2, 0, -2 }, { 2, 0, 2 }, { -2, 0, 2 } };
        leafOffsets(engine, config, world, random, center, fringes, true);
        int[][] upperOne = { { 0, 1, 0 }, { 0, 1, -1 }, { 1, 1, -1 }, { -1, 1, -1 }, { 0, 1, 1 }, { 1, 1, 1 },
            { -1, 1, 1 }, { 1, 1, 0 }, { -1, 1, 0 }, { 0, 1, -2 }, { 1, 1, -2 }, { -1, 1, -2 }, { 0, 1, 2 },
            { 1, 1, 2 }, { -1, 1, 2 }, { 2, 1, 0 }, { 2, 1, -1 }, { 2, 1, 1 }, { -2, 1, 0 }, { -2, 1, -1 },
            { -2, 1, 1 }, { 0, 1, -3 }, { 0, 1, 3 }, { 3, 1, 0 }, { -3, 1, 0 }, { 2, 1, -2 }, { -2, 1, -2 },
            { 2, 1, 2 }, { -2, 1, 2 } };
        leafOffsets(engine, config, world, random, center, upperOne, false);
        int[][] upperTwo = { { 0, 2, 0 }, { 0, 2, -1 }, { 1, 2, -1 }, { -1, 2, -1 }, { 0, 2, 1 }, { 1, 2, 1 },
            { -1, 2, 1 }, { 1, 2, 0 }, { -1, 2, 0 }, { 0, 2, -2 }, { 1, 2, -2 }, { -1, 2, -2 }, { 0, 2, 2 },
            { 1, 2, 2 }, { -1, 2, 2 }, { 2, 2, 0 }, { 2, 2, -1 }, { 2, 2, 1 }, { -2, 2, 0 }, { -2, 2, -1 },
            { -2, 2, 1 } };
        leafOffsets(engine, config, world, random, center, upperTwo, false);
        int[][] top = { { 0, 3, 0 }, { 0, 3, -1 }, { 0, 3, 1 }, { 1, 3, 0 }, { -1, 3, 0 } };
        leafOffsets(engine, config, world, random, center, top, false);
    }

    private void leafOffsets(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, int[][] offsets, boolean withVines) {
        for (int[] offset : offsets) {
            placeLeaf(engine, config, world, random, center.offset(offset[0], offset[1], offset[2]), withVines);
        }
    }

    private void placeBranch(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, int direction) {
        int dx = CARDINALS[direction][0];
        int dz = CARDINALS[direction][1];
        Pos branch = center.offset(dx, 0, dz);
        if (inside(world, branch) && world.isReplaceable(branch.x, branch.y, branch.z)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("branch_provider"), random, center);
            // BranchBlock has horizontal_facing but no rotated-pillar axis. RU uses trySetValue,
            // so its attempted AXIS assignment is intentionally a no-op for this provider.
            state = ModernFeatureEngine.property(state, "facing", FACINGS[direction]);
            world.setBlock(branch.x, branch.y, branch.z, state);
        }
        placeLeaf(engine, config, world, random, branch.offset(0, 1, 0), false);
        placeLeaf(engine, config, world, random, center.offset(dx * 2, 0, dz * 2), false);
        if (dx == 0) {
            placeLeaf(engine, config, world, random, center.offset(1, 0, dz), false);
            placeLeaf(engine, config, world, random, center.offset(-1, 0, dz), false);
        } else {
            placeLeaf(engine, config, world, random, center.offset(dx, 0, 1), false);
            placeLeaf(engine, config, world, random, center.offset(dx, 0, -1), false);
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
                || isAir(current)) {
                placeLog(engine, config, world, random, position, "y", false);
            } else {
                break;
            }
        }
    }

    private void plainLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, String axis) {
        placeLog(engine, config, world, random, position, axis, false);
    }

    private void vineLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, String axis) {
        placeLog(engine, config, world, random, position, axis, true);
    }

    private void placeLog(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, String axis, boolean withVines) {
        if (!inside(world, position)) return;
        String current = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String substrate = substrateReplacement(current);
        if (substrate != null) {
            world.setBlock(position.x, position.y, position.z, substrate);
        } else if (replaceable(engine, world, position)) {
            String state = features.stateProvider(engine, config.getAsJsonObject("trunk_provider"), random, position);
            world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "axis", axis));
            if (withVines && random.nextInt(3) != 0) placeKapokVine(world, position);
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

    private void placeLeaf(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, boolean withVines) {
        if (!inside(world, position) || !world.isReplaceable(position.x, position.y, position.z)) {
            return;
        }
        String state = features.stateProvider(engine, config.getAsJsonObject("foliage_provider"), random, position);
        world.setBlock(position.x, position.y, position.z, ModernFeatureEngine.property(state, "distance", "1"));
        if (withVines && random.nextInt(3) == 0) placeKapokVine(world, position);
    }

    private static void placeKapokVine(World world, Pos support) {
        int size = RuIncidentalRandom
            .sample(world.worldSeed(), support.x, support.y, support.z, RuIncidentalRandom.VINE, 10);
        Pos position = support.offset(0, -1, 0);
        for (int i = 0; i <= size; i++) {
            if (isAir(world.blockState(position.x, position.y, position.z))) {
                world.setBlock(
                    position.x,
                    position.y,
                    position.z,
                    i == size ? "regions_unexplored:kapok_vines[age=25]" : "regions_unexplored:kapok_vines_plant");
                position = position.offset(0, -1, 0);
            } else {
                if (i != 0) {
                    Pos tip = position.offset(0, 1, 0);
                    world.setBlock(tip.x, tip.y, tip.z, "regions_unexplored:kapok_vines[age=25]");
                }
                break;
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

    private static boolean isAir(String state) {
        return "minecraft:air".equals(ModernDecorationEngine.blockName(state));
    }

    private static boolean inside(World world, Pos position) {
        return position.y >= world.minBuildHeight() && position.y < world.maxBuildHeight();
    }
}
