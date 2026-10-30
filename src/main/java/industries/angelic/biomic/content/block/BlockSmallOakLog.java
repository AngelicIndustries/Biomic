package industries.angelic.biomic.content.block;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import industries.angelic.biomic.client.BiomicRenderIds;

/** RU's half-width log. Retains the existing registry ID and vanilla log-axis metadata. */
public final class BlockSmallOakLog extends BlockRuLog {

    private final String leavesTexture;
    private net.minecraft.util.IIcon leavesIcon;

    public BlockSmallOakLog() {
        this("small_oak_log", "ru/small_oak_log", null);
    }

    public BlockSmallOakLog(String name, String texture, String leavesTexture) {
        super("imported_" + name, texture, texture);
        this.leavesTexture = leavesTexture;
        setLightOpacity(0);
        setBlockBoundsForItemRender();
    }

    @Override
    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public void registerBlockIcons(net.minecraft.client.renderer.texture.IIconRegister register) {
        super.registerBlockIcons(register);
        if (leavesTexture != null) leavesIcon = register.registerIcon("biomic:" + leavesTexture);
    }

    @cpw.mods.fml.relauncher.SideOnly(cpw.mods.fml.relauncher.Side.CLIENT)
    public net.minecraft.util.IIcon leavesIcon(int metadata) {
        return (metadata & 1) != 0 ? leavesIcon : null;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return false;
    }

    @Override
    public int getRenderType() {
        return BiomicRenderIds.SMALL_OAK_LOG;
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return false;
    }

    /** Bounds in model units, also used by the geometry regression tests. */
    public static double[] bounds(int metadata) {
        switch (metadata & 12) {
            case 4:
                return new double[] { 0, 4, 4, 16, 12, 12 };
            case 8:
                return new double[] { 4, 4, 0, 12, 12, 16 };
            default:
                return new double[] { 4, 0, 4, 12, 16, 12 };
        }
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        double[] box = bounds(world.getBlockMetadata(x, y, z));
        setBlockBounds(
            (float) box[0] / 16,
            (float) box[1] / 16,
            (float) box[2] / 16,
            (float) box[3] / 16,
            (float) box[4] / 16,
            (float) box[5] / 16);
    }

    @Override
    public void setBlockBoundsForItemRender() {
        setBlockBounds(.25F, 0, .25F, .75F, 1, .75F);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        setBlockBoundsBasedOnState(world, x, y, z);
        return super.getCollisionBoundingBoxFromPool(world, x, y, z);
    }
}
