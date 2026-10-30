package industries.angelic.biomic.world;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.event.world.ChunkEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import industries.angelic.biomic.world.terrain.OrigenFeatureRuntime;

/**
 * Order-independent population's chunk lifecycle: a chunk's population tags are saved and loaded with it, and the
 * writes neighbours' populations queued while it was not loaded land when it loads. Never loads a chunk.
 */
public final class PopulationLifecycle {

    private static OrigenFeatureRuntime runtime(World world) {
        if (!(world instanceof WorldServer server)
            || !(server.theChunkProviderServer.currentChunkProvider instanceof JournaledPopulation provider))
            return null;
        OrigenFeatureRuntime runtime = provider.populationRuntime();
        return runtime.journaledPopulation() ? runtime : null;
    }

    @SubscribeEvent
    public void loaded(ChunkEvent.Load event) {
        OrigenFeatureRuntime runtime = runtime(event.world);
        if (runtime != null) runtime.applyPendingOverflow(event.world, event.getChunk());
    }

    @SubscribeEvent
    public void unloaded(ChunkEvent.Unload event) {
        Chunk chunk = event.getChunk();
        OrigenFeatureRuntime runtime = runtime(event.world);
        if (runtime != null) runtime.unloadPopulationTags(chunk.xPosition, chunk.zPosition);
    }

    @SubscribeEvent
    public void saved(ChunkDataEvent.Save event) {
        Chunk chunk = event.getChunk();
        OrigenFeatureRuntime runtime = runtime(event.world);
        if (runtime != null) runtime.savePopulationTags(
            chunk.xPosition,
            chunk.zPosition,
            event.getData()
                .getCompoundTag("Level"));
    }

    @SubscribeEvent
    public void read(ChunkDataEvent.Load event) {
        Chunk chunk = event.getChunk();
        OrigenFeatureRuntime runtime = runtime(event.world);
        if (runtime != null) runtime.loadPopulationTags(
            chunk.xPosition,
            chunk.zPosition,
            event.getData()
                .getCompoundTag("Level"));
    }
}
