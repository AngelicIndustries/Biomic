package industries.angelic.biomic.world.terrain;

/** Stage ownership for native cave ecology; geometry and shared ore/fluid safety remain Origen-owned. */
final class HostedCaveEcology {

    private HostedCaveEcology() {}

    static boolean protectedStructureState(String state) {
        String block = industries.angelic.biomic.world.modern.ModernDecorationEngine.blockName(state);
        return block.endsWith("_planks") || block.endsWith("_fence")
            || block.endsWith("_stairs")
            || block.endsWith("_door")
            || block.endsWith("_slab")
            || block.endsWith("_rail")
            || block.endsWith("_button")
            || block.contains("stone_brick")
            || switch (block) {
            case "minecraft:rail", "minecraft:chest", "minecraft:spawner", "minecraft:iron_bars", "minecraft:web", "minecraft:cobweb", "minecraft:bookshelf", "minecraft:torch", "minecraft:end_portal", "minecraft:end_portal_frame" -> true;
            default -> false;
            };
    }

    static boolean replaces(String stage, String feature) {
        return switch (stage) {
            case "trees", "flora", "underwater-flora", "preprocessors", "landforms", "postprocessors" -> true;
            case "global-preprocessors" -> "CAVE_GLOW_LICHEN".equals(feature);
            default -> false;
        };
    }
}
