package industries.angelic.biomic.mixins.late.etfuturum;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.client.sound.AmbienceLoop;
import ganymedes01.etfuturum.core.handlers.ClientEventHandler;
import industries.angelic.biomic.client.NetherAmbience;

/**
 * Et Futurum's Nether ambience stays off in the Tartarus Nether while Biomic's is on. It only runs under the vanilla
 * Nether provider, so this matters in stacks without BOP or Netherlicious. Without its loop, Et Futurum also leaves
 * the mood and music alone.
 */
@Mixin(value = ClientEventHandler.class, remap = false)
public abstract class MixinEtFuturumNetherAmbience {

    @Shadow
    private AmbienceLoop netherAmbienceLoop;
    @Shadow
    @Final
    private Minecraft mc;

    @Inject(method = "handleNetherAmbienceLoop", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$tartarusAmbience(CallbackInfo info) {
        if (!NetherAmbience.ownsAny()) return;
        if (netherAmbienceLoop != null) mc.getSoundHandler()
            .stopSound(netherAmbienceLoop);
        netherAmbienceLoop = null;
        info.cancel();
    }
}
