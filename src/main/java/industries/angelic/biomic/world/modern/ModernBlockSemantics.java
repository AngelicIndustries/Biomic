package industries.angelic.biomic.world.modern;

import net.minecraft.block.BlockLeaves;

/** Modern block-state predicates that differ materially from their 1.7 representations. */
public final class ModernBlockSemantics {

    private ModernBlockSemantics() {}

    /** A pointed rock's material is solid, but its top is not a snow/flower support face. */
    public static boolean legacyFullFace(net.minecraft.block.Block block) {
        if (block instanceof industries.angelic.biomic.content.block.BlockIcicle) return false;
        if (block instanceof industries.angelic.biomic.content.block.BlockRuCaveModel cave) return cave.isOpaqueCube();
        return block instanceof BlockLeaves || block.getMaterial()
            .isSolid();
    }

    /** Modern FluidState exposed by blocks whose water is implicit in their block type. */
    public static String fluidState(String modernState) {
        String block = ModernDecorationEngine.blockName(modernState);
        if ("minecraft:water".equals(block) || modernState.contains("waterlogged=true")
            || "minecraft:seagrass".equals(block)
            || "minecraft:tall_seagrass".equals(block)
            || "minecraft:kelp".equals(block)
            || "minecraft:kelp_plant".equals(block)
            || "minecraft:bubble_column".equals(block)) {
            return "minecraft:water";
        }
        if (industries.angelic.biomic.content.RuCaveModelData.alwaysWaterFilled(block)) return "minecraft:water";
        if ("minecraft:lava".equals(block)) return "minecraft:lava";
        return "minecraft:empty";
    }

    /** Exact support/fluid rule inherited by RU DuckweedBlock from WaterlilyBlock. */
    public static boolean canDuckweedSurvive(String supportState, String supportFluid, String currentFluid) {
        String supportBlock = ModernDecorationEngine.blockName(supportState);
        String supportFluidBlock = ModernDecorationEngine.blockName(supportFluid);
        String currentFluidBlock = ModernDecorationEngine.blockName(currentFluid);
        boolean waterOrIce = "minecraft:water".equals(supportFluidBlock)
            // These blocks always return a water FluidState in modern Minecraft even though
            // their serialized BlockState has no waterlogged property to preserve.
            || "minecraft:seagrass".equals(supportBlock)
            || "minecraft:tall_seagrass".equals(supportBlock)
            || "minecraft:kelp".equals(supportBlock)
            || "minecraft:kelp_plant".equals(supportBlock)
            || "minecraft:ice".equals(supportBlock)
            || supportBlock.endsWith("_ice");
        return waterOrIce && !"minecraft:water".equals(currentFluidBlock);
    }
}
