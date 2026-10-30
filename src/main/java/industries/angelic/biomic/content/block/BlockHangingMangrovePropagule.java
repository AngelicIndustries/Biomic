package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModBlocks;
import industries.angelic.biomic.Biomic;
import industries.angelic.biomic.client.BiomicRenderIds;

/** Hanging mangrove-propagule states missing from EFR's otherwise reusable modern sapling block. */
public final class BlockHangingMangrovePropagule extends Block {

    public static final int MAX_AGE = 4;

    private IIcon hangingIcon;

    public BlockHangingMangrovePropagule() {
        super(Material.plants);
        setBlockName(Biomic.MODID + ".hanging_mangrove_propagule");
        setBlockTextureName("minecraft:mangrove_propagule_hanging");
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightOpacity(0);
        setTickRandomly(true);
        setBlockBounds(6.0F / 16.0F, 0.0F, 6.0F / 16.0F, 10.0F / 16.0F, 1.0F, 10.0F / 16.0F);
    }

    public static int age(int metadata) {
        return Math.min(MAX_AGE, metadata & 7);
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
        Block leaves = ModBlocks.LEAVES.get();
        return leaves != null && world.getBlock(x, y + 1, z) == leaves
            && (world.getBlockMetadata(x, y + 1, z) & 3) == 0;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        int currentAge = age(world.getBlockMetadata(x, y, z));
        if (currentAge < MAX_AGE && random.nextInt(7) == 0) {
            world.setBlockMetadataWithNotify(x, y, z, currentAge + 1, 2);
        }
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        Block sapling = ModBlocks.SAPLING.get();
        return sapling == null ? Item.getItemFromBlock(this) : Item.getItemFromBlock(sapling);
    }

    @Override
    public int damageDropped(int metadata) {
        return 0;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        // EFR already ships the exact vanilla artwork in the minecraft namespace, but not the hanging block state.
        hangingIcon = register.registerIcon("minecraft:mangrove_propagule_hanging");
        blockIcon = hangingIcon;
    }

    @SideOnly(Side.CLIENT)
    public IIcon getHangingIcon() {
        return hangingIcon;
    }
}
