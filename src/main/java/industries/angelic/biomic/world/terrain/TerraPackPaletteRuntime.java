package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import industries.angelic.biomic.internal.terra.noise.NoiseSampler;

/**
 * Imported Terra palette executor shared by isolated SkyLands/Tartarus ports.
 * Returns authored block states, not approximated byte materials. The caller
 * owns source-space depth, density normals, height projection and block mapping.
 * No biome selection, chunk/world access, decoration or registration here.
 */
public final class TerraPackPaletteRuntime {

    private final Map<String, Palette> palettes = new LinkedHashMap<>();
    private final Map<String, Surface> surfaces = new LinkedHashMap<>();
    // Pack-private integer handles are stable only inside this immutable compiled runtime.
    private final Map<String, Integer> surfaceIds = new LinkedHashMap<>();
    private final Surface[] indexedSurfaces;
    private final OrigenTerrainRuntime.Compiler compiler;
    private final boolean derivativeSlant;

    public static TerraPackPaletteRuntime tartarus(JsonObject manifest) {
        return new TerraPackPaletteRuntime(manifest, "Jason-Ding19/Tartarus", TartarusTerrainRuntime.SOURCE_COMMIT);
    }

    TerraPackPaletteRuntime(JsonObject manifest, String project, String commit) {
        if (manifest.get("format")
            .getAsInt() != 1
            || !manifest.getAsJsonObject("source")
                .get("project")
                .getAsString()
                .equals(project)
            || !manifest.getAsJsonObject("source")
                .get("commit")
                .getAsString()
                .equals(commit))
            throw new IllegalArgumentException("Unsupported Terra palette source/version");
        compiler = new OrigenTerrainRuntime.Compiler(
            manifest.getAsJsonObject("functions"),
            manifest.getAsJsonObject("samplers"),
            false);
        String slantMethod = manifest.getAsJsonObject("generator_settings")
            .get("slant_method")
            .getAsString();
        if (!slantMethod.equals("Derivative") && !slantMethod.equals("DotProduct"))
            throw new IllegalArgumentException("Unsupported Terra slant calculation: " + slantMethod);
        derivativeSlant = slantMethod.equals("Derivative");
        for (var entry : manifest.getAsJsonObject("palettes")
            .entrySet()) {
            JsonObject raw = entry.getValue()
                .getAsJsonObject();
            NoiseSampler defaultSampler = compiler.compileSampler(raw.getAsJsonObject("sampler"));
            List<Layer> layers = new ArrayList<>();
            for (JsonElement value : raw.getAsJsonArray("layers")) {
                JsonObject layer = value.getAsJsonObject();
                NoiseSampler sampler = layer.has("sampler") ? compiler.compileSampler(layer.getAsJsonObject("sampler"))
                    : defaultSampler;
                List<String> states = new ArrayList<>();
                for (JsonElement material : layer.getAsJsonArray("materials")) {
                    JsonObject weighted = material.getAsJsonObject();
                    int count = (int) compiler.numeric(weighted.get("weight"));
                    if (count < 1 || count > 65536 || states.size() + count > 65536)
                        throw new IllegalArgumentException("Unbounded Terra palette probability array");
                    String state = weighted.get("material")
                        .getAsString();
                    for (int i = 0; i < count; i++) states.add(state);
                }
                if (states.isEmpty()) throw new IllegalArgumentException("Empty Terra palette materials");
                int count = (int) compiler.numeric(layer.get("layers"));
                if (count < 1 || count > 4096 || layers.size() + count > 4096)
                    throw new IllegalArgumentException("Unbounded Terra palette depth");
                Layer compiled = new Layer(states.toArray(new String[0]), sampler);
                for (int i = 0; i < count; i++) layers.add(compiled);
            }
            if (layers.isEmpty()) throw new IllegalArgumentException("Empty Terra palette");
            palettes.put(entry.getKey(), new Palette(layers.toArray(new Layer[0])));
        }
        for (var entry : manifest.getAsJsonObject("surfaces")
            .entrySet()) {
            JsonObject raw = entry.getValue()
                .getAsJsonObject();
            List<Slant> slants = new ArrayList<>();
            for (JsonElement value : raw.getAsJsonArray("slant")) {
                JsonObject slant = value.getAsJsonObject();
                slants
                    .add(new Slant(compiler.numeric(slant.get("threshold")), holder(slant.getAsJsonArray("palette"))));
            }
            slants.sort(Comparator.comparingDouble(Slant::threshold));
            String ocean = raw.get("ocean_palette")
                .isJsonNull() ? null
                    : raw.get("ocean_palette")
                        .getAsString();
            Integer sea = raw.get("ocean_level")
                .isJsonNull() ? null : (int) compiler.numeric(raw.get("ocean_level"));
            surfaces.put(
                entry.getKey(),
                new Surface(
                    holder(raw.getAsJsonArray("palette")),
                    List.copyOf(slants),
                    (int) compiler.numeric(raw.get("slant_depth")),
                    ocean == null ? null : palette(ocean),
                    sea,
                    raw.get("update_palette_when_carving")
                        .getAsBoolean()));
        }
        indexedSurfaces = surfaces.values()
            .toArray(new Surface[0]);
        int surfaceId = 0;
        for (String profile : surfaces.keySet()) surfaceIds.put(profile, surfaceId++);
    }

    int surfaceId(String profile) {
        Integer id = surfaceIds.get(profile);
        if (id == null) throw new IllegalArgumentException("Unknown or quarantined Terra surface profile: " + profile);
        return id;
    }

    boolean updatePaletteWhenCarving(int id) {
        return indexedSurfaces[id].resetWhenCarving;
    }

    Integer oceanLevel(int id) {
        return indexedSurfaces[id].sea;
    }

    boolean needsSlant(int id, int depth) {
        Surface s = indexedSurfaces[id];
        return !s.slants.isEmpty() && depth <= s.slantDepth;
    }

    String oceanState(int id, int depth, long seed, int x, int y, int z) {
        return oceanState(indexedSurfaces[id], depth, seed, x, y, z);
    }

    String state(int id, int depth, long seed, int x, int y, int z, double slant) {
        return state(indexedSurfaces[id], depth, seed, x, y, z, slant);
    }

    private String oceanState(Surface surface, int depth, long seed, int x, int y, int z) {
        if (surface.ocean == null || surface.sea == null) return "minecraft:air";
        return surface.ocean.state(depth, seed, x, y, z);
    }

    private String state(Surface surface, int depth, long seed, int x, int y, int z, double slant) {
        Holder chosen = surface.base;
        if (depth <= surface.slantDepth && !Double.isNaN(slant) && !surface.slants.isEmpty()) {
            if (derivativeSlant && slant > surface.slants.get(0).threshold) {
                for (Slant layer : surface.slants) {
                    if (layer.threshold > slant) break;
                    chosen = layer.holder;
                }
            } else if (!derivativeSlant && slant < surface.slants.get(surface.slants.size() - 1).threshold) {
                for (Slant layer : surface.slants) if (layer.threshold >= slant) {
                    chosen = layer.holder;
                    break;
                }
            }
        }
        return chosen.palette(y)
            .state(depth, seed, x, y, z);
    }

    private Palette palette(String id) {
        if (id.startsWith("BLOCK:"))
            return new Palette(new Layer[] { new Layer(new String[] { id.substring(6) }, NoiseSampler.zero()) });
        Palette value = palettes.get(id);
        if (value == null) throw new IllegalArgumentException("Unknown or quarantined Terra palette: " + id);
        return value;
    }

    private Holder holder(JsonArray raw) {
        List<Cutoff> cutoffs = new ArrayList<>();
        for (JsonElement value : raw) {
            JsonObject entry = value.getAsJsonObject();
            cutoffs.add(
                new Cutoff(
                    (int) compiler.numeric(entry.get("max_y")),
                    palette(
                        entry.get("palette")
                            .getAsString())));
        }
        if (cutoffs.isEmpty()) throw new IllegalArgumentException("Empty Terra palette holder");
        cutoffs.sort(Comparator.comparingInt(Cutoff::maxY));
        return new Holder(List.copyOf(cutoffs));
    }

    private record Surface(Holder base, List<Slant> slants, int slantDepth, Palette ocean, Integer sea,
        boolean resetWhenCarving) {}

    private record Slant(double threshold, Holder holder) {}

    private record Cutoff(int maxY, Palette palette) {}

    private record Holder(List<Cutoff> cutoffs) {

        Palette palette(int y) {
            for (int index = 0; index < cutoffs.size(); index++) {
                Cutoff entry = cutoffs.get(index);
                if (entry.maxY >= y) return entry.palette;
            }
            return cutoffs.get(cutoffs.size() - 1).palette;
        }
    }

    private record Palette(Layer[] layers) {

        String state(int depth, long seed, int x, int y, int z) {
            Layer layer = layers[Math.min(Math.max(0, depth), layers.length - 1)];
            if (layer.states.length == 1) return layer.states[0];
            double sample = layer.sampler.noise(seed, x, y, z);
            if (!Double.isFinite(sample)) throw new IllegalStateException("Non-finite Terra palette sample");
            int index = Math
                .max(0, Math.min(layer.states.length - 1, (int) Math.floor((sample + 1) / 2 * layer.states.length)));
            return layer.states[index];
        }
    }

    private record Layer(String[] states, NoiseSampler sampler) {}
}
