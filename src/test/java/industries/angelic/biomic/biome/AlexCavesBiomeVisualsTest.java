package industries.angelic.biomic.biome;

import static org.junit.Assert.*;

import java.util.Arrays;

import net.minecraft.world.biome.BiomeGenBase;

import org.junit.BeforeClass;
import org.junit.Test;

import industries.angelic.biomic.api.BiomicBiomeApi;
import industries.angelic.biomic.storage.BiomeVolume;

public class AlexCavesBiomeVisualsTest {

    @BeforeClass
    public static void registerBiomes() {
        BiomicBiomes.register();
    }

    @Test
    public void pinnedBiomeJsonEffectsAreExact() {
        var forlorn = AlexCavesBiomeVisuals.profile("alexscaves:forlorn_hollows");
        assertEquals(0x28211A, forlorn.skyColor());
        assertEquals(0x5D6A84, forlorn.waterColor());
        assertEquals(-1, forlorn.grassColor());
        assertEquals(-1, forlorn.foliageColor());
        var primordial = AlexCavesBiomeVisuals.profile("alexscaves:primordial_caves");
        assertEquals(0xB3D600, primordial.grassColor());
        assertEquals(0xA1C442, primordial.foliageColor());
        assertEquals(0x05A599, primordial.waterColor());
        assertEquals(0xF1C973, primordial.skyColor());
        var abyssal = AlexCavesBiomeVisuals.profile("alexscaves:abyssal_chasm");
        assertEquals(0x7BA4FF, abyssal.skyColor());
        assertEquals(0x00154B, abyssal.waterColor());
        assertEquals(-1, abyssal.grassColor());
        assertEquals(-1, abyssal.foliageColor());
        assertNull(AlexCavesBiomeVisuals.profile("minecraft:plains"));
    }

    @Test
    public void colorFacadesAreNotNumericBiomeRegistrations() {
        BiomeGenBase primordial = AlexCavesBiomeVisuals.colorBiome("alexscaves:primordial_caves");
        assertNotNull(primordial);
        assertNotSame(primordial, BiomeGenBase.getBiomeGenArray()[primordial.biomeID]);
        assertEquals(0x05A599, primordial.waterColorMultiplier);
        assertEquals(0xB3D600, primordial.getBiomeGrassColor(0, 64, 0));
        assertEquals(0xA1C442, primordial.getBiomeFoliageColor(0, 64, 0));
        assertEquals(0xF1C973, primordial.getSkyColorByTemp(.95F));
    }

    @Test
    public void immutableThreeDimensionalSnapshotAppliesTintOnlyToStoredHabitat() {
        short[] cells = new short[BiomeVolume.CELL_COUNT];
        Arrays.fill(cells, (short) 1);
        BiomeVolume volume = BiomeVolume
            .create(2, Arrays.asList(BiomeVolume.SURFACE_KEY, "alexscaves:primordial_caves"), cells);
        BiomeGenBase surface = BiomicBiomes.getTemperateLowlands();
        assertSame(
            AlexCavesBiomeVisuals.colorBiome("alexscaves:primordial_caves"),
            BiomicBiomeApi.getSnapshotBiome(surface, volume, 4, 64, 4));
        assertSame(surface, BiomicBiomeApi.getSnapshotBiome(surface, volume, 4, 300, 4));
        assertSame(surface, BiomicBiomeApi.getSnapshotBiome(surface, BiomeVolume.surfaceOnly(2), 4, 64, 4));
    }
}
