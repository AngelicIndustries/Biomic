package industries.angelic.biomic.world.terrain;

import org.junit.Assert;
import org.junit.Test;

public class OrigenAquaticProjectionTest {

    @Test
    public void squeezedLandPairsBecomeTheMatchingShortPlantOnlyWhenAliased() {
        for (String plant : new String[] { "tall_grass", "large_fern" }) {
            int collapsed = 0, retained = 0;
            for (int lower = -63; lower < 318; lower++) {
                String bottom = OrigenFeatureRuntime
                    .projectedLandPlantState("minecraft:" + plant + "[half=lower]", lower);
                String top = OrigenFeatureRuntime
                    .projectedLandPlantState("minecraft:" + plant + "[half=upper]", lower + 1);
                Assert.assertEquals(bottom, top);
                if (OrigenVerticalProfileSampler.toBiomicBlockY(lower)
                    == OrigenVerticalProfileSampler.toBiomicBlockY(lower + 1)) {
                    collapsed++;
                    Assert.assertEquals(plant.equals("tall_grass") ? "minecraft:grass" : "minecraft:fern", bottom);
                } else {
                    retained++;
                    Assert.assertNull(bottom);
                }
            }
            Assert.assertTrue(collapsed > 0);
            Assert.assertTrue(retained > collapsed);
        }
        Assert.assertNull(OrigenFeatureRuntime.projectedLandPlantState("minecraft:rose_bush[half=lower]", 100));
        Assert.assertNull(OrigenFeatureRuntime.projectedLandPlantState("minecraft:grass", 100));
        Assert.assertNull(OrigenFeatureRuntime.projectedLandPlantState("minecraft:tall_seagrass[half=lower]", 100));
    }

    @Test
    public void onlyAliasedSeagrassPairsBecomeOneValidShortPlant() {
        int collapsed = 0, retained = 0;
        for (int lower = -63; lower < 318; lower++) {
            String bottom = OrigenFeatureRuntime.projectedAquaticState("minecraft:tall_seagrass[half=lower]", lower);
            String top = OrigenFeatureRuntime.projectedAquaticState("minecraft:tall_seagrass[half=upper]", lower + 1);
            Assert.assertEquals(bottom, top);
            if (bottom == null) retained++;
            else {
                collapsed++;
                Assert.assertEquals("minecraft:seagrass", bottom);
            }
        }
        Assert.assertTrue(collapsed > 0);
        Assert.assertTrue(retained > collapsed);
        Assert.assertNull(OrigenFeatureRuntime.projectedAquaticState("minecraft:seagrass", 63));
        Assert.assertNull(OrigenFeatureRuntime.projectedAquaticState("minecraft:large_fern[half=upper]", 64));
    }
}
