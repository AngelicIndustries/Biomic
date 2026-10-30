package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import industries.angelic.biomic.client.BiomicRenderIds;

/** Shared non-solid behavior for Biomic's Origen dripleaf compatibility blocks. */
abstract class BlockDripleafBase extends Block implements WaterloggedRenderPlant {

    private final boolean waterlogged;

    BlockDripleafBase() {
        this(false);
    }

    BlockDripleafBase(boolean waterlogged) {
        super(waterlogged ? Material.water : Material.plants);
        this.waterlogged = waterlogged;
        setHardness(0.0F);
        setStepSound(soundTypeGrass);
        setLightOpacity(waterlogged ? 3 : 0);
        setTickRandomly(waterlogged);
    }

    public final boolean isWaterlogged() {
        return waterlogged;
    }

    @Override
    public final boolean containsSourceWater() {
        return waterlogged;
    }

    @Override
    public int getRenderBlockPass() {
        return waterlogged ? 1 : 0;
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return pass == 0 || waterlogged && pass == 1;
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
        return BiomicRenderIds.DRIPLEAF;
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        return canBlockStay(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (waterlogged && !world.checkChunksExist(x, Math.max(0, y - 1), z, x, y + 1, z)) {
            scheduleWater(world, x, y, z);
            return;
        }
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            restoreCell(world, x, y, z);
        } else scheduleWater(world, x, y, z);
    }

    private void restoreCell(World world, int x, int y, int z) {
        if (waterlogged) world.setBlock(x, y, z, net.minecraft.init.Blocks.water, 0, 3);
        else world.setBlockToAir(x, y, z);
    }

    @Override
    public boolean removedByPlayer(World world, net.minecraft.entity.player.EntityPlayer player, int x, int y, int z,
        boolean harvest) {
        if (!waterlogged) return super.removedByPlayer(world, player, x, y, z, harvest);
        return world.setBlock(x, y, z, net.minecraft.init.Blocks.water, 0, 3);
    }

    @Override
    public boolean removedByPlayer(World world, net.minecraft.entity.player.EntityPlayer player, int x, int y, int z) {
        return removedByPlayer(world, player, x, y, z, false);
    }

    @Override
    public void onBlockExploded(World world, int x, int y, int z, net.minecraft.world.Explosion explosion) {
        if (!waterlogged) {
            super.onBlockExploded(world, x, y, z, explosion);
            return;
        }
        if (world.setBlock(x, y, z, net.minecraft.init.Blocks.water, 0, 3))
            onBlockDestroyedByExplosion(world, x, y, z, explosion);
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        scheduleWater(world, x, y, z);
    }

    private void scheduleWater(World world, int x, int y, int z) {
        if (waterlogged && !world.isRemote && world.getBlock(x, y, z) == this)
            RetainedPlantSourceWater.schedule(world, x, y, z);
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (!waterlogged || world.isRemote || world.getBlock(x, y, z) != this) return;
        if (!world.checkChunksExist(x, Math.max(0, y - 1), z, x, y + 1, z)) {
            scheduleWater(world, x, y, z);
            return;
        }
        if (!canBlockStay(world, x, y, z)) {
            dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            restoreCell(world, x, y, z);
            return;
        }
        RetainedPlantSourceWater.tick(world, x, y, z, random);
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    static boolean hasPlantableFloor(IBlockAccess world, int x, int y, int z) {
        Block floor = world.getBlock(x, y - 1, z);
        Material material = floor.getMaterial();
        return material == Material.grass || material == Material.ground
            || material == Material.clay
            || material == Material.sand
            || floor.isOpaqueCube();
    }

    static Item noDrop() {
        return null;
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, int x, int y, int z) {
        return false;
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask,
        java.util.List<AxisAlignedBB> boxes, Entity entity) {}
}
