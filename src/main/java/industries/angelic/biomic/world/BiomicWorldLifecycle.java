package industries.angelic.biomic.world;

import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Releases provider-owned background work without resolving or loading any chunks. */
public final class BiomicWorldLifecycle {

    public static final BiomicWorldLifecycle INSTANCE = new BiomicWorldLifecycle();

    private BiomicWorldLifecycle() {}

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (!(event.world instanceof WorldServer world) || event.world.isRemote) return;
        IChunkProvider provider = world.theChunkProviderServer == null ? null
            : world.theChunkProviderServer.currentChunkProvider;
        if (provider instanceof ChunkProviderBiomic biomic) biomic.shutdownTerrainPrecompute();
        if (provider instanceof ChunkProviderBiomicTall tall) tall.shutdownTerrainPrecompute();
        if (provider instanceof ChunkProviderBiomicNether nether) nether.shutdownTerrainPrecompute();
    }
}
