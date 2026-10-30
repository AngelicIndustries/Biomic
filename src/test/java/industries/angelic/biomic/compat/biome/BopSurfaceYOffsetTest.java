package industries.angelic.biomic.compat.biome;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.EnumSkyBlock;

import org.junit.Test;

import industries.angelic.biomic.compat.flora.NativeFeatureCapture;

public class BopSurfaceYOffsetTest {

    @Test
    public void everyCoordinateQueryDelegatesAtThePhysicalHeight() {
        var calls = new ArrayList<String>();
        var random = new Random(1);
        var source = new NativeFeatureCapture.SurfaceAccess() {

            private void record(String action, int x, int y, int z) {
                calls.add(action + ":" + x + "," + y + "," + z);
            }

            public Block block(int x, int y, int z) {
                record("block", x, y, z);
                return null;
            }

            public int metadata(int x, int y, int z) {
                record("meta", x, y, z);
                return 7;
            }

            public boolean set(int x, int y, int z, Block b, int meta) {
                record("set" + meta, x, y, z);
                return false;
            }

            public int heightValue(int x, int z) {
                record("height", x, 0, z);
                return 144;
            }

            public TileEntity tileEntity(int x, int y, int z) {
                record("tile", x, y, z);
                return null;
            }

            public int savedLight(EnumSkyBlock kind, int x, int y, int z) {
                record("light" + kind, x, y, z);
                return 8;
            }

            public int fullLight(int x, int y, int z) {
                record("full", x, y, z);
                return 9;
            }

            public boolean seesSky(int x, int y, int z) {
                record("sky", x, y, z);
                return false;
            }

            public Random random() {
                return random;
            }
        };
        var translated = new BopSurfaceYOffset(source, 80);
        translated.block(2, 58, 3);
        assertEquals(7, translated.metadata(2, 58, 3));
        assertFalse(translated.set(2, 58, 3, null, 4));
        assertEquals(64, translated.heightValue(2, 3));
        translated.tileEntity(2, 58, 3);
        assertEquals(8, translated.savedLight(EnumSkyBlock.Block, 2, 58, 3));
        assertEquals(9, translated.fullLight(2, 58, 3));
        assertFalse(translated.seesSky(2, 58, 3));
        assertSame(random, translated.random());
        assertEquals(
            List.of(
                "block:2,138,3",
                "meta:2,138,3",
                "set4:2,138,3",
                "height:2,0,3",
                "tile:2,138,3",
                "lightBlock:2,138,3",
                "full:2,138,3",
                "sky:2,138,3"),
            calls);
        assertThrows(ArithmeticException.class, () -> translated.block(0, Integer.MAX_VALUE, 0));
    }
}
