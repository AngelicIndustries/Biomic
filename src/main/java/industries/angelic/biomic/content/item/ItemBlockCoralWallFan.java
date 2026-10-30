package industries.angelic.biomic.content.item;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import industries.angelic.biomic.content.BiomicCoralWallFans;
import industries.angelic.biomic.content.block.BlockCoralWallFan;

/** Placement consumes actual source water, never manufactures it from an inventory variant. */
public final class ItemBlockCoralWallFan extends ItemBlock {

    private final BlockCoralWallFan fan;

    public ItemBlockCoralWallFan(Block block) {
        super(block);
        if (!(block instanceof BlockCoralWallFan value)) throw new IllegalArgumentException("Wall fan required");
        fan = value;
        setHasSubtypes(true);
    }

    @Override
    public int getMetadata(int damage) {
        return damage & 4;
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName(stack) + ((stack.getItemDamage() & 4) != 0 ? ".dead" : "");
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hx, float hy, float hz, int metadata) {
        if (metadata != 0 && metadata != 4) return false;
        return place(fan.species(), (metadata & 4) != 0, stack, player, world, x, y, z, side);
    }

    public static boolean place(int species, boolean dead, ItemStack stack, EntityPlayer player, World world, int x,
        int y, int z, int side) {
        if (!world.blockExists(x, y, z) || side < 2 || side > 5) return false;
        Block old = world.getBlock(x, y, z);
        boolean wet = (old == Blocks.water || old == Blocks.flowing_water) && world.getBlockMetadata(x, y, z) == 0;
        if (wet && world.provider.isHellWorld || !wet && old != Blocks.air) return false;
        int meta = BlockCoralWallFan.facingMetadata(ForgeDirection.getOrientation(side)) | (dead ? 4 : 0);
        var installed = BiomicCoralWallFans.get(species, wet);
        if (!installed.supported(world, x, y, z, meta) || !world.setBlock(x, y, z, installed, meta, 3)) return false;
        installed.onBlockPlacedBy(world, x, y, z, player, stack);
        return true;
    }
}
