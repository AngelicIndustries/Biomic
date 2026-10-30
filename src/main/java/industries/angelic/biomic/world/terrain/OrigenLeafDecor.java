package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import industries.angelic.biomic.api.EcologyOverlays;
import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;

/**
 * What replaces Origen's leaf-block decor (reviewed ecology proposal, "leaf decor"): leaf ellipsoid bushes become
 * clumps of real shrubs, leaf blobs on cliff walls become vines on their free faces, and leaf ropes and ceiling
 * columns become hanging roots and vine curtains on the walls beside them. Origen's distributors and locators stay;
 * only what grows at each located spot changes. Features or profiles absent here keep Origen's own decor. In caves
 * the leaf blobs become moss skins, the leaf rope a moss root bridge and the leaf columns cave vines and roots.
 * Loaded from assets/biomic/ecology/leaf-decor.json and its overlays (EcologyOverlays).
 */
final class OrigenLeafDecor {

    private static final String FILE = "leaf-decor.json";
    private static volatile OrigenLeafDecor instance;

    private final Map<String, Spec> defaults;
    private final Map<String, Map<OrigenBiomeProfile, Spec>> byProfile;
    private final String defaultBamboo;

    private OrigenLeafDecor(Map<String, Spec> defaults, Map<String, Map<OrigenBiomeProfile, Spec>> byProfile,
        String defaultBamboo) {
        this.defaults = defaults;
        this.byProfile = byProfile;
        this.defaultBamboo = defaultBamboo;
    }

    sealed interface Spec permits Clump,Wall,Curtain,Nothing,MossSkin,Substitute,CaveCeiling,FrozenWall,Bamboo {
    }

    /** One centre shrub (the first plant, or one by weight when single) and up to {@code extra} more around it. */
    record Clump(List<String> plants, boolean single, int extra, int radius) implements Spec {}

    /**
     * Wall cover at each located cell: the base vine on every free face, an accent plant on a share of them.
     * {@code nearWater}: only where water lies within six blocks (dry canyon walls stay bare).
     */
    record Wall(String vine, List<String> accents, double accentShare, boolean nearWater) implements Spec {}

    /**
     * At a ceiling spot: hanging roots under the ceiling, then a curtain of the vines down the wall beside it, or a
     * free strand of {@code strand} (a plant that chains downwards) where there is no wall.
     */
    record Curtain(List<String> vines, String roots, String strand, int minLength, int maxLength) implements Spec {}

    record Nothing() implements Spec {}

    /** Cave leaf blobs: the rock they touch turns to moss, wall moss on part of the rest, roots under ceilings. */
    record MossSkin() implements Spec {}

    /** Origen's own structure, with every leaf block it writes swapped for {@code state} (a moss root bridge). */
    record Substitute(String state) implements Spec {}

    /** A cave ceiling spot: a strand of cave vines, hanging roots, or now and then a spore blossom. */
    record CaveCeiling() implements Spec {}

    /** Frozen canyon walls: icicles from overhangs, snow on ledges. */
    record FrozenWall() implements Spec {}

    /** Origen's bamboo stalks as they are drawn, in another species (a block id). */
    record Bamboo(String species) implements Spec {}

    /** The replacement for one leaf-decor feature in one profile, or null to keep Origen's own. */
    static Spec spec(String feature, OrigenBiomeProfile profile) {
        OrigenLeafDecor decor = instance();
        Map<OrigenBiomeProfile, Spec> overrides = decor.byProfile.get(feature);
        Spec spec = overrides == null ? null : overrides.get(profile);
        return spec != null ? spec : decor.defaults.get(feature);
    }

    /** The species Origen's bamboo is drawn in outside a Bamboo spec, or null to keep Origen's own. */
    static String defaultBamboo() {
        return instance().defaultBamboo;
    }

    private static OrigenLeafDecor instance() {
        OrigenLeafDecor loaded = instance;
        if (loaded == null) {
            synchronized (OrigenLeafDecor.class) {
                if (instance == null) instance = parse(EcologyOverlays.load(FILE));
                loaded = instance;
            }
        }
        return loaded;
    }

    static OrigenLeafDecor parse(JsonObject root) {
        if (root.get("format")
            .getAsInt() != 1) throw new IllegalStateException("Unknown leaf decor format");
        Map<String, Spec> defaults = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("features")
            .entrySet())
            defaults.put(
                entry.getKey(),
                spec(
                    entry.getValue()
                        .getAsJsonObject(),
                    entry.getKey()));
        Map<String, Map<OrigenBiomeProfile, Spec>> byProfile = new HashMap<>();
        for (Map.Entry<String, JsonElement> feature : root.getAsJsonObject("profiles")
            .entrySet()) {
            Map<OrigenBiomeProfile, Spec> specs = new EnumMap<>(OrigenBiomeProfile.class);
            for (Map.Entry<String, JsonElement> entry : feature.getValue()
                .getAsJsonObject()
                .entrySet())
                specs.put(
                    OrigenBiomeProfile.valueOf(entry.getKey()),
                    spec(
                        entry.getValue()
                            .getAsJsonObject(),
                        feature.getKey() + "/" + entry.getKey()));
            byProfile.put(feature.getKey(), Collections.unmodifiableMap(specs));
        }
        JsonElement bamboo = root.get("default_bamboo");
        return new OrigenLeafDecor(
            Map.copyOf(defaults),
            Map.copyOf(byProfile),
            bamboo == null || bamboo.isJsonNull() ? null : bamboo.getAsString());
    }

    private static Spec spec(JsonObject spec, String where) {
        return switch (spec.get("type")
            .getAsString()) {
            case "clump" -> new Clump(
                strings(spec, "plants"),
                spec.get("single")
                    .getAsBoolean(),
                spec.get("extra")
                    .getAsInt(),
                spec.get("radius")
                    .getAsInt());
            case "wall" -> new Wall(
                spec.get("vine")
                    .getAsString(),
                strings(spec, "accents"),
                spec.get("accent_share")
                    .getAsDouble(),
                spec.get("near_water")
                    .getAsBoolean());
            case "curtain" -> new Curtain(
                strings(spec, "vines"),
                optional(spec, "roots"),
                optional(spec, "strand"),
                spec.get("min_length")
                    .getAsInt(),
                spec.get("max_length")
                    .getAsInt());
            case "nothing" -> new Nothing();
            case "moss_skin" -> new MossSkin();
            case "substitute" -> new Substitute(
                spec.get("state")
                    .getAsString());
            case "cave_ceiling" -> new CaveCeiling();
            case "frozen_wall" -> new FrozenWall();
            case "bamboo" -> new Bamboo(
                spec.get("species")
                    .getAsString());
            default -> throw new IllegalStateException("Unknown leaf decor type in " + where + ": " + spec.get("type"));
        };
    }

    private static List<String> strings(JsonObject object, String key) {
        List<String> values = new ArrayList<>();
        if (object.has(key)) for (JsonElement value : object.getAsJsonArray(key)) values.add(value.getAsString());
        return List.copyOf(values);
    }

    private static String optional(JsonObject object, String key) {
        return object.has(key) && !object.get(key)
            .isJsonNull() ? object.get(key)
                .getAsString() : null;
    }
}
