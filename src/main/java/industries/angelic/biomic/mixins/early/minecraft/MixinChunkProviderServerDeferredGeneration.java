package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.biomic.world.DeferredChunkGeneration;

/** A chunk watcher's request for a chunk that must be generated waits for a later tick (DeferredChunkGeneration). */
@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServerDeferredGeneration {

    @Inject(
        method = "loadChunk(IILjava/lang/Runnable;)Lnet/minecraft/world/chunk/Chunk;",
        at = @At("HEAD"),
        cancellable = true,
        remap = false,
        require = 1)
    private void biomic$deferGeneration(int chunkX, int chunkZ, Runnable runnable, CallbackInfoReturnable<Chunk> info) {
        if (DeferredChunkGeneration.INSTANCE.defer((ChunkProviderServer) (Object) this, chunkX, chunkZ, runnable))
            info.setReturnValue(null);
    }
}
