package industries.angelic.biomic.world.terrain;

import static industries.angelic.biomic.world.terrain.BiomicTerrainSampler.*;

/** Source-state names for the physical terrain palette, before feature decoration; no fallback substitutions. */
public final class TerrainMaterialStates {

    private TerrainMaterialStates() {}

    public static String state(byte material) {
        return switch (material) {
            case AIR -> "minecraft:air";
            case BEDROCK -> "minecraft:bedrock";
            case STONE -> "minecraft:stone";
            case DIRT -> "minecraft:dirt";
            case GRASS -> "minecraft:grass_block";
            case WATER -> "minecraft:water";
            case SAND -> "minecraft:sand";
            case GRAVEL -> "minecraft:gravel";
            case MOSS -> "minecraft:moss_block";
            case TERRACOTTA -> "minecraft:terracotta";
            case BASALT -> "minecraft:basalt";
            case MAGMA -> "minecraft:magma_block";
            case SNOW -> "minecraft:snow_block";
            case MYCELIUM -> "minecraft:mycelium";
            case ICE -> "minecraft:ice";
            case CHALK -> "regions_unexplored:chalk";
            case TUFF -> "minecraft:tuff";
            case PODZOL -> "minecraft:podzol";
            case MUD -> "minecraft:mud";
            case RED_TERRACOTTA -> "minecraft:red_terracotta";
            case ORANGE_TERRACOTTA -> "minecraft:orange_terracotta";
            case BROWN_TERRACOTTA -> "minecraft:brown_terracotta";
            case COARSE_DIRT -> "minecraft:coarse_dirt";
            case PACKED_MUD -> "minecraft:packed_mud";
            case RED_CONCRETE -> "minecraft:red_concrete";
            case BROWN_CONCRETE -> "minecraft:brown_concrete";
            case LIGHT_GRAY_TERRACOTTA -> "minecraft:light_gray_terracotta";
            case PINK_TERRACOTTA -> "minecraft:pink_terracotta";
            case GRANITE -> "minecraft:granite";
            case SANDSTONE -> "minecraft:sandstone";
            case ANDESITE -> "minecraft:andesite";
            case BLACKSTONE -> "minecraft:blackstone";
            case BLUE_ICE -> "minecraft:blue_ice";
            case PACKED_ICE -> "minecraft:packed_ice";
            case BLUE_TERRACOTTA -> "minecraft:blue_terracotta";
            case CALCITE -> "minecraft:calcite";
            case CLAY -> "minecraft:clay";
            case CYAN_TERRACOTTA -> "minecraft:cyan_terracotta";
            case DEAD_BRAIN_CORAL -> "minecraft:dead_brain_coral_block";
            case DEAD_BUBBLE_CORAL -> "minecraft:dead_bubble_coral_block";
            case DEAD_HORN_CORAL -> "minecraft:dead_horn_coral_block";
            case DEEPSLATE -> "minecraft:deepslate";
            case DIORITE -> "minecraft:diorite";
            case DRIPSTONE_BLOCK -> "minecraft:dripstone_block";
            case FARMLAND_MOIST -> "minecraft:farmland[moisture=7]";
            case LAVA -> "minecraft:lava";
            case LIGHT_BLUE_TERRACOTTA -> "minecraft:light_blue_terracotta";
            case LIME_TERRACOTTA -> "minecraft:lime_terracotta";
            case MOSSY_COBBLESTONE -> "minecraft:mossy_cobblestone";
            case OBSIDIAN -> "minecraft:obsidian";
            case POWDER_SNOW -> "minecraft:powder_snow";
            case RED_SAND -> "minecraft:red_sand";
            case RED_SANDSTONE -> "minecraft:red_sandstone";
            case ROOTED_DIRT -> "minecraft:rooted_dirt";
            case SMOOTH_BASALT -> "minecraft:smooth_basalt";
            case SMOOTH_SANDSTONE -> "minecraft:smooth_sandstone";
            case WHITE_CONCRETE_POWDER -> "minecraft:white_concrete_powder";
            case WHITE_TERRACOTTA -> "minecraft:white_terracotta";
            case YELLOW_CONCRETE -> "minecraft:yellow_concrete";
            case YELLOW_TERRACOTTA -> "minecraft:yellow_terracotta";
            case COBBLESTONE -> "minecraft:cobblestone";
            // This legacy Biomic-only block has no RU equivalent. Reference replay must reject it, not turn it
            // into ordinary grass and report false parity.
            case CHALK_GRASS -> "biomic:chalk_grass";
            case PEAT_GRASS -> "regions_unexplored:peat_grass_block";
            case PEAT_DIRT -> "regions_unexplored:peat_dirt";
            case SILT_GRASS -> "regions_unexplored:silt_grass_block";
            case SILT_DIRT -> "regions_unexplored:silt_dirt";
            case ASHEN_DIRT -> "regions_unexplored:ashen_dirt";
            case ASH -> "regions_unexplored:ash";
            case ARGILLITE -> "regions_unexplored:argillite";
            case ARGILLITE_GRASS -> "regions_unexplored:argillite_grass_block";
            case VIRIDESCENT_NYLIUM -> "regions_unexplored:viridescent_nylium";
            case DEEPSLATE_VIRIDESCENT_NYLIUM -> "regions_unexplored:deepslate_viridescent_nylium";
            case PRISMOSS -> "regions_unexplored:prismoss";
            case DEEPSLATE_PRISMOSS -> "regions_unexplored:deepslate_prismoss";
            case BRIMSPROUT_NYLIUM -> "regions_unexplored:brimsprout_nylium";
            case COBALT_NYLIUM -> "regions_unexplored:cobalt_nylium";
            case GLISTERING_NYLIUM -> "regions_unexplored:glistering_nylium";
            case MYCOTOXIC_MOSS -> "regions_unexplored:mycotoxic_moss";
            case PEAT_MUD -> "regions_unexplored:peat_mud";
            case SILT_MUD -> "regions_unexplored:silt_mud";
            case PEAT_PODZOL -> "regions_unexplored:peat_podzol";
            case SILT_PODZOL -> "regions_unexplored:silt_podzol";
            case FORLORN_GUANOSTONE -> "alexscaves:guanostone";
            case FORLORN_COPROLITH -> "alexscaves:coprolith";
            case PRIMORDIAL_LIMESTONE -> "alexscaves:limestone";
            case ABYSSMARINE -> "alexscaves:abyssmarine";
            case ABYSSAL_MUCK -> "alexscaves:muck";
            case PRIMORDIAL_FLOOD_BASALT -> "alexscaves:flood_basalt[axis=y]";
            case PRIMORDIAL_MAGMA -> "alexscaves:primal_magma[active=false,permanent=false]";
            case PRIMORDIAL_MAGMA_ACTIVE -> "alexscaves:primal_magma[active=true,permanent=true]";
            case PRIMORDIAL_VOLCANIC_CORE -> "alexscaves:volcanic_core";
            case PRIMORDIAL_MOSS_CARPET -> "minecraft:moss_carpet";
            case PRIMORDIAL_MOSS_BLOCK -> "minecraft:moss_block";
            default -> throw new IllegalArgumentException("Unknown terrain material " + (material & 255));
        };
    }
}
