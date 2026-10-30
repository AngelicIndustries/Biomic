package industries.angelic.biomic.content.alexscaves;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Source-textured cave plant with the source feature's solid-floor survival rule. */
final class BlockForlornUnderweed extends Block {

    BlockForlornUnderweed() {
        super(Material.plants);
        setBlockName("biomic.alexs_forlorn_underweed");
        setHardness(0);
        setLightOpacity(0);
        setStepSound(soundTypeGrass);
        // CavePlantBlock(false): 2..14 wide and exactly three pixels tall.
        setBlockBounds(2 / 16F, 0, 2 / 16F, 14 / 16F, 3 / 16F, 14 / 16F);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return y > 0 && world.getBlock(x, y - 1, z)
            .isOpaqueCube()
            && world.getBlock(x, y - 1, z)
                .getMaterial()
                .blocksMovement();
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) world.setBlockToAir(x, y, z);
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
        return 1;
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
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        blockIcon = register.registerIcon("biomic:alexscaves/underweed");
    }
}
