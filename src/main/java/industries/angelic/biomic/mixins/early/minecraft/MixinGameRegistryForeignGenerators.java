package industries.angelic.biomic.mixins.early.minecraft;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import cpw.mods.fml.common.IWorldGenerator;
import cpw.mods.fml.common.registry.GameRegistry;
import industries.angelic.biomic.compat.flora.ReplacedGenerators;

/** Forge's world-generator pass skips the generators that stand down in Biomic's chunks (api.ForeignGenerators). */
@Mixin(value = GameRegistry.class, remap = false)
public abstract class MixinGameRegistryForeignGenerators {

    @WrapWithCondition(
        method = "generateWorld",
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/common/IWorldGenerator;generate(Ljava/util/Random;IILnet/minecraft/world/World;Lnet/minecraft/world/chunk/IChunkProvider;Lnet/minecraft/world/chunk/IChunkProvider;)V"),
        require = 1,
        remap = false)
    private static boolean biomic$standDown(IWorldGenerator generator, Random random, int chunkX, int chunkZ,
        World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider) {
        return !ReplacedGenerators.standsDown(generator, world, chunkGenerator);
    }
}
