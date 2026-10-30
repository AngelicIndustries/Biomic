package industries.angelic.biomic.world.modern;

import org.junit.Assert;
import org.junit.Test;

public class ModernAquaticPlantSupportTest {

    @Test
    public void waterPlantsNeverUseHyacinthsAsTheirSeabed() {
        for (String support : new String[] { "regions_unexplored:hyacinth_bloom",
            "regions_unexplored:tall_hyacinth_stock[tall_hyacinth_stock_shape=tip]" })
            for (String plant : new String[] { "minecraft:seagrass", "minecraft:tall_seagrass[half=lower]",
                "minecraft:kelp[age=0]" })
                Assert.assertFalse(ModernAquaticPlantSupport.test(plant, support, "minecraft:water", true));
    }

    @Test
    public void seagrassSurvivalAndPlacementAreDifferentContracts() {
        Assert
            .assertTrue(ModernAquaticPlantSupport.test("minecraft:seagrass", "minecraft:stone", "minecraft:air", true));
        Assert.assertFalse(
            ModernAquaticPlantSupport.test("minecraft:seagrass", "minecraft:magma_block", "minecraft:water", true));
        Assert.assertFalse(
            ModernAquaticPlantSupport.test("minecraft:seagrass", "minecraft:water", "minecraft:water", false));
    }

    @Test
    public void tallSeagrassRequiresFullWaterOrItsLowerHalf() {
        String lower = "minecraft:tall_seagrass[half=lower]", upper = "minecraft:tall_seagrass[half=upper]";
        for (int level = 0; level < 16; level++) Assert.assertEquals(
            level == 0 || level >= 8,
            ModernAquaticPlantSupport.test(lower, "minecraft:sand", "minecraft:water[level=" + level + "]", true));
        Assert.assertFalse(ModernAquaticPlantSupport.test(lower, "minecraft:sand", "minecraft:air", true));
        Assert.assertTrue(ModernAquaticPlantSupport.test(lower, "minecraft:sand", lower, true));
        Assert.assertTrue(ModernAquaticPlantSupport.test(upper, lower, upper, false));
        Assert.assertFalse(ModernAquaticPlantSupport.test(upper, upper, upper, false));
        Assert.assertNull(ModernAquaticPlantSupport.test("minecraft:fern", "minecraft:dirt", "minecraft:air", true));
    }

    @Test
    public void kelpRequiresSolidFootingOrKelpButNeverMagma() {
        for (String name : new String[] { "minecraft:kelp[age=20]", "minecraft:kelp_plant" }) {
            Assert.assertTrue(ModernAquaticPlantSupport.test(name, "minecraft:stone", "minecraft:water", true));
            Assert.assertTrue(ModernAquaticPlantSupport.test(name, "minecraft:kelp_plant", "minecraft:water", false));
            Assert.assertFalse(ModernAquaticPlantSupport.test(name, "minecraft:magma_block", "minecraft:water", true));
            Assert.assertFalse(ModernAquaticPlantSupport.test(name, "minecraft:water", "minecraft:water", false));
        }
    }

    @Test
    public void cattailUsesSourceSoilAndKeepsItsUpperHalfOutOfSourceWater() {
        String plant = "regions_unexplored:cattail[half=lower,waterlogged=true]";
        Assert.assertTrue(ModernAquaticPlantSupport.cattail(plant, "minecraft:dirt", "minecraft:air", true));
        Assert
            .assertFalse(ModernAquaticPlantSupport.cattail(plant, "minecraft:dirt", "minecraft:water[level=0]", true));
        Assert.assertFalse(ModernAquaticPlantSupport.cattail(plant, "minecraft:stone", "minecraft:air", false));
        Assert.assertTrue(ModernAquaticPlantSupport.cattail(plant, "minecraft:dirt", "minecraft:water[level=1]", true));
        Assert.assertTrue(
            ModernAquaticPlantSupport.cattail("regions_unexplored:cattail[half=upper]", plant, "minecraft:air", false));
    }

    @Test
    public void floweringLilysSubmergedSourceRuleIsKeptExplicit() {
        Assert.assertTrue(
            ModernAquaticPlantSupport.floweringLily("minecraft:water[level=0]", "minecraft:water[level=0]"));
        Assert.assertTrue(ModernAquaticPlantSupport.floweringLily("minecraft:ice", "minecraft:air"));
        Assert.assertFalse(ModernAquaticPlantSupport.floweringLily("minecraft:stone", "minecraft:air"));
    }
}
