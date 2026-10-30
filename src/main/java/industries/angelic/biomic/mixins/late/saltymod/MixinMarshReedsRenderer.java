package industries.angelic.biomic.mixins.late.saltymod;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.biomic.client.render.NativeMarshReedsRendering;

/** Pinned native renderer only; registry, models, survival and harvesting remain Salty's. */
@Pseudo
@Mixin(targets = "darkbum.saltymod.block.render.MarshReedsRenderer", remap = false)
public abstract class MixinMarshReedsRenderer {

    @Inject(method = "renderWorldBlock", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$separatePlantAndWater(IBlockAccess world, int x, int y, int z, Block block, int model,
        RenderBlocks renderer, CallbackInfoReturnable<Boolean> result) {
        result.setReturnValue(NativeMarshReedsRendering.render(world, x, y, z, block, renderer));
    }
}
