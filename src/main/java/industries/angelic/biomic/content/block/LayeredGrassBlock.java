package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Icon contract shared by exact RU grass-over-substrate cube models. */
public interface LayeredGrassBlock {

    @SideOnly(Side.CLIENT)
    IIcon getBottomIcon();

    @SideOnly(Side.CLIENT)
    IIcon getGrassTopIcon();

    @SideOnly(Side.CLIENT)
    IIcon getGrassSideIcon();

    @SideOnly(Side.CLIENT)
    IIcon getGrassSideOverlayIcon();

    @SideOnly(Side.CLIENT)
    IIcon getSnowySideIcon();

    static boolean isSnowy(IBlockAccess world, int x, int y, int z) {
        Block block = world.getBlock(x, y + 1, z);
        Material material = block.getMaterial();
        return material == Material.snow || material == Material.craftedSnow;
    }
}
