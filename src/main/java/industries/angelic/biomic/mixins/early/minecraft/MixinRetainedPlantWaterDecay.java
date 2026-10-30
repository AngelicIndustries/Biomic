package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.block.BlockLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.biomic.content.block.RetainedPlantSourceWater;

/** Source strength is independent of vegetation metadata, both for simulation and water velocity. */
@Mixin(BlockLiquid.class)
public abstract class MixinRetainedPlantWaterDecay {

    private boolean biomic$water() {
        return (Object) this == Blocks.water || (Object) this == Blocks.flowing_water;
    }

    @Inject(method = "func_149804_e", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$sourceDecay(World world, int x, int y, int z, CallbackInfoReturnable<Integer> result) {
        if (biomic$water() && RetainedPlantSourceWater.contains(world, x, y, z)) result.setReturnValue(0);
    }

    @Inject(method = "getEffectiveFlowDecay", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$effectiveSourceDecay(IBlockAccess world, int x, int y, int z,
        CallbackInfoReturnable<Integer> result) {
        if (biomic$water() && RetainedPlantSourceWater.contains(world, x, y, z)) result.setReturnValue(0);
    }

    @Redirect(
        method = "getFlowVector",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IBlockAccess;getBlockMetadata(III)I"),
        require = 1)
    private int biomic$velocityMetadata(IBlockAccess world, int x, int y, int z) {
        return biomic$water() ? RetainedPlantSourceWater.fluidMetadata(world, x, y, z)
            : world.getBlockMetadata(x, y, z);
    }
}
