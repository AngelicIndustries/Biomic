package industries.angelic.biomic.client.render;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import net.minecraft.block.Block;
import net.minecraft.util.IIcon;

import org.junit.Test;

import industries.angelic.biomic.content.gardens.GardensContent;

public class GardensWhistlecaneClientContractTest {

    @Test
    public void sourceLeavesUseCutoutAlphaAndBlockStaysInPassZero() throws Exception {
        for (String name : new String[] { "whistlecane_leaves0.png", "whistlecane_leaves1.png" })
            assertBinaryCutout("/assets/biomic/textures/blocks/gardens/" + name);
        Block cane = GardensContent.WHISTLECANE;
        assertFalse(cane.isOpaqueCube());
        assertFalse(cane.renderAsNormalBlock());
        assertEquals("binary-alpha multipart leaves belong in the cutout pass", 0, cane.getRenderBlockPass());
        assertNull(cane.getCollisionBoundingBoxFromPool(null, 0, 0, 0));
    }

    @Test
    public void inventoryRetainsPinnedGeneratedItemRoleAndSprite() throws Exception {
        assertBinaryCutout("/assets/biomic/textures/blocks/gardens/whistlecane_item.png");
        assertFalse(
            "generated item is a sprite, not a 3-D block preview",
            new GardensWhistlecaneRenderer(71).shouldRender3DInInventory(71));
    }

    @Test
    public void iconOnlyItemConsumersReceiveItemSpriteInsteadOfPostTexture() throws Exception {
        IIcon item = (IIcon) Proxy.newProxyInstance(
            IIcon.class.getClassLoader(),
            new Class<?>[] { IIcon.class },
            (proxy, method, args) -> method.getReturnType() == boolean.class ? false
                : method.getReturnType()
                    .isPrimitive() ? 0 : null);
        Field field = GardensContent.WHISTLECANE.getClass()
            .getDeclaredField("item");
        field.setAccessible(true);
        Object previous = field.get(GardensContent.WHISTLECANE);
        try {
            field.set(GardensContent.WHISTLECANE, item);
            assertSame(item, GardensContent.WHISTLECANE.getIcon(2, 0));
        } finally {
            field.set(GardensContent.WHISTLECANE, previous);
        }
    }

    private static void assertBinaryCutout(String path) throws Exception {
        try (InputStream stream = GardensWhistlecaneClientContractTest.class.getResourceAsStream(path)) {
            assertNotNull(path, stream);
            BufferedImage image = ImageIO.read(stream);
            assertEquals(path, 16, image.getWidth());
            assertEquals(path, 16, image.getHeight());
            boolean clear = false, solid = false;
            for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
                int alpha = image.getRGB(x, y) >>> 24;
                if (alpha == 0) clear = true;
                else if (alpha == 255) solid = true;
                else throw new AssertionError(path + " has non-cutout alpha " + alpha);
            }
            assertTrue(path, clear && solid);
        }
    }

    private static String resourceText(String path) throws Exception {
        try (InputStream stream = GardensWhistlecaneClientContractTest.class.getResourceAsStream(path)) {
            assertNotNull(path, stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
