package industries.angelic.biomic.mixins.late.netherlicious;

import net.minecraftforge.client.event.sound.PlaySoundEvent17;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.biomic.client.NetherAmbience;

/** Netherlicious leaves the Nether music alone in the Tartarus Nether while Biomic picks it. */
@Pseudo
@Mixin(targets = "DelirusCrux.Netherlicious.Client.Utility.NetherMusic", remap = false)
public abstract class MixinNetherliciousMusic {

    @Inject(method = "onPlaySoundEvent", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private void biomic$tartarusMusic(PlaySoundEvent17 event, CallbackInfo info) {
        if (NetherAmbience.ownsMusic()) info.cancel();
    }
}
