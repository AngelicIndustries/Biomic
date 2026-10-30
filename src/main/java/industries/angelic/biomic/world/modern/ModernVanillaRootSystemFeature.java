package industries.angelic.biomic.world.modern;

import com.google.gson.JsonObject;

import industries.angelic.biomic.world.modern.ModernDecorationEngine.Pos;
import industries.angelic.biomic.world.modern.ModernDecorationEngine.World;
import industries.angelic.biomic.world.modern.noise.ModernRandom;

/** Exact control flow of Minecraft 1.21.1's {@code RootSystemFeature}. */
final class ModernVanillaRootSystemFeature {

    private final ModernFeatureEngine features;

    ModernVanillaRootSystemFeature(ModernFeatureEngine features) {
        this.features = features;
    }

    boolean place(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random, Pos origin) {
        if (!isAir(world.blockState(origin.x, origin.y, origin.z))) return false;
        Pos treePosition = placeDirtAndTree(engine, config, world, random, origin);
        if (treePosition != null) placeHangingRoots(engine, config, world, random, origin);
        return true;
    }

    private Pos placeDirtAndTree(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int maximumHeight = config.get("root_column_max_height")
            .getAsInt();
        Pos cursor = origin;
        for (int offset = 0; offset < maximumHeight; offset++) {
            cursor = cursor.offset(0, 1, 0);
            if (!engine.testPredicate(config.getAsJsonObject("allowed_tree_position"), world, cursor)
                || !spaceForTree(config, world, cursor)) continue;
            Pos below = cursor.offset(0, -1, 0);
            if (isLava(world.fluidState(below.x, below.y, below.z)) || !world.isSolid(below.x, below.y, below.z))
                return null;
            if (!engine.place(config.get("feature"), null, world, random, cursor)) continue;
            placeRootedDirt(engine, config, world, random, origin, origin.y + offset);
            return cursor;
        }
        return null;
    }

    private static boolean spaceForTree(JsonObject config, World world, Pos position) {
        int requiredSpace = config.get("required_vertical_space_for_tree")
            .getAsInt();
        int allowedWater = config.get("allowed_vertical_water_for_tree")
            .getAsInt();
        for (int offset = 1; offset <= requiredSpace; offset++) {
            String state = world.blockState(position.x, position.y + offset, position.z);
            if (isAir(state)) continue;
            if (offset + 1 <= allowedWater && isWater(world.fluidState(position.x, position.y + offset, position.z)))
                continue;
            return false;
        }
        return true;
    }

    private void placeRootedDirt(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin, int upperExclusive) {
        for (int y = origin.y; y < upperExclusive; y++) {
            placeRootedDirtLayer(engine, config, world, random, new Pos(origin.x, y, origin.z));
        }
    }

    private void placeRootedDirtLayer(ModernDecorationEngine engine, JsonObject config, World world,
        ModernRandom random, Pos center) {
        int radius = config.get("root_radius")
            .getAsInt();
        int attempts = config.get("root_placement_attempts")
            .getAsInt();
        String replaceable = config.get("root_replaceable")
            .getAsString();
        JsonObject provider = config.getAsJsonObject("root_state_provider");
        for (int attempt = 0; attempt < attempts; attempt++) {
            Pos position = center.offset(
                random.nextInt(radius) - random.nextInt(radius),
                0,
                random.nextInt(radius) - random.nextInt(radius));
            if (engine.isBlockInTag(replaceable, world.blockState(position.x, position.y, position.z))) {
                world.setBlock(
                    position.x,
                    position.y,
                    position.z,
                    features.stateProvider(engine, provider, random, position));
            }
        }
    }

    private void placeHangingRoots(ModernDecorationEngine engine, JsonObject config, World world, ModernRandom random,
        Pos origin) {
        int radius = config.get("hanging_root_radius")
            .getAsInt();
        int verticalSpan = config.get("hanging_roots_vertical_span")
            .getAsInt();
        int attempts = config.get("hanging_root_placement_attempts")
            .getAsInt();
        JsonObject provider = config.getAsJsonObject("hanging_root_state_provider");
        for (int attempt = 0; attempt < attempts; attempt++) {
            Pos position = origin.offset(
                random.nextInt(radius) - random.nextInt(radius),
                random.nextInt(verticalSpan) - random.nextInt(verticalSpan),
                random.nextInt(radius) - random.nextInt(radius));
            if (!isAir(world.blockState(position.x, position.y, position.z))) continue;
            String state = features.stateProvider(engine, provider, random, position);
            if (!world.canSurvive(state, position.x, position.y, position.z)
                || !world.hasSturdyFace(position.x, position.y + 1, position.z, "down")) {
                continue;
            }
            world.setBlock(position.x, position.y, position.z, state);
        }
    }

    private static boolean isAir(String state) {
        String block = ModernDecorationEngine.blockName(state);
        return "minecraft:air".equals(block) || "minecraft:cave_air".equals(block)
            || "minecraft:void_air".equals(block);
    }

    private static boolean isWater(String state) {
        return "minecraft:water".equals(ModernDecorationEngine.blockName(state));
    }

    private static boolean isLava(String state) {
        return "minecraft:lava".equals(ModernDecorationEngine.blockName(state));
    }
}
