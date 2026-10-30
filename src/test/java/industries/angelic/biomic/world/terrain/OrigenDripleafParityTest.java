package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.biomic.client.VanillaTextures;
import industries.angelic.biomic.content.BiomicDripleafBlocks;

public class OrigenDripleafParityTest {

    @Test
    public void modernDripleafStatesResolveToDedicatedBiomicBlocks() {
        OrigenBlockStateResolver resolver = new OrigenBlockStateResolver();

        OrigenBlockStateResolver.ResolvedBlock crown = resolver.resolve("minecraft:big_dripleaf[facing=north]");
        assertSame(BiomicDripleafBlocks.BIG_DRIPLEAF, crown.block());
        assertEquals(2, crown.metadata());
        assertFalse(crown.compatibilitySubstitute());

        OrigenBlockStateResolver.ResolvedBlock stem = resolver.resolve("minecraft:big_dripleaf_stem[facing=east]");
        assertSame(BiomicDripleafBlocks.BIG_DRIPLEAF_STEM, stem.block());
        assertEquals(3, stem.metadata());
        assertFalse(stem.compatibilitySubstitute());

        OrigenBlockStateResolver.ResolvedBlock small = resolver
            .resolve("minecraft:small_dripleaf[facing=west,half=upper]");
        assertSame(BiomicDripleafBlocks.SMALL_DRIPLEAF, small.block());
        assertEquals(5, small.metadata());
        assertFalse(small.compatibilitySubstitute());

        assertEquals("minecraft:big_dripleaf", resolver.canonicalId(crown.block(), crown.metadata()));
        assertEquals("minecraft:big_dripleaf_stem", resolver.canonicalId(stem.block(), stem.metadata()));
        assertEquals("minecraft:small_dripleaf", resolver.canonicalId(small.block(), small.metadata()));
    }

    @Test
    public void lushLoopsRetainsOrigenDripleafFeatureDefinitions() throws Exception {
        JsonObject manifest;
        try (InputStreamReader reader = new InputStreamReader(
            getClass().getResourceAsStream(OrigenTerrainRuntime.RESOURCE),
            StandardCharsets.UTF_8)) {
            manifest = new JsonParser().parse(reader)
                .getAsJsonObject();
        }
        JsonObject features = manifest.getAsJsonObject("features");
        assertDistributor(features.getAsJsonObject("BIG_DRIPLEAF"), 8893, 0.015D);
        assertDistributor(features.getAsJsonObject("SMALL_DRIPLEAF"), 4417, 0.035D);

        JsonArray flora = manifest.getAsJsonObject("profiles")
            .getAsJsonObject("LUSH_LOOPS")
            .getAsJsonObject("features")
            .getAsJsonArray("flora");
        assertTrue(contains(flora, "BIG_DRIPLEAF"));
        assertTrue(contains(flora, "SMALL_DRIPLEAF"));

        assertTrue(VanillaTextures.downloads("textures/blocks/big_dripleaf_top.png"));
        assertTrue(VanillaTextures.downloads("textures/blocks/small_dripleaf_top.png"));
    }

    private static void assertDistributor(JsonObject feature, int salt, double threshold) {
        JsonObject distributor = feature.getAsJsonObject("distributor");
        assertEquals(
            "SAMPLER",
            distributor.get("type")
                .getAsString());
        assertEquals(
            threshold,
            distributor.get("threshold")
                .getAsDouble(),
            0.0D);
        assertEquals(
            salt,
            distributor.getAsJsonObject("sampler")
                .get("salt")
                .getAsInt());
    }

    private static boolean contains(JsonArray values, String expected) {
        for (int index = 0; index < values.size(); index++) {
            if (expected.equals(
                values.get(index)
                    .getAsString()))
                return true;
        }
        return false;
    }
}
