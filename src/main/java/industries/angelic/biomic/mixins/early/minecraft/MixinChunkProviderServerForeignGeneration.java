package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import industries.angelic.biomic.world.GenerationScope;

/**
 * Marks Forge's world-generator pass of a population (GenerationScope#foreign), and the provider's own pass as not it:
 * a chunk another mod's generator makes generate populates inside that pass. Other mods' generators run on the world,
 * whole, as in any world type; the cells they change leave order-independent population's claims
 * (OrigenFeatureRuntime#foreignWrite).
 */
@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServerForeignGeneration {

    @WrapOperation(
        method = "populate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/chunk/IChunkProvider;populate(Lnet/minecraft/world/chunk/IChunkProvider;II)V"),
        require = 1)
    private void biomic$providerPass(IChunkProvider generator, IChunkProvider provider, int chunkX, int chunkZ,
        Operation<Void> original) {
        GenerationScope.pass(false, () -> original.call(generator, provider, chunkX, chunkZ));
    }

    @WrapOperation(
        method = "populate",
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/common/registry/GameRegistry;generateWorld(IILnet/minecraft/world/World;Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/chunk/IChunkProvider;)V",
            remap = false),
        require = 1)
    private void biomic$foreignPass(int chunkX, int chunkZ, World world, IChunkProvider generator,
        IChunkProvider provider, Operation<Void> original) {
        GenerationScope.pass(true, () -> original.call(chunkX, chunkZ, world, generator, provider));
    }
}
