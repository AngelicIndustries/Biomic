package industries.angelic.biomic.mixins.late.angelica;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import industries.angelic.biomic.client.render.AquaticRenderSemantics;

/** Material.water describes the contents, not our pass-zero plant mesh. */
@Mixin(targets = "com.gtnewhorizons.angelica.rendering.celeritas.AngelicaChunkBuilderMeshingTask", remap = false)
public abstract class MixinAquaticRenderPass {

    @ModifyExpressionValue(
        method = "renderBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/block/Block;getMaterial()Lnet/minecraft/block/material/Material;",
            remap = true))
    private Material biomic$plantMeshIsNotFluid(Material material, @Local(argsOnly = true) Block block,
        @Local(argsOnly = true, ordinal = 4) int pass) {
        return AquaticRenderSemantics.material(block, material, pass);
    }
}
