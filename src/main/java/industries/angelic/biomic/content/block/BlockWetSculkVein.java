package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import industries.angelic.biomic.content.BiomicWetSculkBlocks;

/** Exact six-face sculk mesh with a persisted source-water cell, without a tile entity. */
public final class BlockWetSculkVein extends BlockSculkVein implements WaterloggedRenderPlant {

    public BlockWetSculkVein(String name, boolean up, boolean down) {
        super(name, up, down, Material.water);
        setLightOpacity(3);
        setTickRandomly(true);
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
    public boolean getBlocksMovement(net.minecraft.world.IBlockAccess world, int x, int y, int z) {
        return true;
    }

    @Override
    public boolean canPlaceBlockAt(World world, int x, int y, int z) {
        Block before = world.getBlock(x, y, z);
        return (before == Blocks.water || before == Blocks.flowing_water) && world.getBlockMetadata(x, y, z) == 0;
    }

    @Override
    protected boolean canSilkHarvest() {
        return false;
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        schedule(world, x, y, z);
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        schedule(world, x, y, z);
    }

    private void schedule(World world, int x, int y, int z) {
        if (!world.isRemote && BiomicWetSculkBlocks.wet(this) && world.getBlock(x, y, z) == this)
            RetainedPlantSourceWater.schedule(world, x, y, z);
    }

    /** Incomplete resident observations defer; native updates must not load neighboring terrain. */
    public static int survivingFaces(int faces, java.util.function.IntPredicate resident,
        java.util.function.IntPredicate supported) {
        if (faces < 0 || faces > 63) throw new IllegalArgumentException("Invalid sculk faces");
        for (int side = 0; side < 6; side++) if ((faces & (1 << side)) != 0 && !resident.test(side)) return faces;
        int remaining = 0;
        for (int side = 0; side < 6; side++)
            if ((faces & (1 << side)) != 0 && supported.test(side)) remaining |= 1 << side;
        return remaining;
    }

    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        if (world.isRemote || !BiomicWetSculkBlocks.wet(this) || world.getBlock(x, y, z) != this) return;
        int original = faceMask(world.getBlockMetadata(x, y, z));
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) if ((original & (1 << d.ordinal())) != 0
            && !world.blockExists(x + d.offsetX, y + d.offsetY, z + d.offsetZ)) {
                schedule(world, x, y, z);
                return;
            }
        int remaining = survivingFaces(original, side -> {
            ForgeDirection d = ForgeDirection.getOrientation(side);
            return world.blockExists(x + d.offsetX, y + d.offsetY, z + d.offsetZ);
        }, side -> {
            ForgeDirection d = ForgeDirection.getOrientation(side);
            int sx = x + d.offsetX, sy = y + d.offsetY, sz = z + d.offsetZ;
            return world.getBlock(sx, sy, sz)
                .isSideSolid(world, sx, sy, sz, d.getOpposite());
        });
        if (remaining == 0) {
            world.setBlock(x, y, z, Blocks.water, 0, 3);
            return;
        }
        if (remaining != original) {
            if (!world.setBlock(
                x,
                y,
                z,
                BiomicWetSculkBlocks.wetVein((remaining & 2) != 0, (remaining & 1) != 0),
                horizontalMetadata(remaining),
                3)) {
                schedule(world, x, y, z);
                return;
            }
        }
        // The shared hook proves installed identity, normalized source level and resident fluid neighborhood.
        RetainedPlantSourceWater.tick(world, x, y, z, random);
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
    public Item getItem(World world, int x, int y, int z) {
        return Item.getItemFromBlock(BiomicWetSculkBlocks.dryCounterpart(this));
    }
}
