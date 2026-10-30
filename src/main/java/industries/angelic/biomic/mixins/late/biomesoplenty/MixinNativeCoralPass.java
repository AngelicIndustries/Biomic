package industries.angelic.biomic.mixins.late.biomesoplenty;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.biomic.client.BiomicRenderIds;
import industries.angelic.biomic.content.block.WaterloggedRenderPlant;

/** Keep native coral/kelp/algae content and mechanics; separate plant cutout from source-water geometry. */
@Pseudo
@Mixin(targets = "biomesoplenty.common.blocks.BlockBOPCoral", remap = false)
public abstract class MixinNativeCoralPass extends Block implements WaterloggedRenderPlant {

    protected MixinNativeCoralPass(Material material) {
        super(material);
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }

    // Optional target is absent from compileClasspath: cover both MCP and shipped SRG names.
    @Inject(
        method = { "getRenderType", "func_149645_b" },
        at = @At("HEAD"),
        cancellable = true,
        require = 1,
        remap = false)
    private void biomic$renderType(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(BiomicRenderIds.NATIVE_AQUATIC);
    }
}
