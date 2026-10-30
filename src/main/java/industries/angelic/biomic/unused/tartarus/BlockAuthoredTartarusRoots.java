package industries.angelic.biomic.unused.tartarus;

import java.util.Random;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockNetherRoots;

/** Literal Tartarus raw placements use modern neighbor physics, not 1.7's autonomous bush decay. */
public final class BlockAuthoredTartarusRoots extends BlockNetherRoots {

    public BlockAuthoredTartarusRoots() {
        setTickRandomly(false);
        setCreativeTab(null);
    }

    /** Modern RootsBlock has no random/scheduled tick callback; inherited neighbor support stays exact. */
    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {}

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return ModBlocks.NETHER_ROOTS.get()
            .getItemDropped(metadata, random, fortune);
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        return ModBlocks.NETHER_ROOTS.get()
            .getPickBlock(target, world, x, y, z, player);
    }
}
