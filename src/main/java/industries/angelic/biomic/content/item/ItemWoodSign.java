package industries.angelic.biomic.content.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;
import industries.angelic.biomic.content.block.BlockWoodSign;

/**
 * One wood's sign item, named as its sign: places its standing or wall sign the way vanilla's sign does, and opens its
 * editor. The placer is sent the sign first: vanilla opens the editor before the new block reaches the client, which
 * then edits a stand-in vanilla sign and shows it in oak.
 */
public final class ItemWoodSign extends Item {

    private final BlockWoodSign standing;
    private final BlockWoodSign wall;

    public ItemWoodSign(String texture, BlockWoodSign standing, BlockWoodSign wall) {
        this.standing = standing;
        this.wall = wall;
        maxStackSize = 16;
        setTextureName(Biomic.MODID + ":" + texture);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (side == 0 || !world.getBlock(x, y, z)
            .getMaterial()
            .isSolid()) return false;
        if (side == 1) y++;
        if (side == 2) z--;
        if (side == 3) z++;
        if (side == 4) x--;
        if (side == 5) x++;
        if (!player.canPlayerEdit(x, y, z, side, stack) || !standing.canPlaceBlockAt(world, x, y, z)) return false;
        if (world.isRemote) return true;
        if (side == 1) world.setBlock(
            x,
            y,
            z,
            standing,
            MathHelper.floor_double((player.rotationYaw + 180.0F) * 16.0F / 360.0F + 0.5D) & 15,
            3);
        else world.setBlock(x, y, z, wall, side, 3);
        stack.stackSize--;
        if (!(world.getTileEntity(x, y, z) instanceof TileEntitySign sign)) return true;
        if (player instanceof EntityPlayerMP placer)
            placer.playerNetServerHandler.sendPacket(new S23PacketBlockChange(x, y, z, world));
        player.func_146100_a(sign);
        return true;
    }

    @Override
    public String getUnlocalizedName() {
        return standing.getUnlocalizedName();
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return standing.getUnlocalizedName();
    }
}
