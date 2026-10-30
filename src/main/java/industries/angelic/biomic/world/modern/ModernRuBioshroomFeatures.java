package industries.angelic.biomic.world.modern;

import java.util.Random;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Direct ports of pinned RU's giant bioshroom configured features. */
final class ModernRuBioshroomFeatures {

    private final ModernFeatureEngine features;

    ModernRuBioshroomFeatures(ModernFeatureEngine features) {
        this.features = features;
    }

    boolean place(ModernDecorationEngine engine, String type, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        if ("regions_unexplored:giant_yellow_bioshroom".equals(type)) {
            return yellow(engine, config, world, random, origin);
        }
        if ("regions_unexplored:giant_pink_bioshroom".equals(type)) {
            return pink(engine, config, world, random, origin);
        }
        if ("regions_unexplored:giant_green_bioshroom".equals(type)) {
            return green(engine, config, world, random, origin);
        }
        if ("regions_unexplored:giant_blue_bioshroom".equals(type)) {
            return blue(engine, config, world, random, origin);
        }
        throw new IllegalArgumentException("Unsupported pinned RU giant bioshroom " + type);
    }

    private boolean blue(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        random.nextInt(12); // pinned source computes an unused limb direction
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        if (!world.isReplaceable(origin.x, origin.y, origin.z)) return false;

        placeBlueBase(engine, config, world, random, origin);
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            if (y == height) placeBlueCap(engine, config, world, random, position);
            if (y < height - 2) {
                boolean firstDirection = random.nextInt(2) == 0;
                if ((y & 1) == 0) {
                    placeCap(
                        engine,
                        config,
                        world,
                        random,
                        position.offset(firstDirection ? 0 : 1, 0, firstDirection ? -1 : 0));
                } else {
                    placeCap(
                        engine,
                        config,
                        world,
                        random,
                        position.offset(firstDirection ? 0 : -1, 0, firstDirection ? 1 : 0));
                }
            }
            placeStem(engine, config, world, random, position);
        }
        return true;
    }

    private void placeBlueBase(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        Random incidental = RuIncidentalRandom
            .random(world.worldSeed(), origin.x, origin.y, origin.z, RuIncidentalRandom.BASE);
        int[][] directions = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        for (int[] direction : directions) {
            Pos root = origin.offset(direction[0], 0, direction[1]);
            placeYellowRoot(engine, config, world, random, root);
            if (incidental.nextInt(2) == 0) {
                placeStem(engine, config, world, random, root.offset(0, 1, 0));
            }
        }
    }

    private void placeBlueCap(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        int[][] stemCross = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 } };
        for (int depth = 0; depth <= 2; depth++) {
            for (int[] offset : stemCross) {
                placeStem(engine, config, world, random, center.offset(offset[0], -depth, offset[2]));
            }
        }

        int[][] topCross = { { 0, 1, 0 }, { 0, 1, -1 }, { 0, 1, 1 }, { 1, 1, 0 }, { -1, 1, 0 } };
        placeCapOffsets3d(engine, config, world, random, center, topCross);
        int[][] main = { { 0, 0, -2 }, { 1, 0, -2 }, { -1, 0, -2 }, { 0, 0, -3 }, { 1, 0, -3 }, { -1, 0, -3 },
            { 0, 0, 2 }, { 1, 0, 2 }, { -1, 0, 2 }, { 0, 0, 3 }, { 1, 0, 3 }, { -1, 0, 3 }, { 2, 0, 0 }, { 2, 0, -1 },
            { 2, 0, 1 }, { 3, 0, 0 }, { 3, 0, -1 }, { 3, 0, 1 }, { -2, 0, 0 }, { -2, 0, -1 }, { -2, 0, 1 },
            { -3, 0, 0 }, { -3, 0, -1 }, { -3, 0, 1 }, { 1, 0, -1 }, { -1, 0, -1 }, { 1, 0, 1 }, { -1, 0, 1 },
            { 2, 0, -2 }, { -2, 0, -2 }, { 2, 0, 2 }, { -2, 0, 2 } };
        placeCapOffsets3d(engine, config, world, random, center, main);
        int[][] lower = { { 0, -1, -3 }, { 1, -1, -3 }, { 2, -1, -3 }, { -1, -1, -3 }, { -2, -1, -3 }, { 2, -2, -3 },
            { -2, -2, -3 }, { 0, -1, 3 }, { 1, -1, 3 }, { 2, -1, 3 }, { -1, -1, 3 }, { -2, -1, 3 }, { 2, -2, 3 },
            { -2, -2, 3 }, { 3, -1, 0 }, { 3, -1, -1 }, { 3, -1, -2 }, { 3, -1, 1 }, { 3, -1, 2 }, { 3, -2, -2 },
            { 3, -2, 2 }, { -3, -1, 0 }, { -3, -1, -1 }, { -3, -1, -2 }, { -3, -1, 1 }, { -3, -1, 2 }, { -3, -2, -2 },
            { -3, -2, 2 } };
        placeCapOffsets3d(engine, config, world, random, center, lower);
        int[][] glow = { { 0, -1, -2 }, { 1, -1, -2 }, { -1, -1, -2 }, { 0, -1, 2 }, { 1, -1, 2 }, { -1, -1, 2 },
            { 2, -1, 0 }, { 2, -1, -1 }, { 2, -1, 1 }, { -2, -1, 0 }, { -2, -1, -1 }, { -2, -1, 1 }, { 1, -1, -1 },
            { -1, -1, -1 }, { 1, -1, 1 }, { -1, -1, 1 }, { 2, -1, -2 }, { -2, -1, -2 }, { 2, -1, 2 }, { -2, -1, 2 } };
        for (int[] offset : glow) {
            placeGlow(engine, config, world, random, center.offset(offset[0], offset[1], offset[2]));
        }
        if (RuIncidentalRandom.sample(world.worldSeed(), center.x, center.y, center.z, RuIncidentalRandom.TOP, 3)
            == 0) {
            int[][] optionalTop = { { 0, 1, -2 }, { 0, 1, 2 }, { 2, 1, 0 }, { -2, 1, 0 }, { 1, 1, -1 }, { -1, 1, -1 },
                { 1, 1, 1 }, { -1, 1, 1 } };
            placeCapOffsets3d(engine, config, world, random, center, optionalTop);
        }
    }

    private boolean green(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        random.nextInt(12); // pinned source computes an unused limb direction
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        if (!world.isReplaceable(origin.x, origin.y, origin.z)) return false;
        placeGreenBase(engine, config, world, random, origin);
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            if (y == height) placeGreenCap(engine, config, world, random, position);
            placeStem(engine, config, world, random, position);
        }
        return true;
    }

    private void placeGreenBase(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        Random incidental = RuIncidentalRandom
            .random(world.worldSeed(), origin.x, origin.y, origin.z, RuIncidentalRandom.BASE);
        int[][] directions = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        for (int[] direction : directions) {
            Pos root = origin.offset(direction[0], 0, direction[1]);
            placeYellowRoot(engine, config, world, random, root);
            if (incidental.nextInt(2) == 0) {
                placeStem(engine, config, world, random, root.offset(0, 1, 0));
            }
        }
    }

    private void placeGreenCap(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        int[][] cross = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 } };
        for (int[] offset : cross) {
            placeStem(engine, config, world, random, center.offset(offset[0], offset[1], offset[2]));
            placeStem(engine, config, world, random, center.offset(offset[0], -1, offset[2]));
        }
        int[][] diagonals = { { 1, 0, -1 }, { -1, 0, -1 }, { 1, 0, 1 }, { -1, 0, 1 } };
        for (int[] offset : diagonals) {
            placeGlow(engine, config, world, random, center.offset(offset[0], offset[1], offset[2]));
        }
        int[][] arms = { { 0, 0, -2 }, { 1, 0, -2 }, { -1, 0, -2 }, { 0, 0, 2 }, { 1, 0, 2 }, { -1, 0, 2 }, { 2, 0, 0 },
            { 2, 0, -1 }, { 2, 0, 1 }, { -2, 0, 0 }, { -2, 0, -1 }, { -2, 0, 1 } };
        placeCapOffsets3d(engine, config, world, random, center, arms);
        placeCapOffsets3d(engine, config, world, random, center.offset(0, 1, 0), cross);
        Random incidental = RuIncidentalRandom
            .random(world.worldSeed(), center.x, center.y, center.z, RuIncidentalRandom.TOP);
        for (int[] offset : diagonals) {
            if (incidental.nextInt(3) != 0) {
                placeCap(engine, config, world, random, center.offset(offset[0], 1, offset[2]));
            }
        }
        for (int[] offset : arms) {
            placeGlow(engine, config, world, random, center.offset(offset[0], -1, offset[2]));
        }
        int[][] vineStarts = { { 2, -2 }, { -2, -2 }, { 2, 2 }, { -2, 2 }, { 0, -3 }, { 1, -3 }, { -1, -3 }, { 0, 3 },
            { 1, 3 }, { -1, 3 }, { 3, 0 }, { 3, -1 }, { 3, 1 }, { -3, 0 }, { -3, -1 }, { -3, 1 } };
        for (int[] offset : vineStarts) {
            placeGreenCapVine(engine, config, world, random, center.offset(offset[0], -1, offset[1]));
        }
    }

    private void placeGreenCapVine(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos start) {
        int length = RuIncidentalRandom
            .sample(world.worldSeed(), start.x, start.y, start.z, RuIncidentalRandom.VINE, 4);
        for (int step = 0; step <= length; step++) {
            placeCap(engine, config, world, random, start.offset(0, -step, 0));
        }
    }

    private boolean pink(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        random.nextInt(12); // pinned source computes an unused limb direction
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        if (!world.isReplaceable(origin.x, origin.y, origin.z)) return false;
        placeYellowBase(engine, config, world, random, origin);
        for (int y = 0; y <= height; y++) {
            Pos position = origin.offset(0, y, 0);
            if (y == height) placePinkCap(engine, config, world, random, position);
            placeStem(engine, config, world, random, position);
        }
        return true;
    }

    private void placePinkCap(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int[][] diagonals = { { 1, 0, -1 }, { -1, 0, -1 }, { 1, 0, 1 }, { -1, 0, 1 } };
        placeCapOffsets3d(engine, config, world, random, origin.offset(0, -1, 0), diagonals);
        placeCapOffsets3d(engine, config, world, random, origin, diagonals);
        int[][] cardinals = { { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 } };
        for (int[] offset : cardinals) {
            placeGlow(engine, config, world, random, origin.offset(offset[0], offset[1], offset[2]));
        }
        int[][] square = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 }, { 1, 0, -1 },
            { -1, 0, -1 }, { 1, 0, 1 }, { -1, 0, 1 } };
        placeCapOffsets3d(engine, config, world, random, origin.offset(0, 1, 0), square);
        int[][] cross = { { 0, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { 1, 0, 0 }, { -1, 0, 0 } };
        placeCapOffsets3d(engine, config, world, random, origin.offset(0, 2, 0), cross);
        placeCapOffsets3d(engine, config, world, random, origin.offset(0, 3, 0), cross);
        placeCap(engine, config, world, random, origin.offset(0, 4, 0));
    }

    private void placeCapOffsets3d(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, int[][] offsets) {
        for (int[] offset : offsets) {
            placeCap(engine, config, world, random, center.offset(offset[0], offset[1], offset[2]));
        }
    }

    private boolean yellow(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        random.nextInt(12); // pinned source computes an unused limb direction
        int height = random.nextInt(
            config.get("size_variation")
                .getAsInt())
            + config.get("minimum_size")
                .getAsInt();
        if (!world.isReplaceable(origin.x, origin.y, origin.z)) return false;

        placeYellowBase(engine, config, world, random, origin);
        placeYellowGlowCaps(engine, config, world, random, origin, height);
        for (int y = 0; y < height; y++) {
            placeStem(engine, config, world, random, origin.offset(0, y, 0));
        }
        placeYellowCap(engine, config, world, random, origin.offset(0, height, 0));
        return true;
    }

    private void placeYellowBase(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        Random incidental = RuIncidentalRandom
            .random(world.worldSeed(), origin.x, origin.y, origin.z, RuIncidentalRandom.BASE);
        int[][] directions = { { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 } };
        for (int[] direction : directions) {
            if (incidental.nextInt(5) == 0) {
                placeYellowRoot(engine, config, world, random, origin.offset(direction[0], 0, direction[1]));
            }
        }
    }

    private void placeYellowRoot(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        Pos position = origin;
        String stemBlock = ModernDecorationEngine
            .blockName(features.stateProvider(engine, config.getAsJsonObject("stem_provider"), random, origin));
        for (int depth = 0; depth <= 2; depth++, position = position.offset(0, -1, 0)) {
            if (depth > 0
                && !stemBlock
                    .equals(ModernDecorationEngine.blockName(world.blockState(position.x, position.y + 1, position.z)))
                && world.isReplaceable(position.x, position.y, position.z)) return;
            // Source roots deliberately probe/convert buried substrate; they are not canopy clearance cells.
            placeStem(engine, config, world, random, position, false);
        }
    }

    private void placeYellowGlowCaps(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin, int height) {
        int chance = random.nextInt(3);
        int firstHeight = random.nextInt(height - 4) + 3;
        int secondHeight = random.nextInt(height - 4) + 3;
        int direction = random.nextInt(2);
        int diagonalX = direction == 0 ? 1 : -1;
        Pos first = origin.offset(0, firstHeight, 0);
        Pos second = origin.offset(0, secondHeight, 0);
        if (chance != 2) {
            placeGlow(engine, config, world, random, first.offset(0, 0, -1));
            placeGlow(engine, config, world, random, first.offset(diagonalX, 0, 0));
            placeGlow(engine, config, world, random, first.offset(diagonalX, 0, -1));
        }
        if (chance != 1) {
            placeGlow(engine, config, world, random, second.offset(0, 0, 1));
            placeGlow(engine, config, world, random, second.offset(-diagonalX, 0, 0));
            placeGlow(engine, config, world, random, second.offset(-diagonalX, 0, 1));
        }
    }

    private void placeYellowCap(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center) {
        int[][] core = { { 0, 0 }, { 0, -1 }, { 0, 1 }, { 1, 0 }, { -1, 0 }, { 1, -1 }, { -1, -1 }, { 1, 1 }, { -1, 1 },
            { 0, -2 }, { 1, -2 }, { -1, -2 }, { 0, 2 }, { 1, 2 }, { -1, 2 }, { 2, 0 }, { 2, -1 }, { 2, 1 }, { -2, 0 },
            { -2, -1 }, { -2, 1 } };
        placeCapOffsets(engine, config, world, random, center, core);
        if (random.nextInt(2) == 0) {
            int[][] outer = { { 2, -2 }, { -2, -2 }, { 2, 2 }, { -2, 2 }, { 0, -3 }, { 1, -3 }, { -1, -3 }, { 2, -3 },
                { -2, -3 }, { 0, 3 }, { 1, 3 }, { -1, 3 }, { 2, 3 }, { -2, 3 }, { 3, 0 }, { 3, -1 }, { 3, 1 },
                { 3, -2 }, { 3, 2 }, { -3, 0 }, { -3, -1 }, { -3, 1 }, { -3, -2 }, { -3, 2 } };
            placeCapOffsets(engine, config, world, random, center, outer);
        }
    }

    private void placeCapOffsets(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos center, int[][] offsets) {
        for (int[] offset : offsets) {
            placeCap(engine, config, world, random, center.offset(offset[0], 0, offset[1]));
        }
    }

    private void placeCap(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        world.requireBioshroomClearance(position.x, position.y, position.z);
        if (!replaceable(engine, world, position)) return;
        world.setBlock(
            position.x,
            position.y,
            position.z,
            features.stateProvider(engine, config.getAsJsonObject("cap_provider"), random, position));
    }

    private void placeGlow(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        world.requireBioshroomClearance(position.x, position.y, position.z);
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()
            || !world.isReplaceable(position.x, position.y, position.z)) return;
        world.setBlock(
            position.x,
            position.y,
            position.z,
            features.stateProvider(engine, config.getAsJsonObject("glow_block_provider"), random, position));
    }

    private void placeStem(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position) {
        placeStem(engine, config, world, random, position, true);
    }

    private void placeStem(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos position, boolean clearance) {
        if (clearance) world.requireBioshroomClearance(position.x, position.y, position.z);
        if (position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
        String current = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        String replacement = substrateReplacement(current);
        if (replacement != null) {
            world.setBlock(position.x, position.y, position.z, replacement);
        } else if (replaceable(engine, world, position)) {
            world.setBlock(
                position.x,
                position.y,
                position.z,
                features.stateProvider(engine, config.getAsJsonObject("stem_provider"), random, position));
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

    private static String substrateReplacement(String block) {
        if ("regions_unexplored:peat_grass_block".equals(block)) return "regions_unexplored:peat_dirt";
        if ("regions_unexplored:silt_grass_block".equals(block)) return "regions_unexplored:silt_dirt";
        if ("regions_unexplored:alpha_grass_block".equals(block) || "minecraft:grass_block".equals(block)
            || "minecraft:mycelium".equals(block)) {
            return "minecraft:dirt";
        }
        if ("regions_unexplored:mycotoxic_moss".equals(block) || "regions_unexplored:glistering_nylium".equals(block)
            || "minecraft:crimson_nylium".equals(block)
            || "minecraft:warped_nylium".equals(block)) return "minecraft:netherrack";
        if ("regions_unexplored:cobalt_nylium".equals(block)) return "minecraft:blackstone";
        if ("regions_unexplored:prismoss".equals(block) || "regions_unexplored:viridescent_nylium".equals(block)
            || "regions_unexplored:stone_grass_block".equals(block)) return "minecraft:stone";
        if ("regions_unexplored:deepslate_prismoss".equals(block)
            || "regions_unexplored:deepslate_viridescent_nylium".equals(block)
            || "regions_unexplored:deepslate_grass_block".equals(block)) return "minecraft:deepslate";
        return null;
    }

    private static boolean replaceable(ModernDecorationEngine engine, World world, Pos position) {
        return position.y >= world.minBuildHeight() && position.y < world.maxBuildHeight()
            && engine.isBlockInTag(
                "regions_unexplored:replaceable_blocks",
                world.blockState(position.x, position.y, position.z));
    }
}
