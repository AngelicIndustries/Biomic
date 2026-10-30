package industries.angelic.biomic.content.ports;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import industries.angelic.biomic.Biomic;

/** A plain ground block of the sea floor (coral sand, mud with pianguas). */
final class BlockPortGround extends Block {

    BlockPortGround(String name, String texture, Material material, SoundType sound) {
        super(material);
        setBlockName(Biomic.MODID + "." + name);
        setBlockTextureName(Biomic.MODID + ":" + texture);
        setHardness(0.5F);
        setStepSound(sound);
        setHarvestLevel("shovel", 0);
    }
}
