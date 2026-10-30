package industries.angelic.biomic.mixins.late.angelica;

import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizons.angelica.rendering.celeritas.SmoothBiomeColorCache;

import industries.angelic.biomic.client.HostedBiomeColors;

/** Preserve Angelica's existing 3D cache/blending; supply the Y value its stock biome callback discarded. */
@Mixin(value = SmoothBiomeColorCache.class, remap = false)
public abstract class MixinSmoothBiomeColors {

    @Inject(method = "lambda$new$0", at = @At("HEAD"), cancellable = true)
    private static void biomic$resolve(IBlockAccess access, int x, int y, int z,
        CallbackInfoReturnable<BiomeGenBase> cir) {
        cir.setReturnValue(HostedBiomeColors.biome(access, x, y, z));
    }
}
