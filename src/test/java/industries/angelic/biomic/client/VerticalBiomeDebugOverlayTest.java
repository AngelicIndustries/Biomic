package industries.angelic.biomic.client;

import org.junit.Assert;
import org.junit.Test;

public class VerticalBiomeDebugOverlayTest {

    @Test
    public void replacesOnlyTheBiomeFieldInTheForgeDebugLine() {
        String original = "lc: 79 b: Murky Marshlands bl: 0 sl: 0 rl: 0";

        Assert.assertEquals(
            "lc: 79 b: Vine Vault bl: 0 sl: 0 rl: 0",
            VerticalBiomeDebugOverlay.replaceBiomeName(original, "Vine Vault"));
        Assert.assertSame(original, VerticalBiomeDebugOverlay.replaceBiomeName(original, null));
        Assert.assertEquals("unrelated", VerticalBiomeDebugOverlay.replaceBiomeName("unrelated", "Vine Vault"));
    }
}
