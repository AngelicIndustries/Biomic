package industries.angelic.biomic.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import industries.angelic.biomic.Biomic;

/** Numeric biome IDs are configurable for 1.7 pack assembly; stable keys remain the save/API identity. */
@Config(modid = Biomic.MODID, category = "biome_ids", filename = Biomic.MODID)
@Config.LangKeyPattern(pattern = "biomic.config.%cat.%field")
public final class BiomeConfig {

    private BiomeConfig() {}

    @Config.Comment("Numeric ID for biomic:temperate_lowlands. Changing this requires a new world or migration.")
    @Config.DefaultInt(6000)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(0)
    public static int temperateLowlandsId = 6000;

    @Config.Comment("Numeric ID for biomic:temperate_archipelago. Changing this requires a new world or migration.")
    @Config.DefaultInt(6001)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(1)
    public static int temperateArchipelagoId = 6001;

    @Config.Comment("Numeric ID for biomic:windswept_highlands. Changing this requires a new world or migration.")
    @Config.DefaultInt(6002)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(2)
    public static int windsweptHighlandsId = 6002;

    @Config.Comment("Numeric ID for biomic:lush_caves. Changing this requires a new world or migration.")
    @Config.DefaultInt(6003)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(3)
    public static int lushCavesId = 6003;

    @Config.Comment("Numeric ID for biomic:clover_plains. Changing this requires a new world or migration.")
    @Config.DefaultInt(6004)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(4)
    public static int cloverPlainsId = 6004;

    @Config.Comment("Numeric ID for biomic:wisteria_grove. Changing this requires a new world or migration.")
    @Config.DefaultInt(6005)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(5)
    public static int wisteriaGroveId = 6005;

    @Config.Comment("Numeric ID for biomic:chalk_cliffs. Changing this requires a new world or migration.")
    @Config.DefaultInt(6006)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(6)
    public static int chalkCliffsId = 6006;

    @Config.Comment("Numeric ID for biomic:towering_cliffs. Changing this requires a new world or migration.")
    @Config.DefaultInt(6007)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(7)
    public static int toweringCliffsId = 6007;

    @Config.Comment("Numeric ID for biomic:redwoods. Changing this requires a new world or migration.")
    @Config.DefaultInt(6008)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(8)
    public static int redwoodsId = 6008;

    @Config.Comment("Numeric ID for biomic:bayou. Changing this requires a new world or migration.")
    @Config.DefaultInt(6009)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(9)
    public static int bayouId = 6009;

    @Config.Comment("Numeric ID for biomic:old_growth_bayou. Changing this requires a new world or migration.")
    @Config.DefaultInt(6010)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(10)
    public static int oldGrowthBayouId = 6010;

    @Config.Comment("Numeric ID for biomic:joshua_desert. Changing this requires a new world or migration.")
    @Config.DefaultInt(6011)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(11)
    public static int joshuaDesertId = 6011;

    @Config.Comment("Numeric ID for biomic:sky_donut. Changing this requires a new world or migration.")
    @Config.DefaultInt(6012)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(12)
    public static int skyDonutId = 6012;

    @Config.Comment("Numeric ID for biomic:aeropelago. Changing this requires a new world or migration.")
    @Config.DefaultInt(6013)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(13)
    public static int aeropelagoId = 6013;

    @Config.Comment("Numeric ID for biomic:pine_slopes. Changing this requires a new world or migration.")
    @Config.DefaultInt(6014)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(14)
    public static int pineSlopesId = 6014;

    @Config.Comment("Numeric ID for biomic:grassy_beach. Changing this requires a new world or migration.")
    @Config.DefaultInt(6015)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(15)
    public static int grassyBeachId = 6015;

    @Config.Comment("Numeric ID for biomic:eucalyptus_forest. Changing this requires a new world or migration.")
    @Config.DefaultInt(6016)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(16)
    public static int eucalyptusForestId = 6016;

    @Config.Comment({ "First numeric ID in the contiguous block for active Regions Unexplored ports beyond the",
        "eleven save-stable original slices. The block currently contains 53 entries." })
    @Config.DefaultInt(6017)
    @Config.RangeInt(min = 0, max = 65482)
    @Config.RequiresMcRestart
    @Config.Order(17)
    public static int ruExpansionBaseId = 6017;

    @Config.Comment({ "First numeric ID in the contiguous Origen biome block.",
        "The block contains one stable slot per production Origen profile; changing it requires a new world or migration." })
    @Config.DefaultInt(6100)
    @Config.RangeInt(min = 0, max = 65487)
    @Config.RequiresMcRestart
    @Config.Order(18)
    public static int origenBiomeBaseId = 6100;

    @Config.Comment("Numeric ID for biomic:soulblight_forest. Changing this requires a new world or migration.")
    @Config.DefaultInt(6300)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(19)
    public static int soulblightForestId = 6300;

    @Config.Comment("Numeric ID for biomic:whistling_woods. Changing this requires a new world or migration.")
    @Config.DefaultInt(6301)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(20)
    public static int whistlingWoodsId = 6301;
    @Config.Comment("Numeric ID for biomic:estuary, enrolled only by its new signed fresh content policy. Old world signatures remain unchanged.")
    @Config.DefaultInt(6302)
    @Config.RangeInt(min = 0, max = 65534)
    @Config.RequiresMcRestart
    @Config.Order(21)
    public static int estuaryId = 6302;
    @Config.Comment({ "First numeric ID in the contiguous block for the twelve Tartarus Nether biomes.",
        "Changing it requires a new world or migration." })
    @Config.DefaultInt(6310)
    @Config.RangeInt(min = 0, max = 65523)
    @Config.RequiresMcRestart
    @Config.Order(22)
    public static int tartarusBiomeBaseId = 6310;
}
