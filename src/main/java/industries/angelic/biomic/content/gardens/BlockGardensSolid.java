package industries.angelic.biomic.content.gardens;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

final class BlockGardensSolid extends Block {

    BlockGardensSolid(String name, String texture, float hardness) {
        super(Material.wood);
        setBlockName("biomic." + name);
        setBlockTextureName("biomic:" + texture);
        setHardness(hardness);
        setStepSound(soundTypeGrass);
    }
}
