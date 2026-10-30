package industries.angelic.biomic.world.terrain;

/** Explicit visual choices for the blended pack; never changes the standalone modern parity graph. */
public final class HostedVegetationPolicy {

    private HostedVegetationPolicy() {}

    /** These habitats own shallow beds as well as banks; Coniferous Forest has native aquatic features. */
    public static boolean hasWetSurface(String biome) {
        return biome != null && switch (biome) {
            case "biomesoplenty:coral_reef", "biomesoplenty:kelp_forest", "biomesoplenty:mangrove", "biomesoplenty:tropics", "biomesoplenty:volcano", "biomesoplenty:oasis", "biomesoplenty:sacred_springs", "biomesoplenty:orchard", "biomesoplenty:scrubland", "biomesoplenty:spruce_woods", "biomesoplenty:wasteland" -> true;
            case "biomesoplenty:land_of_lakes", "biomesoplenty:land_of_lakes_marsh" -> true;
            case "biomesoplenty:canyon", "biomesoplenty:canyon_ravine", "biomesoplenty:silkglades", "biomesoplenty:quagmire", "biomesoplenty:sludgepit" -> true;
            case "biomesoplenty:wetland", "biomesoplenty:bayou", "biomesoplenty:lush_swamp", "biomesoplenty:bog", "biomesoplenty:fen", "biomesoplenty:moor", "biomesoplenty:dead_swamp", "biomesoplenty:marsh", "biomesoplenty:lush_desert", "biomesoplenty:outback", "biomesoplenty:xeric_shrubland", "biomesoplenty:origin_valley", "regions_unexplored:bayou", "regions_unexplored:old_growth_bayou", "regions_unexplored:fen", "regions_unexplored:fungal_fen", "regions_unexplored:marsh", "regions_unexplored:cold_river", "regions_unexplored:muddy_river", "regions_unexplored:tropical_river", "regions_unexplored:hyacinth_deeps", "regions_unexplored:rocky_reef", "saltymod:salt_marsh", "biomesoplenty:coniferous_forest", "biomesoplenty:snowy_coniferous_forest", "biomesoplenty:frost_forest", "biomesoplenty:meadow", "biomesoplenty:temperate_rainforest", "biomesoplenty:shield", "biomesoplenty:dense_forest", "biomesoplenty:tundra", "biomesoplenty:arctic", "biomesoplenty:alps", "biomesoplenty:dead_forest", "biomesoplenty:ominous_woods", "biomesoplenty:fungi_forest", "biomesoplenty:mystic_grove", "biomesoplenty:garden", "biomesoplenty:brushland", "biomesoplenty:chaparral", "biomesoplenty:grassland", "biomesoplenty:alps_forest", "biomesoplenty:meadow_forest", "biomesoplenty:seasonal_forest_clearing", "biomesoplenty:mountain", "biomesoplenty:glacier", "biomesoplenty:crag", "biomesoplenty:eucalyptus_forest", "biomesoplenty:rainforest", "biomesoplenty:tropical_rainforest", "biomesoplenty:bamboo_forest", "biomesoplenty:redwood_forest", "biomesoplenty:jade_cliffs" -> true;
            default -> false;
        };
    }

    public static boolean isCoastalBed(String biome) {
        if (HostedBopMarineTerrain.marine(biome)) return true;
        return biome != null && switch (biome) {
            case "regions_unexplored:grassy_beach", "regions_unexplored:gravel_beach", "regions_unexplored:cold_river", "regions_unexplored:muddy_river", "regions_unexplored:tropical_river", "regions_unexplored:hyacinth_deeps", "regions_unexplored:rocky_reef" -> true;
            default -> false;
        };
    }

    public static int waterDepthLimit(String biome) {
        if (HostedBopMarineTerrain.marine(biome)) return 63;
        return "regions_unexplored:hyacinth_deeps".equals(biome) || "regions_unexplored:rocky_reef".equals(biome) ? 64
            : 12;
    }

    /** A deep-ocean habitat must not inherit the ordinary wetland allowance for dry banks and tidal shelves. */
    public static int minimumWaterDepth(String biome) {
        if ("biomesoplenty:coral_reef".equals(biome) || "biomesoplenty:kelp_forest".equals(biome)) return 2;
        return "regions_unexplored:hyacinth_deeps".equals(biome) ? 24 : 0;
    }

    /** Host source surface accents, not geodes/ores or a second terrain/structure generator. */
    public static boolean hostsDecoration(int step, String feature) {
        return step == 9 || step == 2 && surfaceLandmark(feature)
            || step == 10 && "minecraft:freeze_top_layer".equals(feature);
    }

    public static boolean surfaceLandmark(String feature) {
        return switch (feature) {
            case "minecraft:forest_rock", "regions_unexplored:rock/group/tundra", "regions_unexplored:patch/ash_vents", "regions_unexplored:tree/ice_spire", "regions_unexplored:special/icicle_up", "regions_unexplored:special/rocky_reef_rocks", "regions_unexplored:special/hyacinth_rocks" -> true;
            default -> false;
        };
    }

    public static boolean isColdExpansion(String biome) {
        return "regions_unexplored:cold_boreal_taiga".equals(biome)
            || "regions_unexplored:frozen_pine_taiga".equals(biome)
            || "regions_unexplored:tundra".equals(biome)
            || "regions_unexplored:cold_river".equals(biome)
            || "regions_unexplored:icy_heights".equals(biome)
            || "regions_unexplored:spires".equals(biome);
    }

    /** Native rock-pillar footings extend below the three-block vegetation skin. */
    public static int foundationDepth(String feature) {
        if (feature != null && feature.startsWith("biomic:native_bop/")) {
            String[] parts = feature.split("/", 3);
            if (parts.length == 3 && java.util.Set
                .of(
                    "biomesoplenty:wasteland",
                    "biomesoplenty:mangrove",
                    "biomesoplenty:oasis",
                    "biomesoplenty:volcano",
                    "biomesoplenty:tropics",
                    "biomesoplenty:coral_reef",
                    "biomesoplenty:kelp_forest",
                    "biomesoplenty:sacred_springs",
                    "biomesoplenty:orchard",
                    "biomesoplenty:scrubland",
                    "biomesoplenty:spruce_woods")
                .contains(parts[1])) return 8;
        }
        if ("biomic:native_bop/biomesoplenty:land_of_lakes/vanilla_shore_clay".equals(feature)
            || "biomic:native_bop/biomesoplenty:land_of_lakes/gravelPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:land_of_lakes_marsh/vanilla_shore_clay".equals(feature)
            || "biomic:native_bop/biomesoplenty:land_of_lakes_marsh/vanilla_shore_sand".equals(feature)
            || "biomic:native_bop/biomesoplenty:land_of_lakes_marsh/vanilla_shore_gravel".equals(feature)
            || "biomic:native_bop/biomesoplenty:land_of_lakes_marsh/mudPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:land_of_lakes_marsh/waterLakesPerChunk".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:silkglades/vanilla_shore_clay".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:quagmire/generateQuagmire".equals(feature)
            || "biomic:native_bop/biomesoplenty:quagmire/vanilla_shore_clay".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:sludgepit/poisonLakesPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:sludgepit/mudPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:sludgepit/vanilla_shore_clay".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:wetland/mudPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:wetland/vanilla_shore_clay".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:bayou/waterLakesPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:bayou/mudPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:bayou/vanilla_shore_clay".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:lush_swamp/poisonLakesPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:lush_swamp/vanilla_shore_sand".equals(feature)
            || "biomic:native_bop/biomesoplenty:lush_swamp/vanilla_shore_clay".equals(feature)
            || "biomic:native_bop/biomesoplenty:lush_swamp/vanilla_shore_gravel".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:bog/waterLakesPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:bog/poisonLakesPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:bog/mudPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:bog/gravelPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:bog/vanilla_shore_clay".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:fen/mudPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:fen/gravelPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:fen/vanilla_shore_clay".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:moor/waterLakesPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:moor/mudPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:moor/vanilla_shore_clay".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:dead_swamp/mudPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:dead_swamp/vanilla_shore_clay".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:marsh/waterLakesPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:marsh/mudPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:marsh/vanilla_shore_clay".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:lush_desert/waterLakesPerChunk".equals(feature)
            || "biomic:native_bop/biomesoplenty:lush_desert/oasesPerChunk".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:grassland/waterLakesPerChunk".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:ominous_woods/poisonLakesPerChunk".equals(feature)) return 8;
        if ("biomic:native_bop/biomesoplenty:mystic_grove/poisonLakesPerChunk".equals(feature)) return 8;
        if ("regions_unexplored:special/rocky_reef_rocks".equals(feature)) return 16;
        if ("regions_unexplored:tree/ice_spire".equals(feature)) return 8;
        return 3;
    }

    public static boolean keepsConfiguredWrites(String type, boolean completed, boolean success) {
        // GiantLilyPadFeature writes its entire pad and then returns false upstream. An envelope
        // exception still cancels it entirely; the false return itself is not a placement rollback.
        return completed && (success || "regions_unexplored:giant_lily".equals(type));
    }

    public static boolean allowsFeature() {
        // Source accents are approved, including Silver Birch's multicolored coneflower patches
        // and yellow shrubs. Do not reinstate the temporary v11/v12 palette exclusions.
        return true;
    }

    public static boolean legacyDeadBushGround(String state) {
        String block = industries.angelic.biomic.world.modern.ModernDecorationEngine.blockName(state);
        // Modern accepts grass/moss/mud too; vanilla 1.7 dead bushes pop off those substrates.
        // Do not change player block behavior or put artificial dirt scars beneath them.
        return block.equals("minecraft:sand") || block.equals("minecraft:red_sand")
            || block.equals("minecraft:dirt")
            || block.equals("minecraft:coarse_dirt")
            || block.equals("minecraft:podzol")
            || block.equals("minecraft:terracotta")
            || block.startsWith("minecraft:") && block.endsWith("_terracotta")
            || block.equals("regions_unexplored:peat_dirt")
            || block.equals("regions_unexplored:silt_dirt")
            || block.equals("regions_unexplored:peat_coarse_dirt")
            || block.equals("regions_unexplored:silt_coarse_dirt");
    }

    /** Null rejects only this terminal mushroom feature, never the selector containing normal trees. */
    public static com.google.gson.JsonObject configured(String biome, String type, com.google.gson.JsonObject config,
        boolean large, boolean small, boolean lights) {
        if (!"regions_unexplored:blackwood_taiga".equals(biome)) return config;
        if ("minecraft:tree".equals(type) && config.has("foliage_placer")
            && "regions_unexplored:bioshroom".equals(
                config.getAsJsonObject("foliage_placer")
                    .get("type")
                    .getAsString())) {
            if (!large) return null;
            if (!lights) {
                com.google.gson.JsonObject adjusted = new com.google.gson.JsonParser().parse(config.toString())
                    .getAsJsonObject();
                adjusted.getAsJsonObject("foliage_placer")
                    .add("secondary_provider", adjusted.get("foliage_provider"));
                return adjusted;
            }
        }
        if (!small && "minecraft:simple_block".equals(type)
            && config.has("to_place")
            && config.get("to_place")
                .toString()
                .contains("bioshroom"))
            return null;
        return config;
    }
}
