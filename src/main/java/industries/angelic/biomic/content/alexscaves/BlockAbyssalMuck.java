package industries.angelic.biomic.content.alexscaves;

import net.minecraft.block.BlockFalling;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

/** Source muck's falling behavior, 13/16 collision shelf and ordinary-entity drag. */
final class BlockAbyssalMuck extends BlockFalling {

    BlockAbyssalMuck() {
        super(Material.sand);
        setBlockName("biomic.alexs_abyssal_muck");
        setBlockTextureName("biomic:alexscaves/muck");
        setHardness(.5F);
        setStepSound(soundTypeSand);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
        return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 13 / 16D, z + 1);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        entity.motionX *= .85D;
        entity.motionZ *= .85D;
    }
}
