package industries.angelic.biomic.world.modern;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernLegacyRandom;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Direct port of RU's Natural-Philosophy-derived carved limited pool. */
final class ModernRuCarvedPoolFeature {

    private final ModernFeatureEngine features;

    ModernRuCarvedPoolFeature(ModernFeatureEngine features) {
        this.features = features;
    }

    boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
        int maximumDepth = 1;
        for (int depth = 0; depth < maximumDepth; depth++) {
            boolean[][] water = new boolean[18][18];
            for (int x = -1; x < 17; x++) {
                for (int z = -1; z < 17; z++) {
                    int surfaceY = world.height("WORLD_SURFACE", origin.x + x, origin.z + z) - 1;
                    world.recordPlacementCandidate(
                        new Pos(origin.x + x, surfaceY, origin.z + z),
                        -7,
                        "pool_height=WORLD_SURFACE;state=" + world.blockState(origin.x + x, surfaceY, origin.z + z));
                    if (depth == 0 && isWater(world, origin.x + x, surfaceY, origin.z + z)) {
                        water[x + 1][z + 1] = true;
                    }
                    int floorY = world.height("OCEAN_FLOOR", origin.x + x, origin.z + z) - 1;
                    Pos floor = new Pos(origin.x + x, floorY, origin.z + z);
                    world.recordPlacementCandidate(
                        floor,
                        -7,
                        "pool_height=OCEAN_FLOOR;state=" + world.blockState(floor.x, floor.y, floor.z));
                    if (!engine.testPredicate(config.getAsJsonObject("pool_allowed"), world, floor)) {
                        continue;
                    }
                    if (!engine.testPredicate(config.getAsJsonObject("wall_allowed"), world, floor.offset(0, -1, 0)))
                        continue;
                    boolean open = true;
                    for (int aroundX = -1; aroundX <= 1 && open; aroundX++) {
                        for (int aroundZ = -1; aroundZ <= 1; aroundZ++) {
                            Pos above = floor.offset(aroundX, 1, aroundZ);
                            if (!isAirOrWater(world, above.x, above.y, above.z)) {
                                open = false;
                                break;
                            }
                        }
                    }
                    if (!open) continue;
                    boolean walled = true;
                    for (int aroundX = -1; aroundX <= 1 && walled; aroundX++) {
                        for (int aroundZ = -1; aroundZ <= 1; aroundZ++) {
                            Pos side = floor.offset(aroundX, 0, aroundZ);
                            if (!engine.testPredicate(config.getAsJsonObject("wall_allowed"), world, side)
                                && !isWater(world, side.x, side.y, side.z)) {
                                walled = false;
                                break;
                            }
                        }
                    }
                    if (!walled) continue;
                    world.setBlock(floor.x, floor.y, floor.z, "minecraft:water[level=0]");
                    water[x + 1][z + 1] = true;
                    // The pinned source never flips increasedDepth, so every successful cell
                    // can grow the loop bound until the configured cap is reached.
                    if (maximumDepth < config.get("depth")
                        .getAsInt()) maximumDepth++;
                }
            }
            if (depth != 0) continue;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    boolean adjacentWater = false;
                    for (int aroundX = -1; aroundX <= 1 && !adjacentWater; aroundX++) {
                        for (int aroundZ = -1; aroundZ <= 1; aroundZ++) {
                            if (water[x + 1 + aroundX][z + 1 + aroundZ]) {
                                adjacentWater = true;
                                break;
                            }
                        }
                    }
                    if (adjacentWater) continue;
                    for (int down = 1; down <= engine.sampleIntValue(config.get("slopeDepth"), random) + 1; down++) {
                        int y = world.height("WORLD_SURFACE", origin.x + x, origin.z + z) - down;
                        Pos position = new Pos(origin.x + x, y, origin.z + z);
                        if (!engine.testPredicate(config.getAsJsonObject("wall_allowed"), world, position)) break;
                        JsonObject provider = config.getAsJsonObject(down == 1 ? "slopeTop" : "slope");
                        // RU deliberately creates a fresh RandomSource for each slope block.
                        String state = features.stateProvider(
                            engine,
                            provider,
                            new ModernLegacyRandom(new java.util.Random().nextLong()),
                            position);
                        world.setBlock(position.x, position.y, position.z, state);
                    }
                }
            }
        }
        return true;
    }

    private static boolean isAirOrWater(World world, int x, int y, int z) {
        String block = ModernDecorationEngine.blockName(world.blockState(x, y, z));
        return "minecraft:air".equals(block) || "minecraft:water".equals(block);
    }

    private static boolean isWater(World world, int x, int y, int z) {
        return "minecraft:water".equals(ModernDecorationEngine.blockName(world.blockState(x, y, z)))
            || "minecraft:water".equals(world.fluidState(x, y, z));
    }
}
