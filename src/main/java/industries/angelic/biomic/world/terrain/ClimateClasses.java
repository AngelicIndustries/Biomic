package industries.angelic.biomic.world.terrain;

import java.util.HashMap;
import java.util.Map;

import industries.angelic.biomic.biome.origen.OrigenBiomeProfile;

/**
 * How warm a biome looks (biome placement study): 0 snowy ground, 1 cold (taiga, bare cold rock), 2 temperate,
 * 3 warm or humid (jungle, savanna, bamboo, warm badlands), 4 hot or arid (desert, badlands, dry scrub). Water,
 * shores and unknown biomes have no class (-1). A hosted habitat may take a province only if it looks like its host
 * or one step away, and never across the temperate and warm line: vanilla 1.7, 1.18, TerraBlender and
 * Geographicraft all keep anything placed after climate within its climate.
 */
final class ClimateClasses {

    static final int NONE = -1;

    private static final Map<String, Integer> CLASSES = new HashMap<>();

    private static void put(int climate, String... ids) {
        for (String id : ids) CLASSES.put(id, climate);
    }

    static {
        // Origen profiles.
        put(
            0,
            "FROSTY_FINGERS",
            "SNOWY_MEADOW",
            "SNOWY_MOUNTAINS",
            "MOUNTAIN_MIRRORS",
            "ALPINE_ASCENDANCY",
            "TUNDRA_TRACKS",
            "FROZEN_FUNGI",
            "SNOWY_SPIRES",
            "ICY_INCISIONS",
            "SINKHOLE_FROZEN",
            "MOUNTAIN_MIRRORS_RIVER",
            "FROZEN_FUNGI_RIVER",
            "ALPINE_ASCENDANCY_RIVER");
        put(1, "COLD_STEPPE", "BARE_BOULDERFIELDS", "TALL_TIMBERLAND");
        put(
            2,
            "BLACK_FOREST",
            "FLOWERING_FOREST",
            "PILLOW_PLAINS_INNER",
            "PILLOW_PLAINS_MIDDLE",
            "PILLOW_PLAINS_OUTER",
            "SECLUDED_VALLEY",
            "SECLUDED_VALLEY_OUTER",
            "VERDANT_VALLEYS",
            "VERDANT_RIVER",
            "FOSSILIZED_FENLANDS",
            "MURKY_MARSHLANDS",
            "VERTICAL_VISTAS",
            "VISTAS_RIVER",
            "SAKURA_STREAMS",
            "GLOOMY_GORGE",
            "GLOOMY_GORGE_RIVER",
            "WHITE_WALLOWS",
            "ROCKY_REFUGE",
            "FOLIAGE_FORTRESS_INNER",
            "FOLIAGE_FORTRESS_MIDDLE",
            "FOLIAGE_FORTRESS_OUTER",
            "PLATEAO_RIVER_INNER",
            "PLATEAO_RIVER_MIDDLE",
            "SINKHOLE_FOREST",
            "SINKHOLE_OUTER",
            "FERN_FISSURES");
        put(
            3,
            "BAMBOO_BASIN",
            "SCARLET_SANCTUARY",
            "SCARLET_SANCTUARY_RIVER",
            "LUSH_LOOPS",
            "LOOPS_RIVER",
            "CANOPY_CASCADES",
            "VERTICAL_JUNGLE_RIVER",
            "OVERGROWN_CLIFFS",
            "WATERY_WILDS",
            "SINKHOLE_JUNGLE",
            "MANGROVE_SWAMP",
            "BADLANDS_BALCONIES",
            "BAD_BALCOONIES_RIVER");
        put(
            4,
            "DESERT",
            "OASIS",
            "SALT_FLATS",
            "DESERT_SPIKES_GOLD",
            "MESA_MONUMENTS",
            "CARVING_CREAKS",
            "ARID_ARBORETUM",
            "SANDY_SPLITS");
        // Regions Unexplored.
        put(
            0,
            "regions_unexplored:icy_heights",
            "regions_unexplored:spires",
            "regions_unexplored:cold_boreal_taiga",
            "regions_unexplored:frozen_pine_taiga",
            "regions_unexplored:tundra");
        put(
            1,
            "regions_unexplored:pine_taiga",
            "regions_unexplored:pine_slopes",
            "regions_unexplored:boreal_taiga",
            "regions_unexplored:old_growth_boreal_taiga",
            "regions_unexplored:old_growth_golden_boreal_taiga",
            "regions_unexplored:blackwood_taiga");
        put(
            2,
            "regions_unexplored:maple_forest",
            "regions_unexplored:autumnal_maple_forest",
            "regions_unexplored:windswept_maple_forest",
            "regions_unexplored:magnolia_woodland",
            "regions_unexplored:silver_birch_forest",
            "regions_unexplored:willow_forest",
            "regions_unexplored:fen",
            "regions_unexplored:fungal_fen",
            "regions_unexplored:marsh",
            "regions_unexplored:bayou",
            "regions_unexplored:old_growth_bayou",
            "regions_unexplored:clover_plains",
            "regions_unexplored:flower_fields",
            "regions_unexplored:poppy_fields",
            "regions_unexplored:grassland",
            "regions_unexplored:highland_fields",
            "regions_unexplored:prairie",
            "regions_unexplored:shrubland",
            "regions_unexplored:chalk_cliffs",
            "regions_unexplored:sparse_redwoods",
            "regions_unexplored:redwoods",
            "regions_unexplored:orchard",
            "regions_unexplored:old_growth_forest",
            "regions_unexplored:towering_cliffs",
            "regions_unexplored:wisteria_grove",
            "regions_unexplored:alpha_grove");
        put(
            3,
            "regions_unexplored:tropics",
            "regions_unexplored:ashen_woodland",
            "regions_unexplored:rainforest",
            "regions_unexplored:sparse_rainforest",
            "regions_unexplored:eucalyptus_forest",
            "regions_unexplored:bamboo_forest",
            "regions_unexplored:baobab_savanna");
        put(
            4,
            "regions_unexplored:dry_bushland",
            "regions_unexplored:outback",
            "regions_unexplored:saguaro_desert",
            "regions_unexplored:joshua_desert");
        // Biomes O' Plenty.
        put(
            0,
            "biomesoplenty:alps",
            "biomesoplenty:alps_forest",
            "biomesoplenty:arctic",
            "biomesoplenty:frost_forest",
            "biomesoplenty:glacier",
            "biomesoplenty:snowy_coniferous_forest");
        put(
            1,
            "biomesoplenty:boreal_forest",
            "biomesoplenty:coniferous_forest",
            "biomesoplenty:crag",
            "biomesoplenty:dead_forest",
            "biomesoplenty:maple_woods",
            "biomesoplenty:moor",
            "biomesoplenty:mountain",
            "biomesoplenty:shield",
            "biomesoplenty:spruce_woods",
            "biomesoplenty:tundra");
        put(
            2,
            "biomesoplenty:bayou",
            "biomesoplenty:bog",
            "biomesoplenty:cherry_blossom_grove",
            "biomesoplenty:dead_swamp",
            "biomesoplenty:deciduous_forest",
            "biomesoplenty:dense_forest",
            "biomesoplenty:fen",
            "biomesoplenty:flower_field",
            "biomesoplenty:fungi_forest",
            "biomesoplenty:garden",
            "biomesoplenty:grassland",
            "biomesoplenty:grove",
            "biomesoplenty:heathland",
            "biomesoplenty:highland",
            "biomesoplenty:jade_cliffs",
            "biomesoplenty:land_of_lakes",
            "biomesoplenty:land_of_lakes_marsh",
            "biomesoplenty:lavender_fields",
            "biomesoplenty:marsh",
            "biomesoplenty:meadow",
            "biomesoplenty:meadow_forest",
            "biomesoplenty:mystic_grove",
            "biomesoplenty:ominous_woods",
            "biomesoplenty:orchard",
            "biomesoplenty:origin_valley",
            "biomesoplenty:prairie",
            "biomesoplenty:quagmire",
            "biomesoplenty:redwood_forest",
            "biomesoplenty:seasonal_forest",
            "biomesoplenty:seasonal_forest_clearing",
            "biomesoplenty:shrubland",
            "biomesoplenty:silkglades",
            "biomesoplenty:sludgepit",
            "biomesoplenty:temperate_rainforest",
            "biomesoplenty:thicket",
            "biomesoplenty:wetland",
            "biomesoplenty:woodland");
        put(
            3,
            "biomesoplenty:bamboo_forest",
            "biomesoplenty:brushland",
            "biomesoplenty:chaparral",
            "biomesoplenty:eucalyptus_forest",
            "biomesoplenty:lush_swamp",
            "biomesoplenty:mangrove",
            "biomesoplenty:rainforest",
            "biomesoplenty:sacred_springs",
            "biomesoplenty:tropical_rainforest",
            "biomesoplenty:tropics");
        put(
            4,
            "biomesoplenty:canyon",
            "biomesoplenty:canyon_ravine",
            "biomesoplenty:lush_desert",
            "biomesoplenty:oasis",
            "biomesoplenty:outback",
            "biomesoplenty:scrubland",
            "biomesoplenty:steppe",
            "biomesoplenty:volcano",
            "biomesoplenty:wasteland",
            "biomesoplenty:xeric_shrubland");
        // Vanilla and others.
        put(0, "minecraft:grove", "minecraft:snowy_plains", "minecraft:snowy_slopes", "minecraft:snowy_taiga");
        put(1, "minecraft:taiga", "minecraft:windswept_forest", "minecraft:windswept_gravelly_hills");
        put(2, "minecraft:cherry_grove", "minecraft:forest", "minecraft:plains", "saltymod:salt_marsh");
        put(3, "minecraft:jungle", "minecraft:savanna");
        put(4, "minecraft:desert", "minecraft:eroded_badlands", "minecraft:wooded_badlands");
    }

    private ClimateClasses() {}

    static int of(String id) {
        return CLASSES.getOrDefault(id, NONE);
    }

    /**
     * Whether {@code habitat} may take a province whose host is {@code host}. A host with no class (seas, shores) is
     * judged by its cell's climate band: cold seas count as cold, medium as temperate, warm as warm.
     */
    static boolean fits(String habitat, OrigenBiomeProfile host, int band) {
        int a = of(habitat);
        int b = host == null ? NONE : of(host.name());
        if (b == NONE) b = band + 1;
        if (a == NONE || a == b) return true;
        if (Math.abs(a - b) > 1) return false;
        return !(a == 2 && b == 3 || a == 3 && b == 2);
    }
}
