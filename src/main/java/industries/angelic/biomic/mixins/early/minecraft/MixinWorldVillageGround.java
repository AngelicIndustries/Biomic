package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.biomic.world.structure.VillageContext;

/**
 * While Biomic populates one of its villages, vanilla's village pieces (and bugtorch's replacements of them) find
 * ground only in that village's layer: under the islands for a ground village, on its island for a sky village.
 */
@Mixin(World.class)
public abstract class MixinWorldVillageGround {

    @Inject(method = "getTopSolidOrLiquidBlock", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$villageGround(int x, int z, CallbackInfoReturnable<Integer> result) {
        int ground = VillageContext.groundAbove((World) (Object) this, x, z, false);
        if (ground != Integer.MIN_VALUE) result.setReturnValue(ground);
    }
}
