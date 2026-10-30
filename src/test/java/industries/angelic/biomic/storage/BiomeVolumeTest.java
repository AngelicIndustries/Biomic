package industries.angelic.biomic.storage;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class BiomeVolumeTest {

    @Test
    public void storedCoverageIsNotTheCompanionsBuildHeight() {
        var volume = BiomeVolume.surfaceOnly(0);
        Assert.assertEquals(256, volume.getBlockHeight());
        Assert.assertTrue(volume.containsBlockY(255));
        for (int y : new int[] { -1, 256, 511, 512 }) Assert.assertFalse(volume.containsBlockY(y));
        Assert.assertEquals(1024, volume.copyPaletteIndices().length);
    }

    @Test
    public void quartCoordinatesUseYThenZThenXOrdering() {
        short[] cells = new short[BiomeVolume.CELL_COUNT];
        cells[(9 * BiomeVolume.QUART_WIDTH + 2) * BiomeVolume.QUART_WIDTH + 3] = 1;
        BiomeVolume volume = BiomeVolume.create(1, Arrays.asList(BiomeVolume.SURFACE_KEY, "biomic:test_caves"), cells);

        Assert.assertEquals(1, volume.getPaletteIndex(3, 9, 2));
        Assert.assertEquals("biomic:test_caves", volume.getBiomeKey(3, 9, 2));
        Assert.assertEquals(BiomeVolume.SURFACE_KEY, volume.getBiomeKey(0, 0, 0));
    }

    @Test
    public void constructorAndAccessorsDefensivelyCopy() {
        short[] cells = new short[BiomeVolume.CELL_COUNT];
        BiomeVolume volume = BiomeVolume.create(0, Arrays.asList(BiomeVolume.SURFACE_KEY), cells);

        cells[0] = 5;
        short[] exported = volume.copyPaletteIndices();
        exported[0] = 5;

        Assert.assertEquals(0, volume.getPaletteIndex(0, 0, 0));
        Assert.assertThrows(
            UnsupportedOperationException.class,
            () -> volume.getPalette()
                .add("biomic:nope"));
    }

    @Test
    public void rejectsInvalidPaletteAndCellShapes() {
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> BiomeVolume.create(0, Arrays.asList("biomic:not_surface"), new short[BiomeVolume.CELL_COUNT]));
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> BiomeVolume.create(0, Arrays.asList(BiomeVolume.SURFACE_KEY), new short[10]));
    }
}
