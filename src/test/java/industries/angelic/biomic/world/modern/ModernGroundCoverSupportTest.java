package industries.angelic.biomic.world.modern;

import org.junit.Assert;
import org.junit.Test;

public class ModernGroundCoverSupportTest {

    @Test
    public void solidVentComponentsDoNotHaveSaplingSubstrateRules() {
        for (String block : new String[] { "minecraft:magma_block", "minecraft:basalt[axis=y]",
            "regions_unexplored:ash_vent" }) {
            Assert.assertEquals(true, ModernGroundCoverSupport.testPinned(block, "minecraft:air"));
        }
    }

    @Test
    public void snowFlowersRetainSourceSnowAndDirtSupport() {
        for (String soil : new String[] { "minecraft:snow_block", "minecraft:snow[layers=8]", "minecraft:dirt" })
            Assert.assertEquals(true, ModernGroundCoverSupport.testPinned("regions_unexplored:bleeding_heart", soil));
        for (String soil : new String[] { "minecraft:snow[layers=7]", "minecraft:stone", "minecraft:farmland" })
            Assert.assertEquals(false, ModernGroundCoverSupport.testPinned("regions_unexplored:bleeding_heart", soil));
    }

    @Test
    public void ashVentSolidsDoNotAcquireSaplingSurvivalRules() {
        for (String solid : new String[] { "minecraft:basalt", "minecraft:smooth_basalt",
            "regions_unexplored:ash_vent" })
            Assert.assertEquals(true, ModernGroundCoverSupport.testPinned(solid, "minecraft:air"));
    }

    @Test
    public void frozenGrassUsesItsSnowTagAndRejectsPartialSnowLayers() {
        Assert.assertEquals(
            true,
            ModernGroundCoverSupport.testPinned("regions_unexplored:frozen_grass", "minecraft:snow_block"));
        Assert.assertEquals(
            true,
            ModernGroundCoverSupport.testPinned("regions_unexplored:frozen_grass", "minecraft:snow[layers=8]"));
        Assert.assertEquals(
            false,
            ModernGroundCoverSupport.testPinned("regions_unexplored:frozen_grass", "minecraft:snow[layers=7]"));
        Assert.assertEquals(
            false,
            ModernGroundCoverSupport.testPinned("regions_unexplored:frozen_grass", "minecraft:air"));
    }

    private static final ModernDecorationGraph GRAPH = new ModernDecorationGraph();

    private static Boolean supports(String plant, String soil) {
        return ModernGroundCoverSupport.test(
            plant,
            soil,
            (tag, state) -> GRAPH.blockTag(tag)
                .contains(ModernDecorationEngine.blockName(state)));
    }

    @Test
    public void saguaroAndJoshuaHaveDifferentSourceSubstrates() {
        Assert.assertEquals(true, supports("regions_unexplored:saguaro_cactus_flower", "minecraft:sand"));
        Assert.assertEquals(
            true,
            supports("regions_unexplored:saguaro_cactus_flower", "regions_unexplored:saguaro_cactus"));
        Assert.assertEquals(false, supports("regions_unexplored:joshua_sapling", "minecraft:sand"));
        Assert.assertEquals(true, supports("regions_unexplored:joshua_sapling", "minecraft:grass_block"));
        Assert.assertEquals(true, supports("regions_unexplored:barrel_cactus", "minecraft:red_sandstone"));
        Assert.assertEquals(false, supports("regions_unexplored:barrel_cactus", "minecraft:dirt"));
    }

    @Test
    public void sourceTagsCoverCustomSoilsAndSandPlants() {
        Assert.assertEquals(true, supports("minecraft:fern", "regions_unexplored:peat_dirt"));
        Assert.assertEquals(false, supports("minecraft:fern", "minecraft:stone"));
        Assert.assertEquals(true, supports("regions_unexplored:sandy_grass", "minecraft:cut_sandstone"));
        Assert.assertEquals(false, supports("regions_unexplored:sandy_grass", "minecraft:dirt"));
        Assert.assertEquals(true, supports("regions_unexplored:small_desert_shrub", "minecraft:dirt"));
        Assert.assertEquals(false, supports("regions_unexplored:maple_shrub", "minecraft:stone"));
    }

    @Test
    public void doublePlantsRequireTheirLowerHalfAndComplexRulesRemainSeparate() {
        Assert.assertEquals(true, supports("minecraft:large_fern[half=upper]", "minecraft:large_fern[half=lower]"));
        Assert.assertEquals(false, supports("minecraft:large_fern[half=upper]", "minecraft:stone"));
        Assert.assertNull(supports("minecraft:red_mushroom", "minecraft:stone"));
        Assert.assertNull(supports("minecraft:vine[north=true]", "minecraft:air"));
        Assert.assertNull(supports("regions_unexplored:maple_leaf_litter", "minecraft:stone"));
    }

    @Test
    public void bambooAndElephantEarsUseTheirActualSourceRules() {
        Assert.assertEquals(true, supports("minecraft:bamboo", "minecraft:bamboo"));
        Assert.assertEquals(true, supports("minecraft:bamboo", "minecraft:sand"));
        Assert.assertEquals(false, supports("minecraft:bamboo", "minecraft:stone"));
        Assert.assertEquals(true, supports("regions_unexplored:elephant_ear[half=lower]", "minecraft:grass_block"));
        Assert.assertEquals(false, supports("regions_unexplored:elephant_ear[half=lower]", "minecraft:sand"));
        Assert.assertEquals(
            true,
            supports("regions_unexplored:elephant_ear[half=upper]", "regions_unexplored:elephant_ear[half=lower]"));
    }

    @Test
    public void physicalRuSproutsAndDesertPlantsUseTheSamePinnedSubstrates() {
        for (String soil : new String[] { "minecraft:stone", "minecraft:terracotta", "minecraft:grass_block",
            "regions_unexplored:peat_coarse_dirt", "regions_unexplored:silt_dirt" }) {
            Assert.assertEquals(true, ModernGroundCoverSupport.testPinned("regions_unexplored:grass_sprouts", soil));
        }
        Assert.assertEquals(
            false,
            ModernGroundCoverSupport.testPinned("regions_unexplored:grass_sprouts", "minecraft:air"));
        Assert.assertEquals(
            true,
            ModernGroundCoverSupport.testPinned("regions_unexplored:sandy_grass", "minecraft:sandstone"));
        Assert.assertEquals(
            false,
            ModernGroundCoverSupport.testPinned("regions_unexplored:sandy_grass", "minecraft:grass_block"));
        Assert.assertEquals(
            true,
            ModernGroundCoverSupport.testPinned("regions_unexplored:small_desert_shrub", "minecraft:terracotta"));
        for (String flower : new String[] { "aster", "blue_lupine", "pink_lupine", "purple_lupine", "red_lupine",
            "yellow_lupine", "daisy", "fireweed", "hyssop", "mallow", "waratah" }) {
            Assert.assertEquals(
                flower,
                true,
                ModernGroundCoverSupport.testPinned("regions_unexplored:" + flower, "minecraft:grass_block"));
        }
    }
}
