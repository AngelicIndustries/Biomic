package industries.angelic.biomic.content.gardens;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Places the authored two-block flower atomically; the upper half is never exposed as an item variant. */
public final class ItemBlockTallBlistercrown extends ItemBlock {

    static final int INVENTORY_ICON_METADATA = 8;

    public ItemBlockTallBlistercrown(Block block) {
        super(block);
    }

    /** The pinned source item model uses the top-half flower texture. */
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int metadata) {
        return field_150939_a.getIcon(2, INVENTORY_ICON_METADATA);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        Block selected = world.getBlock(x, y, z);
        if (!selected.isReplaceable(world, x, y, z)) {
            switch (side) {
                case 0 -> --y;
                case 1 -> ++y;
                case 2 -> --z;
                case 3 -> ++z;
                case 4 -> --x;
                case 5 -> ++x;
                default -> {
                    return false;
                }
            }
        }
        if (y < 1 || y + 1 >= world.getHeight()
            || !world.isAirBlock(x, y, z)
            || !world.isAirBlock(x, y + 1, z)
            || !GardensSupport.nylium(world.getBlock(x, y - 1, z), world.getBlockMetadata(x, y - 1, z))
            || !player.canPlayerEdit(x, y, z, side, stack)
            || !player.canPlayerEdit(x, y + 1, z, side, stack)) return false;
        if (!world.setBlock(x, y, z, field_150939_a, 0, 2)) return false;
        if (!world.setBlock(x, y + 1, z, field_150939_a, 8, 3)) {
            world.setBlockToAir(x, y, z);
            return false;
        }
        if (!player.capabilities.isCreativeMode) --stack.stackSize;
        return true;
    }
}
