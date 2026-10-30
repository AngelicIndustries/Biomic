package industries.angelic.biomic.mixins.late.etfuturum;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ganymedes01.etfuturum.api.event.PostTreeGenerateEvent;
import ganymedes01.etfuturum.core.handlers.WorldEventHandler;
import industries.angelic.biomic.compat.efr.EfrGenerationFirewall;

@Mixin(value = WorldEventHandler.class, remap = false)
public class MixinWorldEventHandler {

    @Inject(method = "onTreeGenerated", at = @At("HEAD"), cancellable = true, require = 1, expect = 1)
    private void biomic$filterTreePostProcess(PostTreeGenerateEvent event, CallbackInfo callbackInfo) {
        if (EfrGenerationFirewall.observeTreePostProcess(event.x, event.z, event.world)
            .cancelsGeneration()) {
            callbackInfo.cancel();
        }
    }
}
