package industries.angelic.biomic.world.modern;

import org.junit.Assert;
import org.junit.Test;

public class ModernBranchStateTest {

    @Test
    public void facingOnlyBranchDoesNotAcquireLogAxis() {
        String state = "regions_unexplored:cypress_branch[facing=north]";
        Assert.assertEquals(state, ModernFeatureEngine.tryProperty(state, "axis", "z"));
        Assert.assertEquals(
            "regions_unexplored:cypress_branch[facing=west]",
            ModernFeatureEngine.tryProperty(state, "facing", "west"));
    }

    @Test
    public void logModeDoesNotAcquireBranchFacing() {
        String state = "minecraft:oak_log[axis=y]";
        Assert.assertEquals(state, ModernFeatureEngine.tryProperty(state, "facing", "north"));
        Assert.assertEquals("minecraft:oak_log[axis=z]", ModernFeatureEngine.tryProperty(state, "axis", "z"));
        Assert.assertEquals("minecraft:stone", ModernFeatureEngine.tryProperty("minecraft:stone", "axis", "x"));
    }
}
