package industries.angelic.biomic.unused.tartarus;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockNetherFungus;

/** Source-only counterpart; EFR's metadata, bonemeal, growth, support, sound and renderer are inherited. */
public final class BlockAuthoredTartarusFungus extends BlockNetherFungus {

    public BlockAuthoredTartarusFungus() {
        setTickRandomly(false);
        setCreativeTab(null);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {}

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return ModBlocks.NETHER_FUNGUS.get()
            .getItemDropped(metadata, random, fortune);
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        return ModBlocks.NETHER_FUNGUS.get()
            .getPickBlock(target, world, x, y, z, player);
    }
}
