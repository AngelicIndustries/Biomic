package industries.angelic.biomic.content.item;

import static org.junit.Assert.*;

import org.junit.Test;

public class CoralWallFanItemTest {

    @Test
    public void sharedFloorItemKeepsSpeciesAndDeathForEveryHorizontalWall() {
        for (int metadata = 0; metadata < 10; metadata++) for (int side = 2; side <= 5; side++) {
            var result = ItemBlockAquaticVariant.wallPlacement(metadata, side);
            assertNotNull(result);
            assertEquals(metadata % 5, result.species());
            assertEquals(metadata >= 5, result.dead());
        }
    }

    @Test
    public void floorSidesRemainLegacyAndInvalidInventoryDoesNotAliasSpecies() {
        for (int metadata = 0; metadata < 10; metadata++) {
            assertNull(ItemBlockAquaticVariant.wallPlacement(metadata, 0));
            assertNull(ItemBlockAquaticVariant.wallPlacement(metadata, 1));
        }
        for (int metadata : new int[] { -1, 10, 15, 64 })
            assertNull(ItemBlockAquaticVariant.wallPlacement(metadata, 2));
        assertNull(ItemBlockAquaticVariant.wallPlacement(0, 6));
    }
}
