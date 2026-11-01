package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** One wood's planks (Regions Unexplored's art): vanilla planks' hardness, resistance and sound. */
public final class BlockWoodPlanks extends Block {

    public BlockWoodPlanks(String name, String texture) {
        super(Material.wood);
        setBlockName(Biomic.MODID + "." + name);
        setBlockTextureName(Biomic.MODID + ":" + texture);
        setHardness(2.0F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }
}
