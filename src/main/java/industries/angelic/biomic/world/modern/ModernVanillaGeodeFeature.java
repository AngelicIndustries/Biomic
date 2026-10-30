package industries.angelic.biomic.world.modern;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernLegacyRandom;
import industries.angelic.biomic.world.modern.noise.ModernNormalNoise;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Exact block-volume port of Minecraft 1.21.1's geode feature. */
final class ModernVanillaGeodeFeature {

    private static final int[][] DIRECTIONS = { { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 }, { -1, 0, 0 },
        { 1, 0, 0 } };
    private static final String[] DIRECTION_NAMES = { "down", "up", "north", "south", "west", "east" };

    private final ModernFeatureEngine features;

    ModernVanillaGeodeFeature(ModernFeatureEngine features) {
        this.features = features;
    }

    boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
        int minimum = config.get("min_gen_offset")
            .getAsInt();
        int maximum = config.get("max_gen_offset")
            .getAsInt();
        int pointCount = engine.sampleIntValue(config.get("distribution_points"), random);
        ModernNormalNoise noise = new ModernNormalNoise(new ModernLegacyRandom(world.worldSeed()), -4, 1.0D);
        List<Point> distribution = new ArrayList<Point>(pointCount);
        List<Pos> cracks = new ArrayList<Pos>(3);
        double distributionAdjustment = (double) pointCount / maximum(config.get("outer_wall_distance"));
        JsonObject layers = config.getAsJsonObject("layers");
        JsonObject blocks = config.getAsJsonObject("blocks");
        JsonObject crack = config.getAsJsonObject("crack");
        double fillingThreshold = 1.0D / Math.sqrt(
            layers.get("filling")
                .getAsDouble());
        double innerThreshold = 1.0D / Math.sqrt(
            layers.get("inner_layer")
                .getAsDouble() + distributionAdjustment);
        double middleThreshold = 1.0D / Math.sqrt(
            layers.get("middle_layer")
                .getAsDouble() + distributionAdjustment);
        double outerThreshold = 1.0D / Math.sqrt(
            layers.get("outer_layer")
                .getAsDouble() + distributionAdjustment);
        double crackThreshold = 1.0D / Math.sqrt(
            crack.get("base_crack_size")
                .getAsDouble() + random.nextDouble() / 2.0D
                + (pointCount > 3 ? distributionAdjustment : 0.0D));
        boolean generateCrack = random.nextFloat() < crack.get("generate_crack_chance")
            .getAsFloat();

        int invalid = 0;
        for (int index = 0; index < pointCount; index++) {
            int x = engine.sampleIntValue(config.get("outer_wall_distance"), random);
            int y = engine.sampleIntValue(config.get("outer_wall_distance"), random);
            int z = engine.sampleIntValue(config.get("outer_wall_distance"), random);
            Pos point = origin.offset(x, y, z);
            String state = world.blockState(point.x, point.y, point.z);
            if (("minecraft:air".equals(ModernDecorationEngine.blockName(state))
                || engine.isBlockInTag("minecraft:geode_invalid_blocks", state))
                && ++invalid > config.get("invalid_blocks_threshold")
                    .getAsInt())
                return false;
            distribution.add(new Point(point, engine.sampleIntValue(config.get("point_offset"), random)));
        }

        if (generateCrack) {
            int direction = random.nextInt(4);
            int distance = pointCount * 2 + 1;
            if (direction == 0) {
                cracks.add(origin.offset(distance, 7, 0));
                cracks.add(origin.offset(distance, 5, 0));
                cracks.add(origin.offset(distance, 1, 0));
            } else if (direction == 1) {
                cracks.add(origin.offset(0, 7, distance));
                cracks.add(origin.offset(0, 5, distance));
                cracks.add(origin.offset(0, 1, distance));
            } else if (direction == 2) {
                cracks.add(origin.offset(distance, 7, distance));
                cracks.add(origin.offset(distance, 5, distance));
                cracks.add(origin.offset(distance, 1, distance));
            } else {
                cracks.add(origin.offset(0, 7, 0));
                cracks.add(origin.offset(0, 5, 0));
                cracks.add(origin.offset(0, 1, 0));
            }
        }

        List<Pos> potentialPlacements = new ArrayList<Pos>();
        int crackPointOffset = crack.get("crack_point_offset")
            .getAsInt();
        double noiseMultiplier = config.get("noise_multiplier")
            .getAsDouble();
        for (int z = minimum; z <= maximum; z++) {
            for (int y = minimum; y <= maximum; y++) {
                for (int x = minimum; x <= maximum; x++) {
                    Pos position = origin.offset(x, y, z);
                    double perturbation = noise.sample(position.x, position.y, position.z) * noiseMultiplier;
                    double distributionValue = 0.0D;
                    double crackValue = 0.0D;
                    for (Point point : distribution) {
                        distributionValue += inverseSqrt(distanceSquared(position, point.position) + point.offset)
                            + perturbation;
                    }
                    for (Pos crackPoint : cracks) {
                        crackValue += inverseSqrt(distanceSquared(position, crackPoint) + crackPointOffset)
                            + perturbation;
                    }
                    if (distributionValue < outerThreshold) continue;
                    if (generateCrack && crackValue >= crackThreshold && distributionValue < fillingThreshold) {
                        safeSet(engine, blocks, world, position, "minecraft:air");
                    } else if (distributionValue >= fillingThreshold) {
                        safeSet(
                            engine,
                            blocks,
                            world,
                            position,
                            features
                                .stateProvider(engine, blocks.getAsJsonObject("filling_provider"), random, position));
                    } else if (distributionValue >= innerThreshold) {
                        boolean alternate = random.nextFloat() < config.get("use_alternate_layer0_chance")
                            .getAsFloat();
                        String provider = alternate ? "alternate_inner_layer_provider" : "inner_layer_provider";
                        safeSet(
                            engine,
                            blocks,
                            world,
                            position,
                            features.stateProvider(engine, blocks.getAsJsonObject(provider), random, position));
                        if ((!config.get("placements_require_layer0_alternate")
                            .getAsBoolean() || alternate)
                            && random.nextFloat() < config.get("use_potential_placements_chance")
                                .getAsFloat()) {
                            potentialPlacements.add(position);
                        }
                    } else if (distributionValue >= middleThreshold) {
                        safeSet(
                            engine,
                            blocks,
                            world,
                            position,
                            features.stateProvider(
                                engine,
                                blocks.getAsJsonObject("middle_layer_provider"),
                                random,
                                position));
                    } else {
                        safeSet(
                            engine,
                            blocks,
                            world,
                            position,
                            features.stateProvider(
                                engine,
                                blocks.getAsJsonObject("outer_layer_provider"),
                                random,
                                position));
                    }
                }
            }
        }

        JsonArray placements = blocks.getAsJsonArray("inner_placements");
        for (Pos source : potentialPlacements) {
            String state = ModernDecorationEngine.state(
                placements.get(random.nextInt(placements.size()))
                    .getAsJsonObject());
            for (int direction = 0; direction < DIRECTIONS.length; direction++) {
                state = property(state, "facing", DIRECTION_NAMES[direction]);
                int[] delta = DIRECTIONS[direction];
                Pos target = source.offset(delta[0], delta[1], delta[2]);
                String existing = world.blockState(target.x, target.y, target.z);
                state = property(
                    state,
                    "waterlogged",
                    "minecraft:water".equals(ModernDecorationEngine.blockName(existing)) ? "true" : "false");
                String existingBlock = ModernDecorationEngine.blockName(existing);
                if (!"minecraft:air".equals(existingBlock) && !"minecraft:water".equals(existingBlock)) continue;
                safeSet(engine, blocks, world, target, state);
                break;
            }
        }
        return true;
    }

    private static void safeSet(ModernDecorationEngine engine, JsonObject blocks, World world, Pos position,
        String state) {
        if (engine.isBlockInTag(
            blocks.get("cannot_replace")
                .getAsString(),
            world.blockState(position.x, position.y, position.z))) return;
        world.setBlock(position.x, position.y, position.z, state);
    }

    private static double distanceSquared(Pos first, Pos second) {
        double x = first.x - second.x;
        double y = first.y - second.y;
        double z = first.z - second.z;
        return x * x + y * y + z * z;
    }

    private static double inverseSqrt(double value) {
        return 1.0D / Math.sqrt(value);
    }

    private static int maximum(JsonElement provider) {
        if (provider.isJsonPrimitive()) return provider.getAsInt();
        JsonObject object = provider.getAsJsonObject();
        if (object.has("max_inclusive")) return object.get("max_inclusive")
            .getAsInt();
        return object.get("value")
            .getAsInt();
    }

    private static String property(String state, String name, String value) {
        String marker = name + "=";
        int start = state.indexOf(marker);
        if (start < 0) return state;
        start += marker.length();
        int end = state.indexOf(',', start);
        if (end < 0) end = state.indexOf(']', start);
        return state.substring(0, start) + value + state.substring(end);
    }

    private static final class Point {

        private final Pos position;
        private final int offset;

        private Point(Pos position, int offset) {
            this.position = position;
            this.offset = offset;
        }
    }
}
