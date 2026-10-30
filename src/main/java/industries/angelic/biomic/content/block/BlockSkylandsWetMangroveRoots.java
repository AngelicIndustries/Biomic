package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModBlocks;
import industries.angelic.biomic.client.BiomicRenderIds;

/** Distinct persisted source-water root cell; never aliases or mutates EFR's dry roots. */
public final class BlockSkylandsWetMangroveRoots extends Block implements WaterloggedRenderPlant {

    public BlockSkylandsWetMangroveRoots() {
        super(Material.water);
        setBlockName("biomic.skylands_wet_mangrove_roots");
        // Wet source water uses native water attenuation3; EFR dry roots retain their own opacity1.
        setHardness(.7F);
        setResistance(.7F);
        setLightOpacity(3);
        setStepSound(soundTypeWood);
    }

    public static Block dryRoots() {
        Block block = ModBlocks.MANGROVE_ROOTS.get();
        if (!ModBlocks.MANGROVE_ROOTS.isEnabled() || block == null
            || Block.blockRegistry.getObject("etfuturum:mangrove_roots") != block)
            throw new IllegalStateException("Installed enabled EFR mangrove roots required");
        return block;
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
        return BiomicRenderIds.WET_MANGROVE_ROOTS;
    }

    @Override
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        // Modern roots use their full collision cube for all attachment faces despite noOcclusion.
        // Forge's default ties support to opaque material/normal rendering, losing these faces
        // for retained-water roots and causing real lichen/sculk/fan neighbor checks to drop them.
        return world != null && side != null && side != ForgeDirection.UNKNOWN && world.getBlockMetadata(x, y, z) == 0;
    }

    // Block's default collision is a full cube despite the retained water material.
    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean harvest) {
        return restoreAfterHarvest(() -> world.setBlock(x, y, z, Blocks.water, 0, 3));
    }

    static boolean restoreAfterHarvest(java.util.function.BooleanSupplier restoreSourceWater) {
        return restoreSourceWater.getAsBoolean();
    }

    @Override
    public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
        if (world.setBlock(x, y, z, Blocks.water, 0, 3)) onBlockDestroyedByExplosion(world, x, y, z, explosion);
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }

    @Override
    protected ItemStack createStackedBlock(int metadata) {
        return new ItemStack(dryRoots(), 1, 0);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        scheduleSource(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        scheduleSource(world, x, y, z);
    }

    private void scheduleSource(World world, int x, int y, int z) {
        if (!world.isRemote && this == industries.angelic.biomic.content.BiomicBlocks.SKYLANDS_WET_MANGROVE_ROOTS
            && world.getBlock(x, y, z) == this
            && world.getBlockMetadata(x, y, z) == 0)
            world.scheduleBlockUpdate(x, y, z, this, Blocks.flowing_water.tickRate(world));
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (!world.isRemote && this == industries.angelic.biomic.content.BiomicBlocks.SKYLANDS_WET_MANGROVE_ROOTS
            && world.getBlock(x, y, z) == this
            && world.getBlockMetadata(x, y, z) == 0) RetainedPlantSourceWater.tick(world, x, y, z, random);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
        return removedByPlayer(world, player, x, y, z, false);
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return Item.getItemFromBlock(dryRoots());
    }

    @Override
    public int damageDropped(int metadata) {
        return 0;
    }

    @Override
    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(dryRoots());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister registry) {
        // EFR registers the shared root sprites; this block delegates getIcon and owns no texture.
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return dryRoots().getIcon(side, 0);
    }
}
