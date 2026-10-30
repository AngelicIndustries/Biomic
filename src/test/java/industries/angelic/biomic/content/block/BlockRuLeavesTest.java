package industries.angelic.biomic.content.block;

import org.junit.Assert;
import org.junit.Test;

public class BlockRuLeavesTest {

    @Test
    public void coneSelectionIsStableAndNotBiomeDependent() {
        // Both reported boreal areas use the same position-only source model selection.
        for (int[] center : new int[][] { { -5088, -20256 }, { -2240, 3808 } }) {
            int total = 0, cones = 0;
            for (int x = center[0] - 12; x <= center[0] + 12; x++) {
                for (int z = center[1] - 12; z <= center[1] + 12; z++) {
                    for (int y = 80; y < 160; y++) {
                        boolean selected = BlockRuLeaves.hasCones(x, y, z);
                        Assert.assertEquals(selected, BlockRuLeaves.hasCones(x, y, z));
                        if (selected) cones++;
                        total++;
                    }
                }
            }
            Assert.assertEquals(1.0 / 7, (double) cones / total, .008);
        }
    }
}
