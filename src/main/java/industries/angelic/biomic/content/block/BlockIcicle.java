package industries.angelic.biomic.content.block;

import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.blocks.BlockPointedDripstone;
import industries.angelic.biomic.Biomic;

/** Regions Unexplored icicle using EFR's ten-state pointed-dripstone geometry and metadata. */
public final class BlockIcicle extends BlockPointedDripstone {

    public BlockIcicle() {
        setBlockName(Biomic.MODID + ".icicle");
        setBlockTextureName(Biomic.MODID + ":ru_decor/icicle");
    }

    // EFR supplies a separate item-atlas "pointed_dripstone" icon. Our item uses
    // RU's upward tip from the block atlas; do not inherit that unrelated sprite.
    @Override
    public String getItemIconName() {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return super.getIcon(side, 5);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        return super.getIcon(side, world.getBlockMetadata(x, y, z));
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return false;
    }
}
