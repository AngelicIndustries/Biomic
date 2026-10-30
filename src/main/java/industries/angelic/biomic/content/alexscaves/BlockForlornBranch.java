package industries.angelic.biomic.content.alexscaves;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.client.BiomicRenderIds;

/** Source-shaped, six-direction thornwood branch. Metadata 0..5 is up, down, north, east, south, west. */
public final class BlockForlornBranch extends Block {

    private IIcon sideIcon;

    BlockForlornBranch() {
        super(Material.wood);
        setBlockName("biomic.alexs_forlorn_thornwood_branch");
        setHardness(0);
        setStepSound(soundTypeWood);
        setLightOpacity(0);
        setBounds(0);
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
        return BiomicRenderIds.FORLORN_BRANCH;
    }

    /** Source model is cutout, not translucent: binary-alpha needles belong in the opaque/cutout pass. */
    @Override
    public int getRenderBlockPass() {
        return 0;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return Item.getItemFromBlock(this);
    }

    @Override
    public int damageDropped(int metadata) {
        return 0;
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) {
        setBounds(world.getBlockMetadata(x, y, z));
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        int facing = facing(world.getBlockMetadata(x, y, z));
        int sx = x - supportX(facing), sy = y - supportY(facing), sz = z - supportZ(facing);
        Block support = world.getBlock(sx, sy, sz);
        return support.isSideSolid(
            world,
            sx,
            sy,
            sz,
            net.minecraftforge.common.util.ForgeDirection.getOrientation(forgeSide(facing)))
            || facing >= 2 && support == this;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) world.setBlockToAir(x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        sideIcon = register.registerIcon("biomic:alexscaves/thornwood_branch");
        blockIcon = sideIcon;
    }

    public static int facing(int metadata) {
        return metadata >= 0 && metadata <= 5 ? metadata : 0;
    }

    private void setBounds(int metadata) {
        switch (facing(metadata)) {
            case 0 -> setBlockBounds(1 / 16F, 0, 1 / 16F, 15 / 16F, 9 / 16F, 15 / 16F);
            case 1 -> setBlockBounds(1 / 16F, 7 / 16F, 1 / 16F, 15 / 16F, 1, 15 / 16F);
            case 2, 4 -> setBlockBounds(7 / 16F, 1 / 16F, 0, 9 / 16F, 15 / 16F, 1);
            case 3, 5 -> setBlockBounds(0, 1 / 16F, 7 / 16F, 1, 15 / 16F, 9 / 16F);
            default -> throw new AssertionError();
        }
    }

    private static int supportX(int facing) {
        return facing == 3 ? 1 : facing == 5 ? -1 : 0;
    }

    private static int supportY(int facing) {
        return facing == 0 ? 1 : facing == 1 ? -1 : 0;
    }

    private static int supportZ(int facing) {
        return facing == 4 ? 1 : facing == 2 ? -1 : 0;
    }

    private static int forgeSide(int facing) {
        return switch (facing) {
            case 0 -> 1;
            case 1 -> 0;
            case 2 -> 2;
            case 3 -> 5;
            case 4 -> 3;
            case 5 -> 4;
            default -> 1;
        };
    }
}
