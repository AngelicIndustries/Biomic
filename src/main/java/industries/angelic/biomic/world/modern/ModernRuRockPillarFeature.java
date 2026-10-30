package industries.angelic.biomic.world.modern;

import java.util.Random;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Exact shape port of pinned RU's rocky-reef {@code RockPillarFeature}. */
final class ModernRuRockPillarFeature {

    boolean place(World world, ModernRandom random, Pos origin) {
        random.nextInt(2); // unused var in the pinned source
        int size = random.nextInt(14) + 7;
        int top = world.seaLevel() + size;
        if (!"minecraft:sand".equals(block(world, origin.offset(0, -1, 0)))) return false;
        if (random.nextInt(3) == 0) return false;

        int radius = random.nextInt(6) + 2;
        Pos down = origin;
        for (int ignored = 0; ignored >= -7; ignored--) {
            if (radius == 1 || !inside(world, down)) return true;
            int north = random.nextInt(5);
            int south = random.nextInt(5);
            int east = random.nextInt(5);
            int west = random.nextInt(5);
            world.setBlock(down.x, down.y, down.z, "minecraft:stone");
            blob(world, down, false, 5, 5, 5);
            if (north == 0) blob(world, down.offset(0, 0, -1), false, 5, 5, 5);
            if (south == 0) blob(world, down.offset(0, 0, 1), false, 5, 5, 5);
            if (east == 0) blob(world, down.offset(1, 0, 0), false, 5, 5, 5);
            if (west == 0) blob(world, down.offset(-1, 0, 0), false, 5, 5, 5);
            down = down.offset(0, -1, 0);
        }

        Pos up = origin;
        int heightAtRadius = 0;
        while (up.y <= top) {
            if (!inside(world, up)) return true;
            int north = random.nextInt(5);
            int south = random.nextInt(5);
            int east = random.nextInt(5);
            int west = random.nextInt(5);
            world.setBlock(up.x, up.y, up.z, "minecraft:stone");
            blob(world, up, true, radius, radius + 1, radius);
            if (north == 0) blob(world, up.offset(0, 0, -1), true, radius, radius + 1, radius);
            if (south == 0) blob(world, up.offset(0, 0, 1), true, radius, radius + 1, radius);
            if (east == 0) blob(world, up.offset(1, 0, 0), true, radius, radius + 1, radius);
            if (west == 0) blob(world, up.offset(-1, 0, 0), true, radius, radius + 1, radius);
            if (++heightAtRadius == 4) {
                radius--;
                heightAtRadius = 0;
                if (radius == 0) break;
            }
            up = up.offset(0, 1, 0);
        }
        return true;
    }

    private void blob(World world, Pos initialCenter, boolean smoothing, int radiusX, int radiusY, int radiusZ) {
        if (initialCenter.y <= world.minBuildHeight() + 3) return;
        Random random = industries.angelic.biomic.world.replay.RuIncidentalRandom.random(
            world.worldSeed(),
            initialCenter.x,
            initialCenter.y,
            initialCenter.z,
            industries.angelic.biomic.world.replay.RuIncidentalRandom.ROCK_BLOB);
        Pos center = initialCenter;
        for (int iteration = 0; iteration < 3; iteration++) {
            float radius = (radiusX + radiusY + radiusZ) * 0.333F + 0.5F;
            float radiusSquared = radius * radius;
            for (int z = center.z - radiusZ; z <= center.z + radiusZ; z++) {
                for (int y = center.y - radiusY; y <= center.y + radiusY; y++) {
                    for (int x = center.x - radiusX; x <= center.x + radiusX; x++) {
                        if (distanceSquared(x, y, z, center) > radiusSquared) continue;
                        // Pinned source compares Block to RUBlocks' Supplier here (never equal).
                        // Do not strip the previous blob's four surviving grass ledges.
                        world.setBlock(x, y, z, "minecraft:stone");
                    }
                }
            }
            if (smoothing) {
                for (int z = center.z - radiusZ; z <= center.z + radiusZ; z++) {
                    for (int y = center.y - radiusY; y <= center.y + radiusY; y++) {
                        for (int x = center.x - radiusX; x <= center.x + radiusX; x++) {
                            if (distanceSquared(x, y, z, center) <= radiusSquared) {
                                smooth(world, new Pos(x, y, z));
                            }
                        }
                    }
                }
            }
            for (int z = center.z - radiusZ; z <= center.z + radiusZ; z++) {
                for (int y = center.y - radiusY; y <= center.y + radiusY; y++) {
                    for (int x = center.x - radiusX; x <= center.x + radiusX; x++) {
                        if (distanceSquared(x, y, z, center) > radiusSquared) continue;
                        Pos position = new Pos(x, y, z);
                        String above = block(world, position.offset(0, 1, 0));
                        if (("minecraft:air".equals(above) || "minecraft:short_grass".equals(above))
                            && "minecraft:stone".equals(block(world, position.offset(0, -1, 0)))) {
                            world.setBlock(x, y, z, "regions_unexplored:stone_grass_block[snowy=false]");
                            if (random.nextInt(4) == 0) {
                                world.setBlock(x, y + 1, z, "minecraft:short_grass");
                            }
                        }
                    }
                }
            }
            center = center.offset(-1 + random.nextInt(2), -random.nextInt(2), -1 + random.nextInt(2));
        }
    }

    private static void smooth(World world, Pos position) {
        if (!isRock(block(world, position))) return;
        String below = block(world, position.offset(0, -1, 0));
        if ("minecraft:air".equals(below) || "minecraft:water".equals(below)) {
            replaceWithAirOrWater(world, position);
            replaceWithAirOrWater(world, position.offset(0, 1, 0));
            return;
        }
        int exposed = 0;
        int[][] neighbors = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
        for (int[] offset : neighbors) {
            if (!isRock(block(world, position.offset(offset[0], 0, offset[1])))) exposed++;
        }
        if (exposed >= 3) replaceWithAirOrWater(world, position);
    }

    /** Air above the sea, water in it; the sea's level is the world's (191 at Hohen Angst's 512, not 63). */
    private static void replaceWithAirOrWater(World world, Pos position) {
        world.setBlock(
            position.x,
            position.y,
            position.z,
            position.y >= world.seaLevel() ? "minecraft:air" : "minecraft:water[level=0]");
    }

    private static double distanceSquared(int x, int y, int z, Pos center) {
        long dx = x - center.x;
        long dy = y - center.y;
        long dz = z - center.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean isRock(String block) {
        return "minecraft:stone".equals(block) || "regions_unexplored:stone_grass_block".equals(block)
            || "minecraft:moss_block".equals(block);
    }

    private static String block(World world, Pos position) {
        return ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
    }

    private static boolean inside(World world, Pos position) {
        return position.y >= world.minBuildHeight() && position.y < world.maxBuildHeight();
    }
}
