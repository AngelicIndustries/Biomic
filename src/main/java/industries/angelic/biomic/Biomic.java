package industries.angelic.biomic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLConstructionEvent;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import industries.angelic.biomic.bootstrap.EfrContentContract;
import industries.angelic.biomic.compat.efr.VanillaArtwork;
import industries.angelic.biomic.config.DebugConfig;
import industries.angelic.biomic.world.WorldgenBenchmark;

@Mod(
    modid = Biomic.MODID,
    name = Biomic.MODNAME,
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = Biomic.DEPENDENCIES,
    customProperties = { @Mod.CustomProperty(k = "issueTrackerUrl", v = Biomic.ISSUE_TRACKER_URL) })
public class Biomic {

    public static final String MODID = "biomic";
    public static final String MODNAME = "Biomic";
    public static final String MODGROUP = "industries.angelic";
    public static final String ISSUE_TRACKER_URL = "https://github.com/AngelicIndustries/Biomic/issues";
    public static final String ANGELLIB_MIN_VERSION = "1.0.0";
    public static final String EFR_MIN_VERSION = "2.6.54-GTNH";
    public static final String ENDLESS_IDS_MIN_VERSION = "1.7.4";
    public static final String CHUNK_API_MIN_VERSION = "0.8.4";
    public static final String GTNH_LIB_MIN_VERSION = "0.11.39";
    public static final String DEPENDENCIES = "required-after:angellib@[" + ANGELLIB_MIN_VERSION
        + ",);"
        + "required-after:etfuturum@["
        + EFR_MIN_VERSION
        + ",);"
        + "required-after:endlessids@["
        + ENDLESS_IDS_MIN_VERSION
        + ",);"
        + "required-after:chunkapi@["
        + CHUNK_API_MIN_VERSION
        + ",);"
        + "required-after:gtnhlib@["
        + GTNH_LIB_MIN_VERSION
        + ",)";

    public static final Logger LOG = LogManager.getLogger(MODID);

    private static boolean environmentDebugMode;

    @SidedProxy(clientSide = MODGROUP + ".biomic.ClientProxy", serverSide = MODGROUP + ".biomic.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void construct(FMLConstructionEvent event) {
        EfrContentContract.enforce();
        if (event.getSide()
            .isClient()) VanillaArtwork.request();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        environmentDebugMode = System.getenv("MCMODDING_DEBUG_MODE") != null;
        proxy.preInit();
        LOG.info("MCMODDING_DEBUG_MODE env var: {}", environmentDebugMode);
        LOG.info("debugMode config option: {}", DebugConfig.debugMode);
        LOG.info("isDebugMode: {}", isDebugMode());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit();
    }

    /** Blocks that moved to another mod keep their worlds (MovedBlocks). */
    @Mod.EventHandler
    public void missingMappings(cpw.mods.fml.common.event.FMLMissingMappingsEvent event) {
        industries.angelic.biomic.content.MovedBlocks.remap(event);
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        WorldgenBenchmark.runIfRequested();
        industries.angelic.biomic.browser.BrowserServer.probeIfRequested();
        industries.angelic.biomic.browser.BrowserServer.dumpCatalogueIfRequested();
    }

    public static boolean isDebugMode() {
        return environmentDebugMode || DebugConfig.debugMode;
    }

}
