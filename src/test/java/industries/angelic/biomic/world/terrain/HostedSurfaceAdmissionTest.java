package industries.angelic.biomic.world.terrain;

import org.junit.Assert;
import org.junit.Test;

public class HostedSurfaceAdmissionTest {

    @Test
    public void lowOutdoorMeadowsAreNotMistakenForCaves() {
        byte[] column = new byte[256];
        java.util.Arrays.fill(column, 0, 50, BiomicTerrainSampler.STONE);
        var surface = DecorationSurfaceColumn.scan(column, y -> false)
            .get(0);
        Assert.assertEquals(49, surface.floorY);
        Assert.assertTrue(surface.canHostMainland(true, false));
    }

    @Test
    public void overhangFloorsNeedLateralExposureAndHeadroom() {
        byte[] column = new byte[256];
        java.util.Arrays.fill(column, 0, 50, BiomicTerrainSampler.STONE);
        java.util.Arrays.fill(column, 85, 96, BiomicTerrainSampler.STONE);
        var surfaces = DecorationSurfaceColumn.scan(column, y -> false);
        Assert.assertTrue(
            surfaces.get(0)
                .canHostMainland(true, false));
        Assert.assertTrue(
            surfaces.get(1)
                .canHostMainland(false, true));
        Assert.assertFalse(
            surfaces.get(1)
                .canHostMainland(false, false));
        java.util.Arrays.fill(column, 52, 85, BiomicTerrainSampler.STONE);
        Assert.assertFalse(
            DecorationSurfaceColumn.scan(column, y -> false)
                .get(1)
                .canHostMainland(false, true));
    }

    @Test
    public void skyLayerKeepsItsOwnHabitatOwner() {
        byte[] column = new byte[256];
        java.util.Arrays.fill(column, 140, 150, BiomicTerrainSampler.STONE);
        Assert.assertFalse(
            DecorationSurfaceColumn.scan(column, y -> true)
                .get(0)
                .canHostMainland(true, true));
    }

    @Test
    public void underArchOwnershipIsBoundedAndSubmergedCapsStayOcean() {
        byte[] column = new byte[256];
        java.util.Arrays.fill(column, 0, 50, BiomicTerrainSampler.STONE);
        java.util.Arrays.fill(column, 85, 96, BiomicTerrainSampler.STONE);
        var surfaces = DecorationSurfaceColumn.scan(column, y -> false);
        Assert.assertTrue(
            surfaces.get(1)
                .ownsDecorationAt(50));
        Assert.assertFalse(
            surfaces.get(0)
                .ownsDecorationAt(50));
        Assert.assertFalse(
            surfaces.get(1)
                .ownsDecorationAt(90));
        Assert.assertFalse(
            surfaces.get(1)
                .ownsDecorationAt(20));
        java.util.Arrays.fill(column, 50, 63, BiomicTerrainSampler.WATER);
        var ocean = DecorationSurfaceColumn.scan(column, y -> false)
            .get(1);
        Assert.assertFalse(ocean.canHostMainland(false, true));
        Assert.assertFalse(ocean.ownsDecorationAt(63));
    }
}
