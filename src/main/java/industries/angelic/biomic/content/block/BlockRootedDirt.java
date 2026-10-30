package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import industries.angelic.biomic.Biomic;

/** Exact visual ground material for Origen's rooted-dirt palettes and azalea tunnels. */
public final class BlockRootedDirt extends Block {

    public BlockRootedDirt() {
        super(Material.ground);
        setBlockName(Biomic.MODID + ".rooted_dirt");
        setBlockTextureName(Biomic.MODID + ":rooted_dirt");
        setHardness(0.5F);
        setStepSound(soundTypeGravel);
        setHarvestLevel("shovel", 0);
    }
}
