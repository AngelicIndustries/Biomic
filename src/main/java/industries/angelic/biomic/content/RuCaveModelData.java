package industries.angelic.biomic.content;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/** Pinned source model selections and lossless legacy shape encoding (no client classes). */
public final class RuCaveModelData {

    public static final String RESOURCE = "/assets/biomic/ru_reference/cave-models.json";
    private static final JsonObject ROOT = load();
    private static final String[] HANGING = { "tip", "frustum", "column" };
    // Preserve zero as the source default for new placements and old metadata-zero blocks.
    private static final String[] HYACINTH = { "base_tip", "tip", "frustum", "middle", "base_frustum", "base" };
    public static final String[] ATTACHMENT_FACES = { "down", "east", "north", "south", "up", "west" };

    private RuCaveModelData() {}

    public static boolean handles(String id) {
        return ROOT.getAsJsonObject("blocks")
            .has(name(id));
    }

    public static String name(String id) {
        return id.substring(id.indexOf(':') + 1);
    }

    public static int metadata(String id, Map<String, String> properties) {
        if (multiface(id)) return attachmentMask(properties) & 15;
        return switch (name(id)) {
            case "dropleaf", "hanging_earlight", "glistering_ivy" -> dropleafAge(properties) & 15;
            case "mycotoxic_mushrooms" -> RuDecorationPaletteMetadata.leafLitterMetadata(
                Integer.parseInt(properties.getOrDefault("flower_amount", "1")),
                RuDecorationPaletteMetadata.horizontalMetadata(properties.get("facing")));
            case "hanging_prismarite" -> index(HANGING, properties.getOrDefault("hanging_prismarite_shape", "tip"));
            case "large_prismarite_cluster", "glister_bulb", "glister_spire" -> "upper".equals(properties.get("half"))
                ? 8
                : 0;
            case "tall_hyacinth_stock" -> index(
                HYACINTH,
                properties.getOrDefault("tall_hyacinth_stock_shape", "base_tip"));
            default -> 0;
        };
    }

    public static String properties(String name, int metadata) {
        if (multiface(name)) {
            StringBuilder state = new StringBuilder();
            for (int bit = 0; bit < ATTACHMENT_FACES.length; bit++) {
                if (bit > 0) state.append(',');
                state.append(ATTACHMENT_FACES[bit])
                    .append('=')
                    .append((metadata & (1 << bit)) != 0);
            }
            return state.toString();
        }
        return switch (name(name)) {
            case "hanging_prismarite" -> "hanging_prismarite_shape=" + HANGING[Math.min(metadata & 15, 2)];
            case "large_prismarite_cluster", "glister_bulb", "glister_spire" -> "half="
                + ((metadata & 8) != 0 ? "upper" : "lower");
            case "tall_hyacinth_stock" -> "tall_hyacinth_stock_shape=" + HYACINTH[Math.min(metadata & 15, 5)];
            case "mycotoxic_mushrooms" -> RuDecorationPaletteMetadata.leafLitterState("", metadata)
                .substring(1)
                .replace("]", "");
            default -> "";
        };
    }

    public static String state(String id, int metadata) {
        return state(id, metadata, 0);
    }

    public static String state(String id, int metadata, int ageBank) {
        if (hangingHead(id)) return id + "[age=" + ((metadata & 15) + ageBank) + "]";
        String properties = properties(id, multiface(id) ? (metadata & 15) | ageBank : metadata);
        if (multiface(id)) properties = properties.replace(",west=", ",waterlogged=false,west=");
        return id + (properties.isEmpty() ? "" : "[" + properties + "]");
    }

    public static int dropleafAge(Map<String, String> properties) {
        int age = Integer.parseInt(properties.getOrDefault("age", "0"));
        if (age < 0 || age > 25) throw new IllegalArgumentException("Invalid RU dropleaf age " + age);
        return age;
    }

    public static boolean hangingHead(String id) {
        return name(id).equals("dropleaf") || name(id).equals("hanging_earlight") || name(id).equals("glistering_ivy");
    }

    public static String hangingFamily(String id) {
        String value = name(id);
        if (value.endsWith("_plant")) value = value.substring(0, value.length() - 6);
        return hangingHead(value) ? value : null;
    }

    public static JsonArray variants(String name, int metadata) {
        return ROOT.getAsJsonObject("blocks")
            .getAsJsonObject(name(name))
            .getAsJsonArray(properties(name, metadata));
    }

    public static JsonArray textures() {
        return ROOT.getAsJsonArray("textures");
    }

    public static boolean alwaysWaterFilled(String id) {
        return name(id).equals("hyacinth_bloom") || name(id).equals("tall_hyacinth_stock");
    }

    /** Geometry-based update after breaking/placing a stalk segment; no terrain generation. */
    public static int hyacinthShape(boolean below, boolean above, boolean twoAbove) {
        return index(
            HYACINTH,
            !below ? (!above ? "base_tip" : !twoAbove ? "base_frustum" : "base")
                : !above ? "tip" : !twoAbove ? "frustum" : "middle");
    }

    public static String itemTexture(String id) {
        if (multiface(id)) return "biomic:ru_decor/" + name(id);
        return switch (name(id)) {
            case "brimsprout" -> "biomic:ru_decor/brimsprout_1";
            case "glistering_fern" -> "biomic:ru_decor/glistering_fern_leaf";
            case "glister_bulb" -> "biomic:ru_decor/glister_bulb_head";
            case "glister_spire" -> "biomic:ru_decor/glister_spire_top";
            case "hyacinth_bloom" -> "biomic:ru_decor/hyacinth_stock";
            case "tall_hyacinth_stock" -> "biomic:ru_decor/hyacinth_stock_tall_tip";
            default -> null;
        };
    }

    /** Keep the existing BlockDoublePlant placement/breaking contract and saved metadata. */
    public static boolean legacyDoublePlant(String id) {
        return name(id).equals("glister_bulb") || name(id).equals("glister_spire");
    }

    public static boolean multiface(String id) {
        return switch (name(id)) {
            case "hyacinth_flowers", "blue_magnolia_flowers", "pink_magnolia_flowers", "white_magnolia_flowers" -> true;
            default -> false;
        };
    }

    public static int attachmentMask(Map<String, String> properties) {
        int mask = 0;
        for (int bit = 0; bit < ATTACHMENT_FACES.length; bit++)
            if ("true".equals(properties.get(ATTACHMENT_FACES[bit]))) mask |= 1 << bit;
        return mask;
    }

    public static int rainbow(int x, int z) {
        // RuColors.getRainbowColor: float coordinate addition is intentional at large world coordinates.
        return java.awt.Color.HSBtoRGB(((float) x + (float) z) / 50F, .9F, 1F) & 0xFFFFFF;
    }

    private static int index(String[] values, String value) {
        for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return i;
        throw new IllegalArgumentException("Unknown RU cave shape " + value);
    }

    private static JsonObject load() {
        try (var stream = RuCaveModelData.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing pinned cave models");
            var root = new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8))
                .getAsJsonObject();
            if (!industries.angelic.biomic.biome.ru.RuRuntimeManifest.SOURCE_REVISION.equals(
                root.get("source_revision")
                    .getAsString()))
                throw new IllegalStateException("Cave model revision drift");
            return root;
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot load cave model data", failure);
        }
    }
}
