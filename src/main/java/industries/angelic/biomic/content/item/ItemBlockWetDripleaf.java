package industries.angelic.biomic.content.item;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/**
 * Wet companions are publication-only; command-obtained items cannot manufacture source water.
 * Players use the normal dry dripleaf item and the exact bucket transition instead.
 */
public final class ItemBlockWetDripleaf extends ItemBlock {

    public ItemBlockWetDripleaf(Block block) {
        super(block);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ, int metadata) {
        return false;
    }
}
