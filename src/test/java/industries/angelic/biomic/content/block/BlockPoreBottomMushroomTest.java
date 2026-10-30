package industries.angelic.biomic.content.block;

import static org.junit.Assert.*;

import org.junit.Test;

import industries.angelic.biomic.content.item.ItemBlockPoreBottomMushroom;

public class BlockPoreBottomMushroomTest {

    @Test
    public void nativeCubeRetainsExactBottomPoreAndFiveCapFacesForBothColors() {
        var block = new BlockPoreBottomMushroom();
        assertTrue(block.isOpaqueCube());
        assertTrue(block.renderAsNormalBlock());
        for (int kind = 0; kind < 2; kind++) {
            assertEquals(kind, BlockPoreBottomMushroom.kind(kind));
            for (int side = 0; side < 6; side++)
                assertEquals(side == 0 ? 0 : 14, BlockPoreBottomMushroom.textureMetadata(side));
        }
        for (int invalid : new int[] { -1, 2, 15 })
            assertThrows(IllegalArgumentException.class, () -> BlockPoreBottomMushroom.kind(invalid));
        assertThrows(IllegalArgumentException.class, () -> BlockPoreBottomMushroom.textureMetadata(6));
    }

    @Test
    public void ordinaryMushroomDropsAreDamageZeroWhileSilkAndItemPlacementRetainColor() {
        var block = new BlockPoreBottomMushroom();
        var item = new ItemBlockPoreBottomMushroom(block);
        for (int kind = 0; kind < 2; kind++) {
            assertEquals(0, block.damageDropped(kind));
            // Detached block has no global item registration. Bind this fixture's subtype item before
            // Forge's ItemStack damage getter delegates to it; native registration is tested at startup.
            var silk = block.createStackedBlock(kind);
            silk.func_150996_a(item);
            assertEquals(kind, silk.getItemDamage());
            assertEquals(kind, item.getMetadata(kind));
        }
        assertThrows(IllegalArgumentException.class, () -> item.getMetadata(2));
    }
}
