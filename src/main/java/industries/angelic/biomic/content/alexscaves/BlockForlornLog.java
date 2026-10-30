package industries.angelic.biomic.content.alexscaves;

import net.minecraft.block.BlockLog;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Single-species orientable thornwood without a dependency on BiomicBlocks static initialization. */
final class BlockForlornLog extends BlockLog {

    private final String sideName, topName;
    private IIcon sideIcon, topIcon;

    BlockForlornLog(String name, String sideName, String topName) {
        this.sideName = sideName;
        this.topName = topName;
        setBlockName("biomic.alexs_forlorn_" + name);
        setHardness(2);
        setResistance(2);
        setStepSound(soundTypeWood);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        sideIcon = register.registerIcon("biomic:" + sideName);
        topIcon = register.registerIcon("biomic:" + topName);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        int axis = metadata & 12;
        boolean end = axis == 0 && (side == 0 || side == 1) || axis == 4 && (side == 4 || side == 5)
            || axis == 8 && (side == 2 || side == 3);
        return end ? topIcon : sideIcon;
    }

    @Override
    public int damageDropped(int metadata) {
        return 0;
    }
}
