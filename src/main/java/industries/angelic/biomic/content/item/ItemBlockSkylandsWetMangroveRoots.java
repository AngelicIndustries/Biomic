package industries.angelic.biomic.content.item;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

/** Even command-obtained wet items cannot create water: placement consumes an existing source-water cell. */
public final class ItemBlockSkylandsWetMangroveRoots extends ItemBlock {

    public ItemBlockSkylandsWetMangroveRoots(Block block) {
        super(block);
    }

    static boolean sourcePlacement(boolean hellWorld, boolean sourceWater, int metadata) {
        return !hellWorld && sourceWater && metadata == 0;
    }

    @Override
    public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ, int metadata) {
        if (metadata != 0 || !world.blockExists(x, y, z)) return false;
        Block existing = world.getBlock(x, y, z);
        if (!sourcePlacement(
            world.provider.isHellWorld,
            existing == Blocks.water || existing == Blocks.flowing_water,
            world.getBlockMetadata(x, y, z))) return false;
        return super.placeBlockAt(stack, player, world, x, y, z, side, hitX, hitY, hitZ, 0);
    }
}
