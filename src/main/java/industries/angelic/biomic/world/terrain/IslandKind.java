package industries.angelic.biomic.world.terrain;

import industries.angelic.biomic.compat.biome.BopNativeBiomes;

/** The broad kind of an island's habitat, for its rock (IslandSkin) and what hangs under it (IslandUnderside). */
enum IslandKind {

    /** Temperate forest and grassland. */
    GREEN,
    /** Wet, tropical or magical: jungles, swamps, wisteria. */
    LUSH,
    SNOWY,
    /** Dry or rocky: deserts, badlands, savannas, bare peaks. */
    DRY;

    private static final String[] SNOWY_WORDS = { "snow", "frozen", "ice_", "icy", "tundra", "glacier", "alps",
        "arctic", "frost", "cold_", "jagged_peaks", "minecraft:grove" };
    private static final String[] DRY_WORDS = { "desert", "badlands", "savanna", "outback", "bushland", "steppe",
        "chaparral", "brushland", "scrub", "wasteland", "canyon", "mesa", "xeric", "volcan", "ashen", "inferno", "crag",
        "baobab", "stony", "gravel", "spires", "mountain", "peaks" };
    private static final String[] LUSH_WORDS = { "rainforest", "jungle", "tropic", "bamboo", "mystic", "fung", "lush",
        "wisteria", "bayou", "swamp", "mangrove", "marsh", "fen", "bog", "eucalyptus", "magnolia", "willow", "sacred",
        "garden", "silkglade", "ominous", "cherry", "mushroom", "quagmire", "wetland", "sludge" };

    /** The kind of a habitat; null (no habitat) counts as green. */
    static IslandKind of(String habitat) {
        if (habitat == null) return GREEN;
        if (BopNativeBiomes.winter(habitat) || any(habitat, SNOWY_WORDS)) return SNOWY;
        if (BopNativeBiomes.barrenSurface(habitat) || any(habitat, DRY_WORDS)) return DRY;
        return any(habitat, LUSH_WORDS) ? LUSH : GREEN;
    }

    boolean verdant() {
        return this == GREEN || this == LUSH;
    }

    private static boolean any(String habitat, String[] words) {
        for (String word : words) if (habitat.contains(word)) return true;
        return false;
    }
}
