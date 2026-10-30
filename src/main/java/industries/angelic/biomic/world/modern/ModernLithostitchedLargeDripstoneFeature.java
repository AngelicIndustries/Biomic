package industries.angelic.biomic.world.modern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Direct port of Lithostitched 1.7.9's configurable large-dripstone feature. */
final class ModernLithostitchedLargeDripstoneFeature {

    private static final float[] SIN = makeSinTable();
    private final ModernFeatureEngine features;

    ModernLithostitchedLargeDripstoneFeature(ModernFeatureEngine features) {
        this.features = features;
    }

    boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
        return place(engine, config, world, random, origin, false);
    }

    boolean placeVanilla(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        return place(engine, config, world, random, origin, true);
    }

    private boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin, boolean vanilla) {
        if (!emptyWater(world.blockState(origin.x, origin.y, origin.z))) return false;
        int search = config.get("floor_to_ceiling_search_range")
            .getAsInt();
        Integer ceiling = scan(engine, config, world, origin, search, 1, vanilla);
        Integer floor = scan(engine, config, world, origin, search, -1, vanilla);
        if (ceiling == null || floor == null || ceiling - floor - 1 < 4) return false;

        JsonElement radiusProvider = config.get("column_radius");
        int minimum = minimum(radiusProvider);
        int maximum = maximum(radiusProvider);
        int caveLimited = (int) ((ceiling - floor - 1) * config.get("max_column_radius_to_cave_height_ratio")
            .getAsFloat());
        int maximumRadius = Math.max(minimum, Math.min(maximum, caveLimited));
        int radius = vanilla ? minimum + random.nextInt(maximumRadius - minimum + 1)
            : between(random, minimum, maximumRadius);

        Dripstone stalactite = make(
            config,
            random,
            origin.atY(ceiling - 1),
            false,
            radius,
            config.get("stalactite_bluntness"),
            vanilla);
        Dripstone stalagmite = make(
            config,
            random,
            origin.atY(floor + 1),
            true,
            radius,
            config.get("stalagmite_bluntness"),
            vanilla);
        Wind wind = stalactite.suitableForWind(config) && stalagmite.suitableForWind(config) ? new Wind(
            origin.y,
            sampleFloat(config.get("wind_speed"), random),
            randomBetween(random, 0.0F, (float) Math.PI)) : Wind.NONE;
        boolean ceilingFits = stalactite.findBase(world, wind);
        boolean floorFits = stalagmite.findBase(world, wind);
        if (ceilingFits) stalactite.place(engine, config, world, random, wind);
        if (floorFits) stalagmite.place(engine, config, world, random, wind);
        return true;
    }

    private Dripstone make(JsonObject config, ModernRandom random, Pos root, boolean pointingUp, int radius,
        JsonElement bluntness, boolean vanilla) {
        return new Dripstone(
            root,
            pointingUp,
            radius,
            sampleFloat(bluntness, random),
            sampleFloat(config.get("height_scale"), random),
            vanilla);
    }

    private Integer scan(ModernDecorationEngine engine, JsonObject config, World world, Pos origin, int range, int dy,
        boolean vanilla) {
        Pos cursor = origin;
        int step = 1;
        while (step < range && emptyWater(world.blockState(cursor.x, cursor.y, cursor.z))) {
            cursor = cursor.offset(0, dy, 0);
            step++;
        }
        String state = world.blockState(cursor.x, cursor.y, cursor.z);
        return (vanilla ? vanillaDripstoneBaseOrLava(engine, state) : replaceableOrLava(engine, config, state))
            ? Integer.valueOf(cursor.y)
            : null;
    }

    private static boolean vanillaDripstoneBaseOrLava(ModernDecorationEngine engine, String state) {
        String block = ModernDecorationEngine.blockName(state);
        return "minecraft:lava".equals(block) || "minecraft:dripstone_block".equals(block)
            || engine.isBlockInTag("minecraft:dripstone_replaceable_blocks", state);
    }

    private boolean replaceableOrLava(ModernDecorationEngine engine, JsonObject config, String state) {
        String block = ModernDecorationEngine.blockName(state);
        if ("minecraft:lava".equals(block)) return true;
        JsonElement replacement = config.get("replaceable_blocks");
        return replacement.getAsString()
            .startsWith("#") ? engine.isBlockInTag(replacement.getAsString(), state)
                : replacement.getAsString()
                    .equals(block);
    }

    private final class Dripstone {

        private Pos root;
        private final boolean pointingUp;
        private int radius;
        private final double bluntness;
        private final double scale;
        private final boolean vanilla;

        private Dripstone(Pos root, boolean pointingUp, int radius, double bluntness, double scale, boolean vanilla) {
            this.root = root;
            this.pointingUp = pointingUp;
            this.radius = radius;
            this.bluntness = bluntness;
            this.scale = scale;
            this.vanilla = vanilla;
        }

        private int height() {
            return heightAt(0.0F);
        }

        private int heightAt(float distance) {
            return (int) dripstoneHeight(distance, radius, scale, bluntness);
        }

        private boolean suitableForWind(JsonObject config) {
            return radius >= config.get("min_radius_for_wind")
                .getAsInt() && bluntness
                    >= config.get("min_bluntness_for_wind")
                        .getAsDouble();
        }

        private boolean findBase(World world, Wind wind) {
            while (radius > 1) {
                Pos cursor = root;
                int attempts = Math.min(10, height());
                for (int attempt = 0; attempt < attempts; attempt++) {
                    if ("minecraft:lava".equals(block(world, cursor))) return false;
                    if (circleEmbedded(world, wind.offset(cursor), radius)) {
                        root = cursor;
                        return true;
                    }
                    cursor = cursor.offset(0, pointingUp ? -1 : 1, 0);
                }
                radius /= 2;
            }
            return false;
        }

        private void place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
            Wind wind) {
            for (int x = -radius; x <= radius; x++) {
                column: for (int z = -radius; z <= radius; z++) {
                    float distance = (float) Math.sqrt(x * x + z * z);
                    if (distance > radius) continue;
                    int height = heightAt(distance);
                    if (height <= 0) continue;
                    if (random.nextFloat() < 0.2F) {
                        height = (int) (height * randomBetween(random, 0.8F, 1.0F));
                    }
                    Pos position = root.offset(x, 0, z);
                    boolean placed = false;
                    int maximumY = pointingUp ? world.height("WORLD_SURFACE_WG", position.x, position.z)
                        : Integer.MAX_VALUE;
                    for (int step = 0; step < height && position.y < maximumY; step++) {
                        Pos target = wind.offset(position);
                        String current = world.blockState(target.x, target.y, target.z);
                        if (emptyWaterLava(current)) {
                            placed = true;
                            String state = vanilla ? "minecraft:dripstone_block"
                                : features
                                    .stateProvider(engine, config.getAsJsonObject("state_provider"), random, target);
                            world.setBlock(target.x, target.y, target.z, state);
                        } else if (placed && engine.isBlockInTag("minecraft:base_stone_overworld", current)) {
                            continue column;
                        }
                        position = position.offset(0, pointingUp ? 1 : -1, 0);
                    }
                }
            }
        }
    }

    private static boolean circleEmbedded(World world, Pos center, int radius) {
        if (emptyWaterLava(world.blockState(center.x, center.y, center.z))) return false;
        float increment = 6.0F / radius;
        for (float angle = 0.0F; angle < (float) Math.PI * 2.0F; angle += increment) {
            int x = (int) (fastCos(angle) * radius);
            int z = (int) (fastSin(angle) * radius);
            Pos edge = center.offset(x, 0, z);
            if (emptyWaterLava(world.blockState(edge.x, edge.y, edge.z))) return false;
        }
        return true;
    }

    private static double dripstoneHeight(double distance, double radius, double scale, double bluntness) {
        if (distance < bluntness) distance = bluntness;
        double scaled = distance / radius * 0.384D;
        double quartic = 0.75D * Math.pow(scaled, 1.3333333333333333D);
        double square = Math.pow(scaled, 0.6666666666666666D);
        double logarithm = 0.3333333333333333D * Math.log(scaled);
        double height = scale * (quartic - square - logarithm);
        return Math.max(height, 0.0D) / 0.384D * radius;
    }

    private static final class Wind {

        private static final Wind NONE = new Wind();
        private final int originY;
        private final double x;
        private final double z;
        private final boolean enabled;

        private Wind() {
            originY = 0;
            x = z = 0.0D;
            enabled = false;
        }

        private Wind(int originY, float speed, float angle) {
            this.originY = originY;
            x = fastCos(angle) * speed;
            z = fastSin(angle) * speed;
            enabled = true;
        }

        private Pos offset(Pos position) {
            if (!enabled) return position;
            int height = originY - position.y;
            return position.offset(floor(x * height), 0, floor(z * height));
        }
    }

    private static int minimum(JsonElement provider) {
        if (provider.isJsonPrimitive()) return provider.getAsInt();
        JsonObject object = provider.getAsJsonObject();
        return object.has("min_inclusive") ? object.get("min_inclusive")
            .getAsInt()
            : object.get("value")
                .getAsInt();
    }

    private static int maximum(JsonElement provider) {
        if (provider.isJsonPrimitive()) return provider.getAsInt();
        JsonObject object = provider.getAsJsonObject();
        return object.has("max_inclusive") ? object.get("max_inclusive")
            .getAsInt()
            : object.get("value")
                .getAsInt();
    }

    private static float sampleFloat(JsonElement provider, ModernRandom random) {
        if (provider.isJsonPrimitive()) return provider.getAsFloat();
        JsonObject object = provider.getAsJsonObject();
        String type = object.get("type")
            .getAsString();
        if ("minecraft:uniform".equals(type)) {
            return randomBetween(
                random,
                object.get("min_inclusive")
                    .getAsFloat(),
                object.get("max_exclusive")
                    .getAsFloat());
        }
        if ("minecraft:constant".equals(type)) return object.get("value")
            .getAsFloat();
        throw new IllegalArgumentException("Unsupported Lithostitched float provider " + type);
    }

    private static float randomBetween(ModernRandom random, float minimum, float maximum) {
        return minimum + random.nextFloat() * (maximum - minimum);
    }

    private static int between(ModernRandom random, int minimum, int maximum) {
        return minimum == maximum ? minimum : minimum + random.nextInt(maximum - minimum + 1);
    }

    private static boolean emptyWater(String state) {
        String block = ModernDecorationEngine.blockName(state);
        return "minecraft:air".equals(block) || "minecraft:water".equals(block);
    }

    private static boolean emptyWaterLava(String state) {
        String block = ModernDecorationEngine.blockName(state);
        return "minecraft:air".equals(block) || "minecraft:water".equals(block) || "minecraft:lava".equals(block);
    }

    private static String block(World world, Pos position) {
        return ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
    }

    private static int floor(double value) {
        int integer = (int) value;
        return value < integer ? integer - 1 : integer;
    }

    private static float fastSin(float value) {
        return SIN[(int) (value * 10430.378F) & 0xffff];
    }

    private static float fastCos(float value) {
        return SIN[(int) (value * 10430.378F + 16384.0F) & 0xffff];
    }

    private static float[] makeSinTable() {
        float[] values = new float[65536];
        for (int index = 0; index < values.length; index++) {
            values[index] = (float) Math.sin(index * Math.PI * 2.0D / values.length);
        }
        return values;
    }
}
