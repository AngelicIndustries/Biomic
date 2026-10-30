package industries.angelic.biomic.content.item;

import static org.junit.Assert.*;

import org.junit.Test;

/** Command/creative item placement still consumes existing source water; no free fluid placement. */
public class ItemBlockSkylandsWetMangroveRootsTest {

    @Test
    public void onlyExistingNonHellSourceWaterCanBecomeWetRoots() {
        assertTrue(ItemBlockSkylandsWetMangroveRoots.sourcePlacement(false, true, 0));
        assertFalse(ItemBlockSkylandsWetMangroveRoots.sourcePlacement(true, true, 0));
        assertFalse(ItemBlockSkylandsWetMangroveRoots.sourcePlacement(false, false, 0));
        for (int metadata = 1; metadata < 16; metadata++)
            assertFalse(ItemBlockSkylandsWetMangroveRoots.sourcePlacement(false, true, metadata));
    }
}
