package industries.angelic.biomic.content.alexscaves;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

/** Standalone intrinsic cube; intentionally does not initialize the broader Biomic block catalogue. */
final class BlockForlornSolid extends Block {

    BlockForlornSolid(String name, Material material, float hardness, float resistance) {
        super(material);
        setBlockName("biomic.alexs_forlorn_" + name);
        setBlockTextureName("biomic:alexscaves/" + (name.equals("guano_block") ? "guano" : name));
        setHardness(hardness);
        setResistance(resistance);
        setStepSound(material == Material.sand ? soundTypeSand : soundTypeStone);
        if (material == Material.rock) setHarvestLevel("pickaxe", 0);
    }
}
