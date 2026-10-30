package industries.angelic.biomic.content.block;

import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import industries.angelic.biomic.Biomic;

/** Traversable powder snow for Origen's frozen-surface deposits. */
public final class BlockPowderSnow extends Block {

    public BlockPowderSnow() {
        super(Material.snow);
        setBlockName(Biomic.MODID + ".powder_snow");
        setBlockTextureName(Biomic.MODID + ":powder_snow");
        setHardness(0.25F);
        setResistance(0.25F);
        setStepSound(soundTypeSnow);
        setLightOpacity(0);
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean renderAsNormalBlock() {
        return true;
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, int x, int y, int z, int side) {
        return world.getBlock(x, y, z) != this && super.shouldSideBeRendered(world, x, y, z, side);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return null;
    }

    @Override
    public void addCollisionBoxesToList(World world, int x, int y, int z, AxisAlignedBB mask, List<AxisAlignedBB> boxes,
        Entity entity) {}

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        entity.motionX *= 0.9D;
        entity.motionZ *= 0.9D;
        if (entity.motionY < 0.0D) entity.motionY *= 0.65D;
        entity.extinguish();
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return null;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }
}
