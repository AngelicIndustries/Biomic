package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunkProvider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.biomic.world.ChunkProviderBiomicNether;
import industries.angelic.biomic.world.NetherWorldData;

/** The Tartarus Nether's generator, whichever provider owns dimension -1. */
@Mixin(WorldServer.class)
public abstract class MixinWorldServerBiomicNether {

    @Redirect(
        method = "createChunkProvider",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/WorldProvider;createChunkGenerator()Lnet/minecraft/world/chunk/IChunkProvider;"),
        require = 1)
    private IChunkProvider biomic$tartarusGenerator(WorldProvider provider) {
        WorldServer world = (WorldServer) (Object) this;
        return NetherWorldData.claims(world) ? new ChunkProviderBiomicNether(world) : provider.createChunkGenerator();
    }
}
