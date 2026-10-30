package industries.angelic.biomic.world.terrain;

import org.junit.Assert;
import org.junit.Test;

public class HostedVegetationPolicyTest {

    @Test
    public void sourceFalseLilyResultIsNotAnEnvelopeRejection() {
        Assert.assertTrue(HostedVegetationPolicy.keepsConfiguredWrites("regions_unexplored:giant_lily", true, false));
        Assert.assertFalse(HostedVegetationPolicy.keepsConfiguredWrites("regions_unexplored:giant_lily", false, false));
        Assert.assertFalse(HostedVegetationPolicy.keepsConfiguredWrites("minecraft:tree", true, false));
        Assert.assertTrue(HostedVegetationPolicy.keepsConfiguredWrites("minecraft:tree", true, true));
    }

    @Test
    public void redwoodsKeepSourceMossyRocksWithoutImportingGeodesOrForeignBoulders() {
        var graph = new industries.angelic.biomic.world.modern.ModernDecorationGraph();
        var source = graph.orderedFeatures(java.util.Collections.singleton("regions_unexplored:redwoods"), 2);
        Assert.assertTrue(source.contains("minecraft:forest_rock"));
        var admitted = new java.util.ArrayList<String>();
        for (String feature : source) if (HostedVegetationPolicy.hostsDecoration(2, feature)) admitted.add(feature);
        Assert.assertEquals(java.util.Collections.singletonList("minecraft:forest_rock"), admitted);
        Assert.assertTrue(
            graph.configuredFeature("minecraft:forest_rock")
                .toString()
                .contains("mossy_cobblestone"));
        Assert.assertFalse(HostedVegetationPolicy.hostsDecoration(2, "minecraft:amethyst_geode"));
        Assert.assertFalse(HostedVegetationPolicy.hostsDecoration(6, "minecraft:ore_dirt"));
        Assert.assertFalse(HostedVegetationPolicy.hostsDecoration(2, "ROCK_BOULDER"));
        for (String feature : graph.featureOrder(9))
            Assert.assertTrue(HostedVegetationPolicy.hostsDecoration(9, feature));
    }

    @Test
    public void deadBushesAreNotPlacedOnModernOnlySubstrates() {
        for (String soil : new String[] { "grass_block", "moss_block", "mud", "water", "stone" }) {
            Assert.assertFalse(HostedVegetationPolicy.legacyDeadBushGround("minecraft:" + soil));
        }
        for (String soil : new String[] { "sand", "red_sand", "dirt", "coarse_dirt", "podzol", "red_terracotta" }) {
            Assert.assertTrue(HostedVegetationPolicy.legacyDeadBushGround("minecraft:" + soil));
        }
    }

    @Test
    public void blackwoodSwitchesRejectOnlyMushroomsAndReplaceLightsWithoutMutatingSource() {
        var tree = new com.google.gson.JsonParser()
            .parse(
                "{\"foliage_placer\":{\"type\":\"regions_unexplored:bioshroom\","
                    + "\"secondary_provider\":{\"state\":{\"Name\":\"minecraft:shroomlight\"}}},"
                    + "\"foliage_provider\":{\"state\":{\"Name\":\"regions_unexplored:blue_bioshroom_block\"}}}")
            .getAsJsonObject();
        String biome = "regions_unexplored:blackwood_taiga";
        Assert.assertNull(HostedVegetationPolicy.configured(biome, "minecraft:tree", tree, false, true, true));
        var unlit = HostedVegetationPolicy.configured(biome, "minecraft:tree", tree, true, false, false);
        Assert.assertEquals(
            tree.get("foliage_provider"),
            unlit.getAsJsonObject("foliage_placer")
                .get("secondary_provider"));
        Assert.assertTrue(
            tree.toString()
                .contains("shroomlight"));
        Assert.assertSame(tree, HostedVegetationPolicy.configured(biome, "minecraft:tree", tree, true, false, true));
        var small = new com.google.gson.JsonParser()
            .parse("{\"to_place\":{\"state\":{\"Name\":\"regions_unexplored:tall_pink_bioshroom\"}}}")
            .getAsJsonObject();
        Assert.assertNull(HostedVegetationPolicy.configured(biome, "minecraft:simple_block", small, true, false, true));
        Assert.assertSame(
            small,
            HostedVegetationPolicy.configured(biome, "minecraft:simple_block", small, false, true, false));
        Assert.assertSame(
            tree,
            HostedVegetationPolicy
                .configured("regions_unexplored:boreal_taiga", "minecraft:tree", tree, false, false, false));
        Assert.assertSame(
            tree,
            HostedVegetationPolicy.configured(biome, "lithostitched:weighted_selector", tree, false, false, false));
    }
}
