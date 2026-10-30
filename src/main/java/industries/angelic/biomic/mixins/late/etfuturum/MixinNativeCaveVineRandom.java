package industries.angelic.biomic.mixins.late.etfuturum;

import java.util.Random;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** EFR's growth method uses World.rand even when its caller supplies a feature Random. */
@Mixin(targets = "ganymedes01.etfuturum.blocks.BlockCaveVines", remap = false)
public abstract class MixinNativeCaveVineRandom {

    @Redirect(
        method = "growVine",
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;rand:Ljava/util/Random;", remap = true),
        require = 2)
    private Random biomic$featureRandom(World world) {
        var frame = NativeFeatureCapture.current(world);
        return frame != null && frame.isolated() ? frame.random() : world.rand;
    }
}
