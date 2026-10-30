package industries.angelic.biomic.mixins.late.netherlicious;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.common.gameevent.TickEvent;
import industries.angelic.biomic.client.NetherAmbience;

/**
 * Netherlicious picks its loops and additions by biome name, so most Tartarus biomes would get the wastes loop. It
 * keeps quiet in the Tartarus Nether while Biomic plays the biome loops; its particles and fog stay.
 */
@Pseudo
@Mixin(targets = "DelirusCrux.Netherlicious.Client.Utility.NetherAmbienceEventHandler", remap = false)
public abstract class MixinNetherliciousAmbience {

    @Inject(method = "onClientTick", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void biomic$tartarusLoops(TickEvent.ClientTickEvent event, CallbackInfo info) {
        if (NetherAmbience.ownsLoops()) info.cancel();
    }
}
