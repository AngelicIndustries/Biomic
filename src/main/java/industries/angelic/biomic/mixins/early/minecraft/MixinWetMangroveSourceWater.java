package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.biomic.content.block.RetainedPlantSourceWater;

/** Preserve exact retained-source companions; all other native fluid cells retain their behavior. */
@Mixin(BlockDynamicLiquid.class)
public abstract class MixinWetMangroveSourceWater {

    @Inject(method = "func_149811_n", at = @At("HEAD"), cancellable = true, require = 1)
    private void biomic$retainRoot(World world, int x, int y, int z, CallbackInfo result) {
        if ((Object) this != Blocks.flowing_water) return;
        if (RetainedPlantSourceWater.contains(world, x, y, z)) result.cancel();
    }

    @Redirect(
        method = { "updateTick", "func_149812_c", "func_149808_o" },
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getBlockMetadata(III)I"),
        require = 3)
    private int biomic$sourceMetadata(World world, int x, int y, int z) {
        return (Object) this == Blocks.flowing_water ? RetainedPlantSourceWater.fluidMetadata(world, x, y, z)
            : world.getBlockMetadata(x, y, z);
    }
}
