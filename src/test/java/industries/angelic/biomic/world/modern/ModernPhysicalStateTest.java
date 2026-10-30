package industries.angelic.biomic.world.modern;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.biomic.content.RuDecorationPaletteMetadata;

/** Pure metadata contract. The actual Forge block conversion is checked by audit_hosted_preview.py. */
public class ModernPhysicalStateTest {

    @Test
    public void importedDoublePlantsPreserveBothHalves() {
        String id = "regions_unexplored:maple_shrub";
        Assert.assertTrue(RuDecorationPaletteMetadata.isDoublePlant(id));
        for (String half : new String[] { "lower", "upper" }) {
            int metadata = RuDecorationPaletteMetadata.doublePlantHalfMetadata(half);
            Assert.assertEquals("upper".equals(half) ? 8 : 0, metadata);
            Assert.assertEquals(id + "[half=" + half + ']', RuDecorationPaletteMetadata.doublePlantState(id, metadata));
        }
        Assert.assertEquals(0, RuDecorationPaletteMetadata.doublePlantHalfMetadata(null));
    }
}
