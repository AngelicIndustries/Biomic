package industries.angelic.biomic.client.render;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

public class RuBranchSpriteTest {

    @Test
    public void everyImportedBranchKeepsEveryOriginalPixel() throws Exception {
        for (String name : new String[] { "baobab", "birch", "blackwood", "dead", "kapok", "larch", "magnolia", "maple",
            "oak", "silver_birch", "socotra" }) {
            try (InputStream input = getClass()
                .getResourceAsStream("/assets/biomic/textures/blocks/ru/" + name + "_branch.png")) {
                Assert.assertNotNull(name, input);
                BufferedImage source = ImageIO.read(input), padded = RuBranchSprite.pad(source);
                Assert.assertEquals(source.getWidth(), padded.getWidth());
                Assert.assertEquals(source.getWidth(), padded.getHeight());
                for (int y = 0; y < padded.getHeight(); y++) for (int x = 0; x < padded.getWidth(); x++) {
                    Assert.assertEquals(
                        name + " " + x + "," + y,
                        y < source.getHeight() ? source.getRGB(x, y) : 0,
                        padded.getRGB(x, y));
                }
            }
        }
    }
}
