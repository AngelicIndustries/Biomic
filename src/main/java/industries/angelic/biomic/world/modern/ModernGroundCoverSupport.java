package industries.angelic.biomic.world.modern;

import java.util.function.BiPredicate;

/**
 * Source-tag substrate rules shared by physical hosting, replay fixtures and settlement.
 * Null means this is not a simple substrate-only rule (e.g. vines, water plants, mushrooms).
 */
public final class ModernGroundCoverSupport {

    private ModernGroundCoverSupport() {}

    public static boolean isNetherPlant(String state) {
        return switch (ModernDecorationEngine.blockName(state)) {
            case "regions_unexplored:brimsprout", "regions_unexplored:dorcel", "regions_unexplored:brimwood_shrub", "regions_unexplored:cobalt_roots", "regions_unexplored:cobalt_earlight", "regions_unexplored:tall_cobalt_earlight", "regions_unexplored:glistering_sprout", "regions_unexplored:glistering_fern", "regions_unexplored:glistering_bloom", "regions_unexplored:glister_bulb", "regions_unexplored:glister_spire", "regions_unexplored:mycotoxic_mushrooms", "regions_unexplored:mycotoxic_daisy", "regions_unexplored:mycotoxic_grass" -> true;
            default -> false;
        };
    }

    /** Physical RU blocks use the same source substrate rules as generation, without legacy light gates. */
    public static Boolean testPinned(String state, String below) {
        return test(
            state,
            below,
            (tag, value) -> Pinned.GRAPH.blockTag(tag)
                .contains(ModernDecorationEngine.blockName(value)));
    }

    private static final class Pinned {

        private static final ModernDecorationGraph GRAPH = new ModernDecorationGraph();
    }

    public static Boolean test(String state, String below, BiPredicate<String, String> tagged) {
        String block = ModernDecorationEngine.blockName(state);
        String ground = ModernDecorationEngine.blockName(below);
        // Basalt, MagmaBlock and AshVentBlock inherit Block.canSurvive unchanged; placement predicates,
        // not a sapling-style dirt test, constrain the ash-vent column to its substrate.
        if (block.equals("minecraft:basalt") || block.equals("minecraft:smooth_basalt")
            || block.equals("minecraft:polished_basalt")
            || block.equals("minecraft:magma_block")
            || block.equals("regions_unexplored:ash_vent")) return true;
        if (state.contains("half=upper")) return block.equals(ground);
        if (block.equals("regions_unexplored:brimsprout") || block.equals("regions_unexplored:dorcel")
            || block.equals("regions_unexplored:brimwood_shrub"))
            return tagged.test("regions_unexplored:brim_plant_can_survive_on", below);
        if (block.equals("regions_unexplored:mycotoxic_mushrooms")) return tagged.test("minecraft:nylium", below);
        if (block.equals("regions_unexplored:cobalt_roots"))
            return tagged.test("minecraft:nylium", below) || ground.equals("minecraft:soul_soil")
                || ground.equals("minecraft:blackstone")
                || tagged.test("minecraft:dirt", below)
                || ground.equals("minecraft:farmland");
        if (block.equals("regions_unexplored:cobalt_earlight")
            || block.equals("regions_unexplored:tall_cobalt_earlight")
            || block.equals("regions_unexplored:glistering_sprout")
            || block.equals("regions_unexplored:glistering_fern")
            || block.equals("regions_unexplored:glistering_bloom")
            || block.equals("regions_unexplored:glister_bulb")
            || block.equals("regions_unexplored:glister_spire")
            || block.equals("regions_unexplored:mycotoxic_daisy")
            || block.equals("regions_unexplored:mycotoxic_grass"))
            return tagged.test("minecraft:nylium", below) || ground.equals("minecraft:soul_soil")
                || tagged.test("minecraft:dirt", below)
                || ground.equals("minecraft:farmland");
        if (block.equals("minecraft:bamboo") || block.equals("minecraft:bamboo_sapling")) {
            return tagged.test("minecraft:bamboo_plantable_on", below);
        }
        if (block.equals("regions_unexplored:saguaro_cactus_flower")) {
            return tagged.test("minecraft:sand", below) || ground.equals("minecraft:cactus")
                || ground.equals("regions_unexplored:saguaro_cactus");
        }
        if (block.equals("regions_unexplored:barrel_cactus")) {
            return tagged.test("minecraft:sand", below) || ground.equals("minecraft:sandstone")
                || ground.equals("minecraft:red_sandstone");
        }
        if (block.equals("regions_unexplored:sandy_grass") || block.equals("regions_unexplored:sandy_tall_grass")) {
            return tagged.test("regions_unexplored:sandy_plant_can_survive_on", below);
        }
        if (block.equals("regions_unexplored:small_desert_shrub")
            || block.equals("regions_unexplored:dead_steppe_shrub")
            || block.equals("minecraft:dead_bush")
            || block.equals("minecraft:deadbush")) {
            return tagged.test("minecraft:dead_bush_may_place_on", below);
        }
        if (block.equals("regions_unexplored:steppe_shrub")) {
            return tagged.test("minecraft:dirt", below) || ground.equals("minecraft:farmland");
        }
        if (block.startsWith("regions_unexplored:") && block.endsWith("_shrub")) {
            return tagged.test("regions_unexplored:supports/shrubs", below);
        }
        if (block.equals("regions_unexplored:grass_sprouts")) {
            return tagged.test("regions_unexplored:supports/grass_sprouts", below);
        }
        if (block.equals("regions_unexplored:ashen_grass")) {
            return tagged.test("regions_unexplored:supports/ashen_grass", below);
        }
        if (block.equals("regions_unexplored:frozen_grass")) {
            if (ground.equals("minecraft:snow") && !below.contains("layers=8")) return false;
            return tagged.test("regions_unexplored:supports/frozen_grass", below);
        }
        if (block.equals("regions_unexplored:bleeding_heart")) {
            if (ground.equals("minecraft:snow") && !below.contains("layers=8")) return false;
            return tagged.test("minecraft:dirt", below)
                || tagged.test("regions_unexplored:supports/frozen_grass", below);
        }
        boolean dirt = tagged.test("minecraft:dirt", below) || ground.equals("minecraft:farmland");
        // RU's FlowerPatchBlock (modern PinkPetalsBlock) and ordinary grass blocks inherit dirt/farmland support.
        if (block.equals("regions_unexplored:clover") || block.equals("regions_unexplored:orange_coneflower")
            || block.equals("regions_unexplored:purple_coneflower")
            || block.equals("regions_unexplored:windswept_grass")
            || block.equals("regions_unexplored:steppe_grass")
            || block.equals("regions_unexplored:bladed_grass")
            || block.equals("regions_unexplored:bladed_tall_grass")
            || block.equals("regions_unexplored:dusktrap")
            || block.equals("regions_unexplored:duskmelon")
            // ElephantEarBlock inherits DoublePlantBlock without changing substrate survival.
            || block.equals("regions_unexplored:elephant_ear")) return dirt;
        if (block.endsWith("_sapling") || tagged.test("minecraft:small_flowers", state)
            || tagged.test("minecraft:tall_flowers", state)
            || block.equals("minecraft:grass")
            || block.equals("minecraft:short_grass")
            || block.equals("minecraft:tall_grass")
            || block.equals("minecraft:fern")
            || block.equals("minecraft:large_fern")
            || block.equals("minecraft:sweet_berry_bush")) return dirt;
        return null;
    }
}
