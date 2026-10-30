package industries.angelic.biomic.mixins.late.angelica;

import net.minecraft.world.biome.BiomeGenBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.gtnewhorizons.angelica.rendering.celeritas.world.WorldSlice;
import com.gtnewhorizons.angelica.rendering.celeritas.world.cloned.ClonedChunkSection;

import industries.angelic.biomic.api.BiomeColorAccess;
import industries.angelic.biomic.api.BiomeVolumeSnapshot;
import industries.angelic.biomic.api.BiomicBiomeApi;

@Mixin(value = WorldSlice.class, remap = false)
public abstract class MixinWorldSliceBiomes implements BiomeColorAccess {

    @Shadow
    private ClonedChunkSection[] sections;
    @Shadow
    private int baseX;
    @Shadow
    private int baseY;
    @Shadow
    private int baseZ;

    @Shadow
    public static int getLocalSectionIndex(int x, int y, int z) {
        throw new AssertionError();
    }

    @Override
    public BiomeGenBase biomic$colorBiome(int x, int y, int z) {
        int sx = (x - baseX) >> 4, sy = (y - baseY) >> 4, sz = (z - baseZ) >> 4;
        if (sections == null || sx < 0 || sx >= 3 || sy < 0 || sy >= 3 || sz < 0 || sz >= 3) return null;
        ClonedChunkSection section = sections[getLocalSectionIndex(sx, sy, sz)];
        if (section == null) return null;
        // Through the vanilla interface, not a @Shadow: this mixin is unmapped (Angelica's members), and the release
        // jar
        // knows this vanilla method only by its SRG name; Biomic's own call is remapped when it is built.
        BiomeGenBase surface = ((net.minecraft.world.IBlockAccess) (Object) this).getBiomeGenForCoords(x, z);
        return BiomicBiomeApi.getSnapshotBiome(surface, ((BiomeVolumeSnapshot) section).biomic$colorVolume(), x, y, z);
    }
}
