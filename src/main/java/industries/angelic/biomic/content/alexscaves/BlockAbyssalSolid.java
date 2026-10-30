package industries.angelic.biomic.content.alexscaves;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public final class BlockAbyssalSolid extends Block {

    public BlockAbyssalSolid(String name, float hardness, float resistance) {
        super(Material.rock);
        setBlockName("biomic.alexs_abyssal_" + name);
        setBlockTextureName("biomic:alexscaves/" + name);
        setHardness(hardness);
        setResistance(resistance);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", 0);
    }
}
