package industries.angelic.biomic.content.item;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** One wood's door item, named as its door: places it the way vanilla's door item does. */
public final class ItemWoodDoor extends Item {

    private final Block door;

    public ItemWoodDoor(String texture, Block door) {
        this.door = door;
        setTextureName(Biomic.MODID + ":" + texture);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (side != 1) return false;
        y++;
        if (!player.canPlayerEdit(x, y, z, side, stack) || !player.canPlayerEdit(x, y + 1, z, side, stack)
            || !door.canPlaceBlockAt(world, x, y, z)) return false;
        int facing = MathHelper.floor_double((player.rotationYaw + 180.0F) * 4.0F / 360.0F - 0.5D) & 3;
        ItemDoor.placeDoorBlock(world, x, y, z, facing, door);
        stack.stackSize--;
        return true;
    }

    @Override
    public String getUnlocalizedName() {
        return door.getUnlocalizedName();
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return door.getUnlocalizedName();
    }
}
