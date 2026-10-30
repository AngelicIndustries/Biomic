package industries.angelic.biomic.content.block;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;

/** Ceiling-mounted vanilla spore blossom used by Origen's lush cave and wet-forest features. */
public final class BlockSporeBlossom extends Block {

    private IIcon flowerIcon;
    private IIcon baseIcon;

    public BlockSporeBlossom() {
        super(Material.plants);
        setBlockName(Biomic.MODID + ".spore_blossom");
        setBlockTextureName(Biomic.MODID + ":spore_blossom");
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightOpacity(0);
        setBlockBounds(1.0F / 16.0F, 13.0F / 16.0F, 1.0F / 16.0F, 15.0F / 16.0F, 1.0F, 15.0F / 16.0F);
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
        return BiomicRenderIds.LUSH_PLANTS;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask,
        java.util.List<AxisAlignedBB> boxes, Entity entity) {}

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return canBlockStay(world, x, y, z);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        Block ceiling = world.getBlock(x, y + 1, z);
        return ceiling.isSideSolid(world, x, y + 1, z, ForgeDirection.DOWN);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        flowerIcon = register.registerIcon(Biomic.MODID + ":spore_blossom");
        baseIcon = register.registerIcon(Biomic.MODID + ":spore_blossom_base");
        blockIcon = flowerIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getFlowerIcon() {
        return flowerIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getBaseIcon() {
        return baseIcon;
    }
}
