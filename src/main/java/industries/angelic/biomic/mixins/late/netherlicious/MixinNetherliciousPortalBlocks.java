package industries.angelic.biomic.mixins.late.netherlicious;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.biomic.world.NetherForeignGeneration;

/**
 * Netherlicious' ruined portals are built partly of its own blackstone and magma, which the Tartarus Nether refuses
 * from other mods' generators (their scattered blobs duplicate Tartarus' own). A portal keeps its blocks.
 */
@Pseudo
@Mixin(targets = "DelirusCrux.Netherlicious.World.Worldgen.NetherPortalGen", remap = false)
public abstract class MixinNetherliciousPortalBlocks {

    @Inject(method = "generate", at = @At("HEAD"), remap = false, require = 1)
    private void biomic$structureBegins(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator,
        IChunkProvider provider, CallbackInfo info) {
        NetherForeignGeneration.beginStructure();
    }

    @Inject(method = "generate", at = @At("RETURN"), remap = false, require = 1)
    private void biomic$structureEnds(Random random, int chunkX, int chunkZ, World world, IChunkProvider generator,
        IChunkProvider provider, CallbackInfo info) {
        NetherForeignGeneration.endStructure();
    }
}
