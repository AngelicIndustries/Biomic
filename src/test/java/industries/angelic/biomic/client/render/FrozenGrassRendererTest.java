package industries.angelic.biomic.client.render;

import org.junit.Assert;
import org.junit.Test;

public class FrozenGrassRendererTest {

    @Test
    public void snowMatchesSmallestAdjacentLayerAndDoesNotFillUnSnowedAreas() {
        Assert.assertEquals(0, FrozenGrassRenderer.layers(0, 0, 0, 0));
        for (int amount = 1; amount <= 8; amount++) for (int direction = 0; direction < 4; direction++) {
            int[] neighbours = new int[4];
            neighbours[direction] = amount;
            Assert.assertEquals(amount, FrozenGrassRenderer.layers(neighbours));
        }
        Assert.assertEquals(1, FrozenGrassRenderer.layers(8, 1, 4, 0));
    }
}
