package industries.angelic.biomic.world.terrain;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.biomic.world.modern.ModernDensityGraph;
import industries.angelic.biomic.world.modern.ModernSurfaceSystem;

/** Executes the selected RU biome-local surface rules, without invoking modern terrain generation. */
public final class HostedIslandSurfacePalette {

    private static final JsonObject ROOT = load();
    private static final JsonObject RULES = ROOT.getAsJsonObject("rules");
    private final ModernDensityGraph noise;
    private final industries.angelic.biomic.world.modern.noise.ModernPositionalRandom random;
    private final java.util.function.IntBinaryOperator surfaceHeight;
    private final HostedSurfaceHeightProjection heights;
    private final long seed;
    private volatile industries.angelic.biomic.compat.biome.BopSurfaceNoise nativeSurfaceNoise;

    public HostedIslandSurfacePalette(long seed, java.util.function.IntBinaryOperator surfaceHeight) {
        this(seed, surfaceHeight, HostedSurfaceHeightProjection.legacy());
    }

    /** Explicit tall path; existing constructors retain the historical compressed transform byte-for-byte. */
    public HostedIslandSurfacePalette(long seed, java.util.function.IntBinaryOperator surfaceHeight,
        AuthoredVerticalProjection projection) {
        this(seed, surfaceHeight, HostedSurfaceHeightProjection.shifted512(projection));
    }

    private HostedIslandSurfacePalette(long seed, java.util.function.IntBinaryOperator surfaceHeight,
        HostedSurfaceHeightProjection heights) {
        if (surfaceHeight == null || heights == null) {
            throw new IllegalArgumentException("Missing hosted surface inputs");
        }
        this.seed = seed;
        noise = new ModernDensityGraph(seed);
        random = new industries.angelic.biomic.world.modern.noise.ModernXoroshiroRandom(seed).forkPositional();
        this.surfaceHeight = surfaceHeight;
        this.heights = heights;
    }

    private synchronized industries.angelic.biomic.compat.biome.BopSurfaceNoise nativeSurfaceNoise() {
        if (nativeSurfaceNoise == null)
            nativeSurfaceNoise = new industries.angelic.biomic.compat.biome.BopSurfaceNoise(seed);
        return nativeSurfaceNoise;
    }

    public static boolean hasGeologicalSkin(String biome) {
        return "regions_unexplored:chalk_cliffs".equals(biome) || "regions_unexplored:icy_heights".equals(biome)
            || "regions_unexplored:spires".equals(biome);
    }

    /** Cave-local source rules on an existing solid host. Null deliberately retains its original geology. */
    public String caveState(String biome, boolean floor, int x, int y, int z, int waterHeight) {
        String rule = "regions_unexplored:overworld/caves/" + biome.substring(biome.indexOf(':') + 1);
        return RULES.has(rule)
            ? evaluate(RULES.getAsJsonObject(rule), biome, floor ? 0 : Integer.MAX_VALUE, x, y, z, waterHeight)
            : null;
    }

    private int primaryDepth(int x, int z) {
        return Math.max(
            0,
            (int) (noise.sampleNoise("minecraft:surface", x, 0, z) * 2.75 + 3
                + random.at(x, 0, z)
                    .nextDouble() * .25));
    }

    /** Source secondary-depth rule, squeezed in Y only; never fills air or changes a landform's silhouette. */
    public int layerCount(String biome, int x, int floor, int z) {
        if (!hasGeologicalSkin(biome)) return 3;
        int depth = primaryDepth(x, z) + (int) ((noise.sampleNoise("minecraft:surface_secondary", x, 0, z) + 1) * 15);
        int layers = 1;
        heights.sourceY(floor);
        while (layers < 48 && heights.containsLowerLayer(floor - layers)
            && heights.sourceDepth(floor, floor - layers) <= depth) layers++;
        return layers;
    }

    /** Water height is the first air block above the local fluid run, or MIN_VALUE for dry soil. */
    public String state(String biome, int depth, int x, int y, int z, int waterHeight) {
        if (depth < 0) return null;
        heights.sourceY(y);
        heights.sourceY(y + depth);
        if (waterHeight != Integer.MIN_VALUE) heights.sourceY(waterHeight);
        if (depth >= layerCount(biome, x, y + depth, z)) return null;
        if (industries.angelic.biomic.compat.biome.BopNativeBiomes.handles(biome))
            return industries.angelic.biomic.compat.biome.BopNativeBiomes.soil(
                biome,
                waterHeight != Integer.MIN_VALUE ? Math.max(1, depth) : depth,
                industries.angelic.biomic.compat.biome.BopNativeBiomes.usesSurfaceNoise(biome)
                    ? nativeSurfaceNoise().sample(x, z)
                    : 0);
        if (industries.angelic.biomic.compat.biome.SaltMarshCompat.BIOME.equals(biome))
            return industries.angelic.biomic.compat.biome.SaltMarshCompat
                .saltSoil(waterHeight != Integer.MIN_VALUE ? Math.max(1, depth) : depth);
        if (hasGeologicalSkin(biome)) {
            int modernDepth = heights.sourceDepth(y + depth, y);
            if ("regions_unexplored:chalk_cliffs".equals(biome) && depth > 0) return "regions_unexplored:chalk";
            if (!"regions_unexplored:chalk_cliffs".equals(biome) && modernDepth > primaryDepth(x, z))
                return "minecraft:packed_ice";
        }
        // The pinned target's normal desert rule applies to both these habitats.
        if ("minecraft:desert".equals(biome)) {
            return "minecraft:sand";
        }
        String name = biome.substring(biome.indexOf(':') + 1);
        String both = "regions_unexplored:overworld/surface_and_under_surface/" + name;
        if (RULES.has(both)) {
            String result = evaluate(RULES.getAsJsonObject(both), biome, depth, x, y, z, waterHeight);
            if (result != null) return result;
        }
        for (String common : new String[] { "gravel", "sand", "snow" }) {
            String result = evaluate(
                RULES.getAsJsonObject("regions_unexplored:overworld/surface_and_under_surface/common/" + common),
                biome,
                depth,
                x,
                y,
                z,
                waterHeight);
            if (result != null) return result;
        }
        boolean exposed = depth == 0 && (waterHeight == Integer.MIN_VALUE || y >= waterHeight - 1);
        String layer = exposed ? "surface/" : "under_surface/";
        String rule = "regions_unexplored:overworld/" + layer + name;
        String result = RULES.has(rule) ? evaluate(RULES.getAsJsonObject(rule), biome, depth, x, y, z, waterHeight)
            : null;
        if (result != null) return result;
        // The established dry-biome blend intentionally retains vanilla dirt. Newly admitted
        // wetlands need the source's peat/silt substrates, including their underwater variants.
        if (HostedVegetationPolicy.hasWetSurface(biome)) {
            for (String common : new String[] { "peat", "silt" }) {
                result = evaluate(
                    RULES.getAsJsonObject("regions_unexplored:overworld/" + layer + "common/" + common),
                    biome,
                    depth,
                    x,
                    y,
                    z,
                    waterHeight);
                if (result != null) return result;
            }
        }
        return exposed ? "minecraft:grass_block" : "minecraft:dirt";
    }

    private String evaluate(JsonObject rule, String biome, int depth, int x, int y, int z, int waterHeight) {
        String type = rule.get("type")
            .getAsString();
        if ("regions_unexplored:config".equals(type)) {
            if (!"custom_dirts".equals(
                rule.get("key")
                    .getAsString())) {
                throw new IllegalStateException("Unaudited RU palette switch " + rule);
            }
            return evaluate(
                rule.getAsJsonObject(
                    ROOT.getAsJsonObject("config")
                        .get("custom_dirts")
                        .getAsBoolean() ? "on_enabled" : "on_disabled"),
                biome,
                depth,
                x,
                y,
                z,
                waterHeight);
        }
        if ("minecraft:block".equals(type)) {
            JsonObject block = rule.getAsJsonObject("result_state");
            String state = block.get("Name")
                .getAsString();
            if (!block.has("Properties")) return state;
            TreeMap<String, String> properties = new TreeMap<>();
            for (Map.Entry<String, JsonElement> entry : block.getAsJsonObject("Properties")
                .entrySet()) {
                properties.put(
                    entry.getKey(),
                    entry.getValue()
                        .getAsString());
            }
            StringBuilder out = new StringBuilder(state).append('[');
            properties.forEach(
                (k, v) -> out.append(k)
                    .append('=')
                    .append(v)
                    .append(','));
            out.setCharAt(out.length() - 1, ']');
            return out.toString();
        }
        if ("minecraft:sequence".equals(type)) {
            for (JsonElement child : rule.getAsJsonArray("sequence")) {
                String state = evaluate(child.getAsJsonObject(), biome, depth, x, y, z, waterHeight);
                if (state != null) return state;
            }
            return null;
        }
        if ("minecraft:condition".equals(type)) {
            JsonObject condition = rule.getAsJsonObject("if_true");
            return matches(condition, biome, depth, x, y, z, waterHeight)
                ? evaluate(rule.getAsJsonObject("then_run"), biome, depth, x, y, z, waterHeight)
                : null;
        }
        throw new IllegalStateException("Unaudited hosted surface rule " + type);
    }

    private boolean matches(JsonObject condition, String biome, int depth, int x, int y, int z, int waterHeight) {
        String kind = condition.get("type")
            .getAsString();
        boolean matches;
        if ("minecraft:vertical_gradient".equals(kind)) {
            int below = condition.getAsJsonObject("true_at_and_below")
                .get("absolute")
                .getAsInt();
            int above = condition.getAsJsonObject("false_at_and_above")
                .get("absolute")
                .getAsInt();
            int modernY = heights.sourceY(y);
            if (modernY <= below) return true;
            if (modernY >= above) return false;
            return random.fromHashOf(
                condition.get("random_name")
                    .getAsString())
                .forkPositional()
                .at(x, modernY, z)
                .nextFloat() < (double) (above - modernY) / (above - below);
        }
        if ("lithostitched:slope".equals(kind)) {
            int minimum = Integer.MAX_VALUE, maximum = Integer.MIN_VALUE;
            // Source uses the four cardinal heights. Deliberately continue over chunk
            // boundaries instead of its edge clamp, which makes visible square seams.
            for (int[] d : new int[][] { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } }) {
                int height = heights
                    .sourceY(Math.max(heights.slopeFloorPhysicalY(), surfaceHeight.applyAsInt(x + d[0], z + d[1])));
                minimum = Math.min(minimum, height);
                maximum = Math.max(maximum, height);
            }
            var range = condition.getAsJsonArray("height_difference");
            return maximum - minimum >= range.get(0)
                .getAsInt() && maximum - minimum
                    <= range.get(1)
                        .getAsInt();
        }
        if ("lithostitched:any_of".equals(kind) || "lithostitched:all_of".equals(kind)) {
            boolean all = "lithostitched:all_of".equals(kind);
            for (JsonElement child : condition.getAsJsonArray("conditions")) {
                boolean result = matches(child.getAsJsonObject(), biome, depth, x, y, z, waterHeight);
                if (all && !result) return false;
                if (!all && result) return true;
            }
            return all;
        }
        if ("minecraft:not".equals(kind)) {
            return !matches(condition.getAsJsonObject("invert"), biome, depth, x, y, z, waterHeight);
        }
        if ("minecraft:y_above".equals(kind)) {
            // Preserve the pinned sea-relative threshold through the legacy-height map.
            // Nonzero surface-noise terms need a richer surface context, not a guess.
            if (condition.get("surface_depth_multiplier")
                .getAsInt() != 0 || condition.get("add_stone_depth")
                    .getAsBoolean()
                || !condition.getAsJsonObject("anchor")
                    .has("absolute")) {
                throw new IllegalStateException("Unaudited hosted height condition " + condition);
            }
            return heights.sourceY(y) >= condition.getAsJsonObject("anchor")
                .get("absolute")
                .getAsInt();
        }
        if ("minecraft:water".equals(kind)) {
            if (condition.get("surface_depth_multiplier")
                .getAsInt() != 0) {
                throw new IllegalStateException("Unaudited hosted water depth condition " + condition);
            }
            return waterHeight == Integer.MIN_VALUE || heights.sourceY(y) + (condition.get("add_stone_depth")
                .getAsBoolean() ? depth + 1 : 0) >= heights.sourceY(waterHeight) + condition.get("offset")
                    .getAsInt();
        }
        if ("lithostitched:biome".equals(kind)) {
            String selector = condition.get("biomes")
                .getAsString();
            if (!selector.startsWith("#")) return biome.equals(selector);
            JsonElement values = ROOT.getAsJsonObject("biome_tags")
                .get(selector.substring(1));
            if (values == null) throw new IllegalStateException("Missing hosted surface biome tag " + selector);
            for (JsonElement id : values.getAsJsonArray()) if (biome.equals(id.getAsString())) return true;
            return false;
        }
        if ("minecraft:biome".equals(kind)) {
            matches = false;
            for (JsonElement id : condition.getAsJsonArray("biome_is")) matches |= biome.equals(id.getAsString());
        } else if ("minecraft:noise_threshold".equals(kind)) {
            double value = noise.sampleNoise(
                condition.get("noise")
                    .getAsString(),
                x,
                0,
                z);
            matches = value >= condition.get("min_threshold")
                .getAsDouble() && value
                    <= condition.get("max_threshold")
                        .getAsDouble();
        } else if ("minecraft:stone_depth".equals(kind)) {
            if ("ceiling".equals(
                condition.get("surface_type")
                    .getAsString()))
                return false;
            if (condition.get("add_surface_depth")
                .getAsBoolean()
                || condition.get("secondary_depth_range")
                    .getAsInt() != 0) {
                throw new IllegalStateException("Unaudited hosted depth condition " + condition);
            }
            matches = depth <= condition.get("offset")
                .getAsInt();
        } else throw new IllegalStateException("Unaudited hosted surface condition " + kind);
        return matches;
    }

    private static JsonObject load() {
        try (InputStreamReader reader = new InputStreamReader(
            HostedIslandSurfacePalette.class.getResourceAsStream(ModernSurfaceSystem.RESOURCE),
            StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader)
                .getAsJsonObject();
        } catch (Exception exception) {
            throw new IllegalStateException("Missing RU surface rules", exception);
        }
    }
}
