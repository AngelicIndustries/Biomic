package industries.angelic.biomic.mixins.early.minecraft;

import java.util.Set;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.biomic.world.DeferredChunkGeneration;

/** Vanilla's per-tick passes over the chunks near players skip those still waiting for deferred generation. */
@Mixin(World.class)
public abstract class MixinWorldDeferredActiveChunks {

    @Shadow
    protected Set<ChunkCoordIntPair> activeChunkSet;

    @Inject(method = "setActivePlayerChunksAndCheckLight", at = @At("TAIL"), require = 1)
    private void biomic$skipWaiting(CallbackInfo info) {
        DeferredChunkGeneration.INSTANCE.dropWaiting((World) (Object) this, activeChunkSet);
    }
}
