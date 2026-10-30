package industries.angelic.biomic.mixins.late.etfuturum;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.world.EtFuturumLateWorldGenerator;
import industries.angelic.biomic.compat.efr.EfrGenerationFirewall;
import industries.angelic.biomic.compat.efr.EfrGeneratorStage;

@Mixin(value = EtFuturumLateWorldGenerator.class, remap = false)
public class MixinEtFuturumLateWorldGenerator {

    @Inject(method = "generate", at = @At("HEAD"), cancellable = true, require = 1, expect = 1)
    private void biomic$filterLateGenerator(Random random, int chunkX, int chunkZ, World world,
        IChunkProvider chunkGenerator, IChunkProvider chunkProvider, CallbackInfo callbackInfo) {
        if (EfrGenerationFirewall.observe(EfrGeneratorStage.LATE, chunkX, chunkZ, world)
            .cancelsGeneration()) {
            callbackInfo.cancel();
        }
    }
}
