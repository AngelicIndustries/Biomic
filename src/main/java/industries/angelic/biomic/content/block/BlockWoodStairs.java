package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** One wood's stairs, faced and sounding like its planks. */
public final class BlockWoodStairs extends BlockStairs {

    public BlockWoodStairs(Block planks, String name) {
        super(planks, 0);
        setBlockName(Biomic.MODID + "." + name);
        setHarvestLevel("axe", 0);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }
}
