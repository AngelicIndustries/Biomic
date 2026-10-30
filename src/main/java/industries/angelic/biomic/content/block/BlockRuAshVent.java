package industries.angelic.biomic.content.block;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Pinned RU ash vent: basalt underside, distinct crater top, smoke and lava particles. */
public final class BlockRuAshVent extends BlockRuSolid {

    public BlockRuAshVent(String name, String side, String top) {
        super(name, side, top, Material.rock, 0);
        setHardness(1.6F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return side == 0 ? ganymedes01.etfuturum.ModBlocks.BASALT.get()
            .getIcon(0, 0) : super.getIcon(side, metadata);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random random) {
        for (int i = 0; i < 10; i++) {
            world.spawnParticle(
                "smoke",
                x + .5 + random.nextDouble() / 3 * (random.nextBoolean() ? 1 : -1),
                y + random.nextDouble() + random.nextDouble(),
                z + .5 + random.nextDouble() / 3 * (random.nextBoolean() ? 1 : -1),
                0,
                .07,
                0);
            world.spawnParticle(
                "smoke",
                x + .5 + random.nextDouble() / 4 * (random.nextBoolean() ? 1 : -1),
                y + .4,
                z + .5 + random.nextDouble() / 4 * (random.nextBoolean() ? 1 : -1),
                0,
                .005,
                0);
        }
        // 1.7 has no campfire crackle; its fire ambience is the closest native sound.
        if (random.nextInt(10) == 0) world.playSound(
            x + .5,
            y + .5,
            z + .5,
            "fire.fire",
            .5F + random.nextFloat(),
            random.nextFloat() * .7F + .6F,
            false);
        for (int i = 0; i < 2; i++)
            world.spawnParticle("lava", x + .5, y + .5, z + .5, random.nextFloat() / 2, 5.0E-5, random.nextFloat() / 2);
    }
}
