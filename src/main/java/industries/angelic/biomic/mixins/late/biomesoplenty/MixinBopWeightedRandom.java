package industries.angelic.biomic.mixins.late.biomesoplenty;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/**
 * BOP picks its weighted grass and flowers with {@code Math.random()}, so its biomes in the Tartarus Nether came out
 * different on every run. Inside a Biomic capture the pick draws on the capture's chunk-seeded random instead;
 * elsewhere BOP is unchanged.
 */
@Pseudo
@Mixin(targets = "biomesoplenty.api.biome.BOPBiomeDecorator", remap = false)
public abstract class MixinBopWeightedRandom {

    @Redirect(
        method = "getRandomWeightedWorldGenerator",
        at = @At(value = "INVOKE", target = "Ljava/lang/Math;random()D"),
        require = 1)
    private static double biomic$captureRandom() {
        Random random = NativeFeatureCapture.isolatedRandomOnThread();
        return random == null ? Math.random() : random.nextDouble();
    }
}
