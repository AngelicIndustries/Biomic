package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;

import industries.angelic.biomic.Biomic;

/** Modern all-sided mushroom-stem texture used by white cap spots, distinct from the real 1.7 stem. */
public final class BlockMushroomFlesh extends Block {

    public BlockMushroomFlesh() {
        super(Material.wood);
        setHardness(0.2F);
        setStepSound(soundTypeWood);
        setBlockName(Biomic.MODID + ".mushroom_flesh");
        setBlockTextureName(Biomic.MODID + ":mushroom_stem");
    }

    @Override
    protected boolean canSilkHarvest() {
        return true;
    }

    @Override
    public int quantityDropped(Random random) {
        return Math.max(0, random.nextInt(10) - 7);
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return Blocks.brown_mushroom_block.getItemDropped(metadata, random, fortune);
    }
}
