package industries.angelic.biomic.content.block;

import net.minecraft.block.BlockFence;
import net.minecraft.block.material.Material;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** One wood's fence, faced with its planks; it joins every wooden fence and gate (MixinBlockFenceConnect). */
public final class BlockWoodFence extends BlockFence {

    public BlockWoodFence(String name, String planksTexture) {
        super(Biomic.MODID + ":" + planksTexture, Material.wood);
        setBlockName(Biomic.MODID + "." + name);
        setHardness(2.0F);
        setResistance(5.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }
}
