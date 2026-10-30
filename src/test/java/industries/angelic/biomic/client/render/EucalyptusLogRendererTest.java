package industries.angelic.biomic.client.render;

import java.awt.Color;

import org.junit.Assert;
import org.junit.Test;

public class EucalyptusLogRendererTest {

    @Test
    public void barkTintRetainsRegionsUnexploredDefaultRainbowFunction() {
        for (int[] point : new int[][] { { 0, 0, 0 }, { 10, 64, -30 }, { -75, 119, 204 }, { 6016, 83, -42 } }) {
            int expected = Color.HSBtoRGB((point[0] + point[1] + point[2]) / 25.0F, 0.5F, 0.8F) & 0xFFFFFF;
            Assert.assertEquals(expected, EucalyptusLogRenderer.rainbowColor(point[0], point[1], point[2]));
        }
    }
}
