package industries.angelic.biomic.client.render;

import org.junit.Assert;
import org.junit.Test;

import industries.angelic.biomic.content.block.BlockSaguaro;

public class SaguaroRendererTest {

    @Test
    public void mirroredCornersRetainTheirActualBendDirection() {
        Assert.assertEquals("west_up", BlockSaguaro.shape(BlockSaguaro.metadata("east_up", 4)));
        Assert.assertEquals("east_up", BlockSaguaro.shape(BlockSaguaro.metadata("west_up", 4)));
        Assert.assertEquals("south_up", BlockSaguaro.shape(BlockSaguaro.metadata("south_up", 4)));
        Assert.assertEquals("north_up", BlockSaguaro.shape(BlockSaguaro.metadata("east_up", 5)));
    }

    @Test
    public void sevenShapesRoundTripAndRotateWholeCubeFaces() {
        for (int metadata = 0; metadata < 7; metadata++) {
            Assert.assertEquals(metadata, BlockSaguaro.metadata(BlockSaguaro.shape(metadata), 0));
            for (int face = 0; face < 6; face++) {
                double[][] vertices = SaguaroRenderer.vertices(metadata, face);
                for (double[] v : vertices) for (double coordinate : v) {
                    Assert.assertTrue(coordinate >= -.001 && coordinate <= 16.001);
                }
            }
        }
        // Horizontal stems move the original top/bottom end faces to the corresponding horizontal axis.
        for (double[] v : SaguaroRenderer.vertices(1, 1)) Assert.assertEquals(16, Math.abs(v[0] - 8) * 2, .001);
        for (double[] v : SaguaroRenderer.vertices(2, 1)) Assert.assertEquals(16, Math.abs(v[2] - 8) * 2, .001);
    }
}
