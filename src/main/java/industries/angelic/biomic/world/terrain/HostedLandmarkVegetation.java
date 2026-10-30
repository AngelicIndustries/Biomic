package industries.angelic.biomic.world.terrain;

/** Landmark geology is shared; its lush hanging vegetation is not appropriate to every new habitat. */
final class HostedLandmarkVegetation {

    private HostedLandmarkVegetation() {}

    static boolean lushDrapes(String habitat) {
        return habitat == null || switch (habitat) {
            case "regions_unexplored:sparse_rainforest", "regions_unexplored:bamboo_forest", "regions_unexplored:eucalyptus_forest", "regions_unexplored:bayou", "regions_unexplored:old_growth_bayou", "regions_unexplored:rainforest", "regions_unexplored:tropics", "regions_unexplored:tropical_river" -> true;
            default -> false;
        };
    }

    static boolean replaceDrape(String habitat, String feature) {
        if (lushDrapes(habitat) || feature == null) return false;
        return switch (feature) {
            case "LARGE_LEAF_VINES", "LARGE_LEAF_VINES_UNCOLORED", "LARGE_LEAF_VINES_CANYON", "LARGE_CEILING_VINES", "CROSSING_VINES", "CROSSING_STONE_VINES", "BIG_DRIPLEAF", "SMALL_DRIPLEAF", "MOUNTAIN_GLOW_BERRIES", "TREE_VINES" -> true;
            default -> false;
        };
    }

    static boolean sameHabitat(String origin, String target) {
        return origin != null && origin.equals(target);
    }

    static boolean replacesSourceWinter(String habitat, String feature) {
        return (industries.angelic.biomic.compat.biome.BopNativeBiomes.winter(habitat)
            || industries.angelic.biomic.compat.biome.BopNativeBiomes.geological(habitat)) && switch (feature) {
            case "SNOW_LAYERS", "SINKHOLE_SNOW_LAYERS", "ICEBERG_SNOW_LAYERS", "TREE_SNOW" -> true;
            default -> false;
        };
    }

    static boolean thinVines(String habitat) {
        if (habitat == null) return false;
        return switch (habitat) {
            case "regions_unexplored:redwoods", "regions_unexplored:sparse_redwoods", "regions_unexplored:silver_birch_forest", "regions_unexplored:maple_forest", "regions_unexplored:autumnal_maple_forest", "regions_unexplored:wisteria_grove", "regions_unexplored:magnolia_woodland", "regions_unexplored:willow_forest", "minecraft:forest" -> true;
            default -> false;
        };
    }
}
