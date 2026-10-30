package industries.angelic.biomic.world.modern;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Exact small surface features from the pinned Minecraft 1.21.1 target. */
final class ModernVanillaSurfaceFeatures {

    private static final int[][] HORIZONTAL = { { 0, 0, -1 }, { 1, 0, 0 }, { 0, 0, 1 }, { -1, 0, 0 } };
    private static final String SANDSTONE_SLAB = "minecraft:sandstone_slab[type=bottom,waterlogged=false]";

    boolean placeDesertWell(World world, ModernRandom random, Pos suppliedOrigin) {
        Pos origin = suppliedOrigin.offset(0, 1, 0);
        while (isAir(world, origin) && origin.y > world.minBuildHeight() + 2) {
            origin = origin.offset(0, -1, 0);
        }
        if (!"minecraft:sand".equals(block(world, origin))) return false;

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (isAir(world, origin.offset(x, -1, z)) && isAir(world, origin.offset(x, -2, z))) return false;
            }
        }

        for (int y = -2; y <= 0; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    set(world, origin.offset(x, y, z), "minecraft:sandstone");
                }
            }
        }
        set(world, origin, "minecraft:water[level=0]");
        for (int[] direction : HORIZONTAL) {
            set(world, origin.offset(direction[0], 0, direction[2]), "minecraft:water[level=0]");
        }

        Pos below = origin.offset(0, -1, 0);
        set(world, below, "minecraft:sand");
        for (int[] direction : HORIZONTAL) {
            set(world, below.offset(direction[0], 0, direction[2]), "minecraft:sand");
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x == -2 || x == 2 || z == -2 || z == 2) {
                    set(world, origin.offset(x, 1, z), "minecraft:sandstone");
                }
            }
        }
        set(world, origin.offset(2, 1, 0), SANDSTONE_SLAB);
        set(world, origin.offset(-2, 1, 0), SANDSTONE_SLAB);
        set(world, origin.offset(0, 1, 2), SANDSTONE_SLAB);
        set(world, origin.offset(0, 1, -2), SANDSTONE_SLAB);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                set(world, origin.offset(x, 4, z), x == 0 && z == 0 ? "minecraft:sandstone" : SANDSTONE_SLAB);
            }
        }
        for (int y = 1; y <= 3; y++) {
            set(world, origin.offset(-1, y, -1), "minecraft:sandstone");
            set(world, origin.offset(-1, y, 1), "minecraft:sandstone");
            set(world, origin.offset(1, y, -1), "minecraft:sandstone");
            set(world, origin.offset(1, y, 1), "minecraft:sandstone");
        }

        Pos[] suspiciousCandidates = { origin, origin.offset(1, 0, 0), origin.offset(0, 0, 1), origin.offset(-1, 0, 0),
            origin.offset(0, 0, -1) };
        set(
            world,
            suspiciousCandidates[random.nextInt(suspiciousCandidates.length)].offset(0, -1, 0),
            "minecraft:suspicious_sand[dusted=0]");
        set(
            world,
            suspiciousCandidates[random.nextInt(suspiciousCandidates.length)].offset(0, -2, 0),
            "minecraft:suspicious_sand[dusted=0]");
        return true;
    }

    boolean placeIceSpike(World world, ModernRandom random, Pos suppliedOrigin) {
        Pos origin = suppliedOrigin;
        while (isAir(world, origin) && origin.y > world.minBuildHeight() + 2) {
            origin = origin.offset(0, -1, 0);
        }
        if (!"minecraft:snow_block".equals(block(world, origin))) return false;

        origin = origin.offset(0, random.nextInt(4), 0);
        int height = random.nextInt(4) + 7;
        int radius = height / 4 + random.nextInt(2);
        if (radius > 1 && random.nextInt(60) == 0) {
            origin = origin.offset(0, 10 + random.nextInt(30), 0);
        }

        for (int y = 0; y < height; y++) {
            float layerRadius = (1.0F - (float) y / height) * radius;
            int extent = (int) Math.ceil(layerRadius);
            for (int x = -extent; x <= extent; x++) {
                float dx = Math.abs(x) - 0.25F;
                for (int z = -extent; z <= extent; z++) {
                    float dz = Math.abs(z) - 0.25F;
                    if (((x != 0 || z != 0) && dx * dx + dz * dz > layerRadius * layerRadius)
                        || ((x == -extent || x == extent || z == -extent || z == extent) && random.nextFloat() > 0.75F))
                        continue;
                    Pos upper = origin.offset(x, y, z);
                    if (iceSpikeReplaceable(world, upper)) {
                        set(world, upper, "minecraft:packed_ice");
                    }
                    if (y == 0 || extent <= 1) continue;
                    Pos lower = origin.offset(x, -y, z);
                    if (iceSpikeReplaceable(world, lower)) {
                        set(world, lower, "minecraft:packed_ice");
                    }
                }
            }
        }

        int rootRadius = Math.max(0, Math.min(1, radius - 1));
        for (int x = -rootRadius; x <= rootRadius; x++) {
            for (int z = -rootRadius; z <= rootRadius; z++) {
                Pos cursor = origin.offset(x, -1, z);
                int remaining = Math.abs(x) == 1 && Math.abs(z) == 1 ? random.nextInt(5) : 50;
                while (cursor.y > 50 && iceSpikeReplaceable(world, cursor)) {
                    set(world, cursor, "minecraft:packed_ice");
                    cursor = cursor.offset(0, -1, 0);
                    if (--remaining > 0) continue;
                    cursor = cursor.offset(0, -(random.nextInt(5) + 1), 0);
                    remaining = random.nextInt(5);
                }
            }
        }
        return true;
    }

    boolean placeLake(ModernFeatureEngine features, ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, Pos suppliedOrigin) {
        if (suppliedOrigin.y <= world.minBuildHeight() + 4) return false;
        Pos origin = suppliedOrigin.offset(0, -4, 0);
        boolean[] carved = new boolean[16 * 16 * 8];
        int ellipsoids = random.nextInt(4) + 4;
        for (int index = 0; index < ellipsoids; index++) {
            double sizeX = random.nextDouble() * 6.0D + 3.0D;
            double sizeY = random.nextDouble() * 4.0D + 2.0D;
            double sizeZ = random.nextDouble() * 6.0D + 3.0D;
            double centerX = random.nextDouble() * (16.0D - sizeX - 2.0D) + 1.0D + sizeX / 2.0D;
            double centerY = random.nextDouble() * (8.0D - sizeY - 4.0D) + 2.0D + sizeY / 2.0D;
            double centerZ = random.nextDouble() * (16.0D - sizeZ - 2.0D) + 1.0D + sizeZ / 2.0D;
            for (int x = 1; x < 15; x++) {
                for (int z = 1; z < 15; z++) {
                    for (int y = 1; y < 7; y++) {
                        double dx = (x - centerX) / (sizeX / 2.0D);
                        double dy = (y - centerY) / (sizeY / 2.0D);
                        double dz = (z - centerZ) / (sizeZ / 2.0D);
                        if (dx * dx + dy * dy + dz * dz < 1.0D) {
                            carved[lakeIndex(x, z, y)] = true;
                        }
                    }
                }
            }
        }

        String fluid = features.stateProvider(engine, config.getAsJsonObject("fluid"), random, origin);
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 8; y++) {
                    if (!lakeBoundary(carved, x, z, y)) continue;
                    String current = world.blockState(origin.x + x, origin.y + y, origin.z + z);
                    String block = ModernDecorationEngine.blockName(current);
                    if (y >= 4 && ("minecraft:water".equals(block) || "minecraft:lava".equals(block))) return false;
                    if (y < 4 && !world.isSolid(origin.x + x, origin.y + y, origin.z + z)
                        && !ModernDecorationEngine.blockName(fluid)
                            .equals(block))
                        return false;
                }
            }
        }

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < 8; y++) {
                    if (!carved[lakeIndex(x, z, y)]) continue;
                    Pos target = origin.offset(x, y, z);
                    if (engine.isBlockInTag(
                        "minecraft:features_cannot_replace",
                        world.blockState(target.x, target.y, target.z))) continue;
                    set(world, target, y >= 4 ? "minecraft:cave_air" : fluid);
                }
            }
        }

        String barrier = features.stateProvider(engine, config.getAsJsonObject("barrier"), random, origin);
        if (!"minecraft:air".equals(ModernDecorationEngine.blockName(barrier))) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < 8; y++) {
                        if (!lakeBoundary(carved, x, z, y) || (y >= 4 && random.nextInt(2) == 0)) continue;
                        Pos target = origin.offset(x, y, z);
                        if (!world.isSolid(target.x, target.y, target.z) || engine.isBlockInTag(
                            "minecraft:lava_pool_stone_cannot_replace",
                            world.blockState(target.x, target.y, target.z))) continue;
                        set(world, target, barrier);
                    }
                }
            }
        }

        if ("minecraft:water".equals(ModernDecorationEngine.blockName(fluid))) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    Pos target = origin.offset(x, 4, z);
                    if (world.shouldFreeze(target.x, target.y, target.z, false) && !engine.isBlockInTag(
                        "minecraft:features_cannot_replace",
                        world.blockState(target.x, target.y, target.z))) {
                        set(world, target, "minecraft:ice");
                    }
                }
            }
        }
        return true;
    }

    private static int lakeIndex(int x, int z, int y) {
        return (x * 16 + z) * 8 + y;
    }

    private static boolean lakeBoundary(boolean[] carved, int x, int z, int y) {
        if (carved[lakeIndex(x, z, y)]) return false;
        return x < 15 && carved[lakeIndex(x + 1, z, y)] || x > 0 && carved[lakeIndex(x - 1, z, y)]
            || z < 15 && carved[lakeIndex(x, z + 1, y)]
            || z > 0 && carved[lakeIndex(x, z - 1, y)]
            || y < 7 && carved[lakeIndex(x, z, y + 1)]
            || y > 0 && carved[lakeIndex(x, z, y - 1)];
    }

    private static boolean iceSpikeReplaceable(World world, Pos position) {
        String block = block(world, position);
        return "minecraft:air".equals(block) || isDirt(block)
            || "minecraft:snow_block".equals(block)
            || "minecraft:ice".equals(block)
            || "minecraft:packed_ice".equals(block);
    }

    private static boolean isDirt(String block) {
        return "minecraft:dirt".equals(block) || "minecraft:grass_block".equals(block)
            || "minecraft:coarse_dirt".equals(block)
            || "minecraft:podzol".equals(block)
            || "minecraft:mycelium".equals(block)
            || "minecraft:rooted_dirt".equals(block);
    }

    private static boolean isAir(World world, Pos position) {
        return "minecraft:air".equals(block(world, position));
    }

    private static String block(World world, Pos position) {
        return ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
    }

    private static void set(World world, Pos position, String state) {
        world.setBlock(position.x, position.y, position.z, state);
    }
}
