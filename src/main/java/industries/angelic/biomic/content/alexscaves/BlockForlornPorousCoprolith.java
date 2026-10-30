package industries.angelic.biomic.content.alexscaves;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import industries.angelic.biomic.client.BiomicRenderIds;

/** Non-occluding intrinsic shell used by the peering-coprolith landmark. */
public final class BlockForlornPorousCoprolith extends Block {

    private final boolean peering;
    private IIcon[] outside;

    BlockForlornPorousCoprolith(String name, boolean peering) {
        super(Material.rock);
        setBlockName("biomic.alexs_forlorn_" + name);
        this.peering = peering;
        setHardness(1.75F);
        setResistance(4F);
        setStepSound(soundTypeStone);
        setHarvestLevel("pickaxe", 0);
    }

    @Override
    public void registerBlockIcons(IIconRegister icons) {
        outside = new IIcon[] { icons.registerIcon("biomic:alexscaves/porous_coprolith_0"),
            icons.registerIcon("biomic:alexscaves/porous_coprolith_1") };
        blockIcon = outside[0];
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public int getRenderBlockPass() {
        return peering ? 1 : 0;
    }

    @Override
    public int getRenderType() {
        return BiomicRenderIds.FORLORN_COPROLITH;
    }
}
