package industries.angelic.biomic.mixins.late.angelica;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.gtnewhorizons.angelica.rendering.celeritas.world.cloned.ClonedChunkSection;

import industries.angelic.biomic.api.BiomeVolumeSnapshot;
import industries.angelic.biomic.client.HostedBiomeColors;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.storage.BiomeVolumeManager;

@Mixin(value = ClonedChunkSection.class, remap = false)
public abstract class MixinClonedSectionBiomes implements BiomeVolumeSnapshot {

    @Shadow
    @Final
    private World world;
    @Unique
    private BiomeVolume biomic$volume;

    @Inject(method = "fillBiomeData", at = @At("HEAD"))
    private void biomic$capture(Chunk chunk, CallbackInfo ci) {
        biomic$volume = HostedBiomeColors.enabled(world) ? BiomeVolumeManager.getVolume(chunk) : null;
    }

    @Override
    public BiomeVolume biomic$colorVolume() {
        return biomic$volume;
    }
}
