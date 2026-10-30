package industries.angelic.biomic.world.terrain;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Origen's imported manifest with Biomic's changes applied (tools/origen_overrides.py writes them): replaced palettes,
 * new profiles (appended to OrigenBiomeProfile), replaced surfaces of a few profiles, feature structure lists, rock
 * deposits that must not show on faces open to the sky, features added to existing profiles, and cellular steps blended
 * into slopes. The imported data stays as upstream.
 */
final class OrigenManifest {

    static final String OVERRIDES = "/assets/biomic/origen/palette-overrides.json";

    private OrigenManifest() {}

    static JsonObject read() {
        JsonObject manifest = TerraPackTerrainRuntime.readManifest(OrigenTerrainRuntime.RESOURCE);
        override(manifest, TerraPackTerrainRuntime.readManifest(OVERRIDES));
        return manifest;
    }

    static void override(JsonObject manifest, JsonObject overrides) {
        JsonObject palettes = manifest.getAsJsonObject("palettes");
        for (var entry : overrides.getAsJsonObject("palettes")
            .entrySet()) palettes.add(entry.getKey(), entry.getValue());
        JsonObject profiles = manifest.getAsJsonObject("profiles");
        for (var entry : overrides.getAsJsonObject("profiles")
            .entrySet()) {
            if (profiles.has(entry.getKey()))
                throw new IllegalArgumentException("Origen already has " + entry.getKey());
            profiles.add(entry.getKey(), entry.getValue());
        }
        for (var entry : overrides.getAsJsonObject("surfaces")
            .entrySet()) profile(profiles, entry.getKey()).add("surface", entry.getValue());
        JsonObject features = manifest.getAsJsonObject("features");
        for (var entry : overrides.getAsJsonObject("feature_structures")
            .entrySet()) {
            if (!features.has(entry.getKey()))
                throw new IllegalArgumentException("No Origen feature " + entry.getKey());
            features.getAsJsonObject(entry.getKey())
                .add("structures", entry.getValue());
        }
        if (overrides.has("profile_features")) {
            for (var entry : overrides.getAsJsonObject("profile_features")
                .entrySet()) {
                JsonObject lists = profile(profiles, entry.getKey()).getAsJsonObject("features");
                for (var added : entry.getValue()
                    .getAsJsonObject()
                    .entrySet()) {
                    var list = lists.getAsJsonArray(added.getKey());
                    for (JsonElement feature : added.getValue()
                        .getAsJsonArray()) {
                        if (!features.has(feature.getAsString()))
                            throw new IllegalArgumentException("No Origen feature " + feature.getAsString());
                        boolean present = false;
                        for (JsonElement listed : list) present |= listed.equals(feature);
                        if (!present) list.add(feature);
                    }
                }
            }
        }
        if (overrides.has("cellular_edges"))
            blendCellularEdges(manifest.getAsJsonObject("graphs"), overrides.getAsJsonObject("cellular_edges"));
        JsonObject ores = manifest.getAsJsonObject("ores");
        for (JsonElement ore : overrides.getAsJsonArray("ores_hidden_from_sky")) {
            if (!ores.has(ore.getAsString())) throw new IllegalArgumentException("No Origen ore " + ore.getAsString());
            ores.getAsJsonObject(ore.getAsString())
                .addProperty("hidden_from_sky", true);
        }
    }

    /** Sets a cellular sampler's edge blend (CellularSampler#setEdgeBlend) in every graph of the named base. */
    private static void blendCellularEdges(JsonObject graphs, JsonObject edges) {
        for (var base : edges.entrySet()) {
            boolean found = false;
            for (var graph : graphs.entrySet()) {
                if (!graph.getKey()
                    .startsWith("base:" + base.getKey() + ":")) continue;
                JsonObject samplers = graph.getValue()
                    .getAsJsonObject()
                    .getAsJsonObject("samplers");
                for (var sampler : base.getValue()
                    .getAsJsonObject()
                    .entrySet()) {
                    JsonObject node = samplers.getAsJsonObject(sampler.getKey());
                    if (node == null)
                        throw new IllegalArgumentException("No sampler " + sampler.getKey() + " in " + graph.getKey());
                    // Down through wrappers (a domain warp, say) to the cellular sampler itself.
                    while (!"CELLULAR".equals(
                        node.get("type")
                            .getAsString()))
                        node = node.getAsJsonObject("sampler");
                    node.add("edge-blend", sampler.getValue());
                }
                found = true;
            }
            if (!found) throw new IllegalArgumentException("No Origen base " + base.getKey());
        }
    }

    private static JsonObject profile(JsonObject profiles, String name) {
        if (!profiles.has(name)) throw new IllegalArgumentException("No Origen profile " + name);
        return profiles.getAsJsonObject(name);
    }
}
