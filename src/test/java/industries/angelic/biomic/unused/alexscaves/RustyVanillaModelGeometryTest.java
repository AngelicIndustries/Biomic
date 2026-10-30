package industries.angelic.biomic.unused.alexscaves;

import static org.junit.Assert.*;

import org.junit.Test;

import industries.angelic.biomic.content.alexscaves.AbyssalModelGeometry;

public class RustyVanillaModelGeometryTest {

    @Test
    public void lightningRodUsesPinnedElementsAndBlockstateRotations() {
        var up = RustyVanillaModelGeometry.lightningRod(1);
        assertEquals(2, up.elements().length);
        assertBounds(up.elements()[0], 6, 12, 6, 10, 16, 10);
        assertEquals(0, up.elements()[0].modelX());
        assertEquals(
            90,
            RustyVanillaModelGeometry.lightningRod(2)
                .elements()[0].modelX());
        assertEquals(
            0,
            RustyVanillaModelGeometry.lightningRod(2)
                .elements()[0].modelY());
        assertEquals(
            180,
            RustyVanillaModelGeometry.lightningRod(3)
                .elements()[0].modelY());
        assertEquals(
            270,
            RustyVanillaModelGeometry.lightningRod(4)
                .elements()[0].modelY());
        assertEquals(
            90,
            RustyVanillaModelGeometry.lightningRod(5)
                .elements()[0].modelY());
    }

    @Test
    public void wetTrapdoorAndCeilingButtonUsePinnedShapes() {
        var trap = RustyVanillaModelGeometry.wetIronTrapdoor(5);
        assertEquals(1, trap.elements().length);
        assertBounds(trap.elements()[0], 0, 13, 0, 16, 16, 16);
        var button = RustyVanillaModelGeometry.ceilingButton(4);
        assertEquals(1, button.elements().length);
        assertBounds(button.elements()[0], 5, 0, 6, 11, 2, 10);
        assertEquals(180, button.elements()[0].modelX());
        assertEquals(90, button.elements()[0].modelY());
    }

    @Test
    public void unsupportedFacingMetadataFailsClosed() {
        for (int facing : new int[] { -1, 0, 6, 15 }) {
            assertThrows(IllegalArgumentException.class, () -> RustyVanillaModelGeometry.lightningRod(facing));
            assertThrows(IllegalArgumentException.class, () -> RustyVanillaModelGeometry.wetIronTrapdoor(facing));
            assertThrows(IllegalArgumentException.class, () -> RustyVanillaModelGeometry.ceilingButton(facing));
        }
    }

    private static void assertBounds(AbyssalModelGeometry.Element e, double fx, double fy, double fz, double tx,
        double ty, double tz) {
        assertEquals(fx, e.fromX(), 0);
        assertEquals(fy, e.fromY(), 0);
        assertEquals(fz, e.fromZ(), 0);
        assertEquals(tx, e.toX(), 0);
        assertEquals(ty, e.toY(), 0);
        assertEquals(tz, e.toZ(), 0);
    }
}
