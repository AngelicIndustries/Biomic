package industries.angelic.biomic.config;

import com.gtnewhorizon.gtnhlib.config.ConfigurationManager;

public final class BiomicConfigManager {

    private static boolean initialized;

    private BiomicConfigManager() {}

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }
        ConfigurationManager.registerConfig(BiomeConfig.class);
        ConfigurationManager.registerConfig(VegetationConfig.class);
        ConfigurationManager.registerConfig(ExpansionConfig.class);
        ConfigurationManager.registerConfig(VisualConfig.class);
        ConfigurationManager.registerConfig(AmbienceConfig.class);
        ConfigurationManager.registerConfig(DebugConfig.class);
        ConfigurationManager.registerConfig(PerformanceConfig.class);
        StructureConfig.load(
            cpw.mods.fml.common.Loader.instance()
                .getConfigDir());
        initialized = true;
    }
}
