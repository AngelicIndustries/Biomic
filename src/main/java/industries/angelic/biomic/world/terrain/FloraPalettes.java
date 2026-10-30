package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;

/**
 * The decided natural-flora palettes (assets/biomic/ecology/palettes.json, exported from the reviewed ecology
 * proposal, and its overlays, EcologyOverlays): per habitat, its budget class and its added plants by tier. Signature
 * plants grow in that habitat only.
 */
public final class FloraPalettes {

    private static final String FILE = "palettes.json";
    private static volatile FloraPalettes instance;

    /** Where a class's plants may stand, beyond the plant's own soil rules. */
    enum Rule {
        NONE,
        NEAR_WATER,
        SPACED,
        NEAR_STONE,
        ABOVE_HIGH_WATER
    }

    enum Tier {
        SIGNATURE,
        COMMON,
        UNCOMMON,
        RARE
    }

    /**
     * Plants per chunk counting the natives, the cap on added plants, patch diameter and fill, and the siting rule.
     */
    record Budget(int plantsMin, int plantsMax, int added, int patchMin, int patchMax, double fillMin, double fillMax,
        Rule rule) {}

    record Plant(String id, Tier tier) {}

    record Habitat(String key, Budget budget, List<Plant> plants) {

        boolean adds() {
            return budget.added() > 0 && !plants.isEmpty();
        }
    }

    /**
     * The plant layer in a habitat (api.PlantLayer): names of its crop patches, bushes, grove trees, trunk vines and
     * shoots, and whether nests hang on its trees. What a name grows as is its resolver's.
     */
    record Food(List<String> crops, List<String> bushes, List<String> trees, List<String> vines, boolean nests,
        List<String> shoots) {

        static final Food NONE = new Food(List.of(), List.of(), List.of(), List.of(), false, List.of());
    }

    /**
     * PMP plants that do not stand on the ground: epiphytes on trunks, vines on trunks, leaves and cliffs, immersed
     * plants in shallow water and floating plants on still water.
     */
    record Forms(List<Plant> epiphytes, List<Plant> vines, List<Plant> immersed, List<Plant> floating) {

        static final Forms NONE = new Forms(List.of(), List.of(), List.of(), List.of());

        boolean any() {
            return !epiphytes.isEmpty() || !vines.isEmpty() || !immersed.isEmpty() || !floating.isEmpty();
        }
    }

    private final Map<String, Habitat> habitats;
    private final Map<OrigenBiomeProfile, Habitat> origen;
    private final Map<OrigenBiomeProfile, List<String>> patches;
    private final Map<String, Food> food;
    private final Map<String, Forms> forms;
    private final Map<String, List<String>> nether;
    private final List<String> sky;

    private FloraPalettes(Map<String, Habitat> habitats, Map<OrigenBiomeProfile, Habitat> origen,
        Map<OrigenBiomeProfile, List<String>> patches, Map<String, Food> food, Map<String, Forms> forms,
        Map<String, List<String>> nether, List<String> sky) {
        this.habitats = habitats;
        this.origen = origen;
        this.patches = patches;
        this.food = food;
        this.forms = forms;
        this.nether = nether;
        this.sky = sky;
    }

    /** HarvestCraft's place in a habitat (by key), never null. */
    Food food(String habitat) {
        return food.getOrDefault(habitat, Food.NONE);
    }

    /** Plants that grow on floating islands only (unearthly colours, an air plant). */
    List<String> sky() {
        return sky;
    }

    /** The habitat's epiphytes, vines and water plants (by key), never null. */
    Forms forms(String habitat) {
        return forms.getOrDefault(habitat, Forms.NONE);
    }

    /** Harvest the Nether in Tartarus: biome name to its plants (registry ids). */
    public static Map<String, List<String>> netherFood() {
        return instance().nether();
    }

    Map<String, List<String>> nether() {
        return nether;
    }

    /** The flowers that replace Origen's generic vanilla flower patches in this profile, or null to keep them. */
    List<String> patches(OrigenBiomeProfile profile) {
        return profile == null ? null : patches.get(profile);
    }

    static FloraPalettes instance() {
        FloraPalettes loaded = instance;
        if (loaded == null) {
            synchronized (FloraPalettes.class) {
                if (instance == null) instance = load();
                loaded = instance;
            }
        }
        return loaded;
    }

    /** The palette of a hosted habitat (its modern key), or null when the proposal gives it none. */
    Habitat habitat(String key) {
        return key == null ? null : habitats.get(key);
    }

    /** The palette of an Origen-owned surface, or null. */
    Habitat origen(OrigenBiomeProfile profile) {
        return profile == null ? null : origen.get(profile);
    }

    private static List<Plant> tiered(JsonObject object, String key) {
        if (!object.has(key)) return List.of();
        List<Plant> plants = new ArrayList<>();
        for (JsonElement value : object.getAsJsonArray(key)) {
            JsonObject plant = value.getAsJsonObject();
            plants.add(
                new Plant(
                    plant.get("id")
                        .getAsString(),
                    Tier.valueOf(
                        plant.get("tier")
                            .getAsString()
                            .toUpperCase(java.util.Locale.ROOT))));
        }
        return List.copyOf(plants);
    }

    private static List<String> strings(JsonObject object, String key) {
        if (!object.has(key)) return List.of();
        List<String> values = new ArrayList<>();
        for (JsonElement value : object.getAsJsonArray(key)) values.add(value.getAsString());
        return List.copyOf(values);
    }

    private static FloraPalettes load() {
        return parse(industries.angelic.biomic.api.EcologyOverlays.load(FILE));
    }

    static FloraPalettes parse(JsonObject root) {
        if (root.get("format")
            .getAsInt() != 1) throw new IllegalStateException("Unknown flora palette format");
        Map<String, Budget> budgets = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("classes")
            .entrySet()) {
            JsonObject c = entry.getValue()
                .getAsJsonObject();
            JsonArray plants = c.getAsJsonArray("plants"), patch = c.getAsJsonArray("patch"),
                fill = c.getAsJsonArray("fill");
            budgets.put(
                entry.getKey(),
                new Budget(
                    plants.get(0)
                        .getAsInt(),
                    plants.get(1)
                        .getAsInt(),
                    c.get("added")
                        .getAsInt(),
                    patch.get(0)
                        .getAsInt(),
                    patch.get(1)
                        .getAsInt(),
                    fill.get(0)
                        .getAsDouble(),
                    fill.get(1)
                        .getAsDouble(),
                    Rule.valueOf(
                        c.get("rule")
                            .getAsString()
                            .toUpperCase(java.util.Locale.ROOT))));
        }
        Map<String, Habitat> habitats = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("habitats")
            .entrySet()) {
            JsonObject h = entry.getValue()
                .getAsJsonObject();
            Budget budget = budgets.get(
                h.get("class")
                    .getAsString());
            if (budget == null) throw new IllegalStateException("Unknown flora class for " + entry.getKey());
            List<Plant> plants = new ArrayList<>();
            for (Tier tier : Tier.values()) {
                for (JsonElement id : h.getAsJsonArray(
                    tier.name()
                        .toLowerCase(java.util.Locale.ROOT)))
                    plants.add(new Plant(id.getAsString(), tier));
            }
            habitats.put(entry.getKey(), new Habitat(entry.getKey(), budget, Collections.unmodifiableList(plants)));
        }
        Map<OrigenBiomeProfile, Habitat> origen = new EnumMap<>(OrigenBiomeProfile.class);
        Map<OrigenBiomeProfile, List<String>> patches = new EnumMap<>(OrigenBiomeProfile.class);
        JsonObject patchLists = root.has("patches") ? root.getAsJsonObject("patches") : new JsonObject();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("origen")
            .entrySet()) {
            Habitat habitat = habitats.get(
                entry.getValue()
                    .getAsString());
            if (habitat == null) throw new IllegalStateException("Origen flora alias to a missing habitat: " + entry);
            OrigenBiomeProfile profile = OrigenBiomeProfile.valueOf(entry.getKey());
            origen.put(profile, habitat);
            if (patchLists.has(habitat.key())) {
                List<String> flowers = new ArrayList<>();
                for (JsonElement id : patchLists.getAsJsonArray(habitat.key())) flowers.add(id.getAsString());
                if (!flowers.isEmpty()) patches.put(profile, Collections.unmodifiableList(flowers));
            }
        }
        Map<String, Food> food = new HashMap<>();
        Map<String, List<String>> nether = new HashMap<>();
        if (root.has("food")) {
            JsonObject f = root.getAsJsonObject("food");
            if (f.has("habitats")) for (Map.Entry<String, JsonElement> entry : f.getAsJsonObject("habitats")
                .entrySet()) {
                    if (!habitats.containsKey(entry.getKey()))
                        throw new IllegalStateException("Food for a missing habitat: " + entry.getKey());
                    JsonObject h = entry.getValue()
                        .getAsJsonObject();
                    food.put(
                        entry.getKey(),
                        new Food(
                            strings(h, "crops"),
                            strings(h, "bushes"),
                            strings(h, "trees"),
                            strings(h, "vines"),
                            h.has("nests") && h.get("nests")
                                .getAsBoolean(),
                            strings(h, "shoots")));
                }
            if (f.has("nether")) for (Map.Entry<String, JsonElement> entry : f.getAsJsonObject("nether")
                .entrySet()) nether.put(entry.getKey(), strings(f.getAsJsonObject("nether"), entry.getKey()));
        }
        Map<String, Forms> forms = new HashMap<>();
        if (root.has("forms")) for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("forms")
            .entrySet()) {
                if (!habitats.containsKey(entry.getKey()))
                    throw new IllegalStateException("Plant forms for a missing habitat: " + entry.getKey());
                JsonObject h = entry.getValue()
                    .getAsJsonObject();
                forms.put(
                    entry.getKey(),
                    new Forms(
                        tiered(h, "epiphytes"),
                        tiered(h, "vines"),
                        tiered(h, "immersed"),
                        tiered(h, "floating")));
            }
        return new FloraPalettes(
            habitats,
            origen,
            patches,
            Map.copyOf(food),
            Map.copyOf(forms),
            Map.copyOf(nether),
            strings(root, "sky"));
    }
}
