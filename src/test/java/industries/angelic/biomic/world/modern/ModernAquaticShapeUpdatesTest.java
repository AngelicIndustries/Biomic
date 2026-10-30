package industries.angelic.biomic.world.modern;

import org.junit.Assert;
import org.junit.Test;

public class ModernAquaticShapeUpdatesTest {

    @Test
    public void replacingEitherHalfRestoresItsPartnerToWater() {
        String lower = "minecraft:tall_seagrass[half=lower]", upper = "minecraft:tall_seagrass[half=upper]";
        Assert.assertFalse(ModernAquaticShapeUpdates.brokenByNeighbor(lower, upper, true));
        Assert.assertFalse(ModernAquaticShapeUpdates.brokenByNeighbor(upper, lower, false));
        for (String replacement : new String[] { "minecraft:seagrass", "minecraft:kelp[age=0]", "minecraft:stone",
            "minecraft:water[level=0]", "minecraft:air" }) {
            Assert.assertTrue(ModernAquaticShapeUpdates.brokenByNeighbor(upper, replacement, false));
            Assert.assertTrue(ModernAquaticShapeUpdates.brokenByNeighbor(lower, replacement, true));
            Assert.assertFalse(ModernAquaticShapeUpdates.brokenByNeighbor(replacement, upper, true));
        }
    }
}
