package industries.angelic.biomic.world.modern;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ModernDecorationGraphTest {

    @Test
    public void livePostModifierClosureAndGlobalOrderLoad() {
        ModernDecorationGraph graph = new ModernDecorationGraph();
        Assert.assertEquals(
            113,
            graph.biomeIds()
                .size());
        Assert.assertEquals(
            312,
            graph.featureOrder(9)
                .size());
        Assert.assertTrue(
            graph.blockTag("minecraft:replaceable_by_trees")
                .contains("minecraft:oak_leaves"));
    }

    @Test
    public void neighboringBiomeUnionRetainsPinnedFeatureSorterOrder() {
        ModernDecorationGraph graph = new ModernDecorationGraph();
        List<String> ordered = graph
            .orderedFeatures(new HashSet<String>(Arrays.asList("minecraft:swamp", "regions_unexplored:bayou")), 9);
        Assert.assertTrue(ordered.contains("regions_unexplored:vanilla_changes/swamp_trees"));
        Assert.assertTrue(ordered.contains("regions_unexplored:tree/group/bayou"));
        int previous = -1;
        for (String feature : ordered) {
            int index = graph.featureOrder(9)
                .indexOf(feature);
            Assert.assertTrue(index > previous);
            previous = index;
        }
    }

    @Test
    public void regionsUnexploredsNetherBiomesFollowTheOverworldInEachStep() {
        ModernDecorationGraph graph = new ModernDecorationGraph();
        Assert.assertEquals(
            113,
            graph.biomeIds()
                .size());
        List<String> step = graph.featureOrder(7);
        // The overworld's own five keep their indices (their seeds); the Nether's follow.
        Assert.assertTrue(step.size() > 5);
        for (String feature : step.subList(0, 5)) Assert.assertFalse(feature, feature.contains(":nether/"));
        for (String biome : new String[] { "blackstone_basin", "glistering_meadow", "infernal_holt",
            "mycotoxic_undergrowth" }) {
            String key = "regions_unexplored:" + biome;
            Assert.assertTrue(graph.hasBiome(key));
            Assert.assertFalse(
                graph.biomeIds()
                    .contains(key));
            List<String> ordered = graph.orderedFeatures(new HashSet<String>(Arrays.asList(key)), 7);
            Assert.assertFalse(ordered.isEmpty());
            for (String feature : ordered) Assert.assertTrue(step.indexOf(feature) >= 5);
        }
    }

    @Test
    public void anOverlayMakesAPlacedFeaturePlaceAnotherConfiguredFeature() {
        String secondary = "regions_unexplored:tree/group/bamboo_forest_secondary";
        Assert.assertEquals(
            "minecraft:bamboo_some_podzol",
            new ModernDecorationGraph(industries.angelic.biomic.api.EcologyOverlays.compose("modern-features.json"))
                .placedFeature(secondary)
                .get("feature")
                .getAsString());
        com.google.gson.JsonObject overlay = new com.google.gson.JsonParser()
            .parse("{\"placed\": {\"" + secondary + "\": \"regions_unexplored:tree/bamboo\"}}")
            .getAsJsonObject();
        com.google.gson.JsonObject overrides = industries.angelic.biomic.api.EcologyOverlays
            .compose("modern-features.json");
        industries.angelic.biomic.api.EcologyOverlays.merge(overrides, overlay, "test");
        ModernDecorationGraph graph = new ModernDecorationGraph(overrides);
        Assert.assertEquals(
            "regions_unexplored:tree/bamboo",
            graph.placedFeature(secondary)
                .get("feature")
                .getAsString());
        Assert.assertEquals(
            new ModernDecorationGraph(industries.angelic.biomic.api.EcologyOverlays.compose("modern-features.json"))
                .featureOrder(9),
            graph.featureOrder(9));
    }
}
