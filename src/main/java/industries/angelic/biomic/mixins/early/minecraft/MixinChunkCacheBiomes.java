package industries.angelic.biomic.mixins.early.minecraft;

import net.minecraft.world.ChunkCache;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.biomic.api.BiomeColorAccess;
import industries.angelic.biomic.api.BiomicBiomeApi;
import industries.angelic.biomic.client.HostedBiomeColors;
import industries.angelic.biomic.storage.BiomeVolume;
import industries.angelic.biomic.storage.BiomeVolumeManager;

@Mixin(ChunkCache.class)
public abstract class MixinChunkCacheBiomes implements BiomeColorAccess {

    @Shadow
    private int chunkX;
    @Shadow
    private int chunkZ;
    @Shadow
    private Chunk[][] chunkArray;

    @Shadow
    public abstract BiomeGenBase getBiomeGenForCoords(int x, int z);

    @Unique
    private BiomeVolume[][] biomic$volumes;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void biomic$capture(World world, int x, int y, int z, int maxX, int maxY, int maxZ, int padding,
        CallbackInfo ci) {
        if (!HostedBiomeColors.enabled(world)) return;
        biomic$volumes = new BiomeVolume[chunkArray.length][];
        for (int cx = 0; cx < chunkArray.length; cx++) {
            biomic$volumes[cx] = new BiomeVolume[chunkArray[cx].length];
            for (int cz = 0; cz < chunkArray[cx].length; cz++) {
                if (chunkArray[cx][cz] != null)
                    biomic$volumes[cx][cz] = BiomeVolumeManager.getVolume(chunkArray[cx][cz]);
            }
        }
    }

    @Override
    public BiomeGenBase biomic$colorBiome(int x, int y, int z) {
        int cx = (x >> 4) - chunkX, cz = (z >> 4) - chunkZ;
        if (biomic$volumes == null || cx < 0
            || cx >= biomic$volumes.length
            || cz < 0
            || cz >= biomic$volumes[cx].length) return null;
        return BiomicBiomeApi.getSnapshotBiome(getBiomeGenForCoords(x, z), biomic$volumes[cx][cz], x, y, z);
    }
}
