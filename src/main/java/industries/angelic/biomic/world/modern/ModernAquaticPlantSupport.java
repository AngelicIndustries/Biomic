package industries.angelic.biomic.world.modern;

/** Pinned 1.21.1 survival, separate from the feature's initial water/replacement checks. */
public final class ModernAquaticPlantSupport {

    private ModernAquaticPlantSupport() {}

    public static boolean handles(String plant) {
        String name = ModernDecorationEngine.blockName(plant);
        return "minecraft:seagrass".equals(name) || "minecraft:tall_seagrass".equals(name)
            || "minecraft:kelp".equals(name)
            || "minecraft:kelp_plant".equals(name);
    }

    public static Boolean test(String plant, String below, String target, boolean sturdyUp) {
        String name = ModernDecorationEngine.blockName(plant);
        String support = ModernDecorationEngine.blockName(below);
        // A water-filled RU plant is not a seabed, even in an adapter which
        // approximates unknown block collision as a cube. Keep this invariant
        // independent of the caller's sturdy-face implementation.
        if (industries.angelic.biomic.content.RuCaveModelData.alwaysWaterFilled(support)) return false;
        if ("minecraft:kelp".equals(name) || "minecraft:kelp_plant".equals(name)) {
            return !"minecraft:magma_block".equals(support)
                && (sturdyUp || "minecraft:kelp".equals(support) || "minecraft:kelp_plant".equals(support));
        }
        if (!"minecraft:seagrass".equals(name) && !"minecraft:tall_seagrass".equals(name)) return null;
        if (plant.contains("half=upper")) {
            return below.startsWith("minecraft:tall_seagrass[") && below.contains("half=lower");
        }
        if (!sturdyUp || "minecraft:magma_block".equals(ModernDecorationEngine.blockName(below))) return false;
        // Short seagrass inherits VegetationBlock's substrate-only survival. Tall lower halves
        // additionally demand a full water fluid; its upper-half placement is checked by the feature.
        if ("minecraft:seagrass".equals(name)) return true;
        if (!"minecraft:water".equals(ModernBlockSemantics.fluidState(target))) return false;
        if (!"minecraft:water".equals(ModernDecorationEngine.blockName(target))) return true;
        int begin = target.indexOf("level=");
        if (begin < 0) return true;
        int end = target.indexOf(',', begin);
        int level = Integer.parseInt(target.substring(begin + 6, end < 0 ? target.length() - 1 : end));
        return level == 0 || level >= 8;
    }

    public static boolean cattail(String plant, String below, String above, boolean taggedSoil) {
        if (plant.contains("half=upper"))
            return below.startsWith("regions_unexplored:cattail[") && below.contains("half=lower");
        // RU tests the exact source-fluid type above the LOWER half (the soil position + 2).
        return taggedSoil && !sourceWater(above);
    }

    public static boolean sourceWater(String state) {
        return "minecraft:water".equals(ModernBlockSemantics.fluidState(state))
            && (!"minecraft:water".equals(ModernDecorationEngine.blockName(state)) || !state.contains("level=")
                || state.contains("level=0"));
    }

    public static boolean floweringLily(String below, String target) {
        // RU intentionally also accepts submerged lilies when their support has any water fluid.
        return "minecraft:water".equals(ModernBlockSemantics.fluidState(below)) || ModernBlockSemantics
            .canDuckweedSurvive(below, ModernBlockSemantics.fluidState(below), ModernBlockSemantics.fluidState(target));
    }
}
