package industries.angelic.biomic.content.ports;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.material.Material;

import industries.angelic.biomic.Biomic;

/** Reindeer lichen in clumps on cold cave-mouth floors (TFC's): a small cross on rock, gravel or soil. */
public final class BlockFloorLichen extends BlockBush {

    BlockFloorLichen(String name, String texture) {
        super(Material.plants);
        setBlockName(Biomic.MODID + "." + name);
        setBlockTextureName(Biomic.MODID + ":" + texture);
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        float f = 0.35F;
        setBlockBounds(0.5F - f, 0.0F, 0.5F - f, 0.5F + f, 0.4F, 0.5F + f);
    }

    @Override
    protected boolean canPlaceBlockOn(Block ground) {
        Material material = ground.getMaterial();
        return material == Material.rock || material == Material.ground
            || material == Material.grass
            || material == Material.sand
            || material == Material.craftedSnow;
    }
}
