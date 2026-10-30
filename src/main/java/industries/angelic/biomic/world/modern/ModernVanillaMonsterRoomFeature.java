package industries.angelic.biomic.world.modern;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Exact pinned 1.21.1 + NeoForge monster-room generation. */
final class ModernVanillaMonsterRoomFeature {

    private static final int[][] HORIZONTAL = { { 0, 0, -1 }, { 1, 0, 0 }, { 0, 0, 1 }, { -1, 0, 0 } };
    private static final String[] NAMES = { "north", "east", "south", "west" };

    boolean place(ModernDecorationEngine engine, World world, ModernRandom random, Pos origin) {
        int radiusX = random.nextInt(2) + 2;
        int minX = -radiusX - 1;
        int maxX = radiusX + 1;
        int radiusZ = random.nextInt(2) + 2;
        int minZ = -radiusZ - 1;
        int maxZ = radiusZ + 1;
        int openings = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = -1; y <= 4; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Pos position = origin.offset(x, y, z);
                    boolean solid = world.isSolid(position.x, position.y, position.z);
                    if ((y == -1 || y == 4) && !solid) return false;
                    boolean boundary = x == minX || x == maxX || z == minZ || z == maxZ;
                    if (boundary && y == 0 && isAir(world, position) && isAir(world, position.offset(0, 1, 0)))
                        openings++;
                }
            }
        }
        if (openings < 1 || openings > 5) return false;

        for (int x = minX; x <= maxX; x++) {
            for (int y = 3; y >= -1; y--) {
                for (int z = minZ; z <= maxZ; z++) {
                    Pos position = origin.offset(x, y, z);
                    String current = ModernDecorationEngine
                        .blockName(world.blockState(position.x, position.y, position.z));
                    boolean boundary = x == minX || y == -1 || z == minZ || x == maxX || y == 4 || z == maxZ;
                    if (boundary) {
                        if (position.y >= world.minBuildHeight()
                            && !world.isSolid(position.x, position.y - 1, position.z)) {
                            safeSet(engine, world, position, "minecraft:cave_air");
                        } else if (world.isSolid(position.x, position.y, position.z)
                            && !"minecraft:chest".equals(current)) {
                                String block = y == -1 && random.nextInt(4) != 0 ? "minecraft:mossy_cobblestone"
                                    : "minecraft:cobblestone";
                                safeSet(engine, world, position, block);
                            }
                    } else if (!"minecraft:chest".equals(current) && !"minecraft:spawner".equals(current)) {
                        safeSet(engine, world, position, "minecraft:cave_air");
                    }
                }
            }
        }

        for (int chest = 0; chest < 2; chest++) {
            for (int attempt = 0; attempt < 3; attempt++) {
                Pos position = new Pos(
                    origin.x + random.nextInt(radiusX * 2 + 1) - radiusX,
                    origin.y,
                    origin.z + random.nextInt(radiusZ * 2 + 1) - radiusZ);
                if (!isAir(world, position)) continue;
                int solidNeighbors = 0;
                for (int[] direction : HORIZONTAL) {
                    if (world.isSolid(position.x + direction[0], position.y, position.z + direction[2]))
                        solidNeighbors++;
                }
                if (solidNeighbors != 1) continue;
                safeSet(engine, world, position, orientedChest(world, position));
                world.setLootTable(
                    position.x,
                    position.y,
                    position.z,
                    "minecraft:chests/simple_dungeon",
                    random.nextLong());
                break;
            }
        }

        safeSet(engine, world, origin, "minecraft:spawner");
        int mob = random.nextInt(400);
        String entity = mob < 100 ? "minecraft:skeleton" : mob < 200 ? "minecraft:spider" : "minecraft:zombie";
        world.setSpawnerMob(origin.x, origin.y, origin.z, entity);
        return true;
    }

    private static String orientedChest(World world, Pos position) {
        int onlySolid = -1;
        for (int direction = 0; direction < HORIZONTAL.length; direction++) {
            int[] delta = HORIZONTAL[direction];
            Pos neighbor = position.offset(delta[0], 0, delta[2]);
            if ("minecraft:chest"
                .equals(ModernDecorationEngine.blockName(world.blockState(neighbor.x, neighbor.y, neighbor.z)))) {
                return chestState("north");
            }
            if (!world.isSolid(neighbor.x, neighbor.y, neighbor.z)) continue;
            if (onlySolid >= 0) {
                onlySolid = -1;
                break;
            }
            onlySolid = direction;
        }
        if (onlySolid >= 0) return chestState(NAMES[(onlySolid + 2) & 3]);

        int facing = 0;
        int[] first = HORIZONTAL[facing];
        if (world.isSolid(position.x + first[0], position.y, position.z + first[2])) {
            facing = (facing + 2) & 3;
        }
        int[] second = HORIZONTAL[facing];
        if (world.isSolid(position.x + second[0], position.y, position.z + second[2])) {
            facing = (facing + 1) & 3;
        }
        int[] third = HORIZONTAL[facing];
        if (world.isSolid(position.x + third[0], position.y, position.z + third[2])) {
            facing = (facing + 2) & 3;
        }
        return chestState(NAMES[facing]);
    }

    private static String chestState(String facing) {
        return "minecraft:chest[facing=" + facing + ",type=single,waterlogged=false]";
    }

    private static boolean isAir(World world, Pos position) {
        String block = ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
        return "minecraft:air".equals(block) || "minecraft:cave_air".equals(block)
            || "minecraft:void_air".equals(block);
    }

    private static void safeSet(ModernDecorationEngine engine, World world, Pos position, String state) {
        if (!engine
            .isBlockInTag("#minecraft:features_cannot_replace", world.blockState(position.x, position.y, position.z))) {
            world.setBlock(position.x, position.y, position.z, state);
        }
    }
}
