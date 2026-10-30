package industries.angelic.biomic.content.item;

import static org.junit.Assert.*;

import org.junit.Test;

import industries.angelic.biomic.content.BiomicWetSculkBlocks;
import industries.angelic.biomic.content.block.BlockSculkVein;

public class ItemBlockWetSculkVeinTest {

    @Test
    public void everyNonemptyFaceSetConsumesExactlySourceWaterAndNeverCreatesIt() {
        for (int faces = 1; faces < 64; faces++) {
            int metadata = BlockSculkVein.horizontalMetadata(faces);
            assertTrue(ItemBlockWetSculkVein.sourcePlacement(false, true, 0, metadata, faces));
            assertFalse(ItemBlockWetSculkVein.sourcePlacement(true, true, 0, metadata, faces));
            assertFalse(ItemBlockWetSculkVein.sourcePlacement(false, false, 0, metadata, faces));
            assertFalse(ItemBlockWetSculkVein.sourcePlacement(false, true, 1, metadata, faces));
        }
        assertFalse(ItemBlockWetSculkVein.sourcePlacement(false, true, 0, 0, 0));
        assertFalse(ItemBlockWetSculkVein.sourcePlacement(false, true, 0, 16, 63));
        assertFalse(ItemBlockWetSculkVein.sourcePlacement(false, true, 0, -1, 63));
    }

    @Test
    public void commandItemRetainsNativeHorizontalMetadataInsteadOfCreatingDefaultFaces() {
        for (boolean up : new boolean[] { false, true }) for (boolean down : new boolean[] { false, true }) {
            var item = new ItemBlockWetSculkVein(BiomicWetSculkBlocks.wetVein(up, down));
            for (int metadata = 0; metadata < 16; metadata++) assertEquals(metadata, item.getMetadata(metadata));
            assertEquals(16, item.getMetadata(16)); // Placement rejects, rather than truncating untrusted damage.
            assertTrue(item.getHasSubtypes());
        }
    }
}
