package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import industries.angelic.biomic.world.JournaledPopulation;

/**
 * Hands Forge's world-generator pass over a Biomic overworld chunk to its population runtime (OrigenFeatureRuntime).
 */
@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServerForeignBox {

    @WrapOperation(
        method = "populate",
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/common/registry/GameRegistry;generateWorld(IILnet/minecraft/world/World;Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/chunk/IChunkProvider;)V",
            remap = false),
        require = 1)
    private void biomic$boxForeign(int chunkX, int chunkZ, World world, IChunkProvider generator,
        IChunkProvider provider, Operation<Void> original) {
        if (generator instanceof JournaledPopulation journaled) journaled.populationRuntime()
            .generateForeign(world, chunkX, chunkZ, () -> original.call(chunkX, chunkZ, world, generator, provider));
        else original.call(chunkX, chunkZ, world, generator, provider);
    }
}
