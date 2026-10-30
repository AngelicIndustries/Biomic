package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.ChunkProviderServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.biomic.world.NetherForeignGeneration;

/** Marks Forge's world-generator pass while it runs over the Tartarus Nether. */
@Mixin(ChunkProviderServer.class)
public abstract class MixinChunkProviderServerNetherGeneration {

    @Shadow
    public IChunkProvider currentChunkProvider;

    private static final String GENERATE_WORLD = "Lcpw/mods/fml/common/registry/GameRegistry;generateWorld(IILnet/minecraft/world/World;Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/chunk/IChunkProvider;)V";

    @Inject(method = "populate", at = @At(value = "INVOKE", target = GENERATE_WORLD, remap = false), require = 1)
    private void biomic$beginForeign(IChunkProvider provider, int chunkX, int chunkZ, CallbackInfo info) {
        NetherForeignGeneration.begin(currentChunkProvider);
    }

    @Inject(
        method = "populate",
        at = @At(value = "INVOKE", target = GENERATE_WORLD, shift = At.Shift.AFTER, remap = false),
        require = 1)
    private void biomic$endForeign(IChunkProvider provider, int chunkX, int chunkZ, CallbackInfo info) {
        NetherForeignGeneration.end();
    }
}
