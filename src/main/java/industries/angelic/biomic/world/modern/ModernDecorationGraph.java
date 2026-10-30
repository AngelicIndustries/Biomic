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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Immutable resolved post-NeoForge-modifier decoration graph for the pinned modern target. */
public final class ModernDecorationGraph {

    public static final String RESOURCE = "/assets/biomic/ru_modern_parity/resolved-modern-decoration.json";
    public static final String SHA256 = "de29ff89461822f7565206ab911f6e4d30015eaef09f7dc06bc9ea6336046b75";
    /**
     * Regions Unexplored's four Nether biomes and their own features, from RU's generated data at the same revision
     * (tools/ru_nether_decoration.py). Their features follow the overworld's in each step's order, so no overworld
     * feature changes index (and so seed).
     */
    public static final String NETHER_RESOURCE = "/assets/biomic/ru_modern_parity/resolved-nether-decoration.json";
    public static final String NETHER_SHA256 = "4c2a763c44465575394e925f05b728261475bb7dfd719a6aea595d721acd3d72";
    /** Ecology file naming, per placed feature, the configured feature it places instead (EcologyOverlays). */
    public static final String OVERRIDES = "modern-features.json";

    private final Map<String, JsonObject> biomes;
    private final Map<String, JsonObject> netherBiomes;
    private final Map<String, JsonObject> configuredFeatures;
    private final Map<String, JsonObject> placedFeatures;
    private final Map<String, JsonObject> configuredCarvers;
    private final Map<String, Set<String>> blockTags;
    private final Map<String, Set<String>> biomeTags;
    private List<List<String>> featuresPerStep;

    public ModernDecorationGraph() {
        this(industries.angelic.biomic.api.EcologyOverlays.load(OVERRIDES));
    }

    /** The graph with these placed-feature overrides (a composed modern-features.json). */
    ModernDecorationGraph(JsonObject overrides) {
        JsonObject root = load(RESOURCE, SHA256);
        biomes = objects(root.getAsJsonObject("overworld_biomes"));
        // Mutable: the Nether supplement joins them once the overworld closure is validated.
        configuredFeatures = new HashMap<String, JsonObject>(objects(root.getAsJsonObject("configured_features")));
        placedFeatures = new HashMap<String, JsonObject>(objects(root.getAsJsonObject("placed_features")));
        overridePlacedFeatures(overrides);
        configuredCarvers = objects(root.getAsJsonObject("configured_carvers"));
        blockTags = tags(root.getAsJsonObject("block_tags"));
        biomeTags = tags(root.getAsJsonObject("biome_tags"));
        List<List<String>> overworldSteps = steps(root.getAsJsonArray("features_per_step"));
        featuresPerStep = overworldSteps;
        validateClosure();
        JsonObject nether = loadJson(NETHER_RESOURCE, NETHER_SHA256);
        if (!"biomic-modern-decoration-nether-supplement-v1".equals(string(nether, "format"))
            || !RuModernParitySpec.SOURCE_REVISION.equals(string(nether, "revision"))) {
            throw new IllegalArgumentException("resolved Nether decoration drift");
        }
        netherBiomes = objects(nether.getAsJsonObject("nether_biomes"));
        for (Map.Entry<String, JsonObject> entry : objects(nether.getAsJsonObject("configured_features")).entrySet())
            if (configuredFeatures.put(entry.getKey(), entry.getValue()) != null)
                throw new IllegalArgumentException("Nether supplement redefines " + entry.getKey());
        for (Map.Entry<String, JsonObject> entry : objects(nether.getAsJsonObject("placed_features")).entrySet())
            if (placedFeatures.put(entry.getKey(), entry.getValue()) != null)
                throw new IllegalArgumentException("Nether supplement redefines " + entry.getKey());
        List<List<String>> netherSteps = steps(nether.getAsJsonArray("features_per_step"));
        List<List<String>> merged = new ArrayList<List<String>>(overworldSteps.size());
        for (int step = 0; step < overworldSteps.size(); step++) {
            List<String> order = new ArrayList<String>(overworldSteps.get(step));
            if (step < netherSteps.size()) order.addAll(netherSteps.get(step));
            merged.add(Collections.unmodifiableList(order));
        }
        featuresPerStep = Collections.unmodifiableList(merged);
        validateNether();
    }

    public Set<String> biomeIds() {
        return biomes.keySet();
    }

    public JsonObject biome(String identifier) {
        JsonObject nether = netherBiomes.get(identifier);
        return nether != null ? nether : required(biomes, identifier, "biome");
    }

    /** Whether the graph holds this biome: an overworld one ({@link #biomeIds}) or one of RU's Nether biomes. */
    public boolean hasBiome(String identifier) {
        return biomes.containsKey(identifier) || netherBiomes.containsKey(identifier);
    }

    public JsonObject configuredFeature(String identifier) {
        return required(configuredFeatures, identifier, "configured feature");
    }

    public JsonObject placedFeature(String identifier) {
        return required(placedFeatures, identifier, "placed feature");
    }

    public Set<String> blockTag(String identifier) {
        return tag(blockTags, identifier);
    }

    public Set<String> biomeTag(String identifier) {
        return tag(biomeTags, identifier);
    }

    public List<String> featureOrder(int step) {
        return step >= 0 && step < featuresPerStep.size() ? featuresPerStep.get(step) : Collections.<String>emptyList();
    }

    /** Mirrors FeatureSorter: union the neighboring biomes, then retain global step order. */
    public List<String> orderedFeatures(Set<String> neighboringBiomes, int step) {
        Set<String> selected = new HashSet<String>();
        for (String biome : neighboringBiomes) {
            JsonArray steps = biome(biome).getAsJsonArray("features");
            if (step >= steps.size()) continue;
            for (JsonElement feature : steps.get(step)
                .getAsJsonArray()) {
                selected.add(feature.getAsString());
            }
        }
        List<String> ordered = new ArrayList<String>(selected.size());
        for (String feature : featureOrder(step)) {
            if (selected.contains(feature)) ordered.add(feature);
        }
        if (ordered.size() != selected.size()) {
            selected.removeAll(ordered);
            throw new IllegalStateException("Features absent from pinned global order: " + selected);
        }
        return Collections.unmodifiableList(ordered);
    }

    private void validateClosure() {
        int expectedBiomes = 113, expectedConfigured = 468, expectedPlaced = 427, expectedCarvers = 3;
        int expectedSteps = 11, expectedEntries = 388;
        if (biomes.size() != expectedBiomes || configuredFeatures.size() != expectedConfigured
            || placedFeatures.size() != expectedPlaced
            || configuredCarvers.size() != expectedCarvers) {
            throw new IllegalArgumentException("resolved modern decoration closure is incomplete");
        }
        if (featuresPerStep.size() != expectedSteps) {
            throw new IllegalArgumentException("resolved modern decoration step count drift");
        }
        Set<String> ordered = new LinkedHashSet<String>();
        int orderedEntries = 0;
        for (List<String> step : featuresPerStep) {
            ordered.addAll(step);
            orderedEntries += step.size();
        }
        if (orderedEntries != expectedEntries) {
            throw new IllegalArgumentException("resolved modern global feature order drift");
        }
        for (String identifier : ordered) {
            if (!placedFeatures.containsKey(identifier)) {
                throw new IllegalArgumentException("global order references missing feature " + identifier);
            }
        }
        for (Map.Entry<String, JsonObject> placed : placedFeatures.entrySet()) {
            JsonElement configured = placed.getValue()
                .get("feature");
            if (configured == null || !configured.isJsonPrimitive()
                || !configuredFeatures.containsKey(configured.getAsString())) {
                throw new IllegalArgumentException(
                    placed.getKey() + " references missing configured feature " + configured);
            }
        }
        for (Map.Entry<String, JsonObject> biome : biomes.entrySet()) {
            JsonArray steps = biome.getValue()
                .getAsJsonArray("features");
            for (JsonElement step : steps) {
                for (JsonElement feature : step.getAsJsonArray()) {
                    if (!placedFeatures.containsKey(feature.getAsString())) {
                        throw new IllegalArgumentException(
                            biome.getKey() + " references missing placed feature " + feature);
                    }
                }
            }
        }
    }

    private void validateNether() {
        for (Map.Entry<String, JsonObject> biome : netherBiomes.entrySet()) {
            for (JsonElement step : biome.getValue()
                .getAsJsonArray("features")) {
                for (JsonElement feature : step.getAsJsonArray()) {
                    JsonObject placed = placedFeatures.get(feature.getAsString());
                    if (placed == null || !configuredFeatures.containsKey(
                        placed.get("feature")
                            .getAsString()))
                        throw new IllegalArgumentException(biome.getKey() + " references missing feature " + feature);
                }
            }
        }
    }

    private static JsonObject required(Map<String, JsonObject> values, String identifier, String kind) {
        JsonObject value = values.get(identifier);
        if (value == null) throw new IllegalArgumentException("Unknown pinned " + kind + " " + identifier);
        return value;
    }

    private static Set<String> tag(Map<String, Set<String>> tags, String identifier) {
        if (identifier.startsWith("#")) identifier = identifier.substring(1);
        Set<String> value = tags.get(identifier);
        return value == null ? Collections.<String>emptySet() : value;
    }

    private static Map<String, JsonObject> objects(JsonObject source) {
        Map<String, JsonObject> result = new HashMap<String, JsonObject>();
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            result.put(
                entry.getKey(),
                entry.getValue()
                    .getAsJsonObject());
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map<String, Set<String>> tags(JsonObject source) {
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
            Set<String> values = new HashSet<String>();
            for (JsonElement value : entry.getValue()
                .getAsJsonArray()) values.add(value.getAsString());
            result.put(entry.getKey(), Collections.unmodifiableSet(values));
        }
        return Collections.unmodifiableMap(result);
    }

    private static List<List<String>> steps(JsonArray source) {
        List<List<String>> result = new ArrayList<List<String>>(source.size());
        for (JsonElement element : source) {
            List<String> step = new ArrayList<String>();
            for (JsonElement value : element.getAsJsonArray()) step.add(value.getAsString());
            result.add(Collections.unmodifiableList(step));
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * A placed feature placing another configured feature (its placement, and so its random, unchanged): the Bamboo
     * Forest's second tree group can grow Regions Unexplored's bamboo tree like its first.
     */
    private void overridePlacedFeatures(JsonObject overrides) {
        if (overrides.get("format")
            .getAsInt() != 1) throw new IllegalStateException("Unknown modern features format");
        if (!overrides.has("placed")) return;
        for (Map.Entry<String, com.google.gson.JsonElement> override : overrides.getAsJsonObject("placed")
            .entrySet()) {
            JsonObject placed = placedFeatures.get(override.getKey());
            if (placed == null)
                throw new IllegalStateException("No placed feature " + override.getKey() + " to override");
            String configured = override.getValue()
                .getAsString();
            if (!configuredFeatures.containsKey(configured))
                throw new IllegalStateException("No configured feature " + configured + " for " + override.getKey());
            placed.addProperty("feature", configured);
        }
    }

    private static JsonObject load(String resource, String hash) {
        JsonObject root = loadJson(resource, hash);
        try {
            if (!"biomic-modern-decoration-closure-v1".equals(string(root, "format"))) {
                throw new IllegalArgumentException("resolved modern decoration format drift");
            }
            if (!RuModernParitySpec.SOURCE_REVISION.equals(string(root, "revision"))) {
                throw new IllegalArgumentException("resolved modern decoration source drift");
            }
            return root;
        } catch (RuntimeException failure) {
            throw failure;
        }
    }

    private static JsonObject loadJson(String resource, String hash) {
        InputStream input = ModernDecorationGraph.class.getResourceAsStream(resource);
        if (input == null) throw new IllegalStateException("Missing resolved modern decoration graph " + resource);
        try {
            byte[] payload = readFully(input);
            if (!hash.equals(sha256(payload))) {
                throw new IllegalArgumentException("resolved modern decoration graph hash drift");
            }
            return new JsonParser()
                .parse(new InputStreamReader(new ByteArrayInputStream(payload), StandardCharsets.UTF_8))
                .getAsJsonObject();
        } catch (IOException failure) {
            throw new IllegalStateException("Could not read resolved modern decoration graph", failure);
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null ? null : value.getAsString();
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
}
