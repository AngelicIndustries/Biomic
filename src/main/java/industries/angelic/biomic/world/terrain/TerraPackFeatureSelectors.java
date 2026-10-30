package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.dfsek.terra.api.util.MathUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import industries.angelic.biomic.internal.terra.noise.NoiseSampler;

/**
 * Detached Terra feature distributors, locators and weighted structure selection.
 * State matching/occlusion are typed platform services, never guessed from block names here.
 * All query results use authored Y; this class neither projects nor writes blocks.
 */
public final class TerraPackFeatureSelectors {

    public interface View {

        int minY();

        int maxY();

        long seed();

        String block(int x, int y, int z);

        boolean air(String state);

        boolean solid(String state);

        boolean matches(String expected, String actual);
    }

    public record Anchor(int y, String structure, long randomSeed) {}

    private interface Distributor {

        boolean at(int x, int z, long seed);
    }

    private interface Locator {

        int[] at(View view, int x, int z, int min, int max);
    }

    private interface Pattern {

        boolean at(View view, int x, int y, int z, int min, int max);
    }

    private interface StatePattern {

        boolean matches(View view, String actual, int x, int y, int z);
    }

    private record Choice(String id, int ceiling) {}

    private record Feature(Distributor distributor, Locator locator, NoiseSampler sampler, List<Choice> choices,
        int weight) {}

    private final OrigenTerrainRuntime.Compiler compiler;
    private final Map<String, Feature> features = new LinkedHashMap<>();
    private final Map<String, String> unavailable = new LinkedHashMap<>();

    TerraPackFeatureSelectors(JsonObject manifest) {
        compiler = new OrigenTerrainRuntime.Compiler(
            manifest.getAsJsonObject("functions"),
            manifest.getAsJsonObject("samplers"),
            false);
        for (var entry : manifest.getAsJsonObject("decoration")
            .getAsJsonObject("features")
            .entrySet()) {
            try {
                var data = entry.getValue()
                    .getAsJsonObject();
                var choices = new ArrayList<Choice>();
                int total = 0;
                for (var raw : data.getAsJsonArray("structures")) {
                    var choice = raw.getAsJsonObject();
                    int weight = integer(choice.get("weight"));
                    if (weight < 1 || weight > 65536 || total > 65536 - weight)
                        throw new IllegalArgumentException("Unsupported structure weights");
                    total += weight;
                    choices.add(
                        new Choice(
                            choice.get("structure")
                                .getAsString(),
                            total));
                }
                if (choices.isEmpty()) throw new IllegalArgumentException("Empty structures");
                features.put(
                    entry.getKey(),
                    new Feature(
                        distributor(data.getAsJsonObject("distributor")),
                        locator(data.getAsJsonObject("locator")),
                        compiler.compileSampler(data.getAsJsonObject("structure_sampler")),
                        List.copyOf(choices),
                        total));
            } catch (RuntimeException error) {
                unavailable.put(entry.getKey(), error.toString());
            }
        }
    }

    public Map<String, String> unavailable() {
        return Collections.unmodifiableMap(unavailable);
    }

    public List<Anchor> select(String id, View view, int x, int z, int min, int max) {
        if (view.maxY() - view.minY() > 512 || view.minY() < -1024
            || view.maxY() > 1024
            || min < view.minY()
            || max > view.maxY()
            || min >= max) throw new IllegalArgumentException("Invalid feature column envelope");
        var feature = features.get(id);
        if (feature == null)
            throw new IllegalArgumentException("Unavailable feature " + id + ": " + unavailable.get(id));
        if (!feature.distributor.at(x, z, view.seed())) return List.of();
        List<Anchor> anchors = new ArrayList<>();
        for (int y : feature.locator.at(view, x, z, min, max)) {
            if (y < view.minY() || y >= view.maxY()) continue;
            int index = feature.choices.size() == 1 ? 0
                : MathUtil.normalizeIndex(feature.sampler.noise(view.seed(), x, y, z), feature.weight);
            Choice selected = null;
            for (var choice : feature.choices) if (index < choice.ceiling) {
                selected = choice;
                break;
            }
            if (selected == null) throw new IllegalStateException("Invalid weighted selection");
            anchors.add(new Anchor(y, selected.id, ((view.seed() * 31 + x) * 31 + z) * 31 + y));
        }
        return List.copyOf(anchors);
    }

    private Distributor distributor(JsonObject data) {
        String type = data.get("type")
            .getAsString();
        return switch (type) {
            case "YES" -> (x, z, seed) -> true;
            case "NO" -> (x, z, seed) -> false;
            case "PADDED_GRID" -> {
                int width = integer(data.get("width")), padding = integer(data.get("padding")),
                    salt = integer(data.get("salt"));
                if (width < 1 || padding < 0 || width + padding > 4096)
                    throw new IllegalArgumentException("Unsupported distributor grid");
                int cell = width + padding;
                yield (x, z, seed) -> {
                    int cx = Math.floorDiv(x, cell), cz = Math.floorDiv(z, cell);
                    var random = new Random((MathUtil.murmur64(MathUtil.squash(cx, cz)) ^ seed) + salt);
                    return x == random.nextInt(width) + cx * cell && z == random.nextInt(width) + cz * cell;
                };
            }
            case "SAMPLER" -> {
                var sampler = compiler.compileSampler(data.getAsJsonObject("sampler"));
                double threshold = number(data, "threshold", 0);
                yield (x, z, seed) -> sampler.noise(seed, x, z) < threshold;
            }
            case "AND", "OR", "XOR" -> {
                List<Distributor> children = new ArrayList<>();
                for (var raw : data.getAsJsonArray("distributors")) children.add(distributor(raw.getAsJsonObject()));
                if (children.isEmpty()) throw new IllegalArgumentException("Empty distributor combination");
                yield (x, z, seed) -> {
                    boolean value = children.get(0)
                        .at(x, z, seed);
                    for (int i = 1; i < children.size(); i++) {
                        boolean next = children.get(i)
                            .at(x, z, seed);
                        value = switch (type) {
                            case "AND" -> value && next;
                            case "OR" -> value || next;
                            default -> value ^ next;
                        };
                    }
                    return value;
                };
            }
            default -> throw new IllegalArgumentException("Unsupported distributor " + type);
        };
    }

    private Locator locator(JsonObject data) {
        String type = data.get("type")
            .getAsString();
        return switch (type) {
            case "AND", "OR" -> {
                List<Locator> children = new ArrayList<>();
                for (var raw : data.getAsJsonArray("locators")) children.add(locator(raw.getAsJsonObject()));
                if (children.isEmpty()) throw new IllegalArgumentException("Empty locator combination");
                yield (v, x, z, min, max) -> {
                    var result = children.get(0)
                        .at(v, x, z, min, max);
                    for (int i = 1; i < children.size(); i++) {
                        int[] next = children.get(i)
                            .at(v, x, z, min, max);
                        result = combine(result, next, type.equals("AND"));
                    }
                    return result;
                };
            }
            case "RANDOM", "GAUSSIAN_RANDOM" -> {
                var height = range(data.get("height"));
                var amount = range(data.get("amount"));
                int salt = (int) number(data, "salt", 0);
                double mean = (height.min + height.max) / 2D, sd = number(data, "standard-deviation", 0);
                if (amount.min < 0 || amount.max > 4096 || sd < 0)
                    throw new IllegalArgumentException("Unsupported random locator");
                yield (v, x, z, min, max) -> {
                    var result = new Heights();
                    var random = new Random(31 * (31 * v.seed() + x) + z + salt);
                    int count = amount.get(random);
                    for (int i = 0; i < count; i++) {
                        int y = type.equals("GAUSSIAN_RANDOM") ? (int) (random.nextGaussian() * sd + mean)
                            : height.get(random);
                        if (y >= min && y < max) result.add(y);
                    }
                    return result.toArray();
                };
            }
            case "SAMPLER_3D" -> {
                var sampler = compiler.compileSampler(data.getAsJsonObject("sampler"));
                yield (v, x, z, min, max) -> {
                    var result = new Heights();
                    for (int y = min; y < max; y++) if (sampler.noise(v.seed(), x, y, z) > 0) result.append(y);
                    return result.toArray();
                };
            }
            case "TOP", "SURFACE", "PATTERN", "ADJACENT_PATTERN" -> {
                var search = range(data.get("range"));
                Pattern pattern = type.endsWith("PATTERN") ? pattern(data.getAsJsonObject("pattern")) : null;
                boolean all = data.has("match-all") && data.get("match-all")
                    .getAsBoolean();
                boolean top = type.equals("TOP"), adjacent = type.equals("ADJACENT_PATTERN"),
                    surface = type.equals("SURFACE"), direct = type.equals("PATTERN");
                yield (v, x, z, min, max) -> {
                    var result = new Heights();
                    if (top) {
                        // Terra deliberately ignores the biome clamp here.
                        for (int y = search.max; y >= search.min; y--)
                            if (v.air(v.block(x, y, z)) && !v.air(v.block(x, y - 1, z))) {
                                result.append(y);
                                break;
                            }
                        return result.toArray();
                    }
                    int low = Math.max(adjacent ? v.minY() : min, search.min);
                    int high = Math.min(adjacent ? v.maxY() : max, search.max);
                    for (int y = low; y < high; y++) {
                        boolean match;
                        if (surface) match = v.air(v.block(x, y, z)) && !v.air(v.block(x, y - 1, z));
                        else if (direct) match = pattern.at(v, x, y, z, min, max);
                        else {
                            boolean a = pattern.at(v, x, y, z - 1, v.minY(), v.maxY()),
                                b = pattern.at(v, x, y, z + 1, v.minY(), v.maxY());
                            boolean c = pattern.at(v, x - 1, y, z, v.minY(), v.maxY()),
                                d = pattern.at(v, x + 1, y, z, v.minY(), v.maxY());
                            match = all ? a && b && c && d : a || b || c || d;
                        }
                        if (match) result.append(y);
                    }
                    return result.toArray();
                };
            }
            default -> throw new IllegalArgumentException("Unsupported locator " + type);
        };
    }

    /** Cardinality-sized owned results, with full signed authored coordinates and no per-height objects. */
    private static final class Heights {

        private static final int[] EMPTY = new int[0];
        private int[] values = EMPTY;
        private int size;

        void append(int value) {
            if (size == values.length) values = Arrays.copyOf(values, size == 0 ? 8 : Math.multiplyExact(size, 2));
            values[size++] = value;
        }

        void add(int value) {
            int slot = Arrays.binarySearch(values, 0, size, value);
            if (slot >= 0) return;
            slot = -slot - 1;
            if (size == values.length) values = Arrays.copyOf(values, size == 0 ? 8 : Math.multiplyExact(size, 2));
            System.arraycopy(values, slot, values, slot + 1, size - slot);
            values[slot] = value;
            size++;
        }

        int[] toArray() {
            return size == 0 ? EMPTY : size == values.length ? values : Arrays.copyOf(values, size);
        }
    }

    private static int[] combine(int[] left, int[] right, boolean intersection) {
        var result = new Heights();
        int a = 0, b = 0;
        while (a < left.length && b < right.length) {
            if (left[a] == right[b]) {
                result.append(left[a]);
                a++;
                b++;
            } else if (left[a] < right[b]) {
                if (!intersection) result.append(left[a]);
                a++;
            } else {
                if (!intersection) result.append(right[b]);
                b++;
            }
        }
        if (!intersection) {
            while (a < left.length) result.append(left[a++]);
            while (b < right.length) result.append(right[b++]);
        }
        return result.toArray();
    }

    private Pattern pattern(JsonObject data) {
        String type = data.get("type")
            .getAsString();
        if (type.equals("AND") || type.equals("OR")) {
            List<Pattern> children = new ArrayList<>();
            for (var raw : data.getAsJsonArray("patterns")) children.add(pattern(raw.getAsJsonObject()));
            if (children.isEmpty()) throw new IllegalArgumentException("Empty pattern combination");
            boolean all = type.equals("AND");
            return (v, x, y, z, min, max) -> {
                for (var child : children) if (child.at(v, x, y, z, min, max) != all) return !all;
                return all;
            };
        }
        if (type.equals("NOT")) {
            var child = pattern(data.getAsJsonObject("pattern"));
            return (v, x, y, z, min, max) -> !child.at(v, x, y, z, min, max);
        }
        if (!Set.of("MATCH_AIR", "MATCH_SOLID", "MATCH", "MATCH_SET")
            .contains(type)) throw new IllegalArgumentException("Unsupported pattern " + type);
        var offset = range(data.get("offset"));
        List<String> states = new ArrayList<>();
        if (type.equals("MATCH")) states.add(
            data.get("block")
                .getAsString());
        if (type.equals("MATCH_SET")) for (var raw : data.getAsJsonArray("blocks")) states.add(raw.getAsString());
        String[] expectedStates = states.toArray(new String[0]);
        StatePattern statePattern = switch (type) {
            case "MATCH_AIR" -> (v, actual, x, y, z) -> v.air(actual);
            case "MATCH_SOLID" -> (v, actual, x, y, z) -> v.solid(actual);
            default -> (v, actual, x, y, z) -> {
                for (String expected : expectedStates) if (v.matches(expected, actual)) return true;
                return false;
            };
        };
        return (v, x, y, z, min, max) -> {
            int low = Math.max(min, y + offset.min), high = Math.min(max, y + offset.max);
            if (low >= high) return false;
            for (int yy = low; yy < high; yy++) {
                String state = v.block(x, yy, z);
                if (!statePattern.matches(v, state, x, yy, z)) return false;
            }
            return true;
        };
    }

    private record Range(int min, int max) {

        Range {
            if (min >= max || (long) max - min > 8192) throw new IllegalArgumentException("Unsupported feature range");
        }

        int get(Random random) {
            return min + random.nextInt(max - min);
        }
    }

    private Range range(JsonElement data) {
        if (data.isJsonObject()) return new Range(
            integer(
                data.getAsJsonObject()
                    .get("min")),
            integer(
                data.getAsJsonObject()
                    .get("max")));
        int value = integer(data);
        return new Range(value, Math.addExact(value, 1));
    }

    private int integer(JsonElement value) {
        return (int) compiler.numeric(value);
    }

    private double number(JsonObject data, String key, double fallback) {
        return data.has(key) ? compiler.numeric(data.get(key)) : fallback;
    }
}
