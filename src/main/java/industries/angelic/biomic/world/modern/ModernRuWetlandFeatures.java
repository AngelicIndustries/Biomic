package industries.angelic.biomic.world.modern;

import java.util.Random;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;
import industries.angelic.biomic.world.replay.RuIncidentalRandom;

/** Direct ports of pinned RU's unconfigured marsh, water-edge and prismarite features. */
final class ModernRuWetlandFeatures {

    boolean place(String type, World world, ModernRandom ignored, Pos origin) {
        if ("regions_unexplored:water_edge".equals(type)) return waterEdge(world, origin);
        if ("regions_unexplored:marsh".equals(type)) return marsh(world, origin);
        if ("regions_unexplored:hanging_prismarite".equals(type)) {
            return hangingPrismarite(world, origin);
        }
        throw new IllegalArgumentException("Unsupported pinned RU wetland feature " + type);
    }

    private boolean waterEdge(World world, Pos origin) {
        if (!world.hasSturdyFace(origin.x, origin.y - 1, origin.z, "up")) return false;
        blob(world, origin, new PointVisitor() {

            @Override
            public void visit(World target, Random random, Pos position) {
                if (position.y == target.seaLevel() - 1
                    && target.hasSturdyFace(position.x, position.y - 1, position.z, "down")
                    && random.nextInt(5) == 0) {
                    placeWaterEdgeBlock(target, position);
                }
            }
        });
        return true;
    }

    private void placeWaterEdgeBlock(World world, Pos position) {
        if (!"minecraft:dirt".equals(block(world, position.offset(0, -1, 0))) || !isWater(world, position)) return;
        world.setBlock(position.x, position.y, position.z, "minecraft:grass_block[snowy=false]");
        Pos above = position.offset(0, 1, 0);
        String state = world.blockState(above.x, above.y, above.z);
        if ("regions_unexplored:cattail[half=upper]".equals(state)
            || "regions_unexplored:duckweed".equals(block(world, above))) {
            world.setBlock(above.x, above.y, above.z, "minecraft:air");
        }
    }

    private boolean marsh(World world, Pos origin) {
        blob(world, origin, new PointVisitor() {

            @Override
            public void visit(World target, Random random, Pos position) {
                if (position.y == target.seaLevel() - 1) placeMarshBlocks(target, position);
            }
        });
        return true;
    }

    private void placeMarshBlocks(World world, Pos position) {
        int oceanFloor = world.height("OCEAN_FLOOR", position.x, position.z) - 1;
        if ("minecraft:mud".equals(block(world, new Pos(position.x, oceanFloor - 1, position.z)))
            && isWater(world, position)) return;

        Random random = RuIncidentalRandom
            .random(world.worldSeed(), position.x, position.y, position.z, RuIncidentalRandom.MARSH_BLOCK);
        int chance = random.nextInt(5);
        int grassChance = random.nextInt(4);
        for (int depth = 1; depth <= 5; depth++) {
            if (isAir(world, position.offset(0, -depth, 0))) return;
        }
        if (chance > 3 || isWater(world, position.offset(0, -5, 0)) || !isWater(world, position)) return;

        if (chance == 0) {
            world.setBlock(position.x, position.y, position.z, "minecraft:grass_block[snowy=false]");
            Pos above = position.offset(0, 1, 0);
            String aboveBlock = block(world, above);
            if ("minecraft:lily_pad".equals(aboveBlock) || "regions_unexplored:duckweed".equals(aboveBlock)
                || "regions_unexplored:flowering_lily_pad".equals(aboveBlock)) {
                world.setBlock(above.x, above.y, above.z, "minecraft:air");
            }
            if (isAir(world, above) && isAir(world, position.offset(0, 2, 0))) {
                if (grassChance <= 1) {
                    world.setBlock(above.x, above.y, above.z, "minecraft:short_grass");
                } else if (grassChance == 2) {
                    world.setBlock(above.x, above.y, above.z, "minecraft:tall_grass[half=lower]");
                    if ("minecraft:tall_grass[half=lower]".equals(world.blockState(above.x, above.y, above.z))) {
                        Pos upper = position.offset(0, 2, 0);
                        world.setBlock(upper.x, upper.y, upper.z, "minecraft:tall_grass[half=upper]");
                    }
                }
            }
            fillWaterWithDirt(world, position, 1, 4);
        } else if (chance == 1) {
            fillWaterWithDirt(world, position, 1, 4);
        } else if (chance == 2) {
            Pos below = position.offset(0, -1, 0);
            if (isAir(world, below)) {
                world.setBlock(below.x, below.y, below.z, "minecraft:water[level=0]");
            }
            fillWaterWithDirt(world, position, 2, 4);
        } else {
            for (int depth = 1; depth <= 2; depth++) {
                Pos below = position.offset(0, -depth, 0);
                if (isAir(world, below)) {
                    world.setBlock(below.x, below.y, below.z, "minecraft:water[level=0]");
                }
            }
            fillWaterWithDirt(world, position, 3, 4);
        }
    }

    private void fillWaterWithDirt(World world, Pos origin, int first, int last) {
        for (int depth = first; depth <= last; depth++) {
            Pos below = origin.offset(0, -depth, 0);
            if (isWater(world, below)) {
                world.setBlock(below.x, below.y, below.z, "minecraft:dirt");
            }
        }
    }

    private boolean hangingPrismarite(World world, Pos origin) {
        if (!world.hasSturdyFace(origin.x, origin.y + 1, origin.z, "down")) return false;
        blob(world, origin, new PointVisitor() {

            @Override
            public void visit(World target, Random random, Pos position) {
                if (target.hasSturdyFace(position.x, position.y + 1, position.z, "down") && random.nextInt(10) == 0)
                    placePrismaritePillar(target, position);
            }
        });
        return true;
    }

    private void placePrismaritePillar(World world, Pos origin) {
        int size = RuIncidentalRandom
            .sample(world.worldSeed(), origin.x, origin.y, origin.z, RuIncidentalRandom.PRISMARITE_PILLAR, 5) + 1;
        if (!isAir(world, origin)) return;
        Pos above = origin.offset(0, 1, 0);
        if (!world.hasSturdyFace(above.x, above.y, above.z, "down")
            && !"regions_unexplored:hanging_prismarite".equals(block(world, above))) return;
        Pos position = origin;
        int count = 0;
        while (isAir(world, position)) {
            if (count == size || position.y < world.minBuildHeight() || position.y >= world.maxBuildHeight()) return;
            Pos oneAbove = position.offset(0, 1, 0);
            Pos twoAbove = position.offset(0, 2, 0);
            if ("regions_unexplored:hanging_prismarite".equals(block(world, oneAbove))) {
                world.setBlock(oneAbove.x, oneAbove.y, oneAbove.z, prismarite("frustum"));
            }
            if ("regions_unexplored:hanging_prismarite".equals(block(world, twoAbove))) {
                world.setBlock(twoAbove.x, twoAbove.y, twoAbove.z, prismarite("column"));
            }
            world.setBlock(position.x, position.y, position.z, prismarite("tip"));
            position = position.offset(0, -1, 0);
            count++;
        }
    }

    private void blob(World world, Pos start, PointVisitor visitor) {
        if (start.y <= world.minBuildHeight() + 3) return;
        Random random = RuIncidentalRandom
            .random(world.worldSeed(), start.x, start.y, start.z, RuIncidentalRandom.WETLAND_BLOB);
        Pos center = start;
        for (int pass = 0; pass < 3; pass++) {
            int radiusX = random.nextInt(4) + 2;
            int radiusY = random.nextInt(4) + 2;
            int radiusZ = random.nextInt(4) + 2;
            float radius = (radiusX + radiusY + radiusZ) * 0.333F + 0.5F;
            double radiusSquared = radius * radius;
            // BlockPos.betweenClosed advances X first, then Y, then Z.
            for (int z = center.z - radiusZ; z <= center.z + radiusZ; z++) {
                for (int y = center.y - radiusY; y <= center.y + radiusY; y++) {
                    for (int x = center.x - radiusX; x <= center.x + radiusX; x++) {
                        long dx = x - center.x;
                        long dy = y - center.y;
                        long dz = z - center.z;
                        if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                            visitor.visit(world, random, new Pos(x, y, z));
                        }
                    }
                }
            }
            center = center.offset(-1 + random.nextInt(2), -random.nextInt(2), -1 + random.nextInt(2));
        }
    }

    private static String prismarite(String shape) {
        return "regions_unexplored:hanging_prismarite[hanging_prismarite_shape=" + shape + "]";
    }

    private static String block(World world, Pos position) {
        return ModernDecorationEngine.blockName(world.blockState(position.x, position.y, position.z));
    }

    private static boolean isAir(World world, Pos position) {
        return "minecraft:air".equals(block(world, position));
    }

    private static boolean isWater(World world, Pos position) {
        return "minecraft:water".equals(block(world, position)) || "minecraft:water"
            .equals(ModernDecorationEngine.blockName(world.fluidState(position.x, position.y, position.z)));
    }

    private interface PointVisitor {

        void visit(World world, Random random, Pos position);
    }
}
