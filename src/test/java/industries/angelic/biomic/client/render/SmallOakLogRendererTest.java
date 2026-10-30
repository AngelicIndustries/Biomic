package industries.angelic.biomic.client.render;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.biomic.content.block.BlockSmallOakLog;

public class SmallOakLogRendererTest {

    @Test
    public void bambooLeafPlanesHaveOppositeFacesAndRotateWithTheTrunk() {
        for (int metadata : new int[] { 1, 5, 9 }) for (int plane = 0; plane < 2; plane++) {
            var front = SmallOakLogRenderer.bambooLeafVertices(metadata, plane, false);
            var back = SmallOakLogRenderer.bambooLeafVertices(metadata, plane, true);
            double[] a = SmallOakLogRenderer.normal(front), b = SmallOakLogRenderer.normal(back);
            for (int axis = 0; axis < 3; axis++) Assert.assertEquals(a[axis], -b[axis], .00001);
            // 18-wide source plane exceeds the small trunk; never stretch the entire texture over a cube.
            double width = 0;
            for (int axis = 0; axis < 3; axis++) width += Math.pow(front[0][axis] - front[3][axis], 2);
            Assert.assertEquals(18, Math.sqrt(width), .00001);
        }
    }

    @Test
    public void allAxisVariantsMatchSourceHalfWidthCollisionBounds() {
        for (int metadata : new int[] { 0, 4, 8 }) {
            double[] bounds = BlockSmallOakLog.bounds(metadata);
            double[] actual = { 16, 16, 16, 0, 0, 0 };
            for (int face = 0; face < 6; face++) {
                double[][] vertices = SmallOakLogRenderer.vertices(metadata, face);
                for (double[] vertex : vertices) for (int axis = 0; axis < 3; axis++) {
                    actual[axis] = Math.min(actual[axis], vertex[axis]);
                    actual[axis + 3] = Math.max(actual[axis + 3], vertex[axis]);
                }
                // Rotations must retain outward winding, including both horizontal end caps.
                double[] normal = SmallOakLogRenderer.normal(vertices);
                double dot = 0;
                for (int axis = 0; axis < 3; axis++) dot += normal[axis] * (vertices[0][axis] - 8);
                Assert.assertTrue(dot > 0);
            }
            Assert.assertArrayEquals(bounds, actual, .00001);
        }
    }

    @Test
    public void barkNeverSamplesThePackedEndGrainOrUnusedQuarter() {
        for (int face = 0; face < 6; face++) {
            double[][] uv = SmallOakLogRenderer.uvs(face);
            Assert.assertArrayEquals(face < 2 ? new double[] { 8, 0 } : new double[] { 0, 0 }, uv[0], 0);
            Assert.assertArrayEquals(face < 2 ? new double[] { 16, 8 } : new double[] { 8, 16 }, uv[2], 0);
        }
    }

    @Test
    public void endGrainMovesWithTheLogAxis() {
        for (int metadata : new int[] { 0, 4, 8 }) {
            int axis = metadata == 4 ? 0 : metadata == 8 ? 2 : 1;
            for (int face = 0; face < 2; face++) {
                double[] normal = SmallOakLogRenderer.normal(SmallOakLogRenderer.vertices(metadata, face));
                Assert.assertEquals(1, Math.abs(normal[axis]), .00001);
            }
        }
    }
}
