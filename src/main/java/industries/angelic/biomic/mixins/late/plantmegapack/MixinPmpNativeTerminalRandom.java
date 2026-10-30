package industries.angelic.biomic.mixins.late.plantmegapack;

import java.util.Random;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/**
 * PMP's growth inside Biomic's captured native frames draws the frame's random, not World.rand, and stays in the frame
 * (exact 4.33 with its safety companion, gated in Mixins). Ordinary planted and ticked growth keeps World.rand. The
 * 512-high bounds are Hohen Angst's.
 */
@Pseudo
@Mixin(targets = "plantmegapack.block.PMPBlockPlant", remap = false)
public abstract class MixinPmpNativeTerminalRandom {

    @Redirect(
        method = "updateBambooFromTop",
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;rand:Ljava/util/Random;", remap = true),
        require = 1)
    private Random biomic$terminalBambooRandom(World world) {
        var frame = NativeFeatureCapture.current(world);
        return frame != null && frame.isolated() ? frame.random() : world.rand;
    }

    /**
     * Source decrements below the last stem before its final metadata call. Consume all
     * native RNG draws, but do not turn an isolated native stem into a substrate mutation.
     */
    @Redirect(
        method = "updateBambooFromTop",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/World;setBlockMetadataWithNotify(IIIII)Z",
            remap = true),
        require = 1)
    private boolean biomic$stemMetadataOnly(World world, int x, int y, int z, int metadata, int flags) {
        var frame = NativeFeatureCapture.current(world);
        if (frame != null && frame.isolated() && world.getBlock(x, y, z) != (Object) this) return false;
        return world.setBlockMetadataWithNotify(x, y, z, metadata, flags);
    }

    @Redirect(
        method = "updateVineTick",
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;rand:Ljava/util/Random;", remap = true),
        require = 1)
    private Random biomic$capturedVineRandom(World world) {
        var frame = NativeFeatureCapture.current(world);
        return frame != null && frame.isolated() ? frame.random() : world.rand;
    }

    /**
     * Outside a captured native frame's bounds nothing is air: its double-body or stalk growth must not update its old
     * body before an inevitably rejected new upper cell. (Hohen Angst does the same for a tall world.)
     */
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
        method = { "growPlant", "growStalkPlantFromBottom" },
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isAirBlock(III)Z", remap = true),
        require = 1)
    private boolean biomic$capturedGrowthAir(World world, int x, int y, int z,
        com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        var frame = NativeFeatureCapture.current(world);
        if (frame != null && frame.isolated() && (y < 0 || y >= world.getHeight())) return false;
        return original.call(world, x, y, z);
    }
}
