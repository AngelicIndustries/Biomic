package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class OrigenFeatureManifestTest {

    @Test
    public void containsEveryAuthoredFeatureStructureAndOreWithResolvableReferences() throws Exception {
        JsonObject manifest;
        try (InputStreamReader reader = new InputStreamReader(
            getClass().getResourceAsStream(OrigenTerrainRuntime.RESOURCE),
            StandardCharsets.UTF_8)) {
            manifest = new JsonParser().parse(reader)
                .getAsJsonObject();
        }
        JsonObject features = manifest.getAsJsonObject("features");
        JsonObject structures = manifest.getAsJsonObject("structures");
        JsonObject ores = manifest.getAsJsonObject("ores");
        List<String> authoredBlockIds = StreamSupport.stream(
            manifest.getAsJsonArray("authored_block_ids")
                .spliterator(),
            false)
            .map(JsonElement::getAsString)
            .collect(Collectors.toList());
        assertEquals(
            229,
            features.entrySet()
                .size());
        assertEquals(
            160,
            structures.entrySet()
                .size());
        assertEquals(
            22,
            ores.entrySet()
                .size());
        assertEquals(219, authoredBlockIds.size());
        assertTrue(authoredBlockIds.contains("minecraft:rooted_dirt"));
        assertTrue(authoredBlockIds.contains("minecraft:beetroots"));
        assertTrue(authoredBlockIds.contains("minecraft:spore_blossom"));
        assertTrue(!authoredBlockIds.contains("minecraft:bee"));

        Set<String> known = new HashSet<>();
        for (Map.Entry<String, JsonElement> structure : structures.entrySet()) known.add(structure.getKey());
        for (Map.Entry<String, JsonElement> ore : ores.entrySet()) known.add(ore.getKey());
        for (Map.Entry<String, JsonElement> feature : features.entrySet()) {
            for (JsonElement rawChoice : feature.getValue()
                .getAsJsonObject()
                .getAsJsonArray("structures")) {
                String id = rawChoice.getAsJsonObject()
                    .get("structure")
                    .getAsString();
                assertTrue(feature.getKey() + " -> " + id, id.startsWith("BLOCK:") || known.contains(id));
            }
        }
        assertNotNull(
            structures.getAsJsonObject("skull_center")
                .get("base64"));

        JsonObject frozenBridges = features.getAsJsonObject("CROSSING_VINES_FROZEN");
        assertEquals(
            "features/vegetation/vines/crossing_vines_frozen.yml",
            frozenBridges.get("locator_source")
                .getAsString());
        assertEquals(
            "62 + 29",
            frozenBridges.getAsJsonObject("locator")
                .getAsJsonObject("range")
                .get("min")
                .getAsString());
        assertEquals(
            "62 + 74",
            frozenBridges.getAsJsonObject("locator")
                .getAsJsonObject("range")
                .get("max")
                .getAsString());
    }

    @Test
    public void commentsOnlyMarineStageInheritsBasePreprocessorsLikeTectonic() throws Exception {
        JsonObject manifest;
        try (InputStreamReader reader = new InputStreamReader(
            getClass().getResourceAsStream(OrigenTerrainRuntime.RESOURCE),
            StandardCharsets.UTF_8)) {
            manifest = new JsonParser().parse(reader)
                .getAsJsonObject();
        }
        List<String> preprocessors = StreamSupport.stream(
            manifest.getAsJsonObject("profiles")
                .getAsJsonObject("MARINE_MONOLITHS")
                .getAsJsonObject("features")
                .getAsJsonArray("global-preprocessors")
                .spliterator(),
            false)
            .map(JsonElement::getAsString)
            .collect(Collectors.toList());
        assertEquals(
            Arrays.asList(
                "LAVA_FLOOR",
                "UNDERGROUND_LAVA_COLUMNS",
                "CONTAIN_FLOATING_WATER",
                "CAVE_GLOW_LICHEN",
                "TEXTURED_STONE_SLANT"),
            preprocessors);
    }
}
