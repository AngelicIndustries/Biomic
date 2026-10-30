package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.biomic.content.BiomicSculkBlocks;
import industries.angelic.biomic.content.block.BlockSculkVein;

public class OrigenSculkParityTest {

    @Test
    public void missingSculkStatesResolveToDedicatedBiomicBlocks() {
        OrigenBlockStateResolver resolver = new OrigenBlockStateResolver();

        assertResolved(resolver, "minecraft:sculk_sensor", BiomicSculkBlocks.SCULK_SENSOR, 0);
        assertResolved(resolver, "minecraft:sculk_shrieker[can_summon=true]", BiomicSculkBlocks.SCULK_SHRIEKER, 0);
        assertResolved(
            resolver,
            "minecraft:sculk_vein[up=false,down=false,north=false,east=true,south=false,west=false]",
            BiomicSculkBlocks.SCULK_VEIN,
            BlockSculkVein.EAST);
        assertResolved(
            resolver,
            "minecraft:sculk_vein[up=true,down=false,north=true,east=false,south=true,west=false]",
            BiomicSculkBlocks.SCULK_VEIN_UP,
            BlockSculkVein.NORTH | BlockSculkVein.SOUTH);
        assertResolved(
            resolver,
            "minecraft:sculk_vein[up=false,down=true,north=false,east=true,south=false,west=true]",
            BiomicSculkBlocks.SCULK_VEIN_DOWN,
            BlockSculkVein.EAST | BlockSculkVein.WEST);
        assertResolved(
            resolver,
            "minecraft:sculk_vein[up=true,down=true,north=true,east=true,south=true,west=true]",
            BiomicSculkBlocks.SCULK_VEIN_VERTICAL,
            15);

        assertEquals(Material.rock, BiomicSculkBlocks.SCULK_SENSOR.getMaterial());
        assertEquals(Material.rock, BiomicSculkBlocks.SCULK_SHRIEKER.getMaterial());
        assertEquals(Material.vine, BiomicSculkBlocks.SCULK_VEIN.getMaterial());
    }

    @Test
    public void deepDarkRetainsTheCompleteOrigenSculkGrammar() throws Exception {
        JsonObject manifest = manifest();
        JsonObject profileFeatures = manifest.getAsJsonObject("profiles")
            .getAsJsonObject("DEEP_DARK")
            .getAsJsonObject("features");
        assertContains(profileFeatures.getAsJsonArray("preprocessors"), "SCULK_WEBS");
        assertContains(profileFeatures.getAsJsonArray("preprocessors"), "SCULK_PATCHES");
        assertContains(profileFeatures.getAsJsonArray("preprocessors"), "SCULK_TENDRIL");
        assertContains(profileFeatures.getAsJsonArray("flora"), "SCULK_FLORA");

        JsonArray flora = manifest.getAsJsonObject("features")
            .getAsJsonObject("SCULK_FLORA")
            .getAsJsonArray("structures");
        assertWeightedStructure(flora, "BLOCK:minecraft:sculk_shrieker[can_summon=true]", 2);
        assertWeightedStructure(flora, "BLOCK:minecraft:sculk_catalyst", 2);
        assertWeightedStructure(flora, "BLOCK:minecraft:sculk_sensor", 5);
        assertWeightedStructure(flora, "blank", 8);

        JsonObject structures = manifest.getAsJsonObject("structures");
        String adjacent = structures.getAsJsonObject("place_adjacent_sculk_veins")
            .get("script")
            .getAsString();
        assertEquals(6, occurrences(adjacent, "sculk_vein"));
        String vein = structures.getAsJsonObject("sculk_vein")
            .get("script")
            .getAsString();
        for (String face : new String[] { "up", "down", "north", "east", "south", "west" }) {
            assertTrue(vein.contains(face + "=true"));
            assertTrue(vein.contains(face + "=false"));
        }
    }

    private static void assertResolved(OrigenBlockStateResolver resolver, String state, Block expectedBlock,
        int expectedMetadata) {
        OrigenBlockStateResolver.ResolvedBlock resolved = resolver.resolve(state);
        assertSame(expectedBlock, resolved.block());
        assertEquals(expectedMetadata, resolved.metadata());
        assertFalse(resolved.compatibilitySubstitute());
        assertEquals(
            state.substring(0, state.indexOf('[') < 0 ? state.length() : state.indexOf('[')),
            resolver.canonicalId(resolved.block(), resolved.metadata()));
    }

    private JsonObject manifest() throws Exception {
        try (InputStreamReader reader = new InputStreamReader(
            getClass().getResourceAsStream(OrigenTerrainRuntime.RESOURCE),
            StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader)
                .getAsJsonObject();
        }
    }

    private static void assertContains(JsonArray values, String expected) {
        for (JsonElement value : values) {
            if (expected.equals(value.getAsString())) return;
        }
        throw new AssertionError("Missing " + expected);
    }

    private static void assertWeightedStructure(JsonArray choices, String expected, int weight) {
        for (JsonElement element : choices) {
            JsonObject choice = element.getAsJsonObject();
            if (expected.equals(
                choice.get("structure")
                    .getAsString())) {
                assertEquals(
                    weight,
                    choice.get("weight")
                        .getAsInt());
                return;
            }
        }
        throw new AssertionError("Missing weighted structure " + expected);
    }

    private static int occurrences(String value, String needle) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(needle, offset)) >= 0) {
            count++;
            offset += needle.length();
        }
        return count;
    }
}
