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

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import ganymedes01.etfuturum.ModBlocks;
import ganymedes01.etfuturum.blocks.BlockGlowLichen;
import ganymedes01.etfuturum.tileentities.TileEntityGlowLichen;
import industries.angelic.biomic.client.BiomicRenderIds;

/** Retained source water plus the original EFR six-face tile and face geometry. Dry EFR is unchanged. */
public final class BlockSkylandsWetGlowLichen extends BlockGlowLichen implements WaterloggedRenderPlant {

    public BlockSkylandsWetGlowLichen() {
        setBlockName("biomic.skylands_wet_glow_lichen");
        setLightOpacity(3);
    }

    public static Block dryLichen() {
        var block = ModBlocks.GLOW_LICHEN.get();
        if (!ModBlocks.GLOW_LICHEN.isEnabled() || block == null
            || block.getClass() != BlockGlowLichen.class
            || Block.blockRegistry.getObject("etfuturum:glow_lichen") != block)
            throw new IllegalStateException("Exact installed EFR glow lichen required");
        return block;
    }

    // EFR's constructor owns a final vine material. Native liquid queries use this accessor; face collision stays EFR.
    @Override
    public Material getMaterial() {
        return Material.water;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public int getRenderType() {
        return BiomicRenderIds.WET_GLOW_LICHEN;
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
    protected boolean canSilkHarvest() {
        return false;
    }

    @Override
    protected ItemStack createStackedBlock(int metadata) {
        return new ItemStack(dryLichen(), 1, 0);
    }

    @Override
    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(dryLichen());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {/* Shared EFR sprite. */}

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return dryLichen().getIcon(side, 0);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        schedule(world, x, y, z);
    }

    private void schedule(World world, int x, int y, int z) {
        RetainedPlantSourceWater.schedule(world, x, y, z);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote || !world.blockExists(x, y, z)
            || world.getBlock(x, y, z) != this
            || world.getBlockMetadata(x, y, z) != 0) return;
        if (!world.checkChunksExist(x - 2, Math.max(0, y - 2), z - 2, x + 2, y + 2, z + 2)) {
            schedule(world, x, y, z);
            return;
        }
        var raw = world.getTileEntity(x, y, z);
        // A freshly written block can precede its transaction's whole tile attach. Never erase that temporary cell.
        if (raw == null || raw.getClass() != TileEntityGlowLichen.class
            || ((TileEntityGlowLichen) raw).getSideMap() == 0) {
            schedule(world, x, y, z);
            return;
        }
        var tile = (TileEntityGlowLichen) raw;
        int original = tile.getSideMap();
        int proved = industries.angelic.biomic.world.terrain.PhysicalLichenSupport
            .supported(original, x, y, z, world.getActualHeight(), (px, py, pz) -> {
                if (!world.blockExists(px, py, pz))
                    throw new UnsupportedOperationException("Nonresident wet lichen support");
                return new industries.angelic.biomic.world.terrain.PhysicalLichenSupport.Cell(
                    world.getBlock(px, py, pz),
                    world.getBlockMetadata(px, py, pz));
            });
        int supported = remainingFaces(original, side -> (proved & (1 << side)) != 0);
        if (supported == 0) {
            world.setBlock(x, y, z, Blocks.water, 0, 3);
            return;
        }
        if (supported != original) tile.setSideMap(supported);
        RetainedPlantSourceWater.tick(world, x, y, z, random);
    }

    /** Never call EFR setSideMap(0): it destroys the source water with setBlockToAir. */
    static int remainingFaces(int original, java.util.function.IntPredicate supported) {
        if (original < 0 || original > 63) throw new IllegalArgumentException("Invalid original EFR face mask");
        int result = 0;
        for (int side = 0; side < 6; side++)
            if ((original & (1 << side)) != 0 && supported.test(side)) result |= 1 << side;
        return result;
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        // Delay support until after the complete publication, not in the middle of multi-cell block/tile writes.
        schedule(world, x, y, z);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean harvest) {
        return world.setBlock(x, y, z, Blocks.water, 0, 3);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
        return removedByPlayer(world, player, x, y, z, false);
    }

    @Override
    public void onBlockExploded(World world, int x, int y, int z, Explosion explosion) {
        if (world.setBlock(x, y, z, Blocks.water, 0, 3)) onBlockDestroyedByExplosion(world, x, y, z, explosion);
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block old, int metadata) {
        super.breakBlock(world, x, y, z, old, metadata);
        // Piston/direct native AIR removal also retains the embedded source; deliberate solid replacement is not
        // undone.
        if (!world.isRemote && world.getBlock(x, y, z) == Blocks.air) world.setBlock(x, y, z, Blocks.water, 0, 3);
    }
}
