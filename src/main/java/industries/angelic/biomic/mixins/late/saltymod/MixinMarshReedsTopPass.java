package industries.angelic.biomic.mixins.late.saltymod;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "darkbum.saltymod.block.BlockMarshReeds$BlockMarshReedsTop", remap = false)
public abstract class MixinMarshReedsTopPass {

    @Inject(method = { "getRenderBlockPass", "func_149701_w" }, at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$cutoutTop(CallbackInfoReturnable<Integer> result) {
        result.setReturnValue(0);
    }
}
