package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Tartarus' imported manifest, with other mods' Nether biomes joined to its biome pool. A neighbour takes the
 * terrain (density, carving, blend, lava sea) of the Tartarus biome whose equation suits it, its own ground as a
 * palette, and Tartarus' ores plus the landforms it lists. Everything else about it (decoration, spawns, colours)
 * stays its mod's.
 */
public final class TartarusManifest {

    /**
     * A joining biome. {@code top} and {@code filler} are block states the Nether's state table resolves; the top
     * covers one block of every floor, in patches when it lists several states (repeat one to weight it), and the
     * filler the next three.
     */
    public record Neighbour(String id, String terrainOf, List<String> top, String filler, int weight,
        List<String> landforms) {}

    private static final List<String> ORES = List.of("NETHER_GOLD_ORE", "QUARTZ_ORE", "ANCIENT_DEBRIS");
    /** Biomic's changes to the pack's palettes and features, kept apart from the imported data. */
    public static final String OVERRIDES = "/assets/biomic/tartarus/palette-overrides.json";

    private TartarusManifest() {}

    public static JsonObject load(List<Neighbour> neighbours) {
        JsonObject manifest = TerraPackTerrainRuntime.readManifest(TartarusTerrainRuntime.RESOURCE);
        override(manifest, TerraPackTerrainRuntime.readManifest(OVERRIDES));
        for (Neighbour neighbour : neighbours) join(manifest, neighbour);
        return manifest;
    }

    /**
     * Biomic's skins: replaced palettes, features taken off their biomes' schedules, edits to structure scripts (each
     * replaced text must occur exactly once), structures removed with the block ids only they placed, and features
     * made rarer.
     */
    static void override(JsonObject manifest, JsonObject overrides) {
        JsonObject palettes = manifest.getAsJsonObject("palettes");
        for (var entry : overrides.getAsJsonObject("palettes")
            .entrySet()) palettes.add(entry.getKey(), entry.getValue());
        JsonObject decoration = manifest.getAsJsonObject("decoration");
        JsonObject structures = decoration.getAsJsonObject("structures");
        for (var entry : overrides.getAsJsonObject("structure_edits")
            .entrySet()) {
            JsonObject structure = structures.getAsJsonObject(entry.getKey());
            String script = structure.get("script")
                .getAsString();
            for (JsonElement edit : entry.getValue()
                .getAsJsonArray()) {
                String from = edit.getAsJsonArray()
                    .get(0)
                    .getAsString(),
                    to = edit.getAsJsonArray()
                        .get(1)
                        .getAsString();
                int at = script.indexOf(from);
                if (at < 0 || script.indexOf(from, at + 1) >= 0)
                    throw new IllegalArgumentException(entry.getKey() + " does not contain exactly one '" + from + "'");
                script = script.replace(from, to);
            }
            structure.addProperty("script", script);
        }
        for (var entry : overrides.getAsJsonObject("removed_structures")
            .entrySet()) {
            String id = entry.getKey();
            if (structures.remove(id) == null) throw new IllegalArgumentException("No structure " + id);
            for (var structure : structures.entrySet()) {
                JsonElement script = structure.getValue()
                    .getAsJsonObject()
                    .get("script"); // schematics have none
                if (script != null && script.getAsString()
                    .contains('"' + id + '"'))
                    throw new IllegalArgumentException(structure.getKey() + " still places " + id);
            }
            // Minecraft 1.7.10's Gson 2.2.4 has no JsonArray.remove.
            List<JsonElement> removed = new ArrayList<>();
            for (JsonElement block : entry.getValue()
                .getAsJsonArray()) removed.add(block);
            JsonArray kept = new JsonArray();
            for (JsonElement block : manifest.getAsJsonArray("authored_block_ids"))
                if (!removed.remove(block)) kept.add(block);
            if (!removed.isEmpty()) throw new IllegalArgumentException("No authored blocks " + removed);
            manifest.add("authored_block_ids", kept);
        }
        JsonObject schedules = decoration.getAsJsonObject("feature_schedules");
        for (var entry : overrides.getAsJsonObject("removed_features")
            .entrySet()) {
            for (JsonElement removed : entry.getValue()
                .getAsJsonArray()) {
                boolean found = false;
                for (var stage : schedules.getAsJsonObject(entry.getKey())
                    .entrySet()) {
                    JsonArray kept = new JsonArray();
                    for (JsonElement feature : stage.getValue()
                        .getAsJsonArray()) if (feature.equals(removed)) found = true;
                    else kept.add(feature);
                    stage.setValue(kept);
                }
                if (!found) throw new IllegalArgumentException("No feature " + removed + " in " + entry.getKey());
            }
        }
        JsonObject features = decoration.getAsJsonObject("features");
        for (var entry : overrides.getAsJsonObject("feature_thresholds")
            .entrySet())
            features.getAsJsonObject(entry.getKey())
                .getAsJsonObject("distributor")
                .add("threshold", entry.getValue());
    }

    private static void join(JsonObject manifest, Neighbour neighbour) {
        String id = neighbour.id(), ground = id + "_GROUND";
        JsonObject profiles = manifest.getAsJsonObject("profiles"), surfaces = manifest.getAsJsonObject("surfaces");
        if (profiles.has(id) || !profiles.has(neighbour.terrainOf()))
            throw new IllegalArgumentException("Cannot join " + id + " on " + neighbour.terrainOf());

        JsonObject palette = new JsonObject();
        JsonArray layers = new JsonArray();
        layers.add(layer(neighbour.top(), 1));
        layers.add(layer(List.of(neighbour.filler()), 3));
        layers.add(layer(List.of("minecraft:netherrack"), 1));
        palette.add("layers", layers);
        // Tartarus' own grounds mix their top blocks in cellular patches; borrow that sampler.
        palette.add(
            "sampler",
            copy(
                manifest.getAsJsonObject("palettes")
                    .getAsJsonObject("CRIMSON_GROUND")).get("sampler"));
        manifest.getAsJsonObject("palettes")
            .add(ground, palette);

        JsonObject profile = copy(profiles.getAsJsonObject(neighbour.terrainOf()));
        for (JsonElement entry : profile.getAsJsonArray("palette")) {
            JsonObject band = entry.getAsJsonObject();
            String name = band.entrySet()
                .iterator()
                .next()
                .getKey();
            if (name.endsWith("_GROUND")) {
                band.add(ground, band.remove(name));
            }
        }
        profiles.add(id, profile);

        JsonObject surface = copy(surfaces.getAsJsonObject(neighbour.terrainOf()));
        for (JsonElement entry : surface.getAsJsonArray("palette")) {
            JsonObject band = entry.getAsJsonObject();
            if (band.get("palette")
                .getAsString()
                .endsWith("_GROUND")) band.addProperty("palette", ground);
        }
        surface.add("slant", new JsonArray());
        surfaces.add(id, surface);

        JsonArray tags = new JsonArray();
        tags.add(parse("\"ALL\""));
        tags.add(parse("\"BIOME:" + id + "\""));
        manifest.getAsJsonObject("biome_tags")
            .add(id, tags);

        JsonObject entry = new JsonObject();
        entry.addProperty(id, neighbour.weight());
        pool(manifest).add(entry);

        JsonObject schedule = new JsonObject();
        schedule.add("landforms", strings(neighbour.landforms()));
        schedule.add("trees", new JsonArray());
        schedule.add("flora", new JsonArray());
        schedule.add("ores", strings(ORES));
        manifest.getAsJsonObject("decoration")
            .getAsJsonObject("feature_schedules")
            .add(id, schedule);
    }

    /** The pipeline stage that replaces the single source biome with the weighted pool. */
    private static JsonArray pool(JsonObject manifest) {
        JsonObject provider = manifest.getAsJsonObject("source_biome_provider");
        if (provider.has("provider")) provider = provider.getAsJsonObject("provider");
        for (JsonElement stage : provider.getAsJsonObject("pipeline")
            .getAsJsonArray("stages")) {
            JsonObject data = stage.getAsJsonObject();
            if ("REPLACE".equals(
                data.get("type")
                    .getAsString())
                && data.get("to")
                    .isJsonArray())
                return data.getAsJsonArray("to");
        }
        throw new IllegalStateException("Tartarus pipeline has no biome pool");
    }

    private static JsonObject layer(List<String> states, int depth) {
        JsonArray materials = new JsonArray();
        for (String state : states) {
            JsonObject material = new JsonObject();
            material.addProperty("material", state);
            material.addProperty("weight", 1);
            materials.add(material);
        }
        JsonObject layer = new JsonObject();
        layer.add("materials", materials);
        layer.addProperty("layers", depth);
        return layer;
    }

    private static JsonArray strings(List<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) array.add(parse("\"" + value + "\""));
        return array;
    }

    private static JsonObject copy(JsonObject object) {
        return parse(object.toString()).getAsJsonObject();
    }

    private static JsonElement parse(String json) {
        return new JsonParser().parse(json);
    }
}
