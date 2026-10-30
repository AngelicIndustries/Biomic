package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import industries.angelic.biomic.world.modern.ModernGroundCoverSupport;
import industries.angelic.biomic.world.modern.ModernStateCompatibility;

/** Source smouldering state retained in metadata; it is not a different plant or a light emitter. */
public final class BlockRuAshenGrass extends BlockRuPlant {

    public BlockRuAshenGrass(String name, String texture) {
        super(name, texture);
        setBlockBounds(.125F, 0, .125F, .875F, .625F, .875F);
    }

    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return y > 0 && Boolean.TRUE.equals(
            ModernGroundCoverSupport.testPinned(
                "regions_unexplored:ashen_grass",
                ModernStateCompatibility
                    .modernState(world.getBlock(x, y - 1, z), world.getBlockMetadata(x, y - 1, z))));
    }

    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block neighbor) {
        if (!canBlockStay(world, x, y, z)) world.setBlockToAir(x, y, z);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        if ((world.getBlockMetadata(x, y, z) & 1) != 0 && random.nextInt(20) == 0) {
            world.spawnParticle("flame", x + random.nextDouble(), y + .25, z + random.nextDouble(), 0, 0, 0);
            world.spawnParticle("smoke", x + random.nextDouble(), y + .25, z + random.nextDouble(), 0, 0, 0);
        }
    }
}
