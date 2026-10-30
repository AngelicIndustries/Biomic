package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.WorldChunkManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.biomic.world.NetherWorldData;
import industries.angelic.biomic.world.WorldChunkManagerBiomicNether;

/**
 * The Tartarus Nether's biome map. Hooked on the base class so it holds whichever provider (vanilla, EFR,
 * Netherlicious) owns dimension -1.
 */
@Mixin(WorldProvider.class)
public abstract class MixinWorldProviderBiomicNether {

    @Shadow
    public WorldChunkManager worldChunkMgr;

    @Inject(method = "registerWorld", at = @At("TAIL"), require = 1)
    private void biomic$tartarusBiomes(World world, CallbackInfo info) {
        if (NetherWorldData.claims(world)) worldChunkMgr = WorldChunkManagerBiomicNether.forWorld(world);
    }
}
