package industries.angelic.biomic.world.terrain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import industries.angelic.biomic.client.VanillaTextures;
import industries.angelic.biomic.content.BiomicAquaticBlocks;
import industries.angelic.biomic.world.ChunkProviderBiomic;

public class OrigenAquaticParityTest {

    private static final String[] CORAL_COLORS = { "brain", "bubble", "fire", "horn", "tube" };

    @Test
    public void authoredAquaticStatesResolveWithoutCompatibilitySubstitutes() {
        OrigenBlockStateResolver resolver = new OrigenBlockStateResolver();

        assertResolved(resolver, "minecraft:kelp[age=25]", BiomicAquaticBlocks.KELP, 1);
        assertResolved(resolver, "minecraft:kelp_plant", BiomicAquaticBlocks.KELP, 0);
        assertResolved(resolver, "minecraft:seagrass", BiomicAquaticBlocks.SEAGRASS, 0);
        assertResolved(resolver, "minecraft:tall_seagrass[half=lower]", BiomicAquaticBlocks.SEAGRASS, 1);
        assertResolved(resolver, "minecraft:tall_seagrass[half=upper]", BiomicAquaticBlocks.SEAGRASS, 2);

        for (int metadata = 0; metadata < CORAL_COLORS.length; metadata++) {
            String color = CORAL_COLORS[metadata];
            assertResolved(resolver, "minecraft:" + color + "_coral", BiomicAquaticBlocks.CORAL_PLANT, metadata);
            assertResolved(resolver, "minecraft:" + color + "_coral_fan", BiomicAquaticBlocks.CORAL_FAN, metadata);
            assertResolved(resolver, "minecraft:" + color + "_coral_block", BiomicAquaticBlocks.CORAL_BLOCK, metadata);
        }

        assertEquals(Material.water, BiomicAquaticBlocks.KELP.getMaterial());
        assertEquals(Material.water, BiomicAquaticBlocks.SEAGRASS.getMaterial());
        assertEquals(Material.water, BiomicAquaticBlocks.CORAL_PLANT.getMaterial());
        assertEquals(Material.water, BiomicAquaticBlocks.CORAL_FAN.getMaterial());
    }

    @Test
    public void deadCoralSurfacePaletteUsesTheBackportedCoralBlock() {
        OrigenBlockStateResolver resolver = new OrigenBlockStateResolver();
        assertResolved(resolver, "minecraft:dead_brain_coral_block", BiomicAquaticBlocks.CORAL_BLOCK, 5);
        assertResolved(resolver, "minecraft:dead_bubble_coral_block", BiomicAquaticBlocks.CORAL_BLOCK, 6);
        assertResolved(resolver, "minecraft:dead_horn_coral_block", BiomicAquaticBlocks.CORAL_BLOCK, 8);

        byte[] materials = { BiomicTerrainSampler.DEAD_BRAIN_CORAL, BiomicTerrainSampler.DEAD_BUBBLE_CORAL,
            BiomicTerrainSampler.DEAD_HORN_CORAL };
        byte[] metadata = ChunkProviderBiomic.resolveMetadata(materials);
        assertEquals(5, metadata[0]);
        assertEquals(6, metadata[1]);
        assertEquals(8, metadata[2]);
    }

    @Test
    public void aquaticProfilesRetainExactOrigenFeatureOwnershipAndSalts() throws Exception {
        JsonObject manifest = manifest();
        JsonObject features = manifest.getAsJsonObject("features");
        assertFeatureSalt(features, "KELP", 7234);
        assertFeatureSalt(features, "SEAGRASS", 5252);
        assertFeatureSalt(features, "CORAL_PLANTS", 6485);
        assertFeatureSalt(features, "CORAL_FANS", 1596);
        assertFeatureSalt(features, "BALL_REEF", 6785);
        assertFeatureSalt(features, "DENDRIFORM_REEF", 8526);

        JsonObject profiles = manifest.getAsJsonObject("profiles");
        assertProfileOwns(profiles, "CORAL_OCEAN", "BALL_REEF");
        assertProfileOwns(profiles, "CORAL_OCEAN", "DENDRIFORM_REEF");
        assertProfileOwns(profiles, "ABYSSAL_ALLEYS", "CORAL_PLANTS");
        assertProfileOwns(profiles, "STONEGATE_SEAS", "CORAL_FANS");
        assertProfileOwns(profiles, "CANOPY_CASCADES", "BALL_REEF");
        assertProfileOwns(profiles, "VERTICAL_JUNGLE_RIVER", "DENDRIFORM_REEF");
        assertEquals(68, featureOwnerCount(profiles, "KELP"));
        assertEquals(68, featureOwnerCount(profiles, "SEAGRASS"));

        JsonObject structures = manifest.getAsJsonObject("structures");
        assertTrue(
            structures.getAsJsonObject("kelp")
                .get("script")
                .getAsString()
                .contains("minecraft:kelp[age=25]"));
        assertTrue(
            structures.getAsJsonObject("tall_seagrass")
                .get("script")
                .getAsString()
                .contains("minecraft:tall_seagrass[half=upper]"));
    }

    @Test
    public void everyBackportedAquaticTextureIsDownloadedNotPackaged() {
        String[] animated = { "kelp", "kelp_plant", "seagrass", "tall_seagrass_bottom", "tall_seagrass_top" };
        for (String texture : animated) {
            assertTexture(texture + ".png");
            assertTexture(texture + ".png.mcmeta");
        }
        for (String color : CORAL_COLORS) {
            assertTexture(color + "_coral.png");
            assertTexture(color + "_coral_fan.png");
            assertTexture(color + "_coral_block.png");
            assertTexture("dead_" + color + "_coral.png");
            assertTexture("dead_" + color + "_coral_fan.png");
            assertTexture("dead_" + color + "_coral_block.png");
        }
    }

    private static void assertResolved(OrigenBlockStateResolver resolver, String state, Block expectedBlock,
        int expectedMetadata) {
        OrigenBlockStateResolver.ResolvedBlock resolved = resolver.resolve(state);
        assertSame(expectedBlock, resolved.block());
        assertEquals(expectedMetadata, resolved.metadata());
        assertFalse(resolved.compatibilitySubstitute());
        // The updated physical boundary preserves the two seagrass halves; stripping them here
        // would regress the shape/survival audit back to its old ambiguous tall-seagrass identity.
        String expected = state.startsWith("minecraft:tall_seagrass[") ? state
            : state.substring(0, state.indexOf('[') < 0 ? state.length() : state.indexOf('['));
        assertEquals(expected, resolver.canonicalId(resolved.block(), resolved.metadata()));
    }

    private JsonObject manifest() throws Exception {
        try (InputStreamReader reader = new InputStreamReader(
            getClass().getResourceAsStream(OrigenTerrainRuntime.RESOURCE),
            StandardCharsets.UTF_8)) {
            return new JsonParser().parse(reader)
                .getAsJsonObject();
        }
    }

    private static void assertFeatureSalt(JsonObject features, String feature, int salt) {
        assertEquals(
            salt,
            features.getAsJsonObject(feature)
                .getAsJsonObject("distributor")
                .getAsJsonObject("sampler")
                .get("salt")
                .getAsInt());
    }

    private static void assertProfileOwns(JsonObject profiles, String profile, String feature) {
        JsonObject stages = profiles.getAsJsonObject(profile)
            .getAsJsonObject("features");
        for (Map.Entry<String, JsonElement> stage : stages.entrySet()) {
            for (JsonElement value : stage.getValue()
                .getAsJsonArray()) {
                if (feature.equals(value.getAsString())) return;
            }
        }
        throw new AssertionError(profile + " does not own " + feature);
    }

    private static int featureOwnerCount(JsonObject profiles, String feature) {
        int owners = 0;
        for (Map.Entry<String, JsonElement> profile : profiles.entrySet()) {
            JsonObject stages = profile.getValue()
                .getAsJsonObject()
                .getAsJsonObject("features");
            boolean found = false;
            for (Map.Entry<String, JsonElement> stage : stages.entrySet()) {
                JsonArray values = stage.getValue()
                    .getAsJsonArray();
                for (JsonElement value : values) {
                    if (feature.equals(value.getAsString())) found = true;
                }
            }
            if (found) owners++;
        }
        return owners;
    }

    private void assertTexture(String filename) {
        assertTrue(filename, VanillaTextures.downloads("textures/blocks/" + filename));
        assertNull(filename, getClass().getResource("/assets/biomic/textures/blocks/" + filename));
    }
}
