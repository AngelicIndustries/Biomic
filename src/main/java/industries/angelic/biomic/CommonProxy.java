package industries.angelic.biomic;

import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import industries.angelic.biomic.biome.BiomicBiomes;
import industries.angelic.biomic.bootstrap.BootstrapValidator;
import industries.angelic.biomic.config.BiomicConfigManager;
import industries.angelic.biomic.content.BiomicBlocks;
import industries.angelic.biomic.network.BiomicNetwork;
import industries.angelic.biomic.storage.BiomeVolumeManager;
import industries.angelic.biomic.world.BiomicWorldLifecycle;

public class CommonProxy {

    /** Client hook for the source whistle smoke; deliberately inert on dedicated servers. */
    public void gardensWhistle(int x, int y, int z) {}

    /** Where a Biomic world's clouds float (see WorldTypeBiomicBlend#getCloudHeight); a server has none to draw. */
    public float cloudHeight() {
        return 128;
    }

    public void preInit() {
        BiomicConfigManager.initialize();
        BootstrapValidator.validate();
        BiomicBlocks.register();
        industries.angelic.biomic.content.alexscaves.AbyssalContentBlocks.register();
        BiomicBiomes.register();
        industries.angelic.biomic.world.WorldTypeBiomicBlend.register();
        industries.angelic.biomic.compat.flora.ReplacedGenerators.register();
        industries.angelic.biomic.compat.HohenAngstGrowth.register();
        BiomicNetwork.initialize();
        Biomic.LOG.info("I am {} at version {}", Biomic.MODNAME, Tags.VERSION);
    }

    public void init(FMLInitializationEvent event) {
        BiomeVolumeManager.register();
        MinecraftForge.EVENT_BUS.register(BiomicWorldLifecycle.INSTANCE);

        MinecraftForge.EVENT_BUS.register(new industries.angelic.biomic.world.terrain.NativeTreeLifecycle());
        MinecraftForge.EVENT_BUS.register(new industries.angelic.biomic.world.PopulationLifecycle());
        FMLCommonHandler.instance()
            .bus()
            .register(industries.angelic.biomic.world.DeferredChunkGeneration.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(industries.angelic.biomic.browser.BrowserServer.INSTANCE);
        industries.angelic.biomic.world.ServerTickTiming.enableIfRequested();
        industries.angelic.biomic.world.structure.BiomicScatteredFeatures.register();
    }

    public void postInit() {
        industries.angelic.biomic.compat.biome.NativeBiomeCatalogue.discoverInstalled();
        industries.angelic.biomic.compat.biome.NativeBlockCatalogue.discoverInstalled();
    }
}
