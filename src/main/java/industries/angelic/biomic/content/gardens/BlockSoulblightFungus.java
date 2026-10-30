package industries.angelic.biomic.content.gardens;

import java.util.Random;

import net.minecraft.block.IGrowable;
import net.minecraft.world.World;

final class BlockSoulblightFungus extends BlockGardensPlant implements IGrowable {

    BlockSoulblightFungus() {
        super("gardens_soulblight_fungus", "gardens/soulblight_fungus", Support.SOUL, 0);
    }

    @Override
    public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
        return y > 0 && GardensSupport.soulSoil(world.getBlock(x, y - 1, z), world.getBlockMetadata(x, y - 1, z));
    }

    @Override
    public boolean func_149852_a(World world, Random random, int x, int y, int z) {
        return true;
    }

    @Override
    public void func_149853_b(World world, Random random, int x, int y, int z) {
        industries.angelic.biomic.world.terrain.gardens.GardensHugeFungusFeature.grow(world, random, x, y, z);
    }
}
