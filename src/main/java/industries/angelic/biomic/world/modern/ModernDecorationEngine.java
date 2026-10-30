package industries.angelic.biomic.world.modern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.noise.ModernPerlinSimplexNoise;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/**
 * Exact 1.21.1 placed-feature scheduler and placement-modifier interpreter.
 *
 * <p>
 * Coordinates passed to this class are modern target coordinates. The eventual 1.7.10 world adapter is
 * responsible for applying {@link ModernVerticalTransform} only when blocks cross the compatibility boundary.
 * </p>
 */
public final class ModernDecorationEngine {

    public static final int DECORATION_STEPS = 11;

    private static final ModernPerlinSimplexNoise BIOME_INFO_NOISE = new ModernPerlinSimplexNoise(2345L, 0);

    private final ModernDecorationGraph graph;
    private final ModernDensityGraph density;
    private final FeatureExecutor featureExecutor;
    private final ThreadLocal<Integer> placementDepth = new ThreadLocal<Integer>() {

        @Override
        protected Integer initialValue() {
            return Integer.valueOf(0);
        }
    };

    public ModernDecorationEngine(long worldSeed, FeatureExecutor featureExecutor) {
        this(new ModernDecorationGraph(), new ModernDensityGraph(worldSeed), featureExecutor);
    }

    public ModernDecorationEngine(ModernDecorationGraph graph, ModernDensityGraph density,
        FeatureExecutor featureExecutor) {
        if (featureExecutor == null) throw new NullPointerException("featureExecutor");
        this.graph = graph;
        this.density = density;
        this.featureExecutor = featureExecutor;
    }

    /** Called by selector/configured-feature implementations for nested placed features. */
    public boolean place(JsonElement placedFeature, String rootFeature, World world, ModernRandom random, Pos origin) {
        JsonObject placed = resolvePlaced(placedFeature);
        boolean[] result = { false };
        int depth = placementDepth.get()
            .intValue() + 1;
        placementDepth.set(Integer.valueOf(depth));
        try {
            world.recordPlacementCandidate(origin, depth, "entry");
            walkPositions(
                placed.getAsJsonArray("placement"),
                0,
                rootFeature,
                world,
                random,
                origin,
                false,
                position -> {
                    world.recordPlacementCandidate(position, depth, "leaf");
                    if (executeConfigured(placed.get("feature"), world, random, position)) {
                        result[0] = true;
                    }
                });
        } finally {
            if (depth == 1) placementDepth.remove();
            else placementDepth.set(Integer.valueOf(depth - 1));
        }
        return result[0];
    }

    /** Direct configured-feature entry point used by parity fixtures and nested compatibility tests. */
    public boolean placeConfigured(String configuredFeature, World world, ModernRandom random, Pos origin) {
        return executeConfigured(identifierElement(configuredFeature), world, random, origin);
    }

    private boolean executeConfigured(JsonElement feature, World world, ModernRandom random, Pos position) {
        JsonObject configured = resolveConfigured(feature);
        return featureExecutor.place(
            this,
            configured.get("type")
                .getAsString(),
            configured.getAsJsonObject("config"),
            world,
            random,
            position);
    }

    /** Mirrors Stream.flatMap's lazy, depth-first traversal used by PlacedFeature. */
    private void walkPositions(JsonArray modifiers, int modifierIndex, String rootFeature, World world,
        ModernRandom random, Pos position, boolean skipBiomeFilter, PositionSink sink) {
        if (modifierIndex >= modifiers.size()) {
            sink.accept(position);
            return;
        }
        JsonObject modifier = modifiers.get(modifierIndex)
            .getAsJsonObject();
        if (skipBiomeFilter && "minecraft:biome".equals(string(modifier, "type"))) {
            walkPositions(modifiers, modifierIndex + 1, rootFeature, world, random, position, true, sink);
            return;
        }
        List<Pos> next = new ArrayList<Pos>();
        applyModifier(modifier, rootFeature, world, random, position, next);
        for (Pos candidate : next) {
            world.recordPlacementCandidate(
                candidate,
                placementDepth.get()
                    .intValue(),
                "modifier=" + modifierIndex);
            walkPositions(modifiers, modifierIndex + 1, rootFeature, world, random, candidate, skipBiomeFilter, sink);
        }
    }

    @FunctionalInterface
    private interface PositionSink {

        void accept(Pos position);
    }

    private void applyModifier(JsonObject modifier, String rootFeature, World world, ModernRandom random, Pos position,
        List<Pos> output) {
        String type = string(modifier, "type");
        if ("minecraft:count".equals(type)) {
            repeat(output, position, sampleInt(modifier.get("count"), random));
        } else if ("minecraft:rarity_filter".equals(type)) {
            if (random.nextFloat() < 1.0F / modifier.get("chance")
                .getAsInt()) output.add(position);
        } else if ("minecraft:in_square".equals(type)) {
            output.add(new Pos(position.x + random.nextInt(16), position.y, position.z + random.nextInt(16)));
        } else if ("minecraft:heightmap".equals(type)) {
            int height = world.height(string(modifier, "heightmap"), position.x, position.z);
            if (height > world.minBuildHeight()) output.add(position.atY(height));
        } else if ("minecraft:height_range".equals(type)) {
            output.add(position.atY(sampleHeight(modifier.get("height"), random, world)));
        } else if ("minecraft:random_offset".equals(type)) {
            int x = position.x + sampleInt(modifier.get("xz_spread"), random);
            int y = position.y + sampleInt(modifier.get("y_spread"), random);
            int z = position.z + sampleInt(modifier.get("xz_spread"), random);
            output.add(new Pos(x, y, z));
        } else if ("minecraft:surface_water_depth_filter".equals(type)) {
            int floor = world.height("OCEAN_FLOOR", position.x, position.z);
            int surface = world.height("WORLD_SURFACE", position.x, position.z);
            int maxDepth = modifier.get("max_water_depth")
                .getAsInt();
            world.recordPlacementCandidate(
                position,
                placementDepth.get()
                    .intValue(),
                "surface_water_depth=" + floor + "," + surface + "," + maxDepth);
            if (surface - floor <= maxDepth) output.add(position);
        } else if ("minecraft:surface_relative_threshold_filter".equals(type)) {
            long surface = world.height(string(modifier, "heightmap"), position.x, position.z);
            long min = surface + integer(modifier, "min_inclusive", Integer.MIN_VALUE);
            long max = surface + integer(modifier, "max_inclusive", Integer.MAX_VALUE);
            if (position.y >= min && position.y <= max) output.add(position);
        } else if ("minecraft:block_predicate_filter".equals(type)) {
            if (predicate(modifier.getAsJsonObject("predicate"), world, position)) output.add(position);
        } else if ("minecraft:biome".equals(type)) {
            if (!world.allowsFeatureAt(rootFeature, position)) return;
            String biome = world.biome(position.x, position.y, position.z);
            if (rootFeature != null && biomeHasFeature(biome, rootFeature)) output.add(position);
        } else if ("minecraft:noise_based_count".equals(type)) {
            double value = BIOME_INFO_NOISE.value(
                position.x / modifier.get("noise_factor")
                    .getAsDouble(),
                position.z / modifier.get("noise_factor")
                    .getAsDouble());
            int count = (int) Math.ceil(
                (value + number(modifier, "noise_offset", 0.0D)) * modifier.get("noise_to_count_ratio")
                    .getAsInt());
            repeat(output, position, count);
        } else if ("minecraft:noise_threshold_count".equals(type)) {
            double value = BIOME_INFO_NOISE.value(position.x / 200.0D, position.z / 200.0D);
            repeat(
                output,
                position,
                value < modifier.get("noise_level")
                    .getAsDouble() ? modifier.get("below_noise")
                        .getAsInt()
                        : modifier.get("above_noise")
                            .getAsInt());
        } else if ("minecraft:count_on_every_layer".equals(type)) {
            countOnEveryLayer(modifier, world, random, position, output);
        } else if ("minecraft:environment_scan".equals(type)) {
            environmentScan(modifier, world, position, output);
        } else if ("minecraft:carving_mask".equals(type)) {
            output.addAll(world.carvingMask(string(modifier, "step"), position.x >> 4, position.z >> 4));
        } else if ("minecraft:fixed_placement".equals(type)) {
            for (JsonElement fixed : modifier.getAsJsonArray("positions")) {
                Pos candidate = blockPos(fixed);
                if ((candidate.x >> 4) == (position.x >> 4) && (candidate.z >> 4) == (position.z >> 4)) {
                    output.add(candidate);
                }
            }
        } else if ("lithostitched:noise_slope".equals(type)) {
            double value = density.sampleNoise(
                string(modifier, "noise"),
                position.x * modifier.get("xz_scale")
                    .getAsDouble(),
                position.y * modifier.get("y_scale")
                    .getAsDouble(),
                position.z * modifier.get("xz_scale")
                    .getAsDouble());
            repeat(
                output,
                position,
                (int) Math.ceil(
                    value * modifier.get("slope")
                        .getAsInt())
                    + integer(modifier, "offset", 0));
        } else if ("lithostitched:condition".equals(type)) {
            if (condition(modifier.getAsJsonObject("condition"), world, position)) output.add(position);
        } else if ("lithostitched:offset".equals(type)) {
            output.add(
                position.offset(
                    sampleInt(optional(modifier, "x_offset", 0), random),
                    sampleInt(optional(modifier, "y_offset", 0), random),
                    sampleInt(optional(modifier, "z_offset", 0), random)));
        } else {
            throw new IllegalArgumentException("Unsupported pinned placement modifier " + type);
        }
    }

    private void countOnEveryLayer(JsonObject modifier, World world, ModernRandom random, Pos origin,
        List<Pos> output) {
        int layer = 0;
        boolean found;
        do {
            found = false;
            int count = sampleInt(modifier.get("count"), random);
            for (int attempt = 0; attempt < count; attempt++) {
                int x = origin.x + random.nextInt(16);
                int z = origin.z + random.nextInt(16);
                int y = groundForLayer(world, x, world.height("MOTION_BLOCKING", x, z), z, layer);
                if (y == Integer.MAX_VALUE) continue;
                output.add(new Pos(x, y, z));
                found = true;
            }
            layer++;
        } while (found);
    }

    private int groundForLayer(World world, int x, int top, int z, int wantedLayer) {
        int layer = 0;
        String previous = world.blockState(x, top, z);
        for (int y = top; y >= world.minBuildHeight() + 1; y--) {
            String below = world.blockState(x, y - 1, z);
            if (!isEmpty(below) && isEmpty(previous) && !"minecraft:bedrock".equals(blockName(below))) {
                if (layer == wantedLayer) return y;
                layer++;
            }
            previous = below;
        }
        return Integer.MAX_VALUE;
    }

    private void environmentScan(JsonObject modifier, World world, Pos origin, List<Pos> output) {
        JsonObject allowed = modifier.has("allowed_search_condition")
            ? modifier.getAsJsonObject("allowed_search_condition")
            : null;
        JsonObject target = modifier.getAsJsonObject("target_condition");
        Pos current = origin;
        if (allowed != null && !predicate(allowed, world, current)) return;
        int[] direction = direction(string(modifier, "direction_of_search"));
        int maxSteps = modifier.get("max_steps")
            .getAsInt();
        for (int step = 0; step < maxSteps; step++) {
            if (predicate(target, world, current)) {
                output.add(current);
                return;
            }
            current = current.offset(direction[0], direction[1], direction[2]);
            if (current.y < world.minBuildHeight() || current.y >= world.maxBuildHeight()) return;
            if (allowed != null && !predicate(allowed, world, current)) break;
        }
        if (predicate(target, world, current)) output.add(current);
    }

    private boolean predicate(JsonObject predicate, World world, Pos unshifted) {
        Pos position = predicate.has("offset") ? unshifted.add(blockPos(predicate.get("offset"))) : unshifted;
        String type = string(predicate, "type");
        if ("minecraft:matching_blocks".equals(type)) {
            return containsBlock(
                predicate.get("blocks"),
                blockName(world.blockState(position.x, position.y, position.z)));
        }
        if ("minecraft:matching_block_tag".equals(type)) {
            return graph.blockTag(string(predicate, "tag"))
                .contains(blockName(world.blockState(position.x, position.y, position.z)));
        }
        if ("minecraft:matching_fluids".equals(type)) {
            return containsString(predicate.get("fluids"), world.fluidState(position.x, position.y, position.z));
        }
        if ("minecraft:solid".equals(type)) return world.isSolid(position.x, position.y, position.z);
        if ("minecraft:would_survive".equals(type)) {
            return world.canSurvive(state(predicate.getAsJsonObject("state")), position.x, position.y, position.z);
        }
        if ("minecraft:has_sturdy_face".equals(type)) {
            return world.hasSturdyFace(position.x, position.y, position.z, string(predicate, "direction"));
        }
        if ("minecraft:inside_world_bounds".equals(type)) {
            return position.y >= world.minBuildHeight() && position.y < world.maxBuildHeight();
        }
        if ("minecraft:replaceable".equals(type)) {
            return world.isReplaceable(position.x, position.y, position.z);
        }
        if ("lithostitched:random_chance".equals(type)) {
            return world.randomChance(
                position.x,
                position.y,
                position.z,
                predicate.get("chance")
                    .getAsFloat());
        }
        if ("lithostitched:in_structure".equals(type)) {
            return world.intersectsStructure(
                position.x,
                position.y,
                position.z,
                predicate.get("search_range")
                    .getAsInt(),
                string(predicate, "structure"));
        }
        if ("lithostitched:grid".equals(type)) {
            int radius = predicate.get("radius")
                .getAsInt();
            int spacing = predicate.get("distance_between_points")
                .getAsInt();
            int minimum = inclusiveMinimum(predicate.get("allowed_count"));
            int maximum = inclusiveMaximum(predicate.get("allowed_count"));
            int count = 0;
            JsonObject child = predicate.getAsJsonObject("predicate");
            for (int x = position.x - radius; x <= position.x + radius; x += spacing) {
                for (int z = position.z - radius; z <= position.z + radius; z += spacing) {
                    if (predicate(child, world, new Pos(x, position.y, z)) && ++count > maximum) {
                        return false;
                    }
                }
            }
            return count >= minimum && count <= maximum;
        }
        if ("minecraft:not".equals(type)) {
            return !predicate(predicate.getAsJsonObject("predicate"), world, position);
        }
        if ("minecraft:all_of".equals(type)) {
            for (JsonElement child : predicate.getAsJsonArray("predicates")) {
                if (!predicate(child.getAsJsonObject(), world, position)) return false;
            }
            return true;
        }
        if ("minecraft:any_of".equals(type)) {
            for (JsonElement child : predicate.getAsJsonArray("predicates")) {
                if (predicate(child.getAsJsonObject(), world, position)) return true;
            }
            return false;
        }
        throw new IllegalArgumentException("Unsupported pinned block predicate " + type);
    }

    boolean testPredicate(JsonObject predicate, World world, Pos position) {
        return predicate(predicate, world, position);
    }

    /** Registry-tag membership used by configured features after codec resolution. */
    public boolean isBlockInTag(String tag, String state) {
        return graph.blockTag(tag)
            .contains(blockName(state));
    }

    /** Effective post-modifier precipitation flag used by modern freeze/snow decoration. */
    public boolean hasPrecipitation(String biome) {
        JsonObject definition = graph.biome(biome);
        return definition.has("has_precipitation") && definition.get("has_precipitation")
            .getAsBoolean();
    }

    private static int inclusiveMinimum(JsonElement range) {
        return range.isJsonPrimitive() ? range.getAsInt()
            : range.getAsJsonObject()
                .get("min_inclusive")
                .getAsInt();
    }

    private static int inclusiveMaximum(JsonElement range) {
        return range.isJsonPrimitive() ? range.getAsInt()
            : range.getAsJsonObject()
                .get("max_inclusive")
                .getAsInt();
    }

    private boolean condition(JsonObject condition, World world, Pos position) {
        String type = string(condition, "type");
        if ("lithostitched:sample_noise_router".equals(type)) {
            double sample = density.sampleRouter(string(condition, "target"), position.x, position.y, position.z);
            return sample >= number(condition, "min_inclusive", Double.MIN_VALUE)
                && sample <= number(condition, "max_inclusive", Double.MAX_VALUE);
        }
        if ("lithostitched:offset".equals(type)) {
            return condition(
                condition.getAsJsonObject("condition"),
                world,
                position.add(blockPos(condition.get("offset"))));
        }
        if ("lithostitched:in_biome".equals(type)) {
            String actual = world.noiseBiome(position.x, position.y, position.z);
            JsonElement expected = condition.get("biomes");
            if (expected.isJsonPrimitive() && expected.getAsString()
                .startsWith("#")) {
                return graph.biomeTag(expected.getAsString())
                    .contains(actual);
            }
            return containsString(expected, actual);
        }
        throw new IllegalArgumentException("Unsupported pinned Lithostitched placement condition " + type);
    }

    private boolean biomeHasFeature(String biome, String rootFeature) {
        // A placement modifier can step from a RU/vanilla origin into a native
        // BOP/Salty/other habitat. It owns none of this pinned graph's features;
        // reject that candidate instead of asking the strict source loader for
        // a definition it cannot contain. Direct graph lookups remain strict.
        if (!graph.hasBiome(biome)) return false;
        JsonArray steps = graph.biome(biome)
            .getAsJsonArray("features");
        for (JsonElement step : steps) {
            for (JsonElement feature : step.getAsJsonArray()) {
                if (rootFeature.equals(feature.getAsString())) return true;
            }
        }
        return false;
    }

    private int sampleHeight(JsonElement element, ModernRandom random, World world) {
        JsonObject provider = element.getAsJsonObject();
        if (!provider.has("type")) return anchor(provider, world);
        String type = string(provider, "type");
        int minimum = anchor(provider.getAsJsonObject("min_inclusive"), world);
        int maximum = anchor(provider.getAsJsonObject("max_inclusive"), world);
        if (minimum > maximum) return minimum;
        if ("minecraft:uniform".equals(type)) return between(random, minimum, maximum);
        if ("minecraft:trapezoid".equals(type)) {
            int plateau = integer(provider, "plateau", 0);
            int span = maximum - minimum;
            if (plateau >= span) return between(random, minimum, maximum);
            int lowerSlope = (span - plateau) / 2;
            int upperSlope = span - lowerSlope;
            return minimum + between(random, 0, upperSlope) + between(random, 0, lowerSlope);
        }
        if ("minecraft:very_biased_to_bottom".equals(type)) {
            int inner = integer(provider, "inner", 1);
            if (maximum - minimum - inner + 1 <= 0) return minimum;
            int first = between(random, minimum + inner, maximum);
            int second = between(random, minimum, first - 1);
            return between(random, minimum, second - 1 + inner);
        }
        throw new IllegalArgumentException("Unsupported pinned height provider " + type);
    }

    private int sampleInt(JsonElement element, ModernRandom random) {
        if (element == null || element.isJsonNull()) {
            throw new IllegalArgumentException("Missing pinned integer provider");
        }
        if (element.isJsonPrimitive()) return exactInt(element, "integer provider");
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Pinned integer provider must be a number or object: " + element);
        }
        JsonObject provider = element.getAsJsonObject();
        String type = string(provider, "type");
        if ("minecraft:uniform".equals(type)) {
            JsonObject body = integerProviderBody(provider, "min_inclusive", "max_inclusive");
            return between(random, requiredInt(body, "min_inclusive"), requiredInt(body, "max_inclusive"));
        }
        if ("minecraft:clamped_normal".equals(type)) {
            JsonObject body = integerProviderBody(provider, "mean", "deviation", "min_inclusive", "max_inclusive");
            double value = requiredFiniteDouble(body, "mean")
                + random.nextGaussian() * requiredFiniteDouble(body, "deviation");
            value = Math.max(requiredInt(body, "min_inclusive"), Math.min(requiredInt(body, "max_inclusive"), value));
            return (int) value;
        }
        if ("minecraft:clamped".equals(type)) {
            JsonObject body = integerProviderBody(provider, "source", "min_inclusive", "max_inclusive");
            return Math.max(
                requiredInt(body, "min_inclusive"),
                Math.min(requiredInt(body, "max_inclusive"), sampleInt(body.get("source"), random)));
        }
        if ("minecraft:biased_to_bottom".equals(type)) {
            JsonObject body = integerProviderBody(provider, "min_inclusive", "max_inclusive");
            int minimum = requiredInt(body, "min_inclusive");
            int span = Math.addExact(Math.subtractExact(requiredInt(body, "max_inclusive"), minimum), 1);
            if (span <= 0) throw new IllegalArgumentException("Invalid biased-to-bottom range " + body);
            return minimum + random.nextInt(random.nextInt(span) + 1);
        }
        if ("minecraft:weighted_list".equals(type)) {
            JsonObject body = integerProviderBody(provider, "distribution");
            JsonElement distributionElement = body.get("distribution");
            if (distributionElement == null || !distributionElement.isJsonArray()) {
                throw new IllegalArgumentException("Missing weighted integer distribution " + body);
            }
            JsonArray distribution = distributionElement.getAsJsonArray();
            int total = 0;
            for (JsonElement entry : distribution) {
                if (!entry.isJsonObject()) throw new IllegalArgumentException("Invalid weighted entry " + entry);
                int weight = requiredInt(entry.getAsJsonObject(), "weight");
                if (weight <= 0) throw new IllegalArgumentException("Invalid weighted integer weight " + weight);
                total = Math.addExact(total, weight);
            }
            if (total <= 0) throw new IllegalArgumentException("Empty weighted integer distribution");
            int selected = random.nextInt(total);
            for (JsonElement entry : distribution) {
                JsonObject weighted = entry.getAsJsonObject();
                selected -= requiredInt(weighted, "weight");
                if (selected < 0) return sampleInt(weighted.get("data"), random);
            }
            throw new AssertionError("Weighted provider selection escaped distribution");
        }
        throw new IllegalArgumentException("Unsupported pinned integer provider " + type);
    }

    /**
     * Mojang 1.20.4 serializes IntProvider values under a {@code value} object while the pinned
     * 1.21 data used by the original engine writes the same fields beside {@code type}. Accept
     * exactly either representation; mixed representations are ambiguous and fail closed.
     */
    private JsonObject integerProviderBody(JsonObject provider, String... fields) {
        JsonElement wrapped = provider.get("value");
        if (wrapped == null) return provider;
        if (!wrapped.isJsonObject()) {
            throw new IllegalArgumentException("Pinned integer provider value must be an object: " + provider);
        }
        for (String field : fields) {
            if (provider.has(field)) {
                throw new IllegalArgumentException(
                    "Mixed pinned integer provider representations for " + field + ": " + provider);
            }
        }
        return wrapped.getAsJsonObject();
    }

    private int requiredInt(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null) throw new IllegalArgumentException("Missing integer field " + name + " in " + object);
        return exactInt(value, name);
    }

    private int exactInt(JsonElement element, String name) {
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive()
            .isNumber()) {
            throw new IllegalArgumentException("Expected numeric integer " + name + ": " + element);
        }
        try {
            return new java.math.BigDecimal(element.getAsString()).intValueExact();
        } catch (ArithmeticException | NumberFormatException failure) {
            throw new IllegalArgumentException("Expected exact 32-bit integer " + name + ": " + element, failure);
        }
    }

    private double requiredFiniteDouble(JsonObject object, String name) {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonPrimitive()
            || !value.getAsJsonPrimitive()
                .isNumber()) {
            throw new IllegalArgumentException("Expected numeric field " + name + " in " + object);
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException("Expected finite field " + name + " in " + object);
        }
        return number;
    }

    int sampleIntValue(JsonElement element, ModernRandom random) {
        return sampleInt(element, random);
    }

    private int anchor(JsonObject anchor, World world) {
        if (anchor.has("absolute")) return anchor.get("absolute")
            .getAsInt();
        if (anchor.has("above_bottom")) return world.minBuildHeight() + anchor.get("above_bottom")
            .getAsInt();
        if (anchor.has("below_top")) return world.maxBuildHeight() - 1
            - anchor.get("below_top")
                .getAsInt();
        throw new IllegalArgumentException("Unsupported vertical anchor " + anchor);
    }

    private JsonObject resolvePlaced(JsonElement element) {
        return element.isJsonPrimitive() ? graph.placedFeature(element.getAsString()) : element.getAsJsonObject();
    }

    private JsonObject resolveConfigured(JsonElement element) {
        return element.isJsonPrimitive() ? graph.configuredFeature(element.getAsString()) : element.getAsJsonObject();
    }

    private static JsonElement identifierElement(String identifier) {
        return new com.google.gson.JsonPrimitive(identifier);
    }

    private static void repeat(List<Pos> output, Pos position, int count) {
        for (int index = 0; index < count; index++) output.add(position);
    }

    private static int between(ModernRandom random, int minimum, int maximum) {
        // Mth.randomBetweenInclusive always calls RandomSource.nextInt, including for a
        // degenerate UniformInt such as [5, 5]. That apparently redundant nextInt(1) still
        // advances the worldgen RNG. Skipping it changes every later sample in a configured
        // feature (notably spruce foliage radius and the following weighted tree choices).
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    private static boolean containsBlock(JsonElement states, String block) {
        if (states.isJsonPrimitive()) return block.equals(blockName(states.getAsString()));
        for (JsonElement state : states.getAsJsonArray()) {
            if (state.isJsonPrimitive() && block.equals(blockName(state.getAsString()))) return true;
            if (state.isJsonObject() && block.equals(blockName(state(state.getAsJsonObject())))) return true;
        }
        return false;
    }

    private static boolean containsString(JsonElement values, String wanted) {
        if (values.isJsonPrimitive()) return wanted.equals(values.getAsString());
        for (JsonElement value : values.getAsJsonArray()) if (wanted.equals(value.getAsString())) return true;
        return false;
    }

    private static boolean isEmpty(String state) {
        String block = blockName(state);
        return "minecraft:air".equals(block) || "minecraft:cave_air".equals(block)
            || "minecraft:void_air".equals(block)
            || "minecraft:water".equals(block)
            || "minecraft:lava".equals(block);
    }

    public static String state(JsonObject encoded) {
        StringBuilder result = new StringBuilder(string(encoded, "Name"));
        if (encoded.has("Properties")) {
            JsonObject properties = encoded.getAsJsonObject("Properties");
            List<String> keys = new ArrayList<String>();
            for (java.util.Map.Entry<String, JsonElement> entry : properties.entrySet()) {
                keys.add(entry.getKey());
            }
            Collections.sort(keys);
            result.append('[');
            for (int index = 0; index < keys.size(); index++) {
                if (index > 0) result.append(',');
                String key = keys.get(index);
                result.append(key)
                    .append('=')
                    .append(
                        properties.get(key)
                            .getAsString());
            }
            result.append(']');
        }
        return result.toString();
    }

    public static String blockName(String state) {
        int properties = state.indexOf('[');
        return properties < 0 ? state : state.substring(0, properties);
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null ? null : value.getAsString();
    }

    private static int integer(JsonObject object, String key, int fallback) {
        JsonElement value = object.get(key);
        return value == null ? fallback : value.getAsInt();
    }

    private static double number(JsonObject object, String key, double fallback) {
        JsonElement value = object.get(key);
        return value == null ? fallback : value.getAsDouble();
    }

    private static JsonElement optional(JsonObject object, String key, int fallback) {
        JsonElement value = object.get(key);
        return value == null ? new com.google.gson.JsonPrimitive(fallback) : value;
    }

    private static int[] direction(String direction) {
        if ("up".equals(direction)) return new int[] { 0, 1, 0 };
        if ("down".equals(direction)) return new int[] { 0, -1, 0 };
        if ("north".equals(direction)) return new int[] { 0, 0, -1 };
        if ("south".equals(direction)) return new int[] { 0, 0, 1 };
        if ("west".equals(direction)) return new int[] { -1, 0, 0 };
        if ("east".equals(direction)) return new int[] { 1, 0, 0 };
        throw new IllegalArgumentException("Unknown direction " + direction);
    }

    private static Pos blockPos(JsonElement element) {
        if (element.isJsonArray()) {
            JsonArray value = element.getAsJsonArray();
            return new Pos(
                value.get(0)
                    .getAsInt(),
                value.get(1)
                    .getAsInt(),
                value.get(2)
                    .getAsInt());
        }
        JsonObject value = element.getAsJsonObject();
        return new Pos(
            value.get("x")
                .getAsInt(),
            value.get("y")
                .getAsInt(),
            value.get("z")
                .getAsInt());
    }

    public interface FeatureExecutor {

        boolean place(ModernDecorationEngine engine, String type, JsonObject config, World world, ModernRandom random,
            Pos position);
    }

    public interface World {

        long worldSeed();

        int minBuildHeight();

        int maxBuildHeight();

        int seaLevel();

        int height(String heightmap, int x, int z);

        String blockState(int x, int y, int z);

        String fluidState(int x, int y, int z);

        String biome(int x, int y, int z);

        boolean shouldFreeze(int x, int y, int z, boolean mustBeAtEdge);

        boolean shouldSnow(int x, int y, int z);

        boolean isSolid(int x, int y, int z);

        boolean hasSturdyFace(int x, int y, int z, String direction);

        /** Vines use a full support OR collision face; leaves are explicitly not face-sturdy. */
        default boolean hasVineSupportFace(int x, int y, int z, String direction) {
            return blockName(blockState(x, y, z)).endsWith("_leaves") || hasSturdyFace(x, y, z, direction);
        }

        boolean canSurvive(String state, int x, int y, int z);

        boolean isReplaceable(int x, int y, int z);

        /** Optional complete giant-fungus admission; source worlds retain their per-cell skip semantics. */
        default void requireBioshroomClearance(int x, int y, int z) {}

        boolean randomChance(int x, int y, int z, float chance);

        String noiseBiome(int x, int y, int z);

        boolean intersectsStructure(int x, int y, int z, int searchRange, String structure);

        List<Pos> carvingMask(String step, int chunkX, int chunkZ);

        boolean setBlock(int x, int y, int z, String state);

        /** Raw-write fixtures deliberately omit Level's immediate neighbor-shape notifications. */
        default boolean immediateShapeUpdates() {
            return true;
        }

        /** Incidental growth age is coordinate-stable in hosted worlds, independent of chunk traversal. */
        default int growingPlantHeadAge(Pos position) {
            return levelRandomInt(position, industries.angelic.biomic.world.replay.RuIncidentalRandom.VINE, 25);
        }

        default int levelRandomInt(Pos position, long purpose, int bound) {
            return industries.angelic.biomic.world.replay.RuIncidentalRandom
                .sample(worldSeed(), position.x, position.y, position.z, purpose, bound);
        }

        /** Optional hosted-surface admission; the exact standalone modern generator keeps its original behavior. */
        default boolean allowsFeatureAt(String feature, Pos position) {
            return true;
        }

        default void recordPlacementCandidate(Pos position, int depth, String phase) {}

        default void setLootTable(int x, int y, int z, String lootTable, long seed) {}

        default void setSpawnerMob(int x, int y, int z, String entityType) {}
    }

    public static final class Pos {

        public final int x;
        public final int y;
        public final int z;

        public Pos(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Pos atY(int newY) {
            return new Pos(x, newY, z);
        }

        public Pos offset(int offsetX, int offsetY, int offsetZ) {
            return new Pos(x + offsetX, y + offsetY, z + offsetZ);
        }

        public Pos add(Pos other) {
            return offset(other.x, other.y, other.z);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Pos)) return false;
            Pos position = (Pos) other;
            return x == position.x && y == position.y && z == position.z;
        }

        @Override
        public int hashCode() {
            // Vec3i/BlockPos hash in the pinned modern target. HashSet iteration is observable
            // in tree decorators that stable-sort positions only by Y before shuffling them.
            return (y + z * 31) * 31 + x;
        }

        @Override
        public String toString() {
            return x + "," + y + "," + z;
        }
    }

}
