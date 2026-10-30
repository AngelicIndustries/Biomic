package industries.angelic.biomic.world.terrain;

import net.minecraft.world.WorldServer;
import net.minecraftforge.event.world.ChunkEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import industries.angelic.biomic.world.ChunkProviderBiomic;

/** Retire pending tree protection when an already-populated source returns from disk. No chunk acquisition. */
public final class NativeTreeLifecycle {

    @SubscribeEvent
    public void loaded(ChunkEvent.Load event) {
        if (event.world instanceof WorldServer world && event.getChunk().isTerrainPopulated
            && world.theChunkProviderServer.currentChunkProvider instanceof ChunkProviderBiomic provider)
            provider.nativeTreeSourceLoaded(event.getChunk());
        else if (event.world instanceof WorldServer world && event.getChunk().isTerrainPopulated
            && world.theChunkProviderServer.currentChunkProvider instanceof industries.angelic.biomic.world.ChunkProviderBiomicTall tall)
            tall.nativeTreeSourceLoaded(event.getChunk());
    }
}
