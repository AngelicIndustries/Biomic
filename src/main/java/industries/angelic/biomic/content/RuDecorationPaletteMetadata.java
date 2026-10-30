package industries.angelic.biomic.content;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.biomic.biome.ru.RuRuntimeManifest;

/** Lightweight block semantics extracted from the generated RU decoration palette. */
public final class RuDecorationPaletteMetadata {

    private static final String RESOURCE = "/assets/biomic/ru_reference/decoration-palette.json";
    private static final Set<String> DOUBLE_PLANTS = loadDoublePlants();

    private RuDecorationPaletteMetadata() {}

    public static boolean isDoublePlant(String modernId) {
        return DOUBLE_PLANTS.contains(modernId);
    }

    public static boolean isFlowerbed(String modernId) {
        return modernId.endsWith("_leaf_litter") || modernId.equals("regions_unexplored:orange_coneflower")
            || modernId.equals("regions_unexplored:purple_coneflower")
            || modernId.equals("regions_unexplored:clover");
    }

    public static int doublePlantHalfMetadata(String half) {
        return "upper".equals(half) ? 8 : 0;
    }

    public static String doublePlantState(String modernId, int metadata) {
        return modernId + "[half=" + ((metadata & 8) == 0 ? "lower" : "upper") + ']';
    }

    public static int horizontalMetadata(String facing) {
        if (facing == null) return 0;
        return switch (facing) {
            case "north" -> 0;
            case "east" -> 1;
            case "south" -> 2;
            case "west" -> 3;
            default -> throw new IllegalArgumentException("Not a horizontal facing: " + facing);
        };
    }

    public static String horizontalFacing(int metadata) {
        return switch (metadata & 3) {
            case 1 -> "east";
            case 2 -> "south";
            case 3 -> "west";
            default -> "north";
        };
    }

    /** Four patch amounts times four horizontal facings fit losslessly in legacy metadata. */
    public static int leafLitterMetadata(int amount, int facing) {
        if (amount < 1 || amount > 4) throw new IllegalArgumentException("Leaf litter amount " + amount);
        return (amount - 1) | ((facing & 3) << 2);
    }

    public static String leafLitterState(String modernId, int metadata) {
        String facing = switch ((metadata >> 2) & 3) {
            case 1 -> "east";
            case 2 -> "south";
            case 3 -> "west";
            default -> "north";
        };
        return modernId + "[facing=" + facing + ",flower_amount=" + ((metadata & 3) + 1) + ']';
    }

    private static Set<String> loadDoublePlants() {
        InputStream stream = RuDecorationPaletteMetadata.class.getResourceAsStream(RESOURCE);
        if (stream == null) throw new IllegalStateException("Missing generated RU decoration palette " + RESOURCE);
        try (InputStream input = stream;
            InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader)
                .getAsJsonObject();
            require(
                "biomic-ru-decoration-palette-v1".equals(
                    root.get("format")
                        .getAsString()),
                "unexpected format");
            require(
                RuRuntimeManifest.SOURCE_REVISION.equals(
                    root.get("source_revision")
                        .getAsString()),
                "source revision drift");
            Set<String> result = new LinkedHashSet<String>();
            for (JsonElement element : root.getAsJsonArray("entries")) {
                JsonObject entry = element.getAsJsonObject();
                if ("double".equals(
                    entry.get("kind")
                        .getAsString())) {
                    result.add(
                        entry.get("id")
                            .getAsString());
                }
            }
            require(result.size() >= 40, "double-plant catalogue lost entries, got " + result.size());
            return Collections.unmodifiableSet(result);
        } catch (Exception failure) {
            throw new IllegalStateException("Could not read RU decoration semantics " + RESOURCE, failure);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Invalid RU decoration palette: " + message);
    }
}
