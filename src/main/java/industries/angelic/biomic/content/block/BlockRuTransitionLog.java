package industries.angelic.biomic.content.block;

import net.minecraft.block.BlockLog;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.content.BiomicBlocks;

/** Pine bark transition used where RU's stripped lower trunk becomes an unstripped crown trunk. */
public final class BlockRuTransitionLog extends BlockLog {

    private IIcon sideIcon;
    private IIcon strippedEndIcon;
    private IIcon barkEndIcon;

    public BlockRuTransitionLog() {
        setBlockName(Biomic.MODID + ".pine_transition_log");
        setHardness(2.0F);
        setStepSound(soundTypeWood);
        setCreativeTab(BiomicBlocks.CREATIVE_TAB);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        sideIcon = register.registerIcon(Biomic.MODID + ":pine_log_transition");
        strippedEndIcon = register.registerIcon(Biomic.MODID + ":stripped_pine_log_top");
        barkEndIcon = register.registerIcon(Biomic.MODID + ":pine_log_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        int orientation = metadata & 12;
        if (orientation == 0) {
            if (side == 0) return strippedEndIcon;
            if (side == 1) return barkEndIcon;
        } else if (orientation == 4) {
            if (side == 4) return strippedEndIcon;
            if (side == 5) return barkEndIcon;
        } else if (orientation == 8) {
            if (side == 2) return strippedEndIcon;
            if (side == 3) return barkEndIcon;
        }
        return sideIcon;
    }

    @Override
    public int damageDropped(int metadata) {
        return 0;
    }
}
