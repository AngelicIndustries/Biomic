package industries.angelic.biomic.content.block;

import net.minecraft.block.BlockLog;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;
import industries.angelic.biomic.content.BiomicBlocks;

/** RU's orientable eucalyptus log with its coordinate-tinted rainbow bark overlay. */
public final class BlockEucalyptusLog extends BlockLog {

    private IIcon sideIcon;
    private IIcon topIcon;
    private IIcon overlayIcon;

    public BlockEucalyptusLog() {
        setBlockName(Biomic.MODID + ".eucalyptus_log");
        setHardness(2.0F);
        setStepSound(soundTypeWood);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        sideIcon = register.registerIcon(Biomic.MODID + ":eucalyptus_log");
        topIcon = register.registerIcon(Biomic.MODID + ":eucalyptus_log_top");
        overlayIcon = register.registerIcon(Biomic.MODID + ":eucalyptus_log_overlay");
        blockIcon = sideIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        int orientation = metadata & 12;
        boolean end = orientation == 0 && (side == 0 || side == 1) || orientation == 4 && (side == 4 || side == 5)
            || orientation == 8 && (side == 2 || side == 3);
        return end ? topIcon : sideIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getOverlayIcon() {
        return overlayIcon;
    }

    @Override
    public int getRenderType() {
        return BiomicRenderIds.EUCALYPTUS_LOG;
    }

    @Override
    public int damageDropped(int metadata) {
        return 0;
    }
}
