package industries.angelic.biomic.client.render;

import org.junit.Assert;
import org.junit.Test;

public final class LegacyBlockModelRendererTest {

    private static final double EPSILON = 1.0E-9D;

    @Test
    public void mapsEveryJsonUvRotationLikeFaceBakery() {
        assertUvs(0, new double[][] { { 2, 3 }, { 2, 13 }, { 11, 13 }, { 11, 3 } });
        assertUvs(90, new double[][] { { 2, 13 }, { 11, 13 }, { 11, 3 }, { 2, 3 } });
        assertUvs(180, new double[][] { { 11, 13 }, { 11, 3 }, { 2, 3 }, { 2, 13 } });
        assertUvs(270, new double[][] { { 11, 3 }, { 2, 3 }, { 2, 13 }, { 11, 13 } });
    }

    @Test
    public void givesHorizontalFacesOppositeOutwardWindings() {
        double[][] up = vertices(LegacyBlockModelRenderer.Face.UP);
        double[][] down = vertices(LegacyBlockModelRenderer.Face.DOWN);
        Assert.assertTrue(normalY(up) > 0.0D);
        Assert.assertTrue(normalY(down) < 0.0D);
    }

    @Test
    public void rotatesElementsAroundTheirJsonOrigin() {
        double[][] points = LegacyBlockModelRenderer.vertices(
            0,
            17,
            2,
            16,
            17,
            18,
            LegacyBlockModelRenderer.Face.UP,
            LegacyBlockModelRenderer.Axis.X,
            -45,
            0,
            0,
            0,
            false,
            0);
        Assert.assertArrayEquals(new double[] { 0, 13.435028842544403D, -10.606601717798211D }, points[0], EPSILON);
    }

    @Test
    public void appliesFaceBakeryRescalePerpendicularToRotationAxis() {
        double[][] points = LegacyBlockModelRenderer.vertices(
            5,
            0.3D,
            8,
            11,
            5.3D,
            8,
            LegacyBlockModelRenderer.Face.NORTH,
            LegacyBlockModelRenderer.Axis.Y,
            45,
            8,
            1.3D,
            8,
            true,
            0);
        // A 45-degree rescaled six-pixel plane remains six pixels wide after projection.
        Assert.assertEquals(11.0D, points[0][0], EPSILON);
        Assert.assertEquals(5.0D, points[0][2], EPSILON);
    }

    @Test
    public void appliesBlockstateYRotationClockwiseAroundBlockCenter() {
        double[][] points = LegacyBlockModelRenderer.vertices(
            0,
            0,
            0,
            16,
            16,
            16,
            LegacyBlockModelRenderer.Face.UP,
            LegacyBlockModelRenderer.Axis.NONE,
            0,
            0,
            0,
            0,
            false,
            90);
        Assert.assertArrayEquals(new double[] { 16, 16, 0 }, points[0], EPSILON);
    }

    private static double[][] vertices(LegacyBlockModelRenderer.Face face) {
        return LegacyBlockModelRenderer
            .vertices(2, 0.3D, 2, 14, 0.3D, 14, face, LegacyBlockModelRenderer.Axis.NONE, 0, 0, 0, 0, false, 0);
    }

    private static double normalY(double[][] points) {
        double ax = points[1][0] - points[0][0];
        double az = points[1][2] - points[0][2];
        double bx = points[2][0] - points[0][0];
        double bz = points[2][2] - points[0][2];
        return az * bx - ax * bz;
    }

    private static void assertUvs(int rotation, double[][] expected) {
        double[][] actual = LegacyBlockModelRenderer.uvs(2, 3, 11, 13, rotation);
        for (int index = 0; index < expected.length; index++) {
            Assert.assertArrayEquals(expected[index], actual[index], EPSILON);
        }
    }
}
