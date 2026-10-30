package industries.angelic.biomic.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import industries.angelic.biomic.Biomic;

@Config(modid = Biomic.MODID, category = "debug", filename = Biomic.MODID)
@Config.LangKeyPattern(pattern = "biomic.config.%cat.%field")
public final class DebugConfig {

    private DebugConfig() {}

    @Config.Comment("Enable Biomic debug logging in addition to the MCMODDING_DEBUG_MODE environment variable.")
    @Config.DefaultBoolean(false)
    @Config.Order(0)
    public static boolean debugMode;

    @Config.Comment("The biome browser, a developer's screen listing every biome (ops only, or the single player with "
        + "cheats). Off: the server answers no one.")
    @Config.DefaultBoolean(true)
    @Config.Order(1)
    public static boolean biomeBrowser;

    @Config.Comment("Enable high-level tracing as chunks move through Biomic's terrain-generation phases.")
    @Config.DefaultBoolean(false)
    @Config.Order(2)
    public static boolean traceTerrainPipeline;

    @Config.Comment("Enable verbose tracing of vertical-biome NBT, packet, clone, and lookup operations.")
    @Config.DefaultBoolean(false)
    @Config.Order(4)
    public static boolean traceVerticalBiomeStorage;

    @Config.Comment("Log every intercepted EFR generation call and the firewall action selected for it.")
    @Config.DefaultBoolean(false)
    @Config.Order(5)
    public static boolean traceEfrGenerationFirewall;

}
