package industries.angelic.biomic.world.modern;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.biomic.world.modern.noise.ModernBlendedNoise;
import industries.angelic.biomic.world.modern.noise.ModernFastNoiseCellular;
import industries.angelic.biomic.world.modern.noise.ModernNormalNoise;

/** Interpreter for the codec-resolved pinned 1.21.1 density graph. */
public final class ModernDensityGraph {

    public static final String RESOURCE = "/assets/biomic/ru_modern_parity/resolved-modern-worldgen.json";
    public static final String SHA256 = "6b57c876bff647ec64d7b224e20cda7af02e987b5f296fdfb9cd3938ea026393";

    private static final JsonObject ROOT = load();

    private final long worldSeed;
    private final JsonObject definitions;
    private final JsonObject noiseRouter;
    private final JsonObject noiseParameters;
    private final JsonObject fastNoiseParameters;
    private final Map<String, Function> functions = new HashMap<String, Function>();
    private final Map<String, Function> interpolatedFunctions = new HashMap<String, Function>();
    private final Map<String, Function> routerFunctions = new HashMap<String, Function>();
    /** Noises are pure functions of the seed and their name: threads that race here build the same one. */
    private final Map<String, ModernNormalNoise> noises = new java.util.concurrent.ConcurrentHashMap<String, ModernNormalNoise>();
    private final Map<String, ModernFastNoiseCellular> fastNoises = new HashMap<String, ModernFastNoiseCellular>();
    private final Set<String> compiling = new HashSet<String>();
    private final ThreadLocal<SamplingSession> samplingSession = new ThreadLocal<SamplingSession>();

    public ModernDensityGraph(long worldSeed) {
        this.worldSeed = worldSeed;
        definitions = ROOT.getAsJsonObject("density_functions");
        noiseRouter = ROOT.getAsJsonObject("noise_settings")
            .getAsJsonObject("noise_router");
        noiseParameters = ROOT.getAsJsonObject("noises");
        fastNoiseParameters = ROOT.getAsJsonObject("fast_noise_configs");
    }

    /** Samples a codec-resolved NoiseRouter member before NoiseChunk marker interpolation. */
    public double sampleRouter(String name, int blockX, int blockY, int blockZ) {
        Function compiled = routerFunctions.get(name);
        if (compiled == null) {
            JsonElement definition = noiseRouter.get(name);
            if (definition == null) throw new IllegalArgumentException("Unknown modern noise router member " + name);
            compiled = compile(definition, false);
            routerFunctions.put(name, compiled);
        }
        return compiled.sample(context(blockX, blockY, blockZ));
    }

    private Context context(int blockX, int blockY, int blockZ) {
        SamplingSession session = samplingSession.get();
        return new Context(blockX, blockY, blockZ, session == null ? new EvaluationCache() : session.cache);
    }

    /** Samples a registered modern NormalNoise field directly. */
    public double sampleNoise(String identifier, double x, double y, double z) {
        return noise(identifier).sample(x, y, z);
    }

    private Function named(String identifier, boolean interpolateMarkers) {
        Map<String, Function> cache = interpolateMarkers ? interpolatedFunctions : functions;
        Function compiled = cache.get(identifier);
        if (compiled != null) return compiled;
        JsonElement definition = definitions.get(identifier);
        if (definition == null) throw new IllegalArgumentException("Unknown modern density function " + identifier);
        String compilingKey = (interpolateMarkers ? "interpolated:" : "raw:") + identifier;
        if (!compiling.add(compilingKey)) throw new IllegalStateException("Recursive density reference " + identifier);
        compiled = compile(definition, interpolateMarkers);
        compiling.remove(compilingKey);
        cache.put(identifier, compiled);
        return compiled;
    }

    private Function compile(JsonElement element, boolean interpolateMarkers) {
        if (element.isJsonPrimitive()) {
            if (element.getAsJsonPrimitive()
                .isNumber()) return constant(element.getAsDouble());
            if (element.getAsJsonPrimitive()
                .isString()) return named(element.getAsString(), interpolateMarkers);
        }
        if (!element.isJsonObject()) throw new IllegalArgumentException("Invalid density node " + element);
        JsonObject node = element.getAsJsonObject();
        String type = string(node, "type");
        if (type == null) throw new IllegalArgumentException("Density object has no type: " + node);

        if ("minecraft:interpolated".equals(type)) {
            Function argument = compile(node.get("argument"), interpolateMarkers);
            return interpolateMarkers ? interpolated(argument) : argument;
        }
        if ("minecraft:flat_cache".equals(type)) {
            Function argument = compile(node.get("argument"), interpolateMarkers);
            return interpolateMarkers ? flatCached(argument) : argument;
        }
        if ("lithostitched:internal/merged".equals(type)) {
            return compile(node.get("original"), interpolateMarkers);
        }
        if ("minecraft:cache_2d".equals(type)) {
            Function argument = compile(node.get("argument"), interpolateMarkers);
            return interpolateMarkers ? cache2d(argument) : argument;
        }
        if ("minecraft:cache_once".equals(type)) {
            Function argument = compile(node.get("argument"), interpolateMarkers);
            return interpolateMarkers ? cacheOnce(argument) : argument;
        }
        if ("minecraft:blend_density".equals(type)) {
            return compile(node.get("argument"), interpolateMarkers);
        }
        if ("minecraft:blend_alpha".equals(type)) return constant(1.0D);
        if ("minecraft:blend_offset".equals(type)) return constant(0.0D);
        if ("minecraft:add".equals(type)) return binary(node, Binary.ADD, interpolateMarkers);
        if ("minecraft:mul".equals(type)) return binary(node, Binary.MULTIPLY, interpolateMarkers);
        if ("minecraft:min".equals(type)) return binary(node, Binary.MINIMUM, interpolateMarkers);
        if ("minecraft:max".equals(type)) return binary(node, Binary.MAXIMUM, interpolateMarkers);
        if ("minecraft:abs".equals(type)) return mapped(node, Mapping.ABSOLUTE, interpolateMarkers);
        if ("minecraft:square".equals(type)) return mapped(node, Mapping.SQUARE, interpolateMarkers);
        if ("minecraft:cube".equals(type)) return mapped(node, Mapping.CUBE, interpolateMarkers);
        if ("minecraft:half_negative".equals(type)) return mapped(node, Mapping.HALF_NEGATIVE, interpolateMarkers);
        if ("minecraft:quarter_negative".equals(type))
            return mapped(node, Mapping.QUARTER_NEGATIVE, interpolateMarkers);
        if ("minecraft:squeeze".equals(type)) return mapped(node, Mapping.SQUEEZE, interpolateMarkers);
        if ("minecraft:y_clamped_gradient".equals(type)) return yGradient(node);
        if ("minecraft:noise".equals(type)) return noise(node);
        if ("minecraft:shift_a".equals(type)) return shift(node, true);
        if ("minecraft:shift_b".equals(type)) return shift(node, false);
        if ("minecraft:shifted_noise".equals(type)) return shiftedNoise(node, interpolateMarkers);
        if ("minecraft:old_blended_noise".equals(type)) return oldBlendedNoise(node);
        if ("minecraft:weird_scaled_sampler".equals(type)) return weirdScaledSampler(node, interpolateMarkers);
        if ("minecraft:spline".equals(type)) return spline(node.getAsJsonObject("spline"), interpolateMarkers);
        if ("minecraft:clamp".equals(type)) return clamp(node, interpolateMarkers);
        if ("minecraft:range_choice".equals(type)) return rangeChoice(node, interpolateMarkers);
        if ("lithostitched:fast_noise".equals(type)) return fastNoise(node, interpolateMarkers);
        throw new UnsupportedOperationException("Modern density type is not implemented yet: " + type);
    }

    private Function binary(JsonObject node, final Binary operation, boolean interpolateMarkers) {
        final Function first = compile(node.get("argument1"), interpolateMarkers);
        final Function second = compile(node.get("argument2"), interpolateMarkers);
        return new Function() {

            @Override
            public double sample(Context context) {
                double left = first.sample(context);
                if (operation == Binary.ADD) return left + second.sample(context);
                if (operation == Binary.MULTIPLY) return left == 0.0D ? 0.0D : left * second.sample(context);
                if (operation == Binary.MINIMUM) return Math.min(left, second.sample(context));
                return Math.max(left, second.sample(context));
            }
        };
    }

    private Function mapped(JsonObject node, final Mapping mapping, boolean interpolateMarkers) {
        final Function argument = compile(node.get("argument"), interpolateMarkers);
        return new Function() {

            @Override
            public double sample(Context context) {
                double value = argument.sample(context);
                switch (mapping) {
                    case ABSOLUTE:
                        return Math.abs(value);
                    case SQUARE:
                        return value * value;
                    case CUBE:
                        return value * value * value;
                    case HALF_NEGATIVE:
                        return value > 0.0D ? value : value * 0.5D;
                    case QUARTER_NEGATIVE:
                        return value > 0.0D ? value : value * 0.25D;
                    case SQUEEZE:
                        value = clamp(value, -1.0D, 1.0D);
                        return value / 2.0D - value * value * value / 24.0D;
                    default:
                        throw new AssertionError(mapping);
                }
            }
        };
    }

    private Function yGradient(JsonObject node) {
        final int fromY = node.get("from_y")
            .getAsInt();
        final int toY = node.get("to_y")
            .getAsInt();
        final double fromValue = node.get("from_value")
            .getAsDouble();
        final double toValue = node.get("to_value")
            .getAsDouble();
        return new Function() {

            @Override
            public double sample(Context context) {
                double delta = clamp((context.y - fromY) / (double) (toY - fromY), 0.0D, 1.0D);
                return fromValue + delta * (toValue - fromValue);
            }
        };
    }

    private Function noise(JsonObject node) {
        final ModernNormalNoise noise = noise(string(node, "noise"));
        final double xzScale = node.has("xz_scale") ? node.get("xz_scale")
            .getAsDouble() : 1.0D;
        final double yScale = node.has("y_scale") ? node.get("y_scale")
            .getAsDouble() : 1.0D;
        return new Function() {

            @Override
            public double sample(Context context) {
                return noise.sample(context.x * xzScale, context.y * yScale, context.z * xzScale);
            }
        };
    }

    private Function shift(JsonObject node, final boolean shiftA) {
        final ModernNormalNoise noise = noise(string(node, "argument"));
        return new Function() {

            @Override
            public double sample(Context context) {
                if (shiftA) return noise.sample(context.x * 0.25D, 0.0D, context.z * 0.25D) * 4.0D;
                return noise.sample(context.z * 0.25D, context.x * 0.25D, 0.0D) * 4.0D;
            }
        };
    }

    private Function shiftedNoise(JsonObject node, boolean interpolateMarkers) {
        final Function shiftX = compile(node.get("shift_x"), interpolateMarkers);
        final Function shiftY = compile(node.get("shift_y"), interpolateMarkers);
        final Function shiftZ = compile(node.get("shift_z"), interpolateMarkers);
        final ModernNormalNoise noise = noise(string(node, "noise"));
        final double xzScale = node.get("xz_scale")
            .getAsDouble();
        final double yScale = node.get("y_scale")
            .getAsDouble();
        return new Function() {

            @Override
            public double sample(Context context) {
                return noise.sample(
                    context.x * xzScale + shiftX.sample(context),
                    context.y * yScale + shiftY.sample(context),
                    context.z * xzScale + shiftZ.sample(context));
            }
        };
    }

    private Function oldBlendedNoise(JsonObject node) {
        final ModernBlendedNoise noise = new ModernBlendedNoise(
            worldSeed,
            node.get("xz_scale")
                .getAsDouble(),
            node.get("y_scale")
                .getAsDouble(),
            node.get("xz_factor")
                .getAsDouble(),
            node.get("y_factor")
                .getAsDouble(),
            node.get("smear_scale_multiplier")
                .getAsDouble());
        return new Function() {

            @Override
            public double sample(Context context) {
                return noise.sample(context.x, context.y, context.z);
            }
        };
    }

    private Function weirdScaledSampler(JsonObject node, boolean interpolateMarkers) {
        final Function input = compile(node.get("input"), interpolateMarkers);
        final ModernNormalNoise noise = noise(string(node, "noise"));
        final String mapper = string(node, "rarity_value_mapper");
        return new Function() {

            @Override
            public double sample(Context context) {
                double rarity = rarity(input.sample(context), mapper);
                return rarity * Math.abs(noise.sample(context.x / rarity, context.y / rarity, context.z / rarity));
            }
        };
    }

    private static double rarity(double value, String mapper) {
        if ("type_1".equals(mapper)) {
            if (value < -0.5D) return 0.75D;
            if (value < 0.0D) return 1.0D;
            if (value < 0.5D) return 1.5D;
            return 2.0D;
        }
        if ("type_2".equals(mapper)) {
            if (value < -0.75D) return 0.5D;
            if (value < -0.5D) return 0.75D;
            if (value < 0.5D) return 1.0D;
            if (value < 0.75D) return 2.0D;
            return 3.0D;
        }
        throw new UnsupportedOperationException("Unsupported weird-noise rarity mapper " + mapper);
    }

    private Function spline(JsonObject node, boolean interpolateMarkers) {
        final Function coordinate = named(string(node, "coordinate"), interpolateMarkers);
        JsonArray points = node.getAsJsonArray("points");
        final float[] locations = new float[points.size()];
        final float[] derivatives = new float[points.size()];
        final List<SplineValue> values = new ArrayList<SplineValue>(points.size());
        for (int index = 0; index < points.size(); index++) {
            JsonObject point = points.get(index)
                .getAsJsonObject();
            locations[index] = point.get("location")
                .getAsFloat();
            derivatives[index] = point.get("derivative")
                .getAsFloat();
            JsonElement value = point.get("value");
            if (value.isJsonPrimitive()) {
                values.add(new ConstantSplineValue(value.getAsFloat()));
            } else {
                values.add(new FunctionSplineValue(spline(value.getAsJsonObject(), interpolateMarkers)));
            }
        }
        return new Function() {

            @Override
            public double sample(Context context) {
                float input = (float) coordinate.sample(context);
                int interval = intervalStart(locations, input);
                int last = locations.length - 1;
                if (interval < 0) {
                    return linearExtend(
                        input,
                        locations,
                        values.get(0)
                            .sample(context),
                        derivatives,
                        0);
                }
                if (interval == last) {
                    return linearExtend(
                        input,
                        locations,
                        values.get(last)
                            .sample(context),
                        derivatives,
                        last);
                }
                float leftLocation = locations[interval];
                float rightLocation = locations[interval + 1];
                float delta = (input - leftLocation) / (rightLocation - leftLocation);
                float left = values.get(interval)
                    .sample(context);
                float right = values.get(interval + 1)
                    .sample(context);
                float leftTerm = derivatives[interval] * (rightLocation - leftLocation) - (right - left);
                float rightTerm = -derivatives[interval + 1] * (rightLocation - leftLocation) + (right - left);
                return lerp(delta, left, right) + delta * (1.0F - delta) * lerp(delta, leftTerm, rightTerm);
            }
        };
    }

    private Function clamp(JsonObject node, boolean interpolateMarkers) {
        final Function input = compile(node.get("input"), interpolateMarkers);
        final double minimum = node.get("min")
            .getAsDouble();
        final double maximum = node.get("max")
            .getAsDouble();
        return new Function() {

            @Override
            public double sample(Context context) {
                return clamp(input.sample(context), minimum, maximum);
            }
        };
    }

    private Function rangeChoice(JsonObject node, boolean interpolateMarkers) {
        final Function input = compile(node.get("input"), interpolateMarkers);
        final double minimum = node.get("min_inclusive")
            .getAsDouble();
        final double maximum = node.get("max_exclusive")
            .getAsDouble();
        final Function inRange = compile(node.get("when_in_range"), interpolateMarkers);
        final Function outOfRange = compile(node.get("when_out_of_range"), interpolateMarkers);
        return new Function() {

            @Override
            public double sample(Context context) {
                double value = input.sample(context);
                return value >= minimum && value < maximum ? inRange.sample(context) : outOfRange.sample(context);
            }
        };
    }

    private ModernNormalNoise noise(String identifier) {
        ModernNormalNoise result = noises.get(identifier);
        if (result != null) return result;
        JsonObject parameters = noiseParameters.getAsJsonObject(identifier);
        if (parameters == null) throw new IllegalArgumentException("Unknown modern noise " + identifier);
        JsonArray amplitudesJson = parameters.getAsJsonArray("amplitudes");
        double[] amplitudes = new double[amplitudesJson.size()];
        for (int index = 0; index < amplitudes.length; index++) {
            amplitudes[index] = amplitudesJson.get(index)
                .getAsDouble();
        }
        result = ModernNormalNoise.named(
            worldSeed,
            identifier,
            parameters.get("firstOctave")
                .getAsInt(),
            amplitudes);
        ModernNormalNoise raced = noises.putIfAbsent(identifier, result);
        return raced != null ? raced : result;
    }

    private Function fastNoise(JsonObject node, boolean interpolateMarkers) {
        final Function shiftX = node.has("shift_x") ? compile(node.get("shift_x"), interpolateMarkers) : constant(0.0D);
        final Function shiftY = node.has("shift_y") ? compile(node.get("shift_y"), interpolateMarkers) : constant(0.0D);
        final Function shiftZ = node.has("shift_z") ? compile(node.get("shift_z"), interpolateMarkers) : constant(0.0D);
        final double xzScale = node.has("xz_scale") ? node.get("xz_scale")
            .getAsDouble() : 1.0D;
        final double yScale = node.has("y_scale") ? node.get("y_scale")
            .getAsDouble() : 1.0D;
        final ModernFastNoiseCellular noise = fastNoise(string(node, "config"));
        return new Function() {

            @Override
            public double sample(Context context) {
                return noise.sample(
                    context.x * xzScale + shiftX.sample(context),
                    context.y * yScale + shiftY.sample(context),
                    context.z * xzScale + shiftZ.sample(context));
            }
        };
    }

    private ModernFastNoiseCellular fastNoise(String identifier) {
        ModernFastNoiseCellular result = fastNoises.get(identifier);
        if (result != null) return result;
        JsonObject parameters = fastNoiseParameters.getAsJsonObject(identifier);
        if (parameters == null) throw new IllegalArgumentException("Unknown modern fast noise " + identifier);
        if (!"lithostitched:cellular".equals(string(parameters, "type"))
            || !"euclidean".equals(string(parameters, "distance_function"))) {
            throw new UnsupportedOperationException("Unsupported pinned fast noise config " + identifier);
        }
        String returnType = string(parameters, "return_type");
        ModernFastNoiseCellular.ReturnType mappedReturn;
        if ("cell_value".equals(returnType)) {
            mappedReturn = ModernFastNoiseCellular.ReturnType.CELL_VALUE;
        } else if ("distance_2_sub".equals(returnType)) {
            mappedReturn = ModernFastNoiseCellular.ReturnType.DISTANCE_2_SUB;
        } else {
            throw new UnsupportedOperationException("Unsupported fast noise return type " + returnType);
        }
        result = new ModernFastNoiseCellular(
            worldSeed,
            parameters.get("salt")
                .getAsInt(),
            parameters.get("frequency")
                .getAsFloat(),
            parameters.get("jitter")
                .getAsFloat(),
            mappedReturn);
        fastNoises.put(identifier, result);
        return result;
    }

    private static Function interpolated(final Function argument) {
        return new CachedFunction() {

            @Override
            protected double sampleUncached(Context context) {
                final int cellWidth = 4;
                final int cellHeight = 8;
                int lowerX = Math.floorDiv(context.x, cellWidth) * cellWidth;
                int lowerY = Math.floorDiv(context.y, cellHeight) * cellHeight;
                int lowerZ = Math.floorDiv(context.z, cellWidth) * cellWidth;
                int upperX = lowerX + cellWidth;
                int upperY = lowerY + cellHeight;
                int upperZ = lowerZ + cellWidth;
                double deltaX = (context.x - lowerX) / (double) cellWidth;
                double deltaY = (context.y - lowerY) / (double) cellHeight;
                double deltaZ = (context.z - lowerZ) / (double) cellWidth;

                double valueXZ00 = lerp(
                    deltaY,
                    cachedArgument(context, argument, lowerX, lowerY, lowerZ),
                    cachedArgument(context, argument, lowerX, upperY, lowerZ));
                double valueXZ10 = lerp(
                    deltaY,
                    cachedArgument(context, argument, upperX, lowerY, lowerZ),
                    cachedArgument(context, argument, upperX, upperY, lowerZ));
                double valueXZ01 = lerp(
                    deltaY,
                    cachedArgument(context, argument, lowerX, lowerY, upperZ),
                    cachedArgument(context, argument, lowerX, upperY, upperZ));
                double valueXZ11 = lerp(
                    deltaY,
                    cachedArgument(context, argument, upperX, lowerY, upperZ),
                    cachedArgument(context, argument, upperX, upperY, upperZ));
                double valueZ0 = lerp(deltaX, valueXZ00, valueXZ10);
                double valueZ1 = lerp(deltaX, valueXZ01, valueXZ11);
                return lerp(deltaZ, valueZ0, valueZ1);
            }

            private double cachedArgument(Context context, Function argument, int x, int y, int z) {
                long key = key3d(x, y, z);
                Double cached = context.evaluation.get(argument, key);
                if (cached != null) return cached.doubleValue();
                double value = argument.sample(context.at(x, y, z));
                context.evaluation.put(argument, key, value);
                return value;
            }
        };
    }

    private static Function flatCached(final Function argument) {
        return new CachedFunction() {

            @Override
            protected double sampleUncached(Context context) {
                int quartX = Math.floorDiv(context.x, 4) * 4;
                int quartZ = Math.floorDiv(context.z, 4) * 4;
                long key = key2d(quartX, quartZ);
                Double cached = context.evaluation.get(this, key);
                if (cached != null) return cached.doubleValue();
                double value = argument.sample(context.at(quartX, 0, quartZ));
                context.evaluation.put(this, key, value);
                return value;
            }
        };
    }

    private static Function cache2d(final Function argument) {
        return new CachedFunction() {

            @Override
            protected double sampleUncached(Context context) {
                long key = key2d(context.x, context.z);
                Double cached = context.evaluation.get(this, key);
                if (cached != null) return cached.doubleValue();
                double value = argument.sample(context);
                context.evaluation.put(this, key, value);
                return value;
            }
        };
    }

    private static Function cacheOnce(final Function argument) {
        return new Function() {

            @Override
            public double sample(Context context) {
                Double cached = context.once.get(this);
                if (cached != null) return cached.doubleValue();
                double value = argument.sample(context);
                context.once.put(this, Double.valueOf(value));
                return value;
            }
        };
    }

    private static Function constant(final double value) {
        return new Function() {

            @Override
            public double sample(Context context) {
                return value;
            }
        };
    }

    private static int intervalStart(float[] values, float target) {
        int low = 0;
        int high = values.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (target < values[middle]) high = middle;
            else low = middle + 1;
        }
        return low - 1;
    }

    private static float linearExtend(float input, float[] locations, float value, float[] derivatives, int index) {
        float derivative = derivatives[index];
        return derivative == 0.0F ? value : value + derivative * (input - locations[index]);
    }

    private static float lerp(float delta, float start, float end) {
        return start + delta * (end - start);
    }

    private static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null ? null : value.getAsString();
    }

    private static JsonObject load() {
        InputStream input = ModernDensityGraph.class.getResourceAsStream(RESOURCE);
        if (input == null) throw new IllegalStateException("Missing resolved modern worldgen graph " + RESOURCE);
        try {
            byte[] payload = readFully(input);
            if (!SHA256.equals(sha256(payload))) {
                throw new IllegalArgumentException("resolved modern worldgen graph hash drift");
            }
            JsonObject root = new JsonParser()
                .parse(new InputStreamReader(new ByteArrayInputStream(payload), StandardCharsets.UTF_8))
                .getAsJsonObject();
            if (!"biomic-modern-worldgen-graph-v1".equals(string(root, "format"))) {
                throw new IllegalArgumentException("resolved modern worldgen format drift");
            }
            if (!RuModernParitySpec.SOURCE_REVISION.equals(string(root, "revision"))) {
                throw new IllegalArgumentException("resolved modern worldgen source drift");
            }
            if (root.getAsJsonObject("density_functions")
                .entrySet()
                .size() != 41
                || root.getAsJsonObject("noises")
                    .entrySet()
                    .size() != 66) {
                throw new IllegalArgumentException("resolved modern registry closure is incomplete");
            }
            return root;
        } catch (IOException failure) {
            throw new IllegalStateException("Could not read resolved modern worldgen graph", failure);
        }
    }

    private static byte[] readFully(InputStream input) throws IOException {
        try (InputStream stream = input; ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) >= 0) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private static String sha256(byte[] payload) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(payload);
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest) result.append(String.format("%02x", value & 0xFF));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError("Every Java runtime must provide SHA-256", impossible);
        }
    }

    private interface Function {

        double sample(Context context);
    }

    private abstract static class CachedFunction implements Function {

        @Override
        public final double sample(Context context) {
            return sampleUncached(context);
        }

        protected abstract double sampleUncached(Context context);
    }

    private interface SplineValue {

        float sample(Context context);
    }

    private static final class ConstantSplineValue implements SplineValue {

        private final float value;

        private ConstantSplineValue(float value) {
            this.value = value;
        }

        @Override
        public float sample(Context context) {
            return value;
        }
    }

    private static final class FunctionSplineValue implements SplineValue {

        private final Function function;

        private FunctionSplineValue(Function function) {
            this.function = function;
        }

        @Override
        public float sample(Context context) {
            return (float) function.sample(context);
        }
    }

    private static final class Context {

        private final int x;
        private final int y;
        private final int z;
        private final EvaluationCache evaluation;
        private final Map<Function, Double> once = new IdentityHashMap<Function, Double>();

        private Context(int x, int y, int z, EvaluationCache evaluation) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.evaluation = evaluation;
        }

        private Context at(int newX, int newY, int newZ) {
            return new Context(newX, newY, newZ, evaluation);
        }
    }

    private static final class SamplingSession {

        private final EvaluationCache cache = new EvaluationCache();
    }

    private static final class EvaluationCache {

        private final Map<Function, Map<Long, Double>> values = new IdentityHashMap<Function, Map<Long, Double>>();

        private Double get(Function function, long key) {
            Map<Long, Double> functionValues = values.get(function);
            return functionValues == null ? null : functionValues.get(Long.valueOf(key));
        }

        private void put(Function function, long key, double value) {
            Map<Long, Double> functionValues = values.get(function);
            if (functionValues == null) {
                functionValues = new HashMap<Long, Double>();
                values.put(function, functionValues);
            }
            functionValues.put(Long.valueOf(key), Double.valueOf(value));
        }
    }

    private static long key2d(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static long key3d(int x, int y, int z) {
        return ((x & 0x3FFFFFFL) << 38) | ((z & 0x3FFFFFFL) << 12) | (y & 0xFFFL);
    }

    private enum Binary {
        ADD,
        MULTIPLY,
        MINIMUM,
        MAXIMUM
    }

    private enum Mapping {
        ABSOLUTE,
        SQUARE,
        CUBE,
        HALF_NEGATIVE,
        QUARTER_NEGATIVE,
        SQUEEZE
    }
}
