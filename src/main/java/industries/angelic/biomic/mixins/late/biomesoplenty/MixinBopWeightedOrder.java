package industries.angelic.biomic.mixins.late.biomesoplenty;

import java.util.HashMap;
import java.util.LinkedHashMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * BOP's weighted grass and flower maps, in the order the biome lists them. As plain HashMaps keyed by generator
 * objects they were walked in identity-hash order, which changes with every run, so even a seeded pick differed.
 */
@Pseudo
@Mixin(targets = "biomesoplenty.api.biome.BiomeFeatures", remap = false)
public abstract class MixinBopWeightedOrder {

    @Redirect(method = "<init>", at = @At(value = "NEW", target = "java/util/HashMap"), require = 2)
    private HashMap<?, ?> biomic$ordered() {
        return new LinkedHashMap<>();
    }
}
