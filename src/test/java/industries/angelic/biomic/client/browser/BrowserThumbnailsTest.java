package industries.angelic.biomic.client.browser;

import static org.junit.Assert.assertEquals;

import java.awt.image.BufferedImage;

import org.junit.Test;

public class BrowserThumbnailsTest {

    @Test
    public void keysBecomeSafePaths() {
        assertEquals("biomic/redwoods.png", BrowserThumbnails.relative("biomic:redwoods"));
        assertEquals("biomic/origen/deep_dark.png", BrowserThumbnails.relative("biomic:origen/deep_dark"));
        assertEquals("evil/_/_/x.png", BrowserThumbnails.relative("evil:../../x"));
        assertEquals("a/b_c.png", BrowserThumbnails.relative("a:B c"));
    }

    @Test
    public void fitKeepsTheMiddleSixteenByNine() {
        // A square frame: its top and bottom go, its middle band stays.
        BufferedImage square = new BufferedImage(160, 160, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 160; y++)
            for (int x = 0; x < 160; x++) square.setRGB(x, y, y < 35 || y >= 125 ? 0xFF0000 : 0x00FF00);
        BufferedImage fitted = BrowserThumbnails.fit(square, 384, 216);
        assertEquals(384, fitted.getWidth());
        assertEquals(216, fitted.getHeight());
        assertEquals(0x00FF00, fitted.getRGB(10, 5) & 0xFFFFFF);
        assertEquals(0x00FF00, fitted.getRGB(370, 210) & 0xFFFFFF);
        // A wide frame: its sides go.
        BufferedImage wide = new BufferedImage(300, 90, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 90; y++)
            for (int x = 0; x < 300; x++) wide.setRGB(x, y, x < 70 || x >= 230 ? 0xFF0000 : 0x0000FF);
        assertEquals(
            0x0000FF,
            BrowserThumbnails.fit(wide, 64, 36)
                .getRGB(1, 18) & 0xFFFFFF);
    }
}
