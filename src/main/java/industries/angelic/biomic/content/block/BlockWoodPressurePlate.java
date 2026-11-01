package industries.angelic.biomic.content.block;

import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.material.Material;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** One wood's pressure plate (every entity presses it, as vanilla's wooden one), faced with its planks. */
public final class BlockWoodPressurePlate extends BlockPressurePlate {

    public BlockWoodPressurePlate(String name, String planksTexture) {
        super(Biomic.MODID + ":" + planksTexture, Material.wood, Sensitivity.everything);
        setBlockName(Biomic.MODID + "." + name);
        setHardness(0.5F);
        setStepSound(soundTypeWood);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }
}
