package industries.angelic.biomic.mixins.early.minecraft;

import java.util.Random;

import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import industries.angelic.biomic.world.GenerationRandom;
import industries.angelic.biomic.world.GenerationScope;

/**
 * Marks a chunk's population, the provider's and other mods' generators alike (GameRegistry.generateWorld runs inside
 * it), so what is meant for generation leaves what players do alone (GenerationScope). In a Biomic world the
 * population also draws world.rand from a random of the chunk's own (GenerationRandom).
 */
@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServerGenerating {

    @Shadow
    public IChunkProvider currentChunkProvider;
    @Shadow
    public WorldServer worldObj;

    @WrapMethod(method = "populate")
    private void biomic$markGeneration(IChunkProvider provider, int chunkX, int chunkZ, Operation<Void> original) {
        Random worldRandom = worldObj.rand;
        boolean own = GenerationRandom.applies(worldObj, currentChunkProvider);
        if (own) worldObj.rand = GenerationRandom.forChunk(worldObj, chunkX, chunkZ);
        GenerationScope.enter();
        try {
            original.call(provider, chunkX, chunkZ);
        } finally {
            GenerationScope.leave();
            if (own) worldObj.rand = worldRandom;
        }
    }
}
