package industries.angelic.biomic.mixins.late.angelica;

import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.prupe.mcpatcher.mal.biome.BiomeAPI;

import industries.angelic.biomic.api.BiomeColorAccess;
import industries.angelic.biomic.client.HostedBiomeColors;

/** Keep MCPatcher's resource-pack colormaps, but never reuse its X/Z-only biome cache for hosted layers. */
@Mixin(value = BiomeAPI.class, remap = false)
public abstract class MixinMCPatcherBiomeColors {

    @Inject(method = "getBiomeGenAt", at = @At("HEAD"), cancellable = true)
    private static void biomic$resolve(IBlockAccess access, int x, int y, int z,
        CallbackInfoReturnable<BiomeGenBase> cir) {
        if (access instanceof BiomeColorAccess
            || access instanceof World && HostedBiomeColors.enabled((World) access)) {
            cir.setReturnValue(HostedBiomeColors.biome(access, x, y, z));
        }
    }
}
