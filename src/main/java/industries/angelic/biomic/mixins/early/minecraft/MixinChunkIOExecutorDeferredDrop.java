package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.World;
import net.minecraftforge.common.chunkio.ChunkIOExecutor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.biomic.world.DeferredChunkGeneration;

/** A watcher that stops waiting for a chunk Biomic deferred drops it there; Forge's own queue never held it. */
@Mixin(value = ChunkIOExecutor.class, remap = false)
public abstract class MixinChunkIOExecutorDeferredDrop {

    @Inject(method = "dropQueuedChunkLoad", at = @At("HEAD"), cancellable = true, require = 1)
    private static void biomic$dropDeferred(World world, int chunkX, int chunkZ, Runnable runnable, CallbackInfo info) {
        if (DeferredChunkGeneration.INSTANCE.drop(world, chunkX, chunkZ, runnable)) info.cancel();
    }
}
