package industries.angelic.biomic.content.block;

import org.junit.Assert;
import org.junit.Test;

public class BlockCloverTest {

    @Test
    public void lowMetadataBitsRepresentOneThroughFourCloverPatches() {
        for (int rotation = 0; rotation < 4; rotation++) {
            for (int amount = 1; amount <= 4; amount++) {
                int metadata = rotation * 4 + amount - 1;
                Assert.assertEquals(amount, BlockClover.getPatchCount(metadata));
            }
        }
    }
}
