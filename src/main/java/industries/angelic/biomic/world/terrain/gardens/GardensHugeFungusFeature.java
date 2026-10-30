package industries.angelic.biomic.world.terrain.gardens;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import industries.angelic.biomic.content.gardens.BlockSoulSpore;
import industries.angelic.biomic.content.gardens.GardensContent;
import industries.angelic.biomic.world.terrain.OrigenBlockStateResolver;

/** Source-faithful 1.7 translation of Gardens' HugeFlatFungusFeature, including its flat wobbling cap. */
public final class GardensHugeFungusFeature {

    private GardensHugeFungusFeature() {}

    public static boolean grow(World world, Random random, int originX, int originY, int originZ) {
        if (world.getBlock(originX, originY - 1, originZ) != Blocks.soul_sand
            && !isSoulSoil(world, originX, originY - 1, originZ)) return false;
        int height = 4 + random.nextInt(6);
        world.setBlockToAir(originX, originY, originZ);
        placeStem(world, random, originX, originY, originZ, height);
        placeHat(world, random, originX, originY, originZ, height);
        return true;
    }

    private static void placeStem(World world, Random random, int ox, int oy, int oz, int height) {
        for (int y = 0; y < height; ++y) if (replaceable(world, ox, oy + y, oz))
            world.setBlock(ox, oy + y, oz, GardensContent.SOULBLIGHT_STEM, 0, 3);
        for (int x = -1; x <= 1; ++x) for (int z = -1; z <= 1; ++z) if (x != 0 || z != 0) {
            boolean corner = x != 0 && z != 0;
            if ((!corner && random.nextFloat() <= .75F) || (corner && random.nextFloat() < .25F)) {
                int y = oy;
                if (!world.isAirBlock(ox + x, oy - 1, oz + z) && tryStem(world, ox + x, y, oz + z)
                    && !corner
                    && random.nextFloat() < .25F) tryStem(world, ox + x, oy + 1, oz + z);
            }
            if ((!corner && random.nextFloat() <= .75F) || (corner && random.nextFloat() < .25F)) {
                int y = oy + height - 1;
                if (tryStem(world, ox + x, y, oz + z) && height > 4 && !corner && random.nextFloat() < .25F)
                    tryStem(world, ox + x, y - 1, oz + z);
            }
        }
    }

    private static boolean tryStem(World world, int x, int y, int z) {
        if (!replaceable(world, x, y, z)) return false;
        if (!world.isAirBlock(x, y - 1, z)) world.setBlockToAir(x, y, z);
        return world.setBlock(x, y, z, GardensContent.SOULBLIGHT_STEM, 0, 3);
    }

    private static void placeHat(World world, Random random, int ox, int oy, int oz, int height) {
        float wobbliness = .1F + random.nextFloat() * .15F;
        float wobbleAngle = random.nextFloat() * (float) Math.PI * 2F;
        float size = (6F + random.nextFloat() * 3F) / 2F;
        int maximum = (int) Math.ceil(size * 1.25F);
        boolean glowing = random.nextFloat() < .4F;
        for (int x = -maximum; x <= maximum; ++x) for (int z = -maximum; z <= maximum; ++z) {
            float angle = x == 0 && z == 0 ? 0 : (float) Math.atan2(x, z);
            float wobble = 1F + wobbliness * (float) Math.cos(angle - wobbleAngle);
            float distance = x * x + z * z;
            if (wobble * wobble * distance > size * size) continue;
            boolean edge = wobble * wobble * distance > (size - 1.5F) * (size - 1.5F);
            int y = oy + height - (edge ? 1 : 0);
            if (!replaceable(world, ox + x, y, oz + z)) continue;
            if (!world.isAirBlock(ox + x, y - 1, oz + z)) world.setBlockToAir(ox + x, y, oz + z);
            world.setBlock(ox + x, y, oz + z, GardensContent.BLIGHTWART_BLOCK, 0, 3);
            if (!edge && random.nextFloat() < .05F && world.isAirBlock(ox + x, y - 1, oz + z)) {
                OrigenBlockStateResolver.ResolvedBlock light = new OrigenBlockStateResolver()
                    .resolve("minecraft:shroomlight");
                world.setBlock(ox + x, y - 1, oz + z, light.block(), light.metadata(), 3);
            } else if (random.nextFloat() < (edge ? .3F : .75F) && world.isAirBlock(ox + x, y - 1, oz + z)) {
                placeSpores(
                    world,
                    random,
                    ox + x,
                    y - 1,
                    oz + z,
                    2 + random.nextInt(2) + (edge ? 0 : 1),
                    edge && glowing ? 1F : 0F);
            }
            if (random.nextFloat() < .2F && world.isAirBlock(ox + x, y + 1, oz + z))
                world.setBlock(ox + x, y + 1, oz + z, GardensContent.SOULBLIGHT_SPROUTS, 0, 2);
        }
    }

    public static void placeSpores(World world, Random random, int x, int y, int z, int length, float glowingChance) {
        int direction = -1;
        for (int step = 1; step <= length; ++step, y += direction) {
            if (!world.isAirBlock(x, y, z)) continue;
            boolean terminal = step == length || !world.isAirBlock(x, y + direction, z);
            if (terminal) {
                if (random.nextFloat() < glowingChance && step > 1)
                    world.setBlock(x, y, z, GardensContent.GLOWING_SOUL_SPORE, BlockSoulSpore.DOWN, 2);
                else world.setBlock(x, y, z, GardensContent.SOUL_SPORE, BlockSoulSpore.metadata(true, true, false), 2);
                break;
            }
            world.setBlock(x, y, z, GardensContent.SOUL_SPORE, BlockSoulSpore.metadata(true, false, false), 2);
        }
    }

    private static boolean replaceable(World world, int x, int y, int z) {
        if (y < 1 || y >= world.getHeight() - 1) return false;
        Block block = world.getBlock(x, y, z);
        return block.isReplaceable(world, x, y, z);
    }

    private static boolean isSoulSoil(World world, int x, int y, int z) {
        String state = industries.angelic.biomic.world.modern.ModernStateCompatibility
            .modernState(world.getBlock(x, y, z), world.getBlockMetadata(x, y, z));
        return state != null && state.startsWith("minecraft:soul_soil");
    }
}
