package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.biomic.client.VanillaTextures;
import industries.angelic.biomic.content.BiomicLushPlantBlocks;

public class OrigenLushPlantParityTest {

    @Test
    public void missingModernStatesResolveWithoutVanillaSubstitutes() {
        OrigenBlockStateResolver resolver = new OrigenBlockStateResolver();

        OrigenBlockStateResolver.ResolvedBlock blossom = resolver.resolve("minecraft:spore_blossom");
        assertSame(BiomicLushPlantBlocks.SPORE_BLOSSOM, blossom.block());
        assertEquals(0, blossom.metadata());
        assertFalse(blossom.compatibilitySubstitute());

        OrigenBlockStateResolver.ResolvedBlock propagule = resolver
            .resolve("minecraft:mangrove_propagule[hanging=true,age=3]");
        assertSame(BiomicLushPlantBlocks.HANGING_MANGROVE_PROPAGULE, propagule.block());
        assertEquals(3, propagule.metadata());
        assertFalse(propagule.compatibilitySubstitute());

        assertEquals("minecraft:spore_blossom", resolver.canonicalId(blossom.block(), blossom.metadata()));
        assertEquals("minecraft:mangrove_propagule", resolver.canonicalId(propagule.block(), propagule.metadata()));
    }

    @Test
    public void wetLushProfilesRetainExactOrigenFeatureOwnership() throws Exception {
        JsonObject manifest = manifest();
        JsonObject features = manifest.getAsJsonObject("features");
        JsonObject caveBlossoms = features.getAsJsonObject("CAVE_SPORE_BLOSSOMS");
        JsonObject caveDistributor = caveBlossoms.getAsJsonObject("distributor");
        assertEquals(
            "PADDED_GRID",
            caveDistributor.get("type")
                .getAsString());
        assertEquals(
            1824,
            caveDistributor.get("salt")
                .getAsInt());
        assertEquals(
            15,
            caveDistributor.get("width")
                .getAsInt());
        assertEquals(
            1,
            caveDistributor.get("padding")
                .getAsInt());

        JsonObject mangroveTrees = features.getAsJsonObject("MANGROVE_TREES");
        assertEquals(
            7718,
            mangroveTrees.getAsJsonObject("distributor")
                .get("salt")
                .getAsInt());

        JsonObject profiles = manifest.getAsJsonObject("profiles");
        assertProfileOwns(profiles, "LUSH_CAVES", "CAVE_SPORE_BLOSSOMS");
        assertProfileOwns(profiles, "VINE_VAULT", "CAVE_SPORE_BLOSSOMS");
        assertProfileOwns(profiles, "SECLUDED_VALLEY", "CAVE_SPORE_BLOSSOMS");
        assertProfileOwns(profiles, "MANGROVE_SWAMP", "MANGROVE_TREES");
        assertProfileOwns(profiles, "MURKY_MARSHLANDS", "MANGROVE_TREES");
        assertProfileOwns(profiles, "BAMBOO_BASIN", "MANGROVE_TREES");

        String propaguleScript = manifest.getAsJsonObject("structures")
            .getAsJsonObject("propagule")
            .get("script")
            .getAsString();
        assertTrue(propaguleScript.contains("minecraft:mangrove_propagule[hanging=true,age="));
        assertTrue(VanillaTextures.downloads("textures/blocks/spore_blossom.png"));
        assertTrue(VanillaTextures.downloads("textures/blocks/spore_blossom_base.png"));
    }

    private JsonObject manifest() throws Exception {
        try (InputStreamReader reader = new InputStreamReader(
            getClass().getResourceAsStream(OrigenTerrainRuntime.RESOURCE),
            StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader)
                .getAsJsonObject();
        }
    }

    private static void assertProfileOwns(JsonObject profiles, String profile, String feature) {
        JsonObject stages = profiles.getAsJsonObject(profile)
            .getAsJsonObject("features");
        for (Map.Entry<String, JsonElement> stage : stages.entrySet()) {
            JsonArray values = stage.getValue()
                .getAsJsonArray();
            for (JsonElement value : values) {
                if (feature.equals(value.getAsString())) return;
            }
        }
        throw new AssertionError(profile + " does not own " + feature);
    }
}
