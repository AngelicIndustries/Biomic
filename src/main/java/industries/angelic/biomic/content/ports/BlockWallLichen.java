package industries.angelic.biomic.content.ports;

import java.util.Random;

import net.minecraft.block.BlockVine;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;

/**
 * Plain lichen on cave-mouth walls (TFC's cobblestone and elegant sunburst lichen): faces as a vine's, never spreads.
 */
public final class BlockWallLichen extends BlockVine {

    private final String texture;
    @SideOnly(Side.CLIENT)
    private IIcon icon;

    BlockWallLichen(String name, String texture) {
        this.texture = texture;
        setBlockName(Biomic.MODID + "." + name);
        setHardness(0.2F);
        setStepSound(soundTypeGrass);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {}

    @Override
    @SideOnly(Side.CLIENT)
    public int getBlockColor() {
        return 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getRenderColor(int metadata) {
        return 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess world, int x, int y, int z) {
        return 0xFFFFFF;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return icon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        icon = register.registerIcon(Biomic.MODID + ":" + texture);
    }
}
