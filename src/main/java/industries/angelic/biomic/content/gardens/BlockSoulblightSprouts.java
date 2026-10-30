package industries.angelic.biomic.content.gardens;

import java.util.Random;

import net.minecraft.block.IGrowable;
import net.minecraft.world.World;

final class BlockSoulblightSprouts extends BlockGardensPlant implements IGrowable {

    BlockSoulblightSprouts() {
        super("gardens_soulblight_sprouts", "gardens/soulblight_sprouts", Support.SOUL, 0);
    }

    @Override
    public boolean func_149851_a(World world, int x, int y, int z, boolean client) {
        return true;
    }

    @Override
    public boolean func_149852_a(World world, Random random, int x, int y, int z) {
        return true;
    }

    @Override
    public void func_149853_b(World world, Random random, int x, int y, int z) {
        world.setBlock(x, y, z, GardensContent.SOULBLIGHT_FUNGUS, 0, 3);
    }
}
