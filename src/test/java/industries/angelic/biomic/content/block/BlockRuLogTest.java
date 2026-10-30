package industries.angelic.biomic.content.block;

import org.junit.Assert;
import org.junit.Test;

public class BlockRuLogTest {

    @Test
    public void silverBirchBaseBarkIsOnlyUsedByUprightLogs() {
        Assert.assertEquals(1, BlockRuLog.textureVariant(1, 2, true));
        Assert.assertEquals(0, BlockRuLog.textureVariant(0, 2, true));
        // Both values of the stored is_base flag render identically for horizontal logs.
        for (int axis : new int[] { 4, 8, 12 }) {
            Assert.assertEquals(0, BlockRuLog.textureVariant(axis, 2, true));
            Assert.assertEquals(0, BlockRuLog.textureVariant(axis | 1, 2, true));
        }
    }

    @Test
    public void bambooAndOrdinaryVariantLogsAreNotAffected() {
        for (int metadata : new int[] { 1, 5, 9, 13 }) {
            Assert.assertEquals(1, BlockRuLog.textureVariant(metadata, 2, false));
        }
        for (int metadata = 0; metadata < 16; metadata++) {
            Assert.assertEquals(0, BlockRuLog.textureVariant(metadata, 1, false));
        }
    }
}
