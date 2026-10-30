package industries.angelic.biomic.client.render;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import industries.angelic.biomic.content.gardens.BlockSoulSpore;
import industries.angelic.biomic.content.gardens.GardensContent;

public class GardensSoulSporeRendererTest {

    @Test
    public void ceilingModelReversesVerticalTextureCoordinatesOnly() {
        assertArrayEquals(new double[] { .125, .875 }, GardensSoulSporeRenderer.verticalUvs(.125, .875, false), 0);
        assertArrayEquals(new double[] { .875, .125 }, GardensSoulSporeRenderer.verticalUvs(.125, .875, true), 0);
    }

    @Test
    public void flipsOnlyTheDownwardStatesSelectedByPinnedBlockstates() {
        assertFalse(GardensContent.SOUL_SPORE.flipModel(BlockSoulSpore.DOWN));
        assertTrue(GardensContent.SOUL_SPORE.flipModel(BlockSoulSpore.DOWN | BlockSoulSpore.TOP));
        assertFalse(GardensContent.SOUL_SPORE.flipModel(BlockSoulSpore.TOP));
        assertTrue(GardensContent.GLOWING_SOUL_SPORE.flipModel(BlockSoulSpore.DOWN));
        assertFalse(GardensContent.GLOWING_SOUL_SPORE.flipModel(0));
    }
}
