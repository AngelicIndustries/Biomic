package industries.angelic.biomic.mixins.late.angelica;

import net.coderbot.iris.block_rendering.BlockRenderingSettings;
import net.minecraft.block.Block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.biomic.client.render.RuShaderMaterials;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;

/** Runs once per shaderpack material reload, never per vertex/chunk. Optional and client-only. */
@Mixin(value = BlockRenderingSettings.class, remap = false)
public abstract class MixinRuShaderMaterials {

    @Inject(method = "setBlockMetaMatches", at = @At("HEAD"))
    private void biomic$foliageMaterials(Reference2ObjectMap<Block, Int2IntMap> materials, CallbackInfo ci) {
        if (industries.angelic.biomic.config.VisualConfig.shaderVegetationMaterials) {
            int count = RuShaderMaterials.apply(materials, Block.blockRegistry);
            industries.angelic.biomic.Biomic.LOG
                .info("Applied shaderpack-relative foliage materials to {} compatible block states", count);
        }
    }
}
