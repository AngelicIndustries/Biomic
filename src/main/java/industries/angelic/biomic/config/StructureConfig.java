package industries.angelic.biomic.config;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.biomic.Biomic;

/**
 * Structure frequencies and sizes, from {@code config/biomic-structures.json}, written with vanilla's values when
 * missing. Read at startup; changes apply to chunks generated afterwards.
 */
public final class StructureConfig {

    public static final String FILE = "biomic-structures.json";

    /**
     * One village cell per {@code spacingChunks}² chunks, its village at least {@code separationChunks} from the next;
     * with {@code skyVillages}, a cell's floating islands get a village of their own too.
     */
    public record Villages(boolean enabled, int spacingChunks, int separationChunks, double chance,
        boolean smallVillages, boolean skyVillages, boolean villageNames) {}

    public record Mineshafts(boolean enabled) {}

    /**
     * Vanilla's ring placement: {@code count} strongholds about {@code distance} chunks out, {@code spread} per ring.
     */
    public record Strongholds(boolean enabled, int count, double distance, int spread) {}

    /** One fortress region per {@code spacingChunks}² chunks, of which {@code chance} get a fortress. */
    public record Fortresses(boolean enabled, int spacingChunks, double chance) {}

    private static volatile StructureConfig current = new StructureConfig(new JsonObject());

    public final Villages villages;
    public final Mineshafts mineshafts;
    public final Strongholds strongholds;
    public final Fortresses fortresses;

    private StructureConfig(JsonObject root) {
        JsonObject v = section(root, "villages"), m = section(root, "mineshafts"), s = section(root, "strongholds"),
            f = section(root, "nether_fortresses");
        int spacing = integer(v, "villages", "spacing_chunks", 32, 2, 4096);
        villages = new Villages(
            bool(v, "enabled"),
            spacing,
            integer(v, "villages", "separation_chunks", 8, 0, spacing - 1),
            chance(v, "villages", "chance", 1),
            bool(v, "small_villages"),
            bool(v, "sky_villages"),
            bool(v, "village_names"));
        mineshafts = new Mineshafts(bool(m, "enabled"));
        strongholds = new Strongholds(
            bool(s, "enabled"),
            integer(s, "strongholds", "count", 3, 1, 1024),
            number(s, "strongholds", "distance", 32, 1, 100_000),
            integer(s, "strongholds", "spread", 3, 1, 1024));
        fortresses = new Fortresses(
            bool(f, "enabled"),
            integer(f, "nether_fortresses", "spacing_chunks", 16, 2, 4096),
            chance(f, "nether_fortresses", "chance", 1D / 3));
    }

    public static StructureConfig get() {
        return current;
    }

    /** Reads the file, writing the defaults first when it does not exist. */
    public static void load(File configDirectory) {
        File file = new File(configDirectory, FILE);
        try {
            if (!file.exists()) {
                try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                    writer.write(
                        new GsonBuilder().setPrettyPrinting()
                            .create()
                            .toJson(defaults()));
                }
            }
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                current = new StructureConfig(
                    new JsonParser().parse(reader)
                        .getAsJsonObject());
            }
        } catch (IOException | RuntimeException error) {
            throw new IllegalStateException("Cannot read " + file + ": " + error.getMessage(), error);
        }
        Biomic.LOG.info(
            "Structures: {} {} {} {}",
            current.villages,
            current.mineshafts,
            current.strongholds,
            current.fortresses);
    }

    private static JsonObject defaults() {
        JsonObject root = new JsonObject();
        root.addProperty(
            "_about",
            "Structure frequencies for Biomic worlds. Changes apply to newly generated chunks. "
                + "Spacing is in chunks; chances are 0..1.");
        JsonObject villages = new JsonObject();
        villages.addProperty("enabled", true);
        villages.addProperty("spacing_chunks", 32);
        villages.addProperty("separation_chunks", 8);
        villages.addProperty("chance", 1.0);
        villages.addProperty("small_villages", true);
        villages.addProperty("sky_villages", true);
        villages.addProperty("village_names", true);
        root.add("villages", villages);
        JsonObject mineshafts = new JsonObject();
        mineshafts.addProperty("enabled", true);
        root.add("mineshafts", mineshafts);
        JsonObject strongholds = new JsonObject();
        strongholds.addProperty("enabled", true);
        strongholds.addProperty("count", 3);
        strongholds.addProperty("distance", 32.0);
        strongholds.addProperty("spread", 3);
        root.add("strongholds", strongholds);
        JsonObject fortresses = new JsonObject();
        fortresses.addProperty("enabled", true);
        fortresses.addProperty("spacing_chunks", 16);
        fortresses.addProperty("chance", 0.333);
        root.add("nether_fortresses", fortresses);
        return root;
    }

    private static JsonObject section(JsonObject root, String name) {
        JsonElement value = root.get(name);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }

    private static boolean bool(JsonObject section, String key) {
        return section.has(key) ? section.get(key)
            .getAsBoolean() : true;
    }

    private static double number(JsonObject section, String name, String key, double fallback, double min, double max) {
        double value = section.has(key) ? section.get(key)
            .getAsDouble() : fallback;
        if (!(value >= min && value <= max)) throw new IllegalArgumentException(
            name + "." + key + " must be within " + min + ".." + max + ", got " + value);
        return value;
    }

    private static int integer(JsonObject section, String name, String key, int fallback, int min, int max) {
        return (int) number(section, name, key, fallback, min, max);
    }

    private static double chance(JsonObject section, String name, String key, double fallback) {
        return number(section, name, key, fallback, 0, 1);
    }
}
