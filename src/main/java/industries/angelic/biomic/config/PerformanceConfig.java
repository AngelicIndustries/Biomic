package industries.angelic.biomic.config;

import com.gtnewhorizon.gtnhlib.config.Config;

import industries.angelic.biomic.Biomic;

@Config(modid = Biomic.MODID, category = "performance", filename = Biomic.MODID)
@Config.LangKeyPattern(pattern = "biomic.config.%cat.%field")
public final class PerformanceConfig {

    private PerformanceConfig() {}

    @Config.Comment({ "Worker count for pure terrain and 3D-biome precomputation.",
        "Forge population, structures, Chunk construction, and all World mutation remain on the server thread.",
        "Set to 0 to disable threaded precomputation." })
    @Config.DefaultInt(4)
    @Config.RangeInt(min = 0, max = 16)
    @Config.Order(0)
    public static int terrainWorkerThreads;

    @Config.Comment("Maximum number of nearby pure-terrain payloads retained for bounded prefetching.")
    @Config.DefaultInt(512)
    @Config.RangeInt(min = 8, max = 1024)
    @Config.Order(1)
    public static int terrainPrefetchTasks;

    @Config.Comment({
        "Generate the new chunks a player's view asks for over the following ticks, nearest first, instead of",
        "all at once: a join or a dimension change at a large view distance otherwise stalls the server long enough",
        "for the client to time out. Chunks already generated load as before." })
    @Config.DefaultBoolean(true)
    @Config.Order(2)
    public static boolean deferInitialChunkGeneration;

    @Config.Comment("Chunks within this radius of a player are generated at once, so the player always stands on loaded ground.")
    @Config.DefaultInt(2)
    @Config.RangeInt(min = 1, max = 4)
    @Config.Order(3)
    public static int initialSynchronousChunkRadius;

    @Config.Comment("Time budget in milliseconds per server tick for generating deferred chunks (at least one a tick).")
    @Config.DefaultInt(20)
    @Config.RangeInt(min = 1, max = 40)
    @Config.Order(4)
    public static int initialChunkGenBudgetMs;
}
