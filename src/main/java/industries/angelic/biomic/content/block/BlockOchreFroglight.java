package industries.angelic.biomic.content.block;

import net.minecraft.block.BlockRotatedPillar;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;

/** Source SkyLands' luminous pillar; deliberately not a log (no wood/leaf-decay behaviour). */
public final class BlockOchreFroglight extends BlockRotatedPillar {

    public BlockOchreFroglight() {
        super(Material.rock);
        setBlockName(Biomic.MODID + ".ochre_froglight");
        setHardness(0.3F);
        setResistance(0.3F);
        setLightLevel(1.0F);
        setStepSound(soundTypeStone);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon(Biomic.MODID + ":ochre_froglight_side");
        field_150164_N = register.registerIcon(Biomic.MODID + ":ochre_froglight_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected IIcon getSideIcon(int variant) {
        return blockIcon;
    }

    @Override
    public int damageDropped(int metadata) {
        return 0;
    }
}
