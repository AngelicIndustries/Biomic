package industries.angelic.biomic.mixins.late.biomesoplenty;

import java.util.Random;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

/** Keeps BOP's two hive-shell World.rand reads inside the saved feature RNG stream. */
@Pseudo
@Mixin(targets = "biomesoplenty.common.world.features.nether.WorldGenWaspHive", remap = false)
public abstract class MixinBopWaspHiveRandom {

    @Redirect(
        method = "generateHiveCube",
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;rand:Ljava/util/Random;", remap = true),
        require = 2)
    private Random biomic$largeCubeRandom(World world) {
        return scoped(world);
    }

    @Redirect(
        method = "generateHiveCubeSmall",
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/World;rand:Ljava/util/Random;", remap = true),
        require = 2)
    private Random biomic$smallCubeRandom(World world) {
        return scoped(world);
    }

    private static Random scoped(World world) {
        var frame = NativeFeatureCapture.current(world);
        return frame != null && frame.isolated() ? frame.random() : world.rand;
    }
}
