package industries.angelic.biomic.content.block;

import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;

import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** One wood's trapdoor, with RU's art. */
public final class BlockWoodTrapdoor extends BlockTrapDoor {

    public BlockWoodTrapdoor(String name, String texture) {
        super(Material.wood);
        setBlockName(Biomic.MODID + "." + name);
        setBlockTextureName(Biomic.MODID + ":" + texture);
        setHardness(3.0F);
        setStepSound(soundTypeWood);
        setHarvestLevel("axe", 0);
        disableStats();
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }
}
