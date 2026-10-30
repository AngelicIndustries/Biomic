package industries.angelic.biomic.mixins.late.villagenames;

import java.util.ArrayList;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.biomic.world.structure.VillageContext;

/**
 * Village Names reads every biome's village type and materials from its config list. For a Biomic village it gets that
 * village's own style instead, chosen from its site and 3D biome; its config list stays untouched.
 */
@Pseudo
@Mixin(targets = "astrotibs.villagenames.config.village.VillageGeneratorConfigHandler", remap = false)
public abstract class MixinVillageNamesStyle {

    @Inject(method = "unpackBiomes", at = @At("HEAD"), cancellable = true, remap = false, require = 1)
    private static void biomic$villageStyle(String[] list,
        CallbackInfoReturnable<Map<String, ArrayList<String>>> result) {
        Map<String, ArrayList<String>> style = VillageContext.villageNamesMapping();
        if (style != null) result.setReturnValue(style);
    }
}
