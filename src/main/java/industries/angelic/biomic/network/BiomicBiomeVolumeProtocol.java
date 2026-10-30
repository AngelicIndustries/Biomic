package industries.angelic.biomic.network;

import java.util.Map;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerAboutToStartEvent;
import cpw.mods.fml.common.event.FMLServerStoppedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.NetworkCheckHandler;
import cpw.mods.fml.relauncher.Side;
import industries.angelic.biomic.world.RequiredWorldHeight;

/** Conditional peer barrier for Biomic's independently versioned biome-volume protocol. */
@Mod(
    modid = BiomicPeerRequirements.BIOME_VOLUME_MARKER,
    name = "Biomic Biome Volume Protocol",
    version = BiomicPeerRequirements.BIOME_VOLUME_REVISION,
    acceptableRemoteVersions = "*",
    useMetadata = false)
public final class BiomicBiomeVolumeProtocol {

    @Mod.Instance(BiomicPeerRequirements.BIOME_VOLUME_MARKER)
    public static BiomicBiomeVolumeProtocol INSTANCE;
    private final BiomicPeerSession session = new BiomicPeerSession();
    private volatile long activeToken;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public synchronized void aboutToStart(FMLServerAboutToStartEvent event) {
        activeToken = session.beginSession();
    }

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load event) {
        World world = event.world;
        if (!(world instanceof WorldServer) || world.isRemote || world.provider.dimensionId != 0) return;
        RequiredWorldHeight.initialize(world);
        int requiredHeight = RequiredWorldHeight.validatedRequiredHeight(world);
        validateLocalHeightProtocol(requiredHeight, loadedVersion(BiomicPeerRequirements.HEIGHT_MARKER));
        int schema = 1;
        if (industries.angelic.biomic.world.TallWorlds.liveTall(world)) {
            var settings = industries.angelic.biomic.world.BiomicWorldSettings.peekLoaded(world);
            if (settings == null)
                throw new IllegalStateException("Tall world has no initialized authored-height settings");
            schema = settings
                .getAuthoredHeightPolicy(
                    requiredHeight,
                    industries.angelic.biomic.storage.BiomeVolumeManager.SUPPORTS_SCHEMA_TWO)
                .biomeVolumeSchema();
            if (schema != 2) throw new IllegalStateException("Tall world did not select schema 2");
        }
        publishValidatedRequirements(schema, requiredHeight);
    }

    @NetworkCheckHandler
    public boolean compatible(Map<String, String> remoteMods, Side remoteSide) {
        if (remoteSide == Side.SERVER) return true;
        if (remoteSide != Side.CLIENT || remoteMods == null) return false;
        BiomicPeerRequirements.Capabilities capabilities = BiomicPeerRequirements.Capabilities.of(
            remoteMods.get(BiomicPeerRequirements.BIOME_VOLUME_MARKER),
            remoteMods.get(BiomicPeerRequirements.HEIGHT_MARKER));
        return session.accepts(BiomicPeerRequirements.Direction.SERVER_CHECKING_CLIENT, capabilities);
    }

    @Mod.EventHandler
    public synchronized void stopped(FMLServerStoppedEvent event) {
        session.endSession(activeToken);
        activeToken = 0;
    }

    void publishValidatedRequirements(int schema, int requiredHeight) {
        if (schema == 2 && requiredHeight != 512)
            throw new IllegalArgumentException("Schema 2 requires physical height 512");
        BiomicPeerSession.PublicationResult result = session
            .publish(activeToken, BiomicPeerRequirements.Requirements.of(schema, requiredHeight));
        if (result != BiomicPeerSession.PublicationResult.PUBLISHED
            && result != BiomicPeerSession.PublicationResult.ALREADY_PUBLISHED_IDENTICAL)
            throw new IllegalStateException("Cannot publish Biomic peer requirements: " + result);
    }

    /** Read-only writer guard. A registered marker alone is not an active server's negotiated policy. */
    public static boolean schemaTwoSessionPublished() {
        BiomicBiomeVolumeProtocol marker = INSTANCE;
        if (marker == null) return false;
        BiomicPeerSession.Snapshot snapshot = marker.session.snapshot();
        return snapshot.active() && snapshot.published()
            && snapshot.requirements()
                .biomeVolumeSchema() == 2
            && snapshot.requirements()
                .requiredPhysicalHeight() == 512;
    }

    static void validateLocalHeightProtocol(int requiredHeight, String loadedRevision) {
        if (requiredHeight != 256 && requiredHeight != 512)
            throw new IllegalArgumentException("Unsupported required physical height " + requiredHeight);
        if (requiredHeight == 512 && !BiomicPeerRequirements.HEIGHT_512_REVISION.equals(loadedRevision))
            throw new IllegalStateException(
                "512-block storage requires local " + BiomicPeerRequirements.HEIGHT_MARKER
                    + "@"
                    + BiomicPeerRequirements.HEIGHT_512_REVISION);
    }

    private static String loadedVersion(String modid) {
        ModContainer container = Loader.instance()
            .getIndexedModList()
            .get(modid);
        return container == null ? null : container.getVersion();
    }
}
