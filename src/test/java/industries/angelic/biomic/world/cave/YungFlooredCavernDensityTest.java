package industries.angelic.biomic.world.cave;

import static industries.angelic.biomic.world.cave.CaveComposition.Material.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class YungFlooredCavernDensityTest {

    private static final Fixture[] UPSTREAM_FIXTURES = { f(42L, 0, 1, 0, 0xbf59b1e8), f(42L, 0, 10, 0, 0xbe6ee7a4),
        f(42L, 0, 18, 0, 0xbec88fac), f(42L, 0, 27, 0, 0x3e84c522), f(42L, 0, 33, 0, 0xbf48b3ed),
        f(42L, -17, 17, 16, 0x3e8aee42), f(42L, 15, 26, -1, 0xbc619380), f(42L, 16, 32, -17, 0xbed9f9ec),
        f(42L, 12345, 18, -23456, 0xbd641750), f(42L, Integer.MIN_VALUE, 27, Integer.MAX_VALUE, 0x3ebb2fcd),
        f(-7046029254386353131L, 0, 1, 0, 0xbf391f17), f(-7046029254386353131L, 0, 10, 0, 0xbe032ae4),
        f(-7046029254386353131L, 0, 18, 0, 0x3f08d8ad), f(-7046029254386353131L, 0, 27, 0, 0xbe636958),
        f(-7046029254386353131L, 0, 33, 0, 0xbf05987f), f(-7046029254386353131L, -17, 17, 16, 0x3ec1ca9a),
        f(-7046029254386353131L, 15, 26, -1, 0xbcc98480), f(-7046029254386353131L, 16, 32, -17, 0xbe4e707f),
        f(-7046029254386353131L, 12345, 18, -23456, 0xbd718fe0),
        f(-7046029254386353131L, Integer.MIN_VALUE, 27, Integer.MAX_VALUE, 0x3ded43d0),
        f(4294967303L, 0, 1, 0, 0xbf391f17), f(4294967303L, 0, 10, 0, 0x3dbd7f1e), f(4294967303L, 0, 18, 0, 0x3eb6bead),
        f(4294967303L, 0, 27, 0, 0x3ee6751e), f(4294967303L, 0, 33, 0, 0xbe70b943),
        f(4294967303L, -17, 17, 16, 0x3e86a30c), f(4294967303L, 15, 26, -1, 0x3e0e3668),
        f(4294967303L, 16, 32, -17, 0xbe826576), f(4294967303L, 12345, 18, -23456, 0xbd036ee0),
        f(4294967303L, Integer.MIN_VALUE, 27, Integer.MAX_VALUE, 0x3ea343d1) };
    private static final YungFlooredCavernDensity.Settings CUSTOM_A = new YungFlooredCavernDensity.Settings(
        1,
        35,
        10,
        1.3f,
        .7f,
        .6f,
        3,
        .5f,
        .041f,
        1);
    private static final YungFlooredCavernDensity.Settings CUSTOM_B = new YungFlooredCavernDensity.Settings(
        1,
        35,
        10,
        1.3f,
        .7f,
        .6f,
        4,
        1f,
        .013f,
        3);
    private static final CustomFixture[] CUSTOM_UPSTREAM_FIXTURES = { c(CUSTOM_A, 0, 1, 0, 0xbece7c05),
        c(CUSTOM_A, -17, 10, 16, 0xbe971af4), c(CUSTOM_A, 15, 18, -1, 0x3e79a2b0), c(CUSTOM_A, 16, 27, -17, 0x3e72720e),
        c(CUSTOM_A, 12345, 32, -23456, 0xbe0139e9), c(CUSTOM_A, Integer.MIN_VALUE, 33, Integer.MAX_VALUE, 0x3e448478),
        c(CUSTOM_B, 0, 1, 0, 0x3fd41e46), c(CUSTOM_B, -17, 10, 16, 0x406a4342), c(CUSTOM_B, 15, 18, -1, 0x405f809a),
        c(CUSTOM_B, 16, 27, -17, 0x404dc742), c(CUSTOM_B, 12345, 32, -23456, 0x4061ef80),
        c(CUSTOM_B, Integer.MIN_VALUE, 33, Integer.MAX_VALUE, 0x3efc421d) };

    @Test
    public void invalidSettingsFailBeforeSampling() {
        assertThrows(IllegalArgumentException.class, () -> new YungFlooredCavernDensity(0, null));
        assertThrows(
            IllegalArgumentException.class,
            () -> new YungFlooredCavernDensity.Settings(-1, 35, 10, 1, 1, .6f, 1, .3f, .028f, 2));
        assertThrows(
            IllegalArgumentException.class,
            () -> new YungFlooredCavernDensity.Settings(1, 2, 10, 1, 1, .6f, 1, .3f, .028f, 2));
        assertThrows(
            IllegalArgumentException.class,
            () -> new YungFlooredCavernDensity.Settings(0, 3, 10, 1, 1, .6f, 1, .3f, .028f, 2));
        assertThrows(
            IllegalArgumentException.class,
            () -> new YungFlooredCavernDensity.Settings(1, 35, 10, Float.NaN, 1, .6f, 1, .3f, .028f, 2));
        assertThrows(
            IllegalArgumentException.class,
            () -> new YungFlooredCavernDensity.Settings(1, 35, 10, 1, 1, .6f, 0, .3f, .028f, 2));
        assertThrows(
            IllegalArgumentException.class,
            () -> new YungFlooredCavernDensity.Settings(1, 35, 10, 1, 1, .6f, 1, .3f, .028f, 0));
    }

    private static Fixture f(long seed, int x, int y, int z, int bits) {
        return new Fixture(seed, x, y, z, bits);
    }

    private static CustomFixture c(YungFlooredCavernDensity.Settings settings, int x, int y, int z, int bits) {
        return new CustomFixture(settings, x, y, z, bits);
    }

    private static int index(int x, int y, int z) {
        return (x * 16 + z) * 256 + y;
    }

    private record Fixture(long seed, int x, int y, int z, int bits) {}

    private record CustomFixture(YungFlooredCavernDensity.Settings settings, int x, int y, int z, int bits) {}
}
