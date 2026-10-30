package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** Soft white coastal stone adapted from Regions Unexplored's chalk material. */
public final class BlockChalk extends Block {

    public BlockChalk() {
        super(Material.rock);
        setBlockName(Biomic.MODID + ".chalk");
        setBlockTextureName(Biomic.MODID + ":chalk");
        setHardness(1.0F);
        setResistance(3.0F);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", 0);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }
}
