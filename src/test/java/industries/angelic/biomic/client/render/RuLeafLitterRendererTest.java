package industries.angelic.biomic.client.render;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.biomic.content.RuDecorationPaletteMetadata;

public class RuLeafLitterRendererTest {

    @Test
    public void allSixteenVariantsRetainAmountAndRotation() {
        String[] directions = { "north", "east", "south", "west" };
        for (int facing = 0; facing < 4; facing++) for (int amount = 1; amount <= 4; amount++) {
            int metadata = RuDecorationPaletteMetadata.leafLitterMetadata(amount, facing);
            Assert.assertEquals(amount - 1, metadata & 3);
            Assert.assertEquals(facing, metadata >> 2);
            Assert.assertEquals(
                "regions_unexplored:maple_leaf_litter[facing=" + directions[facing] + ",flower_amount=" + amount + "]",
                RuDecorationPaletteMetadata.leafLitterState("regions_unexplored:maple_leaf_litter", metadata));
            Assert.assertEquals(amount, Integer.bitCount(RuLeafLitterRenderer.quadrantMask(amount)));
        }
    }
}
