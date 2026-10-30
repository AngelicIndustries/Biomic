package industries.angelic.biomic.api;

import industries.angelic.biomic.Biomic;

/** Stable biome keys used by saves and downstream integrations. */
public final class BiomeKeys {

    public static final String TEMPERATE_LOWLANDS = Biomic.MODID + ":temperate_lowlands";
    public static final String TEMPERATE_ARCHIPELAGO = Biomic.MODID + ":temperate_archipelago";
    public static final String WINDSWEPT_HIGHLANDS = Biomic.MODID + ":windswept_highlands";
    public static final String LUSH_CAVES = Biomic.MODID + ":lush_caves";
    public static final String SKY_DONUT = Biomic.MODID + ":sky_donut";
    public static final String AEROPELAGO = Biomic.MODID + ":aeropelago";
    public static final String SOULBLIGHT_FOREST = Biomic.MODID + ":soulblight_forest";
    public static final String WHISTLING_WOODS = Biomic.MODID + ":whistling_woods";
    public static final String ESTUARY = Biomic.MODID + ":estuary";

    // Final biome identities adapted from the pinned MIT Regions Unexplored catalogue. These are
    // registered only when their complete vertical slice is ready; declaring a stable key does not
    // reserve a numeric EndlessIDs slot or expose an incomplete biome to a save.
    public static final String CLOVER_PLAINS = Biomic.MODID + ":clover_plains";
    public static final String WISTERIA_GROVE = Biomic.MODID + ":wisteria_grove";
    public static final String PINE_SLOPES = Biomic.MODID + ":pine_slopes";
    public static final String CHALK_CLIFFS = Biomic.MODID + ":chalk_cliffs";
    public static final String TOWERING_CLIFFS = Biomic.MODID + ":towering_cliffs";
    public static final String REDWOODS = Biomic.MODID + ":redwoods";
    public static final String BAYOU = Biomic.MODID + ":bayou";
    public static final String OLD_GROWTH_BAYOU = Biomic.MODID + ":old_growth_bayou";
    public static final String JOSHUA_DESERT = Biomic.MODID + ":joshua_desert";
    public static final String EUCALYPTUS_FOREST = Biomic.MODID + ":eucalyptus_forest";
    public static final String GRASSY_BEACH = Biomic.MODID + ":grassy_beach";
    public static final String MUDDY_RIVER = Biomic.MODID + ":muddy_river";

    private BiomeKeys() {}
}
