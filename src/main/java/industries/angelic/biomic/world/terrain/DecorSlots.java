package industries.angelic.biomic.world.terrain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import industries.angelic.biomic.api.EcologyOverlays;

/**
 * The blocks of Biomic's own cave and sea decoration that another mod may swap (assets/biomic/ecology/decor.json and
 * its overlays). Biomic's are vanilla's, Et Futurum's and its own, or nothing: an empty slot places nothing where it
 * would grow. The decoration's rolls stay in code, so a slot changes only what grows, never where.
 */
final class DecorSlots {

    private static final String FILE = "decor.json";
    private static volatile DecorSlots instance;

    final Map<String, String> cave;
    final Map<String, String> marine;
    final Map<String, List<String>> marineLists;

    private DecorSlots(Map<String, String> cave, Map<String, String> marine, Map<String, List<String>> marineLists) {
        this.cave = cave;
        this.marine = marine;
        this.marineLists = marineLists;
    }

    /** A cave decor slot's block (mod:name:meta), or "" for nothing. */
    static String cave(String slot) {
        return instance().cave.getOrDefault(slot, "");
    }

    /** A sea decor slot's plant, or "" for nothing. */
    static String marine(String slot) {
        return instance().marine.getOrDefault(slot, "");
    }

    /** A sea decor list (fresh water plants by climate, reef corals), in order; empty for none. */
    static List<String> marineList(String slot) {
        return instance().marineLists.getOrDefault(slot, Collections.emptyList());
    }

    private static DecorSlots instance() {
        DecorSlots loaded = instance;
        if (loaded == null) {
            synchronized (DecorSlots.class) {
                if (instance == null) instance = parse(EcologyOverlays.load(FILE));
                loaded = instance;
            }
        }
        return loaded;
    }

    static DecorSlots parse(JsonObject root) {
        if (root.get("format")
            .getAsInt() != 1) throw new IllegalStateException("Unknown decor format");
        Map<String, String> cave = new HashMap<>(), marine = new HashMap<>();
        Map<String, List<String>> lists = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("cave")
            .entrySet())
            cave.put(
                entry.getKey(),
                entry.getValue()
                    .getAsString());
        JsonObject sea = root.getAsJsonObject("marine");
        for (Map.Entry<String, JsonElement> entry : sea.getAsJsonObject("plants")
            .entrySet())
            marine.put(
                entry.getKey(),
                entry.getValue()
                    .getAsString());
        for (Map.Entry<String, JsonElement> entry : sea.getAsJsonObject("lists")
            .entrySet()) {
            List<String> values = new ArrayList<>();
            for (JsonElement value : entry.getValue()
                .getAsJsonArray()) values.add(value.getAsString());
            lists.put(entry.getKey(), List.copyOf(values));
        }
        return new DecorSlots(Map.copyOf(cave), Map.copyOf(marine), Map.copyOf(lists));
    }
}
