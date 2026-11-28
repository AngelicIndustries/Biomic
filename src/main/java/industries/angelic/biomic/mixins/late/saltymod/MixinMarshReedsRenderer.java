package industries.angelic.biomic.mixins.late.saltymod;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.biomic.client.render.NativeMarshReedsRendering;

/** Pinned native renderer only; registry, models, survival and harvesting remain Salty's. */
@Pseudo
@Mixin(targets = "darkbum.saltymod.block.render.MarshReedsRenderer", remap = false)
public abstract class MixinMarshReedsRenderer {

    /** Salty's pick of a column's reed texture, one of four. */
    @Shadow(remap = false)
    private static int getVariant(int x, int z) {
        throw new AssertionError();
    }

    @Inject(method = "renderWorldBlock", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$separatePlantAndWater(IBlockAccess world, int x, int y, int z, Block block, int model,
        RenderBlocks renderer, CallbackInfoReturnable<Boolean> result) {
        result.setReturnValue(
            NativeMarshReedsRendering.render(world, x, y, z, block, renderer, biomic$icon(world, x, y, z, block)));
    }

    /**
     * With Salty's diverseMarshReeds on, each column takes one of four textures, and the first is the block's own
     * icon; otherwise the variants stay unregistered and the block's icon is the single texture.
     */
    @Unique
    private static IIcon biomic$icon(IBlockAccess world, int x, int y, int z, Block block) {
        IIcon own = block.getIcon(0, world.getBlockMetadata(x, y, z));
        IIcon[] variants = block instanceof AccessorMarshReedsBottom bottom ? bottom.biomic$variants()
            : block instanceof AccessorMarshReedsTop top ? top.biomic$variants() : null;
        return variants != null && variants[0] != null && variants[0] == own ? variants[getVariant(x, z)] : own;
    }
}
