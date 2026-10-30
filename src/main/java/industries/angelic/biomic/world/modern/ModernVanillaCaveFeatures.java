package industries.angelic.biomic.world.modern;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Exact small cave-feature implementations from the pinned Minecraft 1.21.1 target. */
final class ModernVanillaCaveFeatures {

    private static final int[][] DIRECTIONS = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 },
        { 1, 0, 0 } };
    private static final int[][] HORIZONTAL = { { 0, 0, -1 }, { 1, 0, 0 }, { 0, 0, 1 }, { -1, 0, 0 } };

    boolean placePointedDripstone(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int direction = tipDirection(engine, world, random, origin);
        if (direction < 0) return false;

        int[] step = DIRECTIONS[direction];
        makeDripstonePatch(engine, config, world, random, origin.offset(-step[0], -step[1], -step[2]));
        Pos ahead = origin.offset(step[0], step[1], step[2]);
        int length = random.nextFloat() < config.get("chance_of_taller_dripstone")
            .getAsFloat() && emptyOrWater(world.blockState(ahead.x, ahead.y, ahead.z)) ? 2 : 1;
        growPointedDripstone(engine, world, origin, direction, length, false);
        return true;
    }

    boolean placeUnderwaterMagma(JsonObject config, World world, ModernRandom random, Pos origin) {
        Integer floor = underwaterFloor(
            world,
            origin,
            config.get("floor_search_range")
                .getAsInt());
        if (floor == null) return false;

        int radius = config.get("placement_radius_around_floor")
            .getAsInt();
        float probability = config.get("placement_probability_per_valid_position")
            .getAsFloat();
        boolean placed = false;
        // BlockPos.betweenClosedStream advances x, then y, then z.
        for (int z = origin.z - radius; z <= origin.z + radius; z++) {
            for (int y = floor - radius; y <= floor + radius; y++) {
                for (int x = origin.x - radius; x <= origin.x + radius; x++) {
                    if (random.nextFloat() >= probability || !validMagmaPosition(world, x, y, z)) {
                        continue;
                    }
                    world.setBlock(x, y, z, "minecraft:magma_block");
                    placed = true;
                }
            }
        }
        return placed;
    }

    boolean freezeTopLayer(World world, Pos origin) {
        for (int xOffset = 0; xOffset < 16; xOffset++) {
            for (int zOffset = 0; zOffset < 16; zOffset++) {
                int x = origin.x + xOffset;
                int z = origin.z + zOffset;
                int y = world.height("MOTION_BLOCKING", x, z);
                if (world.shouldFreeze(x, y - 1, z, false)) {
                    world.setBlock(x, y - 1, z, "minecraft:ice");
                }
                if (!world.shouldSnow(x, y, z)) continue;
                world.setBlock(x, y, z, "minecraft:snow[layers=1]");
                String below = world.blockState(x, y - 1, z);
                if (below.contains("snowy=false")) {
                    world.setBlock(x, y - 1, z, below.replace("snowy=false", "snowy=true"));
                }
            }
        }
        return true;
    }

    boolean placeDripstoneCluster(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        if (!emptyOrWater(world.blockState(origin.x, origin.y, origin.z))) return false;
        int maximumHeight = engine.sampleIntValue(config.get("height"), random);
        float wetness = sampleFloat(config.getAsJsonObject("wetness"), random);
        float density = sampleFloat(config.getAsJsonObject("density"), random);
        int radiusX = engine.sampleIntValue(config.get("radius"), random);
        int radiusZ = engine.sampleIntValue(config.get("radius"), random);
        for (int x = -radiusX; x <= radiusX; x++) {
            for (int z = -radiusZ; z <= radiusZ; z++) {
                double chance = columnChance(radiusX, radiusZ, x, z, config);
                placeClusterColumn(
                    engine,
                    config,
                    world,
                    random,
                    origin.offset(x, 0, z),
                    x,
                    z,
                    wetness,
                    chance,
                    maximumHeight,
                    density);
            }
        }
        return true;
    }

    private void placeClusterColumn(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin, int offsetX, int offsetZ, float wetness, double chance, int maximumHeight, float density) {
        int search = config.get("floor_to_ceiling_search_range")
            .getAsInt();
        Integer ceiling = scanBoundary(world, origin, search, 1);
        Integer floor = scanBoundary(world, origin, search, -1);
        if (ceiling == null && floor == null) return;

        Integer adjustedFloor = floor;
        if (random.nextFloat() < wetness && floor != null && canPlaceDripstonePool(engine, world, origin.atY(floor))) {
            world.setBlock(origin.x, floor, origin.z, "minecraft:water[level=0]");
            adjustedFloor = floor - 1;
        }

        int stalactite = 0;
        boolean placeStalactite = random.nextDouble() < chance;
        if (ceiling != null && placeStalactite && !"minecraft:lava".equals(block(world, origin.atY(ceiling)))) {
            int thickness = engine.sampleIntValue(config.get("dripstone_block_layer_thickness"), random);
            replaceDripstoneBaseLayer(engine, world, origin.atY(ceiling), thickness, 1);
            int available = adjustedFloor == null ? maximumHeight : Math.min(maximumHeight, ceiling - adjustedFloor);
            stalactite = dripstoneHeight(random, offsetX, offsetZ, density, available, config);
        }

        int stalagmite = 0;
        boolean placeStalagmite = random.nextDouble() < chance;
        if (adjustedFloor != null && placeStalagmite
            && !"minecraft:lava".equals(block(world, origin.atY(adjustedFloor)))) {
            int thickness = engine.sampleIntValue(config.get("dripstone_block_layer_thickness"), random);
            replaceDripstoneBaseLayer(engine, world, origin.atY(adjustedFloor), thickness, -1);
            if (ceiling != null) {
                int difference = config.get("max_stalagmite_stalactite_height_diff")
                    .getAsInt();
                stalagmite = Math.max(0, stalactite + between(random, -difference, difference));
            } else {
                stalagmite = dripstoneHeight(random, offsetX, offsetZ, density, maximumHeight, config);
            }
        }

        int downLength = stalactite;
        int upLength = stalagmite;
        if (ceiling != null && adjustedFloor != null && ceiling - stalactite <= adjustedFloor + stalagmite) {
            int low = Math.max(ceiling - stalactite, adjustedFloor + 1);
            int high = Math.min(adjustedFloor + stalagmite, ceiling - 1);
            int split = between(random, low, high + 1);
            downLength = ceiling - split;
            upLength = split - 1 - adjustedFloor;
        }

        int columnHeight = ceiling != null && adjustedFloor != null ? ceiling - adjustedFloor - 1 : -1;
        boolean merge = random.nextBoolean() && downLength > 0
            && upLength > 0
            && columnHeight >= 0
            && downLength + upLength == columnHeight;
        if (ceiling != null) {
            growPointedDripstone(engine, world, origin.atY(ceiling - 1), 0, downLength, merge);
        }
        if (adjustedFloor != null) {
            growPointedDripstone(engine, world, origin.atY(adjustedFloor + 1), 1, upLength, merge);
        }
    }

    private static Integer scanBoundary(World world, Pos origin, int range, int dy) {
        Pos cursor = origin;
        int step = 1;
        while (step < range && emptyOrWater(world.blockState(cursor.x, cursor.y, cursor.z))) {
            cursor = cursor.offset(0, dy, 0);
            step++;
        }
        return emptyOrWater(world.blockState(cursor.x, cursor.y, cursor.z)) ? null : Integer.valueOf(cursor.y);
    }

    private static boolean canPlaceDripstonePool(ModernDecorationEngine engine, World world, Pos floor) {
        String state = block(world, floor);
        if ("minecraft:water".equals(state) || "minecraft:dripstone_block".equals(state)
            || "minecraft:pointed_dripstone".equals(state)) return false;
        if (isWater(world, floor.offset(0, 1, 0))) return false;
        for (int[] direction : HORIZONTAL) {
            if (!stoneOrWater(engine, world, floor.offset(direction[0], 0, direction[2]))) return false;
        }
        return stoneOrWater(engine, world, floor.offset(0, -1, 0));
    }

    private static boolean stoneOrWater(ModernDecorationEngine engine, World world, Pos position) {
        return engine
            .isBlockInTag("minecraft:base_stone_overworld", world.blockState(position.x, position.y, position.z))
            || isWater(world, position);
    }

    private static void replaceDripstoneBaseLayer(ModernDecorationEngine engine, World world, Pos origin, int thickness,
        int dy) {
        Pos position = origin;
        for (int index = 0; index < thickness; index++) {
            if (!placeDripstoneBlock(engine, world, position)) return;
            position = position.offset(0, dy, 0);
        }
    }

    private static int dripstoneHeight(ModernRandom random, int offsetX, int offsetZ, float density, int maximum,
        JsonObject config) {
        if (random.nextFloat() > density) return 0;
        int distance = Math.abs(offsetX) + Math.abs(offsetZ);
        float bias = (float) clampedMap(
            distance,
            0.0D,
            config.get("max_distance_from_center_affecting_height_bias")
                .getAsDouble(),
            maximum / 2.0D,
            0.0D);
        float deviation = config.get("height_deviation")
            .getAsFloat();
        return (int) clamp((float) (bias + random.nextGaussian() * deviation), 0.0F, maximum);
    }

    private static double columnChance(int radiusX, int radiusZ, int x, int z, JsonObject config) {
        int distanceFromEdge = Math.min(radiusX - Math.abs(x), radiusZ - Math.abs(z));
        return clampedMap(
            distanceFromEdge,
            0.0D,
            config.get("max_distance_from_edge_affecting_chance_of_dripstone_column")
                .getAsDouble(),
            config.get("chance_of_dripstone_column_at_max_distance_from_center")
                .getAsDouble(),
            1.0D);
    }

    private static float sampleFloat(JsonObject provider, ModernRandom random) {
        String type = provider.get("type")
            .getAsString();
        if ("minecraft:uniform".equals(type)) {
            float minimum = provider.get("min_inclusive")
                .getAsFloat();
            return minimum + random.nextFloat() * (provider.get("max_exclusive")
                .getAsFloat() - minimum);
        }
        if ("minecraft:clamped_normal".equals(type)) {
            float value = (float) (provider.get("mean")
                .getAsFloat()
                + random.nextGaussian() * provider.get("deviation")
                    .getAsFloat());
            return clamp(
                value,
                provider.get("min")
                    .getAsFloat(),
                provider.get("max")
                    .getAsFloat());
        }
        throw new IllegalArgumentException("Unsupported pinned float provider " + type);
    }

    private static int between(ModernRandom random, int minimum, int maximum) {
        // Vertical compression can collapse a one-block modern column into an inverted legacy interval.
        // The modern source never sees that geometry; preserving its lower endpoint is the deterministic
        // one-block squeeze compromise and avoids asking RandomSource for a non-positive bound.
        return minimum >= maximum ? minimum : minimum + random.nextInt(maximum - minimum + 1);
    }

    private static double clampedMap(double value, double inLow, double inHigh, double outLow, double outHigh) {
        double delta = (value - inLow) / (inHigh - inLow);
        return outLow + clamp(delta, 0.0D, 1.0D) * (outHigh - outLow);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static Integer underwaterFloor(World world, Pos origin, int range) {
        if (!isWater(world, origin)) return null;
        Pos cursor = origin;
        int step = 1;
        while (step < range && isWater(world, cursor)) {
            cursor = cursor.offset(0, -1, 0);
            step++;
        }
        return isWater(world, cursor) ? null : Integer.valueOf(cursor.y);
    }

    private static boolean validMagmaPosition(World world, int x, int y, int z) {
        if (waterOrAir(world, x, y, z) || waterOrAir(world, x, y - 1, z)) return false;
        for (int[] direction : HORIZONTAL) {
            if (waterOrAir(world, x + direction[0], y, z + direction[2])) return false;
        }
        return true;
    }

    private static boolean waterOrAir(World world, int x, int y, int z) {
        String block = ModernDecorationEngine.blockName(world.blockState(x, y, z));
        return "minecraft:air".equals(block) || "minecraft:water".equals(block);
    }

    private static String block(World world, Pos position) {
        return ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
    }

    private static boolean isWater(World world, Pos position) {
        return "minecraft:water"
            .equals(ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z)))
            || "minecraft:water".equals(world.fluidState(position.x, position.y, position.z));
    }

    private static int tipDirection(ModernDecorationEngine engine, World world, ModernRandom random, Pos position) {
        boolean ceiling = dripstoneBase(engine, world.blockState(position.x, position.y + 1, position.z));
        boolean floor = dripstoneBase(engine, world.blockState(position.x, position.y - 1, position.z));
        if (ceiling && floor) return random.nextBoolean() ? 0 : 1;
        if (ceiling) return 0;
        return floor ? 1 : -1;
    }

    private static void makeDripstonePatch(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, Pos center) {
        placeDripstoneBlock(engine, world, center);
        float directional = config.get("chance_of_directional_spread")
            .getAsFloat();
        float radiusTwo = config.get("chance_of_spread_radius2")
            .getAsFloat();
        float radiusThree = config.get("chance_of_spread_radius3")
            .getAsFloat();
        for (int[] direction : HORIZONTAL) {
            if (random.nextFloat() > directional) continue;
            Pos first = center.offset(direction[0], 0, direction[2]);
            placeDripstoneBlock(engine, world, first);
            if (random.nextFloat() > radiusTwo) continue;
            int[] randomDirection = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
            Pos second = first.offset(randomDirection[0], randomDirection[1], randomDirection[2]);
            placeDripstoneBlock(engine, world, second);
            if (random.nextFloat() > radiusThree) continue;
            randomDirection = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
            placeDripstoneBlock(
                engine,
                world,
                second.offset(randomDirection[0], randomDirection[1], randomDirection[2]));
        }
    }

    static void growPointedDripstone(ModernDecorationEngine engine, World world, Pos origin, int direction, int length,
        boolean mergeTip) {
        int[] step = DIRECTIONS[direction];
        Pos support = origin.offset(-step[0], -step[1], -step[2]);
        if (!dripstoneBase(engine, world.blockState(support.x, support.y, support.z))) return;

        Pos position = origin;
        for (int index = 0; index < length; index++) {
            String thickness;
            if (length >= 3 && index == 0) thickness = "base";
            else if (length >= 3 && index < length - 2) thickness = "middle";
            else if (length >= 2 && index == length - 2) thickness = "frustum";
            else thickness = mergeTip ? "tip_merge" : "tip";
            boolean waterlogged = "minecraft:water"
                .equals(ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z)));
            world.setBlock(
                position.x,
                position.y,
                position.z,
                "minecraft:pointed_dripstone[thickness=" + thickness
                    + ",vertical_direction="
                    + (direction == 0 ? "down" : "up")
                    + ",waterlogged="
                    + waterlogged
                    + "]");
            position = position.offset(step[0], step[1], step[2]);
        }
    }

    static boolean placeDripstoneBlock(ModernDecorationEngine engine, World world, Pos position) {
        if (!engine.isBlockInTag(
            "minecraft:dripstone_replaceable_blocks",
            world.blockState(position.x, position.y, position.z))) return false;
        world.setBlock(position.x, position.y, position.z, "minecraft:dripstone_block");
        return true;
    }

    static boolean dripstoneBase(ModernDecorationEngine engine, String state) {
        return "minecraft:dripstone_block".equals(ModernDecorationEngine.blockName(state))
            || engine.isBlockInTag("minecraft:dripstone_replaceable_blocks", state);
    }

    static boolean emptyOrWater(String state) {
        String block = ModernDecorationEngine.blockName(state);
        return "minecraft:air".equals(block) || "minecraft:water".equals(block);
    }
}
