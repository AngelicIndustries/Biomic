package industries.angelic.biomic.biome.ru;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Parsed build product of the pinned modern Regions Unexplored registries. */
public final class RuRuntimeManifest {

    public static final String RESOURCE = "/assets/biomic/ru_reference/runtime-biomes.json";
    public static final String SOURCE_REVISION = "f5dfe4eea916cb7ccb4990faebc952459dad5f8b";
    public static final String CLOSURE_SHA256 = "14002030484a93fe6481cc93449bb0500279bd384e5b1ac8728aa2f94b52063e";

    private static final List<BiomeEntry> BIOMES;

    static {
        JsonObject root = readRoot();
        require("biomic-ru-runtime-v1".equals(string(root, "format")), "unexpected runtime format");
        require(SOURCE_REVISION.equals(string(root, "source_revision")), "source revision drift");
        require(CLOSURE_SHA256.equals(string(root, "registry_closure_sha256")), "registry closure drift");
        JsonObject counts = root.getAsJsonObject("counts");
        require(integer(counts, "surface") == 55, "surface catalogue must contain 55 biomes");
        require(
            integer(counts, "cave") == 4,
            "cave catalogue must contain four biomes (Redstone Caves are not generated)");
        require(integer(counts, "nether") == 4, "Nether catalogue must contain four biomes");

        List<BiomeEntry> parsed = new ArrayList<>();
        Map<String, BiomeEntry> keys = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("biomes")) {
            BiomeEntry entry = new BiomeEntry(element.getAsJsonObject());
            require(keys.put(entry.getKey(), entry) == null, "duplicate active biome " + entry.getKey());
            parsed.add(entry);
        }
        require(parsed.size() == 63, "active catalogue must contain 63 biomes");
        BIOMES = Collections.unmodifiableList(parsed);
    }

    private RuRuntimeManifest() {}

    public static List<BiomeEntry> biomes() {
        return BIOMES;
    }

    private static JsonObject readRoot() {
        InputStream stream = RuRuntimeManifest.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException("Missing generated RU runtime manifest " + RESOURCE);
        }
        try (InputStream input = stream;
            InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader)
                .getAsJsonObject();
        } catch (Exception failure) {
            throw new IllegalStateException("Could not load generated RU runtime manifest " + RESOURCE, failure);
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? null : value.getAsString();
    }

    private static int integer(JsonObject object, String key) {
        return object.get(key)
            .getAsInt();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Invalid RU runtime manifest: " + message);
    }

    public static final class BiomeEntry {

        private final String name;
        private final String key;
        private final String displayName;
        private final String category;
        private final int expansionOffset;
        private final String terrainProfile;
        private final String surfaceStyle;
        private final float temperature;
        private final float downfall;
        private final int mapColor;
        private final int grassColor;
        private final int foliageColor;
        private final int waterColor;
        private final int skyColor;
        private final List<String> tags;
        private final boolean naturalSpawn;
        private final boolean villageLowland;
        private final List<DecorationEntry> decorations;

        private BiomeEntry(JsonObject object) {
            name = string(object, "name");
            key = string(object, "key");
            displayName = string(object, "display_name");
            category = string(object, "category");
            expansionOffset = integer(object, "expansion_offset");
            terrainProfile = string(object, "terrain_profile");
            surfaceStyle = string(object, "surface_style");
            temperature = object.get("temperature")
                .getAsFloat();
            downfall = object.get("downfall")
                .getAsFloat();
            mapColor = integer(object, "map_color");
            grassColor = integer(object, "grass_color");
            foliageColor = integer(object, "foliage_color");
            waterColor = integer(object, "water_color");
            skyColor = integer(object, "sky_color");
            List<String> parsedTags = new ArrayList<>();
            for (JsonElement tag : object.getAsJsonArray("tags")) parsedTags.add(tag.getAsString());
            tags = Collections.unmodifiableList(parsedTags);
            naturalSpawn = object.get("natural_spawn")
                .getAsBoolean();
            villageLowland = object.get("village_lowland")
                .getAsBoolean();
            List<DecorationEntry> parsedDecorations = new ArrayList<>();
            for (JsonElement decoration : object.getAsJsonArray("decorations")) {
                parsedDecorations.add(new DecorationEntry(decoration.getAsJsonObject()));
            }
            decorations = Collections.unmodifiableList(parsedDecorations);
        }

        public String getName() {
            return name;
        }

        public String getKey() {
            return key;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCategory() {
            return category;
        }

        public int getExpansionOffset() {
            return expansionOffset;
        }

        public String getTerrainProfile() {
            return terrainProfile;
        }

        public String getSurfaceStyle() {
            return surfaceStyle;
        }

        public float getTemperature() {
            return temperature;
        }

        public float getDownfall() {
            return downfall;
        }

        public int getMapColor() {
            return mapColor;
        }

        public int getGrassColor() {
            return grassColor;
        }

        public int getFoliageColor() {
            return foliageColor;
        }

        public int getWaterColor() {
            return waterColor;
        }

        public int getSkyColor() {
            return skyColor;
        }

        public List<String> getTags() {
            return tags;
        }

        public boolean isNaturalSpawn() {
            return naturalSpawn;
        }

        public List<DecorationEntry> getDecorations() {
            return decorations;
        }

        public boolean isVillageLowland() {
            return villageLowland;
        }

    }

    public static final class DecorationEntry {

        private final int attempts;
        private final List<TerminalEntry> terminals;

        public int getAttempts() {
            return attempts;
        }

        public List<TerminalEntry> getTerminals() {
            return terminals;
        }

        private DecorationEntry(JsonObject object) {
            attempts = object.has("attempts") ? integer(object, "attempts") : 1;
            List<TerminalEntry> parsed = new ArrayList<>();
            JsonArray array = object.getAsJsonArray("terminals");
            if (array != null) {
                for (JsonElement terminal : array) parsed.add(new TerminalEntry(terminal.getAsJsonObject()));
            }
            terminals = Collections.unmodifiableList(parsed);
        }

    }

    public static final class TerminalEntry {

        private final List<String> states;

        public List<String> getStates() {
            return states;
        }

        private TerminalEntry(JsonObject object) {
            List<String> parsedStates = new ArrayList<>();
            JsonArray stateArray = object.getAsJsonArray("states");
            if (stateArray != null) {
                for (JsonElement state : stateArray) parsedStates.add(state.getAsString());
            }
            states = Collections.unmodifiableList(parsedStates);
        }

    }
}
