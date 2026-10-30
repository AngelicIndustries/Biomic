package industries.angelic.biomic.content.item;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import ganymedes01.etfuturum.tileentities.TileEntityGlowLichen;

/** Even command-obtained wet items consume existing source water; item metadata cannot invent faces/water. */
public final class ItemBlockSkylandsWetGlowLichen extends ItemBlock {

    public ItemBlockSkylandsWetGlowLichen(Block block) {
        super(block);
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hx, float hy, float hz, int metadata) {
        if (world.provider.isHellWorld || side < 0 || side >= 6 || !world.blockExists(x, y, z)) return false;
        Block before = world.getBlock(x, y, z);
        if ((before != Blocks.water && before != Blocks.flowing_water) || world.getBlockMetadata(x, y, z) != 0)
            return false;
        var support = ForgeDirection.getOrientation(side)
            .getOpposite();
        if (!world.isSideSolid(x + support.offsetX, y + support.offsetY, z + support.offsetZ, support.getOpposite()))
            return false;
        if (!world.setBlock(x, y, z, field_150939_a, 0, 2)) return false;
        var tile = world.getTileEntity(x, y, z);
        if (tile == null || tile.getClass() != TileEntityGlowLichen.class) {
            if (!world.setBlock(x, y, z, before, 0, 3))
                throw new IllegalStateException("Wet lichen item rollback failed");
            return false;
        }
        var tag = new NBTTagCompound();
        tag.setInteger("x", x);
        tag.setInteger("y", y);
        tag.setInteger("z", z);
        tag.setInteger("State", 1 << support.ordinal());
        tile.readFromNBT(tag);
        tile.markDirty();
        world.markBlockForUpdate(x, y, z);
        world.notifyBlocksOfNeighborChange(x, y, z, field_150939_a);
        return true;
    }
}
